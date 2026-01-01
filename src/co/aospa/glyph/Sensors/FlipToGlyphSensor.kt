package co.aospa.glyph.Sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.time.Duration
import java.util.Objects
import java.util.function.Consumer
import kotlin.math.abs

class FlipToGlyphSensor(
    private val context: Context,
    onFlip: (Boolean) -> Unit
) : SensorEventListener {

    companion object {
        private const val DEBUG = true
        private const val TAG = "FlipToGlyphSensor"
        private const val MOVING_AVERAGE_WEIGHT = 0.5f
    }

    constructor(context: Context, onFlip: Consumer<Boolean>) : this(context, { onFlip.accept(it) })

    private var isFlipped = false
    private val onFlipCb: (Boolean) -> Unit = Objects.requireNonNull(onFlip)

    private val sensorManager: SensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, /*wakeUp=*/false)

    private val timeThreshold: Duration = Duration.ofMillis(1_000L)
    private val accelerationThreshold = 0.2f
    private val zAccelerationThreshold = -9.5f
    private val zAccelerationThresholdLenient = zAccelerationThreshold + 1.0f

    private var prevAcceleration = 0f
    private var prevAccelerationTime = 0L

    private var zIsFaceDown = false
    private var zFaceDownTime = 0L

    private val currentXYAcceleration = ExponentialMovingAverage(MOVING_AVERAGE_WEIGHT)
    private val currentZAcceleration = ExponentialMovingAverage(MOVING_AVERAGE_WEIGHT)

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        currentXYAcceleration.updateMovingAverage(x * x + y * y)
        currentZAcceleration.updateMovingAverage(event.values[2])

        val curTime = event.timestamp

        if (abs(currentXYAcceleration.movingAverage - prevAcceleration) > accelerationThreshold) {
            prevAcceleration = currentXYAcceleration.movingAverage
            prevAccelerationTime = curTime
        }

        val moving = (curTime - prevAccelerationTime) <= timeThreshold.toNanos()

        val zThreshold = if (isFlipped) zAccelerationThresholdLenient else zAccelerationThreshold
        val isCurrentlyFaceDown = currentZAcceleration.movingAverage < zThreshold

        val isFaceDownForPeriod =
            isCurrentlyFaceDown &&
                zIsFaceDown &&
                (curTime - zFaceDownTime) > timeThreshold.toNanos()

        if (isCurrentlyFaceDown && !zIsFaceDown) {
            zFaceDownTime = curTime
            zIsFaceDown = true
        } else if (!isCurrentlyFaceDown) {
            zIsFaceDown = false
        }

        if (!moving && isFaceDownForPeriod && !isFlipped) {
            internalOnFlip(true)
        } else if (!isFaceDownForPeriod && isFlipped) {
            internalOnFlip(false)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun internalOnFlip(flipped: Boolean) {
        if (DEBUG) Log.d(TAG, "Flipped: $flipped")
        onFlipCb(flipped)
        isFlipped = flipped
    }

    fun enable() {
        if (DEBUG) Log.d(TAG, "Enabling Sensor")
        val acc = accelerometer
        if (acc == null) {
            Log.w(TAG, "No accelerometer sensor available")
            return
        }
        sensorManager.registerListener(
            this,
            acc,
            SensorManager.SENSOR_DELAY_NORMAL,
            context.resources.getInteger(
                com.android.internal.R.integer.config_flipToScreenOffMaxLatencyMicros
            )
        )
    }

    fun disable() {
        if (DEBUG) Log.d(TAG, "Disabling Sensor")
        internalOnFlip(false)
        accelerometer?.let { sensorManager.unregisterListener(this, it) }
    }

    private class ExponentialMovingAverage(
        private val alpha: Float,
        private val initialAverage: Float = 0.0f
    ) {
        var movingAverage: Float = initialAverage
            private set

        fun updateMovingAverage(newValue: Float) {
            movingAverage = newValue + alpha * (movingAverage - newValue)
        }

        fun reset() {
            movingAverage = initialAverage
        }
    }
}