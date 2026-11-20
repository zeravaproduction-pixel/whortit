package com.example.domain

import com.example.model.FinalScoreResult
import com.example.model.FinancialSnapshot
import com.example.model.ImpulseQuestion
import com.example.model.VerdictType
import kotlin.math.max

object BehavioralEngine {

    // PRD v1.1 Page 10 — Exact 5 Impulse Filter Questions & Weights (Total = 100)
    val QUESTIONS: List<ImpulseQuestion> = listOf(
        ImpulseQuestion(
            index = 0,
            question = "Apakah barang ini sudah masuk daftar keinginan sebelum kamu melihat promosi atau konten tentangnya?",
            weight = 20,
            concreteExample = "Sudah dicatat di wishlist jauh hari sebelum lihat flash sale, FYP, atau live shopping.",
            weakExample = "Baru kepikiran setelah muncul di beranda, review kreator, atau iklan diskon."
        ),
        ImpulseQuestion(
            index = 1,
            question = "Apakah kamu memiliki alasan konkret mengapa barang ini dibutuhkan sekarang?",
            weight = 25,
            concreteExample = "Sepatu sekolah rusak, charger tidak berfungsi, atau perlengkapan kerja habis.",
            weakExample = "Lucu, sedang viral, takut kehabisan, atau sekadar murah."
        ),
        ImpulseQuestion(
            index = 2,
            question = "Jika barang ini tidak sedang diskon, apakah kamu tetap akan mempertimbangkan membelinya?",
            weight = 15,
            concreteExample = "Manfaatnya tetap terasa sepadan walaupun harus membayar harga normal.",
            weakExample = "Tergoda terutama karena potongan harga, voucher gratis ongkir, atau label terbatas."
        ),
        ImpulseQuestion(
            index = 3,
            question = "Apakah kamu sudah mengetahui kapan, di mana, dan seberapa sering barang ini akan digunakan?",
            weight = 20,
            concreteExample = "Rencana pakai jelas, misalnya dipakai rutin setiap hari kerja/kuliah.",
            weakExample = "\"Mungkin nanti kepakai\" atau cuma untuk satu momen yang belum pasti."
        ),
        ImpulseQuestion(
            index = 4,
            question = "Apakah kamu sudah membandingkan barang ini dengan alternatif lebih murah atau barang yang sudah dimiliki?",
            weight = 20,
            concreteExample = "Sudah cek barang serupa yang dimiliki di rumah dan membandingkan opsi yang lebih hemat.",
            weakExample = "Belum membandingkan dan langsung ingin menekan tombol checkout."
        )
    )

    fun calculateBehavioralScore(answers: List<Boolean>): Int {
        var score = 0
        QUESTIONS.forEachIndexed { idx, question ->
            if (answers.getOrNull(idx) == true) {
                score += question.weight
            }
        }
        return score.coerceIn(0, 100)
    }

    // PRD v1.1 Page 10 — Financial Penalty Table
    fun calculateFinancialPenalty(priceRatio: Double, freeFunds: Double): Int {
        if (!freeFunds.isFinite() || freeFunds <= 0.0 || priceRatio.isInfinite() || priceRatio.isNaN()) {
            return 60
        }
        val pct = priceRatio * 100.0
        return when {
            pct <= 5.0 -> 0
            pct <= 10.0 -> 5
            pct <= 20.0 -> 10
            pct <= 35.0 -> 20
            pct <= 50.0 -> 30
            pct <= 75.0 -> 40
            pct <= 100.0 -> 50
            else -> 60
        }
    }

    fun getVerdict(finalScore: Int, priceRatio: Double, freeFunds: Double): VerdictType {
        val baseVerdict = when {
            finalScore >= 75 -> VerdictType.WORTH_IT
            finalScore >= 45 -> VerdictType.RECONSIDER
            else -> VerdictType.LIKELY_IMPULSIVE
        }
        // PRD 3.4 & 3.5 Hard Rule: Green verdict is blocked if freeFunds <= 0 or priceRatio > 75%
        val hardRuleBlocksGreen = !freeFunds.isFinite() || freeFunds <= 0.0 ||
            priceRatio.isInfinite() || priceRatio > 0.75
        return if (baseVerdict == VerdictType.WORTH_IT && hardRuleBlocksGreen) {
            VerdictType.RECONSIDER
        } else {
            baseVerdict
        }
    }

