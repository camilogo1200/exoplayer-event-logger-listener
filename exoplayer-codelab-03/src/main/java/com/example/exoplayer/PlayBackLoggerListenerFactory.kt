package com.example.exoplayer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.media3.common.*
import androidx.media3.common.C.*
import androidx.media3.common.Player.*
import androidx.media3.datasource.HttpDataSource.*
import androidx.media3.exoplayer.ExoPlayer
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque


private const val TAG = "EventLogger"

class PlayBackLoggerListenerFactory(private val context: Context) : Player.Listener {

    private lateinit var privateRootDir: File
    private val exoplayerMediaLogsFolderDir = "logs"
    private val exoplayerMediaLogFileDir = "mediaex_exoplayer_logs"
    private val exoplayerMediaLogPrefixName = "exoplayer_mediaex_log"
    private val PROVIDER = ".provider"
    private val logs = ConcurrentLinkedDeque<PlaybackLog>()
    private var openAlert = false
    private val JSON_MYME_TYPE = "text/json"

    fun exportVideoLog() {
        val filename = createLogFile(context)
        filename?.let {
            createShareableFileIntent(filename)
        }
    }

    private fun createShareableFileIntent(filename: String) {
        if (!openAlert) {

            val requestFile = File(filename)
            // Use the FileProvider to get a content URI
            val fileUri: Uri? = try {
                FileProvider.getUriForFile(
                    context,
                    BuildConfig.LIBRARY_PACKAGE_NAME + PROVIDER,
                    requestFile
                )
            } catch (e: IllegalArgumentException) {
                Timber.tag(TAG).e("The selected file can't be shared: $requestFile")
                null
            }

            fileUri?.let {
                val intent = Intent(Intent.ACTION_SEND)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                intent.setDataAndType(it, JSON_MYME_TYPE)
                intent.putExtra(Intent.EXTRA_STREAM, it)
                context.startActivity(Intent.createChooser(intent, "Export Video Log"))
            }
        }
    }

    private fun createLogFile(context: Context): String? {
        val directory = getLogsDirectory(context) ?: return null
        val filename = "$directory/${exoplayerMediaLogPrefixName}_${getLogDate()}.json"

        val file = File(filename)
        if (!file.createNewFile()) return null

        FileWriter(file).use {
            it.write(getLogsFileContent())
            it.flush()
            it.close()
        }

        return filename
//
//        context.openFileOutput(filename, Context.MODE_PRIVATE).use {
//            it.write(getLogsFileContent().toByteArray(Charsets.UTF_8))
//        }
    }

    private fun getLogsFileContent(): String {
        val fileContent = buildString {
            logs.onEach { appendLine(it.toJson()) }
        }
        return fileContent
    }

    private fun getLogDate(): String {
        val language = "en"
        val country = "US"
        val pattern = "dd-M-yyyy_hh:mm:ss"
        val locale = Locale(language, country)
        val dateFormat = SimpleDateFormat(pattern, locale)
        return dateFormat.format(Date())
    }

    private fun getLogsDirectory(context: Context): File? {
        val directory =
            File(context.filesDir, "$exoplayerMediaLogsFolderDir/$exoplayerMediaLogFileDir")
        if (!logsFileDirectoryExist(directory)) {
            if (!createLogsFileDirectory(directory)) return null
        }
        return directory
    }

    private fun createLogsFileDirectory(directory: File) =
        directory.mkdirs()

    private fun logsFileDirectoryExist(directory: File) =
        directory.isDirectory


    override fun onEvents(player: Player, events: Events) {
        val message = buildString {
            append("Events - ")

            for (i in 0 until events.size()) {
                append(" { ${getEventType(events.get(i))} } ")
            }

            if (events.size() == 1 && events[0] != EVENT_IS_LOADING_CHANGED) {
                append("Content duration : [${player.contentDuration}] - ")
                append("Player Volume : [${player.volume}] - ")
                player.currentMediaItem?.let {
                    append("MediaItem Id : [${it.mediaId}] - ")
                    it.localConfiguration?.let { config ->
                        append("MediaItem URI : [${config.uri}] - ")
                        append("MimeType : [${config.mimeType}] - ")
                    }
                }
            }
        }

        Timber.tag(TAG).d(message)
        val event = PlaybackLog(message, "onEvents")
        logs.addLast(event)
    }

