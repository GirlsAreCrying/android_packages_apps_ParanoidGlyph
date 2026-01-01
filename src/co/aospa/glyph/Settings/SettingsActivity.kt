package co.aospa.glyph.Settings

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.GlyphScheduleManager
import co.aospa.glyph.Manager.SettingsManager
import co.aospa.glyph.Manager.ShakeManager
import co.aospa.glyph.R
import co.aospa.glyph.Utils.Prefs
import co.aospa.glyph.Utils.ResourceUtils
import co.aospa.glyph.Utils.ServiceUtils

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Constants.CONTEXT = applicationContext

        setContent {
            MaterialTheme {
                MainSettingsScreen(
                    onOpenNotifs = { startActivity(Intent(this, NotifsSettingsActivity::class.java)) },
                    onOpenCall = { startActivity(Intent(this, CallSettingsActivity::class.java)) },
                    onOpenSchedule = { startActivity(Intent(this, ScheduleSettingsActivity::class.java)) },
                    onOpenComposerApply = { startActivity(Intent(this, GlyphPatternSelectorActivity::class.java)) },
                    onOpenComposerPreview = { startActivity(Intent(this, GlyphPatternPreviewActivity::class.java)) },
                    onOpenComposerCreate = { startActivity(Intent(this, GlyphPatternCreatorActivity::class.java)) },
                )
            }
        }
    }
}

