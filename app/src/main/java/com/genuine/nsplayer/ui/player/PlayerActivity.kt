package com.genuine.nsplayer.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.genuine.nsplayer.core.DeviceDetector
import com.genuine.nsplayer.core.DeviceMode
import com.genuine.nsplayer.data.model.Channel
import com.genuine.nsplayer.player.AspectRatioMode
import com.genuine.nsplayer.player.TrackInfo
import com.genuine.nsplayer.ui.common.FocusableCard
import com.genuine.nsplayer.ui.theme.*
import com.genuine.nsplayer.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

/**
 * Bridge for Android TV remote D-pad and media key events
 */
class RemoteControlState {
    var onDpadCenter: (() -> Boolean)? = null
    var onDpadUp: (() -> Boolean)? = null
    var onDpadDown: (() -> Boolean)? = null
    var onDpadLeft: (() -> Boolean)? = null
    var onDpadRight: (() -> Boolean)? = null
    var onBackPressed: (() -> Boolean)? = null
    var onVolumeUp: (() -> Boolean)? = null
    var onVolumeDown: (() -> Boolean)? = null
    var onVolumeMute: (() -> Boolean)? = null
    var onMenu: (() -> Boolean)? = null
    var onAnyKey: (() -> Unit)? = null
}

class PlayerActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()
    val remoteControlState = RemoteControlState()

    companion object {
        const val EXTRA_STREAM_URL = "extra_stream_url"
        const val EXTRA_CHANNEL_NAME = "extra_channel_name"
        const val EXTRA_CHANNEL_LOGO = "extra_channel_logo"
        const val EXTRA_GROUP_TITLE = "extra_group_title"
        const val EXTRA_PLAYLIST_ID = "extra_playlist_id"
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
        const val EXTRA_DRM_SCHEME = "extra_drm_scheme"
        const val EXTRA_DRM_LICENSE = "extra_drm_license"

        fun launch(
            context: Context,
            channel: Channel
        ) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_STREAM_URL, channel.streamUrl)
                putExtra(EXTRA_CHANNEL_NAME, channel.name)
                putExtra(EXTRA_CHANNEL_LOGO, channel.logoUrl)
                putExtra(EXTRA_GROUP_TITLE, channel.groupTitle)
                putExtra(EXTRA_PLAYLIST_ID, channel.playlistId)
                putExtra(EXTRA_CHANNEL_ID, channel.id)
                putExtra(EXTRA_DRM_SCHEME, channel.drmScheme)
                putExtra(EXTRA_DRM_LICENSE, channel.drmLicense)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Lock to landscape on phones/tablets for optimal viewing
        val isTV = DeviceDetector.getDeviceMode(this) == DeviceMode.TELEVISION
        if (!isTV) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL)
        val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "Live Stream"
        val channelLogo = intent.getStringExtra(EXTRA_CHANNEL_LOGO)
        val groupTitle = intent.getStringExtra(EXTRA_GROUP_TITLE) ?: "Live TV"
        val playlistId = intent.getLongExtra(EXTRA_PLAYLIST_ID, 0L)
        val channelId = intent.getLongExtra(EXTRA_CHANNEL_ID, 0L)
        val drmScheme = intent.getStringExtra(EXTRA_DRM_SCHEME)
        val drmLicense = intent.getStringExtra(EXTRA_DRM_LICENSE)

        if (playlistId > 0) {
            playerViewModel.loadPlaylistChannels(playlistId)
        }

        if (!streamUrl.isNullOrBlank()) {
            val channel = Channel(
                id = channelId,
                playlistId = playlistId,
                name = channelName,
                streamUrl = streamUrl,
                logoUrl = channelLogo,
                groupTitle = groupTitle,
                drmScheme = drmScheme,
                drmLicense = drmLicense
            )
            playerViewModel.playChannel(channel)
        }

        setContent {
            NSPlayerTheme {
                PlayerScreen(
                    viewModel = playerViewModel,
                    remoteControlState = remoteControlState,
                    onEnterPip = { enterPipMode() },
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        remoteControlState.onAnyKey?.invoke()

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (remoteControlState.onDpadCenter?.invoke() == true) return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (remoteControlState.onDpadDown?.invoke() == true) return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (remoteControlState.onDpadUp?.invoke() == true) return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (remoteControlState.onDpadLeft?.invoke() == true) return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (remoteControlState.onDpadRight?.invoke() == true) return true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (remoteControlState.onBackPressed?.invoke() == true) return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (remoteControlState.onVolumeUp?.invoke() == true) return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (remoteControlState.onVolumeDown?.invoke() == true) return true
            }
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                if (remoteControlState.onVolumeMute?.invoke() == true) return true
            }
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO, KeyEvent.KEYCODE_GUIDE -> {
                if (remoteControlState.onMenu?.invoke() == true) return true
            }
            KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_PAGE_UP -> {
                playerViewModel.playNextChannel()
                return true
            }
            KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_PAGE_DOWN -> {
                playerViewModel.playPreviousChannel()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                playerViewModel.mediaEngine.togglePlayPause()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                playerViewModel.mediaEngine.exoPlayer.play()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                playerViewModel.mediaEngine.exoPlayer.pause()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                playerViewModel.mediaEngine.seekRelative(10_000)
                return true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                playerViewModel.mediaEngine.seekRelative(-10_000)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) {
            // Keep playing in PiP
        } else {
            playerViewModel.mediaEngine.exoPlayer.pause()
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    remoteControlState: RemoteControlState,
    onEnterPip: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val mediaEngine = viewModel.mediaEngine
    val channel by viewModel.currentChannel.collectAsState()
    val playlistChannels by viewModel.playlistChannels.collectAsState()
    val isPlaying by mediaEngine.isPlaying.collectAsState()
    val isBuffering by mediaEngine.isBuffering.collectAsState()
    val error by mediaEngine.playbackError.collectAsState()
    val aspectRatioMode by mediaEngine.aspectRatio.collectAsState()
    val audioTracks by mediaEngine.audioTracks.collectAsState()
    val subtitleTracks by mediaEngine.subtitleTracks.collectAsState()
    val sleepTimer by viewModel.sleepTimerMinutes.collectAsState()

    // Audio & Volume Management
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var currentVolume by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    var lastNonZeroVolume by remember { mutableStateOf(if (currentVolume > 0) currentVolume else (maxVolume / 2)) }

    // Brightness Management
    var currentBrightness by remember {
        val cur = activity?.window?.attributes?.screenBrightness ?: 0.5f
        mutableStateOf(if (cur < 0.05f) 0.5f else cur)
    }

    // HUD Notification State
    var hudText by remember { mutableStateOf<String?>(null) }
    var hudIcon by remember { mutableStateOf(Icons.AutoMirrored.Filled.VolumeUp) }
    var hudProgress by remember { mutableStateOf(0.5f) }

    fun triggerHud(text: String, icon: ImageVector, progress: Float) {
        hudText = text
        hudIcon = icon
        hudProgress = progress.coerceIn(0f, 1f)
    }

    fun setVolume(newVal: Int) {
        val clamped = newVal.coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
        currentVolume = clamped
        if (clamped > 0) lastNonZeroVolume = clamped
        val pct = (clamped * 100) / maxVolume
        val icon = if (clamped == 0) Icons.AutoMirrored.Filled.VolumeMute else if (pct < 50) Icons.AutoMirrored.Filled.VolumeDown else Icons.AutoMirrored.Filled.VolumeUp
        triggerHud("Volume: $pct%", icon, clamped.toFloat() / maxVolume)
    }

    fun toggleMute() {
        if (currentVolume > 0) {
            lastNonZeroVolume = currentVolume
            setVolume(0)
        } else {
            setVolume(if (lastNonZeroVolume > 0) lastNonZeroVolume else (maxVolume / 2))
        }
    }

    fun setBrightness(newVal: Float) {
        val clamped = newVal.coerceIn(0.05f, 1.0f)
        currentBrightness = clamped
        activity?.window?.let { win ->
            val lp = win.attributes
            lp.screenBrightness = clamped
            win.attributes = lp
        }
        val pct = (clamped * 100).toInt()
        val icon = if (pct < 40) Icons.Default.BrightnessLow else if (pct < 75) Icons.Default.BrightnessMedium else Icons.Default.BrightnessHigh
        triggerHud("Brightness: $pct%", icon, clamped)
    }

    // UI Dialog & Drawer States
    var showControls by remember { mutableStateOf(true) }
    var showTrackDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showChannelDrawer by remember { mutableStateOf(false) }

    // Fast seek visual animation state
    var fastSeekText by remember { mutableStateOf<String?>(null) }

    // Activity key & focus management
    var autoHideCounter by remember { mutableIntStateOf(0) }
    val playButtonFocusRequester = remember { FocusRequester() }

    // Auto-hide controls after 6 seconds of inactivity (resets on any user interaction)
    LaunchedEffect(showControls, isPlaying, autoHideCounter) {
        if (showControls && isPlaying && !showChannelDrawer && !showTrackDialog && !showSleepTimerDialog) {
            delay(6000)
            showControls = false
        }
    }

    // Auto-focus on primary play control when OSD appears
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(80)
            try {
                playButtonFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Auto-hide floating HUD
    LaunchedEffect(hudText) {
        if (hudText != null) {
            delay(1500)
            hudText = null
        }
    }

    // Bind hardware remote controller state to player state
    DisposableEffect(remoteControlState, showControls, showChannelDrawer, showTrackDialog, showSleepTimerDialog) {
        remoteControlState.onAnyKey = {
            autoHideCounter++
        }

        remoteControlState.onDpadCenter = {
            if (!showControls && !showChannelDrawer && !showTrackDialog && !showSleepTimerDialog) {
                showControls = true
                true
            } else {
                false // Handled by currently focused UI element
            }
        }

        remoteControlState.onDpadDown = {
            if (!showControls && !showChannelDrawer && !showTrackDialog && !showSleepTimerDialog) {
                showChannelDrawer = true
                true
            } else {
                false
            }
        }

        remoteControlState.onDpadUp = {
            if (!showControls && !showChannelDrawer && !showTrackDialog && !showSleepTimerDialog) {
                showControls = true
                true
            } else {
                false
            }
        }

        remoteControlState.onDpadLeft = {
            if (!showControls && !showChannelDrawer && !showTrackDialog && !showSleepTimerDialog) {
                mediaEngine.seekRelative(-10_000)
                fastSeekText = "-10s"
                true
            } else {
                false
            }
        }

        remoteControlState.onDpadRight = {
            if (!showControls && !showChannelDrawer && !showTrackDialog && !showSleepTimerDialog) {
                mediaEngine.seekRelative(10_000)
                fastSeekText = "+10s"
                true
            } else {
                false
            }
        }

        remoteControlState.onBackPressed = {
            when {
                showTrackDialog -> {
                    showTrackDialog = false
                    true
                }
                showSleepTimerDialog -> {
                    showSleepTimerDialog = false
                    true
                }
                showChannelDrawer -> {
                    showChannelDrawer = false
                    true
                }
                showControls -> {
                    showControls = false
                    true
                }
                else -> false // Let system exit activity
            }
        }

        remoteControlState.onVolumeUp = {
            setVolume(currentVolume + 1)
            true
        }

        remoteControlState.onVolumeDown = {
            setVolume(currentVolume - 1)
            true
        }

        remoteControlState.onVolumeMute = {
            toggleMute()
            true
        }

        remoteControlState.onMenu = {
            showControls = !showControls
            true
        }

        onDispose {
            remoteControlState.onAnyKey = null
            remoteControlState.onDpadCenter = null
            remoteControlState.onDpadDown = null
            remoteControlState.onDpadUp = null
            remoteControlState.onDpadLeft = null
            remoteControlState.onDpadRight = null
            remoteControlState.onBackPressed = null
            remoteControlState.onVolumeUp = null
            remoteControlState.onVolumeDown = null
            remoteControlState.onVolumeMute = null
            remoteControlState.onMenu = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = { offset ->
                        val isRightSide = offset.x > size.width / 2
                        val seekAmount = if (isRightSide) 10_000L else -10_000L
                        mediaEngine.seekRelative(seekAmount)
                        fastSeekText = if (isRightSide) "+10s" else "-10s"
                    }
                )
            }
    ) {
        // ExoPlayer Media Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = mediaEngine.exoPlayer
                    useController = false
                    resizeMode = when (aspectRatioMode) {
                        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatioMode.SIXTEEN_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                        AspectRatioMode.FOUR_THREE -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                    }
                }
            },
            update = { playerView ->
                playerView.resizeMode = when (aspectRatioMode) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.SIXTEEN_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    AspectRatioMode.FOUR_THREE -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }
            }
        }

        // Fast Seek Indicator HUD
        if (fastSeekText != null) {
            LaunchedEffect(fastSeekText) {
                delay(800)
                fastSeekText = null
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, NSBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (fastSeekText == "+10s") Icons.Default.Forward10 else Icons.Default.Replay10,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = fastSeekText ?: "",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Floating Center HUD for Volume / Brightness / Aspect Feedback
        if (hudText != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 70.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xDD12151F),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                    shadowElevation = 10.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(hudIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = hudText ?: "",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { hudProgress },
                            modifier = Modifier
                                .width(140.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.DarkGray
                        )
                    }
                }
            }
        }

        // Playback Error Banner
        if (error != null) {
            val retryFocusRequester = remember { FocusRequester() }
            LaunchedEffect(error) {
                delay(100)
                try {
                    retryFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xF01A0B10),
                    border = BorderStroke(1.5.dp, NSError),
                    modifier = Modifier.widthIn(max = 480.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = NSError, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Stream Playback Error", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = error ?: "Unable to connect to streaming server",
                            color = NSTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(18.dp))
                        TvTextButton(
                            text = "Retry Connection",
                            icon = Icons.Default.Refresh,
                            onClick = { mediaEngine.retryPlayback() },
                            focusRequester = retryFocusRequester
                        )
                    }
                }
            }
        }

        // Complete Glassmorphic On-Screen Display (OSD) Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(250))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                // Top Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .align(Alignment.TopCenter)
                        .background(PlayerScrimTop)
                )

                // Bottom Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .align(Alignment.BottomCenter)
                        .background(PlayerScrimBottom)
                )

                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Back button + Channel Identity
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        TvIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            size = 42.dp,
                            iconSize = 22.dp,
                            onClick = onClose
                        )

                        Spacer(Modifier.width(14.dp))

                        if (!channel?.logoUrl.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x88090A0F),
                                border = BorderStroke(1.dp, NSBorder),
                                modifier = Modifier.size(44.dp)
                            ) {
                                AsyncImage(
                                    model = channel?.logoUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = channel?.name ?: "Network Stream",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width(8.dp))
                                // Live Tag
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = NSLiveRed
                                ) {
                                    Text(
                                        text = "LIVE",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = channel?.groupTitle ?: "Live TV",
                                color = NSTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Right: Quick Function Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Aspect Ratio Toggle
                        TvTextButton(
                            text = aspectRatioMode.name,
                            icon = Icons.Default.AspectRatio,
                            onClick = {
                                val nextMode = when (aspectRatioMode) {
                                    AspectRatioMode.FIT -> AspectRatioMode.SIXTEEN_NINE
                                    AspectRatioMode.SIXTEEN_NINE -> AspectRatioMode.CROP
                                    AspectRatioMode.CROP -> AspectRatioMode.STRETCH
                                    AspectRatioMode.STRETCH -> AspectRatioMode.FOUR_THREE
                                    AspectRatioMode.FOUR_THREE -> AspectRatioMode.FIT
                                }
                                mediaEngine.setAspectRatio(nextMode)
                                triggerHud("Aspect: $nextMode", Icons.Default.AspectRatio, 1.0f)
                            }
                        )

                        // Tracks (Audio / Subtitles)
                        TvIconButton(
                            icon = Icons.Default.Subtitles,
                            contentDescription = "Audio and Subtitles",
                            onClick = { showTrackDialog = true }
                        )

                        // Sleep Timer
                        TvIconButton(
                            icon = Icons.Default.Timer,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimer != null) MaterialTheme.colorScheme.primary else Color.White,
                            onClick = { showSleepTimerDialog = true }
                        )

                        // Picture-in-Picture
                        TvIconButton(
                            icon = Icons.Default.PictureInPicture,
                            contentDescription = "Picture in Picture",
                            onClick = onEnterPip
                        )
                    }
                }

                // Center Play / Rewind / Fast-Forward Transport Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    // Rewind 10s Button
                    TvIconButton(
                        icon = Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        size = 56.dp,
                        iconSize = 30.dp,
                        onClick = {
                            mediaEngine.seekRelative(-10_000)
                            fastSeekText = "-10s"
                        }
                    )

                    // Main Glassmorphic Play/Pause Button (Primary initial focus on TV)
                    TvIconButton(
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        size = 80.dp,
                        iconSize = 46.dp,
                        focusedBorderColor = Color.White,
                        focusedBackgroundColor = MaterialTheme.colorScheme.primary,
                        defaultBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        focusRequester = playButtonFocusRequester,
                        onClick = { mediaEngine.togglePlayPause() }
                    )

                    // Forward 10s Button
                    TvIconButton(
                        icon = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        size = 56.dp,
                        iconSize = 30.dp,
                        onClick = {
                            mediaEngine.seekRelative(10_000)
                            fastSeekText = "+10s"
                        }
                    )
                }

                // Bottom Controls Cluster: Manual Volume & Brightness + Channels & Zap
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // MANUAL ON-SCREEN CONTROLS: Remote D-pad + Touch Enabled
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xEE12151F))
                            .border(1.dp, NSBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Section: Manual Brightness Control
                        TvLevelControl(
                            label = "Brightness",
                            icon = if (currentBrightness < 0.4f) Icons.Default.BrightnessLow else if (currentBrightness < 0.75f) Icons.Default.BrightnessMedium else Icons.Default.BrightnessHigh,
                            tint = NSWarning,
                            currentFraction = currentBrightness,
                            displayText = "${(currentBrightness * 100).toInt()}%",
                            onStepDown = { setBrightness(currentBrightness - 0.10f) },
                            onStepUp = { setBrightness(currentBrightness + 0.10f) },
                            onSetFraction = { setBrightness(it) },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(24.dp))

                        // Right Section: Manual Volume Control
                        TvLevelControl(
                            label = "Volume",
                            icon = if (currentVolume == 0) Icons.AutoMirrored.Filled.VolumeMute else if ((currentVolume * 100) / maxVolume < 50) Icons.AutoMirrored.Filled.VolumeDown else Icons.AutoMirrored.Filled.VolumeUp,
                            tint = MaterialTheme.colorScheme.primary,
                            currentFraction = currentVolume.toFloat() / maxVolume.toFloat(),
                            displayText = "${(currentVolume * 100) / maxVolume}%",
                            onIconClick = { toggleMute() },
                            onStepDown = { setVolume(currentVolume - 1) },
                            onStepUp = { setVolume(currentVolume + 1) },
                            onSetFraction = { fraction -> setVolume((fraction * maxVolume).toInt()) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Bottom Navigation: Previous/Next Channel + Drawer + Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Channel Switching Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Previous Channel Button
                            TvTextButton(
                                text = "Prev",
                                icon = Icons.Default.SkipPrevious,
                                onClick = { viewModel.playPreviousChannel() }
                            )

                            // Next Channel Button
                            TvTextButton(
                                text = "Next",
                                icon = Icons.Default.SkipNext,
                                onClick = { viewModel.playNextChannel() }
                            )

                            // In-Player Quick Channels Drawer Button
                            TvTextButton(
                                text = "Channels",
                                icon = Icons.Default.Tv,
                                textColor = MaterialTheme.colorScheme.primary,
                                onClick = { showChannelDrawer = true }
                            )

                            // Favorite Toggle
                            channel?.let { ch ->
                                TvIconButton(
                                    icon = if (ch.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "Favorite",
                                    tint = if (ch.isFavorite) NSGold else Color.White.copy(alpha = 0.7f),
                                    size = 40.dp,
                                    iconSize = 22.dp,
                                    onClick = { viewModel.toggleFavorite(ch) }
                                )
                            }
                        }

                        // Stream Specs Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (sleepTimer != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(
                                        text = "Sleep: ${sleepTimer}m",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x66161B26),
                                border = BorderStroke(1.dp, NSBorder)
                            ) {
                                Text(
                                    text = "HLS Live • Media3 ExoPlayer",
                                    color = NSTextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // In-Player Quick Channels Drawer (Fully D-Pad Remote Navigable)
    if (showChannelDrawer) {
        InPlayerChannelDrawer(
            channels = playlistChannels,
            currentChannel = channel,
            onSelectChannel = { selected ->
                viewModel.playChannel(selected)
                showChannelDrawer = false
            },
            onDismiss = { showChannelDrawer = false }
        )
    }

    // Audio & Subtitle Tracks Dialog (Remote Navigable)
    if (showTrackDialog) {
        TrackSelectionDialog(
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            onSelectAudio = { mediaEngine.selectAudioTrack(it) },
            onSelectSubtitle = { mediaEngine.selectSubtitleTrack(it) },
            onDismiss = { showTrackDialog = false }
        )
    }

    // Sleep Timer Dialog (Remote Navigable)
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentMinutes = sleepTimer,
            onSelect = {
                viewModel.setSleepTimer(it)
                showSleepTimerDialog = false
            },
            onDismiss = { showSleepTimerDialog = false }
        )
    }
}

