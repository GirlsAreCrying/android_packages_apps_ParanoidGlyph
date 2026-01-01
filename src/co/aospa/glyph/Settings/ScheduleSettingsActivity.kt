package co.aospa.glyph.Settings

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.aospa.glyph.Manager.GlyphScheduleManager
import co.aospa.glyph.R
import java.util.Calendar

class ScheduleSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { ScheduleSettingsScreen(onBack = { finish() }) } }
    }
}

@Composable
private fun ScheduleSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var enabled by remember { mutableStateOf(GlyphScheduleManager.isScheduleEnabled(context)) }
    var days by remember { mutableStateOf(GlyphScheduleManager.getScheduleDays(context).toMutableSet()) }
    var startH by remember { mutableIntStateOf(GlyphScheduleManager.getScheduleStartHour(context)) }
    var startM by remember { mutableIntStateOf(GlyphScheduleManager.getScheduleStartMinute(context)) }
    var endH by remember { mutableIntStateOf(GlyphScheduleManager.getScheduleEndHour(context)) }
    var endM by remember { mutableIntStateOf(GlyphScheduleManager.getScheduleEndMinute(context)) }

    fun refreshStatus(): String {
        val activeToday = GlyphScheduleManager.isScheduleActiveToday(context)
        val activeNow = GlyphScheduleManager.isScheduleCurrentlyActive(context)
        return when {
            !enabled -> context.getString(R.string.glyph_settings_schedule_disabled)
            !activeToday -> context.getString(R.string.glyph_settings_schedule_inactive) // today not in days -> effectively inactive
            activeNow -> context.getString(R.string.glyph_settings_schedule_active)
            else -> context.getString(R.string.glyph_settings_schedule_inactive)
        }
    }

    var status by remember { mutableStateOf(refreshStatus()) }

    fun updateAll() {
        startH = GlyphScheduleManager.getScheduleStartHour(context)
        startM = GlyphScheduleManager.getScheduleStartMinute(context)
        endH = GlyphScheduleManager.getScheduleEndHour(context)
        endM = GlyphScheduleManager.getScheduleEndMinute(context)
        days = GlyphScheduleManager.getScheduleDays(context).toMutableSet()
        enabled = GlyphScheduleManager.isScheduleEnabled(context)
        status = refreshStatus()
    }

    LaunchedEffect(Unit) { updateAll() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.glyph_settings_schedule_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth()) {

            SettingSwitchRow(
                title = stringResource(R.string.glyph_settings_schedule_enable_title),
                subtitle = stringResource(R.string.glyph_settings_schedule_enable_summary),
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    GlyphScheduleManager.setScheduleEnabled(context, it)
                    updateAll()
                }
            )
            Divider()

            SettingNavRow(
                title = stringResource(R.string.glyph_settings_active_days_title),
                subtitle = GlyphScheduleManager.getScheduleDaysFormatted(context),
                enabled = enabled,
                onClick = { /* picker is inline below */ }
            )

            DaysPicker(
                enabled = enabled,
                days = days,
                onDaysChanged = { newSet ->
                    days = newSet.toMutableSet()
                    GlyphScheduleManager.setScheduleDays(context, newSet)
                    updateAll()
                }
            )

            Divider()

            TimeRow(
                title = stringResource(R.string.glyph_settings_schedule_start_time_title),
                enabled = enabled,
                timeLabel = GlyphScheduleManager.formatTime(context, startH, startM),
                onClick = {
                    val is24 = DateFormat.is24HourFormat(context)
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            GlyphScheduleManager.setScheduleStartTime(context, h, m)
                            updateAll()
                        },
                        startH,
                        startM,
                        is24
                    ).show()
                }
            )

            TimeRow(
                title = stringResource(R.string.glyph_settings_schedule_end_time_title),
                enabled = enabled,
                timeLabel = GlyphScheduleManager.formatTime(context, endH, endM),
                onClick = {
                    val is24 = DateFormat.is24HourFormat(context)
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            GlyphScheduleManager.setScheduleEndTime(context, h, m)
                            updateAll()
                        },
                        endH,
                        endM,
                        is24
                    ).show()
                }
            )

            Divider()

            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.glyph_settings_schedule_status_title), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(6.dp))
                Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.glyph_settings_schedule_info_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DaysPicker(
    enabled: Boolean,
    days: Set<String>,
    onDaysChanged: (Set<String>) -> Unit
) {
    val items = listOf(
        Calendar.MONDAY.toString() to R.string.glyph_day_mon_short,
        Calendar.TUESDAY.toString() to R.string.glyph_day_tue_short,
        Calendar.WEDNESDAY.toString() to R.string.glyph_day_wed_short,
        Calendar.THURSDAY.toString() to R.string.glyph_day_thu_short,
        Calendar.FRIDAY.toString() to R.string.glyph_day_fri_short,
        Calendar.SATURDAY.toString() to R.string.glyph_day_sat_short,
        Calendar.SUNDAY.toString() to R.string.glyph_day_sun_short
    )

    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        items.forEach { (key, labelRes) ->
            val checked = days.contains(key)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) {
                        val tmp = days.toMutableSet()
                        if (checked) tmp.remove(key) else tmp.add(key)
                        onDaysChanged(tmp)
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    enabled = enabled,
                    onCheckedChange = {
                        val tmp = days.toMutableSet()
                        if (it) tmp.add(key) else tmp.remove(key)
                        onDaysChanged(tmp)
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(labelRes))
            }
        }
    }
}

@Composable
private fun TimeRow(
    title: String,
    enabled: Boolean,
    timeLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(timeLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
