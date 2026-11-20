package com.example.data

import com.example.domain.Formatters
import com.example.domain.UrlValidationResult
import com.example.model.AchievementRecord
import com.example.model.AppMetadata
import com.example.model.DecisionOutcome
import com.example.model.EvaluationRecord
import com.example.model.FinancialSnapshot
import com.example.model.HistoryEntry
import com.example.model.IncomePeriod
import com.example.model.IncomeType
import com.example.model.ItemStatus
import com.example.model.UserSettings
import com.example.model.VaultItem
import com.example.model.VerdictType
import com.example.model.WorthItAppData
import org.json.JSONArray
import org.json.JSONObject

object WorthItJsonCodec {

    private const val SUPPORTED_VERSION = "1.0.0"

    fun encode(data: WorthItAppData): String {
        val root = JSONObject()
        root.put("appVersion", data.appVersion)

        val settingsObj = JSONObject().apply {
            put("currency", data.settings.currency)
            put("incomeType", data.settings.incomeType.jsonKey)
            put("incomeAmount", data.settings.incomeAmount)
            put("incomePeriod", data.settings.incomePeriod.jsonKey)
            put("mandatoryExpenses", data.settings.mandatoryExpenses)
            put("lunchPrice", data.settings.lunchPrice)
            put("coffeePrice", data.settings.coffeePrice)
            put("internetPrice", data.settings.internetPrice)
            if (data.settings.customDailyAllowance != null) {
                put("customDailyAllowance", data.settings.customDailyAllowance)
            }
            if (data.settings.customHourlyWage != null) {
                put("customHourlyWage", data.settings.customHourlyWage)
            }
        }
        root.put("settings", settingsObj)

        val itemsArray = JSONArray()
        data.items.forEach { item ->
            itemsArray.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("price", item.price)
                    put("productUrl", item.productUrl)
                    put("createdAt", item.createdAt)
                    put("durationMs", item.durationMs)
                    put("expiresAt", item.expiresAt)
                    put("lastSeenAt", item.lastSeenAt)
                    put("status", item.status.jsonKey)
                    put("clockTamperDetected", item.clockTamperDetected)
                }
            )
        }
        root.put("items", itemsArray)

        val evalArray = JSONArray()
        data.evaluations.forEach { eval ->
            evalArray.put(
                JSONObject().apply {
                    put("id", eval.id)
                    put("itemId", eval.itemId ?: JSONObject.NULL)
                    put("itemName", eval.itemName)
                    put("itemPrice", eval.itemPrice)
                    put("behavioralScore", eval.behavioralScore)
                    put("financialPenalty", eval.financialPenalty)
                    put("finalScore", eval.finalScore)
                    put("answers", JSONArray(eval.answers))
                    val safeRatio = if (eval.priceRatio.isFinite()) eval.priceRatio else 999.0
                    put("priceRatio", safeRatio)
                    put("verdict", eval.verdict.jsonKey)
                    put("hardRuleTriggered", eval.hardRuleTriggered)
                    put("hardRuleReason", eval.hardRuleReason)
                    put("reasons", JSONArray(eval.reasons))
                    put("decision", eval.decision.jsonKey)
                    put("createdAt", eval.createdAt)
                    put(
                        "financialSnapshot",
                        JSONObject().apply {
                            put("monthlyIncome", eval.financialSnapshot.monthlyIncome)
                            put("monthlyExpenses", eval.financialSnapshot.monthlyExpenses)
                            put("freeFunds", eval.financialSnapshot.freeFunds)
                        }
                    )
                }
            )
        }
        root.put("evaluations", evalArray)

        val achArray = JSONArray()
        data.achievements.forEach { ach ->
            achArray.put(
                JSONObject().apply {
                    put("id", ach.id)
                    put("unlockedAt", ach.unlockedAt)
                }
            )
        }
        root.put("achievements", achArray)

        val histArray = JSONArray()
        data.history.forEach { entry ->
            histArray.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("type", entry.type)
                    put("itemId", entry.itemId ?: JSONObject.NULL)
                    put("evaluationId", entry.evaluationId ?: JSONObject.NULL)
                    put("title", entry.title)
                    put("amount", entry.amount)
                    put("timestamp", entry.timestamp)
                    put("note", entry.note)
                }
            )
        }
        root.put("history", histArray)

        root.put(
            "metadata",
            JSONObject().apply {
                put("createdAt", data.metadata.createdAt)
                put("updatedAt", data.metadata.updatedAt)
                put("lastBackupAt", data.metadata.lastBackupAt ?: JSONObject.NULL)
            }
        )

        return root.toString(2)
    }

    fun decodeAndValidate(rawJson: String): Result<WorthItAppData> {
        return try {
            if (rawJson.isBlank()) {
                return Result.failure(IllegalArgumentException("File JSON kosong."))
            }
            val root = JSONObject(rawJson)

            val version = root.optString("appVersion", "")
            if (version != SUPPORTED_VERSION) {
                return Result.failure(
                    IllegalArgumentException("Versi backup tidak dikenali atau tidak didukung ($version).")
                )
            }

            val settingsJson = root.optJSONObject("settings")
                ?: return Result.failure(IllegalArgumentException("Bagian 'settings' tidak ditemukan."))
            val settings = parseSettings(settingsJson)
                ?: return Result.failure(IllegalArgumentException("Format data 'settings' tidak valid."))

            val itemsArray = root.optJSONArray("items")
                ?: return Result.failure(IllegalArgumentException("Bagian 'items' harus berupa array."))
            val items = parseItems(itemsArray)

            val evalArray = root.optJSONArray("evaluations")
                ?: return Result.failure(IllegalArgumentException("Bagian 'evaluations' harus berupa array."))
            val itemsById = items.associateBy { it.id }
            val evaluations = parseEvaluations(evalArray, itemsById)

            val achArray = root.optJSONArray("achievements") ?: JSONArray()
            val achievements = parseAchievements(achArray)

            val histArray = root.optJSONArray("history") ?: JSONArray()
            val history = parseHistory(histArray)

            val metaJson = root.optJSONObject("metadata")
                ?: return Result.failure(IllegalArgumentException("Bagian 'metadata' tidak ditemukan."))
            val createdAt = parseValidLong(metaJson, "createdAt", min = 0L)
                ?: return Result.failure(IllegalArgumentException("Metadata 'createdAt' tidak valid."))
            val updatedAt = parseValidLong(metaJson, "updatedAt", min = 0L)
                ?: return Result.failure(IllegalArgumentException("Metadata 'updatedAt' tidak valid."))
            val lastBackupAt = if (metaJson.isNull("lastBackupAt")) null else {
                parseValidLong(metaJson, "lastBackupAt", min = 0L)
            }

            Result.success(
                WorthItAppData(
                    appVersion = version,
                    settings = settings,
                    items = items,
                    evaluations = evaluations,
                    achievements = achievements,
                    history = history,
                    metadata = AppMetadata(
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        lastBackupAt = lastBackupAt
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalArgumentException(e.message ?: "File JSON rusak atau tidak sesuai skema WorthIt?.")
            )
        }
    }

    private fun parseSettings(obj: JSONObject): UserSettings? {
        val currency = obj.optString("currency", "IDR").ifBlank { "IDR" }
        val incomeAmount = parseValidLong(obj, "incomeAmount", min = 0L) ?: return null
        val mandatoryExpenses = parseValidLong(obj, "mandatoryExpenses", min = 0L) ?: return null
        val lunchPrice = parseValidLong(obj, "lunchPrice", min = 1L) ?: return null
        val coffeePrice = parseValidLong(obj, "coffeePrice", min = 1L) ?: return null
        val internetPrice = parseValidLong(obj, "internetPrice", min = 1L) ?: return null

        val customDaily = if (obj.has("customDailyAllowance") && !obj.isNull("customDailyAllowance")) {
            parseValidLong(obj, "customDailyAllowance", min = 1L) ?: return null
        } else null
        val customHourly = if (obj.has("customHourlyWage") && !obj.isNull("customHourlyWage")) {
            parseValidLong(obj, "customHourlyWage", min = 1L) ?: return null
        } else null

        return UserSettings(
            currency = currency,
            incomeType = IncomeType.fromJson(obj.optString("incomeType", "allowance")),
            incomeAmount = incomeAmount,
            incomePeriod = IncomePeriod.fromJson(obj.optString("incomePeriod", "monthly")),
            mandatoryExpenses = mandatoryExpenses,
            lunchPrice = lunchPrice,
            coffeePrice = coffeePrice,
            internetPrice = internetPrice,
            customDailyAllowance = customDaily,
            customHourlyWage = customHourly
        )
    }

    private fun parseItems(array: JSONArray): List<VaultItem> {
        val result = mutableListOf<VaultItem>()
        val seenIds = mutableSetOf<String>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i)
                ?: throw IllegalArgumentException("Item indeks $i bukan objek JSON.")
            val id = obj.optString("id", "").trim()
            if (id.isEmpty()) throw IllegalArgumentException("Item indeks $i tidak memiliki ID.")
            if (!seenIds.add(id)) throw IllegalArgumentException("Terdapat ID item duplikat: $id")

            val name = Formatters.sanitizeItemName(obj.optString("name", ""))
            val price = parseValidLong(obj, "price", min = 1L)
                ?: throw IllegalArgumentException("Harga item '$name' tidak valid.")
            val rawUrl = obj.optString("productUrl", "").trim()
            val urlValidation = Formatters.validateProductUrl(rawUrl)
            if (urlValidation is UrlValidationResult.Invalid) {
                throw IllegalArgumentException("URL item '$name' tidak valid: ${urlValidation.message}")
            }

            val createdAt = parseValidLong(obj, "createdAt", min = 0L)
                ?: throw IllegalArgumentException("createdAt item '$name' tidak valid.")
            val durationMs = parseValidLong(obj, "durationMs", min = 1L)
                ?: throw IllegalArgumentException("durationMs item '$name' tidak valid.")
            val expiresAt = parseValidLong(obj, "expiresAt", min = 0L)
                ?: throw IllegalArgumentException("expiresAt item '$name' tidak valid.")
            val lastSeenAt = parseValidLong(obj, "lastSeenAt", min = 0L) ?: createdAt
            val statusStr = obj.optString("status", "")
            val status = ItemStatus.fromJson(statusStr)
                ?: throw IllegalArgumentException("Status item '$name' tidak valid: $statusStr")

            result += VaultItem(
                id = id,
                name = name,
                price = price,
                productUrl = rawUrl,
                createdAt = createdAt,
                durationMs = durationMs,
                expiresAt = expiresAt,
                lastSeenAt = lastSeenAt,
                status = status,
                clockTamperDetected = obj.optBoolean("clockTamperDetected", false)
            )
        }
        return result
    }

    private fun parseEvaluations(
        array: JSONArray,
        itemsById: Map<String, VaultItem>
    ): List<EvaluationRecord> {
        val result = mutableListOf<EvaluationRecord>()
        val seenIds = mutableSetOf<String>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i)
                ?: throw IllegalArgumentException("Evaluasi indeks $i bukan objek JSON.")
            val id = obj.optString("id", "").trim()
            if (id.isEmpty()) throw IllegalArgumentException("Evaluasi indeks $i tidak memiliki ID.")
            if (!seenIds.add(id)) throw IllegalArgumentException("Terdapat ID evaluasi duplikat: $id")

            val itemId = if (obj.isNull("itemId")) null else obj.optString("itemId", "").ifBlank { null }
            val linkedItem = itemId?.let { itemsById[it] }

            val itemName = obj.optString("itemName", "").ifBlank {
                linkedItem?.name ?: Formatters.DEFAULT_UNNAMED_ITEM
            }
            val itemPrice = if (obj.has("itemPrice")) {
                parseValidLong(obj, "itemPrice", min = 0L) ?: (linkedItem?.price ?: 0L)
            } else {
                linkedItem?.price ?: 0L
            }

            val behavioralScore = obj.optInt("behavioralScore", -1).takeIf { it in 0..100 }
                ?: throw IllegalArgumentException("behavioralScore evaluasi '$id' tidak valid.")
            val financialPenalty = obj.optInt("financialPenalty", -1).takeIf { it in 0..100 }
                ?: throw IllegalArgumentException("financialPenalty evaluasi '$id' tidak valid.")
            val finalScore = obj.optInt("finalScore", -1).takeIf { it in 0..100 }
                ?: throw IllegalArgumentException("finalScore evaluasi '$id' tidak valid.")

            val ansArray = obj.optJSONArray("answers")
                ?: throw IllegalArgumentException("answers evaluasi '$id' harus berupa array.")
            val answers = mutableListOf<Boolean>()
            for (j in 0 until ansArray.length()) {
                val value = ansArray.opt(j)
                if (value !is Boolean) {
                    throw IllegalArgumentException("Jawaban evaluasi '$id' harus boolean.")
                }
                answers += value
            }
            if (answers.size != 5) {
                throw IllegalArgumentException("Evaluasi '$id' harus memiliki tepat 5 jawaban.")
            }

            val priceRatio = obj.optDouble("priceRatio", Double.NaN)
            if (!priceRatio.isFinite() || priceRatio < 0.0) {
                throw IllegalArgumentException("priceRatio evaluasi '$id' tidak valid.")
            }

            val verdictStr = obj.optString("verdict", "")
            val verdict = VerdictType.fromJson(verdictStr)
                ?: throw IllegalArgumentException("Verdict evaluasi '$id' tidak valid: $verdictStr")

            val createdAt = parseValidLong(obj, "createdAt", min = 0L)
                ?: throw IllegalArgumentException("createdAt evaluasi '$id' tidak valid.")

            val snapJson = obj.optJSONObject("financialSnapshot")
                ?: throw IllegalArgumentException("financialSnapshot evaluasi '$id' tidak ditemukan.")
            val monthlyIncome = snapJson.optDouble("monthlyIncome", Double.NaN)
            val monthlyExpenses = snapJson.optDouble("monthlyExpenses", Double.NaN)
            val freeFunds = snapJson.optDouble("freeFunds", Double.NaN)
            if (!monthlyIncome.isFinite() || !monthlyExpenses.isFinite() || !freeFunds.isFinite()) {
                throw IllegalArgumentException("financialSnapshot evaluasi '$id' mengandung angka tidak valid.")
            }

            val reasonsList = mutableListOf<String>()
            val reasonsJson = obj.optJSONArray("reasons")
            if (reasonsJson != null) {
                for (r in 0 until reasonsJson.length()) {
                    reasonsList += reasonsJson.optString(r, "")
                }
            }

            result += EvaluationRecord(
                id = id,
                itemId = itemId,
                itemName = Formatters.sanitizeItemName(itemName),
                itemPrice = itemPrice,
                behavioralScore = behavioralScore,
                financialPenalty = financialPenalty,
                finalScore = finalScore,
                answers = answers,
                priceRatio = priceRatio,
                verdict = verdict,
                hardRuleTriggered = obj.optBoolean("hardRuleTriggered", false),
                hardRuleReason = obj.optString("hardRuleReason", ""),
                reasons = reasonsList.filter { it.isNotBlank() },
                decision = DecisionOutcome.fromJson(obj.optString("decision", "saved_only")),
                createdAt = createdAt,
                financialSnapshot = FinancialSnapshot(
                    monthlyIncome = monthlyIncome,
                    monthlyExpenses = monthlyExpenses,
                    freeFunds = freeFunds
                )
            )
        }
        return result
    }

    private fun parseAchievements(array: JSONArray): List<AchievementRecord> {
        val result = mutableListOf<AchievementRecord>()
        val seenIds = mutableSetOf<String>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id", "").trim()
            if (id.isNotEmpty() && seenIds.add(id)) {
                val unlockedAt = parseValidLong(obj, "unlockedAt", min = 0L) ?: 0L
                result += AchievementRecord(id = id, unlockedAt = unlockedAt)
            }
        }
        return result
    }

    private fun parseHistory(array: JSONArray): List<HistoryEntry> {
        val result = mutableListOf<HistoryEntry>()
        val seenIds = mutableSetOf<String>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id", "").trim()
            if (id.isNotEmpty() && seenIds.add(id)) {
                result += HistoryEntry(
                    id = id,
                    type = obj.optString("type", "event"),
                    itemId = if (obj.isNull("itemId")) null else obj.optString("itemId", "").ifBlank { null },
                    evaluationId = if (obj.isNull("evaluationId")) null else obj.optString("evaluationId", "").ifBlank { null },
                    title = obj.optString("title", ""),
                    amount = obj.optLong("amount", 0L),
                    timestamp = obj.optLong("timestamp", 0L),
                    note = obj.optString("note", "")
                )
            }
        }
        return result
    }

    private fun parseValidLong(obj: JSONObject, key: String, min: Long): Long? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val raw = obj.opt(key)
        if (raw !is Number) return null
        val doubleVal = raw.toDouble()
        if (!doubleVal.isFinite()) return null
        val longVal = raw.toLong()
        if (longVal < min || longVal > Formatters.MAX_REASONABLE_IDR && key in setOf(
                "price", "incomeAmount", "mandatoryExpenses", "lunchPrice", "coffeePrice", "internetPrice"
            )
        ) {
            return null
        }
        return longVal
    }

    fun mergeAppData(current: WorthItAppData, imported: WorthItAppData, nowMs: Long): MergeResult {
        val existingItemIds = current.items.map { it.id }.toSet()
        val newItems = imported.items.filter { it.id !in existingItemIds }

        val existingEvalIds = current.evaluations.map { it.id }.toSet()
        val newEvals = imported.evaluations.filter { it.id !in existingEvalIds }

        val existingAchIds = current.achievements.map { it.id }.toSet()
        val newAchievements = imported.achievements.filter { it.id !in existingAchIds }

        val existingHistIds = current.history.map { it.id }.toSet()
        val newHistory = imported.history.filter { it.id !in existingHistIds }

        val mergedSettings = if (current.settings.incomeAmount <= 0L && imported.settings.incomeAmount > 0L) {
            imported.settings
        } else {
            current.settings
        }

        val mergedData = current.copy(
            settings = mergedSettings,
            items = current.items + newItems,
            evaluations = current.evaluations + newEvals,
            achievements = current.achievements + newAchievements,
            history = current.history + newHistory,
            metadata = current.metadata.copy(updatedAt = nowMs)
        )

        return MergeResult(
            mergedData = mergedData,
            addedItemsCount = newItems.size,
            addedEvaluationsCount = newEvals.size,
            addedAchievementsCount = newAchievements.size,
            skippedDuplicatesCount = (imported.items.size - newItems.size) +
                (imported.evaluations.size - newEvals.size) +
                (imported.achievements.size - newAchievements.size)
        )
    }
}

data class MergeResult(
    val mergedData: WorthItAppData,
    val addedItemsCount: Int,
    val addedEvaluationsCount: Int,
    val addedAchievementsCount: Int,
    val skippedDuplicatesCount: Int
)
