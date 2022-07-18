/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.exoplayer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.exoplayer.databinding.ActivityPlayerBinding
import timber.log.Timber
import timber.log.Timber.*


/**
 * A fullscreen activity to play audio or video streams.
 */


class PlayerActivity : AppCompatActivity() {

    private val logger: Player.Listener by lazy(LazyThreadSafetyMode.NONE) {
        PlayBackLoggerListenerFactory()
    }

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityPlayerBinding.inflate(layoutInflater)
    }

    private var player: ExoPlayer? = null
    private var openAlert = false
    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.videoView.setOnTouchListener { view, motionEvent ->
            Timber.tag("PlayerActivity").d("Pointer Count : ${motionEvent.pointerCount}")
            if (motionEvent.pointerCount === 3) {

                //
                // showAlert()
                createIntent()
            }
            true
        }
    }

    private fun createIntent() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "This is my text to send.")
            type = "application/json"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun showAlert() {
        if (!openAlert) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)

            // Set the message show for the Alert time

            // Set the message show for the Alert time
            builder.setMessage("Do you want to exit ?")

            // Set Alert Title

            // Set Alert Title
            builder.setTitle("Alert !")
            builder.setOnCancelListener {
                openAlert = false
            }
            openAlert = true
            builder.create().show()

        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            Timber.tag("PlayerActivity").d("-----")
        }
        return true
    }

    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    public override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun initializePlayer() {
        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }
        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build()
            .also { exoPlayer ->
                viewBinding.videoView.player = exoPlayer

                val mediaItem = MediaItem.Builder()
                    .setUri(getString(R.string.media_url_dash))
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .build()
                exoPlayer.setMediaItem(mediaItem)

                val mediaItem2 = MediaItem
                    .fromUri("https://dash.akamaized.net/akamai/bbb_30fps/bbb_30fps.mpd")

                exoPlayer.addMediaItem(mediaItem2)
                val mediaItem3 = MediaItem
                    .fromUri("https://dash.akamaized.net/digitalprimates/fraunhofer/480p_video/heaac_2_0_with_video/Sintel/sintel_480p_heaac2_0.mpd")

                exoPlayer.addMediaItem(mediaItem3)
                val mediaItem4 = MediaItem
                    .fromUri("https://livesim.dashif.org/livesim/scte35_2/testpic_2s/Manifest.mpd")
                exoPlayer.addMediaItem(mediaItem4)



                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.seekTo(currentItem, playbackPosition)
                exoPlayer.addListener(logger)
                exoPlayer.prepare()
            }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.release()
        }
        player = null
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, viewBinding.videoView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }


}
