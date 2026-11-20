package com.example.domain

import com.example.model.AchievementDefinition
import com.example.model.AchievementRecord
import com.example.model.EvaluationRecord
import com.example.model.ItemStatus
import com.example.model.PatternInsights
import com.example.model.SavingsSummary
import com.example.model.UserSettings
import com.example.model.VaultItem
import com.example.model.VerdictType
import java.util.Calendar
import java.util.TimeZone

object SavingsAndAchievementsEngine {

    const val MIN_EVALUATIONS_FOR_INSIGHTS = 5

    // PRD v1.1 Page 11 — 6 Appreciative Achievements
    val ALL_ACHIEVEMENTS: List<AchievementDefinition> = listOf(
        AchievementDefinition(
            id = "first_pause",
            title = "First Pause",
            description = "Menyelesaikan evaluasi pertama.",
            badgeCode = "01"
        ),
        AchievementDefinition(
            id = "first_save",
            title = "First Save",
            description = "Membatalkan pembelian pertama.",
            badgeCode = "02"
        ),
        AchievementDefinition(
            id = "smart_shopper",
            title = "Smart Shopper",
            description = "Membatalkan 3 pembelian.",
            badgeCode = "03"
        ),
        AchievementDefinition(
            id = "money_guardian",
            title = "Money Guardian",
            description = "Menyelamatkan nilai Rp500.000.",
            badgeCode = "04"
        ),
        AchievementDefinition(
            id = "impulse_master",
            title = "Impulse Master",
            description = "Membatalkan 10 pembelian.",
            badgeCode = "05"
        ),
        AchievementDefinition(
            id = "consistent_mind",
            title = "Consistent Mind",
            description = "Melakukan evaluasi pada 7 hari berbeda.",
            badgeCode = "06"
        )
    )

    // PRD 4.6 & 9.1: Total saved MUST be recalculated strictly from items with status == CANCELLED.
    fun calculateSavingsSummary(
        items: List<VaultItem>,
        settings: UserSettings
    ): SavingsSummary {
        val cancelledItems = items.filter { it.status == ItemStatus.CANCELLED }
        val totalSaved = cancelledItems.sumOf { it.price.coerceAtLeast(0L) }
        val cancelledCount = cancelledItems.size
        val averageSaved = if (cancelledCount > 0) totalSaved / cancelledCount else 0L

        val lunch = if (settings.lunchPrice > 0L) totalSaved.toDouble() / settings.lunchPrice else null
        val coffee = if (settings.coffeePrice > 0L) totalSaved.toDouble() / settings.coffeePrice else null
        val internet = if (settings.internetPrice > 0L) totalSaved.toDouble() / settings.internetPrice else null

        val decidedCount = items.count {
            it.status == ItemStatus.CANCELLED || it.status == ItemStatus.PURCHASED || it.status == ItemStatus.ARCHIVED
        }
        val cancelRate = if (decidedCount > 0) {
            (cancelledCount.toDouble() / decidedCount.toDouble()) * 100.0
        } else {
            null
        }

        return SavingsSummary(
            totalSaved = totalSaved,
            cancelledCount = cancelledCount,
            averageSaved = averageSaved,
            lunchPortions = lunch,
            coffeeCups = coffee,
            internetPackages = internet,
            cancellationRatePercent = cancelRate
        )
    }

    fun evaluateNewAchievements(
        existing: List<AchievementRecord>,
        items: List<VaultItem>,
        evaluations: List<EvaluationRecord>,
        nowMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<AchievementRecord> {
        val existingIds = existing.map { it.id }.toSet()
        val cancelledItems = items.filter { it.status == ItemStatus.CANCELLED }
        val cancelledCount = cancelledItems.size
        val totalSaved = cancelledItems.sumOf { it.price.coerceAtLeast(0L) }
        val cal = Calendar.getInstance(timeZone)
        val distinctDays = evaluations
            .map {
                cal.timeInMillis = it.createdAt
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
            }
            .distinct()
            .size

        val unlockedNow = mutableListOf<AchievementRecord>()

        fun unlockIfEligible(id: String, condition: Boolean) {
            if (condition && id !in existingIds) {
                unlockedNow += AchievementRecord(id = id, unlockedAt = nowMs)
            }
        }

        unlockIfEligible("first_pause", evaluations.isNotEmpty())
        unlockIfEligible("first_save", cancelledCount >= 1)
        unlockIfEligible("smart_shopper", cancelledCount >= 3)
        unlockIfEligible("money_guardian", totalSaved >= 500_000L)
        unlockIfEligible("impulse_master", cancelledCount >= 10)
        unlockIfEligible("consistent_mind", distinctDays >= 7)

        return existing + unlockedNow
    }

    // PRD 5.2: Insights only appear if sample size is sufficient (>= 5 evaluations)
    fun computePatternInsights(evaluations: List<EvaluationRecord>): PatternInsights {
        val count = evaluations.size
        if (count < MIN_EVALUATIONS_FOR_INSIGHTS) {
            return PatternInsights(
                hasEnoughData = false,
                totalEvaluations = count,
                minimumRequired = MIN_EVALUATIONS_FOR_INSIGHTS
            )
        }

        val avgScore = evaluations.map { it.finalScore }.average()
        val impulsiveCount = evaluations.count { it.verdict == VerdictType.LIKELY_IMPULSIVE }
        val impulsiveRate = (impulsiveCount.toDouble() / count.toDouble()) * 100.0

        val noCounts = IntArray(BehavioralEngine.QUESTIONS.size)
        evaluations.forEach { eval ->
            BehavioralEngine.QUESTIONS.indices.forEach { idx ->
                if (eval.answers.getOrNull(idx) == false) {
                    noCounts[idx]++
                }
            }
        }
        val maxNoIndex = noCounts.indices.maxByOrNull { noCounts[it] } ?: 0
        val weakestQuestion = if (noCounts[maxNoIndex] > 0) {
            when (maxNoIndex) {
                0 -> "Sering muncul setelah terpapar promo atau konten (Q1)"
                1 -> "Alasan kebutuhan konkret masih sering belum terpenuhi (Q2)"
                2 -> "Potongan harga atau diskon sering menjadi pendorong utama (Q3)"
                3 -> "Rencana penggunaan rutin masih sering belum spesifik (Q4)"
                else -> "Jarang membandingkan dengan alternatif yang lebih hemat (Q5)"
            }
        } else {
            "Sebagian besar evaluasimu memiliki alasan yang cukup matang."
        }

        return PatternInsights(
            hasEnoughData = true,
            totalEvaluations = count,
            minimumRequired = MIN_EVALUATIONS_FOR_INSIGHTS,
            averageFinalScore = avgScore,
            mostCommonWeakQuestion = weakestQuestion,
            impulsiveRatePercent = impulsiveRate
        )
    }
}