    private fun getEventType(event: Int): String {
        return when (event) {
            EVENT_TIMELINE_CHANGED -> "EVENT_TIMELINE_CHANGED"
            EVENT_MEDIA_ITEM_TRANSITION -> "EVENT_MEDIA_ITEM_TRANSITION"
            EVENT_TRACKS_CHANGED -> "EVENT_TRACKS_CHANGED"
            EVENT_IS_LOADING_CHANGED -> "EVENT_IS_LOADING_CHANGED"
            EVENT_PLAYBACK_STATE_CHANGED -> "EVENT_PLAYBACK_STATE_CHANGED"
            EVENT_PLAY_WHEN_READY_CHANGED -> "EVENT_PLAY_WHEN_READY_CHANGED"
            EVENT_PLAYBACK_SUPPRESSION_REASON_CHANGED -> "EVENT_PLAYBACK_SUPPRESSION_REASON_CHANGED"
            EVENT_IS_PLAYING_CHANGED -> "EVENT_IS_PLAYING_CHANGED"
            EVENT_REPEAT_MODE_CHANGED -> "EVENT_REPEAT_MODE_CHANGED"
            EVENT_SHUFFLE_MODE_ENABLED_CHANGED -> "EVENT_SHUFFLE_MODE_ENABLED_CHANGED"
            EVENT_PLAYER_ERROR -> "EVENT_PLAYER_ERROR"
            EVENT_POSITION_DISCONTINUITY -> "EVENT_POSITION_DISCONTINUITY"
            EVENT_PLAYBACK_PARAMETERS_CHANGED -> "EVENT_PLAYBACK_PARAMETERS_CHANGED"
            EVENT_AVAILABLE_COMMANDS_CHANGED -> "EVENT_AVAILABLE_COMMANDS_CHANGED"
            EVENT_MEDIA_METADATA_CHANGED -> "EVENT_MEDIA_METADATA_CHANGED"
            EVENT_PLAYLIST_METADATA_CHANGED -> "EVENT_PLAYLIST_METADATA_CHANGED"
            EVENT_SEEK_BACK_INCREMENT_CHANGED -> "EVENT_SEEK_BACK_INCREMENT_CHANGED"
            EVENT_SEEK_FORWARD_INCREMENT_CHANGED -> "EVENT_SEEK_FORWARD_INCREMENT_CHANGED"
            EVENT_MAX_SEEK_TO_PREVIOUS_POSITION_CHANGED -> "EVENT_MAX_SEEK_TO_PREVIOUS_POSITION_CHANGED"
            EVENT_TRACK_SELECTION_PARAMETERS_CHANGED -> "EVENT_TRACK_SELECTION_PARAMETERS_CHANGED"
            EVENT_AUDIO_ATTRIBUTES_CHANGED -> "EVENT_AUDIO_ATTRIBUTES_CHANGED"
            EVENT_AUDIO_SESSION_ID -> "EVENT_AUDIO_SESSION_ID"
            EVENT_VOLUME_CHANGED -> "EVENT_VOLUME_CHANGED"
            EVENT_SKIP_SILENCE_ENABLED_CHANGED -> "EVENT_SKIP_SILENCE_ENABLED_CHANGED"
            EVENT_SURFACE_SIZE_CHANGED -> "EVENT_SURFACE_SIZE_CHANGED"
            EVENT_VIDEO_SIZE_CHANGED -> "EVENT_VIDEO_SIZE_CHANGED"
            EVENT_RENDERED_FIRST_FRAME -> "EVENT_RENDERED_FIRST_FRAME"
            EVENT_CUES -> "EVENT_CUES"
            EVENT_METADATA -> "EVENT_METADATA"
            EVENT_DEVICE_INFO_CHANGED -> "EVENT_DEVICE_INFO_CHANGED"
            EVENT_DEVICE_VOLUME_CHANGED -> "EVENT_DEVICE_VOLUME_CHANGED"
            else -> "EVENT UNKNOWN - [$event]"
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        val message = buildString {
            append("Repeat mode changed -")
            append(
                when (repeatMode) {
                    REPEAT_MODE_OFF -> "REPEAT_MODE_OFF"
                    REPEAT_MODE_ONE -> "REPEAT_MODE_ONE"
                    REPEAT_MODE_ALL -> "REPEAT_MODE_ALL"
                    else -> "REPEAT MODE UNKNOWN"
                }
            )
        }
        Timber.tag(TAG).d(message)
        val event = PlaybackLog(message, "onRepeatModeChanged")
        logs.addLast(event)
    }

    override fun onRenderedFirstFrame() {
        val message = "Rendered first frame"
        Timber.tag(TAG).d(message)
        val event = PlaybackLog(message, "onRenderedFirstFrame")
        logs.addLast(event)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val message = buildString {
            append(
                when (reason) {
                    MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "MEDIA_ITEM_TRANSITION_REASON_REPEAT"
                    MEDIA_ITEM_TRANSITION_REASON_SEEK -> "MEDIA_ITEM_TRANSITION_REASON_SEEK"
                    MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> "MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED"
                    MEDIA_ITEM_TRANSITION_REASON_AUTO -> "MEDIA_ITEM_TRANSITION_REASON_AUTO"
                    else -> "UNKNOWN TRANSITION REASON"
                }
            )
        }
        Timber.tag(TAG).d(message)
        val event = PlaybackLog(message, "onMediaItemTransition")
        logs.addLast(event)
        if (reason != MEDIA_ITEM_TRANSITION_REASON_REPEAT) logs.clear()
    }

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
        logs.addLast(event)
    }

