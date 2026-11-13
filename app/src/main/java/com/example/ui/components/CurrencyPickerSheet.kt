package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.CurrencyInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerSheet(
  title: String,
  allCurrencies: List<CurrencyInfo>,
  favorites: Set<String>,
  recents: List<String>,
  selectedCode: String?,
  excludedCodes: Set<String> = emptySet(),
  onSelect: (String) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var query by remember { mutableStateOf("") }

  val filteredAll = remember(allCurrencies, query, excludedCodes) {
    val q = query.trim().lowercase()
    allCurrencies.filter { info ->
      info.code !in excludedCodes && (
        q.isEmpty() ||
          info.code.lowercase().contains(q) ||
          info.name.lowercase().contains(q)
        )
    }
  }

  val pinnedList = remember(filteredAll, favorites) {
    filteredAll.filter { it.code in favorites }
  }

  val recentList = remember(filteredAll, recents, favorites) {
    val byCode = filteredAll.associateBy { it.code }
    recents.mapNotNull { code ->
      if (code in favorites) null else byCode[code]
    }
  }

  val remainingList = remember(filteredAll, favorites, recentList) {
    val shownAbove = favorites + recentList.map { it.code }.toSet()
    filteredAll.filter { it.code !in shownAbove }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.88f)
        .padding(horizontal = 16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
        )
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("close_currency_picker"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(id = R.string.cd_close_sheet),
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("currency_search_input"),
        placeholder = { Text("Search code or name (e.g. BTC, EUR, Yen)") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
          )
        },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { query = "" }) {
              Icon(imageVector = Icons.Rounded.Close, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
      )

      Spacer(modifier = Modifier.height(10.dp))

      if (filteredAll.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "No matching currencies for \"$query\"",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
          contentPadding = PaddingValues(bottom = 24.dp),
        ) {
          if (pinnedList.isNotEmpty()) {
            item(key = "header_pinned") {
              SectionHeader("Pinned Favorites")
            }
            items(pinnedList, key = { "pin_${it.code}" }) { info ->
              CurrencyPickerRow(
                info = info,
                isFavorite = true,
                isSelected = info.code == selectedCode,
                onClick = {
                  onSelect(info.code)
                  onDismiss()
                },
                onToggleFavorite = { onToggleFavorite(info.code) },
              )
            }
          }

          if (recentList.isNotEmpty()) {
            item(key = "header_recent") {
              SectionHeader("Recently Used")
            }
            items(recentList, key = { "rec_${it.code}" }) { info ->
              CurrencyPickerRow(
                info = info,
                isFavorite = info.code in favorites,
                isSelected = info.code == selectedCode,
                onClick = {
                  onSelect(info.code)
                  onDismiss()
                },
                onToggleFavorite = { onToggleFavorite(info.code) },
              )
            }
          }

          if (remainingList.isNotEmpty()) {
            item(key = "header_all") {
              SectionHeader(
                if (pinnedList.isEmpty() && recentList.isEmpty()) "Currencies"
                else "All Currencies (${remainingList.size})"
              )
            }
            items(remainingList, key = { "all_${it.code}" }) { info ->
              CurrencyPickerRow(
                info = info,
                isFavorite = info.code in favorites,
                isSelected = info.code == selectedCode,
                onClick = {
                  onSelect(info.code)
                  onDismiss()
                },
                onToggleFavorite = { onToggleFavorite(info.code) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
    text = title.uppercase(),
    style = MaterialTheme.typography.labelMedium.copy(
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.8.sp,
    ),
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp, start = 4.dp),
  )
}

@Composable
private fun CurrencyPickerRow(
  info: CurrencyInfo,
  isFavorite: Boolean,
  isSelected: Boolean,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 10.dp)
      .testTag("picker_row_${info.code}"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(
          if (info.isCrypto) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
          else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = info.flagEmoji,
        fontSize = 20.sp,
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = info.code,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (info.isCrypto) {
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "BITCOIN",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
              .padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      }
      Text(
        text = info.name,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    if (isSelected) {
      Icon(
        imageVector = Icons.Rounded.Check,
        contentDescription = "Selected",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(end = 4.dp),
      )
    }

    IconButton(
      onClick = onToggleFavorite,
      modifier = Modifier
        .minimumInteractiveComponentSize()
        .testTag("favorite_toggle_${info.code}"),
    ) {
      Icon(
        imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
        contentDescription = stringResource(id = R.string.cd_toggle_favorite),
        tint = if (isFavorite) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
