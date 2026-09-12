package com.v2ray.ang.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.ui.compose.SkyPalette
import com.v2ray.ang.ui.compose.skyPalette
import kotlinx.coroutines.delay

private enum class SkyWelcomePhase { Form, Intro }

/**
 * SkyVPN: first-launch name capture + greeting animation, standing in for the mockup's
 * full register/login screen. There is no backend yet (brief section 5), so this only
 * asks for a display name - nothing is validated against a server or password.
 */
@Composable
fun SkyWelcomeGate(isDark: Boolean) {
    val palette = skyPalette(isDark)
    var phase by remember { mutableStateOf(SkyWelcomePhase.Form) }
    var name by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (phase) {
            SkyWelcomePhase.Form -> SkyWelcomeForm(
                palette = palette,
                name = name,
                onNameChange = { name = it },
                onContinue = { phase = SkyWelcomePhase.Intro }
            )

            SkyWelcomePhase.Intro -> SkyWelcomeIntro(
                palette = palette,
                name = name.trim(),
                onFinished = { SkyAppState.updateWelcomeName(name.trim()) }
            )
        }
    }
}

@Composable
private fun SkyWelcomeForm(
    palette: SkyPalette,
    name: String,
    onNameChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(palette.glass)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(palette.lime)
        )

        Text(
            text = "Добро пожаловать в SkyVPN",
            color = palette.text,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
        )
        Text(
            text = "Введите имя, чтобы продолжить",
            color = palette.text2,
            fontSize = 12.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 22.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                onNameChange(it)
                error = null
            },
            placeholder = { Text("Ваше имя", color = palette.text3) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Text(
                text = error.orEmpty(),
                color = Color(0xFFFF8080),
                fontSize = 11.5.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
        }

        Button(
            onClick = {
                val trimmed = name.trim()
                if (trimmed.isEmpty()) {
                    error = "Введите имя"
                } else {
                    onContinue()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = palette.lime, contentColor = palette.bg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Продолжить", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SkyWelcomeIntro(palette: SkyPalette, name: String, onFinished: () -> Unit) {
    var showWelcome by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(900)
        showWelcome = true
        delay(1700)
        onFinished()
    }

    Box(contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = !showWelcome, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = "SkyVPN",
                color = palette.lime,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        AnimatedVisibility(visible = showWelcome, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = palette.lime)) {
                        append(name)
                    }
                    append(", добро пожаловать!")
                },
                color = palette.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}
