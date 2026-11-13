package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.CurrencyCatalog
import com.example.domain.CurrencyConverter
import com.example.domain.DecimalPrecisionMode
import com.example.ui.theme.NegativeCoral
import com.example.ui.theme.PositiveEmerald
import java.math.BigDecimal
import java.util.Locale

@Composable
fun SparklineCard(
  fromCode: String,
  toCode: String,
  points: List<Double>,
  modifier: Modifier = Modifier,
) {
  val involvesBtcOrSats = (
    fromCode == CurrencyCatalog.CODE_BTC ||
      fromCode == CurrencyCatalog.CODE_SATS ||
      toCode == CurrencyCatalog.CODE_BTC ||
      toCode == CurrencyCatalog.CODE_SATS
    ) && !(
    (fromCode == CurrencyCatalog.CODE_BTC || fromCode == CurrencyCatalog.CODE_SATS) &&
      (toCode == CurrencyCatalog.CODE_BTC || toCode == CurrencyCatalog.CODE_SATS)
    )

  if (points.size < 2) {
    // Graceful compact info pill when historical data is unavailable (e.g. fiat-to-fiat)
    Row(
      modifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerLow)
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant,
          shape = RoundedCornerShape(14.dp),
        )
        .padding(horizontal = 12.dp, vertical = 10.dp)
        .testTag("sparkline_unavailable"),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.ShowChart,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = if (involvesBtcOrSats) {
          "7-day price trend unavailable offline until synced"
        } else {
          "7-day trend sparkline available for BTC & SATS pairs"
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    return
  }

  val first = points.first()
  val last = points.last()
  val minVal = remember(points) { points.minOrNull() ?: 0.0 }
  val maxVal = remember(points) { points.maxOrNull() ?: 0.0 }
  val pctChange = if (first > 0.0) ((last - first) / first) * 100.0 else 0.0
  val isPositive = pctChange >= 0.0
  val trendColor = if (isPositive) PositiveEmerald else NegativeCoral
  val formattedPct = String.format(Locale.US, "%+.2f%%", pctChange)

  val precision = if (toCode == CurrencyCatalog.CODE_BTC) DecimalPrecisionMode.EIGHT
  else DecimalPrecisionMode.AUTO

  val minLabel = CurrencyConverter.formatConvertedAmount(
    BigDecimal.valueOf(minVal),
    toCode,
    precision,
  )
  val maxLabel = CurrencyConverter.formatConvertedAmount(
    BigDecimal.valueOf(maxVal),
    toCode,
    precision,
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
        shape = RoundedCornerShape(16.dp),
      )
      .padding(horizontal = 14.dp, vertical = 10.dp)
      .testTag("sparkline_card"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "7D Trend • $fromCode/$toCode",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(trendColor.copy(alpha = 0.16f))
          .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector = if (isPositive) Icons.AutoMirrored.Rounded.TrendingUp
          else Icons.AutoMirrored.Rounded.TrendingDown,
          contentDescription = null,
          tint = trendColor,
          modifier = Modifier.size(14.dp),
        )
        Text(
          text = formattedPct,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = trendColor,
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
    ) {
      val w = size.width
      val h = size.height
      val verticalPadding = 4.dp.toPx()
      val usableHeight = (h - verticalPadding * 2).coerceAtLeast(1f)
      val range = (maxVal - minVal).takeIf { it > 0.0 } ?: 1.0

      val coords = points.mapIndexed { index, value ->
        val x = (index.toFloat() / (points.size - 1).coerceAtLeast(1)) * w
        val normalizedY = ((value - minVal) / range).toFloat()
        val y = verticalPadding + usableHeight * (1f - normalizedY)
        Offset(x, y)
      }

      val linePath = Path().apply {
        coords.forEachIndexed { idx, pt ->
          if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
        }
      }

      val fillPath = Path().apply {
        addPath(linePath)
        lineTo(coords.last().x, h)
        lineTo(coords.first().x, h)
        close()
      }

      drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
          colors = listOf(
            trendColor.copy(alpha = 0.28f),
            Color.Transparent,
          ),
          startY = 0f,
          endY = h,
        ),
      )

      drawPath(
        path = linePath,
        color = trendColor,
        style = Stroke(
          width = 2.2.dp.toPx(),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round,
        ),
      )

      val lastPt = coords.last()
      drawCircle(
        color = trendColor.copy(alpha = 0.28f),
        radius = 6.dp.toPx(),
        center = lastPt,
      )
      drawCircle(
        color = trendColor,
        radius = 3.dp.toPx(),
        center = lastPt,
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Low: $minLabel",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = "High: $maxLabel",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
