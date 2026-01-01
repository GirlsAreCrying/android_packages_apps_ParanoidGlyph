package co.aospa.glyph.Tiles

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.SettingsManager
import co.aospa.glyph.Manager.StatusManager
import co.aospa.glyph.R
import co.aospa.glyph.Utils.FileUtils
import co.aospa.glyph.Utils.ResourceUtils

class TorchTileService : TileService() {

    companion object {
        private const val ACTION_UPDATE_TILE = "co.aospa.glyph.UPDATE_TORCH_TILE"

        @JvmStatic
        fun requestTileUpdate(context: Context) {
            context.sendBroadcast(Intent(ACTION_UPDATE_TILE))
        }
    }

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(ACTION_UPDATE_TILE)
        // API 33+: Context.RECEIVER_NOT_EXPORTED
        runCatching {
            registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        }.getOrElse {
            @Suppress("DEPRECATION")
            registerReceiver(updateReceiver, filter)
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(updateReceiver) }
        super.onDestroy()
    }

    override fun onStartListening() {
        super.onStartListening()
        ensureContext()
        updateState()
    }

    override fun onClick() {
        super.onClick()
        ensureContext()

        if (!SettingsManager.isGlyphEnabledIgnoreSchedule()) {
            updateState()
            return
        }

        setEnabled(!getEnabled())
        updateState()
    }

    private fun ensureContext() {
        // Some QS flows can start service in a fresh process.
        runCatching { Constants.CONTEXT }.getOrNull() ?: run {
            Constants.CONTEXT = applicationContext
        }
    }

    private fun updateState() {
        val tile = qsTile ?: return

        val glyphEnabled = SettingsManager.isGlyphEnabledIgnoreSchedule()
        if (!glyphEnabled) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.subtitle = getString(R.string.glyph_accessibility_quick_settings_disabled)
            tile.updateTile()
            return
        }

        val enabled = getEnabled()
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle =
            if (enabled) getString(R.string.glyph_accessibility_quick_settings_on)
            else getString(R.string.glyph_accessibility_quick_settings_off)
        tile.updateTile()
    }

    private fun getEnabled(): Boolean = StatusManager.isAllLedActive()

    private fun setEnabled(enabled: Boolean) {
        StatusManager.setAllLedsActive(enabled)
        FileUtils.writeAllLed(if (enabled) Constants.getMaxBrightness() else 0)

        // Restore essential LED while turning off all LEDs.
        if (StatusManager.isEssentialLedActive() && !enabled) {
            val led = ResourceUtils.getInteger("glyph_settings_notifs_essential_led")
            val level = (Constants.getMaxBrightness() / 100f) * 7f
            FileUtils.writeSingleLed(led, level)
        }
    }
}