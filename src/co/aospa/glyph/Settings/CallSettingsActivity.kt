package co.aospa.glyph.Settings

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Manager.AnimationManager
import co.aospa.glyph.Manager.SettingsManager
import co.aospa.glyph.R
import co.aospa.glyph.Settings.Composables.InlineGlyphAnimationPreview
import co.aospa.glyph.Utils.Prefs
import co.aospa.glyph.Utils.ResourceUtils
import co.aospa.glyph.Utils.ServiceUtils

class CallSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Constants.CONTEXT = applicationContext
        setContent { MaterialTheme { CallSettingsScreen(onBack = { finish() }) } }
    }
}

@Composable
private fun CallSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs.default(context) }

    var enabled by remember { mutableStateOf(SettingsManager.isGlyphCallEnabled()) }

    val callAnimations = remember { ResourceUtils.getCallAnimations().toList() }
    var selectedAnim by remember {
        mutableStateOf(
            prefs.getString(
                Constants.GLYPH_CALL_SUB_ANIMATIONS,
                ResourceUtils.getString("glyph_settings_call_animations_default")
            ) ?: ResourceUtils.getString("glyph_settings_call_animations_default")
        )
    }
    if (!callAnimations.contains(selectedAnim) && callAnimations.isNotEmpty()) {
        selectedAnim = ResourceUtils.getString("glyph_settings_call_animations_default")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.glyph_settings_call_toggle_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
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
                    title = stringResource(R.string.glyph_settings_call_sub_toggle_title),
                    subtitle = null,
                    checked = enabled,
                    enabled = SettingsManager.isGlyphEnabled(),
                    onCheckedChange = {
                        enabled = it
                        SettingsManager.setGlyphCallEnabled(it)
                        ServiceUtils.checkGlyphService()
                    }
                )
                Divider()
            }

            item {
                SettingDropdownRow(
                    title = stringResource(R.string.glyph_settings_call_sub_animations_title),
                    enabled = SettingsManager.isGlyphEnabled(),
                    currentValueLabel = selectedAnim,
                    options = callAnimations,
                    onOptionSelected = { v ->
                        selectedAnim = v
                        prefs.edit().putString(Constants.GLYPH_CALL_SUB_ANIMATIONS, v).apply()
                        // optional one-shot preview
                        if (enabled) AnimationManager.playCall(v)
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
                Button(
                    onClick = { if (enabled) AnimationManager.playCall(selectedAnim) },
                    enabled = SettingsManager.isGlyphEnabled(),
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                ) {
                    Text(stringResource(R.string.glyph_settings_preview))
                }
            }
        }
    }
}
