package com.genuine.nsplayer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.genuine.nsplayer.core.PreferencesManager
import com.genuine.nsplayer.data.local.AppDatabase
import com.genuine.nsplayer.data.model.*
import com.genuine.nsplayer.data.repository.*
import com.genuine.nsplayer.player.AspectRatioMode
import com.genuine.nsplayer.player.MediaEngine
import com.genuine.nsplayer.player.TrackInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val preferences = PreferencesManager(application)
    val playlistRepo = PlaylistRepository(application, db.playlistDao(), db.channelDao(), db.epgDao())
    val channelRepo = ChannelRepository(db.channelDao(), db.epgDao())
    val historyRepo = HistoryRepository(db.historyDao())
    val pluginRepo = PluginRepositoryManager(application, db.pluginDao())

    // Playlists
    val playlists: StateFlow<List<Playlist>> = playlistRepo.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Extensions & Plugins Ecosystem
    val repositories: StateFlow<List<PluginRepositoryItem>> = pluginRepo.getAllRepositories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlugins: StateFlow<List<PluginItem>> = pluginRepo.getAllPlugins()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val installedPlugins: StateFlow<List<PluginItem>> = pluginRepo.getInstalledPlugins()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importProgress = MutableStateFlow<Int?>(null)
    val importProgress: StateFlow<Int?> = _importProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    // Active Selection
    private val _activePlaylistId = MutableStateFlow<Long?>(null)
    val activePlaylistId: StateFlow<Long?> = _activePlaylistId.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>("All")
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedContentType = MutableStateFlow<ContentType?>(null)
    val selectedContentType: StateFlow<ContentType?> = _selectedContentType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Continue Watching & History
    val continueWatching: StateFlow<List<PlaybackHistory>> = historyRepo.getContinueWatching()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<PlaybackHistory>> = historyRepo.getHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Channel>> = channelRepo.getFavoriteChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Channels Flow based on active playlist, category, type and search query
    val categories: StateFlow<List<String>> = _activePlaylistId.flatMapLatest { id ->
        if (id != null) channelRepo.getCategories(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: StateFlow<List<Channel>> = combine(
        _activePlaylistId,
        _selectedCategory,
        _selectedContentType,
        _searchQuery
    ) { playlistId, category, contentType, query ->
        when {
            query.isNotBlank() -> channelRepo.searchChannels(query)
            playlistId == null -> channelRepo.getFavoriteChannels()
            category != null && category != "All" -> channelRepo.getChannelsByGroup(playlistId, category)
            contentType != null -> channelRepo.getChannelsByType(playlistId, contentType)
            else -> channelRepo.getChannelsByPlaylist(playlistId)
        }
    }.flatMapLatest { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically select the first available playlist if active playlist is null
        viewModelScope.launch {
            playlists.collect { list ->
                if (_activePlaylistId.value == null && list.isNotEmpty()) {
                    _activePlaylistId.value = list.first().id
                }
            }
        }
    }

    fun selectPlaylist(id: Long) {
        _activePlaylistId.value = id
        _selectedCategory.value = "All"
        _searchQuery.value = ""
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        _selectedContentType.value = null
    }

    fun selectContentType(type: ContentType?) {
        _selectedContentType.value = type
        _selectedCategory.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addPlaylist(name: String, url: String?, filePath: String?, userAgent: String?) {
        viewModelScope.launch {
            try {
                _importProgress.value = 0
                _errorMessage.value = null
                val newId = playlistRepo.addOrUpdatePlaylist(name, url, filePath, userAgent) { count ->
                    _importProgress.value = count
                }
                _activePlaylistId.value = newId
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to import playlist"
            } finally {
                _importProgress.value = null
            }
        }
    }

    fun refreshPlaylist(id: Long) {
        viewModelScope.launch {
            try {
                _importProgress.value = 0
                _errorMessage.value = null
                playlistRepo.refreshPlaylistChannels(id) { count ->
                    _importProgress.value = count
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to refresh playlist"
            } finally {
                _importProgress.value = null
            }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            playlistRepo.deletePlaylist(id)
            if (_activePlaylistId.value == id) {
                _activePlaylistId.value = playlists.value.firstOrNull { it.id != id }?.id
            }
        }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            channelRepo.toggleFavorite(channel.id, !channel.isFavorite)
        }
    }

    // Extensions & Repositories operations
    fun addRepository(name: String, url: String) {
        viewModelScope.launch {
            try {
                _importProgress.value = 0
                _errorMessage.value = null
                pluginRepo.addRepository(name, url)
                _infoMessage.value = "Repository added successfully."
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add repository: ${e.localizedMessage}"
            } finally {
                _importProgress.value = null
            }
        }
    }

    fun refreshRepository(repoId: Long) {
        viewModelScope.launch {
            try {
                _importProgress.value = 0
                _errorMessage.value = null
                pluginRepo.refreshRepository(repoId)
                _infoMessage.value = "Repository refreshed successfully."
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh repository: ${e.localizedMessage}"
            } finally {
                _importProgress.value = null
            }
        }
    }

    fun deleteRepository(repoId: Long) {
        viewModelScope.launch {
            try {
                pluginRepo.deleteRepository(repoId)
                _infoMessage.value = "Repository removed."
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete repository: ${e.localizedMessage}"
            }
        }
    }

    fun installPlugin(pluginId: String) {
        viewModelScope.launch {
            try {
                pluginRepo.installPlugin(pluginId)
                _infoMessage.value = "Extension installed."
            } catch (e: Exception) {
                _errorMessage.value = "Failed to install extension: ${e.localizedMessage}"
            }
        }
    }

    fun updatePlugin(pluginId: String) {
        viewModelScope.launch {
            try {
                pluginRepo.updatePlugin(pluginId)
                _infoMessage.value = "Extension updated."
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update extension: ${e.localizedMessage}"
            }
        }
    }

    fun togglePluginEnabled(pluginId: String, enabled: Boolean) {
        viewModelScope.launch {
            pluginRepo.setPluginEnabled(pluginId, enabled)
        }
    }

    fun uninstallPlugin(pluginId: String) {
        viewModelScope.launch {
            try {
                pluginRepo.uninstallPlugin(pluginId)
                _infoMessage.value = "Extension uninstalled."
            } catch (e: Exception) {
                _errorMessage.value = "Failed to uninstall extension: ${e.localizedMessage}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearInfo() {
        _infoMessage.value = null
    }
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val historyRepo = HistoryRepository(db.historyDao())
    private val channelRepo = ChannelRepository(db.channelDao(), db.epgDao())
    val mediaEngine = MediaEngine(application)

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    private val _playlistChannels = MutableStateFlow<List<Channel>>(emptyList())
    val playlistChannels: StateFlow<List<Channel>> = _playlistChannels.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private var sleepTimerJob: Job? = null

    fun loadPlaylistChannels(playlistId: Long) {
        if (playlistId > 0) {
            viewModelScope.launch {
                channelRepo.getChannelsByPlaylist(playlistId).collect { list ->
                    _playlistChannels.value = list
                }
            }
        }
    }

    fun playNextChannel() {
        val list = _playlistChannels.value
        val cur = _currentChannel.value ?: return
        if (list.isNotEmpty()) {
            val idx = list.indexOfFirst { it.streamUrl == cur.streamUrl }
            val nextIdx = if (idx >= 0 && idx + 1 < list.size) idx + 1 else 0
            playChannel(list[nextIdx])
        }
    }

    fun playPreviousChannel() {
        val list = _playlistChannels.value
        val cur = _currentChannel.value ?: return
        if (list.isNotEmpty()) {
            val idx = list.indexOfFirst { it.streamUrl == cur.streamUrl }
            val prevIdx = if (idx > 0) idx - 1 else list.size - 1
            playChannel(list[prevIdx])
        }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            val newFav = !channel.isFavorite
            channelRepo.toggleFavorite(channel.id, newFav)
            _currentChannel.value = _currentChannel.value?.copy(isFavorite = newFav)
        }
    }

    fun playChannel(channel: Channel, startPositionMs: Long = 0L) {
        _currentChannel.value = channel
        mediaEngine.play(
            url = channel.streamUrl,
            headers = channel.headers,
            drmScheme = channel.drmScheme,
            drmLicense = channel.drmLicense,
            startPositionMs = startPositionMs
        )

        // Record history
        viewModelScope.launch {
            historyRepo.recordHistory(
                channelId = channel.id,
                streamUrl = channel.streamUrl,
                title = channel.name,
                logoUrl = channel.logoUrl,
                positionMs = startPositionMs,
                durationMs = 0L
            )
        }
    }

    fun playDirectStream(
        url: String,
        headers: Map<String, String> = emptyMap(),
        drmScheme: String? = null,
        drmLicense: String? = null
    ) {
        val dummyChannel = Channel(
            playlistId = -1,
            name = "Direct Stream",
            streamUrl = url,
            headers = headers,
            drmScheme = drmScheme,
            drmLicense = drmLicense
        )
        _currentChannel.value = dummyChannel
        mediaEngine.play(url, headers, drmScheme, drmLicense)

        viewModelScope.launch {
            historyRepo.recordHistory(
                channelId = null,
                streamUrl = url,
                title = "Direct Stream",
                logoUrl = null,
                positionMs = 0L,
                durationMs = 0L
            )
        }
    }

    fun updatePlaybackPosition() {
        val channel = _currentChannel.value ?: return
        val pos = mediaEngine.exoPlayer.currentPosition
        val dur = mediaEngine.exoPlayer.duration
        if (dur > 0 && pos > 0) {
            viewModelScope.launch {
                historyRepo.recordHistory(
                    channelId = if (channel.id > 0) channel.id else null,
                    streamUrl = channel.streamUrl,
                    title = channel.name,
                    logoUrl = channel.logoUrl,
                    positionMs = pos,
                    durationMs = dur
                )
            }
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes

        if (minutes != null && minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                delay(minutes * 60_000L)
                mediaEngine.exoPlayer.pause()
                _sleepTimerMinutes.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        updatePlaybackPosition()
        mediaEngine.release()
    }
}
