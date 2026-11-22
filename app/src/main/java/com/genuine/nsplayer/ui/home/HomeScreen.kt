package com.genuine.nsplayer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.genuine.nsplayer.data.model.Channel
import com.genuine.nsplayer.data.model.ContentType
import com.genuine.nsplayer.ui.common.*
import com.genuine.nsplayer.ui.theme.*
import com.genuine.nsplayer.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onPlayChannel: (Channel) -> Unit,
    onPlayDirectStream: (String, Map<String, String>, String?, String?) -> Unit,
    onNavigateToChannels: () -> Unit,
    onNavigateToPlaylists: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val activeId by viewModel.activePlaylistId.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val history by viewModel.history.collectAsState()

    val activePlaylist = playlists.firstOrNull { it.id == activeId }
    var showDirectStreamDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 90.dp)
    ) {
        // Top Header: Branding + Status Pill + Stream URL Action Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.LiveTv, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "NS Player",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NSTextPrimary
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (activePlaylist != null) NSEPGGreen else NSTextMuted)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (activePlaylist != null) "${activePlaylist.name} • ${activePlaylist.channelCount} Channels" else "No active playlist selected",
                            fontSize = 12.sp,
                            color = NSTextSecondary
                        )
                    }
                }

                Button(
                    onClick = { showDirectStreamDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Stream URL", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Spotlight Hero Card
        item {
            HeroBanner(
                title = activePlaylist?.name ?: "Welcome to NS Player",
                subtitle = if (activePlaylist != null) "${activePlaylist.channelCount} channels ready for streaming" else "Add an M3U / M3U8 playlist or direct link to start watching",
                tag = if (activePlaylist != null) "ACTIVE PLAYLIST" else "GET STARTED",
                onPlayClick = {
                    if (favorites.isNotEmpty()) {
                        onPlayChannel(favorites.first())
                    } else {
                        onNavigateToChannels()
                    }
                },
                onBrowseClick = onNavigateToChannels
            )
        }

        // Quick Navigation Rail: Live TV | Movies | Series | Playlists
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickNavCard(
                    title = "Live TV",
                    subtitle = "All Channels",
                    icon = Icons.Default.LiveTv,
                    iconTint = NSPrimary,
                    onClick = {
                        viewModel.selectContentType(ContentType.LIVE_TV)
                        onNavigateToChannels()
                    },
                    modifier = Modifier.weight(1f)
                )

                QuickNavCard(
                    title = "Movies",
                    subtitle = "VOD Cinema",
                    icon = Icons.Default.Movie,
                    iconTint = CyberPurplePrimary,
                    onClick = {
                        viewModel.selectContentType(ContentType.MOVIE)
                        onNavigateToChannels()
                    },
                    modifier = Modifier.weight(1f)
                )

                QuickNavCard(
                    title = "TV Series",
                    subtitle = "Shows & Seasons",
                    icon = Icons.Default.Tv,
                    iconTint = EmeraldPrimary,
                    onClick = {
                        viewModel.selectContentType(ContentType.SERIES)
                        onNavigateToChannels()
                    },
                    modifier = Modifier.weight(1f)
                )

                QuickNavCard(
                    title = "Playlists",
                    subtitle = "${playlists.size} Saved",
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    iconTint = AmberGlowPrimary,
                    onClick = onNavigateToPlaylists,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Continue Watching Section
        if (continueWatching.isNotEmpty()) {
            item {
                SectionHeader(title = "Continue Watching", icon = Icons.Default.History, badgeText = "${continueWatching.size}")
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(continueWatching, key = { it.streamUrl }) { item ->
                        ContinueWatchingCard(
                            item = item,
                            onClick = {
                                onPlayDirectStream(item.streamUrl, emptyMap(), null, null)
                            }
                        )
                    }
                }
            }
        }

        // Favorites Horizontal Carousel
        if (favorites.isNotEmpty()) {
            item {
                SectionHeader(title = "Favorite Channels", icon = Icons.Default.Star, badgeText = "${favorites.size}")
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(favorites, key = { it.id }) { channel ->
                        Box(modifier = Modifier.width(148.dp)) {
                            ChannelCard(
                                channel = channel,
                                onClick = { onPlayChannel(channel) },
                                onToggleFavorite = { viewModel.toggleFavorite(channel) }
                            )
                        }
                    }
                }
            }
        }

        // Playlist Overview Card
        item {
            FocusableCard(
                onClick = onNavigateToPlaylists,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.PlaylistPlay,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Playlist Management", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NSTextPrimary)
                            Text("${playlists.size} playlists loaded • Tap to manage or add new sources", fontSize = 12.sp, color = NSTextSecondary)
                        }
                    }

                    Button(
                        onClick = onNavigateToPlaylists,
                        colors = ButtonDefaults.buttonColors(containerColor = NSSurfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Manage", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Recently Watched List
        if (history.isNotEmpty()) {
            item {
                SectionHeader(title = "Recently Watched", icon = Icons.Default.History)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    history.take(6).forEach { item ->
                        FocusableCard(
                            onClick = { onPlayDirectStream(item.streamUrl, emptyMap(), null, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(14.dp))
                                    Text(
                                        text = item.title,
                                        color = NSTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NSTextSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDirectStreamDialog) {
        DirectStreamDialog(
            onDismiss = { showDirectStreamDialog = false },
            onPlay = { url, headers, drmScheme, drmLicense ->
                showDirectStreamDialog = false
                onPlayDirectStream(url, headers, drmScheme, drmLicense)
            }
        )
    }
}

@Composable
fun DirectStreamDialog(
    onDismiss: () -> Unit,
    onPlay: (url: String, headers: Map<String, String>, drmScheme: String?, drmLicense: String?) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var userAgent by remember { mutableStateOf("") }
    var referer by remember { mutableStateOf("") }
    var drmScheme by remember { mutableStateOf("") }
    var drmLicense by remember { mutableStateOf("") }

    // Quick test stream presets
    val presets = listOf(
        "Big Buck Bunny (HLS)" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        "Tears of Steel (HLS 1080p)" to "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
        "Akamai Live Test (HLS)" to "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Play Network Stream", color = NSTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enter any M3U8, HLS, MP4 or progressive stream link:", color = NSTextSecondary, fontSize = 12.sp)

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Stream URL (M3U8 / HTTP)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Presets row
                Text("Or select a test sample:", color = NSTextMuted, fontSize = 11.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presets.forEach { (name, link) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NSSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { url = link }
                        ) {
                            Text(
                                text = "▶ $name",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = userAgent,
                    onValueChange = { userAgent = it },
                    label = { Text("User-Agent (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = referer,
                    onValueChange = { referer = it },
                    label = { Text("Referer (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        val headers = mutableMapOf<String, String>()
                        if (userAgent.isNotBlank()) headers["User-Agent"] = userAgent
                        if (referer.isNotBlank()) headers["Referer"] = referer

                        onPlay(
                            url.trim(),
                            headers,
                            drmScheme.ifBlank { null },
                            drmLicense.ifBlank { null }
                        )
                    }
                },
                enabled = url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Play Stream")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NSTextSecondary)
            }
        },
        containerColor = NSSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