/**
 * High-contrast, focus-scaled icon button engineered for Android TV remote D-pads and touch
 */
@Composable
fun TvIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    tint: Color = Color.White,
    focusedBorderColor: Color = MaterialTheme.colorScheme.primary,
    focusedBackgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
    defaultBackgroundColor: Color = Color(0x66161B26),
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "BtnScale"
    )

    Surface(
        shape = shape,
        color = if (isFocused) focusedBackgroundColor else defaultBackgroundColor,
        border = BorderStroke(if (isFocused) 2.5.dp else 1.dp, if (isFocused) focusedBorderColor else NSBorder),
        shadowElevation = if (isFocused) 12.dp else 2.dp,
        modifier = modifier
            .size(size)
            .scale(scale)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    onClick()
                    true
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isFocused) Color.White else tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Focus-highlighted text button for TV remotes
 */
@Composable
fun TvTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(10.dp),
    textColor: Color = Color.White,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "TextBtnScale"
    )

    Surface(
        shape = shape,
        color = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color(0x66161B26),
        border = BorderStroke(if (isFocused) 2.5.dp else 1.dp, if (isFocused) MaterialTheme.colorScheme.primary else NSBorder),
        shadowElevation = if (isFocused) 10.dp else 2.dp,
        modifier = modifier
            .scale(scale)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    onClick()
                    true
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) MaterialTheme.colorScheme.primary else textColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = if (isFocused) Color.White else textColor,
                fontSize = 12.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

