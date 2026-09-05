package com.xyecoc.mail.ui.components

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.xyecoc.mail.util.RemoteConfigManager

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PromoBanner(
    rc: RemoteConfigManager,
    configTick: Long = 0L,
    modifier: Modifier = Modifier
) {
    if (!rc.promoBannerEnabled) return

    val context = LocalContext.current
    val mediaUrl = rc.promoBannerMediaUrl
    val text = rc.promoBannerText
    val clickUrl = rc.promoBannerUrl

    if (mediaUrl.isBlank() && text.isBlank()) return

    val bannerColor = remember(rc.promoBannerColor, configTick) {
        try {
            if (rc.promoBannerColor.isNotBlank()) {
                Color(android.graphics.Color.parseColor(rc.promoBannerColor))
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    val onBannerClick: () -> Unit = {
        if (clickUrl.isNotBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(clickUrl))
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("PromoBanner", "Failed to open URL: $clickUrl", e)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = bannerColor ?: MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(enabled = clickUrl.isNotBlank(), onClick = onBannerClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (mediaUrl.isNotBlank()) {
                val isVideo = mediaUrl.contains(".mp4", ignoreCase = true)

                if (isVideo) {
                    var isMuted by remember(mediaUrl, configTick) {
                        mutableStateOf(!rc.promoBannerSoundDefault)
                    }
                    var isBuffering by remember { mutableStateOf(true) }

                    val exoPlayer = remember(mediaUrl, configTick) {
                        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                            .setAllowCrossProtocolRedirects(true)
                            .setConnectTimeoutMs(15000)
                            .setReadTimeoutMs(15000)

                        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

                        ExoPlayer.Builder(context)
                            .setMediaSourceFactory(mediaSourceFactory)
                            .build().apply {
                                val mediaItem = MediaItem.Builder()
                                    .setUri(mediaUrl)
                                    .setMimeType(MimeTypes.VIDEO_MP4)
                                    .build()
                                setMediaItem(mediaItem)
                                repeatMode = Player.REPEAT_MODE_ALL
                                volume = if (isMuted) 0f else 1f
                                playWhenReady = true

                                addListener(object : Player.Listener {
                                    override fun onPlaybackStateChanged(playbackState: Int) {
                                        isBuffering = playbackState == Player.STATE_BUFFERING
                                        Log.d("PromoBanner", "Playback state: $playbackState")
                                    }

                                    override fun onPlayerError(error: PlaybackException) {
                                        Log.e("PromoBanner", "ExoPlayer error: ${error.message}", error)
                                        isBuffering = false
                                    }
                                })

                                prepare()
                            }
                    }

                    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

                    DisposableEffect(lifecycleOwner, exoPlayer) {
                        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                            when (event) {
                                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                                    exoPlayer.pause()
                                    exoPlayer.volume = 0f
                                }
                                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                                    exoPlayer.volume = if (isMuted) 0f else 1f
                                    exoPlayer.play()
                                }
                                else -> {}
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    DisposableEffect(exoPlayer) {
                        onDispose {
                            exoPlayer.release()
                        }
                    }

                    LaunchedEffect(isMuted) {
                        exoPlayer.volume = if (isMuted) 0f else 1f
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    isClickable = false
                                    isFocusable = false
                                }
                            },
                            update = { view ->
                                if (view.player != exoPlayer) {
                                    view.player = exoPlayer
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Прозрачный слой клика для видео
                        if (clickUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(onClick = onBannerClick)
                            )
                        }

                        // Индикатор буферизации
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(36.dp)
                                    .align(Alignment.Center),
                                color = Color.White
                            )
                        }

                        // Кнопка переключения звука (вкл/выкл) для MP4
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "Включить звук" else "Выключить звук",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    // GIF, JPG, PNG, WebP
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = "Промо изображение",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(
                                if (text.isNotBlank()) {
                                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                } else {
                                    RoundedCornerShape(16.dp)
                                }
                            )
                    )
                }
            }

            if (text.isNotBlank()) {
                Text(
                    text = text,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (bannerColor != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
