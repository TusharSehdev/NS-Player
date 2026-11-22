package com.genuine.nsplayer.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.genuine.nsplayer.data.model.Playlist
import com.genuine.nsplayer.ui.common.FocusableCard
import com.genuine.nsplayer.ui.theme.*
import com.genuine.nsplayer.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlaylistScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val activeId by viewModel.activePlaylistId.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Playlists", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NSTextPrimary)
                Text("${playlists.size} playlists registered", fontSize = 12.sp, color = NSTextSecondary)
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Playlist", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Import progress indicator
        if (importProgress != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NSSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, NSBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Importing channels… ($importProgress channels parsed)", color = NSTextPrimary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Error message card
        if (errorMessage != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x33FF453A),
                border = androidx.compose.foundation.BorderStroke(1.dp, NSError),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(errorMessage ?: "", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = null,
                        tint = NSTextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "No playlists configured yet",
                        color = NSTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tap '+ Add Playlist' above to import your M3U / M3U8 IPTV list.",
                        color = NSTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    val isActive = playlist.id == activeId
                    PlaylistItemCard(
                        playlist = playlist,
                        isActive = isActive,
                        onSelect = { viewModel.selectPlaylist(playlist.id) },
                        onRefresh = { viewModel.refreshPlaylist(playlist.id) },
                        onDelete = { viewModel.deletePlaylist(playlist.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddPlaylistDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, url, file, userAgent ->
                showAddDialog = false
                viewModel.addPlaylist(name, url, file, userAgent)
            }
        )
    }
}

@Composable
fun PlaylistItemCard(
    playlist: Playlist,
    isActive: Boolean,
    onSelect: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    FocusableCard(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .height(94.dp),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else NSSurfaceCard,
        borderColor = if (isActive) MaterialTheme.colorScheme.primary else NSBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isActive) MaterialTheme.colorScheme.primary else NSSurfaceVariant,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isActive) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.PlaylistPlay,
                            contentDescription = null,
                            tint = if (isActive) Color.White else NSTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = playlist.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NSTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isActive) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${playlist.channelCount} channels • Updated ${dateFormat.format(Date(playlist.lastUpdated))}",
                        fontSize = 12.sp,
                        color = NSTextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = NSTextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun AddPlaylistDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, url: String?, filePath: String?, userAgent: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var userAgent by remember { mutableStateOf("") }

    val sampleUrl = "https://iptv-org.github.io/iptv/index.m3u"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add IPTV Playlist", color = NSTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enter the remote URL or local storage path for your playlist:", color = NSTextSecondary, fontSize = 12.sp)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name (e.g. World IPTV)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("M3U / M3U8 Playlist URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Sample Fill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NSSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (name.isBlank()) name = "Global Free IPTV"
                            url = sampleUrl
                        }
                ) {
                    Text(
                        text = "⚡ Fill Sample: Free IPTV-org index.m3u",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                OutlinedTextField(
                    value = userAgent,
                    onValueChange = { userAgent = it },
                    label = { Text("User-Agent (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        onAdd(name.trim(), url.trim(), null, userAgent.ifBlank { null })
                    }
                },
                enabled = name.isNotBlank() && url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Import Playlist")
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
