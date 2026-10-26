package com.example.runner.presentation.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.runner.domain.model.AppThemeMode
import com.example.runner.presentation.achievements.AchievementsScreen
import com.example.runner.presentation.detail.DetailScreen
import com.example.runner.presentation.heatmap.HeatmapScreen
import com.example.runner.presentation.history.HistoryScreen
import com.example.runner.presentation.map.MapScreen
import com.example.runner.presentation.permissions.PermissionScreen
import com.example.runner.presentation.result.ResultScreen
import com.example.runner.presentation.settings.SettingsScreen
import com.example.runner.presentation.stats.StatsScreen
import com.example.runner.ui.theme.isAppDark

@Composable
fun AppNavGraph(
    themeMode: AppThemeMode,
    startFromWidget: Boolean,
    runActive: Boolean,
    onConsumedWidgetStart: () -> Unit,
) {
    val context = LocalContext.current
    val hasPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val isMap = route == Routes.MAP
    val showBottomBar = route in setOf(
        Routes.MAP,
        Routes.HISTORY,
        Routes.ACHIEVEMENTS,
        Routes.SETTINGS,
    ) && !runActive
    val darkMap = isAppDark(themeMode)

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    NavItem(
                        selected = route == Routes.MAP,
                        label = "Карта",
                        selectedIcon = Icons.Filled.Map,
                        idleIcon = Icons.Outlined.Map,
                        onClick = { navController.navigateTab(Routes.MAP) },
                    )
                    NavItem(
                        selected = route == Routes.HISTORY,
                        label = "Лента",
                        selectedIcon = Icons.Filled.History,
                        idleIcon = Icons.Outlined.History,
                        onClick = { navController.navigateTab(Routes.HISTORY) },
                    )
                    NavItem(
                        selected = route == Routes.ACHIEVEMENTS,
                        label = "Ачивки",
                        selectedIcon = Icons.Filled.EmojiEvents,
                        idleIcon = Icons.Outlined.EmojiEvents,
                        onClick = { navController.navigateTab(Routes.ACHIEVEMENTS) },
                    )
                    NavItem(
                        selected = route == Routes.SETTINGS,
                        label = "Ещё",
                        selectedIcon = Icons.Filled.Tune,
                        idleIcon = Icons.Outlined.Tune,
                        onClick = { navController.navigateTab(Routes.SETTINGS) },
                    )
                }
            }
        },
    ) { padding ->
        val mapLift = if (showBottomBar) padding.calculateBottomPadding() else 0.dp
        NavHost(
            navController = navController,
            startDestination = if (hasPermission) Routes.MAP else Routes.PERMISSIONS,
            modifier = if (isMap) Modifier else Modifier.padding(padding),
            enterTransition = {
                fadeIn(tween(220)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(280),
                )
            },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = {
                fadeOut(tween(180)) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(260),
                )
            },
        ) {
            composable(Routes.PERMISSIONS) {
                PermissionScreen(onGranted = {
                    navController.navigate(Routes.MAP) {
                        popUpTo(Routes.PERMISSIONS) { inclusive = true }
                    }
                })
            }
            composable(Routes.MAP) {
                MapScreen(
                    viewModel = hiltViewModel(),
                    darkMap = darkMap,
                    startFromWidget = startFromWidget,
                    onConsumedWidgetStart = onConsumedWidgetStart,
                    onOpenHeatmap = { navController.navigate(Routes.HEATMAP) },
                    onFinished = {
                        if (navController.currentDestination?.route != Routes.RESULT) {
                            navController.navigate(Routes.RESULT) {
                                launchSingleTop = true
                            }
                        }
                    },
                    bottomLift = mapLift,
                )
            }
            composable(Routes.RESULT) {
                ResultScreen(
                    viewModel = hiltViewModel(),
                    darkMap = darkMap,
                    onSaved = {
                        navController.navigate(Routes.HISTORY) {
                            popUpTo(Routes.MAP) { inclusive = false }
                        }
                    },
                    onDiscarded = { navController.popBackStack() },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    viewModel = hiltViewModel(),
                    onOpen = { navController.navigate(Routes.detail(it)) },
                    onOpenStats = { navController.navigate(Routes.STATS) },
                    onOpenHeatmap = { navController.navigate(Routes.HEATMAP) },
                )
            }
            composable(Routes.DETAIL) {
                DetailScreen(
                    viewModel = hiltViewModel(),
                    darkMap = darkMap,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.STATS) {
                StatsScreen(
                    viewModel = hiltViewModel(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel = hiltViewModel())
            }
            composable(Routes.HEATMAP) {
                HeatmapScreen(
                    viewModel = hiltViewModel(),
                    darkMap = darkMap,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ACHIEVEMENTS) {
                AchievementsScreen(viewModel = hiltViewModel())
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    idleIcon: ImageVector,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(if (selected) selectedIcon else idleIcon, contentDescription = label) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    )
}

private fun androidx.navigation.NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
