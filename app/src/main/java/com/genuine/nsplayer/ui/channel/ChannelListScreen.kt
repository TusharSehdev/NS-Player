package com.genuine.nsplayer.ui.channel

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genuine.nsplayer.data.model.Channel
import com.genuine.nsplayer.data.model.ContentType
import com.genuine.nsplayer.ui.common.ChannelCard
import com.genuine.nsplayer.ui.common.SearchBar
import com.genuine.nsplayer.ui.common.ShimmerPlaceholder
import com.genuine.nsplayer.ui.theme.NSBorder
import com.genuine.nsplayer.ui.theme.NSSurfaceCard
import com.genuine.nsplayer.ui.theme.NSTextMuted
import com.genuine.nsplayer.ui.theme.NSTextSecondary
import com.genuine.nsplayer.ui.viewmodel.MainViewModel

@Composable
fun ChannelListScreen(
    viewModel: MainViewModel,
    onPlayChannel: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val channels by viewModel.channels.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedContentType by viewModel.selectedContentType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Search Bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) }
        )

        Spacer(Modifier.height(14.dp))

        // Content Type & Category Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == "All" && selectedContentType == null,
                onClick = { viewModel.selectCategory("All") },
                label = { Text("All Channels", fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = androidx.compose.ui.graphics.Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            FilterChip(
                selected = selectedContentType == ContentType.LIVE_TV,
                onClick = { viewModel.selectContentType(ContentType.LIVE_TV) },
                leadingIcon = { Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Live TV") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = androidx.compose.ui.graphics.Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            FilterChip(
                selected = selectedContentType == ContentType.MOVIE,
                onClick = { viewModel.selectContentType(ContentType.MOVIE) },
                leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Movies") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = androidx.compose.ui.graphics.Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            FilterChip(
                selected = selectedContentType == ContentType.SERIES,
                onClick = { viewModel.selectContentType(ContentType.SERIES) },
                leadingIcon = { Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Series") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = androidx.compose.ui.graphics.Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Dynamic Groups from M3U
            categories.take(35).forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { viewModel.selectCategory(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = androidx.compose.ui.graphics.Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Loading or Channel Grid
        if (importProgress != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NSSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, NSBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(10.dp))
                    Text("Importing playlist channels… ($importProgress parsed)", fontSize = 13.sp, color = NSTextSecondary)
                }
            }
        }

        if (channels.isEmpty() && importProgress == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = NSTextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No channels matching '$searchQuery'" else "No channels found in this category",
                        color = NSTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(channels, key = { it.id }) { channel ->
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