    fun calculateFinalScore(
        price: Long,
        answers: List<Boolean>,
        snapshot: FinancialSnapshot
    ): FinalScoreResult {
        val behavioralScore = calculateBehavioralScore(answers)
        val priceRatio = FinancialCalculator.calculatePriceRatio(price, snapshot.freeFunds)
        val financialPenalty = calculateFinancialPenalty(priceRatio, snapshot.freeFunds)
        val finalScore = max(0, behavioralScore - financialPenalty).coerceIn(0, 100)

        val isFreeFundsNonPositive = !snapshot.freeFunds.isFinite() || snapshot.freeFunds <= 0.0
        val isExceedingFreeFunds = isFreeFundsNonPositive || priceRatio.isInfinite() || priceRatio > 1.0
        val hardRuleTriggered = isFreeFundsNonPositive || priceRatio.isInfinite() || priceRatio > 0.75

        val hardRuleReason = when {
            isFreeFundsNonPositive ->
                "Dana bebasmu saat ini nol atau negatif, sehingga vonis hijau diblokir untuk melindungi anggaran wajib."
            priceRatio > 1.0 ->
                "Harga barang melebihi 100% dana bebas bulananmu, sehingga vonis hijau diblokir."
            priceRatio > 0.75 ->
                "Harga barang memakan lebih dari 75% dana bebas bulananmu, sehingga vonis hijau diblokir."
            else -> ""
        }

        val verdict = getVerdict(finalScore, priceRatio, snapshot.freeFunds)
        val reasons = buildReasons(
            answers = answers,
            behavioralScore = behavioralScore,
            financialPenalty = financialPenalty,
            priceRatio = priceRatio,
            snapshot = snapshot,
            hardRuleReason = hardRuleReason
        )

        return FinalScoreResult(
            behavioralScore = behavioralScore,
            financialPenalty = financialPenalty,
            finalScore = finalScore,
            priceRatio = priceRatio,
            isExceedingFreeFunds = isExceedingFreeFunds,
            isFreeFundsNonPositive = isFreeFundsNonPositive,
            hardRuleTriggered = hardRuleTriggered,
            hardRuleReason = hardRuleReason,
            verdict = verdict,
            reasons = reasons
        )
    }

    private fun buildReasons(
        answers: List<Boolean>,
        behavioralScore: Int,
        financialPenalty: Int,
        priceRatio: Double,
        snapshot: FinancialSnapshot,
        hardRuleReason: String
    ): List<String> {
        val reasons = mutableListOf<String>()

        val yesCount = answers.count { it }
        reasons += "Skor refleksi perilaku: $behavioralScore/100 ($yesCount dari 5 indikator kebutuhan terpenuhi)."

        if (answers.getOrNull(1) == false) {
            reasons += "Belum ada alasan kebutuhan konkret saat ini (bobot terbesar 25 poin)."
        }
        if (answers.getOrNull(0) == false) {
            reasons += "Keinginan muncul setelah melihat promosi atau konten, bukan dari daftar kebutuhan sebelumnya."
        }
        if (answers.getOrNull(2) == false) {
            reasons += "Ketertarikan masih dipengaruhi oleh diskon atau promo sementara."
        }
        if (answers.getOrNull(3) == false) {
            reasons += "Rencana pemakaian barang (kapan dan seberapa sering) belum terbayang jelas."
        }
        if (answers.getOrNull(4) == false) {
            reasons += "Belum membandingkan dengan barang yang sudah dimiliki atau alternatif lebih hemat."
        }

        if (!snapshot.freeFunds.isFinite() || snapshot.freeFunds <= 0.0) {
            reasons += "Tekanan anggaran tinggi (-$financialPenalty poin): pengeluaran wajib sudah sama dengan atau melebihi pemasukan."
        } else {
            val pctText = Formatters.formatPercent(priceRatio)
            val freeText = Formatters.formatRupiah(snapshot.freeFunds)
            if (financialPenalty == 0) {
                reasons += "Dampak finansial ringan ($pctText dari dana bebas $freeText, penalty 0 poin)."
            } else {
                reasons += "Harga setara $pctText dari dana bebas bulanan ($freeText), sehingga mendapat penyesuaian -$financialPenalty poin."
            }
        }

        if (hardRuleReason.isNotEmpty()) {
            reasons += hardRuleReason
        }

        return reasons
    }
}
