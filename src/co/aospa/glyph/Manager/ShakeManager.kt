package co.aospa.glyph.Manager

import android.content.Context
import android.content.Intent
import android.util.Log
import co.aospa.glyph.Utils.Prefs
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Services.ShakeDetectorService

object ShakeManager {
    private const val TAG = "GlyphShakeManager"
    private const val DEBUG = false

    @JvmStatic
    fun startShakeService(context: Context?) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot start service")
            return
        }

        if (isShakeEnabled(context) && SettingsManager.isGlyphEnabledIgnoreSchedule()) {
            try {
                val serviceIntent = Intent(context, ShakeDetectorService::class.java)
                context.startService(serviceIntent)
                if (DEBUG) Log.d(TAG, "Shake service start requested")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start shake service", e)
            }
        } else {
            if (DEBUG) Log.d(TAG, "Shake or Glyph disabled, not starting service")
        }
    }

    @JvmStatic
    fun stopShakeService(context: Context?) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot stop service")
            return
        }

        try {
            val serviceIntent = Intent(context, ShakeDetectorService::class.java)
            context.stopService(serviceIntent)
            if (DEBUG) Log.d(TAG, "Shake service stop requested")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop shake service", e)
        }
    }

    @JvmStatic
    fun restartShakeService(context: Context?) {
        if (DEBUG) Log.d(TAG, "Restarting shake service")
        stopShakeService(context)
        try { Thread.sleep(100) } catch (_: InterruptedException) {}
        startShakeService(context)
    }

    @JvmStatic
    fun isShakeEnabled(context: Context?): Boolean {
        if (context == null) return false
        return try {
            val prefs = Prefs.default(context)
            prefs.getBoolean(Constants.GLYPH_SHAKE_TORCH_ENABLE, false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read shake preference", e)
            false
        }
    }

    @JvmStatic
    fun setShakeEnabled(context: Context?, enabled: Boolean) {
        if (context == null) return

        try {
            val prefs = Prefs.default(context)
            prefs.edit().putBoolean(Constants.GLYPH_SHAKE_TORCH_ENABLE, enabled).apply()

            if (enabled) startShakeService(context) else stopShakeService(context)

            if (DEBUG) Log.d(TAG, "Shake gesture " + if (enabled) "enabled" else "disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set shake preference", e)
        }
    }
}
