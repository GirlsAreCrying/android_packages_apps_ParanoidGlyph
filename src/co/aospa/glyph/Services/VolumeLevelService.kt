package co.aospa.glyph.Services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.AudioSystem
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import co.aospa.glyph.Manager.AnimationManager
import kotlin.math.roundToInt

class VolumeLevelService : Service() {

    companion object {
        private const val TAG = "GlyphVolumeLevelService"
        private const val DEBUG = true
        private const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
    }

    private lateinit var thread: HandlerThread
    private lateinit var threadHandler: Handler
    private lateinit var volumeReceiver: VolumeChangeReceiver

    private lateinit var ctx: Context
    private lateinit var audioManager: AudioManager

    private val dismissVolume = Runnable { AnimationManager.dismissVolume(ctx) }

    override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")

        ctx = this

        thread = HandlerThread("VolumeLevelService")
        thread.start()
        threadHandler = Handler(thread.looper)

        audioManager = getSystemService(AudioManager::class.java)

        volumeReceiver = VolumeChangeReceiver()
        registerReceiver(volumeReceiver, IntentFilter(ACTION_VOLUME_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        unregisterReceiver(volumeReceiver)
        thread.quit()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class VolumeChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_VOLUME_CHANGED) return

            val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
            val currentVolume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
            val oldVolume = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1)

            if (streamType in 0..AudioSystem.NUM_STREAMS && currentVolume >= 0 && oldVolume >= 0) {
                val maxVolume = audioManager.getStreamMaxVolume(streamType)
                val oldPercent = (100.0 / maxVolume * oldVolume).roundToInt()
                val currentPercent = (100.0 / maxVolume * currentVolume).roundToInt()

                if (oldPercent != currentPercent) {
                    if (threadHandler.hasCallbacks(dismissVolume)) {
                        threadHandler.removeCallbacks(dismissVolume)
                    }

                    if (DEBUG) {
                        Log.d(
                            TAG,
                            "Volume level changed for stream type $streamType: old=$oldPercent, current=$currentPercent"
                        )
                    }

                    threadHandler.post {
                        AnimationManager.playVolume(context, currentPercent, false)
                    }
                    threadHandler.postDelayed(dismissVolume, 3000)
                }
            }
        }
    }
}
