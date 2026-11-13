package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.components.SplashOverlay
import com.example.ui.screens.ConvertScreen
import com.example.ui.screens.MultiCurrencyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CurrencyConverterTheme
import com.example.ui.theme.ThemeMode

const val ROUTE_CONVERT = "convert"
const val ROUTE_MULTI = "multi"
const val ROUTE_SETTINGS = "settings"

private data class TopLevelDestination(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val TopDestinations = listOf(
  TopLevelDestination(
    route = ROUTE_CONVERT,
    labelRes = R.string.nav_convert,
    selectedIcon = Icons.Rounded.CurrencyExchange,
    unselectedIcon = Icons.Outlined.CurrencyExchange,
    testTag = "nav_tab_convert",
  ),
  TopLevelDestination(
    route = ROUTE_MULTI,
    labelRes = R.string.nav_multi,
    selectedIcon = Icons.AutoMirrored.Rounded.ViewList,
    unselectedIcon = Icons.AutoMirrored.Outlined.ViewList,
    testTag = "nav_tab_multi",
  ),
  TopLevelDestination(
    route = ROUTE_SETTINGS,
    labelRes = R.string.nav_settings,
    selectedIcon = Icons.Rounded.Settings,
    unselectedIcon = Icons.Outlined.Settings,
    testTag = "nav_tab_settings",
  ),
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CurrencyConverterApp()
    }
  }
}

@Composable
fun CurrencyConverterApp(
  viewModel: MainViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_CONVERT
  val snackbarHostState = remember { SnackbarHostState() }
  var showSplash by rememberSaveable { mutableStateOf(true) }

  LaunchedEffect(Unit) {
    viewModel.snackbarMessages.collect { message ->
      snackbarHostState.currentSnackbarData?.dismiss()
      snackbarHostState.showSnackbar(message)
    }
  }

  val isDark = when (uiState.preferences.themeMode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
  }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !isDark && !showSplash
        controller.isAppearanceLightNavigationBars = !isDark && !showSplash
      }
    }
  }

  CurrencyConverterTheme(themeMode = uiState.preferences.themeMode) {
    Box(modifier = Modifier.fillMaxSize()) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        val navigateToTab: (String) -> Unit = { route ->
          navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
              saveState = true
            }
            launchSingleTop = true
            restoreState = true
          }
        }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          contentWindowInsets = WindowInsets.safeDrawing,
          containerColor = MaterialTheme.colorScheme.background,
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          bottomBar = {
            if (!useRail) {
              NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
              ) {
                TopDestinations.forEach { dest ->
                  val selected = currentRoute == dest.route
                  NavigationBarItem(
                    selected = selected,
                    onClick = { navigateToTab(dest.route) },
                    icon = {
                      Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = stringResource(id = dest.labelRes),
                      )
                    },
                    label = {
                      Text(
                        text = stringResource(id = dest.labelRes),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                      )
                    },
                    colors = NavigationBarItemDefaults.colors(
                      selectedIconColor = MaterialTheme.colorScheme.primary,
                      selectedTextColor = MaterialTheme.colorScheme.primary,
                      indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                      unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.testTag(dest.testTag),
                  )
                }
              }
            }
          },
        ) { innerPadding ->
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding),
          ) {
            if (useRail) {
              NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
              ) {
                TopDestinations.forEach { dest ->
                  val selected = currentRoute == dest.route
                  NavigationRailItem(
                    selected = selected,
                    onClick = { navigateToTab(dest.route) },
                    icon = {
                      Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = stringResource(id = dest.labelRes),
                      )
                    },
                    label = {
                      Text(
                        text = stringResource(id = dest.labelRes),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                      )
                    },
                    colors = NavigationRailItemDefaults.colors(
                      selectedIconColor = MaterialTheme.colorScheme.primary,
                      selectedTextColor = MaterialTheme.colorScheme.primary,
                      indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                      unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.testTag(dest.testTag),
                  )
                }
              }
            }

            NavHost(
              navController = navController,
              startDestination = ROUTE_CONVERT,
              modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            ) {
              composable(ROUTE_CONVERT) {
                ConvertScreen(
                  uiState = uiState,
                  onDigit = viewModel::onConvertKeyDigit,
                  onDoubleZero = viewModel::onConvertKeyDoubleZero,
                  onDecimal = viewModel::onConvertKeyDecimal,
                  onBackspace = viewModel::onConvertBackspace,
                  onClear = viewModel::onConvertClear,
                  onPreset = viewModel::onConvertPreset,
                  onSwap = viewModel::swapConvertCurrencies,
                  onSelectFrom = viewModel::selectConvertFrom,
                  onSelectTo = viewModel::selectConvertTo,
                  onToggleFavorite = viewModel::toggleFavorite,
                  onRefresh = viewModel::manualRefresh,
                  onCopyResult = viewModel::notifyCopied,
                )
              }

              composable(ROUTE_MULTI) {
                MultiCurrencyScreen(
                  uiState = uiState,
                  onDigit = viewModel::onMultiKeyDigit,
                  onDoubleZero = viewModel::onMultiKeyDoubleZero,
                  onDecimal = viewModel::onMultiKeyDecimal,
                  onBackspace = viewModel::onMultiBackspace,
                  onClear = viewModel::onMultiClear,
                  onPreset = viewModel::onMultiPreset,
                  onSelectBase = viewModel::selectMultiBase,
                  onAddTarget = viewModel::addMultiTarget,
                  onRemoveTarget = viewModel::removeMultiTarget,
                  onMoveTarget = viewModel::moveMultiTarget,
                  onToggleFavorite = viewModel::toggleFavorite,
                  onRefresh = viewModel::manualRefresh,
                  onCopyResult = viewModel::notifyCopied,
                )
              }

              composable(ROUTE_SETTINGS) {
                SettingsScreen(
                  uiState = uiState,
                  onSetDefaultBase = viewModel::setDefaultBaseCurrency,
                  onSetThemeMode = viewModel::setThemeMode,
                  onSetPrecisionMode = viewModel::setPrecisionMode,
                  onSetRefreshInterval = viewModel::setRefreshIntervalMinutes,
                  onToggleFavorite = viewModel::toggleFavorite,
                  onClearCache = viewModel::clearCache,
                )
              }
            }
          }
        }
      }

      SplashOverlay(
        visible = showSplash,
        onFinished = { showSplash = false },
      )
    }
  }
}
