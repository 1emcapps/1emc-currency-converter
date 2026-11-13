package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BitcoinOrange
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.TextLightPrimary
import com.example.ui.theme.TextLightSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashOverlay(
  visible: Boolean,
  onFinished: () -> Unit,
) {
  val scale = remember { Animatable(0.86f) }
  val alpha = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    alpha.animateTo(1f, animationSpec = tween(260, easing = FastOutSlowInEasing))
    scale.animateTo(1f, animationSpec = tween(320, easing = FastOutSlowInEasing))
    delay(420)
    onFinished()
  }

  AnimatedVisibility(
    visible = visible,
    exit = fadeOut(animationSpec = tween(280)),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            colors = listOf(
              Color(0xFF16233E),
              NavyBackground,
              NavyVoid,
            ),
          )
        ),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
          .padding(32.dp)
          .scale(scale.value)
          .alpha(alpha.value),
      ) {
        Box(
          modifier = Modifier
            .size(104.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF0C1325)),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.app_icon_mark_1794537082785),
            contentDescription = stringResource(id = R.string.cd_app_logo),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        }
        Spacer(modifier = Modifier.height(22.dp))
        Text(
          text = "1emc",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = BitcoinOrange,
            letterSpacing = 1.sp,
          ),
        )
        Text(
          text = "Currency Converter",
          style = MaterialTheme.typography.titleLarge.copy(
            color = TextLightPrimary,
            fontWeight = FontWeight.SemiBold,
          ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "150+ Fiat  •  Bitcoin  •  Satoshis",
          style = MaterialTheme.typography.bodyMedium.copy(
            color = TextLightSecondary,
          ),
        )
      }
    }
  }
}
