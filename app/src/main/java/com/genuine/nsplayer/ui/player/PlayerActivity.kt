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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

class PlayerActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

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

        // Lock to landscape on phones/tablets for optimal cinematic viewing
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
                    onEnterPip = { enterPipMode() },
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                playerViewModel.mediaEngine.togglePlayPause()
                true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                playerViewModel.mediaEngine.seekRelative(10_000)
                true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                playerViewModel.mediaEngine.seekRelative(-10_000)
                true
            }
            KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_MEDIA_NEXT -> {
                playerViewModel.playNextChannel()
                true
            }
            KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                playerViewModel.playPreviousChannel()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
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

    // Audio & Volume Management (NO GESTURES - MANUAL CONTROLS ONLY)
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var currentVolume by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    var lastNonZeroVolume by remember { mutableStateOf(if (currentVolume > 0) currentVolume else (maxVolume / 2)) }

    // Brightness Management (NO GESTURES - MANUAL CONTROLS ONLY)
    var currentBrightness by remember {
        val cur = activity?.window?.attributes?.screenBrightness ?: 0.5f
        mutableStateOf(if (cur < 0.05f) 0.5f else cur)
    }

    // HUD Notification State (for on-screen feedback when manual buttons are pressed)
    var hudText by remember { mutableStateOf<String?>(null) }
    var hudIcon by remember { mutableStateOf(Icons.AutoMirrored.Filled.VolumeUp) }
    var hudProgress by remember { mutableStateOf(0.5f) }

    fun triggerHud(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, progress: Float) {
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

    // Auto-hide controls after 6 seconds of inactivity during playback
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(6000)
            showControls = false
        }
    }

    // Auto-hide floating HUD
    LaunchedEffect(hudText) {
        if (hudText != null) {
            delay(1500)
            hudText = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                // Strictly tap & double-tap only: NO VERTICAL DRAG GESTURES for volume or brightness
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
                    color = Color.Black.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NSBorder)
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

        // Floating Center HUD for Manual Volume & Brightness Feedback
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xF01A0B10),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NSError),
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
                        Button(
                            onClick = { mediaEngine.retryPlayback() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry Connection")
                        }
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
                        Surface(
                            shape = CircleShape,
                            color = Color(0x66161B26),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NSBorder),
                            modifier = Modifier.size(42.dp)
                        ) {
                            IconButton(onClick = onClose) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        if (!channel?.logoUrl.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x88090A0F),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NSBorder),
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
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
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

                    // Right: Quick Function Icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Aspect Ratio Toggle
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x66161B26),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NSBorder)
                        ) {
                            TextButton(
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
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.AspectRatio, contentDescription = "Aspect", tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(aspectRatioMode.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Tracks (Audio / Subtitles)
                        Surface(
                            shape = CircleShape,
                            color = Color(0x66161B26),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NSBorder),
                            modifier = Modifier.size(40.dp)
                        ) {
                            IconButton(onClick = { showTrackDialog = true }) {
                                Icon(Icons.Default.Subtitles, contentDescription = "Tracks", tint = Color.White, modifier = Modifier.size(19.dp))
                            }
                        }

                        // Sleep Timer
                        Surface(
                            shape = CircleShape,
                            color = if (sleepTimer != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color(0x66161B26),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (sleepTimer != null) MaterialTheme.colorScheme.primary else NSBorder),
                            modifier = Modifier.size(40.dp)
                        ) {
                            IconButton(onClick = { showSleepTimerDialog = true }) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = "Sleep Timer",
                                    tint = if (sleepTimer != null) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        // Picture in Picture
                        Surface(
                            shape = CircleShape,
                            color = Color(0x66161B26),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NSBorder),
                            modifier = Modifier.size(40.dp)
                        ) {
                            IconButton(onClick = onEnterPip) {
                                Icon(Icons.Default.PictureInPicture, contentDescription = "PiP", tint = Color.White, modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                }

                // Center Play / Rewind / Fast-Forward Transport Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    // Rewind 10s Button
                    Surface(
                        shape = CircleShape,
                        color = Color(0x8812151F),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NSBorder),
                        modifier = Modifier.size(54.dp)
                    ) {
                        IconButton(onClick = {
                            mediaEngine.seekRelative(-10_000)
                            fastSeekText = "-10s"
                        }) {
                            Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }

                    // Main Glassmorphic Play/Pause Button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.6f)),
                        shadowElevation = 12.dp,
                        modifier = Modifier.size(76.dp)
                    ) {
                        IconButton(onClick = { mediaEngine.togglePlayPause() }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    // Forward 10s Button
                    Surface(
                        shape = CircleShape,
                        color = Color(0x8812151F),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NSBorder),
                        modifier = Modifier.size(54.dp)
                    ) {
                        IconButton(onClick = {
                            mediaEngine.seekRelative(10_000)
                            fastSeekText = "+10s"
                        }) {
                            Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                // Bottom Controls Cluster: Manual Volume & Brightness + Channels & Zap
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // MANUAL ON-SCREEN CONTROLS BAR: Brightness & Volume (NO GESTURES)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xDD12151F))
                            .border(1.dp, NSBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Section: Manual Brightness Control
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val briPct = (currentBrightness * 100).toInt()
                            val briIcon = if (briPct < 40) Icons.Default.BrightnessLow else if (briPct < 75) Icons.Default.BrightnessMedium else Icons.Default.BrightnessHigh
                            Icon(briIcon, contentDescription = null, tint = NSWarning, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))

                            // Step Down [-] Button
                            Surface(
                                shape = CircleShape,
                                color = Color(0x55262E42),
                                modifier = Modifier.size(30.dp)
                            ) {
                                IconButton(onClick = { setBrightness(currentBrightness - 0.10f) }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Brightness Down", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }

                            // Interactive Slider
                            Slider(
                                value = currentBrightness,
                                onValueChange = { setBrightness(it) },
                                valueRange = 0.05f..1.0f,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = NSWarning,
                                    activeTrackColor = NSWarning,
                                    inactiveTrackColor = Color.DarkGray
                                )
                            )

                            // Step Up [+] Button
                            Surface(
                                shape = CircleShape,
                                color = Color(0x55262E42),
                                modifier = Modifier.size(30.dp)
                            ) {
                                IconButton(onClick = { setBrightness(currentBrightness + 0.10f) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Brightness Up", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "$briPct%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp)
                            )
                        }

                        Spacer(Modifier.width(20.dp))

                        // Right Section: Manual Volume Control
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val volPct = (currentVolume * 100) / maxVolume
                            val volIcon = if (currentVolume == 0) Icons.AutoMirrored.Filled.VolumeMute else if (volPct < 50) Icons.AutoMirrored.Filled.VolumeDown else Icons.AutoMirrored.Filled.VolumeUp

                            // Mute Toggle Icon Button
                            IconButton(onClick = { toggleMute() }, modifier = Modifier.size(28.dp)) {
                                Icon(volIcon, contentDescription = "Mute", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(6.dp))

                            // Step Down [-] Button
                            Surface(
                                shape = CircleShape,
                                color = Color(0x55262E42),
                                modifier = Modifier.size(30.dp)
                            ) {
                                IconButton(onClick = { setVolume(currentVolume - 1) }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Volume Down", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }

                            // Interactive Volume Slider
                            Slider(
                                value = currentVolume.toFloat() / maxVolume.toFloat(),
                                onValueChange = { fraction ->
                                    val newVol = (fraction * maxVolume).toInt()
                                    setVolume(newVol)
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.DarkGray
                                )
                            )

                            // Step Up [+] Button
                            Surface(
                                shape = CircleShape,
                                color = Color(0x55262E42),
                                modifier = Modifier.size(30.dp)
                            ) {
                                IconButton(onClick = { setVolume(currentVolume + 1) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Volume Up", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "$volPct%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp)
                            )
                        }
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Previous Channel Button
                            Button(
                                onClick = { viewModel.playPreviousChannel() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x99161B26)),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NSBorder),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Prev", fontSize = 12.sp, color = Color.White)
                            }

                            // Next Channel Button
                            Button(
                                onClick = { viewModel.playNextChannel() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x99161B26)),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NSBorder),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Next", fontSize = 12.sp, color = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }

                            // In-Player Quick Channels Drawer Button
                            Button(
                                onClick = { showChannelDrawer = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Channels", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            // Favorite Toggle
                            channel?.let { ch ->
                                IconButton(onClick = { viewModel.toggleFavorite(ch) }) {
                                    Icon(
                                        imageVector = if (ch.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "Favorite",
                                        tint = if (ch.isFavorite) NSGold else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
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
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
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
                                border = androidx.compose.foundation.BorderStroke(1.dp, NSBorder)
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

    // In-Player Quick Channels Drawer
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

    // Audio & Subtitle Tracks Dialog
    if (showTrackDialog) {
        TrackSelectionDialog(
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            onSelectAudio = { mediaEngine.selectAudioTrack(it) },
            onSelectSubtitle = { mediaEngine.selectSubtitleTrack(it) },
            onDismiss = { showTrackDialog = false }
        )
    }

    // Sleep Timer Dialog
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

@Composable
fun InPlayerChannelDrawer(
    channels: List<Channel>,
    currentChannel: Channel?,
    onSelectChannel: (Channel) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(360.dp)
                .align(Alignment.CenterEnd)
                .clickable(enabled = false) {}, // Prevent dismiss when tapping inside drawer
            shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp),
            color = NSSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NSBorder)
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
                        Text("Channels", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NSTextPrimary)
                        Text("${channels.size} channels available", fontSize = 12.sp, color = NSTextSecondary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NSTextSecondary)
                    }
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(channels, key = { it.id }) { ch ->
                            val isSelected = ch.streamUrl == currentChannel?.streamUrl
                            FocusableCard(
                                onClick = { onSelectChannel(ch) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp),
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
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Audio", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                if (audioTracks.isEmpty()) {
                    Text("Default Audio Track", color = NSTextSecondary, fontSize = 13.sp)
                } else {
                    audioTracks.forEach { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectAudio(track) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = track.isSelected,
                                onClick = { onSelectAudio(track) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(track.name, color = NSTextPrimary, fontSize = 14.sp)
                        }
                    }
                }

                HorizontalDivider(color = NSBorder)

                Text("Subtitles", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectSubtitle(null) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = subtitleTracks.none { it.isSelected },
                        onClick = { onSelectSubtitle(null) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Off", color = NSTextPrimary, fontSize = 14.sp)
                }

                subtitleTracks.forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectSubtitle(track) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = track.isSelected,
                            onClick = { onSelectSubtitle(track) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(track.name, color = NSTextPrimary, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(minutes) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMinutes == minutes,
                            onClick = { onSelect(minutes) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = NSTextPrimary, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NSTextSecondary)
            }
        },
        containerColor = NSSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
