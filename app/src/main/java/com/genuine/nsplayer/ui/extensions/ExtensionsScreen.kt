package com.genuine.nsplayer.ui.extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.genuine.nsplayer.data.model.PluginCapability
import com.genuine.nsplayer.data.model.PluginItem
import com.genuine.nsplayer.data.model.PluginRepositoryItem
import com.genuine.nsplayer.data.model.PluginStatus
import com.genuine.nsplayer.ui.common.FocusableCard
import com.genuine.nsplayer.ui.common.SearchBar
import com.genuine.nsplayer.ui.theme.*
import com.genuine.nsplayer.ui.viewmodel.MainViewModel

enum class ExtensionTab(val title: String) {
    DISCOVER("Discover"),
    INSTALLED("Installed"),
    REPOSITORIES("Repositories"),
    UPDATES("Updates")
}

@Composable
fun ExtensionsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ExtensionTab.DISCOVER) }
    var showAddRepoDialog by remember { mutableStateOf(false) }

    val allPlugins by viewModel.allPlugins.collectAsState()
    val installedPlugins by viewModel.installedPlugins.collectAsState()
    val repositories by viewModel.repositories.collectAsState()
    val isProgressing by viewModel.importProgress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(infoMessage) {
        infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfo()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NexoraBackground
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header with Title and Tab Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EXTENSIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexoraPrimaryAccent,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Plugin Ecosystem",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexoraTextPrimary
                    )
                }

                if (selectedTab == ExtensionTab.REPOSITORIES) {
                    Button(
                        onClick = { showAddRepoDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexoraPrimaryAccent,
                            contentColor = NexoraBackground
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Repository", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Navigation Tabs (Discover, Installed, Repositories, Updates)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NexoraSurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExtensionTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val badgeCount = when (tab) {
                        ExtensionTab.INSTALLED -> installedPlugins.size
                        ExtensionTab.REPOSITORIES -> repositories.size
                        ExtensionTab.UPDATES -> allPlugins.count { it.status == PluginStatus.UPDATE_AVAILABLE }
                        else -> 0
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) NexoraSurfaceElevated else Color.Transparent,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, NexoraPrimaryAccent) else null,
                        onClick = { selectedTab = tab }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tab.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) NexoraTextPrimary else NexoraTextSecondary
                            )
                            if (badgeCount > 0) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (tab == ExtensionTab.UPDATES) NexoraWarning else NexoraPrimaryAccent.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "$badgeCount",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tab == ExtensionTab.UPDATES) Color.Black else NexoraPrimaryAccent,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tab Content
            when (selectedTab) {
                ExtensionTab.DISCOVER -> DiscoverPluginsTab(
                    plugins = allPlugins,
                    onInstall = { viewModel.installPlugin(it.id) }
                )
                ExtensionTab.INSTALLED -> InstalledPluginsTab(
                    plugins = installedPlugins,
                    onToggleEnable = { plugin, enabled -> viewModel.togglePluginEnabled(plugin.id, enabled) },
                    onSync = { viewModel.syncPlugin(it.id) },
                    onUninstall = { viewModel.uninstallPlugin(it.id) }
                )
                ExtensionTab.REPOSITORIES -> RepositoriesTab(
                    repositories = repositories,
                    onRefresh = { viewModel.refreshRepository(it.id) },
                    onDelete = { viewModel.deleteRepository(it.id) }
                )
                ExtensionTab.UPDATES -> UpdatesTab(
                    plugins = allPlugins.filter { it.status == PluginStatus.UPDATE_AVAILABLE },
                    onUpdate = { viewModel.updatePlugin(it.id) }
                )
            }
        }
    }

    if (showAddRepoDialog) {
        AddRepositoryDialog(
            onDismiss = { showAddRepoDialog = false },
            onConfirm = { name, url ->
                viewModel.addRepository(name, url)
                showAddRepoDialog = false
            }
        )
    }
}

