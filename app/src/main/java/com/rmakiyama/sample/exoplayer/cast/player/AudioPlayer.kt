package com.rmakiyama.sample.exoplayer.cast.player

import android.content.Context
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.cast.MediaItem
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastContext
import timber.log.Timber
import java.util.*

class AudioPlayer(
    context: Context
) : SessionAvailabilityListener {

    private val exoPlayer: SimpleExoPlayer =
        ExoPlayerFactory.newSimpleInstance(
            context,
            DefaultRenderersFactory(context),
            DefaultTrackSelector(),
            DefaultLoadControl()
        )
    private var castPlayer: CastPlayer = CastPlayer(CastContext.getSharedInstance(context))
    private lateinit var currentPlayer: Player

    private val mediaQueue: ArrayList<MediaItem> = arrayListOf()

    init {
        castPlayer.setSessionAvailabilityListener(this)
        setCurrentPlayer(if (castPlayer.isCastSessionAvailable) castPlayer else exoPlayer)
    }

    override fun onCastSessionAvailable() = setCurrentPlayer(castPlayer)

    override fun onCastSessionUnavailable() = setCurrentPlayer(exoPlayer)

    private fun setCurrentPlayer(currentPlayer: Player) {
        if (!this::currentPlayer.isInitialized) this.currentPlayer = currentPlayer
        if (this.currentPlayer === currentPlayer) {
            return
        }
        this.currentPlayer.stop(true)
        this.currentPlayer = currentPlayer
    }

    fun prepare(vararg items: MediaItem) {
        mediaQueue.addAll(items)
        // prepare for ExoPlayer
        val sources = items.map { item -> item.toMediaSource() }
        val source = ConcatenatingMediaSource().apply { addMediaSources(sources) }
        exoPlayer.prepare(source)
        // prepare for CastPlayer
        val castItems = items.map { it.toMediaQueueItem() }
        castPlayer.addItems(*castItems.toTypedArray())
    }

    fun play() {
        Timber.i("info: play | current player = ${currentPlayer::class.java.name}")
        if (currentPlayer === castPlayer && castPlayer.currentTimeline.isEmpty) {
            val items = arrayOfNulls<MediaQueueItem>(mediaQueue.size)
            for (index in items.indices) {
                items[index] = mediaQueue[index].toMediaQueueItem()
            }
            castPlayer.loadItems(items, 0, 0, Player.REPEAT_MODE_OFF)
        } else {
            currentPlayer.seekTo(0, 0)
            currentPlayer.playWhenReady = true
        }
    }

    fun pause() {
        Timber.i("info: pause | current player = ${currentPlayer::class.java.name}")
        currentPlayer.playWhenReady = false
    }

    fun stop() {
        Timber.i("info: stop | current player = ${currentPlayer::class.java.name}")
        currentPlayer.stop()
    }

    /**
     * build item for ExoPlayer
     */
    private fun MediaItem.toMediaSource(): MediaSource {
        return ProgressiveMediaSource.Factory(
            DefaultHttpDataSourceFactory(USER_AGENT)
        ).createMediaSource(media.uri)
    }

    /**
     * build item for CastPlayer
     */
    private fun MediaItem.toMediaQueueItem(): MediaQueueItem {
        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
        movieMetadata.putString(MediaMetadata.KEY_TITLE, title)
        val mediaInfo = MediaInfo.Builder(media.uri.toString())
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(mimeType)
            .setMetadata(movieMetadata)
            .build()
        return MediaQueueItem.Builder(mediaInfo).build()
    }

    companion object {

        private const val USER_AGENT = "AudioPlayer"
    }
}