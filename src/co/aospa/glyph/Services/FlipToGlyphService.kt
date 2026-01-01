package co.aospa.glyph.Services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import android.util.Log
import co.aospa.glyph.Manager.AnimationManager
import co.aospa.glyph.Manager.SettingsManager
import co.aospa.glyph.Sensors.FlipToGlyphSensor

class FlipToGlyphService : Service() {

    companion object {
        private const val TAG = "FlipToGlyphService"
        private const val DEBUG = true
    }

    private var isFlipped = false
    private var ringerMode = 0

    private lateinit var audioManager: AudioManager
    private lateinit var flipSensor: FlipToGlyphSensor
    private lateinit var ctx: Context

    override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")

        ctx = this
        flipSensor = FlipToGlyphSensor(this) { flipped -> onFlip(flipped) }
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        flipSensor.enable()
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        flipSensor.disable()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun onFlip(flipped: Boolean) {
        if (flipped == isFlipped) return
        if (DEBUG) Log.d(TAG, "Flipped: $flipped")

        if (flipped) {
            AnimationManager.playCsv(ctx, "flip")
            ringerMode = audioManager.ringerModeInternal
            val preferredMode = SettingsManager.getFlipRingerMode()
            if (DEBUG) Log.d(TAG, "Preferred ringer mode: $preferredMode")

            if (preferredMode != -1) {
                if (DEBUG) Log.d(TAG, "Setting ringer mode to: $preferredMode")
                audioManager.setRingerModeInternal(preferredMode)
            } else {
                if (DEBUG) Log.d(TAG, "Following system ringer mode: $ringerMode")
            }
        } else {
            val preferredMode = SettingsManager.getFlipRingerMode()
            if (preferredMode != -1) {
                audioManager.setRingerModeInternal(ringerMode)
            }
        }

        isFlipped = flipped
    }
}
