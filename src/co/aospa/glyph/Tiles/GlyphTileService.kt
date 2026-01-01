package co.aospa.glyph.Tiles

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import co.aospa.glyph.Manager.SettingsManager
import co.aospa.glyph.R
import co.aospa.glyph.Utils.ServiceUtils

/** Quick settings tile: Glyph **/
class GlyphTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateState()
    }

    override fun onClick() {
        super.onClick()
        setEnabled(!getEnabled())
        updateState()
    }

    private fun updateState() {
        val enabled = getEnabled()
        qsTile.subtitle =
            if (enabled) getString(R.string.glyph_accessibility_quick_settings_on)
            else getString(R.string.glyph_accessibility_quick_settings_off)

        qsTile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.updateTile()
    }

    private fun getEnabled(): Boolean = SettingsManager.isGlyphEnabled()

    private fun setEnabled(enabled: Boolean) {
        SettingsManager.enableGlyph(enabled)
        ServiceUtils.checkGlyphService()
    }
}
