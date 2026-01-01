package co.aospa.glyph.Services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Utils.FileUtils

class ThirdPartyService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Keep compatibility with the previous AIDL helper.
        // This service is used by external callers to sync brightness to LEDs.
        FileUtils.writeAllLed(Constants.getBrightness())
        stopSelf()
        return START_NOT_STICKY
    }
}
