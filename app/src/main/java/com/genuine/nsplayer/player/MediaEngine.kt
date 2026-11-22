package com.genuine.nsplayer.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class AspectRatioMode {
    FIT,
    CROP,
    STRETCH,
    SIXTEEN_NINE,
    FOUR_THREE
}

data class TrackInfo(
    val id: String,
    val name: String,
    val language: String?,
    val isSelected: Boolean,
    val groupIndex: Int,
    val trackIndex: Int
)

@OptIn(UnstableApi::class)
class MediaEngine(private val context: Context) {

    private val trackSelector = DefaultTrackSelector(context)
    val exoPlayer: ExoPlayer

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatio: StateFlow<AspectRatioMode> = _aspectRatio.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val audioTracks: StateFlow<List<TrackInfo>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val subtitleTracks: StateFlow<List<TrackInfo>> = _subtitleTracks.asStateFlow()

    private var currentStreamUrl: String? = null
    private var currentHeaders: Map<String, String> = emptyMap()
    private var currentDrmScheme: String? = null
    private var currentDrmLicense: String? = null
    private var retryCount = 0
    private val maxRetries = 5

    init {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        // Custom buffer configuration for IPTV streaming stability
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000, // minBufferMs
                50_000, // maxBufferMs
                2_500,  // bufferForPlaybackMs
                5_000   // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        exoPlayer = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()

        setupPlayerListener()
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        _isBuffering.value = true
                        _playbackError.value = null
                    }
                    Player.STATE_READY -> {
                        _isBuffering.value = false
                        _playbackError.value = null
                        retryCount = 0
                        _duration.value = if (exoPlayer.isCurrentMediaItemLive) -1L else exoPlayer.duration
                        updateAvailableTracks()
                    }
                    Player.STATE_ENDED -> {
                        _isBuffering.value = false
                    }
                    Player.STATE_IDLE -> {
                        _isBuffering.value = false
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                _isBuffering.value = false
                val errorMsg = error.localizedMessage ?: "Unknown playback error (${error.errorCode})"
                _playbackError.value = errorMsg

                // Automatic reconnect with exponential backoff
                if (retryCount < maxRetries && currentStreamUrl != null) {
                    retryCount++
                    val delayMs = (1000L * (1 shl (retryCount - 1))).coerceAtMost(10_000L)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        retryPlayback()
                    }, delayMs)
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateAvailableTracks()
            }
        })
    }

    fun play(
        url: String,
        headers: Map<String, String> = emptyMap(),
        drmScheme: String? = null,
        drmLicense: String? = null,
        startPositionMs: Long = 0L
    ) {
        currentStreamUrl = url
        currentHeaders = headers
        currentDrmScheme = drmScheme
        currentDrmLicense = drmLicense
        retryCount = 0
        _playbackError.value = null

        val mediaSource = buildMediaSource(url, headers, drmScheme, drmLicense)
        exoPlayer.setMediaSource(mediaSource)
        if (startPositionMs > 0) {
            exoPlayer.seekTo(startPositionMs)
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun retryPlayback() {
        val url = currentStreamUrl ?: return
        val currentPos = exoPlayer.currentPosition
        play(
            url = url,
            headers = currentHeaders,
            drmScheme = currentDrmScheme,
            drmLicense = currentDrmLicense,
            startPositionMs = currentPos
        )
    }

    private fun buildMediaSource(
        url: String,
        headers: Map<String, String>,
        drmScheme: String?,
        drmLicense: String?
    ): MediaSource {
        val uri = Uri.parse(url)

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)

        // Set User-Agent and custom HTTP headers
        val userAgent = headers["User-Agent"] ?: "NSPlayer/1.0 (Android)"
        httpDataSourceFactory.setUserAgent(userAgent)

        val defaultRequestProperties = mutableMapOf<String, String>()
        headers.forEach { (k, v) ->
            if (!k.equals("User-Agent", ignoreCase = true)) {
                defaultRequestProperties[k] = v
            }
        }
        httpDataSourceFactory.setDefaultRequestProperties(defaultRequestProperties)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val mediaItemBuilder = MediaItem.Builder().setUri(uri)

        // DRM Configuration (Widevine, ClearKey, PlayReady)
        if (!drmScheme.isNullOrBlank() && !drmLicense.isNullOrBlank()) {
            val schemeUuid = when (drmScheme.lowercase()) {
                "widevine" -> C.WIDEVINE_UUID
                "playready" -> C.PLAYREADY_UUID
                "clearkey" -> C.CLEARKEY_UUID
                else -> UUID.fromString(drmScheme)
            }
            mediaItemBuilder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(schemeUuid)
                    .setLicenseUri(drmLicense)
                    .build()
            )
        }

        val mediaItem = mediaItemBuilder.build()

        return when {
            url.contains(".m3u8", ignoreCase = true) || url.contains("m3u8", ignoreCase = true) -> {
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            }
            else -> {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _aspectRatio.value = mode
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekRelative(offsetMs: Long) {
        if (!exoPlayer.isCurrentMediaItemLive) {
            val newPos = (exoPlayer.currentPosition + offsetMs).coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
            exoPlayer.seekTo(newPos)
        }
    }

    fun selectAudioTrack(trackInfo: TrackInfo) {
        val parameters = trackSelector.buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)

        val trackGroups = exoPlayer.currentTracks.groups
        if (trackInfo.groupIndex in trackGroups.indices) {
            val group = trackGroups[trackInfo.groupIndex]
            parameters.addOverride(
                TrackSelectionOverride(group.mediaTrackGroup, listOf(trackInfo.trackIndex))
            )
        }
        trackSelector.setParameters(parameters)
    }

    fun selectSubtitleTrack(trackInfo: TrackInfo?) {
        val parameters = trackSelector.buildUponParameters()
        if (trackInfo == null) {
            // Disable subtitles
            parameters.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
        } else {
            parameters.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            val trackGroups = exoPlayer.currentTracks.groups
            if (trackInfo.groupIndex in trackGroups.indices) {
                val group = trackGroups[trackInfo.groupIndex]
                parameters.addOverride(
                    TrackSelectionOverride(group.mediaTrackGroup, listOf(trackInfo.trackIndex))
                )
            }
        }
        trackSelector.setParameters(parameters)
    }

    private fun updateAvailableTracks() {
        val audioList = mutableListOf<TrackInfo>()
        val subtitleList = mutableListOf<TrackInfo>()

        val trackGroups = exoPlayer.currentTracks.groups
        for (gIndex in trackGroups.indices) {
            val group = trackGroups[gIndex]
            val trackType = group.type

            for (tIndex in 0 until group.length) {
                val isSelected = group.isTrackSelected(tIndex)
                val format = group.getTrackFormat(tIndex)
                val lang = format.language
                val label = format.label ?: lang ?: "Track ${tIndex + 1}"

                val info = TrackInfo(
                    id = format.id ?: "$gIndex-$tIndex",
                    name = label,
                    language = lang,
                    isSelected = isSelected,
                    groupIndex = gIndex,
                    trackIndex = tIndex
                )

                if (trackType == C.TRACK_TYPE_AUDIO) {
                    audioList.add(info)
                } else if (trackType == C.TRACK_TYPE_TEXT) {
                    subtitleList.add(info)
                }
            }
        }

        _audioTracks.value = audioList
        _subtitleTracks.value = subtitleList
    }

    fun release() {
        exoPlayer.release()
    }
}
