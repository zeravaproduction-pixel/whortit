package com.example

import com.example.data.WorthItJsonCodec
import com.example.domain.BehavioralEngine
import com.example.domain.CooldownEngine
import com.example.domain.FinancialCalculator
import com.example.domain.Formatters
import com.example.domain.ParsePriceResult
import com.example.domain.SavingsAndAchievementsEngine
import com.example.domain.UrlValidationResult
import com.example.model.AppMetadata
import com.example.model.EvaluationRecord
import com.example.model.FinancialSnapshot
import com.example.model.IncomePeriod
import com.example.model.IncomeType
import com.example.model.ItemStatus
import com.example.model.UserSettings
import com.example.model.VaultItem
import com.example.model.VerdictType
import com.example.model.WorthItAppData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WorthItEnginesTest {

    @Test
    fun `formatRupiah and parsePositivePrice handle valid and invalid inputs`() {
        assertEquals("Rp250.000", Formatters.formatRupiah(250_000L))
        assertEquals("Rp0", Formatters.formatRupiah(0L))

        assertTrue(Formatters.parsePositivePrice("") is ParsePriceResult.Invalid)
        assertTrue(Formatters.parsePositivePrice("0") is ParsePriceResult.Invalid)
        assertTrue(Formatters.parsePositivePrice("-5000") is ParsePriceResult.Invalid)
        assertTrue(Formatters.parsePositivePrice("abc") is ParsePriceResult.Invalid)

        val valid = Formatters.parsePositivePrice("Rp 150.000")
        assertTrue(valid is ParsePriceResult.Valid)
        assertEquals(150_000L, (valid as ParsePriceResult.Valid).amount)
    }

    @Test
    fun `sanitizeItemName limits to 80 chars and handles blank`() {
        assertEquals("Barang tanpa nama", Formatters.sanitizeItemName("   "))
        val longName = "a".repeat(120)
        assertEquals(80, Formatters.sanitizeItemName(longName).length)
    }

    @Test
    fun `validateProductUrl accepts empty and http https only`() {
        assertEquals(UrlValidationResult.Valid(""), Formatters.validateProductUrl("   "))
        assertEquals(
            UrlValidationResult.Valid("https://example.com/item"),
            Formatters.validateProductUrl("https://example.com/item")
        )
        assertEquals(
            UrlValidationResult.Valid("http://shop.test/p"),
            Formatters.validateProductUrl("http://shop.test/p")
        )
        assertTrue(Formatters.validateProductUrl("javascript:alert(1)") is UrlValidationResult.Invalid)
        assertTrue(Formatters.validateProductUrl("file:///etc/passwd") is UrlValidationResult.Invalid)
        assertTrue(Formatters.validateProductUrl("not a url") is UrlValidationResult.Invalid)
    }

    @Test
    fun `monthly normalization and reality check calculations match PRD`() {
        val dailySettings = UserSettings(
            incomeType = IncomeType.ALLOWANCE,
            incomeAmount = 50_000L,
            incomePeriod = IncomePeriod.DAILY,
            mandatoryExpenses = 15_000L,
            lunchPrice = 15_000L,
            coffeePrice = 20_000L,
            internetPrice = 50_000L
        )
        val snap = FinancialCalculator.computeSnapshot(dailySettings)
        assertEquals(1_500_000.0, snap.monthlyIncome, 0.01)
        assertEquals(450_000.0, snap.monthlyExpenses, 0.01)
        assertEquals(1_050_000.0, snap.freeFunds, 0.01)

        val reality = FinancialCalculator.calculateRealityCheck(150_000L, dailySettings)
        assertEquals(3.0, reality.allowanceDays!!, 0.01)
        assertEquals(10.0, reality.lunchPortions!!, 0.01)
        assertEquals(7.5, reality.coffeeCups!!, 0.01)
        assertEquals(3.0, reality.internetPackages!!, 0.01)

        assertEquals(433_000.0, FinancialCalculator.normalizeToMonthly(100_000.0, IncomePeriod.WEEKLY), 0.01)
        assertEquals(100_000.0, FinancialCalculator.normalizeToMonthly(1_200_000.0, IncomePeriod.YEARLY), 0.01)
    }

    @Test
    fun `behavioral score weights sum to 100 and penalty table is exact`() {
        assertEquals(100, BehavioralEngine.calculateBehavioralScore(listOf(true, true, true, true, true)))
        assertEquals(0, BehavioralEngine.calculateBehavioralScore(listOf(false, false, false, false, false)))
        assertEquals(65, BehavioralEngine.calculateBehavioralScore(listOf(true, true, false, true, false)))
        assertEquals(85, BehavioralEngine.calculateBehavioralScore(listOf(true, true, false, true, true)))

        assertEquals(0, BehavioralEngine.calculateFinancialPenalty(0.05, 1_000_000.0))
        assertEquals(5, BehavioralEngine.calculateFinancialPenalty(0.08, 1_000_000.0))
        assertEquals(10, BehavioralEngine.calculateFinancialPenalty(0.15, 1_000_000.0))
        assertEquals(20, BehavioralEngine.calculateFinancialPenalty(0.30, 1_000_000.0))
        assertEquals(30, BehavioralEngine.calculateFinancialPenalty(0.45, 1_000_000.0))
        assertEquals(40, BehavioralEngine.calculateFinancialPenalty(0.75, 1_000_000.0))
        assertEquals(50, BehavioralEngine.calculateFinancialPenalty(0.80, 1_000_000.0))
        assertEquals(60, BehavioralEngine.calculateFinancialPenalty(1.20, 1_000_000.0))
        assertEquals(60, BehavioralEngine.calculateFinancialPenalty(0.10, 0.0))
    }

    @Test
    fun `hard rules block green verdict when freeFunds non-positive or ratio above 75 percent`() {
        assertEquals(VerdictType.RECONSIDER, BehavioralEngine.getVerdict(90, 0.80, 1_000_000.0))
        assertEquals(VerdictType.RECONSIDER, BehavioralEngine.getVerdict(95, 0.10, 0.0))
        assertEquals(VerdictType.RECONSIDER, BehavioralEngine.getVerdict(95, 0.10, -50_000.0))
        assertEquals(VerdictType.WORTH_IT, BehavioralEngine.getVerdict(80, 0.15, 1_000_000.0))

        val highRatioResult = BehavioralEngine.calculateFinalScore(
            price = 800_000L,
            answers = listOf(true, true, true, true, true),
            snapshot = FinancialSnapshot(1_500_000.0, 500_000.0, 1_000_000.0)
        )
        assertEquals(50, highRatioResult.finalScore) // 100 - 50
        assertTrue(highRatioResult.hardRuleTriggered)
        assertFalse(highRatioResult.verdict == VerdictType.WORTH_IT)
    }

    @Test
    fun `cooldown engine detects backward clock change without accelerating timer`() {
        val created = 1_700_000_000_000L
        val item = CooldownEngine.createVaultItem(
            id = "v1",
            name = "Keyboard",
            price = 300_000L,
            productUrl = "",
            durationMs = CooldownEngine.DURATION_48H_MS,
            nowMs = created
        )
        // Advance forward by 10 hours
        val forward = CooldownEngine.refreshVaultItems(listOf(item), created + 36_000_000L)
        assertFalse(forward.anyClockTamperDetected)
        assertEquals(ItemStatus.WAITING, forward.items.first().status)

        // Move device clock backward by 5 hours
        val backward = CooldownEngine.refreshVaultItems(forward.items, created + 18_000_000L)
        assertTrue(backward.anyClockTamperDetected)
        assertTrue(backward.items.first().clockTamperDetected)
        assertEquals(ItemStatus.WAITING, backward.items.first().status)
        // lastSeenAt must remain at the highest seen timestamp
        assertEquals(created + 36_000_000L, backward.items.first().lastSeenAt)

        // Advance past expiration -> READY
        val expired = CooldownEngine.refreshVaultItems(backward.items, created + CooldownEngine.DURATION_48H_MS)
        assertEquals(ItemStatus.READY, expired.items.first().status)
    }

    @Test
    fun `savings counts only cancelled items and achievements never duplicate`() {
        val items = listOf(
            VaultItem("1", "A", 100_000L, "", 1L, 1L, 2L, 2L, ItemStatus.WAITING),
            VaultItem("2", "B", 200_000L, "", 1L, 1L, 2L, 2L, ItemStatus.READY),
            VaultItem("3", "C", 300_000L, "", 1L, 1L, 2L, 2L, ItemStatus.CANCELLED),
            VaultItem("4", "D", 250_000L, "", 1L, 1L, 2L, 2L, ItemStatus.CANCELLED),
            VaultItem("5", "E", 400_000L, "", 1L, 1L, 2L, 2L, ItemStatus.PURCHASED),
            VaultItem("6", "F", 500_000L, "", 1L, 1L, 2L, 2L, ItemStatus.ARCHIVED)
        )
        val settings = UserSettings(lunchPrice = 15_000L, coffeePrice = 20_000L, internetPrice = 50_000L)
        val summary = SavingsAndAchievementsEngine.calculateSavingsSummary(items, settings)

        assertEquals(550_000L, summary.totalSaved)
        assertEquals(2, summary.cancelledCount)
        assertEquals(275_000L, summary.averageSaved)

        val dayMs = 86_400_000L
        val evals = (0 until 7).map { idx ->
            EvaluationRecord(
                id = "e$idx",
                behavioralScore = 80,
                financialPenalty = 10,
                finalScore = 70,
                answers = listOf(true, true, false, true, true),
                priceRatio = 0.15,
                verdict = VerdictType.RECONSIDER,
                createdAt = 1_700_000_000_000L + idx * dayMs,
                financialSnapshot = FinancialSnapshot(1_200_000.0, 300_000.0, 900_000.0)
            )
        }
        val firstPass = SavingsAndAchievementsEngine.evaluateNewAchievements(
            existing = emptyList(),
            items = items,
            evaluations = evals,
            nowMs = 1_700_600_000_000L,
            timeZone = TimeZone.getTimeZone("UTC")
        )
        val unlockedIds = firstPass.map { it.id }.toSet()
        assertTrue("first_pause" in unlockedIds)
        assertTrue("first_save" in unlockedIds)
        assertTrue("money_guardian" in unlockedIds)
        assertTrue("consistent_mind" in unlockedIds)
        assertFalse("smart_shopper" in unlockedIds) // only 2 cancelled

        // Second pass must NOT duplicate any ID
        val secondPass = SavingsAndAchievementsEngine.evaluateNewAchievements(
            existing = firstPass,
            items = items,
            evaluations = evals,
            nowMs = 1_700_700_000_000L,
            timeZone = TimeZone.getTimeZone("UTC")
        )
        assertEquals(firstPass.size, secondPass.size)
    }

    @Test
    fun `pattern insights require at least 5 evaluations`() {
        val few = listOf(
            EvaluationRecord(
                id = "e1",
                behavioralScore = 60,
                financialPenalty = 0,
                finalScore = 60,
                answers = listOf(true, false, true, true, false),
                priceRatio = 0.04,
                verdict = VerdictType.RECONSIDER,
                createdAt = 1_700_000_000_000L,
                financialSnapshot = FinancialSnapshot(1_000_000.0, 200_000.0, 800_000.0)
            )
        )
        assertFalse(SavingsAndAchievementsEngine.computePatternInsights(few).hasEnoughData)

        val five = (1..5).map { few.first().copy(id = "e$it") }
        assertTrue(SavingsAndAchievementsEngine.computePatternInsights(five).hasEnoughData)
    }

    @Test
    fun `json codec validates PRD schema, rejects corrupt JSON, and merges without duplicate IDs`() {
        val prdSampleJson = """
        {
          "appVersion": "1.0.0",
          "settings": {
            "currency": "IDR",
            "incomeType": "allowance",
            "incomeAmount": 1200000,
            "incomePeriod": "monthly",
            "mandatoryExpenses": 300000,
            "lunchPrice": 15000,
            "coffeePrice": 20000,
            "internetPrice": 50000
          },
          "items": [{
            "id": "item_001",
            "name": "Wireless Earbuds",
            "price": 250000,
            "productUrl": "https://example.com/product",
            "createdAt": 1795200000000,
            "durationMs": 172800000,
            "expiresAt": 1795372800000,
            "lastSeenAt": 1795200000000,
            "status": "waiting"
          }],
          "evaluations": [{
            "id": "eval_001",
            "itemId": "item_001",
            "behavioralScore": 80,
            "financialPenalty": 10,
            "finalScore": 70,
            "answers": [true, true, false, true, true],
            "priceRatio": 0.15,
            "verdict": "reconsider",
            "createdAt": 1795200000000,
            "financialSnapshot": {
              "monthlyIncome": 1200000,
              "monthlyExpenses": 300000,
              "freeFunds": 900000
            }
          }],
          "achievements": [],
          "history": [],
          "metadata": {
            "createdAt": 1795200000000,
            "updatedAt": 1795200000000,
            "lastBackupAt": null
          }
        }
        """.trimIndent()

        val decoded = WorthItJsonCodec.decodeAndValidate(prdSampleJson)
        assertTrue(decoded.isSuccess)
        val data = decoded.getOrThrow()
        assertEquals("Wireless Earbuds", data.items.first().name)
        assertEquals(250_000L, data.evaluations.first().itemPrice)

        // Corrupt JSON rejected
        assertTrue(WorthItJsonCodec.decodeAndValidate("{bad json").isFailure)
        // Dangerous URL rejected
        val badUrlJson = prdSampleJson.replace("https://example.com/product", "javascript:alert(1)")
        assertTrue(WorthItJsonCodec.decodeAndValidate(badUrlJson).isFailure)

        // Merge prevents duplicates
        val current = data.copy(
            items = data.items + VaultItem(
                id = "item_002",
                name = "Stand Laptop",
                price = 120_000L,
                createdAt = 10L,
                durationMs = 86_400_000L,
                expiresAt = 86_400_010L,
                lastSeenAt = 10L,
                status = ItemStatus.CANCELLED
            )
        )
        val merged = WorthItJsonCodec.mergeAppData(current, data, nowMs = 2_000L)
        assertEquals(2, merged.mergedData.items.size)
        assertEquals(0, merged.addedItemsCount)
        assertEquals(2, merged.skippedDuplicatesCount)
    }
}
