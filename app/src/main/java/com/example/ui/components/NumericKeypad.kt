package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun NumericKeypad(
  onDigit: (String) -> Unit,
  onDecimal: () -> Unit,
  onDoubleZero: () -> Unit,
  onBackspace: () -> Unit,
  onClear: () -> Unit,
  onPreset: (String) -> Unit,
  onSwapOrDone: (() -> Unit)? = null,
  swapOrDoneLabel: String? = null,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val presets = listOf("0.01", "1", "10", "100", "1000", "10000")
  val presetLabels = mapOf(
    "0.01" to "0.01",
    "1" to "1",
    "10" to "10",
    "100" to "100",
    "1000" to "1K",
    "10000" to "10K",
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Quick preset pills for effortless one-handed inputs
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      presets.forEach { preset ->
        Box(
          modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onPreset(preset)
            }
            .padding(horizontal = 14.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = presetLabels[preset] ?: preset,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // 4x4 Fintech Keypad Grid
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        KeypadDigitButton("7", Modifier.weight(1f)) { onDigit("7") }
        KeypadDigitButton("8", Modifier.weight(1f)) { onDigit("8") }
        KeypadDigitButton("9", Modifier.weight(1f)) { onDigit("9") }
        KeypadActionButton(
          label = "AC",
          containerColor = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.onErrorContainer,
          testTag = "keypad_clear",
          modifier = Modifier.weight(1f),
          onClick = onClear,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        KeypadDigitButton("4", Modifier.weight(1f)) { onDigit("4") }
        KeypadDigitButton("5", Modifier.weight(1f)) { onDigit("5") }
        KeypadDigitButton("6", Modifier.weight(1f)) { onDigit("6") }
        KeypadIconButton(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          contentColor = MaterialTheme.colorScheme.onSurface,
          testTag = "keypad_backspace",
          modifier = Modifier.weight(1f),
          onClick = onBackspace,
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.Backspace,
            contentDescription = stringResource(id = R.string.cd_backspace),
            modifier = Modifier.size(22.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        KeypadDigitButton("1", Modifier.weight(1f)) { onDigit("1") }
        KeypadDigitButton("2", Modifier.weight(1f)) { onDigit("2") }
        KeypadDigitButton("3", Modifier.weight(1f)) { onDigit("3") }
        KeypadActionButton(
          label = "00",
          containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          contentColor = MaterialTheme.colorScheme.onSurface,
          testTag = "keypad_00",
          modifier = Modifier.weight(1f),
          onClick = onDoubleZero,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        KeypadDigitButton(".", Modifier.weight(1f), testTag = "keypad_decimal") { onDecimal() }
        KeypadDigitButton("0", Modifier.weight(1f)) { onDigit("0") }
        if (onSwapOrDone != null) {
          if (swapOrDoneLabel == null) {
            KeypadIconButton(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              testTag = "keypad_swap",
              modifier = Modifier.weight(2f),
              onClick = onSwapOrDone,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.SwapVert,
                  contentDescription = stringResource(id = R.string.cd_swap_currencies),
                  modifier = Modifier.size(20.dp),
                )
                Text(
                  text = "SWAP",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
              }
            }
          } else {
            KeypadActionButton(
              label = swapOrDoneLabel,
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              testTag = "keypad_done",
              modifier = Modifier.weight(2f),
              onClick = onSwapOrDone,
            )
          }
        } else {
          Spacer(modifier = Modifier.weight(2f))
        }
      }
    }
  }
}

@Composable
private fun KeypadDigitButton(
  label: String,
  modifier: Modifier = Modifier,
  testTag: String = "keypad_$label",
  onClick: () -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  Box(
    modifier = modifier
      .height(54.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(MaterialTheme.colorScheme.surfaceContainer)
      .clickable {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
      }
      .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.titleLarge.copy(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
      ),
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun KeypadActionButton(
  label: String,
  containerColor: Color,
  contentColor: Color,
  testTag: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  Box(
    modifier = modifier
      .height(54.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(containerColor)
      .clickable {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
      }
      .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = contentColor,
    )
  }
}

@Composable
private fun KeypadIconButton(
  containerColor: Color,
  contentColor: Color,
  testTag: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
  content: @Composable () -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  Box(
    modifier = modifier
      .height(54.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(containerColor)
      .clickable {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
      }
      .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    androidx.compose.runtime.CompositionLocalProvider(
      androidx.compose.material3.LocalContentColor provides contentColor
    ) {
      content()
    }
  }
}
