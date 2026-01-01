package co.aospa.glyph.Services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.FileObserver
import android.os.IBinder
import android.util.Log
import co.aospa.glyph.Manager.AnimationManager
import co.aospa.glyph.Utils.FileUtils
import co.aospa.glyph.Utils.ResourceUtils

class PowershareService : Service() {

    companion object {
        private const val TAG = "GlyphPowershareService"
        private const val DEBUG = true

        private val POWERSHARE_ACTIVE: String =
            ResourceUtils.getString("glyph_settings_paths_powershare_active_absolute")
        private val POWERSHARE_ENABLED: String =
            ResourceUtils.getString("glyph_settings_paths_powershare_enabled_absolute")
    }

    private lateinit var powershareActiveObserver: PowershareActiveObserver
    private lateinit var ctx: Context

    override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")
        powershareActiveObserver = PowershareActiveObserver()
        ctx = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        fileObserver.startWatching()
        powershareActiveObserver.startWatching()
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        fileObserver.stopWatching()
        powershareActiveObserver.stopWatching()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun onPowershareEnabled() {
        if (DEBUG) Log.e(TAG, "onPowershareEnabled")
        powershareActiveObserver.continueWatching()
    }

    private fun onPowershareDisabled() {
        if (DEBUG) Log.e(TAG, "onPowershareDisabled")
        powershareActiveObserver.pauseWatching()
    }

    private val fileObserver: FileObserver = object : FileObserver(POWERSHARE_ENABLED, MODIFY) {

        override fun onEvent(event: Int, path: String?) {
            checkIfPowerShareIsEnabled()
        }

        override fun startWatching() {
            if (DEBUG) Log.e(TAG, "FileObserver: startWatching")
            checkIfPowerShareIsEnabled()
            super.startWatching()
        }

        private fun checkIfPowerShareIsEnabled() {
            val enabled = FileUtils.readLineInt(POWERSHARE_ENABLED)
            if (DEBUG) Log.e(TAG, "FileObserver: checkIfPowerShareIsEnabled: $enabled")
            if (enabled == 1) onPowershareEnabled() else onPowershareDisabled()
        }
    }

    private inner class PowershareActiveObserver : Thread() {

        private var lastState = false
        private var paused = true
        private var ended = false

        private val lock = Object()

        fun startWatching() {
            if (DEBUG) Log.e(TAG, "PowershareActiveObserver: startWatching")
            if (isAlive) return
            start()
        }

        fun continueWatching() {
            if (DEBUG) Log.e(TAG, "PowershareActiveObserver: continueWatching")
            if (!paused) return
            paused = false
            synchronized(lock) { lock.notify() }
        }

        fun pauseWatching() {
            if (DEBUG) Log.e(TAG, "PowershareActiveObserver: pauseWatching")
            if (paused) return
            lastState = false
            paused = true
        }

        fun stopWatching() {
            if (DEBUG) Log.e(TAG, "PowershareActiveObserver: stopWatching")
            if (paused) continueWatching()
            ended = true
        }

        private fun updatePowershareState() {
            val active = FileUtils.readLineInt(POWERSHARE_ACTIVE)
            if (DEBUG) Log.d(TAG, "updatePowershareState: $active")

            if (active == 1) {
                if (lastState) return
                lastState = true
                AnimationManager.playCsv(ctx, "powershare", true)
            } else {
                lastState = false
            }
        }

        override fun run() {
            if (DEBUG) Log.e(TAG, "PowershareActiveObserver: run")
            while (!ended) {
                synchronized(lock) {
                    if (paused) {
                        try {
                            if (DEBUG) Log.d(TAG, "lock.wait()")
                            lock.wait()
                        } catch (_: InterruptedException) {
                        }
                    }
                }

                updatePowershareState()

                try {
                    sleep(500)
                } catch (_: InterruptedException) {
                }
            }
        }
    }
}
