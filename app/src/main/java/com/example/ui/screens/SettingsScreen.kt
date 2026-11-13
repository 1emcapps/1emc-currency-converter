package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CurrencyCatalog
import com.example.domain.DecimalPrecisionMode
import com.example.ui.MainUiState
import com.example.ui.components.CurrencyPickerSheet
import com.example.ui.theme.ThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  uiState: MainUiState,
  onSetDefaultBase: (String) -> Unit,
  onSetThemeMode: (ThemeMode) -> Unit,
  onSetPrecisionMode: (DecimalPrecisionMode) -> Unit,
  onSetRefreshInterval: (Int) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onClearCache: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showBasePicker by remember { mutableStateOf(false) }
  var showClearConfirm by remember { mutableStateOf(false) }

  val defaultBaseInfo = remember(uiState.preferences.defaultBaseCurrency) {
    CurrencyCatalog.getInfo(uiState.preferences.defaultBaseCurrency)
  }

  val refreshOptions = listOf(
    1 to "1 min",
    5 to "5 min",
    15 to "15 min",
    30 to "30 min",
    0 to "Manual",
  )

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 600.dp)
        .align(Alignment.TopCenter)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = "Settings",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )

      // Default Base Currency
      SettingsCard(
        icon = Icons.Rounded.CurrencyExchange,
        title = "Default Base Currency",
        subtitle = "Used for initial conversions and multi-currency defaults",
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { showBasePicker = true }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("settings_default_base"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = defaultBaseInfo.flagEmoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = defaultBaseInfo.code,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              )
              Text(
                text = defaultBaseInfo.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = "Change default base currency",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Theme Selector
      SettingsCard(
        icon = Icons.Rounded.Brightness6,
        title = "Appearance",
        subtitle = "Dark navy fintech theme is default; switch anytime",
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ThemeMode.entries.forEach { mode ->
            val selected = uiState.preferences.themeMode == mode
            FilterChip(
              selected = selected,
              onClick = { onSetThemeMode(mode) },
              label = { Text(mode.label, fontWeight = FontWeight.SemiBold) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("theme_chip_${mode.name}"),
            )
          }
        }
      }

      // Decimal Precision
      SettingsCard(
        icon = Icons.Rounded.PrecisionManufacturing,
        title = "Decimal Precision",
        subtitle = "Customize rounding across fiat and Bitcoin displays",
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          DecimalPrecisionMode.entries.forEach { mode ->
            val selected = uiState.preferences.precisionMode == mode
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onSetPrecisionMode(mode) }
                .padding(vertical = 6.dp, horizontal = 4.dp)
                .testTag("precision_option_${mode.name}"),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = selected,
                onClick = { onSetPrecisionMode(mode) },
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text(
                  text = mode.label,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                  text = mode.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }

      // Auto-Refresh Interval
      SettingsCard(
        icon = Icons.Rounded.Sync,
        title = "Auto-Refresh Interval",
        subtitle = "Automatically refreshes rates while the app is open",
      ) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          refreshOptions.forEach { (minutes, label) ->
            val selected = uiState.preferences.refreshIntervalMinutes == minutes
            FilterChip(
              selected = selected,
              onClick = { onSetRefreshInterval(minutes) },
              label = { Text(label, fontWeight = FontWeight.SemiBold) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              ),
              modifier = Modifier.testTag("refresh_interval_$minutes"),
            )
          }
        }
      }

      // Cache Management
      SettingsCard(
        icon = Icons.Rounded.DeleteSweep,
        title = "Offline Cache",
        subtitle = "${uiState.ratesPerUsd.size} currencies cached • Last updated: ${uiState.lastUpdatedLabel}",
      ) {
        OutlinedButton(
          onClick = { showClearConfirm = true },
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("clear_cache_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteSweep,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Clear Cached Rates", fontWeight = FontWeight.SemiBold)
        }
      }

      // Attribution & Privacy
      SettingsCard(
        icon = Icons.Rounded.Info,
        title = "1emc Currency Converter",
        subtitle = "Private by design — zero ads, zero login, zero tracking",
      ) {
        Text(
          text = "Fiat rates: open.er-api.com (USD pivot)\nBitcoin & 7-day history: CoinGecko Public API",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 18.sp,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }

  if (showBasePicker) {
    CurrencyPickerSheet(
      title = "Default Base Currency",
      allCurrencies = uiState.availableCurrencies,
      favorites = uiState.preferences.favorites,
      recents = uiState.preferences.recents,
      selectedCode = uiState.preferences.defaultBaseCurrency,
      onSelect = onSetDefaultBase,
      onToggleFavorite = onToggleFavorite,
      onDismiss = { showBasePicker = false },
    )
  }

  if (showClearConfirm) {
    AlertDialog(
      onDismissRequest = { showClearConfirm = false },
      title = { Text("Clear cached rates?") },
      text = {
        Text("This removes locally saved exchange rates and 7-day BTC history. You can pull down to refresh anytime when online.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            showClearConfirm = false
            onClearCache()
          },
          modifier = Modifier.testTag("confirm_clear_cache"),
        ) {
          Text("Clear Cache", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirm = false }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun SettingsCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  content: @Composable () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
        shape = RoundedCornerShape(20.dp),
      )
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    content()
  }
}
