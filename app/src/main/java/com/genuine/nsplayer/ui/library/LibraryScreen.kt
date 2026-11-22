package com.genuine.nsplayer.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genuine.nsplayer.data.model.Channel
import com.genuine.nsplayer.data.model.PlaybackHistory
import com.genuine.nsplayer.ui.common.ChannelCard
import com.genuine.nsplayer.ui.common.FocusableCard
import com.genuine.nsplayer.ui.common.ContinueWatchingCard
import com.genuine.nsplayer.ui.common.SectionHeader
import com.genuine.nsplayer.ui.playlist.AddPlaylistDialog
import com.genuine.nsplayer.ui.theme.*
import com.genuine.nsplayer.ui.viewmodel.MainViewModel

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onPlayChannel: (Channel) -> Unit,
    onPlayHistoryItem: (PlaybackHistory) -> Unit,
    onNavigateToExtensions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val history by viewModel.history.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val installedPlugins by viewModel.installedPlugins.collectAsState()

    var showAddPlaylistDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Library Title
        item {
            Column {
                Text(
                    text = "LIBRARY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexoraPrimaryAccent,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Media & Playlists",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexoraTextPrimary
                )
            }
        }

        // Playlists Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(title = "Playlists (${playlists.size})", icon = Icons.AutoMirrored.Filled.PlaylistPlay)
                Button(
                    onClick = { showAddPlaylistDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexoraPrimaryAccent,
                        contentColor = NexoraBackground
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Playlist", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(10.dp))

            if (playlists.isEmpty()) {
                FocusableCard(
                    onClick = { showAddPlaylistDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = NexoraSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NexoraSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddLink, contentDescription = null, tint = NexoraPrimaryAccent)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "No Playlists Added",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = NexoraTextPrimary
                            )
                            Text(
                                "Click here to add an M3U/M3U8 link or import a local file.",
                                fontSize = 12.sp,
                                color = NexoraTextSecondary
                            )
                        }
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(playlists, key = { it.id }) { pl ->
                        FocusableCard(
                            onClick = { viewModel.selectPlaylist(pl.id) },
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = NexoraSurface,
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.PlaylistPlay,
                                        contentDescription = null,
                                        tint = NexoraPrimaryAccent
                                    )
                                    IconButton(
                                        onClick = { viewModel.refreshPlaylist(pl.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NexoraTextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = pl.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = NexoraTextPrimary,
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${pl.channelCount} Channels",
                                    fontSize = 12.sp,
                                    color = NexoraTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Continue Watching Section
        if (continueWatching.isNotEmpty()) {
            item {
                SectionHeader(title = "Continue Watching", icon = Icons.Default.History)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(continueWatching, key = { it.streamUrl }) { h ->
                        ContinueWatchingCard(
                            item = h,
                            onClick = { onPlayHistoryItem(h) }
                        )
                    }
                }
            }
        }

        // Favorites Section
        if (favorites.isNotEmpty()) {
            item {
                SectionHeader(title = "Favorites (${favorites.size})", icon = Icons.Default.Star)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(favorites, key = { it.id }) { channel ->
                        ChannelCard(
                            channel = channel,
                            onClick = { onPlayChannel(channel) },
                            onToggleFavorite = { viewModel.toggleFavorite(channel) },
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }
        }

        // Extensions Section Shortcut
        item {
            FocusableCard(
                onClick = onNavigateToExtensions,
                shape = RoundedCornerShape(12.dp),
                backgroundColor = NexoraSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NexoraSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Extension, contentDescription = null, tint = NexoraPrimaryAccent)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Extensions & Community Plugins",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NexoraTextPrimary
                        )
                        Text(
                            text = "${installedPlugins.size} installed • Manage repositories and updates",
                            fontSize = 12.sp,
                            color = NexoraTextSecondary
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NexoraTextMuted)
                }
            }
        }
    }

    if (showAddPlaylistDialog) {
        AddPlaylistDialog(
            onDismiss = { showAddPlaylistDialog = false },
            onAdd = { name, url, filePath, userAgent ->
                viewModel.addPlaylist(name, url, filePath, userAgent)
                showAddPlaylistDialog = false
            }
        )
    }
}
