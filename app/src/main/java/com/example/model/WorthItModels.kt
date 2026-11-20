package com.example.model

enum class IncomeType(val jsonKey: String, val label: String) {
    ALLOWANCE("allowance", "Uang Saku"),
    SALARY("salary", "Gaji / Upah"),
    FREELANCE("freelance", "Penghasilan Tidak Tetap");

    companion object {
        fun fromJson(key: String?): IncomeType =
            entries.firstOrNull { it.jsonKey == key } ?: ALLOWANCE
    }
}

enum class IncomePeriod(
    val jsonKey: String,
    val label: String,
    val monthlyMultiplier: Double
) {
    DAILY("daily", "Harian", 30.0),
    WEEKLY("weekly", "Mingguan", 4.33),
    MONTHLY("monthly", "Bulanan", 1.0),
    YEARLY("yearly", "Tahunan", 1.0 / 12.0);

    companion object {
        fun fromJson(key: String?): IncomePeriod =
            entries.firstOrNull { it.jsonKey == key } ?: MONTHLY
    }
}

enum class ItemStatus(val jsonKey: String, val label: String) {
    WAITING("waiting", "Menunggu"),
    READY("ready", "Siap Diputuskan"),
    CANCELLED("cancelled", "Dibatalkan"),
    PURCHASED("purchased", "Masih Butuh"),
    ARCHIVED("archived", "Diarsipkan");

    companion object {
        fun fromJson(key: String?): ItemStatus? =
            entries.firstOrNull { it.jsonKey == key }
    }
}

enum class VerdictType(
    val jsonKey: String,
    val title: String,
    val copywriting: String,
    val actionHint: String
) {
    WORTH_IT(
        jsonKey = "worth_it",
        title = "LAYAK DIPERTIMBANGKAN",
        copywriting = "Kelihatannya masuk akal. Tetap pastikan sesuai kebutuhan.",
        actionHint = "Simpan hasil; tetap pastikan sesuai kebutuhan dan anggaran."
    ),
    RECONSIDER(
        jsonKey = "reconsider",
        title = "PIKIR ULANG",
        copywriting = "Hmm, ada yang perlu dipikirkan lagi.",
        actionHint = "Masukkan barang ke Cooldown Vault."
    ),
    LIKELY_IMPULSIVE(
        jsonKey = "likely_impulsive",
        title = "KEMUNGKINAN BESAR IMPULSIF",
        copywriting = "Kayaknya bukan barangnya yang kamu butuhkan, tapi sensasi belanjanya.",
        actionHint = "Pertimbangkan membatalkan atau menunda."
    );

    companion object {
        fun fromJson(key: String?): VerdictType? =
            entries.firstOrNull { it.jsonKey == key }
    }
}

enum class DecisionOutcome(val jsonKey: String, val label: String) {
    SAVED_ONLY("saved_only", "Hanya Evaluasi"),
    IN_VAULT("in_vault", "Masuk Cooldown Vault"),
    CANCELLED("cancelled", "Dibatalkan"),
    PURCHASED("purchased", "Tetap Dibeli");

    companion object {
        fun fromJson(key: String?): DecisionOutcome =
            entries.firstOrNull { it.jsonKey == key } ?: SAVED_ONLY
    }
}

data class UserSettings(
    val currency: String = "IDR",
    val incomeType: IncomeType = IncomeType.ALLOWANCE,
    val incomeAmount: Long = 0L,
    val incomePeriod: IncomePeriod = IncomePeriod.MONTHLY,
    val mandatoryExpenses: Long = 0L,
    val lunchPrice: Long = 15_000L,
    val coffeePrice: Long = 20_000L,
    val internetPrice: Long = 50_000L,
    val customDailyAllowance: Long? = null,
    val customHourlyWage: Long? = null
)

data class VaultItem(
    val id: String,
    val name: String,
    val price: Long,
    val productUrl: String = "",
    val createdAt: Long,
    val durationMs: Long,
    val expiresAt: Long,
    val lastSeenAt: Long,
    val status: ItemStatus = ItemStatus.WAITING,
    val clockTamperDetected: Boolean = false
)

data class FinancialSnapshot(
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val freeFunds: Double
)

data class EvaluationRecord(
    val id: String,
    val itemId: String? = null,
    val itemName: String = "Barang tanpa nama",
    val itemPrice: Long = 0L,
    val behavioralScore: Int,
    val financialPenalty: Int,
    val finalScore: Int,
    val answers: List<Boolean>,
    val priceRatio: Double,
    val verdict: VerdictType,
    val hardRuleTriggered: Boolean = false,
    val hardRuleReason: String = "",
    val reasons: List<String> = emptyList(),
    val decision: DecisionOutcome = DecisionOutcome.SAVED_ONLY,
    val createdAt: Long,
    val financialSnapshot: FinancialSnapshot
)

data class AchievementRecord(
    val id: String,
    val unlockedAt: Long
)

data class HistoryEntry(
    val id: String,
    val type: String,
    val itemId: String? = null,
    val evaluationId: String? = null,
    val title: String,
    val amount: Long = 0L,
    val timestamp: Long,
    val note: String = ""
)

data class AppMetadata(
    val createdAt: Long,
    val updatedAt: Long,
    val lastBackupAt: Long? = null
)

data class WorthItAppData(
    val appVersion: String = "1.0.0",
    val settings: UserSettings = UserSettings(),
    val items: List<VaultItem> = emptyList(),
    val evaluations: List<EvaluationRecord> = emptyList(),
    val achievements: List<AchievementRecord> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val metadata: AppMetadata
)

data class RealityCheckResult(
    val price: Long,
    val dailyAllowanceUsed: Double,
    val hourlyWageUsed: Double,
    val allowanceDays: Double?,
    val workHours: Double?,
    val lunchPortions: Double?,
    val coffeeCups: Double?,
    val internetPackages: Double?
)

data class FinalScoreResult(
    val behavioralScore: Int,
    val financialPenalty: Int,
    val finalScore: Int,
    val priceRatio: Double,
    val isExceedingFreeFunds: Boolean,
    val isFreeFundsNonPositive: Boolean,
    val hardRuleTriggered: Boolean,
    val hardRuleReason: String,
    val verdict: VerdictType,
    val reasons: List<String>
)

data class SavingsSummary(
    val totalSaved: Long,
    val cancelledCount: Int,
    val averageSaved: Long,
    val lunchPortions: Double?,
    val coffeeCups: Double?,
    val internetPackages: Double?,
    val cancellationRatePercent: Double?
)

data class PatternInsights(
    val hasEnoughData: Boolean,
    val totalEvaluations: Int,
    val minimumRequired: Int = 5,
    val averageFinalScore: Double = 0.0,
    val mostCommonWeakQuestion: String? = null,
    val impulsiveRatePercent: Double = 0.0
)

data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val badgeCode: String
)

data class ImpulseQuestion(
    val index: Int,
    val question: String,
    val weight: Int,
    val concreteExample: String,
    val weakExample: String
)