/**
 * Universal Level Control (Volume & Brightness):
 * - Operated on TV Remote by focusing on [-]/[+] buttons or focusing on the bar and pressing D-pad Left/Right!
 * - Operated on Mobile by touch tapping or dragging!
 */
@Composable
fun TvLevelControl(
    label: String,
    icon: ImageVector,
    tint: Color,
    currentFraction: Float,
    displayText: String,
    onStepDown: () -> Unit,
    onStepUp: () -> Unit,
    onSetFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onIconClick: (() -> Unit)? = null
) {
    val barInteractionSource = remember { MutableInteractionSource() }
    val isBarFocused by barInteractionSource.collectIsFocusedAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Icon / Mute Button
        if (onIconClick != null) {
            TvIconButton(
                icon = icon,
                contentDescription = label,
                size = 32.dp,
                iconSize = 20.dp,
                tint = tint,
                onClick = onIconClick
            )
        } else {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(8.dp))

        // Step Down [-] Button
        TvIconButton(
            icon = Icons.Default.Remove,
            contentDescription = "$label Down",
            size = 30.dp,
            iconSize = 16.dp,
            onClick = onStepDown
        )

        Spacer(Modifier.width(6.dp))

        // Interactive Level Bar (D-Pad Left/Right + Touch Slider)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    BorderStroke(
                        if (isBarFocused) 2.dp else 0.dp,
                        if (isBarFocused) MaterialTheme.colorScheme.primary else Color.Transparent
                    ),
                    RoundedCornerShape(8.dp)
                )
                .focusable(interactionSource = barInteractionSource)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                onStepDown()
                                true
                            }
                            Key.DirectionRight -> {
                                onStepUp()
                                true
                            }
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                if (onIconClick != null) onIconClick() else onStepUp()
                                true
                            }
                            else -> false
                        }
                    } else false
                },
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = currentFraction.coerceIn(0f, 1f),
                onValueChange = onSetFraction,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = tint,
                    activeTrackColor = tint,
                    inactiveTrackColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.width(6.dp))

        // Step Up [+] Button
        TvIconButton(
            icon = Icons.Default.Add,
            contentDescription = "$label Up",
            size = 30.dp,
            iconSize = 16.dp,
            onClick = onStepUp
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = displayText,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp)
        )
    }
}

