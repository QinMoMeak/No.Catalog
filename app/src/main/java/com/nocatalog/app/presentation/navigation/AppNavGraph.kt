package com.nocatalog.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nocatalog.app.presentation.ui.backup.BackupScreen
import com.nocatalog.app.presentation.ui.backup.BackupViewModel
import com.nocatalog.app.presentation.ui.detail.EntryDetailScreen
import com.nocatalog.app.presentation.ui.detail.EntryDetailViewModel
import com.nocatalog.app.presentation.ui.edit.EditScreen
import com.nocatalog.app.presentation.ui.edit.EditViewModel
import com.nocatalog.app.presentation.ui.home.HomeScreen
import com.nocatalog.app.presentation.ui.home.HomeViewModel
import com.nocatalog.app.presentation.ui.importexport.ImportPreviewScreen
import com.nocatalog.app.presentation.ui.importexport.ImportPreviewViewModel
import com.nocatalog.app.presentation.ui.lock.LockScreen
import com.nocatalog.app.presentation.ui.lock.LockViewModel
import com.nocatalog.app.presentation.ui.stats.StatsScreen
import com.nocatalog.app.presentation.ui.stats.StatsViewModel
import com.nocatalog.app.presentation.ui.settings.SettingsScreen
import com.nocatalog.app.presentation.ui.settings.SettingsViewModel

/**
 * 第一阶段先完成关键页面的导航骨架，业务流在后续阶段逐步接入。
 */
@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val sessionViewModel: AppSessionViewModel = hiltViewModel()
    val sessionState by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringBefore("/")

    LaunchedEffect(sessionState.requiresLock, currentRoute) {
        if (sessionState.initialized && sessionState.requiresLock && currentRoute != Routes.lock) {
            navController.navigate(Routes.lock) {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (currentRoute in Routes.topLevelRoutes) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.home,
                        onClick = {
                            navController.navigate(Routes.home) {
                                popUpTo(Routes.home) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
                        label = { Text("首页") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.stats,
                        onClick = {
                            navController.navigate(Routes.stats) {
                                popUpTo(Routes.home) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "统计") },
                        label = { Text("统计") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.settings,
                        onClick = {
                            navController.navigate(Routes.settings) {
                                popUpTo(Routes.home) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                        label = { Text("设置") },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.lock,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.lock) {
                val viewModel: LockViewModel = hiltViewModel()
                LockScreen(
                    viewModel = viewModel,
                    onUnlocked = {
                        navController.navigate(Routes.home) {
                            popUpTo(Routes.lock) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.home) {
                val viewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = viewModel,
                    onAdd = { navController.navigate(Routes.edit()) },
                    onImportPreview = { navController.navigate(Routes.importPreview) },
                    onBackup = { navController.navigate(Routes.backup) },
                    onOpenDetail = { navController.navigate(Routes.detail(it)) },
                )
            }
            composable(Routes.stats) {
                val viewModel: StatsViewModel = hiltViewModel()
                StatsScreen(viewModel = viewModel)
            }
            composable(
                route = Routes.editPattern,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
            ) {
                val viewModel: EditViewModel = hiltViewModel()
                EditScreen(
                    viewModel = viewModel,
                    onBack = navController::popBackStack,
                )
            }
            composable(
                route = Routes.detailPattern,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
            ) {
                val viewModel: EntryDetailViewModel = hiltViewModel()
                EntryDetailScreen(
                    viewModel = viewModel,
                    onBack = navController::popBackStack,
                    onEdit = { entryId -> navController.navigate(Routes.edit(entryId)) },
                )
            }
            composable(Routes.importPreview) {
                val viewModel: ImportPreviewViewModel = hiltViewModel()
                ImportPreviewScreen(viewModel = viewModel, onBack = navController::popBackStack)
            }
            composable(Routes.backup) {
                val viewModel: BackupViewModel = hiltViewModel()
                BackupScreen(viewModel = viewModel, onBack = navController::popBackStack)
            }
            composable(Routes.settings) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = navController::popBackStack,
                    onOpenImportExport = { navController.navigate(Routes.importPreview) },
                    onOpenBackup = { navController.navigate(Routes.backup) },
                )
            }
        }
    }
}
