package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.MainUiState
import com.example.ui.MultiCurrencyItemUi
import com.example.ui.components.CurrencyPickerSheet
import com.example.ui.components.NumericKeypad
import com.example.ui.components.RateStatusHeader

private enum class MultiPickerMode { BASE, ADD_TARGET }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiCurrencyScreen(
  uiState: MainUiState,
  onDigit: (String) -> Unit,
  onDoubleZero: () -> Unit,
  onDecimal: () -> Unit,
  onBackspace: () -> Unit,
  onClear: () -> Unit,
  onPreset: (String) -> Unit,
  onSelectBase: (String) -> Unit,
  onAddTarget: (String) -> Unit,
  onRemoveTarget: (String) -> Unit,
  onMoveTarget: (Int, Int) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onRefresh: () -> Unit,
  onCopyResult: (String, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var pickerMode by remember { mutableStateOf<MultiPickerMode?>(null) }
  var showAmountKeypad by remember { mutableStateOf(false) }

  val clipboardManager = LocalClipboardManager.current
  val haptic = LocalHapticFeedback.current

  PullToRefreshBox(
    isRefreshing = uiState.isRefreshing,
    onRefresh = onRefresh,
    modifier = modifier.fillMaxSize(),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 600.dp)
        .align(Alignment.TopCenter),
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        RateStatusHeader(
          title = "Multi-Currency",
          lastUpdatedLabel = uiState.lastUpdatedLabel,
          isRefreshing = uiState.isRefreshing,
          isOfflineBannerVisible = uiState.isOfflineBannerVisible,
          onRefreshClick = onRefresh,
        )

        // Base Amount Card
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
              width = 1.dp,
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
              shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "BASE AMOUNT",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
              ),
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Hold any row to copy • Tap amount to edit",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { pickerMode = MultiPickerMode.BASE }
                .padding(horizontal = 12.dp)
                .testTag("multi_base_selector"),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(text = uiState.multiBaseInfo.flagEmoji, fontSize = 20.sp)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = uiState.multiBaseInfo.code,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Choose base currency",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable { showAmountKeypad = true }
                .padding(horizontal = 12.dp)
                .testTag("multi_amount_editor"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.End,
            ) {
              Text(
                text = uiState.multiFormattedAmountInput,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 24.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = "Edit base amount",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }

        if (uiState.multiItems.isEmpty()) {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(32.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "No target currencies added.\nTap \"Add Currency\" below to compare multiple currencies at once.",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            itemsIndexed(
              items = uiState.multiItems,
              key = { _, item -> item.info.code },
            ) { index, item ->
              MultiCurrencyRowCard(
                item = item,
                canMoveUp = index > 0,
                canMoveDown = index < uiState.multiItems.lastIndex,
                onMoveUp = { onMoveTarget(index, index - 1) },
                onMoveDown = { onMoveTarget(index, index + 1) },
                onRemove = { onRemoveTarget(item.info.code) },
                onLongPress = {
                  if (item.hasRate && item.formattedAmount != "—") {
                    clipboardManager.setText(AnnotatedString(item.formattedAmount))
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCopyResult(item.formattedAmount, item.info.code)
                  }
                },
              )
            }
          }
        }
      }

      ExtendedFloatingActionButton(
        onClick = { pickerMode = MultiPickerMode.ADD_TARGET },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = stringResource(id = R.string.cd_add_currency),
          )
        },
        text = { Text("Add Currency", fontWeight = FontWeight.Bold) },
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(16.dp)
          .testTag("add_multi_currency_fab"),
      )
    }
  }

  pickerMode?.let { mode ->
    CurrencyPickerSheet(
      title = if (mode == MultiPickerMode.BASE) "Select Base Currency" else "Add Target Currency",
      allCurrencies = uiState.availableCurrencies,
      favorites = uiState.preferences.favorites,
      recents = uiState.preferences.recents,
      selectedCode = if (mode == MultiPickerMode.BASE) uiState.multiBaseInfo.code else null,
      excludedCodes = if (mode == MultiPickerMode.ADD_TARGET) {
        uiState.preferences.multiTargets.toSet()
      } else {
        emptySet()
      },
      onSelect = { code ->
        if (mode == MultiPickerMode.BASE) onSelectBase(code) else onAddTarget(code)
      },
      onToggleFavorite = onToggleFavorite,
      onDismiss = { pickerMode = null },
    )
  }

  if (showAmountKeypad) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { showAmountKeypad = false },
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 20.dp),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Base Amount (${uiState.multiBaseInfo.code})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = uiState.multiFormattedAmountInput,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 26.sp),
            color = MaterialTheme.colorScheme.primary,
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        NumericKeypad(
          onDigit = onDigit,
          onDecimal = onDecimal,
          onDoubleZero = onDoubleZero,
          onBackspace = onBackspace,
          onClear = onClear,
          onPreset = onPreset,
          onSwapOrDone = { showAmountKeypad = false },
          swapOrDoneLabel = "DONE",
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MultiCurrencyRowCard(
  item: MultiCurrencyItemUi,
  canMoveUp: Boolean,
  canMoveDown: Boolean,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onRemove: () -> Unit,
  onLongPress: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
        shape = RoundedCornerShape(18.dp),
      )
      .combinedClickable(
        onClick = {},
        onLongClick = onLongPress,
      )
      .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 10.dp)
      .testTag("multi_card_${item.info.code}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
      ) {
        Text(text = item.info.flagEmoji, fontSize = 20.sp)
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.info.code,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = item.info.name,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Text(
        text = item.formattedAmount,
        style = MaterialTheme.typography.titleLarge.copy(
          fontFamily = com.example.ui.theme.JetBrainsMono,
          fontSize = if (item.formattedAmount.length > 13) 16.sp else 20.sp,
          fontWeight = FontWeight.Bold,
        ),
        color = if (item.hasRate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(end = 8.dp),
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = item.unitRateLabel,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onMoveUp,
          enabled = canMoveUp,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("move_up_${item.info.code}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.ArrowUpward,
            contentDescription = stringResource(id = R.string.cd_move_up),
            modifier = Modifier.size(18.dp),
          )
        }
        IconButton(
          onClick = onMoveDown,
          enabled = canMoveDown,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("move_down_${item.info.code}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.ArrowDownward,
            contentDescription = stringResource(id = R.string.cd_move_down),
            modifier = Modifier.size(18.dp),
          )
        }
        IconButton(
          onClick = onRemove,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("remove_${item.info.code}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(id = R.string.cd_remove_currency),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}