    override fun onPlayerError(error: PlaybackException) {
        val cause = error.cause
        val codeError = error.errorCodeName
        val message = if (cause is HttpDataSourceException) {
            when (cause) {
                is InvalidContentTypeException -> {
                    "ExoPlayer - InvalidContentTypeException - contentType => [${cause.contentType}]"
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

        logs.addLast(event)
    }

    override fun onPositionDiscontinuity(
        oldPosition: PositionInfo,
        newPosition: PositionInfo,
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

        logs.addLast(event)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        val message = "Is video playing => $isPlaying"
        Timber.tag(TAG).d(message)
    }

    override fun onTracksChanged(tracks: Tracks) {

        val groupSize = tracks.groups.size

        val message = buildString {
            append("Tracks group size => [$groupSize] \n")

            if (groupSize > 0) {
                for (iter in tracks.groups.withIndex()) {
                    val group = iter.value
                    appendLine("Track : [${iter.index}]")
                    append("TrackType => [${group.type}] - ")
                    append("Supported [${if (group.isSupported) "Yes" else "No"}] - ")
                    append("Length => [${group.length}] - ")
                    appendLine("Is Selected - ${group.isSelected}")

                    appendLine("Format :")
                    for (i in 0 until group.mediaTrackGroup.length) {
                        val format = group.getTrackFormat(i)
                        append("{")
                        append(" Id - [${format.id}] |")
                        append(" Bitrate - [${format.bitrate}] |")
                        append(" Codecs - [${format.codecs}] |")
                        append(" Dimensions - [height: ${format.height} - width: ${format.width}] |")
                        append(" FrameRate - [${format.frameRate}] |")
                        append(" SampleRate - [${format.sampleRate}] ")
                        appendLine("}")
                    }
                }
            }
        }
        Timber.tag(TAG).d(message)
        val event = PlaybackLog(message, "onTracksChanged")
        logs.addLast(event)

    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        val message = "Loading - $isLoading"
        val event = PlaybackLog(message, "onIsLoadingChanged")
        logs.addLast(event)
        Timber.tag(TAG).d(message)
    }

    override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) {
        val message = buildString {
            append(
                when (audioAttributes.contentType) {
                    AUDIO_CONTENT_TYPE_SPEECH -> "AUDIO_CONTENT_TYPE_SPEECH"
                    AUDIO_CONTENT_TYPE_MOVIE -> "CONTENT_TYPE_MUSIC"
                    AUDIO_CONTENT_TYPE_MUSIC -> "CONTENT_TYPE_MOVIE"
                    AUDIO_CONTENT_TYPE_SONIFICATION -> "AUDIO_CONTENT_TYPE_SONIFICATION"
                    else -> "AUDIO_CONTENT_TYPE_UNKNOWN"
                }
            )


            append(
                when (audioAttributes.spatializationBehavior) {
                    SPATIALIZATION_BEHAVIOR_AUTO -> "SPATIALIZATION_BEHAVIOR_AUTO"
                    SPATIALIZATION_BEHAVIOR_NEVER -> "SPATIALIZATION_BEHAVIOR_NEVER"
                    else -> "UNKNOWN BEHAVIOR"
                }
            )
        }
        val event = PlaybackLog(message, "onAudioAttributesChanged")
        logs.addLast(event)
        Timber.tag(TAG).d(message)
    }

    override fun onVolumeChanged(volume: Float) {
        val message = "Volume changed [$volume]"

        val event = PlaybackLog(message, "onVolumeChanged")
        logs.addLast(event)
        Timber.tag(TAG).d(message)
    }

    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
        super.onDeviceInfoChanged(deviceInfo)
    }

    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {

        val message = "Device Volume changed [$volume] - muted [$muted]"

        val event = PlaybackLog(message, "onDeviceVolumeChanged")
        logs.addLast(event)
        Timber.tag(TAG).d(message)
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        val message = buildString {
            append("Video size changed height: [${videoSize.height}] - width [${videoSize.width}] - ")
            append(
                if (!videoSize.pixelWidthHeightRatio.equals(1.0F)) {
                    "pixelWidthHeightRatio - Anamorphic content"
                } else {
                    "pixelWidthHeightRatio - Square pixels"
                }
            )
        }

        val event = PlaybackLog(message, "onVideoSizeChanged")
        logs.addLast(event)
        Timber.tag(TAG).d(message)
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        val message = "Surface size changed - height:[$height] - width:[$width]"
        val event = PlaybackLog(message, "onSurfaceSizeChanged")
        logs.addLast(event)
        Timber.tag(TAG).d(message)
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


    fun toJson(): String {
        return "JSON //"//Json.encodeToString(this)
    }
}
