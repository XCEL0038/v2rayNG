package com.v2ray.ang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.compose.SkyPalette
import com.v2ray.ang.ui.compose.ThemeManager
import com.v2ray.ang.ui.compose.skyPalette
import kotlinx.coroutines.delay
import java.util.Locale

private enum class SkyTab { Home, Tariffs, Profile }

/**
 * SkyVPN: the customer-facing home surface (hero card + mode list + tariffs/profile
 * dialogs + bottom tab bar), matching the reference web mockup (skyvpn-mobile.html).
 * Real connect/disconnect and server info come from [mainViewModel] via [onAction] -
 * only the plan/promo-code simulation in [SkyAppState] is backend-free (see its doc).
 */
@Composable
fun SkyMainRoot(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
    isDark: Boolean,
    onOpenAdvanced: () -> Unit,
) {
    if (!SkyAppState.hasWelcomeName) {
        SkyWelcomeGate(isDark = isDark)
        return
    }

    val palette = skyPalette(isDark)
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf(SkyTab.Home) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { SkyTopBar(palette = palette, isDark = isDark) },
            bottomBar = {
                SkyBottomTabBar(
                    palette = palette,
                    selected = activeTab,
                    onSelect = { tab -> activeTab = tab }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SkyHeroCard(
                    palette = palette,
                    selectedGuid = uiState.selectedGuid,
                    isRunning = uiState.isRunning,
                    onAction = onAction,
                    onOpenTariffs = { activeTab = SkyTab.Tariffs }
                )
                SkyModeListCard(
                    palette = palette,
                    selectedMode = SkyAppState.selectedMode,
                    onSelect = { SkyAppState.selectMode(it) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        if (activeTab == SkyTab.Tariffs) {
            SkyTariffsDialog(palette = palette, onDismiss = { activeTab = SkyTab.Home })
        }
        if (activeTab == SkyTab.Profile) {
            SkyProfileDialog(
                palette = palette,
                onDismiss = { activeTab = SkyTab.Home },
                onOpenAdvanced = {
                    activeTab = SkyTab.Home
                    onOpenAdvanced()
                }
            )
        }
    }
}

@Composable
private fun SkyTopBar(palette: SkyPalette, isDark: Boolean) {
    var showSettings by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.bg)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.lime)
            )
            Text("SkyVPN", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Box {
            IconButton(
                onClick = { showSettings = true },
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.glass)
                    .border(1.dp, palette.glassBorder, RoundedCornerShape(10.dp))
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings_24dp),
                    contentDescription = "Настройки",
                    tint = palette.text2,
                    modifier = Modifier.size(17.dp)
                )
            }

            DropdownMenu(expanded = showSettings, onDismissRequest = { showSettings = false }) {
                Text(
                    text = "Тема",
                    color = palette.text3,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                DropdownMenuItem(
                    text = { Text(if (isDark) "✓  Тёмная" else "Тёмная") },
                    onClick = {
                        ThemeManager.setThemeMode("2")
                        showSettings = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (!isDark) "✓  Светлая" else "Светлая") },
                    onClick = {
                        ThemeManager.setThemeMode("1")
                        showSettings = false
                    }
                )
                Text(
                    text = "Язык",
                    color = palette.text3,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                val isRussian = remember { Locale.getDefault().language.equals("ru", ignoreCase = true) }
                DropdownMenuItem(
                    text = { Text(if (isRussian) "✓  RU" else "RU") },
                    onClick = {
                        AppLocaleManager.setApplicationLanguage("ru")
                        showSettings = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (!isRussian) "✓  EN" else "EN") },
                    onClick = {
                        AppLocaleManager.setApplicationLanguage("en")
                        showSettings = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SkyHeroCard(
    palette: SkyPalette,
    selectedGuid: String?,
    isRunning: Boolean,
    onAction: (MainAction) -> Unit,
    onOpenTariffs: () -> Unit,
) {
    var isConnectingLocal by remember { mutableStateOf(false) }
    LaunchedEffect(isRunning) {
        if (isRunning) isConnectingLocal = false
    }
    LaunchedEffect(isConnectingLocal) {
        if (isConnectingLocal) {
            delay(8000)
            isConnectingLocal = false
        }
    }

    var downMbps by remember { mutableStateOf(0.0) }
    var upMbps by remember { mutableStateOf(0.0) }
    var sessionSeconds by remember { mutableStateOf(0) }
    var totalMb by remember { mutableStateOf(0.0) }

    LaunchedEffect(isRunning) {
        if (!isRunning) {
            downMbps = 0.0
            upMbps = 0.0
            sessionSeconds = 0
            totalMb = 0.0
            return@LaunchedEffect
        }
        sessionSeconds = 0
        totalMb = 0.0
        // Discard the stale delta accumulated before this session started.
        CoreServiceManager.queryAllOutboundTrafficStats()
        var lastQueryAt = System.currentTimeMillis()
        while (true) {
            delay(1000)
            sessionSeconds += 1
            val now = System.currentTimeMillis()
            val elapsedSeconds = ((now - lastQueryAt) / 1000.0).coerceAtLeast(0.5)
            lastQueryAt = now
            var down = 0L
            var up = 0L
            CoreServiceManager.queryAllOutboundTrafficStats().forEach { stat ->
                if (stat.tag != AppConfig.TAG_BLOCKED) {
                    when (stat.direction) {
                        AppConfig.DOWNLINK -> down += stat.value
                        AppConfig.UPLINK -> up += stat.value
                    }
                }
            }
            downMbps = (down / elapsedSeconds) / 1_000_000.0
            upMbps = (up / elapsedSeconds) / 1_000_000.0
            totalMb += (down + up) / 1_000_000.0
        }
    }

    val serverName = remember(selectedGuid) {
        val remarks = selectedGuid?.let { MmkvManager.decodeServerConfig(it)?.remarks }
        if (remarks.isNullOrBlank()) "SkyVPN" else remarks
    }
    val showConnecting = isConnectingLocal && !isRunning
    val statusWord = when {
        showConnecting -> "Подключение"
        isRunning -> "Подключено"
        else -> "Не подключено"
    }
    val statusSub = when {
        showConnecting -> "Устанавливаем соединение — $serverName…"
        isRunning -> "Подключено · $serverName"
        else -> "Ваш трафик виден в этой сети"
    }
    val planLabel = if (SkyAppState.plan.isBlank()) "Бесплатный тариф" else skyPlanInfo(SkyAppState.plan).name
    val hasSub = SkyAppState.plan.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(palette.glass)
            .border(1.dp, palette.glassBorder, RoundedCornerShape(26.dp))
            .padding(vertical = 30.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(palette.glass)
                .border(1.dp, palette.glassBorder, RoundedCornerShape(999.dp))
                .clickable(onClick = onOpenTariffs)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(planLabel, color = palette.text2, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            Text("›", color = palette.text3, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(22.dp))

        Box(modifier = Modifier.size(190.dp), contentAlignment = Alignment.Center) {
            if (showConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A84FF),
                    trackColor = Color(0x260A84FF),
                    strokeWidth = 3.dp
                )
            }
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(52.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = if (isRunning) {
                                listOf(Color(0xFFD9FF5C), Color(0xFFA3E635))
                            } else {
                                listOf(palette.btnIdle1, palette.btnIdle2)
                            }
                        )
                    )
                    .clickable(enabled = !showConnecting) {
                        if (!isRunning) isConnectingLocal = true
                        onAction(MainAction.ToggleService)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        painter = painterResource(if (isRunning) R.drawable.ic_stop_24dp else R.drawable.ic_play_24dp),
                        contentDescription = null,
                        tint = if (isRunning) palette.bg else palette.text2,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = when {
                            showConnecting -> "Подключение…"
                            isRunning -> "Отключиться"
                            else -> "Подключиться"
                        },
                        color = if (isRunning) palette.bg else palette.text2,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        Text(
            text = statusWord,
            color = if (isRunning) palette.lime else palette.text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            text = statusSub,
            color = palette.text2,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.glass)
                .border(1.dp, palette.glassBorder, RoundedCornerShape(14.dp))
                .clickable(enabled = !hasSub, onClick = onOpenTariffs)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (hasSub) "До конца: ${SkyAppState.daysLeft} дн." else "У вас нет подписки!",
                color = palette.text2,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SkyStatTile(
                value = String.format(Locale.US, "%.1f", downMbps),
                label = "МБ/с ↓",
                palette = palette,
                modifier = Modifier.weight(1f)
            )
            SkyStatTile(
                value = String.format(Locale.US, "%.1f", upMbps),
                label = "МБ/с ↑",
                palette = palette,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val minutes = sessionSeconds / 60
            val seconds = sessionSeconds % 60
            SkyStatTile(
                value = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                label = "Сессия",
                palette = palette,
                modifier = Modifier.weight(1f)
            )
            SkyStatTile(
                value = if (totalMb > 1024) {
                    String.format(Locale.US, "%.2f ГБ", totalMb / 1024)
                } else {
                    totalMb.toInt().toString()
                },
                label = "МБ передано",
                palette = palette,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SkyStatTile(value: String, label: String, palette: SkyPalette, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.glass)
            .border(1.dp, palette.glassBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = palette.text, fontSize = 18.sp, fontWeight = FontWeight.Light)
        Text(label, color = palette.text3, fontSize = 10.5.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SkyModeListCard(palette: SkyPalette, selectedMode: Int, onSelect: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(palette.glass)
            .border(1.dp, palette.glassBorder, RoundedCornerShape(26.dp))
    ) {
        Text(
            text = "Режим подключения",
            color = palette.text2,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 6.dp)
        )
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SkyModeRow(
                title = "Зарубежные сервисы",
                desc = "Claude, YouTube, Telegram и др.",
                selected = selectedMode == 0,
                locked = false,
                palette = palette,
                onClick = { onSelect(0) }
            )
            SkyModeRow(
                title = "Обход глушилок",
                desc = "Недоступен",
                selected = false,
                locked = true,
                palette = palette,
                onClick = {}
            )
        }
    }
}

@Composable
private fun SkyModeRow(
    title: String,
    desc: String,
    selected: Boolean,
    locked: Boolean,
    palette: SkyPalette,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) palette.limeSoft else palette.glass)
            .border(
                width = 1.dp,
                color = if (selected) palette.lime.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = !locked, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (selected) palette.lime.copy(alpha = 0.22f) else palette.glassStrong),
            contentAlignment = Alignment.Center
        ) {
            if (locked) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock_24dp),
                    contentDescription = null,
                    tint = palette.text3,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (selected) palette.lime else palette.text3)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (locked) palette.text3 else palette.text,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (locked) "Недоступен" else desc,
                color = if (locked) Color(0xFFFF8A8A) else palette.text3,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (selected) {
            Text("✓", color = palette.lime, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun SkyBottomTabBar(palette: SkyPalette, selected: SkyTab, onSelect: (SkyTab) -> Unit) {
    NavigationBar(containerColor = palette.bg) {
        NavigationBarItem(
            selected = selected == SkyTab.Home,
            onClick = { onSelect(SkyTab.Home) },
            icon = { SkyTabDot(active = selected == SkyTab.Home, palette = palette) },
            label = { Text("Главная") }
        )
        NavigationBarItem(
            selected = selected == SkyTab.Tariffs,
            onClick = { onSelect(SkyTab.Tariffs) },
            icon = { SkyTabDot(active = selected == SkyTab.Tariffs, palette = palette) },
            label = { Text("Тарифы") }
        )
        NavigationBarItem(
            selected = selected == SkyTab.Profile,
            onClick = { onSelect(SkyTab.Profile) },
            icon = { SkyTabDot(active = selected == SkyTab.Profile, palette = palette) },
            label = { Text("Профиль") }
        )
    }
}

@Composable
private fun SkyTabDot(active: Boolean, palette: SkyPalette) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (active) palette.lime else palette.text3)
    )
}

@Composable
private fun SkyTariffsDialog(palette: SkyPalette, onDismiss: () -> Unit) {
    var activePlanId by remember { mutableStateOf(SkyAppState.plan.ifBlank { "plus" }) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(palette.glassStrong)
                .border(1.dp, palette.glassBorder, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Тарифы SkyVPN", color = palette.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "✕",
                    color = palette.text2,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.glass)
                    .border(1.dp, palette.glassBorder, RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("plus" to "Plus", "premium" to "Premium").forEach { (id, label) ->
                    val isSelected = id == activePlanId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) palette.lime else Color.Transparent)
                            .clickable { activePlanId = id }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) palette.bg else palette.text2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val info = skyPlanInfo(activePlanId)
            val isCurrent = activePlanId == SkyAppState.plan
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isCurrent) palette.limeSoft else palette.glass)
                    .border(
                        width = 1.dp,
                        color = if (isCurrent) palette.lime.copy(alpha = 0.5f) else palette.glassBorder,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Text(info.name, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = info.price,
                    color = palette.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Column(
                    modifier = Modifier.padding(top = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    info.features.forEach { feature ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("✓", color = palette.lime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(feature, color = palette.text2, fontSize = 12.sp)
                        }
                    }
                }
                Button(
                    onClick = {
                        if (!isCurrent) {
                            SkyAppState.choosePlan(activePlanId)
                            onDismiss()
                        }
                    },
                    enabled = !isCurrent,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.lime,
                        contentColor = palette.bg,
                        disabledContainerColor = palette.glassStrong,
                        disabledContentColor = palette.text3
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isCurrent) "Текущий план" else "Выбрать", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SkyProfileDialog(palette: SkyPalette, onDismiss: () -> Unit, onOpenAdvanced: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(palette.glassStrong)
                .border(1.dp, palette.glassBorder, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Профиль", color = palette.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "✕",
                    color = palette.text2,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.glass)
                    .border(1.dp, palette.glassBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Подписка:", color = palette.text2, fontSize = 13.sp)
                Text(
                    text = if (SkyAppState.plan.isBlank()) {
                        "Не активна"
                    } else {
                        "Активна · ${skyPlanInfo(SkyAppState.plan).name} (${SkyAppState.daysLeft} дн.)"
                    },
                    color = palette.text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Активировать код", color = palette.text2, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it.uppercase()
                    message = null
                },
                placeholder = { Text("Код", color = palette.text3) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (message != null) {
                Text(
                    text = message.orEmpty(),
                    color = if (isSuccess) palette.lime else Color(0xFFFF8080),
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Button(
                onClick = {
                    when (val result = SkyAppState.redeemCode(code)) {
                        is SkyAppState.RedeemResult.Success -> {
                            message = "Готово: +${result.days} дн. (${skyPlanInfo(result.planId).name})"
                            isSuccess = true
                            code = ""
                        }

                        SkyAppState.RedeemResult.Invalid -> {
                            message = "Неверный код"
                            isSuccess = false
                        }

                        SkyAppState.RedeemResult.AlreadyUsed -> {
                            message = "Вы уже использовали этот код"
                            isSuccess = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.lime, contentColor = palette.bg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text("Применить", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
            TextButton(onClick = onOpenAdvanced) {
                Text("Расширенные настройки (техническое)", color = palette.text3, fontSize = 11.5.sp)
            }
        }
    }
}
