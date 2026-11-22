package com.genuine.nsplayer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genuine.nsplayer.core.DeviceDetector
import com.genuine.nsplayer.core.DeviceMode
import com.genuine.nsplayer.data.model.Channel
import com.genuine.nsplayer.ui.channel.ChannelListScreen
import com.genuine.nsplayer.ui.home.HomeScreen
import com.genuine.nsplayer.ui.player.PlayerActivity
import com.genuine.nsplayer.ui.playlist.PlaylistScreen
import com.genuine.nsplayer.ui.settings.SettingsScreen
import com.genuine.nsplayer.ui.theme.NSBlack
import com.genuine.nsplayer.ui.theme.NSPlayerTheme
import com.genuine.nsplayer.ui.theme.NSSurface
import com.genuine.nsplayer.ui.theme.NSTextSecondary
import com.genuine.nsplayer.ui.viewmodel.MainViewModel

enum class MainTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    CHANNELS("Channels", Icons.Default.LiveTv),
    PLAYLISTS("Playlists", Icons.AutoMirrored.Filled.PlaylistPlay),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deviceMode = DeviceDetector.getDeviceMode(this)

        setContent {
            val currentTheme by mainViewModel.preferences.themeStyle.collectAsState(initial = "Obsidian")
            val isDark by mainViewModel.preferences.isDarkMode.collectAsState(initial = true)

            NSPlayerTheme(themeStyle = currentTheme, darkTheme = isDark) {
                var currentTab by remember { mutableStateOf(MainTab.HOME) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NSBlack
                ) {
                    if (deviceMode == DeviceMode.TELEVISION) {
                        // Android TV 10-foot Navigation Rail Layout
                        Row(modifier = Modifier.fillMaxSize()) {
                            NavigationRail(
                                containerColor = NSSurface,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(88.dp)
                            ) {
                                Spacer(Modifier.height(16.dp))
                                MainTab.values().forEach { tab ->
                                    NavigationRailItem(
                                        selected = currentTab == tab,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        },
                                        label = { Text(tab.title, fontSize = 11.sp) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = NSTextSecondary,
                                            unselectedTextColor = NSTextSecondary
                                        )
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(NSBlack)
                            ) {
                                AppTabContent(
                                    tab = currentTab,
                                    viewModel = mainViewModel,
                                    onPlayChannel = { channel -> PlayerActivity.launch(this@MainActivity, channel) },
                                    onPlayDirectStream = { url, headers, drmScheme, drmLicense ->
                                        val c = Channel(
                                            playlistId = 0,
                                            name = "Direct Stream",
                                            streamUrl = url,
                                            headers = headers,
                                            drmScheme = drmScheme,
                                            drmLicense = drmLicense
                                        )
                                        PlayerActivity.launch(this@MainActivity, c)
                                    },
                                    onNavigateToChannels = { currentTab = MainTab.CHANNELS },
                                    onNavigateToPlaylists = { currentTab = MainTab.PLAYLISTS }
                                )
                            }
                        }
                    } else {
                        // Mobile / Tablet Bottom Navigation Scaffold
                        Scaffold(
                            bottomBar = {
                                NavigationBar(
                                    containerColor = NSSurface,
                                    contentColor = Color.White
                                ) {
                                    MainTab.values().forEach { tab ->
                                        NavigationBarItem(
                                            selected = currentTab == tab,
                                            onClick = { currentTab = tab },
                                            icon = {
                                                Icon(
                                                    imageVector = tab.icon,
                                                    contentDescription = tab.title,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            },
                                            label = { Text(tab.title, fontSize = 11.sp) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primary,
                                                unselectedIconColor = NSTextSecondary,
                                                unselectedTextColor = NSTextSecondary
                                            )
                                        )
                                    }
                                }
                            },
                            containerColor = NSBlack
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                AppTabContent(
                                    tab = currentTab,
                                    viewModel = mainViewModel,
                                    onPlayChannel = { channel -> PlayerActivity.launch(this@MainActivity, channel) },
                                    onPlayDirectStream = { url, headers, drmScheme, drmLicense ->
                                        val c = Channel(
                                            playlistId = 0,
                                            name = "Direct Stream",
                                            streamUrl = url,
                                            headers = headers,
                                            drmScheme = drmScheme,
                                            drmLicense = drmLicense
                                        )
                                        PlayerActivity.launch(this@MainActivity, c)
                                    },
                                    onNavigateToChannels = { currentTab = MainTab.CHANNELS },
                                    onNavigateToPlaylists = { currentTab = MainTab.PLAYLISTS }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppTabContent(
    tab: MainTab,
    viewModel: MainViewModel,
    onPlayChannel: (Channel) -> Unit,
    onPlayDirectStream: (String, Map<String, String>, String?, String?) -> Unit,
    onNavigateToChannels: () -> Unit,
    onNavigateToPlaylists: () -> Unit
) {
    when (tab) {
        MainTab.HOME -> HomeScreen(
            viewModel = viewModel,
            onPlayChannel = onPlayChannel,
            onPlayDirectStream = onPlayDirectStream,
            onNavigateToChannels = onNavigateToChannels,
            onNavigateToPlaylists = onNavigateToPlaylists
        )
        MainTab.CHANNELS -> ChannelListScreen(
            viewModel = viewModel,
            onPlayChannel = onPlayChannel
        )
        MainTab.PLAYLISTS -> PlaylistScreen(
            viewModel = viewModel,
            onNavigateBack = onNavigateToChannels
        )
        MainTab.SETTINGS -> SettingsScreen(
            viewModel = viewModel
        )
    }
}
