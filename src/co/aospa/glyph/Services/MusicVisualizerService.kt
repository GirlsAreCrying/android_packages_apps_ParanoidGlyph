package co.aospa.glyph.Services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import co.aospa.glyph.Manager.AnimationManager
import co.aospa.glyph.Manager.StatusManager
import kotlin.math.abs
import kotlin.math.sqrt

class MusicVisualizerService : Service() {

    companion object {
        private const val TAG = "GlyphMusicVisualizerService"
        private const val DEBUG = true

        private const val LOW_FREQUENCY = 200
        private const val MID_LOW_FREQUENCY = 500
        private const val MID_FREQUENCY = 1500
        private const val MID_HIGH_FREQUENCY = 5000
        private const val HIGH_FREQUENCY = 10000
    }

    private lateinit var audioManager: AudioManager
    private lateinit var thread: HandlerThread
    private lateinit var handler: Handler

    private var visualizer: Visualizer? = null
    private var bufferSize: Int = 0

    private lateinit var runningSoundAvg: DoubleArray
    private lateinit var currentAvgEnergyOneSec: DoubleArray
    private var numberOfSamplesInOneSec: Int = 0
    private var systemTimeStartSec: Long = 0L

    override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")
        super.onCreate()

        thread = HandlerThread("MusicVisualizerService")
        thread.start()
        handler = Handler(thread.looper)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        handler.post {
            val v = runCatching { Visualizer(0) }.getOrNull()
            if (v == null) {
                if (DEBUG) Log.e(TAG, "Failed to create Visualizer")
                return@post
            }
            visualizer = v

            bufferSize = Visualizer.getCaptureSizeRange()[1]
            v.captureSize = bufferSize

            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer,
                        waveform: ByteArray,
                        samplingRate: Int
                    ) = Unit

                    override fun onFftDataCapture(
                        visualizer: Visualizer,
                        fft: ByteArray,
                        samplingRate: Int
                    ) {
                        if (audioManager.isMusicActive && StatusManager.isGlyphIdle()) {
                            processAudioFFT(fft, samplingRate)
                        }
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                false,
                true
            )

            v.enabled = true

            runningSoundAvg = DoubleArray(5)
            currentAvgEnergyOneSec = DoubleArray(5) { -1.0 }
            numberOfSamplesInOneSec = 0
            systemTimeStartSec = System.currentTimeMillis()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        handler.post {
            runCatching { visualizer?.enabled = false }
            runCatching { visualizer?.release() }
            visualizer = null
        }
        runCatching { thread.quitSafely() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun processAudioFFT(audioBytes: ByteArray, samplingRate: Int) {
        if (!::runningSoundAvg.isInitialized || !::currentAvgEnergyOneSec.isInitialized) return
        if (audioBytes.size < 4) return

        val captureSize = audioBytes.size / 2.0
        val sampleRate = samplingRate / 2000.0 // keep as double to avoid truncation
        var k = 2

        fun nextFreq(k: Int): Double = (k / 2.0 * sampleRate) / captureSize

        var nextFrequency = nextFreq(k)

        fun consumeUntil(threshold: Int, initial: Double = 0.0): Pair<Double, Int> {
            var energy = initial
            var kk = k
            var nf = nextFrequency
            while (nf < threshold && kk + 1 < audioBytes.size) {
                val re = audioBytes[kk].toInt().toDouble()
                val im = audioBytes[kk + 1].toInt().toDouble()
                energy += sqrt(re * re + im * im)
                kk += 2
                nf = nextFreq(kk)
            }
            k = kk
            nextFrequency = nf
            return energy to (kk / 2)
        }

        var energySum: Double = abs(audioBytes[0].toInt()).toDouble()
        val (lowEnergy, lowCount) = consumeUntil(LOW_FREQUENCY, energySum)
        val lowAvg = if (lowCount > 0) lowEnergy / lowCount else 0.0
        runningSoundAvg[0] += lowAvg
        if (currentAvgEnergyOneSec[0] > 0 && lowAvg > currentAvgEnergyOneSec[0]) {
            if (DEBUG) Log.d(TAG, "Low frequency band beat detected")
            AnimationManager.playMusic("low")
        }

        val (midLowEnergy, midLowCount) = consumeUntil(MID_LOW_FREQUENCY, 0.0)
        val midLowAvg = if (midLowCount > 0) midLowEnergy / midLowCount else 0.0
        runningSoundAvg[1] += midLowAvg
        if (currentAvgEnergyOneSec[1] > 0 && midLowAvg > currentAvgEnergyOneSec[1]) {
            if (DEBUG) Log.d(TAG, "Mid-low frequency band beat detected")
            AnimationManager.playMusic("mid_low")
        }

        val (midEnergy, midCount) = consumeUntil(MID_FREQUENCY, 0.0)
        val midAvg = if (midCount > 0) midEnergy / midCount else 0.0
        runningSoundAvg[2] += midAvg
        if (currentAvgEnergyOneSec[2] > 0 && midAvg > currentAvgEnergyOneSec[2]) {
            if (DEBUG) Log.d(TAG, "Mid frequency band beat detected")
            AnimationManager.playMusic("mid")
        }

        val (midHighEnergy, midHighCount) = consumeUntil(MID_HIGH_FREQUENCY, 0.0)
        val midHighAvg = if (midHighCount > 0) midHighEnergy / midHighCount else 0.0
        runningSoundAvg[3] += midHighAvg
        if (currentAvgEnergyOneSec[3] > 0 && midHighAvg > currentAvgEnergyOneSec[3]) {
            if (DEBUG) Log.d(TAG, "Mid-high frequency band beat detected")
            AnimationManager.playMusic("mid_high")
        }

        energySum = abs(audioBytes[1].toInt()).toDouble()
        val (highEnergy, highCount) = consumeUntil(HIGH_FREQUENCY, energySum)
        val highAvg = if (highCount > 0) highEnergy / highCount else 0.0
        runningSoundAvg[4] += highAvg
        if (currentAvgEnergyOneSec[4] > 0 && highAvg > currentAvgEnergyOneSec[4]) {
            if (DEBUG) Log.d(TAG, "High frequency band beat detected")
            AnimationManager.playMusic("high")
        }

        numberOfSamplesInOneSec++

        val currentTime = System.currentTimeMillis()
        if (currentTime - systemTimeStartSec >= 1000) {
            val denom = numberOfSamplesInOneSec.coerceAtLeast(1).toDouble()
            for (i in 0..4) {
                currentAvgEnergyOneSec[i] = runningSoundAvg[i] / denom
                runningSoundAvg[i] = 0.0
            }
            numberOfSamplesInOneSec = 0
            systemTimeStartSec = currentTime
        }
    }
}