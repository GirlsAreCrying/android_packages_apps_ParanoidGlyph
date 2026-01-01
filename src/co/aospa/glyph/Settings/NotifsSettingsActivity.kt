package co.aospa.glyph.Settings

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.AnimationManager
import co.aospa.glyph.Manager.SettingsManager
import co.aospa.glyph.R
import co.aospa.glyph.Settings.Composables.InlineGlyphAnimationPreview
import co.aospa.glyph.Utils.Prefs
import co.aospa.glyph.Utils.ResourceUtils
import co.aospa.glyph.Utils.ServiceUtils
import com.android.internal.util.ArrayUtils

class NotifsSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Constants.CONTEXT = applicationContext
        setContent { MaterialTheme { NotifsSettingsScreen(onBack = { finish() }) } }
    }
}

private data class AppEntry(
    val pkg: String,
    val label: String,
    val icon: Drawable?
)

@Composable
private fun NotifsSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val pm = remember { context.packageManager }
    val prefs = remember { Prefs.default(context) }

    var enabled by remember { mutableStateOf(SettingsManager.isGlyphNotifsEnabled()) }

    val notifAnimations = remember { ResourceUtils.getNotificationAnimations().toList() }
    var selectedAnim by remember {
        mutableStateOf(
            prefs.getString(
                Constants.GLYPH_NOTIFS_SUB_ANIMATIONS,
                ResourceUtils.getString("glyph_settings_notifs_animations_default")
            ) ?: ResourceUtils.getString("glyph_settings_notifs_animations_default")
        )
    }
    if (!notifAnimations.contains(selectedAnim) && notifAnimations.isNotEmpty()) {
        selectedAnim = ResourceUtils.getString("glyph_settings_notifs_animations_default")
    }

    val apps by remember { mutableStateOf(loadApps(pm)) }

    var essentialSet by remember {
        mutableStateOf(
            (prefs.getStringSet(Constants.GLYPH_NOTIFS_SUB_ESSENTIAL, emptySet<String>())
                ?: emptySet()).toSet()
        )
    }
    var essentialDialog by remember { mutableStateOf(false) }

    fun refreshServices() = ServiceUtils.checkGlyphService()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.glyph_settings_notifs_toggle_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                SettingSwitchRow(
                    title = stringResource(R.string.glyph_settings_notifs_sub_toggle_title),
                    subtitle = null,
                    checked = enabled,
                    enabled = SettingsManager.isGlyphEnabled(),
                    onCheckedChange = {
                        enabled = it
                        SettingsManager.setGlyphNotifsEnabled(it)
                        refreshServices()
                    }
                )
                Divider()
            }

            item {
                SettingDropdownRow(
                    title = stringResource(R.string.glyph_settings_notifs_sub_animations_title),
                    enabled = SettingsManager.isGlyphEnabled(),
                    currentValueLabel = selectedAnim,
                    options = notifAnimations,
                    onOptionSelected = { v ->
                        selectedAnim = v
                        prefs.edit().putString(Constants.GLYPH_NOTIFS_SUB_ANIMATIONS, v).apply()
                        if (enabled) AnimationManager.playCsv(context, v)
                    }
                )

                InlineGlyphAnimationPreview(
                    animationName = selectedAnim,
                    playing = enabled && SettingsManager.isGlyphEnabled(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp),
                    timeBetweenLoopsMs = 0
                )

                Divider()
            }

            item {
                SettingNavRow(
                    title = stringResource(R.string.glyph_settings_notifs_sub_essential_title),
                    subtitle = essentialSet.size.toString(),
                    enabled = true,
                    onClick = { essentialDialog = true }
                )
                Divider()
            }

            item {
                SectionHeader(title = stringResource(R.string.glyph_settings_notifs_sub_title))
            }

            items(apps, key = { it.pkg }) { app ->
                var appEnabled by remember(app.pkg, essentialSet) {
                    mutableStateOf(prefs.getBoolean(app.pkg, true))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AndroidView(
                        factory = { android.widget.ImageView(it).apply { setImageDrawable(app.icon) } },
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.bodyLarge)
                        if (essentialSet.contains(app.pkg)) {
                            Text(
                                stringResource(R.string.glyph_settings_notifs_essential_badge),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Switch(
                        checked = appEnabled,
                        onCheckedChange = { v ->
                            appEnabled = v
                            prefs.edit().putBoolean(app.pkg, v).apply()
                            refreshServices()
                        }
                    )
                }
                Divider()
            }

            item {
                Button(
                    onClick = { if (enabled) AnimationManager.playCsv(context, selectedAnim) },
                    enabled = SettingsManager.isGlyphEnabled(),
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                ) {
                    Text(stringResource(R.string.glyph_settings_preview))
                }
            }
        }

        if (essentialDialog) {
            EssentialAppsDialog(
                apps = apps,
                selected = essentialSet,
                onDismiss = { essentialDialog = false },
                onApply = { newSet ->
                    essentialSet = newSet.toSet()
                    prefs.edit().putStringSet(Constants.GLYPH_NOTIFS_SUB_ESSENTIAL, essentialSet).apply()
                    essentialDialog = false
                    refreshServices()
                }
            )
        }
    }
}

@Composable
private fun EssentialAppsDialog(
    apps: List<AppEntry>,
    selected: Set<String>,
    onDismiss: () -> Unit,
    onApply: (Set<String>) -> Unit
) {
    var tmp by remember { mutableStateOf(selected.toMutableSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onApply(tmp) }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
        title = { Text(stringResource(R.string.glyph_settings_notifs_essential_apps_title)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                apps.forEach { app ->
                    val checked = tmp.contains(app.pkg)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (checked) tmp.remove(app.pkg) else tmp.add(app.pkg)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                if (it) tmp.add(app.pkg) else tmp.remove(app.pkg)
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(app.label)
                    }
                }
            }
        }
    )
}

private fun loadApps(pm: PackageManager): List<AppEntry> {
    val apps = pm.getInstalledApplications(PackageManager.GET_GIDS)
        .sortedWith(ApplicationInfo.DisplayNameComparator(pm))

    val list = ArrayList<AppEntry>()
    for (app in apps) {
        val pkg = app.packageName

        if (pm.getLaunchIntentForPackage(pkg) == null) continue
        if (ArrayUtils.contains(Constants.APPS_TO_IGNORE, pkg)) continue

        val label = pm.getApplicationLabel(app).toString()
        val icon = runCatching { pm.getApplicationIcon(app) }.getOrNull()
        list.add(AppEntry(pkg, label, icon))
    }
    return list
}
