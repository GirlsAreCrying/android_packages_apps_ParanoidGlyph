package co.aospa.glyph.Tiles

import android.content.ContentResolver
import android.content.SharedPreferences
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import co.aospa.glyph.Utils.Prefs
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.SettingsManager
import co.aospa.glyph.R
import co.aospa.glyph.Utils.ServiceUtils

/** Quick settings tile: Music Visualizer **/
class MusicVisualizerTileService : TileService() {

    private lateinit var contentResolverRef: ContentResolver
    private lateinit var settingObserver: SettingObserver

    override fun onCreate() {
        super.onCreate()
        contentResolverRef = contentResolver
        settingObserver = SettingObserver()
        settingObserver.register(contentResolverRef)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateState()
    }

    override fun onClick() {
        super.onClick()
        if (SettingsManager.isGlyphEnabled()) {
            setEnabled(!getEnabled())
            updateState()
        }
    }

    override fun onDestroy() {
        settingObserver.unregister(contentResolverRef)
        super.onDestroy()
    }

    private fun updateState() {
        if (!SettingsManager.isGlyphEnabled()) {
            qsTile.subtitle = getString(R.string.glyph_accessibility_quick_settings_unavailable)
            qsTile.state = Tile.STATE_INACTIVE
        } else {
            val enabled = getEnabled()
            qsTile.subtitle =
                if (enabled) getString(R.string.glyph_accessibility_quick_settings_on)
                else getString(R.string.glyph_accessibility_quick_settings_off)
            qsTile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        }
        qsTile.updateTile()
    }

    private fun getEnabled(): Boolean =
        Prefs.default(this)
            .getBoolean(Constants.GLYPH_MUSIC_VISUALIZER_ENABLE, false)

    private fun setEnabled(enabled: Boolean) {
        val prefs: SharedPreferences = Prefs.default(this)
        prefs.edit().putBoolean(Constants.GLYPH_MUSIC_VISUALIZER_ENABLE, enabled).apply()
        ServiceUtils.checkGlyphService()
    }

    private inner class SettingObserver : ContentObserver(Handler()) {
        fun register(cr: ContentResolver) {
            cr.registerContentObserver(
                Settings.Secure.getUriFor(Constants.GLYPH_ENABLE),
                false,
                this
            )
        }

        fun unregister(cr: ContentResolver) {
            cr.unregisterContentObserver(this)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            if (uri == Settings.Secure.getUriFor(Constants.GLYPH_ENABLE)) {
                updateState()
            }
        }
    }
}
