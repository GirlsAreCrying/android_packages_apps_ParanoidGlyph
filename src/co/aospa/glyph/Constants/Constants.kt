package co.aospa.glyph.Constants

import android.content.Context
import co.aospa.glyph.Utils.ResourceUtils

object Constants {

    private const val TAG = "GlyphConstants"
    private const val DEBUG = true

    @JvmField val APPS_TO_IGNORE = arrayOf(
        "android",
        "com.android.bluetooth",
        "com.android.deskclock",
        "com.android.dialer",
        "com.android.incallui",
        "com.android.phone",
        "com.android.providers.telephony",
        "com.android.server.telecom",
        "com.android.settings",
        "com.android.systemui",
        "com.google.android.dialer",
        "com.google.android.setupwizard",
        "com.google.android.talk",
        "com.google.android.tts",
        "com.google.android.youtube",
        "com.google.android.apps.messaging",
        "com.google.android.apps.nexuslauncher",
        "com.google.android.apps.pixelmigrate",
        "com.google.android.apps.wear.companion",
        "com.google.android.as",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.google.android.inputmethod.latin",
        "com.google.android.pixel.setupwizard",
        "com.google.android.projection.gearhead",
    )

    @JvmField val NOTIFS_TO_IGNORE = arrayOf(
        "android:android",
        "com.android.systemui:BAT",
        "com.android.systemui:charging",
        "com.android.systemui:foreground_service",
        "com.android.systemui:screenrecord",
        "com.android.systemui:usb",
    )

    lateinit var CONTEXT: Context

    @JvmField
    val MAX_PATTERN_BRIGHTNESS: Int = 4095

    @JvmField val GLYPH_ENABLE = "glyph_settings_enable"

    @JvmField val GLYPH_FLIP_ENABLE = "glyph_settings_flip_toggle"
    @JvmField val GLYPH_BRIGHTNESS = "glyph_settings_brightness"
    @JvmField val GLYPH_CHARGING_CATEGORY = "glyph_settings_charging"
    @JvmField val GLYPH_CHARGING_LEVEL_ENABLE = "glyph_settings_charging_level"
    @JvmField val GLYPH_CHARGING_POWERSHARE_ENABLE = "glyph_settings_charging_powershare"
    @JvmField val GLYPH_CALL_CATEGORY = "glyph_settings_call"
    @JvmField val GLYPH_CALL_ENABLE = "glyph_settings_call_toggle"
    @JvmField val GLYPH_CALL_SUB_PREVIEW = "glyph_settings_call_sub_preview"
    @JvmField val GLYPH_CALL_SUB_ANIMATIONS = "glyph_settings_call_sub_animations"
    @JvmField val GLYPH_CALL_SUB_ENABLE = "glyph_settings_call_sub_toggle"
    @JvmField val GLYPH_MUSIC_VISUALIZER_ENABLE = "glyph_settings_music_visualizer_toggle"
    @JvmField val GLYPH_VOLUME_LEVEL_ENABLE = "glyph_settings_volume_level_toggle"
    @JvmField val GLYPH_SHAKE_TORCH_ENABLE = "glyph_settings_shake_torch_toggle"
    @JvmField val GLYPH_SHAKE_SENSITIVITY = "glyph_settings_shake_sensitivity"
    @JvmField val GLYPH_AUTO_BRIGHTNESS_ENABLE = "glyph_settings_auto_brightness_toggle"
    @JvmField val GLYPH_CHARGING_POWERSHARE_MODE = "glyph_settings_charging_powershare_mode"

    @JvmField val GLYPH_COMPOSER_ENABLE = "glyph_settings_composer_enable"
    @JvmField val GLYPH_COMPOSER_FALLBACK = "glyph_settings_composer_fallback"

    @JvmField val GLYPH_NOTIFS_ENABLE = "glyph_settings_notifs_sub_toggle"
    @JvmField val GLYPH_NOTIFS_SUB_APPS = "glyph_settings_notifs_sub_apps"
    @JvmField val GLYPH_NOTIFS_SUB_ANIMATIONS = "glyph_settings_notifs_sub_animations"
    @JvmField val GLYPH_NOTIFS_SUB_ESSENTIAL = "glyph_settings_notifs_sub_essential"

    @JvmField val GLYPH_SCHEDULE_ENABLE = "glyph_settings_schedule_enable"
    @JvmField val GLYPH_SCHEDULE_DAYS = "glyph_settings_schedule_days"
    @JvmField val GLYPH_SCHEDULE_START = "glyph_settings_schedule_start"
    @JvmField val GLYPH_SCHEDULE_END = "glyph_settings_schedule_end"

    @JvmField val ACTION_SCHEDULE_START = "co.aospa.glyph.ACTION_SCHEDULE_START"
    @JvmField val ACTION_SCHEDULE_END = "co.aospa.glyph.ACTION_SCHEDULE_END"

    private var brightness = -1
    private var brightnessMax = -1
    private var device = -1

    fun isDeviceKnown(): Boolean = device >= 0

    fun getDevice(): Int {
        if (device < 0) device = ResourceUtils.getDevice(CONTEXT)
        return device
    }

    fun setDevice(v: Int) {
        device = v
    }

    fun getBrightness(): Int {
        if (brightness < 0) brightness = getMaxBrightness()
        return brightness
    }

    fun setBrightness(v: Int) {
        brightness = v
    }

    fun getMaxBrightness(): Int {
        if (brightnessMax < 0) {
            // config in resources, fallback to MAX_PATTERN_BRIGHTNESS
            brightnessMax = ResourceUtils.getMaxBrightness(CONTEXT, MAX_PATTERN_BRIGHTNESS)
        }
        return brightnessMax
    }

    fun setMaxBrightness(v: Int) {
        brightnessMax = v
    }
}