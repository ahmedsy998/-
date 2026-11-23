package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AddEditTransactionScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.viewmodel.AddEditTransactionViewModel
import com.example.ui.viewmodel.MainViewModel

object MasroofyRoutes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val ADD_EDIT = "add_edit?txId={txId}"
    const val CATEGORIES = "categories"
    const val BUDGETS = "budgets"

    fun addEditRoute(txId: Long? = null): String {
        return if (txId != null && txId > 0L) "add_edit?txId=$txId" else "add_edit?txId=-1"
    }
}

private data class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val bottomTabs = listOf(
    BottomTab(
        route = MasroofyRoutes.HOME,
        label = "الرئيسية",
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home"
    ),
    BottomTab(
        route = MasroofyRoutes.TRANSACTIONS,
        label = "الحركات",
        selectedIcon = Icons.Rounded.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong,
        testTag = "nav_transactions"
    ),
    BottomTab(
        route = MasroofyRoutes.REPORTS,
        label = "التقارير",
        selectedIcon = Icons.Rounded.Assessment,
        unselectedIcon = Icons.Outlined.Assessment,
        testTag = "nav_reports"
    ),
    BottomTab(
        route = MasroofyRoutes.SETTINGS,
        label = "الإعدادات",
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
)

@Composable
fun MasroofyRoot(
    mainViewModel: MainViewModel,
    addEditViewModel: AddEditTransactionViewModel
) {
    val navController = rememberNavController()
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val statusMessage by mainViewModel.statusMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            mainViewModel.clearStatusMessage()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isTopLevelTab = bottomTabs.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
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

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (isTopLevelTab) {
                MasroofyBottomBarWithCenterFab(
                    currentRoute = currentDestination?.route,
                    onTabSelected = { navigateToTab(it) },
                    onAddClick = { navController.navigate(MasroofyRoutes.addEditRoute(null)) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MasroofyRoutes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(MasroofyRoutes.HOME) {
                HomeScreen(
                    uiState = uiState,
                    onAddTransactionClick = { navController.navigate(MasroofyRoutes.addEditRoute(null)) },
                    onEditTransactionClick = { txId -> navController.navigate(MasroofyRoutes.addEditRoute(txId)) },
                    onViewAllTransactions = { navigateToTab(MasroofyRoutes.TRANSACTIONS) },
                    onChartCategoryClick = { catId ->
                        mainViewModel.applyChartCategoryFilter(catId)
                        navigateToTab(MasroofyRoutes.TRANSACTIONS)
                    },
                    onChartDayClick = { date ->
                        mainViewModel.applyChartDayFilter(date)
                        navigateToTab(MasroofyRoutes.TRANSACTIONS)
                    },
                    onOpenBudgetsClick = { navController.navigate(MasroofyRoutes.BUDGETS) },
                    onRetry = { mainViewModel.refreshRecurringOnResume() }
                )
            }

            composable(MasroofyRoutes.TRANSACTIONS) {
                TransactionsScreen(
                    uiState = uiState,
                    onSearchQueryChange = mainViewModel::updateSearchQuery,
                    onTypeFilterChange = mainViewModel::updateTypeFilter,
                    onCategoryFilterChange = mainViewModel::updateCategoryFilter,
                    onPaymentMethodFilterChange = mainViewModel::updatePaymentMethodFilter,
                    onDateRangePresetChange = mainViewModel::updateDateRangePreset,
                    onCustomDateRangeSelected = mainViewModel::updateCustomDateRange,
                    onClearFilters = mainViewModel::clearAllFilters,
                    onEditTransaction = { txId -> navController.navigate(MasroofyRoutes.addEditRoute(txId)) },
                    onDeleteTransaction = mainViewModel::deleteTransaction,
                    onAddTransactionClick = { navController.navigate(MasroofyRoutes.addEditRoute(null)) },
                    onRetry = { mainViewModel.refreshRecurringOnResume() }
                )
            }

            composable(MasroofyRoutes.REPORTS) {
                ReportsScreen(
                    uiState = uiState,
                    onSetMonthly = mainViewModel::setReportMonthly,
                    onShiftPeriod = mainViewModel::shiftReportPeriod,
                    onRetry = { mainViewModel.refreshRecurringOnResume() }
                )
            }

            composable(MasroofyRoutes.SETTINGS) {
                SettingsScreen(
                    uiState = uiState,
                    onThemeModeChange = mainViewModel::setThemeMode,
                    onOpenBudgets = { navController.navigate(MasroofyRoutes.BUDGETS) },
                    onOpenCategories = { navController.navigate(MasroofyRoutes.CATEGORIES) },
                    onGenerateBackupJson = mainViewModel::generateBackupJson,
                    onParseBackup = mainViewModel::parseBackup,
                    onRestoreBackup = mainViewModel::restoreBackup,
                    onShowMessage = mainViewModel::showStatusMessage
                )
            }

            composable(
                route = MasroofyRoutes.ADD_EDIT,
                arguments = listOf(
                    navArgument("txId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val rawId = backStackEntry.arguments?.getLong("txId") ?: -1L
                val txId = if (rawId > 0L) rawId else null
                AddEditTransactionScreen(
                    transactionId = txId,
                    viewModel = addEditViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onManageCategories = { navController.navigate(MasroofyRoutes.CATEGORIES) }
                )
            }

            composable(MasroofyRoutes.CATEGORIES) {
                CategoriesScreen(
                    uiState = uiState,
                    onSaveCategory = mainViewModel::saveCustomCategory,
                    onDeleteCategory = mainViewModel::deleteCustomCategory,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(MasroofyRoutes.BUDGETS) {
                BudgetsScreen(
                    uiState = uiState,
                    onShiftMonth = mainViewModel::shiftBudgetMonth,
                    onSaveBudget = mainViewModel::saveBudget,
                    onDeleteBudget = mainViewModel::deleteBudget,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun MasroofyBottomBarWithCenterFab(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 4.dp,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // First two tabs (Home, Transactions)
                bottomTabs.take(2).forEach { tab ->
                    BottomNavItem(
                        tab = tab,
                        selected = currentRoute == tab.route,
                        onClick = { onTabSelected(tab.route) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Spacer for center FAB
                Box(modifier = Modifier.weight(0.9f))

                // Last two tabs (Reports, Settings)
                bottomTabs.drop(2).forEach { tab ->
                    BottomNavItem(
                        tab = tab,
                        selected = currentRoute == tab.route,
                        onClick = { onTabSelected(tab.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .offset(y = (-22).dp)
                .size(62.dp)
                .testTag("center_add_fab")
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "إضافة حركة جديدة",
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: BottomTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag(tab.testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else androidx.compose.ui.graphics.Color.Transparent
                )
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.label,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
