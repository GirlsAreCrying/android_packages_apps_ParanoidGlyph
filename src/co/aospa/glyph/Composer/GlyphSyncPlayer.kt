package co.aospa.glyph.Composer

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import co.aospa.glyph.Manager.AnimationManager
import co.aospa.glyph.Manager.SettingsManager
import java.io.IOException

class GlyphSyncPlayer(private val context: Context) {

    companion object {
        private const val TAG = "GlyphSyncPlayer"
        private const val DEBUG = true
    }

    fun interface OnCompletionListener {
        fun onCompleted()
    }

    private var mediaPlayer: MediaPlayer? = null
    private var completionListener: OnCompletionListener? = null

    private var pattern: GlyphPattern? = null
    private var currentFrameIndex: Int = 0
    private var isPlaying: Boolean = false

    private val mainHandler = Handler(Looper.getMainLooper())

    fun play(uri: Uri, glyphPattern: GlyphPattern) {
        stop()

        pattern = glyphPattern
        currentFrameIndex = 0
        isPlaying = true

        val mp = MediaPlayer()
        mediaPlayer = mp

        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setDataSource(context, uri)
            mp.setOnPreparedListener {
                it.start()
                startGlyphSync()
            }
            mp.setOnCompletionListener {
                stop()
                completionListener?.onCompleted()
            }
            mp.setOnErrorListener { _, what, extra ->
                if (DEBUG) Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                stop()
                true
            }
            mp.prepareAsync()
        } catch (e: IOException) {
            if (DEBUG) Log.e(TAG, "Failed to play uri=$uri", e)
            stop()
        }
    }

    fun stop() {
        isPlaying = false

        mediaPlayer?.let { mp ->
            runCatching { mp.setOnPreparedListener(null) }
            runCatching { mp.setOnCompletionListener(null) }
            runCatching { mp.setOnErrorListener(null) }
            runCatching { mp.stop() }
            runCatching { mp.reset() }
            runCatching { mp.release() }
        }
        mediaPlayer = null

        AnimationManager.stopAll()
        pattern = null
        currentFrameIndex = 0
    }

    private fun startGlyphSync() {
        val p = pattern
        val frames = p?.frames
        if (p == null || frames == null) {
            if (DEBUG) Log.e(TAG, "No pattern to sync")
            return
        }

        currentFrameIndex = 0
        scheduleNextFrame(System.currentTimeMillis())
    }

    private fun scheduleNextFrame(startTime: Long) {
        val p = pattern ?: return
        val frames = p.frames ?: return
        if (!isPlaying) return

        if (currentFrameIndex >= frames.size) {
            stop()
            completionListener?.onCompleted()
            return
        }

        val frame = frames[currentFrameIndex]
        val now = System.currentTimeMillis() - startTime
        val delayMs = (frame.timestamp - now).coerceAtLeast(0)

        mainHandler.postDelayed({
            if (!isPlaying) return@postDelayed
            playFrame(frame)
            currentFrameIndex++
            scheduleNextFrame(startTime)
        }, delayMs)
    }

    private fun playFrame(frame: GlyphPattern.GlyphFrame) {
        val zones = frame.zones ?: return
        val brightness = scaleBrightness(frame.brightness)
        for (zone in zones) {
            AnimationManager.singleLedBlink(context, zone, brightness, frame.duration)
        }
    }

    private fun scaleBrightness(patternBrightness: Int): Int {
        val userBrightness = SettingsManager.getGlyphBrightness()
        return (patternBrightness * userBrightness) / 100
    }

    fun isPlaying(): Boolean = isPlaying

    fun setOnCompletionListener(listener: OnCompletionListener?) {
        completionListener = listener
    }

    fun release() {
        stop()
    }
}