package com.genuine.nsplayer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genuine.nsplayer.core.DeviceDetector
import com.genuine.nsplayer.core.DeviceMode
import com.genuine.nsplayer.ui.common.FocusableCard
import com.genuine.nsplayer.ui.theme.*
import com.genuine.nsplayer.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToExtensions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = viewModel.preferences
    val deviceMode = remember { DeviceDetector.getDeviceMode(context) }

    val currentTheme by prefs.themeStyle.collectAsState(initial = "Obsidian")
    val isHardwareAccel by prefs.isHardwareAccel.collectAsState(initial = true)
    val bufferSize by prefs.bufferSize.collectAsState(initial = "Normal")
    val defaultAspect by prefs.defaultAspectRatio.collectAsState(initial = "FIT")
    val isParentalEnabled by prefs.isParentalEnabled.collectAsState(initial = false)

    var showThemeDialog by remember { mutableStateOf(false) }
    var showBufferDialog by remember { mutableStateOf(false) }
    var showAspectDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 90.dp)
    ) {
        item {
            Column {
                Text(
                    text = "SETTINGS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexoraPrimaryAccent,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Preferences & System",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexoraTextPrimary
                )
                Text(
                    text = "Playback engine, appearance, extensions and system parameters",
                    fontSize = 12.sp,
                    color = NexoraTextSecondary
                )
            }
        }

        // Appearance
        item {
            SettingsCategoryHeader("Appearance")
        }

        item {
            SettingItemCard(
                icon = Icons.Default.Palette,
                title = "Cinematic Theme",
                subtitle = currentTheme,
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(getThemePrimary(currentTheme))
                    )
                },
                onClick = { showThemeDialog = true }
            )
        }

        // Playback Engine
        item {
            SettingsCategoryHeader("Playback Engine")
        }

        item {
            SettingItemCard(
                icon = Icons.Default.AspectRatio,
                title = "Default Aspect Ratio",
                subtitle = "Initial scaling mode: $defaultAspect",
                onClick = { showAspectDialog = true }
            )
        }

        item {
            SettingItemCard(
                icon = Icons.Default.Speed,
                title = "Stream Buffer Profile",
                subtitle = "$bufferSize (Buffer latency & stream resilience)",
                onClick = { showBufferDialog = true }
            )
        }

        item {
            FocusableCard(
                onClick = { coroutineScope.launch { prefs.setHardwareAccel(!isHardwareAccel) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(12.dp),
                backgroundColor = NexoraSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NexoraPrimaryAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Memory, contentDescription = null, tint = NexoraPrimaryAccent, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Hardware Acceleration", color = NexoraTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(if (isHardwareAccel) "Enabled (MediaCodec hardware decoding)" else "Disabled (Software decoding fallback)", color = NexoraTextSecondary, fontSize = 12.sp)
                        }
                    }

                    Switch(
                        checked = isHardwareAccel,
                        onCheckedChange = { checked -> coroutineScope.launch { prefs.setHardwareAccel(checked) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NexoraPrimaryAccent,
                            checkedTrackColor = NexoraPrimaryAccent.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        // Extensions Ecosystem
        item {
            SettingsCategoryHeader("Extensions & Plugins")
        }

        item {
            SettingItemCard(
                icon = Icons.Default.Extension,
                title = "Manage Extensions",
                subtitle = "Browse community plugins, add repositories, install and update",
                onClick = onNavigateToExtensions
            )
        }

        // Security & Parental Controls
        item {
            SettingsCategoryHeader("Parental Controls")
        }

        item {
            FocusableCard(
                onClick = { showPinDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(12.dp),
                backgroundColor = NexoraSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = (if (isParentalEnabled) NexoraSuccess else NexoraPrimaryAccent).copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isParentalEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = if (isParentalEnabled) NexoraSuccess else NexoraPrimaryAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Parental PIN Protection", color = NexoraTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(if (isParentalEnabled) "Protected with 4-digit PIN" else "Disabled (Tap to set PIN)", color = NexoraTextSecondary, fontSize = 12.sp)
                        }
                    }

                    Switch(
                        checked = isParentalEnabled,
                        onCheckedChange = { showPinDialog = true },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NexoraPrimaryAccent,
                            checkedTrackColor = NexoraPrimaryAccent.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        // Storage & Data
        item {
            SettingsCategoryHeader("Data & Storage")
        }

        item {
            SettingItemCard(
                icon = Icons.Default.DeleteSweep,
                title = "Clear Watch History",
                subtitle = "Remove saved resume positions and playback records",
                onClick = { showClearHistoryDialog = true }
            )
        }

        // About NEXORA
        item {
            SettingsCategoryHeader("About")
        }

        item {
            SettingItemCard(
                icon = Icons.Default.Info,
                title = "About NEXORA",
                subtitle = "Version 1.0.0 • Your Media. Your Way.",
                onClick = { showAboutDialog = true }
            )
        }
    }

    if (showThemeDialog) {
        SingleSelectDialog(
            title = "Select Theme Style",
            options = listOf("Obsidian", "Midnight Blue", "Slate", "Emerald", "Amber"),
            selectedOption = currentTheme,
            onDismiss = { showThemeDialog = false },
            onSelect = {
                coroutineScope.launch { prefs.setThemeStyle(it) }
                showThemeDialog = false
            }
        )
    }

    if (showAspectDialog) {
        SingleSelectDialog(
            title = "Default Aspect Ratio",
            options = listOf("FIT", "CROP", "STRETCH", "SIXTEEN_NINE", "FOUR_THREE"),
            selectedOption = defaultAspect,
            onDismiss = { showAspectDialog = false },
            onSelect = {
                coroutineScope.launch { prefs.setDefaultAspectRatio(it) }
                showAspectDialog = false
            }
        )
    }

    if (showBufferDialog) {
        SingleSelectDialog(
            title = "Stream Buffer Profile",
            options = listOf("Low Latency (Fast)", "Normal (Balanced)", "Aggressive (High Stability)"),
            selectedOption = bufferSize,
            onDismiss = { showBufferDialog = false },
            onSelect = {
                coroutineScope.launch { prefs.setBufferSize(it) }
                showBufferDialog = false
            }
        )
    }

    if (showPinDialog) {
        PinSetupDialog(
            isEnabled = isParentalEnabled,
            onDismiss = { showPinDialog = false },
            onSetPin = { pin ->
                coroutineScope.launch {
                    prefs.setParentalPin(pin)
                    prefs.setParentalEnabled(pin.isNotEmpty())
                }
                showPinDialog = false
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Playback History", color = NexoraTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear all playback history and resume points?", color = NexoraTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch { viewModel.historyRepo.clearHistory() }
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoraError)
                ) {
                    Text("Clear", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = NexoraTextSecondary)
                }
            },
            containerColor = NexoraSurfaceElevated,
            shape = RoundedCornerShape(14.dp)
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Column {
                    Text("NEXORA", color = NexoraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Your Media. Your Way.", color = NexoraPrimaryAccent, fontSize = 12.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version: 1.0.0 (Production Release)", color = NexoraTextSecondary, fontSize = 13.sp)
                    Text("Architecture: Multi-Architecture (Universal, ARM64, ARMv7, x86_64)", color = NexoraTextSecondary, fontSize = 13.sp)
                    Text("Streaming Core: AndroidX Media3 ExoPlayer 1.4.1", color = NexoraTextSecondary, fontSize = 13.sp)
                    Text("Design: Cinematic Dark Design System", color = NexoraTextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Designed for Android Mobile, Tablets, and Android TV / Google TV.", color = NexoraTextMuted, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoraPrimaryAccent, contentColor = NexoraBackground)
                ) {
                    Text("Close")
                }
            },
            containerColor = NexoraSurfaceElevated,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = NexoraPrimaryAccent,
        letterSpacing = 1.1.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingItemCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = NexoraSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NexoraPrimaryAccent.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = NexoraPrimaryAccent, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, color = NexoraTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(subtitle, color = NexoraTextSecondary, fontSize = 12.sp)
                }
            }

            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NexoraTextMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun SingleSelectDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = NexoraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(option) }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option.equals(selectedOption, ignoreCase = true),
                            onClick = { onSelect(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = NexoraPrimaryAccent)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(option, color = NexoraTextPrimary, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NexoraTextSecondary)
            }
        },
        containerColor = NexoraSurfaceElevated,
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun PinSetupDialog(
    isEnabled: Boolean,
    onDismiss: () -> Unit,
    onSetPin: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEnabled) "Change or Disable PIN" else "Set 4-Digit Parental PIN", color = NexoraTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter a 4-digit numeric code to protect sensitive categories:", color = NexoraTextSecondary, fontSize = 13.sp)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pin = it },
                    label = { Text("4-Digit PIN") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoraPrimaryAccent,
                        unfocusedBorderColor = NexoraBorder,
                        focusedTextColor = NexoraTextPrimary,
                        unfocusedTextColor = NexoraTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSetPin(pin) },
                enabled = pin.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = NexoraPrimaryAccent, contentColor = NexoraBackground)
            ) {
                Text("Save PIN")
            }
        },
        dismissButton = {
            if (isEnabled) {
                TextButton(onClick = { onSetPin("") }) {
                    Text("Disable PIN", color = NexoraError)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = NexoraTextSecondary)
                }
            }
        },
        containerColor = NexoraSurfaceElevated,
        shape = RoundedCornerShape(14.dp)
    )
}
