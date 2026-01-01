package co.aospa.glyph.Services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import co.aospa.glyph.Utils.Prefs
import com.android.internal.util.ArrayUtils
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.AnimationManager
import co.aospa.glyph.Manager.SettingsManager

class NotificationService : NotificationListenerService(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val TAG = "GlyphNotification"
        private const val DEBUG = true
    }

    private lateinit var ctx: Context
    private lateinit var notificationManager: NotificationManager

    private lateinit var contentResolverRef: ContentResolver
    private lateinit var settingObserver: SettingObserver

    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")

        ctx = this
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        contentResolverRef = contentResolver
        settingObserver = SettingObserver()
        settingObserver.register(contentResolverRef)

        sharedPrefs = Prefs.default(this)
        sharedPrefs.registerOnSharedPreferenceChangeListener(this)

        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        onNotificationUpdated()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        AnimationManager.stopEssential()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this)
        settingObserver.unregister(contentResolverRef)
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        runCatching { Constants.CONTEXT }.getOrNull() ?: return

        if (DEBUG) Log.d(TAG, "onNotificationPosted")
        if (!SettingsManager.isGlyphNotifsEnabled()) return

        val packageName = sbn.packageName
        val channelId = sbn.notification.channelId

        var packageImportance = -1
        var canBypassDnd = false
        val interruptionFilter = notificationManager.currentInterruptionFilter

        try {
            val packageContext = createPackageContext(packageName, 0)
            val pkgNm = packageContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel: NotificationChannel? = pkgNm.getNotificationChannel(channelId)
            if (channel != null) {
                packageImportance = channel.importance
                canBypassDnd = channel.canBypassDnd()
            }
        } catch (_: PackageManager.NameNotFoundException) {
        }

        val baseFiltersOk =
            SettingsManager.isGlyphNotifsAppEnabled(packageName) &&
                !sbn.isOngoing &&
                !ArrayUtils.contains(Constants.APPS_TO_IGNORE, packageName) &&
                !ArrayUtils.contains(Constants.NOTIFS_TO_IGNORE, "$packageName:$channelId") &&
                (packageImportance >= NotificationManager.IMPORTANCE_DEFAULT || packageImportance == -1) &&
                (interruptionFilter <= NotificationManager.INTERRUPTION_FILTER_ALL || canBypassDnd)

        if (baseFiltersOk) {
            AnimationManager.playCsv(ctx, SettingsManager.getGlyphNotifsAnimation())
        }

        val essentialOk =
            SettingsManager.isGlyphNotifsAppEssential(packageName) &&
                !sbn.isOngoing &&
                !ArrayUtils.contains(Constants.APPS_TO_IGNORE, packageName) &&
                !ArrayUtils.contains(Constants.NOTIFS_TO_IGNORE, "$packageName:$channelId") &&
                (packageImportance >= NotificationManager.IMPORTANCE_DEFAULT || packageImportance == -1) &&
                (interruptionFilter <= NotificationManager.INTERRUPTION_FILTER_ALL || canBypassDnd) &&
                notificationManager.isNotificationPolicyAccessGranted

        if (essentialOk) {
            AnimationManager.playEssential()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (DEBUG) Log.d(TAG, "onNotificationRemoved: package:${sbn.packageName} | channel id: ${sbn.notification.channelId}")
        onNotificationUpdated()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == Constants.GLYPH_NOTIFS_SUB_ESSENTIAL) {
            if (DEBUG) Log.d(TAG, "onSharedPreferenceChanged: glyph_settings_notifs_sub_essential")
            onNotificationUpdated()
        }
    }

    private fun onNotificationUpdated() {
        if (DEBUG) Log.d(TAG, "onNotificationUpdated")

        var playEssential = false
        if (SettingsManager.isGlyphNotifsEnabled()) {
            if (!notificationManager.isNotificationPolicyAccessGranted) return

            val active = activeNotifications ?: emptyArray()
            for (sbn in active) {
                val packageName = sbn.packageName
                val channelId = sbn.notification.channelId

                var packageImportance = -1
                var canBypassDnd = false
                val interruptionFilter = notificationManager.currentInterruptionFilter

                try {
                    val packageContext = createPackageContext(packageName, 0)
                    val pkgNm = packageContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val channel: NotificationChannel? = pkgNm.getNotificationChannel(channelId)
                    if (channel != null) {
                        packageImportance = channel.importance
                        canBypassDnd = channel.canBypassDnd()
                    }
                } catch (_: PackageManager.NameNotFoundException) {
                }

                val essentialOk =
                    SettingsManager.isGlyphNotifsAppEssential(packageName) &&
                        !sbn.isOngoing &&
                        !ArrayUtils.contains(Constants.APPS_TO_IGNORE, packageName) &&
                        !ArrayUtils.contains(Constants.NOTIFS_TO_IGNORE, "$packageName:$channelId") &&
                        (packageImportance >= NotificationManager.IMPORTANCE_DEFAULT || packageImportance == -1) &&
                        (interruptionFilter <= NotificationManager.INTERRUPTION_FILTER_ALL || canBypassDnd)

                if (essentialOk) {
                    if (DEBUG) Log.d(TAG, "onNotificationUpdated: found essential notification | package:$packageName")
                    playEssential = true
                }
            }
        }

        if (playEssential) AnimationManager.playEssential() else AnimationManager.stopEssential()
    }

    private inner class SettingObserver : ContentObserver(Handler()) {
        fun register(cr: ContentResolver) {
            cr.registerContentObserver(Settings.Secure.getUriFor(Constants.GLYPH_ENABLE), false, this)
            cr.registerContentObserver(Settings.Secure.getUriFor(Constants.GLYPH_NOTIFS_ENABLE), false, this)
        }

        fun unregister(cr: ContentResolver) {
            cr.unregisterContentObserver(this)
        }

        override fun onChange(selfChange: Boolean) {
            if (DEBUG) Log.d(TAG, "SettingObserver: onChange")
            onNotificationUpdated()
            super.onChange(selfChange)
        }
    }
}