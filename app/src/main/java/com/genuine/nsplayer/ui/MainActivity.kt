package com.genuine.nsplayer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.genuine.nsplayer.data.model.ContentType
import com.genuine.nsplayer.ui.channel.ChannelListScreen
import com.genuine.nsplayer.ui.discover.DiscoverScreen
import com.genuine.nsplayer.ui.extensions.ExtensionsScreen
import com.genuine.nsplayer.ui.home.HomeScreen
import com.genuine.nsplayer.ui.library.LibraryScreen
import com.genuine.nsplayer.ui.player.PlayerActivity
import com.genuine.nsplayer.ui.playlist.PlaylistScreen
import com.genuine.nsplayer.ui.settings.SettingsScreen
import com.genuine.nsplayer.ui.theme.*
import com.genuine.nsplayer.ui.viewmodel.MainViewModel

enum class MobileTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    LIVE_TV("Live TV", Icons.Default.LiveTv),
    DISCOVER("Discover", Icons.Default.Explore),
    LIBRARY("Library", Icons.Default.VideoLibrary),
    SETTINGS("Settings", Icons.Default.Settings),
    EXTENSIONS("Extensions", Icons.Default.Extension)
}

enum class TvTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    LIVE_TV("Live TV", Icons.Default.LiveTv),
    MOVIES("Movies", Icons.Default.Movie),
    SERIES("Series", Icons.Default.Tv),
    LIBRARY("Library", Icons.Default.VideoLibrary),
    EXTENSIONS("Extensions", Icons.Default.Extension),
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
                var mobileTab by remember { mutableStateOf(MobileTab.HOME) }
                var tvTab by remember { mutableStateOf(TvTab.HOME) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NexoraBackground
                ) {
                    if (deviceMode == DeviceMode.TELEVISION) {
                        // Android TV / Google TV: 10-foot Navigation Rail Layout
                        Row(modifier = Modifier.fillMaxSize()) {
                            NavigationRail(
                                containerColor = NexoraSurface,
                                contentColor = NexoraTextPrimary,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(96.dp)
                            ) {
                                Spacer(Modifier.height(16.dp))
                                TvTab.values().forEach { tab ->
                                    val isSelected = tvTab == tab
                                    NavigationRailItem(
                                        selected = isSelected,
                                        onClick = {
                                            tvTab = tab
                                            when (tab) {
                                                TvTab.LIVE_TV -> mainViewModel.selectContentType(ContentType.LIVE_TV)
                                                TvTab.MOVIES -> mainViewModel.selectContentType(ContentType.MOVIE)
                                                TvTab.SERIES -> mainViewModel.selectContentType(ContentType.SERIES)
                                                else -> {}
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                tab.title,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = NexoraBackground,
                                            selectedTextColor = NexoraPrimaryAccent,
                                            indicatorColor = NexoraPrimaryAccent,
                                            unselectedIconColor = NexoraTextSecondary,
                                            unselectedTextColor = NexoraTextMuted
                                        )
                                    )
                                    Spacer(Modifier.height(6.dp))
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(NexoraBackground)
                            ) {
                                when (tvTab) {
                                    TvTab.HOME -> HomeScreen(
                                        viewModel = mainViewModel,
                                        onPlayChannel = { channel -> PlayerActivity.launch(this@MainActivity, channel) },
                                        onPlayDirectStream = { url, headers, title, logo ->
                                            launchDirectStream(url, headers, title, logo)
                                        },
                                        onNavigateToChannels = { tvTab = TvTab.LIVE_TV },
                                        onNavigateToPlaylists = { tvTab = TvTab.LIBRARY },
                                        onNavigateToExtensions = { tvTab = TvTab.EXTENSIONS }
                                    )
                                    TvTab.LIVE_TV -> ChannelListScreen(
                                        viewModel = mainViewModel,
                                        onPlayChannel = { channel -> PlayerActivity.launch(this@MainActivity, channel) }
                                    )
                                    TvTab.MOVIES, TvTab.SERIES -> DiscoverScreen(
                                        viewModel = mainViewModel,
                                        onPlayChannel = { channel -> PlayerActivity.launch(this@MainActivity, channel) },
                                        onPlayDirectStream = { url, headers, title, logo ->
                                            launchDirectStream(url, headers, title, logo)
                                        },
                                        onNavigateToExtensions = { tvTab = TvTab.EXTENSIONS }
                                    )
                                    TvTab.LIBRARY -> LibraryScreen(
                                        viewModel = mainViewModel,
                                        onPlayChannel = { channel -> PlayerActivity.launch(this@MainActivity, channel) },
                                        onPlayHistoryItem = { h ->
                                            launchDirectStream(h.streamUrl, emptyMap(), null, null)
                                        },
                                        onNavigateToExtensions = { tvTab = TvTab.EXTENSIONS }
                                    )
                                    TvTab.EXTENSIONS -> ExtensionsScreen(
                                        viewModel = mainViewModel
                                    )
                                    TvTab.SETTINGS -> SettingsScreen(
                                        viewModel = mainViewModel,
                                        onNavigateToExtensions = { tvTab = TvTab.EXTENSIONS }
                                    )
                                }
                            }
                        }
                    } else {
                        // Mobile / Tablet: 5 Primary Bottom Navigation Items
                        Scaffold(
                            bottomBar = {
                                NavigationBar(
                                    containerColor = NexoraSurface,
                                    contentColor = NexoraTextPrimary
                                ) {
                                    val mobileTabs = listOf(
                                        MobileTab.HOME,
                                        MobileTab.LIVE_TV,
                                        MobileTab.DISCOVER,
                                        MobileTab.LIBRARY,
                                        MobileTab.SETTINGS
                                    )

                                    mobileTabs.forEach { tab ->
                                        val isSelected = mobileTab == tab
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = {
                                                mobileTab = tab
                                                if (tab == MobileTab.LIVE_TV) {
                                                    mainViewModel.selectContentType(ContentType.LIVE_TV)
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = tab.icon,
                                                    contentDescription = tab.title,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    tab.title,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = NexoraBackground,
                                                selectedTextColor = NexoraPrimaryAccent,
                                                indicatorColor = NexoraPrimaryAccent,
                                                unselectedIconColor = NexoraTextSecondary,
                                                unselectedTextColor = NexoraTextMuted
                                            )
                                        )
                                    }
                                }
                            },
                            containerColor = NexoraBackground
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (mobileTab) {
                                    MobileTab.HOME -> HomeScreen(
                                        viewModel = mainViewModel,
                                        onPlayChannel = { channel -> PlayerActivity.launch(this@MainActivity, channel) },
                                        onPlayDirectStream = { url, headers, title, logo ->
                                            launchDirectStream(url, headers, title, logo)
                                        },
                                        onNavigateToChannels = { mobileTab = MobileTab.LIVE_TV },
                                        onNavigateToPlaylists = { mobileTab = MobileTab.LIBRARY },
                                        onNavigateToExtensions = { mobileTab = MobileTab.EXTENSIONS }
                                    )
                                    MobileTab.LIVE_TV -> ChannelListScreen(
                                        viewModel = mainViewModel,
                                        onPlayChannel = { channel -> PlayerActivity.launch(this@MainActivity, channel) }
                                    )
                                    MobileTab.DISCOVER -> DiscoverScreen(
                                        viewModel = mainViewModel,
                                        onPlayChannel = { channel -> PlayerActivity.launch(this@MainActivity, channel) },
                                        onPlayDirectStream = { url, headers, title, logo ->
                                            launchDirectStream(url, headers, title, logo)
                                        },
                                        onNavigateToExtensions = { mobileTab = MobileTab.EXTENSIONS }
                                    )
                                    MobileTab.LIBRARY -> LibraryScreen(
                                        viewModel = mainViewModel,
                                        onPlayChannel = { channel -> PlayerActivity.launch(this@MainActivity, channel) },
                                        onPlayHistoryItem = { h ->
                                            launchDirectStream(h.streamUrl, emptyMap(), null, null)
                                        },
                                        onNavigateToExtensions = { mobileTab = MobileTab.EXTENSIONS }
                                    )
                                    MobileTab.EXTENSIONS -> ExtensionsScreen(
                                        viewModel = mainViewModel
                                    )
                                    MobileTab.SETTINGS -> SettingsScreen(
                                        viewModel = mainViewModel,
                                        onNavigateToExtensions = { mobileTab = MobileTab.EXTENSIONS }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun launchDirectStream(
        url: String,
        headers: Map<String, String>,
        title: String? = null,
        logoUrl: String? = null,
        drmScheme: String? = null,
        drmLicense: String? = null
    ) {
        val channel = Channel(
            playlistId = 0,
            name = title ?: "Direct Stream",
            streamUrl = url,
            logoUrl = logoUrl,
            headers = headers,
            drmScheme = drmScheme,
            drmLicense = drmLicense
        )
        PlayerActivity.launch(this, channel)
    }
}