@Composable
fun DiscoverPluginsTab(
    plugins: List<PluginItem>,
    onInstall: (PluginItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCapability by remember { mutableStateOf<PluginCapability?>(null) }

    val filteredPlugins = remember(plugins, searchQuery, selectedCapability) {
        plugins.filter { plugin ->
            val matchesQuery = searchQuery.isBlank() ||
                    plugin.name.contains(searchQuery, ignoreCase = true) ||
                    plugin.description.contains(searchQuery, ignoreCase = true) ||
                    plugin.author.contains(searchQuery, ignoreCase = true)

            val matchesCap = selectedCapability == null || plugin.capabilities.contains(selectedCapability)
            matchesQuery && matchesCap
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        // Filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedCapability == null,
                    onClick = { selectedCapability = null },
                    label = { Text("All Extensions") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NexoraPrimaryAccent.copy(alpha = 0.2f),
                        selectedLabelColor = NexoraPrimaryAccent
                    )
                )
            }
            items(PluginCapability.values()) { cap ->
                FilterChip(
                    selected = selectedCapability == cap,
                    onClick = { selectedCapability = cap },
                    label = { Text(cap.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NexoraPrimaryAccent.copy(alpha = 0.2f),
                        selectedLabelColor = NexoraPrimaryAccent
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (filteredPlugins.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = NexoraTextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No Extensions Available",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexoraTextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Add a repository in the 'Repositories' tab to explore community extensions.",
                        fontSize = 13.sp,
                        color = NexoraTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredPlugins, key = { it.id }) { plugin ->
                    PluginCard(
                        plugin = plugin,
                        onAction = { onInstall(plugin) }
                    )
                }
            }
        }
    }
}

@Composable
fun InstalledPluginsTab(
    plugins: List<PluginItem>,
    onToggleEnable: (PluginItem, Boolean) -> Unit,
    onSync: (PluginItem) -> Unit,
    onUninstall: (PluginItem) -> Unit
) {
    if (plugins.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.FolderZip,
                    contentDescription = null,
                    tint = NexoraTextMuted,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "No Installed Extensions",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexoraTextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Discover and install extensions from configured repositories to stream extra content.",
                    fontSize = 13.sp,
                    color = NexoraTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(plugins, key = { it.id }) { plugin ->
                FocusableCard(
                    onClick = { /* View details */ },
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = NexoraSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NexoraSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!plugin.iconUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = plugin.iconUrl,
                                    contentDescription = plugin.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = NexoraPrimaryAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = plugin.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexoraTextPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "v${plugin.version}",
                                    fontSize = 11.sp,
                                    color = NexoraTextSecondary
                                )
                            }
                            Text(
                                text = "By ${plugin.author}",
                                fontSize = 12.sp,
                                color = NexoraTextMuted
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = plugin.description,
                                fontSize = 12.sp,
                                color = NexoraTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // Switch Enable/Disable
                        Switch(
                            checked = plugin.isEnabled,
                            onCheckedChange = { onToggleEnable(plugin, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NexoraPrimaryAccent,
                                checkedTrackColor = NexoraPrimaryAccent.copy(alpha = 0.3f)
                            )
                        )

                        Spacer(Modifier.width(8.dp))

                        // Sync Button
                        IconButton(onClick = { onSync(plugin) }) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "Refresh Catalog",
                                tint = NexoraPrimaryAccent
                            )
                        }

                        // Uninstall Button
                        IconButton(onClick = { onUninstall(plugin) }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Uninstall",
                                tint = NexoraError
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepositoriesTab(
    repositories: List<PluginRepositoryItem>,
    onRefresh: (PluginRepositoryItem) -> Unit,
    onDelete: (PluginRepositoryItem) -> Unit
) {
    if (repositories.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = NexoraTextMuted,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "No Repositories Configured",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexoraTextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Click 'Add Repository' to connect to an extension repository URL.",
                    fontSize = 13.sp,
                    color = NexoraTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(repositories, key = { it.id }) { repo ->
                FocusableCard(
                    onClick = { onRefresh(repo) },
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = NexoraSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NexoraSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                tint = NexoraPrimaryAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = repo.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexoraTextPrimary
                            )
                            Text(
                                text = repo.url,
                                fontSize = 11.sp,
                                color = NexoraTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${repo.pluginCount} extensions available",
                                fontSize = 11.sp,
                                color = NexoraSecondaryAccent
                            )
                        }

                        IconButton(onClick = { onRefresh(repo) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NexoraPrimaryAccent)
                        }

                        IconButton(onClick = { onDelete(repo) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NexoraError)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdatesTab(
    plugins: List<PluginItem>,
    onUpdate: (PluginItem) -> Unit
) {
    if (plugins.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = NexoraSuccess,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Extensions Up to Date",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexoraTextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "All installed media extensions are at their latest version.",
                    fontSize = 13.sp,
                    color = NexoraTextSecondary
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(plugins, key = { it.id }) { plugin ->
                FocusableCard(
                    onClick = { onUpdate(plugin) },
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = NexoraSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = plugin.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexoraTextPrimary
                            )
                            Text(
                                text = "New Version: v${plugin.version}",
                                fontSize = 12.sp,
                                color = NexoraWarning
                            )
                        }

                        Button(
                            onClick = { onUpdate(plugin) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NexoraPrimaryAccent,
                                contentColor = NexoraBackground
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Update", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PluginCard(
    plugin: PluginItem,
    onAction: () -> Unit
) {
    FocusableCard(
        onClick = onAction,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = NexoraSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NexoraSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (!plugin.iconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = plugin.iconUrl,
                            contentDescription = plugin.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Extension,
                            contentDescription = null,
                            tint = NexoraPrimaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexoraTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "v${plugin.version} • ${plugin.author}",
                        fontSize = 11.sp,
                        color = NexoraTextMuted
                    )
                }

                val isInstalled = plugin.status == PluginStatus.INSTALLED || plugin.status == PluginStatus.UPDATE_AVAILABLE
                Button(
                    onClick = onAction,
                    enabled = !isInstalled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInstalled) NexoraSurfaceElevated else NexoraPrimaryAccent,
                        contentColor = if (isInstalled) NexoraTextMuted else NexoraBackground
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isInstalled) "Installed" else "Install",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = plugin.description,
                fontSize = 12.sp,
                color = NexoraTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            // Capabilities Pills
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                plugin.capabilities.take(3).forEach { cap ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NexoraSurfaceElevated
                    ) {
                        Text(
                            text = cap.label,
                            fontSize = 10.sp,
                            color = NexoraSecondaryAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddRepositoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String) -> Unit
) {
    var repoName by remember { mutableStateOf("") }
    var repoUrl by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Extension Repository", color = NexoraTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Enter a repository JSON URL (e.g. plugins.json) to discover and install community extensions.",
                    fontSize = 13.sp,
                    color = NexoraTextSecondary
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = repoName,
                    onValueChange = { repoName = it },
                    label = { Text("Repository Name (Optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoraPrimaryAccent,
                        unfocusedBorderColor = NexoraBorder,
                        focusedTextColor = NexoraTextPrimary,
                        unfocusedTextColor = NexoraTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = repoUrl,
                    onValueChange = {
                        repoUrl = it
                        isError = false
                    },
                    label = { Text("Repository URL *") },
                    placeholder = { Text("https://example.com/plugins.json") },
                    isError = isError,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoraPrimaryAccent,
                        unfocusedBorderColor = NexoraBorder,
                        focusedTextColor = NexoraTextPrimary,
                        unfocusedTextColor = NexoraTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        text = "URL cannot be blank and must start with http:// or https://",
                        color = NexoraError,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = repoUrl.trim()
                    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                        onConfirm(repoName, trimmed)
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexoraPrimaryAccent,
                    contentColor = NexoraBackground
                )
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NexoraTextSecondary)
            }
        },
        containerColor = NexoraSurfaceElevated,
        shape = RoundedCornerShape(12.dp)
    )
}
