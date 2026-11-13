package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.CurrencyInfo
import com.example.ui.MainUiState
import com.example.ui.components.CurrencyPickerSheet
import com.example.ui.components.NumericKeypad
import com.example.ui.components.RateStatusHeader
import com.example.ui.components.SparklineCard

private enum class PickerTarget { FROM, TO }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConvertScreen(
  uiState: MainUiState,
  onDigit: (String) -> Unit,
  onDoubleZero: () -> Unit,
  onDecimal: () -> Unit,
  onBackspace: () -> Unit,
  onClear: () -> Unit,
  onPreset: (String) -> Unit,
  onSwap: () -> Unit,
  onSelectFrom: (String) -> Unit,
  onSelectTo: (String) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onRefresh: () -> Unit,
  onCopyResult: (String, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }
  val clipboardManager = LocalClipboardManager.current
  val haptic = LocalHapticFeedback.current

  val swapRotation by animateFloatAsState(
    targetValue = uiState.swapCounter * 180f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "swap_rotation",
  )

  PullToRefreshBox(
    isRefreshing = uiState.isRefreshing,
    onRefresh = onRefresh,
    modifier = modifier.fillMaxSize(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 600.dp)
        .align(Alignment.TopCenter),
    ) {
      RateStatusHeader(
        title = "1emc Converter",
        lastUpdatedLabel = uiState.lastUpdatedLabel,
        isRefreshing = uiState.isRefreshing,
        isOfflineBannerVisible = uiState.isOfflineBannerVisible,
        onRefreshClick = onRefresh,
      )

      when {
        uiState.isInitialLoading -> {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
              Text(
                text = "Loading live fiat & Bitcoin rates...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        uiState.errorMessage != null && !uiState.hasRates -> {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
              )
              Text(
                text = "Unable to load exchange rates",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
              )
              Text(
                text = uiState.errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
              Button(
                onClick = onRefresh,
                modifier = Modifier.testTag("retry_rates_button"),
              ) {
                Text("Retry")
              }
            }
          }
        }

        else -> {
          Column(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            // Overlapping From / To Cards with floating circular Swap button
            Box(modifier = Modifier.fillMaxWidth()) {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CurrencyAmountCard(
                  label = "FROM",
                  currency = uiState.fromInfo,
                  amountText = uiState.formattedAmountInput,
                  isTarget = false,
                  onPickCurrency = { pickerTarget = PickerTarget.FROM },
                  onLongPressValue = {
                    val copyText = uiState.formattedAmountInput
                    clipboardManager.setText(AnnotatedString(copyText))
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCopyResult(copyText, uiState.fromInfo.code)
                  },
                  modifier = Modifier.testTag("from_currency_card"),
                )

                CurrencyAmountCard(
                  label = "TO (HOLD TO COPY)",
                  currency = uiState.toInfo,
                  amountText = uiState.convertedDisplay,
                  isTarget = true,
                  isMissingRate = uiState.hasMissingRate,
                  onPickCurrency = { pickerTarget = PickerTarget.TO },
                  onLongPressValue = {
                    if (!uiState.hasMissingRate && uiState.convertedDisplay != "—") {
                      clipboardManager.setText(AnnotatedString(uiState.convertedDisplay))
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      onCopyResult(uiState.convertedDisplay, uiState.toInfo.code)
                    }
                  },
                  modifier = Modifier.testTag("to_currency_card"),
                )
              }

              // Floating Swap Button anchored between cards
              Surface(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  onSwap()
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 6.dp,
                modifier = Modifier
                  .align(Alignment.Center)
                  .size(48.dp)
                  .shadow(6.dp, CircleShape)
                  .testTag("swap_currencies_button"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.SwapVert,
                    contentDescription = stringResource(id = R.string.cd_swap_currencies),
                    modifier = Modifier
                      .size(24.dp)
                      .rotate(swapRotation),
                  )
                }
              }
            }

            // Unit rate pill
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = uiState.unitRateSummary,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              if (!uiState.hasMissingRate && uiState.convertedDisplay != "—") {
                Row(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      clipboardManager.setText(AnnotatedString(uiState.convertedDisplay))
                      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      onCopyResult(uiState.convertedDisplay, uiState.toInfo.code)
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Copy converted result",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp),
                  )
                  Text(
                    text = "Copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }

            // 7-day Sparkline Card
            SparklineCard(
              fromCode = uiState.fromInfo.code,
              toCode = uiState.toInfo.code,
              points = uiState.sparklinePoints,
            )

            Spacer(modifier = Modifier.height(2.dp))
          }

          // Docked custom numeric keypad for one-handed thumb operation
          NumericKeypad(
            onDigit = onDigit,
            onDecimal = onDecimal,
            onDoubleZero = onDoubleZero,
            onBackspace = onBackspace,
            onClear = onClear,
            onPreset = onPreset,
            onSwapOrDone = onSwap,
            modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
          )
        }
      }
    }
  }

  pickerTarget?.let { target ->
    CurrencyPickerSheet(
      title = if (target == PickerTarget.FROM) "Select Base Currency" else "Select Target Currency",
      allCurrencies = uiState.availableCurrencies,
      favorites = uiState.preferences.favorites,
      recents = uiState.preferences.recents,
      selectedCode = if (target == PickerTarget.FROM) uiState.fromInfo.code else uiState.toInfo.code,
      onSelect = { code ->
        if (target == PickerTarget.FROM) onSelectFrom(code) else onSelectTo(code)
      },
      onToggleFavorite = onToggleFavorite,
      onDismiss = { pickerTarget = null },
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CurrencyAmountCard(
  label: String,
  currency: CurrencyInfo,
  amountText: String,
  isTarget: Boolean,
  isMissingRate: Boolean = false,
  onPickCurrency: () -> Unit,
  onLongPressValue: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val containerColor = if (isTarget) {
    MaterialTheme.colorScheme.surfaceContainer
  } else {
    MaterialTheme.colorScheme.surface
  }

  val borderColor = if (isTarget) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
  } else {
    MaterialTheme.colorScheme.outlineVariant
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(containerColor)
      .border(1.dp, borderColor, RoundedCornerShape(20.dp))
      .combinedClickable(
        onClick = {},
        onLongClick = onLongPressValue,
      )
      .padding(horizontal = 16.dp, vertical = 14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          letterSpacing = 0.8.sp,
          fontWeight = FontWeight.Bold,
        ),
        color = if (isTarget) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = currency.name,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 12.dp),
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      // Currency Selector Pill (>= 48dp touch target)
      Row(
        modifier = Modifier
          .height(48.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHigh)
          .clickable(onClick = onPickCurrency)
          .padding(horizontal = 12.dp)
          .testTag(if (isTarget) "to_currency_selector" else "from_currency_selector"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = currency.flagEmoji,
          fontSize = 20.sp,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = currency.code,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
          imageVector = Icons.Rounded.KeyboardArrowDown,
          contentDescription = "Choose currency",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Amount Display
      val fontSize = when {
        amountText.length > 16 -> 20.sp
        amountText.length > 12 -> 24.sp
        amountText.length > 9 -> 28.sp
        else -> 32.sp
      }

      AnimatedContent(
        targetState = amountText,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "amount_anim",
        modifier = Modifier.weight(1f),
      ) { animatedValue ->
        Text(
          text = animatedValue,
          style = MaterialTheme.typography.displayMedium.copy(
            fontSize = fontSize,
            lineHeight = fontSize * 1.15f,
          ),
          color = when {
            isMissingRate -> MaterialTheme.colorScheme.error
            isTarget -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
          },
          textAlign = TextAlign.End,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isTarget) "converted_amount_text" else "input_amount_text"),
        )
      }
    }
  }
}