@Composable
private fun MainSettingsScreen(
    onOpenNotifs: () -> Unit,
    onOpenCall: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenComposerApply: () -> Unit,
    onOpenComposerPreview: () -> Unit,
    onOpenComposerCreate: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { Prefs.default(context) }

    var glyphEnabled by remember { mutableStateOf(SettingsManager.isGlyphEnabledIgnoreSchedule()) }

    var flipEnabled by remember { mutableStateOf(prefs.getBoolean(Constants.GLYPH_FLIP_ENABLE, false)) }
    var autoBrightnessEnabled by remember { mutableStateOf(prefs.getBoolean(Constants.GLYPH_AUTO_BRIGHTNESS_ENABLE, false)) }
    var chargingLevelEnabled by remember { mutableStateOf(prefs.getBoolean(Constants.GLYPH_CHARGING_LEVEL_ENABLE, false)) }
    var powershareEnabled by remember { mutableStateOf(prefs.getBoolean(Constants.GLYPH_CHARGING_POWERSHARE_ENABLE, false)) }
    var volumeLevelEnabled by remember { mutableStateOf(prefs.getBoolean(Constants.GLYPH_VOLUME_LEVEL_ENABLE, false)) }
    var shakeTorchEnabled by remember { mutableStateOf(prefs.getBoolean(Constants.GLYPH_SHAKE_TORCH_ENABLE, false)) }
    var musicVisualizerEnabled by remember { mutableStateOf(prefs.getBoolean(Constants.GLYPH_MUSIC_VISUALIZER_ENABLE, false)) }

    var flipRingerMode by remember { mutableIntStateOf(SettingsManager.getFlipRingerMode()) }

    var composerEnabled by remember { mutableStateOf(SettingsManager.isGlyphComposerEnabled()) }
    var composerFallback by remember { mutableStateOf(SettingsManager.useComposerFallback()) }

    // Brightness is stored as int; slider works with float state
    var brightnessSetting by remember { mutableIntStateOf(SettingsManager.getGlyphBrightnessSetting()) }
    var brightnessSlider by remember { mutableFloatStateOf(brightnessSetting.toFloat()) }

    // Shake sensitivity
    var shakeSensitivity by remember { mutableIntStateOf(prefs.getInt(Constants.GLYPH_SHAKE_SENSITIVITY, 35)) }
    var shakeSlider by remember { mutableFloatStateOf(shakeSensitivity.toFloat()) }

    // Keep local slider in sync if value changed elsewhere
    LaunchedEffect(brightnessSetting) { brightnessSlider = brightnessSetting.toFloat() }
    LaunchedEffect(shakeSensitivity) { shakeSlider = shakeSensitivity.toFloat() }

    val brightnessMax = remember { Constants.getBrightnessLevels().size }
    val hasLightSensor = remember { ResourceUtils.getString("glyph_light_sensor").isNotBlank() }

    fun commitAndRefreshServices() {
        ServiceUtils.checkGlyphService()
    }

    fun updateTorchTile() {
        runCatching {
            context.sendBroadcast(Intent("co.aospa.glyph.UPDATE_TORCH_TILE"))
        }
    }

    var scheduleSummary by remember { mutableStateOf(GlyphScheduleManager.getScheduleSummary(context)) }
    LaunchedEffect(Unit) {
        commitAndRefreshServices()
        scheduleSummary = GlyphScheduleManager.getScheduleSummary(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.glyph_settings_title)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {
            SectionHeader(title = stringResource(R.string.glyph_settings_title))

            // Main switch
            SettingSwitchRow(
                title = stringResource(R.string.glyph_settings_toggle_title),
                subtitle = if (glyphEnabled) stringResource(R.string.glyph_accessibility_quick_settings_on)
                else stringResource(R.string.glyph_accessibility_quick_settings_off),
                checked = glyphEnabled,
                onCheckedChange = { enabled ->
                    glyphEnabled = enabled
                    SettingsManager.enableGlyph(enabled)

                    if (!enabled) {
                        ShakeManager.stopShakeService(context)
                    } else if (shakeTorchEnabled) {
                        ShakeManager.startShakeService(context)
                    }

                    commitAndRefreshServices()
                    updateTorchTile()
                }
            )

            Divider()

            // Flip
            SettingSwitchRow(
                title = stringResource(R.string.glyph_settings_flip_toggle_title),
                subtitle = null,
                enabled = glyphEnabled,
                checked = flipEnabled,
                onCheckedChange = { v ->
                    flipEnabled = v
                    prefs.edit().putBoolean(Constants.GLYPH_FLIP_ENABLE, v).apply()
                    commitAndRefreshServices()
                }
            )

            // Flip ringer mode
            val ringerEntries = listOf(
                context.getString(R.string.glyph_settings_flip_ringer_mode_follow_system) to -1,
                context.getString(R.string.glyph_settings_flip_ringer_mode_silent) to 0,
                context.getString(R.string.glyph_settings_flip_ringer_mode_vibrate) to 1,
                context.getString(R.string.glyph_settings_flip_ringer_mode_ring) to 2,
            )
            SettingDropdownRow(
                title = stringResource(R.string.glyph_settings_flip_ringer_mode_title),
                enabled = glyphEnabled && flipEnabled,
                currentValueLabel = ringerEntries.firstOrNull { it.second == flipRingerMode }?.first
                    ?: ringerEntries[0].first,
                options = ringerEntries.map { it.first },
                onOptionSelected = { label ->
                    val mode = ringerEntries.first { it.first == label }.second
                    flipRingerMode = mode
                    android.provider.Settings.Secure.putInt(
                        context.contentResolver,
                        Constants.GLYPH_FLIP_RINGER_MODE,
                        mode
                    )
                    commitAndRefreshServices()
                }
            )

            Divider()

            // Auto brightness
            if (hasLightSensor) {
                SettingSwitchRow(
                    title = stringResource(R.string.glyph_settings_auto_brightness_toggle_title),
                    subtitle = null,
                    enabled = glyphEnabled,
                    checked = autoBrightnessEnabled,
                    onCheckedChange = { v ->
                        autoBrightnessEnabled = v
                        prefs.edit().putBoolean(Constants.GLYPH_AUTO_BRIGHTNESS_ENABLE, v).apply()
                        commitAndRefreshServices()
                    }
                )
            }

            // Brightness slider (fixed: no valueLabel param, proper callbacks)
            SettingSliderRow(
                title = stringResource(R.string.glyph_settings_brightness_title),
                subtitle = stringResource(R.string.glyph_settings_brightness_summary),
                enabled = glyphEnabled && (!hasLightSensor || !autoBrightnessEnabled),
                value = brightnessSlider,
                valueRange = 1f..brightnessMax.toFloat(),
                steps = (brightnessMax - 2).coerceAtLeast(0),
                onValueChange = { newValue ->
                    brightnessSlider = newValue
                },
                onValueChangeFinished = {
                    val v = brightnessSlider.toInt().coerceIn(1, brightnessMax)
                    brightnessSetting = v
                    prefs.edit().putInt(Constants.GLYPH_BRIGHTNESS, v).apply()
                    commitAndRefreshServices()
                }
            )

            Divider()

            // Notifications
            SettingNavRow(
                title = stringResource(R.string.glyph_settings_notifs_toggle_title),
                subtitle = if (SettingsManager.isGlyphNotifsEnabled()) stringResource(R.string.glyph_accessibility_quick_settings_on)
                else stringResource(R.string.glyph_accessibility_quick_settings_off),
                enabled = glyphEnabled,
                onClick = {
                    if (!ServiceUtils.isNotificationServiceEnabled()) {
                        AlertDialog.Builder(context)
                            .setTitle(R.string.glyph_settings_notifs_permission_dialog_title)
                            .setMessage(R.string.glyph_settings_notifs_permission_dialog_message)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    } else {
                        onOpenNotifs()
                    }
                }
            )

            // Calls
            SettingNavRow(
                title = stringResource(R.string.glyph_settings_call_toggle_title),
                subtitle = if (SettingsManager.isGlyphCallEnabled()) stringResource(R.string.glyph_accessibility_quick_settings_on)
                else stringResource(R.string.glyph_accessibility_quick_settings_off),
                enabled = glyphEnabled,
                onClick = onOpenCall
            )

            Divider()

            // Charging
            SettingSwitchRow(
                title = stringResource(R.string.glyph_settings_charging_level_title),
                subtitle = null,
                enabled = glyphEnabled,
                checked = chargingLevelEnabled,
                onCheckedChange = { v ->
                    chargingLevelEnabled = v
                    prefs.edit().putBoolean(Constants.GLYPH_CHARGING_LEVEL_ENABLE, v).apply()
                    commitAndRefreshServices()
                }
            )

            SettingSwitchRow(
                title = stringResource(R.string.glyph_settings_charging_powershare_title),
                subtitle = null,
                enabled = glyphEnabled,
                checked = powershareEnabled,
                onCheckedChange = { v ->
                    powershareEnabled = v
                    prefs.edit().putBoolean(Constants.GLYPH_CHARGING_POWERSHARE_ENABLE, v).apply()
                    commitAndRefreshServices()
                }
            )

            Divider()

            // Volume
            SettingSwitchRow(
                title = stringResource(R.string.glyph_settings_volume_level_toggle_title),
                subtitle = null,
                enabled = glyphEnabled,
                checked = volumeLevelEnabled,
                onCheckedChange = { v ->
                    volumeLevelEnabled = v
                    prefs.edit().putBoolean(Constants.GLYPH_VOLUME_LEVEL_ENABLE, v).apply()
                    commitAndRefreshServices()
                }
            )

            // Shake torch
            SettingSwitchRow(
                title = stringResource(R.string.glyph_settings_shake_torch_toggle_title),
                subtitle = null,
                enabled = glyphEnabled,
                checked = shakeTorchEnabled,
                onCheckedChange = { v ->
                    shakeTorchEnabled = v
                    prefs.edit().putBoolean(Constants.GLYPH_SHAKE_TORCH_ENABLE, v).apply()

                    if (glyphEnabled) {
                        if (v) ShakeManager.startShakeService(context) else ShakeManager.stopShakeService(context)
                    }
                    commitAndRefreshServices()
                }
            )

            SettingSliderRow(
                title = stringResource(R.string.glyph_settings_shake_sensitivity_title),
                subtitle = null,
                enabled = glyphEnabled && shakeTorchEnabled,
                value = shakeSlider,
                valueRange = 5f..60f,
                steps = 54,
                onValueChange = { newValue ->
                    shakeSlider = newValue
                },
                onValueChangeFinished = {
                    val v = shakeSlider.toInt().coerceIn(5, 60)
                    shakeSensitivity = v
                    prefs.edit().putInt(Constants.GLYPH_SHAKE_SENSITIVITY, v).apply()
                    if (glyphEnabled && shakeTorchEnabled) ShakeManager.restartShakeService(context)
                    commitAndRefreshServices()
                }
            )

            Divider()

            // Music visualizer
            SettingSwitchRow(
                title = stringResource(R.string.glyph_settings_music_visualizer_toggle_title),
                subtitle = null,
                enabled = glyphEnabled,
                checked = musicVisualizerEnabled,
                onCheckedChange = { v ->
                    musicVisualizerEnabled = v
                    prefs.edit().putBoolean(Constants.GLYPH_MUSIC_VISUALIZER_ENABLE, v).apply()
                    commitAndRefreshServices()
                }
            )

            Divider()

            // Schedule
            scheduleSummary = GlyphScheduleManager.getScheduleSummary(context)
            SettingNavRow(
                title = stringResource(R.string.glyph_settings_schedule_title),
                subtitle = scheduleSummary,
                enabled = true,
                onClick = { onOpenSchedule() }
            )

            Divider()

            // Composer
            SettingSwitchRow(
                title = stringResource(R.string.glyph_settings_composer_enable_title),
                subtitle = stringResource(R.string.glyph_settings_composer_enable_summary),
                enabled = glyphEnabled,
                checked = composerEnabled,
                onCheckedChange = { v ->
                    composerEnabled = v
                    SettingsManager.setGlyphComposerEnabled(v)
                    commitAndRefreshServices()
                }
            )

            SettingSwitchRow(
                title = stringResource(R.string.glyph_settings_composer_fallback_title),
                subtitle = stringResource(R.string.glyph_settings_composer_fallback_summary),
                enabled = glyphEnabled && composerEnabled,
                checked = composerFallback,
                onCheckedChange = { v ->
                    composerFallback = v
                    android.provider.Settings.Secure.putInt(
                        context.contentResolver,
                        Constants.GLYPH_COMPOSER_FALLBACK,
                        if (v) 1 else 0
                    )
                    commitAndRefreshServices()
                }
            )

            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Button(
                    onClick = onOpenComposerApply,
                    enabled = true,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.glyph_settings_composer_apply_title)) }

                Spacer(Modifier.width(12.dp))

                Button(
                    onClick = onOpenComposerPreview,
                    enabled = true,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.glyph_settings_composer_preview_title)) }
            }

            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Button(
                    onClick = onOpenComposerCreate,
                    enabled = true,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.glyph_settings_composer_create_title)) }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}