package co.aospa.glyph.Manager

import co.aospa.glyph.Utils.ResourceUtils

object StatusManager {
    private const val TAG = "GlyphStatusManager"
    private const val DEBUG = true

    @Volatile private var allLedActive = false
    @Volatile private var animationActive = false
    @Volatile private var chargingAnimationActive = false
    @Volatile private var volumeAnimationActive = false
    @Volatile private var callLedActive = false
    @Volatile private var essentialLedActive = false

    @Volatile private var chargingLedLast = 0
    @Volatile private var batteryArray: IntArray =
        IntArray(ResourceUtils.getInteger("glyph_settings_battery_levels_num"))

    @Volatile private var volumeLedLast = 0
    @Volatile private var volumeArray: IntArray =
        IntArray(ResourceUtils.getInteger("glyph_settings_volume_levels_num"))

    @Volatile private var callLedEnabled = false

    @JvmStatic fun isAnimationActive() = animationActive
    @JvmStatic fun setAnimationActive(status: Boolean) { animationActive = status }

    @JvmStatic fun isChargingAnimationActive() = chargingAnimationActive
    @JvmStatic fun setChargingAnimationActive(status: Boolean) { chargingAnimationActive = status }

    @JvmStatic fun isVolumeAnimationActive() = volumeAnimationActive
    @JvmStatic fun setVolumeAnimationActive(status: Boolean) { volumeAnimationActive = status }

    @JvmStatic fun isAllLedActive() = allLedActive
    @JvmStatic fun setAllLedsActive(status: Boolean) { allLedActive = status }

    @JvmStatic fun isCallLedActive() = callLedActive
    @JvmStatic fun setCallLedActive(status: Boolean) { callLedActive = status }

    @JvmStatic fun isEssentialLedActive() = essentialLedActive
    @JvmStatic fun setEssentialLedActive(status: Boolean) { essentialLedActive = status }

    @JvmStatic fun getChargingLedLast() = chargingLedLast
    @JvmStatic fun setChargingLedLast(last: Int) { chargingLedLast = last }

    @JvmStatic fun getBatteryArray() = batteryArray
    @JvmStatic fun setBatteryArray(next: IntArray) { batteryArray = next }

    @JvmStatic fun getVolumeLedLast() = volumeLedLast
    @JvmStatic fun setVolumeLedLast(last: Int) { volumeLedLast = last }

    @JvmStatic fun getVolumeArray() = volumeArray
    @JvmStatic fun setVolumeArray(next: IntArray) { volumeArray = next }

    @JvmStatic fun isCallLedEnabled() = callLedEnabled
    @JvmStatic fun setCallLedEnabled(status: Boolean) { callLedEnabled = status }

    @JvmStatic
    fun isGlyphIdle(): Boolean {
        return !(isAllLedActive() ||
            isCallLedActive() ||
            isAnimationActive() ||
            isChargingAnimationActive() ||
            isVolumeAnimationActive() ||
            isCallLedEnabled())
    }
}
