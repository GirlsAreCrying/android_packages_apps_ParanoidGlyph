package co.aospa.glyph.Settings

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import co.aospa.glyph.Composer.GlyphComposerParser
import co.aospa.glyph.Composer.GlyphPattern
import co.aospa.glyph.Manager.AnimationManager
import co.aospa.glyph.Manager.SettingsManager
import android.media.RingtoneManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

class GlyphPatternPreviewActivity : ComponentActivity() {

    companion object { private const val TAG = "GlyphPatternPreview" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { PatternPreviewScreen(onBack = { finish() }) } }
    }

    override fun onPause() {
        super.onPause()
        AnimationManager.stopAll()
    }

    override fun onDestroy() {
        AnimationManager.stopAll()
        super.onDestroy()
    }
}

@Composable
private fun PatternPreviewScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var ringtoneUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("Analyzing current ringtone...") }

    var hasPattern by remember { mutableStateOf(false) }
    var pattern by remember { mutableStateOf<GlyphPattern?>(null) }

    var isRunning by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }

    fun getRealPathFromUri(uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        if (uri.scheme == "content") {
            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val col = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    return c.getString(col)
                }
            }
        }
        return null
    }

    fun scaleBrightness(patternBrightness: Int): Int {
        val userBrightness = SettingsManager.getGlyphBrightness()
        return (patternBrightness * userBrightness) / 100
    }

    fun analyze() {
        ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
        val uri = ringtoneUri
        if (uri == null) {
            status = "No ringtone set\n\nOnly fallback preview available"
            hasPattern = false
            pattern = null
            return
        }

        val audioPath = getRealPathFromUri(uri)
        if (audioPath == null) {
            status = "Current Ringtone: ${uri.lastPathSegment}\n\n⚠  Cannot access file path\n\nOnly fallback preview available"
            hasPattern = false
            pattern = null
            return
        }

        val sb = StringBuilder()
        sb.append("Current Ringtone:\n")
        sb.append(audioPath.substringAfterLast('/'))
        sb.append("\n\n")

        val patternPath = GlyphComposerParser.getGlyphPatternPath(audioPath)
        if (patternPath != null) {
            val p = GlyphComposerParser.parseFromFile(patternPath)
            if (p != null && GlyphComposerParser.isValid(p)) {
                hasPattern = true
                pattern = p
                sb.append("✓ Composer Pattern Found\n")
                sb.append("  Frames: ${p.frames?.size ?: 0}\n")
                sb.append("  Duration: ${(p.duration / 1000)}s\n\n")
                sb.append("Both preview options available:")
            } else {
                hasPattern = false
                pattern = null
                sb.append("✗ Pattern file invalid\n\nOnly fallback preview available")
            }
        } else {
            hasPattern = false
            pattern = null
            sb.append("✗ No Composer Pattern\n\nOnly fallback preview available")
        }

        status = sb.toString()
    }

    fun stop() {
        isRunning = false
        job?.cancel()
        job = null
        AnimationManager.stopAll()
        Toast.makeText(context, "Preview stopped", Toast.LENGTH_SHORT).show()
    }

    fun previewComposer() {
        val p = pattern ?: return
        if (!SettingsManager.isGlyphEnabled()) {
            Toast.makeText(context, "Glyph is disabled in settings", Toast.LENGTH_SHORT).show()
            return
        }
        stop()
        isRunning = true
        Toast.makeText(context, "Playing Composer Pattern", Toast.LENGTH_SHORT).show()

        job = scope.launch {
            val frames = p.frames ?: emptyList()
            val start = System.currentTimeMillis()
            for (f in frames) {
                if (!isRunning) break
                val now = System.currentTimeMillis() - start
                val delayMs = max(0L, f.timestamp - now)
                delay(delayMs)
                if (!isRunning) break
                val brightness = scaleBrightness(f.brightness)
                AnimationManager.playGlyphFrame(context, f.zones, brightness, f.duration)
            }
            isRunning = false
            Toast.makeText(context, "Preview completed", Toast.LENGTH_SHORT).show()
        }
    }

    fun previewFallback() {
        if (!SettingsManager.isGlyphEnabled()) {
            Toast.makeText(context, "Glyph is disabled in settings", Toast.LENGTH_SHORT).show()
            return
        }
        stop()
        isRunning = true
        val fallback = SettingsManager.getGlyphCallAnimation()
        Toast.makeText(context, "Playing Fallback: $fallback", Toast.LENGTH_SHORT).show()

        job = scope.launch {
            AnimationManager.playCsv(context, fallback)
            isRunning = false
            Toast.makeText(context, "Preview completed", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) { analyze() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Glyph Pattern Preview") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(status, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(Modifier.padding(16.dp)) {
                Button(
                    onClick = { previewComposer() },
                    enabled = hasPattern && !isRunning,
                    modifier = Modifier.weight(1f)
                ) { Text("Preview Composer") }

                Spacer(Modifier.width(12.dp))

                Button(
                    onClick = { previewFallback() },
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f)
                ) { Text("Preview Fallback") }
            }

            OutlinedButton(
                onClick = { stop() },
                enabled = isRunning,
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) { Text("Stop Preview") }
        }
    }
}
