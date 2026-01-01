package co.aospa.glyph.Manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.util.Log
import co.aospa.glyph.Utils.Prefs
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.R
import co.aospa.glyph.Utils.ServiceUtils
import java.util.Calendar
import java.util.Date
import java.util.HashSet

object GlyphScheduleManager {
    private const val TAG = "GlyphScheduleManager"
    private const val DEBUG = true

    private const val PREF_SCHEDULE_ENABLED = "glyph_schedule_enabled"
    private const val PREF_SCHEDULE_START_HOUR = "glyph_schedule_start_hour"
    private const val PREF_SCHEDULE_START_MINUTE = "glyph_schedule_start_minute"
    private const val PREF_SCHEDULE_END_HOUR = "glyph_schedule_end_hour"
    private const val PREF_SCHEDULE_END_MINUTE = "glyph_schedule_end_minute"
    private const val PREF_SCHEDULE_ACTIVE = "glyph_schedule_currently_active"
    private const val PREF_SCHEDULE_DAYS = "glyph_schedule_days"

    private const val ACTION_SCHEDULE_START = "co.aospa.glyph.ACTION_SCHEDULE_START"
    private const val ACTION_SCHEDULE_END = "co.aospa.glyph.ACTION_SCHEDULE_END"

    private const val REQUEST_CODE_START = 1001
    private const val REQUEST_CODE_END = 1002

    const val SUNDAY = Calendar.SUNDAY
    const val MONDAY = Calendar.MONDAY
    const val TUESDAY = Calendar.TUESDAY
    const val WEDNESDAY = Calendar.WEDNESDAY
    const val THURSDAY = Calendar.THURSDAY
    const val FRIDAY = Calendar.FRIDAY
    const val SATURDAY = Calendar.SATURDAY

    @JvmStatic
    fun isScheduleEnabled(context: Context): Boolean {
        val prefs = Prefs.default(context)
        return prefs.getBoolean(PREF_SCHEDULE_ENABLED, false)
    }

    @JvmStatic
    fun setScheduleEnabled(context: Context, enabled: Boolean) {
        val prefs = Prefs.default(context)
        prefs.edit().putBoolean(PREF_SCHEDULE_ENABLED, enabled).apply()

        if (enabled) {
            setupScheduleAlarms(context)
            if (isWithinSchedulePeriod(context)) {
                applyScheduleStart(context)
            }
        } else {
            cancelScheduleAlarms(context)
            if (isScheduleCurrentlyActive(context)) {
                setScheduleActive(context, false)
                ServiceUtils.checkGlyphService()
                updateTorchTile(context)
            }
        }
    }

    @JvmStatic fun getScheduleStartHour(context: Context): Int =
        Prefs.default(context).getInt(PREF_SCHEDULE_START_HOUR, 22)

    @JvmStatic fun getScheduleStartMinute(context: Context): Int =
        Prefs.default(context).getInt(PREF_SCHEDULE_START_MINUTE, 0)

    @JvmStatic fun getScheduleEndHour(context: Context): Int =
        Prefs.default(context).getInt(PREF_SCHEDULE_END_HOUR, 7)

    @JvmStatic fun getScheduleEndMinute(context: Context): Int =
        Prefs.default(context).getInt(PREF_SCHEDULE_END_MINUTE, 0)

    @JvmStatic
    fun setScheduleStartTime(context: Context, hour: Int, minute: Int) {
        Prefs.default(context).edit()
            .putInt(PREF_SCHEDULE_START_HOUR, hour)
            .putInt(PREF_SCHEDULE_START_MINUTE, minute)
            .apply()

        if (isScheduleEnabled(context)) setupScheduleAlarms(context)
    }

    @JvmStatic
    fun setScheduleEndTime(context: Context, hour: Int, minute: Int) {
        Prefs.default(context).edit()
            .putInt(PREF_SCHEDULE_END_HOUR, hour)
            .putInt(PREF_SCHEDULE_END_MINUTE, minute)
            .apply()

        if (isScheduleEnabled(context)) setupScheduleAlarms(context)
    }

    @JvmStatic
    fun getScheduleDays(context: Context): Set<String> {
        val prefs = Prefs.default(context)
        val defaultDays = HashSet<String>().apply {
            add(MONDAY.toString())
            add(TUESDAY.toString())
            add(WEDNESDAY.toString())
            add(THURSDAY.toString())
            add(FRIDAY.toString())
            add(SATURDAY.toString())
            add(SUNDAY.toString())
        }
        return prefs.getStringSet(PREF_SCHEDULE_DAYS, defaultDays) ?: defaultDays
    }

    @JvmStatic
    fun setScheduleDays(context: Context, days: Set<String>) {
        Prefs.default(context).edit()
            .putStringSet(PREF_SCHEDULE_DAYS, days)
            .apply()

        if (isScheduleEnabled(context)) setupScheduleAlarms(context)
    }

