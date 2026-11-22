package com.genuine.nsplayer.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.genuine.nsplayer.ui.common.ChannelCard
import com.genuine.nsplayer.ui.common.FocusableCard
import com.genuine.nsplayer.ui.common.SearchBar
import com.genuine.nsplayer.ui.common.SectionHeader
import com.genuine.nsplayer.ui.theme.*
import com.genuine.nsplayer.ui.viewmodel.MainViewModel

@Composable
fun DiscoverScreen(
    viewModel: MainViewModel,
    onPlayChannel: (Channel) -> Unit,
    onNavigateToExtensions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val channels by viewModel.channels.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val installedPlugins by viewModel.installedPlugins.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val movies = remember(channels) { channels.filter { it.contentType == ContentType.MOVIE } }
    val series = remember(channels) { channels.filter { it.contentType == ContentType.SERIES } }
    val liveChannels = remember(channels) { channels.filter { it.contentType == ContentType.LIVE_TV } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
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

        // Hero Spotlight Banner
        if (searchQuery.isBlank() && channels.isNotEmpty()) {
            item {
                val featured = channels.firstOrNull { it.isFavorite } ?: channels.first()
                FocusableCard(
                    onClick = { onPlayChannel(featured) },
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = NexoraSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF162032),
                                        NexoraSurfaceElevated
                                    )
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
                                    text = "FEATURED MEDIA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexoraPrimaryAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = featured.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexoraTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = featured.groupTitle.ifBlank { "Live Stream" },
                                fontSize = 12.sp,
                                color = NexoraTextSecondary
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { onPlayChannel(featured) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NexoraPrimaryAccent,
                                        contentColor = NexoraBackground
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Watch Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Channels Row
        if (liveChannels.isNotEmpty()) {
            item {
                SectionHeader(title = "Live Channels", icon = Icons.Default.LiveTv)
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(liveChannels.take(15), key = { it.id }) { channel ->
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

        // Movies Row
        if (movies.isNotEmpty()) {
            item {
                SectionHeader(title = "On-Demand Movies", icon = Icons.Default.Movie)
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(movies.take(15), key = { it.id }) { movie ->
                        ChannelCard(
                            channel = movie,
                            onClick = { onPlayChannel(movie) },
                            onToggleFavorite = { viewModel.toggleFavorite(movie) },
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }
        }

        // TV Series Row
        if (series.isNotEmpty()) {
            item {
                SectionHeader(title = "TV Series", icon = Icons.Default.Tv)
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(series.take(15), key = { it.id }) { s ->
                        ChannelCard(
                            channel = s,
                            onClick = { onPlayChannel(s) },
                            onToggleFavorite = { viewModel.toggleFavorite(s) },
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }
        }

        // Community Extensions Banner
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
                                "${installedPlugins.size} extension(s) installed and active.",
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
}
