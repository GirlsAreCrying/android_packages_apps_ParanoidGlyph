package co.aospa.glyph.Manager

import android.provider.Settings
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Utils.Prefs
import co.aospa.glyph.Utils.ResourceUtils

object SettingsManager {

    private val context get() = Constants.CONTEXT

    @JvmStatic
    fun enableGlyph(enabled: Boolean) {
        Prefs.default(context).edit()
            .putBoolean(Constants.GLYPH_ENABLE, enabled)
            .apply()
    }

    @JvmStatic
    fun isGlyphEnabled(): Boolean {
        return Settings.Secure.getInt(context.contentResolver, Constants.GLYPH_ENABLE, 1) != 0 ||
            Prefs.default(context).getBoolean(Constants.GLYPH_ENABLE, true)
    }

    @JvmStatic
    fun isGlyphFlipEnabled(): Boolean {
        return Prefs.default(context).getBoolean(Constants.GLYPH_FLIP_ENABLE, false) && isGlyphEnabled()
    }

    @JvmStatic
    fun getGlyphBrightness(): Int {
        val def = ResourceUtils.getDefaultBrightness(context)
        return Prefs.default(context).getInt(Constants.GLYPH_BRIGHTNESS, def).coerceAtLeast(1)
    }

    @JvmStatic
    fun isGlyphAutoBrightnessEnabled(): Boolean {
        return Prefs.default(context).getBoolean(Constants.GLYPH_AUTO_BRIGHTNESS_ENABLE, false) && isGlyphEnabled()
    }

    @JvmStatic
    fun isGlyphChargingLevelEnabled(): Boolean {
        return Prefs.default(context).getBoolean(Constants.GLYPH_CHARGING_LEVEL_ENABLE, false) && isGlyphEnabled()
    }

    @JvmStatic
    fun isGlyphPowershareEnabled(): Boolean {
        return Prefs.default(context).getBoolean(Constants.GLYPH_CHARGING_POWERSHARE_ENABLE, false) && isGlyphEnabled()
    }

    @JvmStatic
    fun isGlyphCallEnabled(): Boolean {
        return Settings.Secure.getInt(context.contentResolver, Constants.GLYPH_CALL_ENABLE, 1) != 0 &&
            isGlyphEnabled()
    }

    @JvmStatic
    fun isGlyphCallSubEnabled(): Boolean {
        return Prefs.default(context).getBoolean(Constants.GLYPH_CALL_SUB_ENABLE, true) && isGlyphCallEnabled()
    }

    @JvmStatic
    fun getGlyphCallAnimation(): String {
        val def = ResourceUtils.getDefaultCallAnimation(context)
        return Prefs.default(context).getString(Constants.GLYPH_CALL_SUB_ANIMATIONS, def) ?: def
    }

    @JvmStatic
    fun isGlyphNotifsEnabled(): Boolean {
        return Settings.Secure.getInt(context.contentResolver, Constants.GLYPH_NOTIFS_ENABLE, 1) != 0 &&
            isGlyphEnabled()
    }

    @JvmStatic
    fun getGlyphNotifsAnimation(): String {
        val def = ResourceUtils.getDefaultNotifsAnimation(context)
        return Prefs.default(context).getString(Constants.GLYPH_NOTIFS_SUB_ANIMATIONS, def) ?: def
    }

    @JvmStatic
    fun isGlyphMusicVisualizerEnabled(): Boolean {
        return Prefs.default(context).getBoolean(Constants.GLYPH_MUSIC_VISUALIZER_ENABLE, false) && isGlyphEnabled()
    }

    @JvmStatic
    fun isGlyphVolumeLevelEnabled(): Boolean {
        return Prefs.default(context).getBoolean(Constants.GLYPH_VOLUME_LEVEL_ENABLE, false) && isGlyphEnabled()
    }

    @JvmStatic
    fun isGlyphShakeTorchEnabled(): Boolean {
        return Prefs.default(context).getBoolean(Constants.GLYPH_SHAKE_TORCH_ENABLE, false) && isGlyphEnabled()
    }

    @JvmStatic
    fun getGlyphShakeSensitivity(): Int {
        return Prefs.default(context).getInt(Constants.GLYPH_SHAKE_SENSITIVITY, 5).coerceIn(1, 10)
    }

    @JvmStatic
    fun isGlyphNotifsAppEnabled(app: String): Boolean {
        return Prefs.default(context).getBoolean(app, true) && isGlyphNotifsEnabled()
    }

    @JvmStatic
    fun isGlyphNotifsAppEssential(app: String): Boolean {
        val set = Prefs.default(context).getStringSet(Constants.GLYPH_NOTIFS_SUB_ESSENTIAL, emptySet()) ?: emptySet()
        return set.contains(app) && isGlyphNotifsEnabled()
    }

    @JvmStatic
    fun isGlyphComposerEnabled(): Boolean {
        return Settings.Secure.getInt(context.contentResolver, Constants.GLYPH_COMPOSER_ENABLE, 1) == 1
    }

    @JvmStatic
    fun setGlyphComposerEnabled(enabled: Boolean) {
        Settings.Secure.putInt(context.contentResolver, Constants.GLYPH_COMPOSER_ENABLE, if (enabled) 1 else 0)
    }

    @JvmStatic
    fun useComposerFallback(): Boolean {
        return Settings.Secure.getInt(context.contentResolver, Constants.GLYPH_COMPOSER_FALLBACK, 1) == 1
    }

    @JvmStatic
    fun setComposerFallback(enabled: Boolean) {
        Settings.Secure.putInt(context.contentResolver, Constants.GLYPH_COMPOSER_FALLBACK, if (enabled) 1 else 0)
    }
}