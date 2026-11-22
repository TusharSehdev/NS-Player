package com.genuine.nsplayer.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genuine.nsplayer.data.model.Channel
import com.genuine.nsplayer.data.model.ContentType
import com.genuine.nsplayer.data.model.PluginMediaItem
import com.genuine.nsplayer.ui.common.*
import com.genuine.nsplayer.ui.theme.*
import com.genuine.nsplayer.ui.viewmodel.MainViewModel

@Composable
fun DiscoverScreen(
    viewModel: MainViewModel,
    onPlayChannel: (Channel) -> Unit,
    onPlayDirectStream: (url: String, headers: Map<String, String>, title: String?, logo: String?) -> Unit,
    onNavigateToExtensions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val channels by viewModel.channels.collectAsState()
    val installedPlugins by viewModel.installedPlugins.collectAsState()
    val pluginMedia by viewModel.pluginMedia.collectAsState()
    val pluginSources by viewModel.pluginSources.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedSource by remember { mutableStateOf("All Sources") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedMediaDetail by remember { mutableStateOf<PluginMediaItem?>(null) }

    // Filter media items
    val filteredPluginMedia = remember(pluginMedia, selectedSource, selectedCategory, searchQuery) {
        pluginMedia.filter { item ->
            val matchesSource = when (selectedSource) {
                "All Sources" -> true
                "Local Playlists" -> false
                else -> item.pluginName.equals(selectedSource, ignoreCase = true)
            }
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Movies" -> item.type == ContentType.MOVIE
                "TV Series" -> item.type == ContentType.SERIES
                "Live TV" -> item.type == ContentType.LIVE_TV
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.title.contains(searchQuery, ignoreCase = true) || item.category.contains(searchQuery, ignoreCase = true)
            }
            matchesSource && matchesCategory && matchesSearch
        }
    }

    val filteredChannels = remember(channels, selectedSource, selectedCategory, searchQuery) {
        if (selectedSource != "All Sources" && selectedSource != "Local Playlists") {
            emptyList()
        } else {
            channels.filter { ch ->
                val matchesCategory = when (selectedCategory) {
                    "All" -> true
                    "Movies" -> ch.contentType == ContentType.MOVIE
                    "TV Series" -> ch.contentType == ContentType.SERIES
                    "Live TV" -> ch.contentType == ContentType.LIVE_TV
                    else -> true
                }
                val matchesSearch = if (searchQuery.isBlank()) true else {
                    ch.name.contains(searchQuery, ignoreCase = true) || ch.groupTitle.contains(searchQuery, ignoreCase = true)
                }
                matchesCategory && matchesSearch
            }
        }
    }

    val allSources = remember(pluginSources) {
        listOf("All Sources") + pluginSources + listOf("Local Playlists")
    }

    val groupedByRow = remember(filteredPluginMedia) {
        filteredPluginMedia.groupBy { "${it.pluginName} • ${it.category}" }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Top Header & Search
        item {
            Column {
                Text(
                    text = "DISCOVER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexoraPrimaryAccent,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Cinematic Catalog",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexoraTextPrimary
                )
                Spacer(Modifier.height(14.dp))
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) }
                )
            }
        }

        // Stremio-style Source Selection Chips (Horizontal Carousel)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Source Provider",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexoraTextSecondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(allSources) { source ->
                        val isSelected = (selectedSource == source)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) NexoraPrimaryAccent else NexoraSurfaceElevated,
                            modifier = Modifier.clickable { selectedSource = source }
                        ) {
                            Text(
                                text = source,
                                color = if (isSelected) NexoraBackground else NexoraTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            val categories = listOf("All", "Movies", "TV Series", "Live TV")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = (selectedCategory == cat)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) NexoraSecondaryAccent.copy(alpha = 0.25f) else NexoraSurface,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, NexoraPrimaryAccent) else null,
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) NexoraPrimaryAccent else NexoraTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Hero Spotlight Banner (When search query is blank and content exists)
        if (searchQuery.isBlank() && filteredPluginMedia.isNotEmpty()) {
            item {
                val featured = filteredPluginMedia.first()
                FocusableCard(
                    onClick = { selectedMediaDetail = featured },
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = NexoraSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF162032), NexoraSurfaceElevated)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(360.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NexoraPrimaryAccent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "FEATURED • ${featured.pluginName.uppercase()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexoraPrimaryAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = featured.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexoraTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = featured.category,
                                fontSize = 12.sp,
                                color = NexoraTextSecondary
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { selectedMediaDetail = featured },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NexoraPrimaryAccent,
                                    contentColor = NexoraBackground
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Details & Play", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Extension Catalogs Content
        if (filteredPluginMedia.isNotEmpty()) {
            groupedByRow.forEach { (groupTitle, itemsList) ->
                item(key = groupTitle) {
                    SectionHeader(
                        title = groupTitle,
                        icon = Icons.Default.Movie,
                        badgeText = "${itemsList.size}"
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(itemsList, key = { it.id }) { mediaItem ->
                            PluginMediaCard(
                                media = mediaItem,
                                onClick = {
                                    if (mediaItem.type == ContentType.LIVE_TV && !mediaItem.streamUrl.isNullOrBlank()) {
                                        onPlayDirectStream(mediaItem.streamUrl, mediaItem.headers, mediaItem.title, mediaItem.posterUrl)
                                    } else {
                                        selectedMediaDetail = mediaItem
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Local Playlist Channels
        if (filteredChannels.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Playlist Channels",
                    icon = Icons.Default.Tv,
                    badgeText = "${filteredChannels.size}"
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(filteredChannels.take(20), key = { it.id }) { ch ->
                        ChannelCard(
                            channel = ch,
                            onClick = { onPlayChannel(ch) },
                            onToggleFavorite = { viewModel.toggleFavorite(ch) },
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }
        }

        // Empty state when no media found
        if (filteredPluginMedia.isEmpty() && filteredChannels.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = NexoraTextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matches for \"$searchQuery\"" else "No content available in this category",
                            color = NexoraTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Install extensions or load playlists to explore catalogs",
                            color = NexoraTextMuted,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = onNavigateToExtensions,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NexoraPrimaryAccent,
                                contentColor = NexoraBackground
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Open Extensions Hub", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Community Extensions Promo Banner
        item {
            FocusableCard(
                onClick = onNavigateToExtensions,
                shape = RoundedCornerShape(14.dp),
                backgroundColor = NexoraSurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NexoraPrimaryAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Extension,
                            contentDescription = null,
                            tint = NexoraPrimaryAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NEXORA Extensions Ecosystem",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexoraTextPrimary
                        )
                        Text(
                            text = if (installedPlugins.isEmpty())
                                "No extensions installed. Connect repositories to unlock extra catalogs."
                            else
                                "${installedPlugins.size} extension(s) installed • ${pluginMedia.size} titles synced.",
                            fontSize = 12.sp,
                            color = NexoraTextSecondary
                        )
                    }
                    Button(
                        onClick = onNavigateToExtensions,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexoraPrimaryAccent,
                            contentColor = NexoraBackground
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Manage", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Media Details Dialog (Stremio style)
    selectedMediaDetail?.let { media ->
        MediaDetailDialog(
            media = media,
            onDismiss = { selectedMediaDetail = null },
            onPlay = { sUrl, headers ->
                val current = selectedMediaDetail
                selectedMediaDetail = null
                onPlayDirectStream(sUrl, headers, current?.title, current?.posterUrl)
            },
            onResolveStream = { m, epId, q, onReady ->
                viewModel.resolveMediaStream(m, epId, q, onReady)
            },
            onFetchEpisodes = { m, onReady ->
                viewModel.fetchEpisodes(m, onReady)
            }
        )
    }
}