/**
 * In-Player Quick Channels Drawer:
 * - Slides out from the right
 * - Auto-scrolls and requests initial focus on currently active channel
 * - Navigable with D-pad Up/Down
 * - Pressing OK zaps to the selected channel immediately
 */
@Composable
fun InPlayerChannelDrawer(
    channels: List<Channel>,
    currentChannel: Channel?,
    onSelectChannel: (Channel) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedIndex = remember(channels, currentChannel) {
        val idx = channels.indexOfFirst { it.streamUrl == currentChannel?.streamUrl }
        if (idx >= 0) idx else 0
    }

    val listState = rememberLazyListState()
    val initialFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (channels.isNotEmpty()) {
            delay(100)
            listState.scrollToItem(maxOf(0, selectedIndex - 2))
            try {
                initialFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(380.dp)
                .align(Alignment.CenterEnd)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp),
            color = NSSurface,
            border = BorderStroke(1.dp, NSBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Channels", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = NSTextPrimary)
                        Text("${channels.size} channels available (D-Pad Navigate)", fontSize = 12.sp, color = NSTextSecondary)
                    }
                    TvIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close Drawer",
                        size = 36.dp,
                        iconSize = 18.dp,
                        onClick = onDismiss
                    )
                }

                Spacer(Modifier.height(14.dp))

                if (channels.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No channels in this playlist", color = NSTextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(channels, key = { _, ch -> ch.id }) { index, ch ->
                            val isSelected = ch.streamUrl == currentChannel?.streamUrl
                            val itemRequester = if (index == selectedIndex) initialFocusRequester else null

                            FocusableCard(
                                onClick = { onSelectChannel(ch) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .then(if (itemRequester != null) Modifier.focusRequester(itemRequester) else Modifier),
                                shape = RoundedCornerShape(10.dp),
                                backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else NSSurfaceCard,
                                borderColor = if (isSelected) MaterialTheme.colorScheme.primary else NSBorder
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!ch.logoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ch.logoUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.LiveTv,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else NSTextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ch.name,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else NSTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = ch.groupTitle,
                                            color = NSTextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "PLAYING",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Focusable radio option for TV dialogs (Audio, Subtitles, Sleep Timer)
 */
@Composable
fun TvDialogOption(
    selected: Boolean,
    label: String,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else if (selected) NSSurfaceVariant else Color.Transparent,
        border = if (isFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
            .fillMaxWidth()
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    onSelect()
                    true
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
            .padding(vertical = 3.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                color = if (isFocused || selected) Color.White else NSTextPrimary,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun TrackSelectionDialog(
    audioTracks: List<TrackInfo>,
    subtitleTracks: List<TrackInfo>,
    onSelectAudio: (TrackInfo) -> Unit,
    onSelectSubtitle: (TrackInfo?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Audio & Subtitle Tracks", color = NSTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Audio Tracks", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                if (audioTracks.isEmpty()) {
                    Text("Default Audio Stream", color = NSTextSecondary, fontSize = 13.sp)
                } else {
                    audioTracks.forEach { track ->
                        TvDialogOption(
                            selected = track.isSelected,
                            label = track.name,
                            onSelect = { onSelectAudio(track) }
                        )
                    }
                }

                HorizontalDivider(color = NSBorder, modifier = Modifier.padding(vertical = 4.dp))

                Text("Subtitle Tracks", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                TvDialogOption(
                    selected = subtitleTracks.none { it.isSelected },
                    label = "Off (Disable Subtitles)",
                    onSelect = { onSelectSubtitle(null) }
                )

                subtitleTracks.forEach { track ->
                    TvDialogOption(
                        selected = track.isSelected,
                        label = track.name,
                        onSelect = { onSelectSubtitle(track) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TvTextButton(
                text = "Close",
                onClick = onDismiss
            )
        },
        containerColor = NSSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun SleepTimerDialog(
    currentMinutes: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        "Off" to null,
        "15 Minutes" to 15,
        "30 Minutes" to 30,
        "45 Minutes" to 45,
        "60 Minutes" to 60,
        "120 Minutes" to 120
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Sleep Timer", color = NSTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (label, minutes) ->
                    TvDialogOption(
                        selected = currentMinutes == minutes,
                        label = label,
                        onSelect = { onSelect(minutes) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TvTextButton(
                text = "Cancel",
                onClick = onDismiss
            )
        },
        containerColor = NSSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
