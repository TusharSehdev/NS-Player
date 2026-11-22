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
import com.genuine.nsplayer.core.PreferencesManager
import com.genuine.nsplayer.ui.common.FocusableCard
import com.genuine.nsplayer.ui.theme.*
import com.genuine.nsplayer.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 90.dp)
    ) {
        item {
            Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NSTextPrimary)
            Text("Preferences, themes, video decoders and parental controls", fontSize = 12.sp, color = NSTextSecondary)
        }

        // Appearance & Themes
        item {
            SettingsCategoryHeader("Appearance & Themes")
        }

        item {
            SettingItemCard(
                icon = Icons.Default.ColorLens,
                title = "Theme Color Scheme",
                subtitle = currentTheme,
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(getThemePrimary(currentTheme))
                    )
                },
                onClick = { showThemeDialog = true }
            )
        }

        // Playback Engine Settings
        item {
            SettingsCategoryHeader("Playback & Engine")
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
                icon = Icons.Default.Memory,
                title = "Stream Buffer Size",
                subtitle = "$bufferSize (Buffer latency optimization)",
                onClick = { showBufferDialog = true }
            )
        }

        item {
            FocusableCard(
                onClick = { coroutineScope.launch { prefs.setHardwareAccel(!isHardwareAccel) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(14.dp)
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
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Hardware Decoding", color = NSTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("Accelerates H.264 / HEVC via device GPU", color = NSTextSecondary, fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = isHardwareAccel,
                        onCheckedChange = { checked -> coroutineScope.launch { prefs.setHardwareAccel(checked) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        // Parental Controls
        item {
            SettingsCategoryHeader("Security & Content Protection")
        }

        item {
            SettingItemCard(
                icon = Icons.Default.Lock,
                title = "Parental Controls PIN",
                subtitle = if (isParentalEnabled) "Protected with 4-Digit PIN" else "Disabled (All categories visible)",
                onClick = { showPinDialog = true }
            )
        }

        // Storage & Cache
        item {
            SettingsCategoryHeader("Data & Storage")
        }

        item {
            SettingItemCard(
                icon = Icons.Default.Delete,
                title = "Clear Playback History",
                subtitle = "Removes all recently watched streams",
                onClick = { showClearHistoryDialog = true }
            )
        }

        // About NS Player
        item {
            SettingsCategoryHeader("About & Device")
        }

        item {
            FocusableCard(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("NS Player v1.0.0", color = NSTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Platform: ${if (deviceMode == DeviceMode.TELEVISION) "Android TV / Google TV" else "Android Mobile / Tablet"} • Media3 ExoPlayer 1.4.1",
                            color = NSTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        val themes = listOf("Obsidian", "Frosty Cyan", "Ruby Glow", "Emerald Stream", "Amber Glow", "Cyber Purple")
        SingleSelectDialog(
            title = "Select Theme Style",
            options = themes,
            selectedOption = currentTheme,
            onDismiss = { showThemeDialog = false },
            onSelect = {
                coroutineScope.launch { prefs.setThemeStyle(it) }
                showThemeDialog = false
            }
        )
    }

    if (showAspectDialog) {
        val aspectModes = listOf("FIT", "CROP", "STRETCH", "16:9", "4:3")
        SingleSelectDialog(
            title = "Default Aspect Ratio",
            options = aspectModes,
            selectedOption = defaultAspect,
            onDismiss = { showAspectDialog = false },
            onSelect = {
                coroutineScope.launch { prefs.setDefaultAspectRatio(it) }
                showAspectDialog = false
            }
        )
    }

    if (showBufferDialog) {
        val buffers = listOf("Low Latency", "Normal", "Deep Buffer")
        SingleSelectDialog(
            title = "Stream Buffer Profile",
            options = buffers,
            selectedOption = bufferSize,
            onDismiss = { showBufferDialog = false },
            onSelect = {
                coroutineScope.launch { prefs.setBufferSize(it) }
                showBufferDialog = false
            }
        )
    }

    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Parental Controls PIN", color = NSTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a 4-digit numeric PIN to protect adult groups, or leave blank to disable.", color = NSTextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        label = { Text("4-Digit PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (pinInput.length == 4) {
                                prefs.setParentalPin(pinInput)
                            } else if (pinInput.isEmpty()) {
                                prefs.setParentalPin(null)
                            }
                        }
                        showPinDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (pinInput.isEmpty()) "Disable PIN" else "Set PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel", color = NSTextSecondary)
                }
            },
            containerColor = NSSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Playback History?", color = NSTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text("This will remove all recently watched channels and continue watching items.", color = NSTextSecondary, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.historyRepo.clearHistory()
                        }
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = NSTextSecondary)
                }
            },
            containerColor = NSSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
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
        shape = RoundedCornerShape(14.dp)
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
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, color = NSTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(subtitle, color = NSTextSecondary, fontSize = 12.sp)
                }
            }

            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NSTextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
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
        title = { Text(title, color = NSTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                            onClick = { onSelect(option) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(option, color = NSTextPrimary, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NSTextSecondary)
            }
        },
        containerColor = NSSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
