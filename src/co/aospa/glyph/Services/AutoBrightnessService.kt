package co.aospa.glyph.Services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.AnimationManager
import co.aospa.glyph.Manager.StatusManager
import co.aospa.glyph.Utils.ResourceUtils

class AutoBrightnessService : Service() {

    companion object {
        private const val TAG = "GlyphAutoBrightnessService"
        private const val DEBUG = true
    }

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var sensorType: Int = Sensor.TYPE_LIGHT

    private val autoBrightnessLux: IntArray by lazy {
        ResourceUtils.getIntArray("glyph_auto_brightness_levels")
    }

    private val brightnessValues: IntArray by lazy {
        Constants.getBrightnessLevels()
    }

    override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Find configured sensor by stringType
        val sensorName = ResourceUtils.getString("glyph_light_sensor")
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        for (s in sensors) {
            if (sensorName == s.stringType) {
                sensorType = s.type
                break
            }
        }

        lightSensor = sensorManager.getDefaultSensor(sensorType)
        lightSensor?.let {
            sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        sensorManager.unregisterListener(sensorEventListener)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val lux = event.values[0].toInt()
            var luxIndex = 0

            for (i in 1 until autoBrightnessLux.size) {
                if (lux < autoBrightnessLux[i]) break
                if (lux >= autoBrightnessLux[i]) luxIndex = i
            }

            val brightnessValue = brightnessValues[luxIndex]

            if (brightnessValue != Constants.getBrightness()) {
                if (DEBUG) {
                    val ledLux = autoBrightnessLux[luxIndex]
                    Log.d(
                        TAG,
                        "Brightness changed: RealLux: $lux | BrightnessLux: $ledLux | BrightnessValue: $brightnessValue"
                    )
                }
                Constants.setBrightness(brightnessValue)
                if (StatusManager.isEssentialLedActive()) {
                    AnimationManager.playEssential()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
}
