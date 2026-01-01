package co.aospa.glyph

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.GlyphScheduleManager
import co.aospa.glyph.Manager.ShakeManager
import co.aospa.glyph.Utils.ServiceUtils

class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val DEBUG = true
        private const val TAG = "ParanoidGlyph"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (DEBUG) Log.d(TAG, "Received boot completed intent")

        Constants.CONTEXT = context.applicationContext

        if (GlyphScheduleManager.isScheduleEnabled(context)) {
            GlyphScheduleManager.setupScheduleAlarms(context)
            if (DEBUG) Log.d(TAG, "Schedule alarms restored on boot")
        }

        ServiceUtils.checkGlyphService()

        if (ShakeManager.isShakeEnabled(context)) {
            ShakeManager.startShakeService(context)
            if (DEBUG) Log.d(TAG, "Shake service started on boot")
        }
    }
}
