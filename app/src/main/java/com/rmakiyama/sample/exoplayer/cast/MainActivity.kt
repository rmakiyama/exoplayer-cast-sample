package com.rmakiyama.sample.exoplayer.cast

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.ext.cast.MediaItem
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.rmakiyama.sample.exoplayer.cast.databinding.ActivityMainBinding
import com.rmakiyama.sample.exoplayer.cast.player.AudioPlayer

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
    }

    private val player: AudioPlayer by lazy { AudioPlayer(this) }
    private lateinit var castContext: CastContext
    private val mediaItemBuilder: MediaItem.Builder = MediaItem.Builder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        castContext = CastContext.getSharedInstance(this)

        binding.play.setOnClickListener { player.play() }
        binding.pause.setOnClickListener { player.pause() }

        setupMediaItem()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu, menu)
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.menu_cast)
        return true
    }

    private fun setupMediaItem() {
        val items = resources.getStringArray(R.array.audio_urls).map { uri ->
            mediaItemBuilder
                .clear()
                .setMedia(uri)
                .setTitle("name")
                .setMimeType(MimeTypes.AUDIO_MP4)
                .build()
        }
        player.prepare(*items.toTypedArray())
    }
}
