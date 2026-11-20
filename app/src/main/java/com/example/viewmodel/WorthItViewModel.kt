package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LoadResult
import com.example.data.WorthItJsonCodec
import com.example.data.WorthItRepository
import com.example.domain.BehavioralEngine
import com.example.domain.CooldownEngine
import com.example.domain.FinancialCalculator
import com.example.domain.Formatters
import com.example.domain.ParseAmountResult
import com.example.domain.ParsePriceResult
import com.example.domain.SavingsAndAchievementsEngine
import com.example.domain.UrlValidationResult
import com.example.model.RealityCheckResult
import com.example.model.AchievementDefinition
import com.example.model.AppMetadata
import com.example.model.DecisionOutcome
import com.example.model.EvaluationRecord
import com.example.model.FinancialSnapshot
import com.example.model.HistoryEntry
import com.example.model.IncomePeriod
import com.example.model.IncomeType
import com.example.model.ItemStatus
import com.example.model.PatternInsights
import com.example.model.SavingsSummary
import com.example.model.UserSettings
import com.example.model.VaultItem
import com.example.model.WorthItAppData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class CheckDraftState(
    val itemNameInput: String = "",
    val priceDigitsInput: String = "",
    val productUrlInput: String = "",
    val priceError: String? = null,
    val urlError: String? = null,
    val profileError: String? = null,
    val answers: List<Boolean?> = List(5) { null },
    val currentQuestionIndex: Int = 0,
    val framingRemainingSeconds: Int = 60,
    val isFramingActive: Boolean = false,
    val activeEvaluation: EvaluationRecord? = null,
    val linkedVaultItemId: String? = null,
    val isSubmittingAction: Boolean = false
) {
    val parsedPriceOrNull: Long?
        get() = (Formatters.parsePositivePrice(priceDigitsInput) as? ParsePriceResult.Valid)?.amount
}

data class ImportPreviewState(
    val rawJson: String,
    val parsedData: WorthItAppData,
    val itemsCount: Int,
    val evaluationsCount: Int,
    val achievementsCount: Int
)

data class WorthItUiState(
    val isLoading: Boolean = true,
    val appData: WorthItAppData = WorthItAppData(
        metadata = AppMetadata(createdAt = 0L, updatedAt = 0L, lastBackupAt = null)
    ),
    val nowMs: Long = System.currentTimeMillis(),
    val financialSnapshot: FinancialSnapshot = FinancialSnapshot(0.0, 0.0, 0.0),
    val savingsSummary: SavingsSummary = SavingsSummary(0L, 0, 0L, null, null, null, null),
    val patternInsights: PatternInsights = PatternInsights(false, 0),
    val anyClockTamperDetected: Boolean = false,
    val storageErrorMessage: String? = null,
    val hasPreReplaceBackup: Boolean = false,
    val checkDraft: CheckDraftState = CheckDraftState(),
    val importPreview: ImportPreviewState? = null
) {
    val isFinancialProfileConfigured: Boolean
        get() = appData.settings.incomeAmount > 0L

    val liveRealityCheck: RealityCheckResult?
        get() {
            val price = checkDraft.parsedPriceOrNull ?: return null
            return FinancialCalculator.calculateRealityCheck(price, appData.settings)
        }
}

sealed class UiEvent {
    data class ShowMessage(val message: String) : UiEvent()
    data class AchievementUnlocked(val achievement: AchievementDefinition) : UiEvent()
    data class OpenExternalUrl(val url: String) : UiEvent()
}

class WorthItViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: WorthItRepository = WorthItRepository(application),
    private val timeProvider: () -> Long = { System.currentTimeMillis() },
    startTicker: Boolean = true
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(WorthItUiState())
    val uiState: StateFlow<WorthItUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        loadInitialData()
        if (startTicker) {
            startBackgroundTicker()
        }
    }

    private fun loadInitialData() {
        val now = timeProvider()
        val loadResult = repository.loadData()
        val (initialData, loadError) = when (loadResult) {
            is LoadResult.Fresh -> loadResult.data to null
            is LoadResult.Loaded -> loadResult.data to null
            is LoadResult.Corrupted -> loadResult.fallback to loadResult.message
        }

        val refreshed = CooldownEngine.refreshVaultItems(initialData.items, now)
        val normalizedData = if (refreshed.changed) {
            val updated = initialData.copy(items = refreshed.items)
            repository.saveData(updated, now).getOrDefault(updated)
        } else {
            initialData
        }

        publishState(
            data = normalizedData,
            nowMs = now,
            storageError = loadError,
            isLoading = false,
            anyTamper = refreshed.anyClockTamperDetected
        )
    }

    private fun startBackgroundTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                tickClock(timeProvider())
            }
        }
    }

    fun tickClock(now: Long = timeProvider()) {
        val current = _uiState.value
        val refreshed = CooldownEngine.refreshVaultItems(current.appData.items, now)
        var nextData = current.appData

        if (refreshed.changed) {
            nextData = nextData.copy(items = refreshed.items)
            // Persist status transitions (e.g. waiting -> ready or tamper flag)
            repository.saveData(nextData, now)
        }

        val nextDraft = if (current.checkDraft.isFramingActive && current.checkDraft.framingRemainingSeconds > 0) {
            current.checkDraft.copy(framingRemainingSeconds = current.checkDraft.framingRemainingSeconds - 1)
        } else {
            current.checkDraft
        }

        _uiState.update { state ->
            state.copy(
                appData = nextData,
                nowMs = now,
                financialSnapshot = FinancialCalculator.computeSnapshot(nextData.settings),
                savingsSummary = SavingsAndAchievementsEngine.calculateSavingsSummary(nextData.items, nextData.settings),
                patternInsights = SavingsAndAchievementsEngine.computePatternInsights(nextData.evaluations),
                anyClockTamperDetected = refreshed.anyClockTamperDetected,
                checkDraft = nextDraft
            )
        }
    }

    private fun publishState(
        data: WorthItAppData,
        nowMs: Long = timeProvider(),
        storageError: String? = _uiState.value.storageErrorMessage,
        isLoading: Boolean = false,
        anyTamper: Boolean = data.items.any { it.clockTamperDetected }
    ) {
        _uiState.update { state ->
            state.copy(
                isLoading = isLoading,
                appData = data,
                nowMs = nowMs,
                financialSnapshot = FinancialCalculator.computeSnapshot(data.settings),
                savingsSummary = SavingsAndAchievementsEngine.calculateSavingsSummary(data.items, data.settings),
                patternInsights = SavingsAndAchievementsEngine.computePatternInsights(data.evaluations),
                anyClockTamperDetected = anyTamper,
                storageErrorMessage = storageError,
                hasPreReplaceBackup = repository.hasPreReplaceBackup()
            )
        }
    }

    private fun persistAndPublish(
        mutatedData: WorthItAppData,
        nowMs: Long = timeProvider(),
        evaluateAchievements: Boolean = true
    ): Boolean {
        val withAchievements = if (evaluateAchievements) {
            val prevIds = mutatedData.achievements.map { it.id }.toSet()
            val updatedAchievements = SavingsAndAchievementsEngine.evaluateNewAchievements(
                existing = mutatedData.achievements,
                items = mutatedData.items,
                evaluations = mutatedData.evaluations,
                nowMs = nowMs
            )
            val newlyAdded = updatedAchievements.filter { it.id !in prevIds }
            newlyAdded.forEach { rec ->
                SavingsAndAchievementsEngine.ALL_ACHIEVEMENTS.firstOrNull { it.id == rec.id }?.let { def ->
                    _events.tryEmit(UiEvent.AchievementUnlocked(def))
                }
            }
            mutatedData.copy(achievements = updatedAchievements)
        } else {
            mutatedData
        }

        val saveResult = repository.saveData(withAchievements, nowMs)
        return saveResult.fold(
            onSuccess = { saved ->
                publishState(saved, nowMs = nowMs, storageError = null)
                true
            },
            onFailure = { err ->
                // PRD 8.3: Do not erase active data; surface storage error clearly
                publishState(withAchievements, nowMs = nowMs, storageError = err.message)
                _events.tryEmit(UiEvent.ShowMessage(err.message ?: "Gagal menyimpan data."))
                false
            }
        )
    }

    fun dismissStorageError() {
        _uiState.update { it.copy(storageErrorMessage = null) }
    }

    // --- Check Item & Reality Check Draft ---

    fun startNewCheck() {
        _uiState.update {
            it.copy(
                checkDraft = CheckDraftState(
                    framingRemainingSeconds = 60,
                    isFramingActive = false
                )
            )
        }
    }

    fun updateCheckName(raw: String) {
        val truncated = Formatters.truncateNameInput(raw)
        _uiState.update {
            it.copy(checkDraft = it.checkDraft.copy(itemNameInput = truncated))
        }
    }

    fun updateCheckPrice(raw: String) {
        val digitsOnly = raw.filter { it.isDigit() }.take(13)
        _uiState.update {
            it.copy(
                checkDraft = it.checkDraft.copy(
                    priceDigitsInput = digitsOnly,
                    priceError = null
                )
            )
        }
    }

    fun updateCheckUrl(raw: String) {
        _uiState.update {
            it.copy(
                checkDraft = it.checkDraft.copy(
                    productUrlInput = raw,
                    urlError = null
                )
            )
        }
    }

    fun validateAndStartImpulseFilter(): Boolean {
        val state = _uiState.value
        val draft = state.checkDraft

        val priceResult = Formatters.parsePositivePrice(draft.priceDigitsInput)
        val priceError = (priceResult as? ParsePriceResult.Invalid)?.message

        val urlResult = Formatters.validateProductUrl(draft.productUrlInput)
        val urlError = (urlResult as? UrlValidationResult.Invalid)?.message

        val profileError = if (!state.isFinancialProfileConfigured) {
            "Lengkapi pemasukan pada Profil Finansial terlebih dahulu sebelum lanjut."
        } else null

        if (priceError != null || urlError != null || profileError != null) {
            _uiState.update {
                it.copy(
                    checkDraft = draft.copy(
                        priceError = priceError,
                        urlError = urlError,
                        profileError = profileError
                    )
                )
            }
            return false
        }

        _uiState.update {
            it.copy(
                checkDraft = draft.copy(
                    priceError = null,
                    urlError = null,
                    profileError = null,
                    currentQuestionIndex = 0,
                    framingRemainingSeconds = 60,
                    isFramingActive = true,
                    activeEvaluation = null,
                    linkedVaultItemId = null
                )
            )
        }
        return true
    }

    fun setFramingActive(active: Boolean) {
        _uiState.update {
            it.copy(checkDraft = it.checkDraft.copy(isFramingActive = active))
        }
    }

    fun answerImpulseQuestion(questionIndex: Int, answer: Boolean) {
        if (questionIndex !in 0..4) return
        _uiState.update { state ->
            val updatedAnswers = state.checkDraft.answers.toMutableList()
            updatedAnswers[questionIndex] = answer
            state.copy(
                checkDraft = state.checkDraft.copy(answers = updatedAnswers)
            )
        }
    }

    fun goToImpulseQuestion(index: Int) {
        val clamped = index.coerceIn(0, 4)
        _uiState.update {
            it.copy(checkDraft = it.checkDraft.copy(currentQuestionIndex = clamped))
        }
    }

    fun submitImpulseFilter(): Boolean {
        val state = _uiState.value
        val draft = state.checkDraft
        if (draft.isSubmittingAction) return false

        val price = draft.parsedPriceOrNull ?: return false
        if (draft.answers.any { it == null }) {
            _events.tryEmit(UiEvent.ShowMessage("Jawab kelima pertanyaan terlebih dahulu."))
            return false
        }

        _uiState.update { it.copy(checkDraft = it.checkDraft.copy(isSubmittingAction = true)) }

        val now = timeProvider()
        val boolAnswers = draft.answers.map { it == true }
        val snapshot = FinancialCalculator.computeSnapshot(state.appData.settings)
        val result = BehavioralEngine.calculateFinalScore(
            price = price,
            answers = boolAnswers,
            snapshot = snapshot
        )
        val itemName = Formatters.sanitizeItemName(draft.itemNameInput)

        val existingEval = draft.activeEvaluation
        val evalId = existingEval?.id ?: UUID.randomUUID().toString()

        val evaluation = EvaluationRecord(
            id = evalId,
            itemId = draft.linkedVaultItemId,
            itemName = itemName,
            itemPrice = price,
            behavioralScore = result.behavioralScore,
            financialPenalty = result.financialPenalty,
            finalScore = result.finalScore,
            answers = boolAnswers,
            priceRatio = if (result.priceRatio.isFinite()) result.priceRatio else 999.0,
            verdict = result.verdict,
            hardRuleTriggered = result.hardRuleTriggered,
            hardRuleReason = result.hardRuleReason,
            reasons = result.reasons,
            decision = existingEval?.decision ?: DecisionOutcome.SAVED_ONLY,
            createdAt = existingEval?.createdAt ?: now,
            financialSnapshot = snapshot
        )

        val nextEvals = if (existingEval != null) {
            state.appData.evaluations.map { if (it.id == evalId) evaluation else it }
        } else {
            listOf(evaluation) + state.appData.evaluations
        }

        val nextHistory = if (existingEval == null) {
            listOf(
                HistoryEntry(
                    id = UUID.randomUUID().toString(),
                    type = "evaluation",
                    evaluationId = evalId,
                    title = itemName,
                    amount = price,
                    timestamp = now,
                    note = "${evaluation.verdict.title} (${evaluation.finalScore}/100)"
                )
            ) + state.appData.history
        } else {
            state.appData.history
        }

        val nextData = state.appData.copy(
            evaluations = nextEvals,
            history = nextHistory
        )

        persistAndPublish(nextData, nowMs = now)

        _uiState.update {
            it.copy(
                checkDraft = it.checkDraft.copy(
                    isFramingActive = false,
                    activeEvaluation = evaluation,
                    isSubmittingAction = false
                )
            )
        }
        return true
    }

    fun addVerdictToVault(durationMs: Long, rawUrl: String): Boolean {
        val state = _uiState.value
        val draft = state.checkDraft
        val activeEval = draft.activeEvaluation ?: return false
        if (draft.isSubmittingAction || draft.linkedVaultItemId != null) {
            return false
        }

        val urlValidation = Formatters.validateProductUrl(rawUrl)
        if (urlValidation is UrlValidationResult.Invalid) {
            _events.tryEmit(UiEvent.ShowMessage(urlValidation.message))
            return false
        }
        val cleanUrl = (urlValidation as UrlValidationResult.Valid).url

        _uiState.update { it.copy(checkDraft = it.checkDraft.copy(isSubmittingAction = true)) }

        val now = timeProvider()
        val item = CooldownEngine.createVaultItem(
            id = UUID.randomUUID().toString(),
            name = activeEval.itemName,
            price = activeEval.itemPrice,
            productUrl = cleanUrl,
            durationMs = durationMs,
            nowMs = now
        )

        val updatedEval = activeEval.copy(
            itemId = item.id,
            decision = DecisionOutcome.IN_VAULT
        )

        val nextData = state.appData.copy(
            items = listOf(item) + state.appData.items,
            evaluations = state.appData.evaluations.map {
                if (it.id == updatedEval.id) updatedEval else it
            },
            history = listOf(
                HistoryEntry(
                    id = UUID.randomUUID().toString(),
                    type = "vault_added",
                    itemId = item.id,
                    evaluationId = updatedEval.id,
                    title = item.name,
                    amount = item.price,
                    timestamp = now,
                    note = "Ditunda selama ${durationMs / 3_600_000L} jam"
                )
            ) + state.appData.history
        )

        persistAndPublish(nextData, nowMs = now)

        _uiState.update {
            it.copy(
                checkDraft = it.checkDraft.copy(
                    productUrlInput = cleanUrl,
                    activeEvaluation = updatedEval,
                    linkedVaultItemId = item.id,
                    isSubmittingAction = false
                )
            )
        }
        _events.tryEmit(UiEvent.ShowMessage("Biarkan keinginan ini duduk sebentar. Barang masuk ke Vault."))
        return true
    }

    fun cancelDirectlyFromVerdict(): Boolean {
        val state = _uiState.value
        val draft = state.checkDraft
        val activeEval = draft.activeEvaluation ?: return false
        if (draft.isSubmittingAction || draft.linkedVaultItemId != null) {
            return false
        }

        _uiState.update { it.copy(checkDraft = it.checkDraft.copy(isSubmittingAction = true)) }

        val now = timeProvider()
        val item = VaultItem(
            id = UUID.randomUUID().toString(),
            name = activeEval.itemName,
            price = activeEval.itemPrice,
            productUrl = draft.productUrlInput.trim(),
            createdAt = now,
            durationMs = CooldownEngine.DURATION_48H_MS,
            expiresAt = now,
            lastSeenAt = now,
            status = ItemStatus.CANCELLED
        )

        val updatedEval = activeEval.copy(
            itemId = item.id,
            decision = DecisionOutcome.CANCELLED
        )

        val nextData = state.appData.copy(
            items = listOf(item) + state.appData.items,
            evaluations = state.appData.evaluations.map {
                if (it.id == updatedEval.id) updatedEval else it
            },
            history = listOf(
                HistoryEntry(
                    id = UUID.randomUUID().toString(),
                    type = "cancelled",
                    itemId = item.id,
                    evaluationId = updatedEval.id,
                    title = item.name,
                    amount = item.price,
                    timestamp = now,
                    note = "Dibatalkan langsung setelah evaluasi"
                )
            ) + state.appData.history
        )

        persistAndPublish(nextData, nowMs = now)

        _uiState.update {
            it.copy(
                checkDraft = it.checkDraft.copy(
                    activeEvaluation = updatedEval,
                    linkedVaultItemId = item.id,
                    isSubmittingAction = false
                )
            )
        }
        _events.tryEmit(UiEvent.ShowMessage("Uangmu aman. Keputusanmu juga."))
        return true
    }

    // --- Vault Actions ---

    fun addManualVaultItem(
        rawName: String,
        rawPriceDigits: String,
        rawUrl: String,
        durationMs: Long
    ): Result<Unit> {
        val priceResult = Formatters.parsePositivePrice(rawPriceDigits)
        if (priceResult is ParsePriceResult.Invalid) {
            return Result.failure(IllegalArgumentException(priceResult.message))
        }
        val price = (priceResult as ParsePriceResult.Valid).amount

        val urlResult = Formatters.validateProductUrl(rawUrl)
        if (urlResult is UrlValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(urlResult.message))
        }
        val cleanUrl = (urlResult as UrlValidationResult.Valid).url

        val now = timeProvider()
        val item = CooldownEngine.createVaultItem(
            id = UUID.randomUUID().toString(),
            name = rawName,
            price = price,
            productUrl = cleanUrl,
            durationMs = durationMs,
            nowMs = now
        )

        val current = _uiState.value.appData
        val nextData = current.copy(
            items = listOf(item) + current.items,
            history = listOf(
                HistoryEntry(
                    id = UUID.randomUUID().toString(),
                    type = "vault_added",
                    itemId = item.id,
                    title = item.name,
                    amount = item.price,
                    timestamp = now,
                    note = "Ditambahkan langsung ke Vault (${durationMs / 3_600_000L} jam)"
                )
            ) + current.history
        )
        persistAndPublish(nextData, nowMs = now)
        _events.tryEmit(UiEvent.ShowMessage("Biarkan keinginan ini duduk sebentar."))
        return Result.success(Unit)
    }

    fun decideVaultItem(itemId: String, cancelPurchase: Boolean) {
        val current = _uiState.value.appData
        val target = current.items.firstOrNull { it.id == itemId } ?: return
        // Double-click / state guard: only WAITING? No, only READY can be decided!
        // (Ensure cancelled can NEVER be counted twice or changed back silently)
        if (target.status != ItemStatus.READY) return

        val now = timeProvider()
        val nextStatus = if (cancelPurchase) ItemStatus.CANCELLED else ItemStatus.PURCHASED
        val nextOutcome = if (cancelPurchase) DecisionOutcome.CANCELLED else DecisionOutcome.PURCHASED

        val updatedItems = current.items.map {
            if (it.id == itemId) it.copy(status = nextStatus, lastSeenAt = now) else it
        }
        val updatedEvaluations = current.evaluations.map { eval ->
            if (eval.itemId == itemId) eval.copy(decision = nextOutcome) else eval
        }
        val historyEntry = HistoryEntry(
            id = UUID.randomUUID().toString(),
            type = nextStatus.jsonKey,
            itemId = itemId,
            title = target.name,
            amount = target.price,
            timestamp = now,
            note = if (cancelPurchase) "Dibatalkan dari Vault" else "Dilanjutkan setelah masa jeda"
        )

        val nextData = current.copy(
            items = updatedItems,
            evaluations = updatedEvaluations,
            history = listOf(historyEntry) + current.history
        )

        persistAndPublish(nextData, nowMs = now)

        if (cancelPurchase) {
            _events.tryEmit(UiEvent.ShowMessage("Uangmu aman. Keputusanmu juga."))
        } else {
            _events.tryEmit(UiEvent.ShowMessage("Keputusan dicatat. Semoga barang ini benar-benar bermanfaat."))
            if (target.productUrl.isNotBlank()) {
                _events.tryEmit(UiEvent.OpenExternalUrl(target.productUrl))
            }
        }
    }

    fun archiveVaultItem(itemId: String) {
        val current = _uiState.value.appData
        val target = current.items.firstOrNull { it.id == itemId } ?: return
        // PRD 4.6: archived items are NOT counted in savings, so only archive purchased items
        if (target.status != ItemStatus.PURCHASED) return

        val now = timeProvider()
        val updatedItems = current.items.map {
            if (it.id == itemId) it.copy(status = ItemStatus.ARCHIVED) else it
        }
        val nextData = current.copy(items = updatedItems)
        persistAndPublish(nextData, nowMs = now, evaluateAchievements = false)
        _events.tryEmit(UiEvent.ShowMessage("Item dipindahkan ke arsip."))
    }

    // --- Settings & Financial Profile ---

    fun updateFinancialSettings(
        incomeType: IncomeType,
        incomePeriod: IncomePeriod,
        rawIncomeDigits: String,
        rawExpensesDigits: String,
        rawLunchDigits: String,
        rawCoffeeDigits: String,
        rawInternetDigits: String,
        rawCustomDailyAllowance: String = "",
        rawCustomHourlyWage: String = ""
    ): Result<Unit> {
        val incomeRes = Formatters.parseNonNegativeAmount(rawIncomeDigits, "Pemasukan")
        if (incomeRes is ParseAmountResult.Invalid) {
            return Result.failure(IllegalArgumentException(incomeRes.message))
        }
        val income = (incomeRes as ParseAmountResult.Valid).amount
        if (income <= 0L) {
            return Result.failure(IllegalArgumentException("Pemasukan harus lebih dari Rp0."))
        }

        val expenseRes = Formatters.parseNonNegativeAmount(rawExpensesDigits, "Pengeluaran wajib")
        if (expenseRes is ParseAmountResult.Invalid) {
            return Result.failure(IllegalArgumentException(expenseRes.message))
        }
        val expenses = (expenseRes as ParseAmountResult.Valid).amount

        val lunchRes = Formatters.parsePositivePrice(rawLunchDigits)
        if (lunchRes is ParsePriceResult.Invalid) {
            return Result.failure(IllegalArgumentException("Harga makan siang harus lebih dari Rp0."))
        }
        val coffeeRes = Formatters.parsePositivePrice(rawCoffeeDigits)
        if (coffeeRes is ParsePriceResult.Invalid) {
            return Result.failure(IllegalArgumentException("Harga kopi harus lebih dari Rp0."))
        }
        val internetRes = Formatters.parsePositivePrice(rawInternetDigits)
        if (internetRes is ParsePriceResult.Invalid) {
            return Result.failure(IllegalArgumentException("Harga kuota internet harus lebih dari Rp0."))
        }

        val customDaily = if (rawCustomDailyAllowance.isBlank()) null else {
            (Formatters.parsePositivePrice(rawCustomDailyAllowance) as? ParsePriceResult.Valid)?.amount
                ?: return Result.failure(IllegalArgumentException("Uang saku harian kustom harus lebih dari Rp0."))
        }
        val customHourly = if (rawCustomHourlyWage.isBlank()) null else {
            (Formatters.parsePositivePrice(rawCustomHourlyWage) as? ParsePriceResult.Valid)?.amount
                ?: return Result.failure(IllegalArgumentException("Upah per jam kustom harus lebih dari Rp0."))
        }

        val newSettings = UserSettings(
            currency = "IDR",
            incomeType = incomeType,
            incomeAmount = income,
            incomePeriod = incomePeriod,
            mandatoryExpenses = expenses,
            lunchPrice = (lunchRes as ParsePriceResult.Valid).amount,
            coffeePrice = (coffeeRes as ParsePriceResult.Valid).amount,
            internetPrice = (internetRes as ParsePriceResult.Valid).amount,
            customDailyAllowance = customDaily,
            customHourlyWage = customHourly
        )

        val current = _uiState.value.appData
        val nextData = current.copy(settings = newSettings)
        persistAndPublish(nextData, evaluateAchievements = false)

        _uiState.update {
            it.copy(checkDraft = it.checkDraft.copy(profileError = null))
        }
        _events.tryEmit(UiEvent.ShowMessage("Pengaturan finansial berhasil disimpan."))
        return Result.success(Unit)
    }

    fun updateComparatorsQuick(
        rawLunchDigits: String,
        rawCoffeeDigits: String,
        rawInternetDigits: String,
        rawDailyAllowanceDigits: String,
        rawHourlyWageDigits: String
    ): Result<Unit> {
        val lunchRes = Formatters.parsePositivePrice(rawLunchDigits)
        val coffeeRes = Formatters.parsePositivePrice(rawCoffeeDigits)
        val internetRes = Formatters.parsePositivePrice(rawInternetDigits)
        if (lunchRes is ParsePriceResult.Invalid ||
            coffeeRes is ParsePriceResult.Invalid ||
            internetRes is ParsePriceResult.Invalid
        ) {
            return Result.failure(IllegalArgumentException("Harga pembanding harus lebih dari Rp0."))
        }

        val customDaily = if (rawDailyAllowanceDigits.isBlank()) null else {
            (Formatters.parsePositivePrice(rawDailyAllowanceDigits) as? ParsePriceResult.Valid)?.amount
                ?: return Result.failure(IllegalArgumentException("Uang saku harian harus lebih dari Rp0."))
        }
        val customHourly = if (rawHourlyWageDigits.isBlank()) null else {
            (Formatters.parsePositivePrice(rawHourlyWageDigits) as? ParsePriceResult.Valid)?.amount
                ?: return Result.failure(IllegalArgumentException("Upah per jam harus lebih dari Rp0."))
        }

        val current = _uiState.value.appData
        val updatedSettings = current.settings.copy(
            lunchPrice = (lunchRes as ParsePriceResult.Valid).amount,
            coffeePrice = (coffeeRes as ParsePriceResult.Valid).amount,
            internetPrice = (internetRes as ParsePriceResult.Valid).amount,
            customDailyAllowance = customDaily,
            customHourlyWage = customHourly
        )
        persistAndPublish(current.copy(settings = updatedSettings), evaluateAchievements = false)
        _events.tryEmit(UiEvent.ShowMessage("Harga pembanding diperbarui."))
        return Result.success(Unit)
    }

    // --- Export / Import / Reset ---

    fun buildExportJson(): String {
        val now = timeProvider()
        val current = _uiState.value.appData
        val withBackupTimestamp = current.copy(
            metadata = current.metadata.copy(lastBackupAt = now, updatedAt = now)
        )
        persistAndPublish(withBackupTimestamp, nowMs = now, evaluateAchievements = false)
        return WorthItJsonCodec.encode(withBackupTimestamp)
    }

    fun prepareImportJson(rawJson: String): Result<ImportPreviewState> {
        val decoded = WorthItJsonCodec.decodeAndValidate(rawJson)
        return decoded.fold(
            onSuccess = { parsed ->
                val preview = ImportPreviewState(
                    rawJson = rawJson,
                    parsedData = parsed,
                    itemsCount = parsed.items.size,
                    evaluationsCount = parsed.evaluations.size,
                    achievementsCount = parsed.achievements.size
                )
                _uiState.update { it.copy(importPreview = preview) }
                Result.success(preview)
            },
            onFailure = { err ->
                _events.tryEmit(UiEvent.ShowMessage(err.message ?: "File JSON tidak valid."))
                Result.failure(err)
            }
        )
    }

    fun dismissImportPreview() {
        _uiState.update { it.copy(importPreview = null) }
    }

    fun confirmImportReplace() {
        val preview = _uiState.value.importPreview ?: return
        val current = _uiState.value.appData
        val now = timeProvider()

        // PRD 5.3: Create backup of existing data BEFORE Replace
        val backupRes = repository.savePreReplaceBackup(current)
        if (backupRes.isFailure) {
            _events.tryEmit(UiEvent.ShowMessage("Gagal membuat backup pengaman sebelum Replace."))
            return
        }

        val refreshed = CooldownEngine.refreshVaultItems(preview.parsedData.items, now)
        val importedNormalized = preview.parsedData.copy(items = refreshed.items)

        val saved = persistAndPublish(importedNormalized, nowMs = now, evaluateAchievements = true)
        if (saved) {
            _uiState.update { it.copy(importPreview = null, hasPreReplaceBackup = true) }
            _events.tryEmit(UiEvent.ShowMessage("Data berhasil diganti (Replace). Backup data lama tersimpan di perangkat."))
        }
    }

    fun confirmImportMerge() {
        val preview = _uiState.value.importPreview ?: return
        val current = _uiState.value.appData
        val now = timeProvider()

        val mergeResult = WorthItJsonCodec.mergeAppData(current, preview.parsedData, now)
        val refreshed = CooldownEngine.refreshVaultItems(mergeResult.mergedData.items, now)
        val mergedNormalized = mergeResult.mergedData.copy(items = refreshed.items)

        val saved = persistAndPublish(mergedNormalized, nowMs = now, evaluateAchievements = true)
        if (saved) {
            _uiState.update { it.copy(importPreview = null) }
            _events.tryEmit(
                UiEvent.ShowMessage(
                    "Merge selesai: +${mergeResult.addedItemsCount} item, +${mergeResult.addedEvaluationsCount} evaluasi (${mergeResult.skippedDuplicatesCount} duplikat dilewati)."
                )
            )
        }
    }

    fun restorePreReplaceBackup() {
        val backupRes = repository.loadPreReplaceBackup()
        backupRes.fold(
            onSuccess = { oldData ->
                val now = timeProvider()
                val refreshed = CooldownEngine.refreshVaultItems(oldData.items, now)
                persistAndPublish(oldData.copy(items = refreshed.items), nowMs = now, evaluateAchievements = false)
                _events.tryEmit(UiEvent.ShowMessage("Data sebelum Replace berhasil dipulihkan."))
            },
            onFailure = { err ->
                _events.tryEmit(UiEvent.ShowMessage(err.message ?: "Gagal memulihkan backup."))
            }
        )
    }

    fun deleteAllData() {
        val now = timeProvider()
        repository.clearAllData(now).fold(
            onSuccess = { fresh ->
                publishState(fresh, nowMs = now, storageError = null)
                startNewCheck()
                _events.tryEmit(UiEvent.ShowMessage("Semua data WorthIt? telah dihapus."))
            },
            onFailure = { err ->
                _events.tryEmit(UiEvent.ShowMessage(err.message ?: "Gagal menghapus data."))
            }
        )
    }
}
