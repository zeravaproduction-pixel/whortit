package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.WorthItRepository
import com.example.domain.CooldownEngine
import com.example.model.IncomePeriod
import com.example.model.IncomeType
import com.example.model.ItemStatus
import com.example.share.ShareCardGenerator
import com.example.ui.WorthItApp
import com.example.ui.theme.WorthItTheme
import com.example.viewmodel.WorthItViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WorthItCujRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var appContext: Application
    private var fakeNowMs: Long = 1_750_000_000_000L

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        appContext.getSharedPreferences(WorthItRepository.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `full CUJ check item, impulse filter, verdict, vault expiry, cancel and savings`() {
        val repository = WorthItRepository(appContext)
        val viewModel = WorthItViewModel(
            application = appContext,
            repository = repository,
            timeProvider = { fakeNowMs },
            startTicker = false
        )

        composeRule.setContent {
            WorthItTheme {
                WorthItApp(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // 1. Dashboard visible with Light UI copy
        composeRule.onNodeWithText("Sebelum checkout, cek dulu.").assertIsDisplayed()

        // 2. Click CTA to Check Item
        composeRule.onNodeWithTag("home_cta_check_item").performClick()
        composeRule.waitForIdle()

        // 3. Enter Item Name & Price
        composeRule.onNodeWithTag("check_item_name_input").performTextInput("Headphone Wireless")
        composeRule.onNodeWithTag("check_item_price_input").performTextInput("250000")

        // 4. Configure inline quick financial profile (1.5M monthly, 500k expenses -> 1M free funds)
        composeRule.onNodeWithTag("quick_profile_income_input").performScrollTo().performTextInput("1500000")
        composeRule.onNodeWithTag("quick_profile_expenses_input").performScrollTo().performTextInput("500000")
        composeRule.onNodeWithTag("quick_profile_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // 5. Proceed to Impulse Filter
        composeRule.onNodeWithTag("check_proceed_filter_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // 6. Answer all 5 questions (Yes, Yes, No, Yes, Yes -> 20+25+0+20+20 = 85; ratio 25% -> penalty -20; final = 65 -> PIKIR ULANG)
        assertEquals(0, viewModel.uiState.value.checkDraft.currentQuestionIndex)
        composeRule.onNodeWithTag("filter_yes_button").performScrollTo().performClick() // Q1 -> Q2
        composeRule.waitForIdle()
        assertEquals(1, viewModel.uiState.value.checkDraft.currentQuestionIndex)
        composeRule.onNodeWithTag("filter_yes_button").performScrollTo().performClick() // Q2 -> Q3
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("filter_no_button").performScrollTo().performClick()  // Q3 -> Q4
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("filter_yes_button").performScrollTo().performClick() // Q4 -> Q5
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("filter_yes_button").performScrollTo().performClick() // Q5 stays on Q5
        composeRule.waitForIdle()
        assertEquals(4, viewModel.uiState.value.checkDraft.currentQuestionIndex)
        assertTrue(viewModel.uiState.value.checkDraft.answers.all { it != null })

        composeRule.onNodeWithTag("filter_submit_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // 7. Verify Verdict screen shows 65 and PIKIR ULANG
        composeRule.onNodeWithTag("verdict_final_score").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("65").assertIsDisplayed()
        composeRule.onNodeWithText("PIKIR ULANG").assertIsDisplayed()

        // 8. Verify Share Card bitmap generation works cleanly at 1080x1350
        val activeEval = viewModel.uiState.value.checkDraft.activeEvaluation!!
        val shareBitmap = ShareCardGenerator.renderShareCardBitmap(activeEval)
        assertEquals(1080, shareBitmap.width)
        assertEquals(1350, shareBitmap.height)

        // 9. Add to Cooldown Vault (48h default)
        composeRule.onNodeWithTag("verdict_add_vault_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        val vaultItem = viewModel.uiState.value.appData.items.first()
        assertEquals(ItemStatus.WAITING, vaultItem.status)
        assertEquals("Headphone Wireless", vaultItem.name)

        // 10. Advance time by 48 hours -> item becomes READY
        fakeNowMs += CooldownEngine.DURATION_48H_MS
        viewModel.tickClock(fakeNowMs)
        composeRule.waitForIdle()
        assertEquals(ItemStatus.READY, viewModel.uiState.value.appData.items.first().status)

        // 11. Navigate to Vault tab and click Batal Beli
        composeRule.onNodeWithText("Buka Cooldown Vault").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("vault_cancel_${vaultItem.id}").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Double-click / repeat call must NOT count cancelled item twice
        viewModel.decideVaultItem(vaultItem.id, true)
        composeRule.waitForIdle()

        // 12. Verify Savings Tracker updated ONCE and Achievements unlocked
        assertEquals(250_000L, viewModel.uiState.value.savingsSummary.totalSaved)
        assertEquals(1, viewModel.uiState.value.savingsSummary.cancelledCount)
        val achIds = viewModel.uiState.value.appData.achievements.map { it.id }.toSet()
        assertTrue("first_pause" in achIds)
        assertTrue("first_save" in achIds)

        // 13. Open Savings tab and verify formatted Rupiah appears
        composeRule.onNodeWithTag("nav_tab_savings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("savings_total_amount").assertIsDisplayed()
    }

    @Test
    fun `export and import replace creates pre-replace backup and preserves integrity`() {
        val repository = WorthItRepository(appContext)
        val viewModel = WorthItViewModel(
            application = appContext,
            repository = repository,
            timeProvider = { fakeNowMs },
            startTicker = false
        )

        viewModel.updateFinancialSettings(
            incomeType = IncomeType.SALARY,
            incomePeriod = IncomePeriod.MONTHLY,
            rawIncomeDigits = "3000000",
            rawExpensesDigits = "1000000",
            rawLunchDigits = "15000",
            rawCoffeeDigits = "20000",
            rawInternetDigits = "50000"
        )
        viewModel.addManualVaultItem("Kamera", "1200000", "https://example.com/cam", CooldownEngine.DURATION_24H_MS)
        val exportedJson = viewModel.buildExportJson()

        // Mutate state then import Replace
        viewModel.addManualVaultItem("Tripod", "200000", "", CooldownEngine.DURATION_48H_MS)
        assertEquals(2, viewModel.uiState.value.appData.items.size)

        val previewRes = viewModel.prepareImportJson(exportedJson)
        assertTrue(previewRes.isSuccess)
        viewModel.confirmImportReplace()

        assertEquals(1, viewModel.uiState.value.appData.items.size)
        assertTrue(viewModel.uiState.value.hasPreReplaceBackup)

        // Restore pre-replace backup -> back to 2 items
        viewModel.restorePreReplaceBackup()
        assertEquals(2, viewModel.uiState.value.appData.items.size)
    }
}
