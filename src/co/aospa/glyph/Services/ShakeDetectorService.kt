package co.aospa.glyph.Services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import co.aospa.glyph.Utils.Prefs
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.SettingsManager
import co.aospa.glyph.Manager.StatusManager
import co.aospa.glyph.Utils.FileUtils
import co.aospa.glyph.Utils.ResourceUtils
import kotlin.math.sqrt

class ShakeDetectorService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "GlyphShakeDetector"
        private const val DEBUG = false

        private const val SHAKE_TIME_WINDOW = 500
        private const val SHAKE_COUNT_THRESHOLD = 2

        private const val DEFAULT_SENSITIVITY = 35
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var sharedPrefs: SharedPreferences
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null

    private var lastShakeTime = 0L
    private var shakeCount = 0
    private var currentThreshold = DEFAULT_SENSITIVITY.toFloat()

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "Service created")

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sharedPrefs = Prefs.default(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG:ShakeWakeLock")

        loadSensitivity()
        registerSensorListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Service started")
        loadSensitivity()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun loadSensitivity() {
        val sensitivity = sharedPrefs.getInt(Constants.GLYPH_SHAKE_SENSITIVITY, DEFAULT_SENSITIVITY)
        currentThreshold = sensitivity.toFloat()
        if (DEBUG) Log.d(TAG, "Shake sensitivity loaded: $currentThreshold")
    }

    private fun registerSensorListener() {
        if (accelerometer != null && isShakeEnabled() && SettingsManager.isGlyphEnabled()) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            if (DEBUG) Log.d(TAG, "Accelerometer listener registered with threshold: $currentThreshold")
        } else {
            if (DEBUG) Log.d(TAG, "Shake disabled or Glyph disabled, listener not registered")
        }
    }

    private fun unregisterSensorListener() {
        sensorManager.unregisterListener(this)
        if (DEBUG) Log.d(TAG, "Accelerometer listener unregistered")
    }

    private fun isShakeEnabled(): Boolean =
        sharedPrefs.getBoolean(Constants.GLYPH_SHAKE_TORCH_ENABLE, false)

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        // Allow shake even during schedule (torch)
        if (!isShakeEnabled() || !SettingsManager.isGlyphEnabledIgnoreSchedule()) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = (sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH)

        if (acceleration > currentThreshold) {
            val currentTime = SystemClock.elapsedRealtime()

            if (currentTime - lastShakeTime < SHAKE_TIME_WINDOW) {
                shakeCount++
                if (DEBUG) Log.d(TAG, "Shake detected, count: $shakeCount, accel: $acceleration")

                if (shakeCount >= SHAKE_COUNT_THRESHOLD) {
                    onShakeDetected()
                    shakeCount = 0
                }
            } else {
                shakeCount = 1
                if (DEBUG) Log.d(TAG, "First shake detected, accel: $acceleration")
            }

            lastShakeTime = currentTime
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun onShakeDetected() {
        if (DEBUG) Log.d(TAG, "Shake gesture triggered, toggling torch")

        if (!SettingsManager.isGlyphEnabledIgnoreSchedule()) {
            if (DEBUG) Log.d(TAG, "Glyph completely disabled, ignoring shake")
            return
        }

        performHapticFeedback()

        wakeLock?.let {
            if (!it.isHeld) it.acquire(3000)
        }

        try {
            val currentState = StatusManager.isAllLedActive()
            val newState = !currentState

            StatusManager.setAllLedsActive(newState)
            FileUtils.writeAllLed(if (newState) Constants.getMaxBrightness() else 0)

            if (StatusManager.isEssentialLedActive() && !newState) {
                FileUtils.writeSingleLed(
                    ResourceUtils.getInteger("glyph_settings_notifs_essential_led"),
                    (Constants.getMaxBrightness() / 100f) * 7f
                )
            }

            showToastNotification(newState)

            if (DEBUG) Log.d(TAG, "Torch toggled to: ${if (newState) "ON" else "OFF"}")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling torch", e)
        } finally {
            wakeLock?.let { if (it.isHeld) it.release() }
        }
    }

    private fun performHapticFeedback() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(50)
        }

        if (DEBUG) Log.d(TAG, "Haptic feedback triggered")
    }

    private fun showToastNotification(torchOn: Boolean) {
        val handler = android.os.Handler(mainLooper)
        handler.post {
            val message = if (torchOn) "Glyph Torch ON" else "Glyph Torch OFF"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            if (DEBUG) Log.d(TAG, "Toast shown: $message")
        }
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Service destroyed")
        unregisterSensorListener()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}
