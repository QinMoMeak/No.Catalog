package com.nocatalog.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(sessionState.requiresLock, currentRoute) {
        if (sessionState.initialized && sessionState.requiresLock && currentRoute != Routes.lock) {
            navController.navigate(Routes.lock) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.lock,
        modifier = modifier,
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
                onSettings = { navController.navigate(Routes.settings) },
                onOpenDetail = { navController.navigate(Routes.detail(it)) },
            )
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
            SettingsScreen(viewModel = viewModel, onBack = navController::popBackStack)
        }
    }
}
