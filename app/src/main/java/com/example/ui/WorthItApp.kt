package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.Formatters
import com.example.domain.UrlValidationResult
import com.example.ui.components.LoadingStateView
import com.example.ui.components.StorageErrorBanner
import com.example.ui.screens.CheckItemScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImpulseFilterScreen
import com.example.ui.screens.SavingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.screens.VerdictScreen
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.PaleSage
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmGray
import com.example.viewmodel.UiEvent
import com.example.viewmodel.WorthItViewModel
import kotlinx.coroutines.launch

object WorthItRoutes {
    const val HOME = "home"
    const val VAULT = "vault"
    const val SAVINGS = "savings"
    const val SETTINGS = "settings"
    const val CHECK_ITEM = "check_item"
    const val IMPULSE_FILTER = "impulse_filter"
    const val VERDICT = "verdict"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val MAIN_TABS = listOf(
    NavTab(WorthItRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    NavTab(WorthItRoutes.VAULT, "Vault", Icons.Filled.LockClock, Icons.Outlined.LockClock),
    NavTab(WorthItRoutes.SAVINGS, "Savings", Icons.Filled.Savings, Icons.Outlined.Savings),
    NavTab(WorthItRoutes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun WorthItApp(
    viewModel: WorthItViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun openExternalUrlSafely(url: String) {
        val validated = Formatters.validateProductUrl(url)
        if (validated is UrlValidationResult.Valid && validated.url.isNotBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validated.url))
                context.startActivity(intent)
            } catch (_: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Tidak ada browser yang tersedia untuk membuka link ini.")
                }
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Link produk tidak valid.")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.AchievementUnlocked -> {
                    snackbarHostState.showSnackbar(
                        "Lencana Baru Tercapai: ${event.achievement.title} — ${event.achievement.description}"
                    )
                }
                is UiEvent.OpenExternalUrl -> openExternalUrlSafely(event.url)
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: WorthItRoutes.HOME
    val showMainNav = currentRoute in MAIN_TABS.map { it.route }

    LaunchedEffect(currentRoute) {
        snackbarHostState.currentSnackbarData?.dismiss()
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = WarmCream,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = if (showMainNav) WindowInsets.navigationBars else WindowInsets(0),
            bottomBar = {
                if (showMainNav && !useNavigationRail) {
                    NavigationBar(
                        containerColor = SoftWhite,
                        contentColor = DeepCharcoal
                    ) {
                        MAIN_TABS.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SageGreen,
                                    selectedTextColor = SageGreen,
                                    indicatorColor = PaleSage,
                                    unselectedIconColor = WarmGray,
                                    unselectedTextColor = WarmGray
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showMainNav && useNavigationRail) {
                    NavigationRail(
                        containerColor = SoftWhite,
                        contentColor = DeepCharcoal,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        MAIN_TABS.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = SageGreen,
                                    selectedTextColor = SageGreen,
                                    indicatorColor = PaleSage,
                                    unselectedIconColor = WarmGray,
                                    unselectedTextColor = WarmGray
                                ),
                                modifier = Modifier.testTag("nav_rail_${tab.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
                    ) {
                        state.storageErrorMessage?.let { err ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                StorageErrorBanner(
                                    message = err,
                                    onExportClick = { navigateToTab(WorthItRoutes.SETTINGS) },
                                    onDismiss = { viewModel.dismissStorageError() }
                                )
                            }
                        }

                        if (state.isLoading) {
                            LoadingStateView()
                        } else {
                            NavHost(
                                navController = navController,
                                startDestination = WorthItRoutes.HOME,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(WorthItRoutes.HOME) {
                                    HomeScreen(
                                        state = state,
                                        onStartCheckItem = {
                                            viewModel.startNewCheck()
                                            navController.navigate(WorthItRoutes.CHECK_ITEM)
                                        },
                                        onNavigateToVault = { navigateToTab(WorthItRoutes.VAULT) },
                                        onNavigateToSavings = { navigateToTab(WorthItRoutes.SAVINGS) },
                                        onNavigateToSettings = { navigateToTab(WorthItRoutes.SETTINGS) }
                                    )
                                }

                                composable(WorthItRoutes.VAULT) {
                                    VaultScreen(
                                        state = state,
                                        onAddManualVaultItem = { n, p, u, d ->
                                            viewModel.addManualVaultItem(n, p, u, d)
                                        },
                                        onDecideVaultItem = { id, cancel ->
                                            viewModel.decideVaultItem(id, cancel)
                                        },
                                        onArchiveVaultItem = { id ->
                                            viewModel.archiveVaultItem(id)
                                        },
                                        onOpenProductUrl = { url -> openExternalUrlSafely(url) },
                                        onStartCheckItem = {
                                            viewModel.startNewCheck()
                                            navController.navigate(WorthItRoutes.CHECK_ITEM)
                                        }
                                    )
                                }

                                composable(WorthItRoutes.SAVINGS) {
                                    SavingsScreen(
                                        state = state,
                                        onStartCheckItem = {
                                            viewModel.startNewCheck()
                                            navController.navigate(WorthItRoutes.CHECK_ITEM)
                                        }
                                    )
                                }

                                composable(WorthItRoutes.SETTINGS) {
                                    SettingsScreen(
                                        state = state,
                                        onSaveSettings = { type, period, inc, exp, l, c, i, d, h ->
                                            viewModel.updateFinancialSettings(type, period, inc, exp, l, c, i, d, h)
                                        },
                                        onBuildExportJson = { viewModel.buildExportJson() },
                                        onPrepareImportJson = { raw -> viewModel.prepareImportJson(raw) },
                                        onDismissImportPreview = { viewModel.dismissImportPreview() },
                                        onConfirmImportReplace = { viewModel.confirmImportReplace() },
                                        onConfirmImportMerge = { viewModel.confirmImportMerge() },
                                        onRestorePreReplaceBackup = { viewModel.restorePreReplaceBackup() },
                                        onDeleteAllData = { viewModel.deleteAllData() },
                                        onShowMessage = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    )
                                }

                                composable(WorthItRoutes.CHECK_ITEM) {
                                    CheckItemScreen(
                                        state = state,
                                        onNameChange = { viewModel.updateCheckName(it) },
                                        onPriceChange = { viewModel.updateCheckPrice(it) },
                                        onUrlChange = { viewModel.updateCheckUrl(it) },
                                        onSaveQuickProfile = { type, period, inc, exp, l, c, i, d, h ->
                                            viewModel.updateFinancialSettings(type, period, inc, exp, l, c, i, d, h)
                                        },
                                        onUpdateComparators = { l, c, i, d, h ->
                                            viewModel.updateComparatorsQuick(l, c, i, d, h)
                                        },
                                        onProceedToFilter = {
                                            if (viewModel.validateAndStartImpulseFilter()) {
                                                navController.navigate(WorthItRoutes.IMPULSE_FILTER)
                                            }
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(WorthItRoutes.IMPULSE_FILTER) {
                                    ImpulseFilterScreen(
                                        state = state,
                                        onAnswerQuestion = { idx, ans ->
                                            viewModel.answerImpulseQuestion(idx, ans)
                                        },
                                        onGoToQuestion = { idx ->
                                            viewModel.goToImpulseQuestion(idx)
                                        },
                                        onSubmitFilter = {
                                            if (viewModel.submitImpulseFilter()) {
                                                navController.navigate(WorthItRoutes.VERDICT)
                                            }
                                        },
                                        onBackToCheckItem = {
                                            viewModel.setFramingActive(false)
                                            navController.popBackStack()
                                        }
                                    )
                                }

                                composable(WorthItRoutes.VERDICT) {
                                    VerdictScreen(
                                        state = state,
                                        onAddToVault = { dur, url ->
                                            viewModel.addVerdictToVault(dur, url)
                                        },
                                        onCancelDirectly = {
                                            viewModel.cancelDirectlyFromVerdict()
                                        },
                                        onShowMessage = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        },
                                        onOpenVault = {
                                            navController.navigate(WorthItRoutes.VAULT) {
                                                popUpTo(WorthItRoutes.HOME) { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        },
                                        onDoneToHome = {
                                            navController.popBackStack(WorthItRoutes.HOME, inclusive = false)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
