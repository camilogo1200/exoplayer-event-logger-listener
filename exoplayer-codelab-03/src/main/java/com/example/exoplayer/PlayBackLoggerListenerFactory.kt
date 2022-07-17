package com.example.exoplayer

import androidx.media3.common.*
import androidx.media3.datasource.HttpDataSource.*
import androidx.media3.exoplayer.ExoPlayer
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque


private val TAG = PlayBackLoggerListenerFactory::class.java.name

class PlayBackLoggerListenerFactory : Player.Listener {
    private val logs = ConcurrentLinkedDeque<PlaybackLog>()


    override fun onPlaybackStateChanged(playbackState: Int) {
        val date = Date()
        val stateString: String = when (playbackState) {
            ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE"
            ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING"
            ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY"
            ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED"
            else -> "UNKNOWN_STATE"
        }
        val message = "${date.time} - changed state to $stateString"

        Timber.tag(TAG).d(message)
        val event = PlaybackLog(message, "onPlaybackStateChanged")
        logs.addFirst(event)
    }

    override fun onPlayerError(error: PlaybackException) {
        val cause = error.cause
        val codeError = error.errorCodeName
        val message = if (cause is HttpDataSourceException) {
            when (cause) {
                is InvalidContentTypeException -> {
                    "ExoPlayer - InvalidContentTypeException - contentType => ${cause.contentType}"
                }
                is InvalidResponseCodeException -> {
                    "InvalidResponseCodeException - responseCode => [${cause.responseCode}] - ${cause.message}"
                }
                is CleartextNotPermittedException -> {
                    "CleartextNotPermittedException - ${cause.message}"
                }
                else -> {
                    "Unknown HttpDataSourceException - ${cause.message}"
                }
            }
        } else {
            "Cause => ${cause?.message}"
        }

        Timber.tag(TAG).d(message)
        val event = PlaybackLog(message, "onPlayerError - $codeError")

        logs.addFirst(event)
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        val message = if (oldPosition.positionMs > newPosition.positionMs) {
            "Rewinding video - Id[${oldPosition.mediaItem?.mediaId ?: "null"} ] - from ${oldPosition.positionMs} to ${newPosition.positionMs} "
        } else {
            "Advancing video - Id[${oldPosition.mediaItem?.mediaId ?: "null"}] - from ${oldPosition.positionMs} to ${newPosition.positionMs} \""
        }
        Timber.tag(TAG).d(message)
        val event =
            PlaybackLog(message, "onPositionDiscontinuity - DiscontinuityReason => [$reason]")

        logs.addFirst(event)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        val message = "Is video playing => $isPlaying"
        Timber.tag(TAG).d(message)
    }

    override fun onTracksChanged(tracks: Tracks) {
        super.onTracksChanged(tracks)
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {

    }

    override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) {
        super.onAudioAttributesChanged(audioAttributes)
    }

    override fun onVolumeChanged(volume: Float) {
        super.onVolumeChanged(volume)
    }

    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
        super.onDeviceInfoChanged(deviceInfo)
    }

    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
        super.onDeviceVolumeChanged(volume, muted)
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        super.onVideoSizeChanged(videoSize)
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        super.onSurfaceSizeChanged(width, height)
    }
}


class PlaybackLog constructor() {

    private val language = "en"
    private val country = "US"
    private val pattern: String = "E, dd MMM yyyy HH:mm:ss.SSS z"
    private val locale = Locale(language, country)
    private val dateFormat: SimpleDateFormat = SimpleDateFormat(pattern, locale)

    private val date = Date()
    val timestamp: Long = date.time
    val readableDate: String = dateFormat.format(date)
    var message: String? = ""
    var type: String = ""

    constructor(
        message: String,
        type: String
    ) : this() {
        this.message = message
        this.type = type
    }
}
