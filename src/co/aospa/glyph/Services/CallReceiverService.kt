package co.aospa.glyph.Services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import co.aospa.glyph.Composer.GlyphComposerParser
import co.aospa.glyph.Composer.GlyphPattern
import co.aospa.glyph.Composer.GlyphSyncPlayer
import co.aospa.glyph.Manager.AnimationManager
import co.aospa.glyph.Manager.SettingsManager

class CallReceiverService : Service() {

    companion object {
        private const val TAG = "GlyphCallReceiverService"
        private const val DEBUG = true
    }

    private lateinit var audioManager: AudioManager
    private var glyphSyncPlayer: GlyphSyncPlayer? = null

    private lateinit var thread: HandlerThread
    private lateinit var threadHandler: Handler

    private var isComposerPatternPlaying = false
    private var composerPatternStartTime: Long = 0L

    private val playCallRunnable = Runnable { playRingtoneWithGlyphSync() }

    override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")
        super.onCreate()

        thread = HandlerThread("GlyphCallReceiverService")
        thread.start()
        threadHandler = Handler(thread.looper)

        audioManager = getSystemService(AudioManager::class.java)
        audioManager.addOnModeChangedListener({ cmd -> threadHandler.post(cmd) }, audioManagerOnModeChangedListener)
        audioManagerOnModeChangedListener.onModeChanged(audioManager.mode)

        glyphSyncPlayer = GlyphSyncPlayer(this).apply {
            setOnCompletionListener {
                if (DEBUG) Log.d(TAG, "Ringtone playback completed")
            }
        }

        val callReceiver = IntentFilter().apply {
            addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        }
        registerReceiver(callReceiverReceiver, callReceiver)
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")

        runCatching { unregisterReceiver(callReceiverReceiver) }
        runCatching { audioManager.removeOnModeChangedListener(audioManagerOnModeChangedListener) }

        disableCallAnimation()

        runCatching { glyphSyncPlayer?.release() }
        glyphSyncPlayer = null

        runCatching { thread.quitSafely() }
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun enableCallAnimation() {
        threadHandler.post(playCallRunnable)
    }

    private fun disableCallAnimation() {
        if (threadHandler.hasCallbacks(playCallRunnable)) {
            threadHandler.removeCallbacks(playCallRunnable)
        }

        if (isComposerPatternPlaying) {
            isComposerPatternPlaying = false
            threadHandler.removeCallbacksAndMessages(null)
            if (DEBUG) Log.d(TAG, "Stopped composer pattern playback")
        }

        runCatching { glyphSyncPlayer?.stop() }
        AnimationManager.stopCall()
    }

    private fun playRingtoneWithGlyphSync() {
        if (!SettingsManager.isGlyphCallEnabled()) return

        if (!SettingsManager.isGlyphComposerEnabled()) {
            if (DEBUG) Log.d(TAG, "Glyph Composer disabled, using standard animation")
            AnimationManager.playCall(SettingsManager.getGlyphCallAnimation())
            return
        }

        val ringtoneUri: Uri? =
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)

        if (ringtoneUri == null) {
            if (DEBUG) Log.w(TAG, "No ringtone URI, falling back to standard animation")
            AnimationManager.playCall(SettingsManager.getGlyphCallAnimation())
            return
        }

        val audioPath = getRealPathFromUri(ringtoneUri)
        if (audioPath.isNullOrBlank()) {
            if (DEBUG) Log.w(TAG, "No ringtone file path, falling back")
            AnimationManager.playCall(SettingsManager.getGlyphCallAnimation())
            return
        }

        val glyphPatternPath = GlyphComposerParser.getGlyphPatternPath(audioPath)
        if (!glyphPatternPath.isNullOrBlank()) {
            val pattern = GlyphComposerParser.parseFromFile(glyphPatternPath)
            if (pattern != null && GlyphComposerParser.isValid(pattern)) {
                if (DEBUG) Log.d(TAG, "Playing ringtone with Glyph Composer sync: $glyphPatternPath")
                runCatching {
                    glyphSyncPlayer?.play(Uri.parse(ringtoneUri.toString()), pattern)
                }.onFailure { e ->
                    if (DEBUG) Log.e(TAG, "Failed to play with GlyphSyncPlayer, fallback", e)
                    if (SettingsManager.useComposerFallback()) {
                        AnimationManager.playCall(SettingsManager.getGlyphCallAnimation())
                    }
                }
                return
            }
        }

        if (SettingsManager.useComposerFallback()) {
            if (DEBUG) Log.d(TAG, "No valid pattern, using fallback call animation")
            AnimationManager.playCall(SettingsManager.getGlyphCallAnimation())
        } else {
            if (DEBUG) Log.d(TAG, "No valid pattern and fallback disabled; do nothing")
        }
    }

    private fun playGlyphPatternOnly(pattern: GlyphPattern) {
        val frames = pattern.frames ?: return

        isComposerPatternPlaying = true
        composerPatternStartTime = System.currentTimeMillis()

        scheduleGlyphFrames(pattern, 0, composerPatternStartTime)
    }

    private fun scheduleGlyphFrames(pattern: GlyphPattern, frameIndex: Int, startTime: Long) {
        val frames = pattern.frames ?: return

        if (!isComposerPatternPlaying) return
        if (frameIndex >= frames.size) {
            isComposerPatternPlaying = false
            return
        }

        val frame = frames[frameIndex]
        val now = System.currentTimeMillis() - startTime
        var delayMs = frame.timestamp - now
        if (delayMs < 0) delayMs = 0

        threadHandler.postDelayed({
            if (!isComposerPatternPlaying) return@postDelayed
            activateGlyphFrame(frame)
            scheduleGlyphFrames(pattern, frameIndex + 1, startTime)
        }, delayMs)
    }

    private fun activateGlyphFrame(frame: GlyphPattern.GlyphFrame?) {
        if (frame?.zones == null) return
        val brightness = scaleBrightness(frame.brightness)
        AnimationManager.playGlyphFrame(this, frame.zones, brightness, frame.duration)
    }

    private fun scaleBrightness(patternBrightness: Int): Int {
        val userBrightness = SettingsManager.getGlyphBrightness()
        return (patternBrightness * userBrightness) / 100
    }

    private fun getRealPathFromUri(uri: Uri?): String? {
        if (uri == null) return null

        if ("file" == uri.scheme) return uri.path

        if ("content" == uri.scheme) {
            var cursor: Cursor? = null
            return try {
                val projection = arrayOf(android.provider.MediaStore.Audio.Media.DATA)
                cursor = contentResolver.query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val col =
                        cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                    cursor.getString(col)
                } else null
            } catch (e: Exception) {
                if (DEBUG) Log.e(TAG, "Error getting path from URI", e)
                null
            } finally {
                cursor?.close()
            }
        }

        return null
    }

    private val callReceiverReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    if (DEBUG) Log.d(TAG, "EXTRA_STATE_RINGING")
                    enableCallAnimation()
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    if (DEBUG) Log.d(TAG, "EXTRA_STATE_OFFHOOK")
                    disableCallAnimation()
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    if (DEBUG) Log.d(TAG, "EXTRA_STATE_IDLE")
                    disableCallAnimation()
                }
            }
        }
    }

    private val audioManagerOnModeChangedListener = AudioManager.OnModeChangedListener { mode ->
        if (mode != AudioManager.MODE_RINGTONE) {
            if (DEBUG) Log.d(TAG, "Audio mode changed: $mode")
            disableCallAnimation()
        }
    }
}