    @JvmStatic
    fun isScheduleActiveToday(context: Context): Boolean {
        if (!isScheduleEnabled(context)) return false
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_WEEK)
        val enabledDays = getScheduleDays(context)
        return enabledDays.contains(currentDay.toString())
    }

    @JvmStatic
    fun isScheduleCurrentlyActive(context: Context): Boolean {
        if (!isScheduleEnabled(context)) return false
        if (!isScheduleActiveToday(context)) return false
        val prefs = Prefs.default(context)
        return prefs.getBoolean(PREF_SCHEDULE_ACTIVE, false)
    }

    @JvmStatic
    fun isWithinSchedulePeriod(context: Context): Boolean {
        if (!isScheduleActiveToday(context)) return false

        val now = Calendar.getInstance()
        val currentTotalMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val startTotalMinutes = getScheduleStartHour(context) * 60 + getScheduleStartMinute(context)
        val endTotalMinutes = getScheduleEndHour(context) * 60 + getScheduleEndMinute(context)

        return if (startTotalMinutes < endTotalMinutes) {
            currentTotalMinutes >= startTotalMinutes && currentTotalMinutes < endTotalMinutes
        } else {
            currentTotalMinutes >= startTotalMinutes || currentTotalMinutes < endTotalMinutes
        }
    }

    private fun setScheduleActive(context: Context, active: Boolean) {
        Prefs.default(context).edit()
            .putBoolean(PREF_SCHEDULE_ACTIVE, active)
            .apply()
    }

    @JvmStatic
    fun setupScheduleAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null")
            return
        }

        cancelScheduleAlarms(context)

        val startIntent = Intent(context, ScheduleReceiver::class.java).setAction(ACTION_SCHEDULE_START)
        val endIntent = Intent(context, ScheduleReceiver::class.java).setAction(ACTION_SCHEDULE_END)

        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_START,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_END,
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val startTriggerTime = calculateNextTriggerTime(getScheduleStartHour(context), getScheduleStartMinute(context))
        val endTriggerTime = calculateNextTriggerTime(getScheduleEndHour(context), getScheduleEndMinute(context))

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            startTriggerTime,
            AlarmManager.INTERVAL_DAY,
            startPendingIntent
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            endTriggerTime,
            AlarmManager.INTERVAL_DAY,
            endPendingIntent
        )

        if (isWithinSchedulePeriod(context)) applyScheduleStart(context) else applyScheduleEnd(context)

        if (DEBUG) {
            Log.d(TAG, "Start alarm set for: ${Date(startTriggerTime)}")
            Log.d(TAG, "End alarm set for: ${Date(endTriggerTime)}")
        }
    }

    @JvmStatic
    fun cancelScheduleAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val startIntent = Intent(context, ScheduleReceiver::class.java).setAction(ACTION_SCHEDULE_START)
        val endIntent = Intent(context, ScheduleReceiver::class.java).setAction(ACTION_SCHEDULE_END)

        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_START,
            startIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        val endPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_END,
            endIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        startPendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
        endPendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun calculateNextTriggerTime(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    @JvmStatic
    fun applyScheduleStart(context: Context) {
        setScheduleActive(context, true)
        ServiceUtils.checkGlyphService()
        updateTorchTile(context)
    }

    @JvmStatic
    fun applyScheduleEnd(context: Context) {
        setScheduleActive(context, false)
        ServiceUtils.checkGlyphService()
        updateTorchTile(context)
    }

    private fun updateTorchTile(context: Context) {
        try {
            context.sendBroadcast(Intent("co.aospa.glyph.UPDATE_TORCH_TILE"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update torch tile", e)
        }
    }

    @JvmStatic
    fun formatTime(context: Context, hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        val timeFormat = DateFormat.getTimeFormat(context)
        return timeFormat.format(calendar.time)
    }

    @JvmStatic
    fun formatTime24Hour(hour: Int, minute: Int): String =
        String.format("%02d:%02d", hour, minute)

    @JvmStatic
    fun getScheduleDaysFormatted(context: Context): String {
        val days = getScheduleDays(context)

        if (days.size == 7) return context.getString(R.string.glyph_settings_active_days_summary)
        if (days.isEmpty()) return context.getString(R.string.glyph_schedule_no_days_selected_summary)

        val weekdays = hashSetOf(
            MONDAY.toString(), TUESDAY.toString(), WEDNESDAY.toString(), THURSDAY.toString(), FRIDAY.toString()
        )
        if (days == weekdays) return context.getString(R.string.glyph_schedule_weekdays_summary)

        val weekends = hashSetOf(SATURDAY.toString(), SUNDAY.toString())
        if (days == weekends) return context.getString(R.string.glyph_schedule_weekends_summary)

        fun labelFor(day: Int): String = when (day) {
            SUNDAY -> context.getString(R.string.glyph_day_sun_short)
            MONDAY -> context.getString(R.string.glyph_day_mon_short)
            TUESDAY -> context.getString(R.string.glyph_day_tue_short)
            WEDNESDAY -> context.getString(R.string.glyph_day_wed_short)
            THURSDAY -> context.getString(R.string.glyph_day_thu_short)
            FRIDAY -> context.getString(R.string.glyph_day_fri_short)
            SATURDAY -> context.getString(R.string.glyph_day_sat_short)
            else -> day.toString()
        }

        val order = intArrayOf(SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY)
        return order
            .filter { days.contains(it.toString()) }
            .joinToString(separator = ", ") { labelFor(it) }
    }

    @JvmStatic
    fun getScheduleTimeRange(context: Context): String {
        val start = formatTime(context, getScheduleStartHour(context), getScheduleStartMinute(context))
        val end = formatTime(context, getScheduleEndHour(context), getScheduleEndMinute(context))
        return "$start - $end"
    }

    @JvmStatic
    fun getScheduleSummary(context: Context): String {
        if (!isScheduleEnabled(context)) return context.getString(R.string.glyph_settings_schedule_disabled)
        val d = getScheduleDaysFormatted(context)
        val t = getScheduleTimeRange(context)
        return "$d • $t"
    }

    class ScheduleReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val action = intent?.action ?: return

            Constants.CONTEXT = context.applicationContext
            if (!isScheduleEnabled(context)) return
            if (!isScheduleActiveToday(context)) return

            when (action) {
                ACTION_SCHEDULE_START -> applyScheduleStart(context)
                ACTION_SCHEDULE_END -> applyScheduleEnd(context)
            }
        }
    }
}
