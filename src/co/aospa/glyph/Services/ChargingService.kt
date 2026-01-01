package co.aospa.glyph.Services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import co.aospa.glyph.Manager.AnimationManager
import kotlin.math.sqrt

class ChargingService : Service() {

    companion object {
        private const val TAG = "GlyphChargingService"
        private const val DEBUG = true

        private const val ACCELEROMETER_THRESHOLD = 10.0f
        private const val ZFACEDOWN_THRESHOLD = -5.0f
    }

    private lateinit var thread: HandlerThread
    private lateinit var threadHandler: Handler

    private lateinit var batteryManager: BatteryManager
    private lateinit var sensorManager: SensorManager
    private lateinit var powerManager: PowerManager

    private var accelerometer: Sensor? = null

    private val dismissCharging = Runnable { AnimationManager.dismissCharging() }

    override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")

        thread = HandlerThread("ChargingService")
        thread.start()
        threadHandler = Handler(thread.looper)

        batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(powerMonitor, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        unregisterReceiver(powerMonitor)
        onPowerDisconnected()
        thread.quit()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun getBatteryLevel(): Int =
        batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

    private fun onPowerConnected() {
        if (DEBUG) {
            Log.d(TAG, "Power connected")
            Log.d(TAG, "Battery level: ${getBatteryLevel()}")
        }
        playChargingAnimation(wait = true)
        accelerometer?.let {
            sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun onPowerDisconnected() {
        if (DEBUG) Log.d(TAG, "Power disconnected")
        sensorManager.unregisterListener(sensorEventListener)
    }

    private fun playChargingAnimation(wait: Boolean) {
        if (threadHandler.hasCallbacks(dismissCharging)) {
            threadHandler.removeCallbacks(dismissCharging)
        }

        threadHandler.post {
            AnimationManager.playCharging(getBatteryLevel(), wait)
        }
        threadHandler.postDelayed(dismissCharging, 1190)
    }

    private val powerMonitor = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> onPowerConnected()
                Intent.ACTION_POWER_DISCONNECTED -> onPowerDisconnected()
            }
        }
    }

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val acceleration = sqrt(x * x + y * y + z * z)

            if (acceleration > ACCELEROMETER_THRESHOLD &&
                z <= ZFACEDOWN_THRESHOLD &&
                !powerManager.isInteractive
            ) {
                playChargingAnimation(wait = false)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
}
