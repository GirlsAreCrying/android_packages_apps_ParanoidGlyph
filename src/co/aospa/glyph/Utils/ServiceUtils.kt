package co.aospa.glyph.Utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.AnimationManager
import co.aospa.glyph.Manager.SettingsManager
import co.aospa.glyph.Manager.ShakeManager
import co.aospa.glyph.Manager.StatusManager
import co.aospa.glyph.Services.AutoBrightnessService
import co.aospa.glyph.Services.CallReceiverService
import co.aospa.glyph.Services.ChargingService
import co.aospa.glyph.Services.FlipToGlyphService
import co.aospa.glyph.Services.MusicVisualizerService
import co.aospa.glyph.Services.PowershareService
import co.aospa.glyph.Services.ThirdPartyService
import co.aospa.glyph.Services.VolumeLevelService

object ServiceUtils {
    private const val TAG = "GlyphServiceUtils"
    private const val DEBUG = true

    private val context: Context
        get() = Constants.CONTEXT

    @JvmStatic
    fun isNotificationServiceEnabled(): Boolean {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_NOTIFICATION_LISTENERS
        )

        if (flat != null) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                    return true
                }
            }
        }
        return false
    }

    private fun startCallReceiverService() {
        if (DEBUG) Log.d(TAG, "Starting Glyph call receiver service")
        context.startServiceAsUser(Intent(context, CallReceiverService::class.java), UserHandle.CURRENT)
    }

    private fun stopCallReceiverService() {
        if (DEBUG) Log.d(TAG, "Stopping Glyph call receiver service")
        context.stopServiceAsUser(Intent(context, CallReceiverService::class.java), UserHandle.CURRENT)
    }

    private fun startChargingService() {
        if (DEBUG) Log.d(TAG, "Starting Glyph charging service")
        context.startServiceAsUser(Intent(context, ChargingService::class.java), UserHandle.CURRENT)
    }

    private fun stopChargingService() {
        if (DEBUG) Log.d(TAG, "Stopping Glyph charging service")
        context.stopServiceAsUser(Intent(context, ChargingService::class.java), UserHandle.CURRENT)
    }

    private fun startFlipToGlyphService() {
        if (DEBUG) Log.d(TAG, "Starting Flip to Glyph service")
        context.startServiceAsUser(Intent(context, FlipToGlyphService::class.java), UserHandle.CURRENT)
    }

    private fun stopFlipToGlyphService() {
        if (DEBUG) Log.d(TAG, "Stopping Flip to Glyph service")
        context.stopServiceAsUser(Intent(context, FlipToGlyphService::class.java), UserHandle.CURRENT)
    }

    @JvmStatic
    fun startMusicVisualizerService() {
        if (DEBUG) Log.d(TAG, "Starting Music Visualizer service")
        context.startServiceAsUser(Intent(context, MusicVisualizerService::class.java), UserHandle.CURRENT)
    }

    private fun stopMusicVisualizerService() {
        if (DEBUG) Log.d(TAG, "Stopping Music Visualizer service")
        context.stopServiceAsUser(Intent(context, MusicVisualizerService::class.java), UserHandle.CURRENT)
    }

    private fun startPowershareService() {
        if (DEBUG) Log.d(TAG, "Starting Glyph powershare service")
        context.startServiceAsUser(Intent(context, PowershareService::class.java), UserHandle.CURRENT)
    }

    private fun stopPowershareService() {
        if (DEBUG) Log.d(TAG, "Stopping Glyph powershare service")
        context.stopServiceAsUser(Intent(context, PowershareService::class.java), UserHandle.CURRENT)
    }

    @JvmStatic
    fun startVolumeLevelService() {
        if (DEBUG) Log.d(TAG, "Starting Volume Level service")
        context.startServiceAsUser(Intent(context, VolumeLevelService::class.java), UserHandle.CURRENT)
    }

    private fun stopVolumeLevelService() {
        if (DEBUG) Log.d(TAG, "Stopping Volume Listener service")
        context.stopServiceAsUser(Intent(context, VolumeLevelService::class.java), UserHandle.CURRENT)
    }

    private fun startAutoBrightnessService() {
        if (DEBUG) Log.d(TAG, "Starting Auto Brightness service")
        context.startServiceAsUser(Intent(context, AutoBrightnessService::class.java), UserHandle.CURRENT)
    }

    private fun stopAutoBrightnessService() {
        if (DEBUG) Log.d(TAG, "Stopping Auto Brightness service")
        context.stopServiceAsUser(Intent(context, AutoBrightnessService::class.java), UserHandle.CURRENT)
    }

    @JvmStatic
    fun startThirdPartyService() {
        if (DEBUG) Log.d(TAG, "Starting ThirdParty service")
        context.startServiceAsUser(Intent(context, ThirdPartyService::class.java), UserHandle.CURRENT)
    }

    private fun stopThirdPartyService() {
        if (DEBUG) Log.d(TAG, "Stopping ThirdParty service")
        context.stopServiceAsUser(Intent(context, ThirdPartyService::class.java), UserHandle.CURRENT)
    }

    @JvmStatic
    fun checkGlyphService() {
        if (SettingsManager.getGlyphBrightness() != Constants.getBrightness()) {
            Constants.setBrightness(SettingsManager.getGlyphBrightness())
            startThirdPartyService()
            if (StatusManager.isEssentialLedActive()) {
                AnimationManager.playEssential()
            }
        }

        val glyphEnabled = SettingsManager.isGlyphEnabled()
        val glyphBaseEnabled = SettingsManager.isGlyphEnabledIgnoreSchedule()

        if (glyphEnabled) {
            if (SettingsManager.isGlyphChargingEnabled()) startChargingService() else stopChargingService()
            if (SettingsManager.isGlyphPowershareEnabled()) startPowershareService() else stopPowershareService()
            if (SettingsManager.isGlyphCallEnabled()) startCallReceiverService() else stopCallReceiverService()
            if (SettingsManager.isGlyphFlipEnabled()) startFlipToGlyphService() else stopFlipToGlyphService()
            if (SettingsManager.isGlyphMusicVisualizerEnabled()) startMusicVisualizerService() else stopMusicVisualizerService()
            if (SettingsManager.isGlyphVolumeLevelEnabled()) startVolumeLevelService() else stopVolumeLevelService()
            if (SettingsManager.isGlyphAutoBrightnessEnabled()) startAutoBrightnessService() else stopAutoBrightnessService()
        } else {
            stopChargingService()
            stopPowershareService()
            stopCallReceiverService()
            stopFlipToGlyphService()
            stopMusicVisualizerService()
            stopVolumeLevelService()
            stopAutoBrightnessService()
        }

        if (glyphBaseEnabled && ShakeManager.isShakeEnabled(context)) {
            ShakeManager.startShakeService(context)
        } else {
            ShakeManager.stopShakeService(context)
        }

        if (glyphBaseEnabled) startThirdPartyService() else stopThirdPartyService()
    }
}
