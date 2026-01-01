package co.aospa.glyph.Settings

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

import android.app.AlertDialog
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import co.aospa.glyph.Manager.AnimationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import kotlin.math.max
import kotlin.random.Random

class GlyphPatternCreatorActivity : ComponentActivity() {

    companion object {
        private const val TAG = "GlyphPatternCreator"
        private const val LED_COUNT = 33
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { PatternCreatorScreen(onBack = { finish() }) } }
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

private data class FrameData(
    val timestamp: Long,
    val zones: IntArray,
    val brightness: Int,
    val duration: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PatternCreatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var selectedZones by remember { mutableStateOf(BooleanArray(LED_COUNT) { false }) }
    var frames by remember { mutableStateOf(listOf<FrameData>()) }

    var currentBrightness by remember { mutableIntStateOf(4095) }
    var currentDuration by remember { mutableIntStateOf(200) }

    var isPreviewRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var previewJob by remember { mutableStateOf<Job?>(null) }

    fun selectedCount(): Int = selectedZones.count { it }
    fun totalDurationMs(): Long = frames.sumOf { it.duration.toLong() }

    fun updateStatusText(): String =
        "Selected: ${selectedCount()} zones | Frames: ${frames.size} | Duration: ${(totalDurationMs() / 1000.0)}s"

    fun clearSelection() {
        selectedZones = BooleanArray(LED_COUNT) { false }
    }

    fun addFrame() {
        val zonesList = selectedZones.withIndex().filter { it.value }.map { it.index }
        if (zonesList.isEmpty()) {
            Toast.makeText(context, "Please select at least one zone", Toast.LENGTH_SHORT).show()
            return
        }
        val zonesArray = zonesList.toIntArray()

        val timestamp = frames.sumOf { it.duration.toLong() }
        val frame = FrameData(timestamp, zonesArray, currentBrightness, currentDuration)
        frames = frames + frame

        Toast.makeText(context, "Frame added at ${timestamp}ms", Toast.LENGTH_SHORT).show()
    }

    fun deleteFrame(index: Int) {
        frames = frames.toMutableList().also { it.removeAt(index) }
    }

    fun stopPreview() {
        isPreviewRunning = false
        previewJob?.cancel()
        previewJob = null
        AnimationManager.stopAll()
    }

    fun preview() {
        if (frames.isEmpty()) return
        stopPreview()
        isPreviewRunning = true
        Toast.makeText(context, "Playing preview...", Toast.LENGTH_SHORT).show()

        previewJob = scope.launch {
            val start = System.currentTimeMillis()
            for (f in frames) {
                if (!isPreviewRunning) break
                val now = System.currentTimeMillis() - start
                val delayMs = max(0L, f.timestamp - now)
                delay(delayMs)
                if (!isPreviewRunning) break
                AnimationManager.playGlyphFrame(context, f.zones, f.brightness, f.duration)
            }
            isPreviewRunning = false
            Toast.makeText(context, "Preview completed", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveToFile(filenameRaw: String) {
        if (frames.isEmpty()) {
            Toast.makeText(context, "No frames to save", Toast.LENGTH_SHORT).show()
            return
        }

        var filename = filenameRaw.trim()
        if (filename.isEmpty()) {
            Toast.makeText(context, "Please enter a filename", Toast.LENGTH_SHORT).show()
            return
        }
        if (!filename.endsWith(".glyphring")) filename += ".glyphring"

        try {
            val json = JSONObject()
            json.put("version", 1)
            json.put("audio_file", filename.replace(".glyphring", ".ogg"))

            var totalDuration = 0L
            for (f in frames) {
                totalDuration = max(totalDuration, f.timestamp + f.duration)
            }
            json.put("duration", totalDuration)

            val framesArray = JSONArray()
            for (f in frames) {
                val frameJson = JSONObject()
                frameJson.put("timestamp", f.timestamp)

                val zonesArray = JSONArray()
                for (z in f.zones) zonesArray.put(z)
                frameJson.put("zones", zonesArray)

                frameJson.put("brightness", f.brightness)
                frameJson.put("duration", f.duration)
                framesArray.put(frameJson)
            }
            json.put("frames", framesArray)

            val baseDir = File(Environment.getExternalStorageDirectory(), "Ringtones")
            val dir = File(baseDir, "SavedPattern")
            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (!created) {
                    Toast.makeText(context, "Failed to create directory", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Failed to create directory: ${dir.absolutePath}")
                    return
                }
            }

            val file = File(dir, filename)
            FileWriter(file).use { it.write(json.toString(2)) }

            Toast.makeText(context, "Saved to: SavedPattern/$filename", Toast.LENGTH_LONG).show()
            Log.d(TAG, "Pattern saved: ${file.absolutePath}")

            AlertDialog.Builder(context)
                .setTitle("Pattern Saved!")
                .setMessage("Pattern saved to:\n${file.absolutePath}\n\nWhat would you like to do?")
                .setPositiveButton("Create Another") { _, _ ->
                    frames = emptyList()
                    clearSelection()
                    Toast.makeText(context, "Ready to create new pattern", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("Done") { _, _ ->
                    stopPreview()
                    (context as? ComponentActivity)?.finish()
                }
                .setNegativeButton("View File") { _, _ ->
                    Toast.makeText(context, "File: ${file.name}", Toast.LENGTH_LONG).show()
                }
                .show()

        } catch (e: Exception) {
            Log.e(TAG, "Error saving pattern", e)
            Toast.makeText(context, "Error saving pattern: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun showSaveDialog() {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "pattern_name"
        }
        AlertDialog.Builder(context)
            .setTitle("Save Pattern")
            .setView(input)
            .setPositiveButton("Save") { _, _ -> saveToFile(input.text?.toString() ?: "") }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // templates
    fun loadTemplate(type: String) {
        fun wave(): List<FrameData> {
            val out = ArrayList<FrameData>()
            val frameCount = 30
            val ledsPerFrame = 4
            for (i in 0 until frameCount) {
                val timestamp = i * 500L
                val start = (i * ledsPerFrame) % LED_COUNT
                val zones = IntArray(ledsPerFrame) { j -> (start + j) % LED_COUNT }
                out.add(FrameData(timestamp, zones, 4095, 400))
            }
            return out
        }

        fun pulse(): List<FrameData> {
            val out = ArrayList<FrameData>()
            val allZones = IntArray(LED_COUNT) { it }
            for (pulse in 0 until 10) {
                val base = pulse * 1500L
                for (step in 0 until 5) {
                    val timestamp = base + (step * 60L)
                    val brightness = 800 + (step * 650)
                    out.add(FrameData(timestamp, allZones.clone(), brightness, 60))
                }
                out.add(FrameData(base + 300, allZones.clone(), 4095, 400))
                for (step in 0 until 5) {
                    val timestamp = base + 700 + (step * 60L)
                    val brightness = 4095 - (step * 650)
                    out.add(FrameData(timestamp, allZones.clone(), brightness, 60))
                }
            }
            return out
        }

        fun blink(): List<FrameData> {
            val out = ArrayList<FrameData>()
            for (i in 0 until 30) {
                val timestamp = i * 500L
                val zones = if (i % 2 == 0) intArrayOf(0, 2, 4, 6, 8, 10)
                else intArrayOf(1, 3, 5, 7, 9, 11)
                out.add(FrameData(timestamp, zones, 4095, 250))
            }
            return out
        }

        fun breathe(): List<FrameData> {
            val out = ArrayList<FrameData>()
            val allZones = IntArray(LED_COUNT) { it }
            for (cycle in 0 until 5) {
                val base = cycle * 3000L
                for (step in 0 until 10) {
                    val timestamp = base + (step * 150L)
                    val brightness = 500 + (step * 360)
                    out.add(FrameData(timestamp, allZones.clone(), brightness, 150))
                }
                for (step in 0 until 10) {
                    val timestamp = base + 1500 + (step * 150L)
                    val brightness = 4095 - (step * 360)
                    out.add(FrameData(timestamp, allZones.clone(), brightness, 150))
                }
            }
            return out
        }

        fun random(): List<FrameData> {
            val out = ArrayList<FrameData>()
            for (i in 0 until 50) {
                val timestamp = i * 300L
                val zoneCount = 3 + Random.nextInt(6)
                val zones = IntArray(zoneCount) { Random.nextInt(LED_COUNT) }
                val brightness = 2000 + Random.nextInt(2096)
                out.add(FrameData(timestamp, zones, brightness, 250))
            }
            return out
        }

        fun allOn(): List<FrameData> {
            val allZones = IntArray(LED_COUNT) { it }
            return listOf(
                FrameData(0, allZones.clone(), 4095, 5000),
                FrameData(5000, allZones.clone(), 3000, 5000),
                FrameData(10000, allZones.clone(), 4095, 5000),
            )
        }

        stopPreview()
        frames = when (type) {
            "wave" -> wave()
            "pulse" -> pulse()
            "blink" -> blink()
            "breathe" -> breathe()
            "random" -> random()
            "allon" -> allOn()
            else -> emptyList()
        }
        clearSelection()

        Toast.makeText(
            context,
            "Template loaded: ${type.uppercase()} (15s, ${frames.size} frames)",
            Toast.LENGTH_LONG
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Glyph Pattern Creator") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {
            Text(
                updateStatusText(),
                Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                "Quick Templates (15s):",
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.titleSmall
            )

            Row(Modifier.padding(horizontal = 16.dp)) {
                TemplateButton("Wave") { loadTemplate("wave") }
                Spacer(Modifier.width(8.dp))
                TemplateButton("Pulse") { loadTemplate("pulse") }
                Spacer(Modifier.width(8.dp))
                TemplateButton("Blink") { loadTemplate("blink") }
            }

            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TemplateButton("Breathe") { loadTemplate("breathe") }
                Spacer(Modifier.width(8.dp))
                TemplateButton("Random") { loadTemplate("random") }
                Spacer(Modifier.width(8.dp))
                TemplateButton("All On") { loadTemplate("allon") }
            }

            Divider()

            Text(
                "Select LED Zones:",
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleSmall
            )

            val cols = 8
            val rows = (LED_COUNT + cols - 1) / cols
            Column(Modifier.padding(horizontal = 16.dp)) {
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth()) {
                        for (c in 0 until cols) {
                            val idx = r * cols + c
                            if (idx >= LED_COUNT) {
                                Spacer(Modifier.weight(1f))
                            } else {
                                val checked = selectedZones[idx]
                                AssistChip(
                                    onClick = {
                                        val copy = selectedZones.clone()
                                        copy[idx] = !copy[idx]
                                        selectedZones = copy
                                    },
                                    label = { Text(idx.toString()) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                OutlinedButton(onClick = { clearSelection() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear Selection")
                }
            }

            Divider()

            Text("Brightness: $currentBrightness", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            Slider(
                value = currentBrightness.toFloat(),
                onValueChange = { currentBrightness = it.toInt().coerceIn(0, 4095) },
                valueRange = 0f..4095f,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Text("Duration (ms): $currentDuration", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            Slider(
                value = currentDuration.toFloat(),
                onValueChange = { currentDuration = it.toInt().coerceIn(50, 1000) },
                valueRange = 50f..1000f,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Row(Modifier.padding(16.dp)) {
                Button(onClick = { addFrame() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Add Frame")
                }
            }

            Divider()

            Text(
                "Timeline:",
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleSmall
            )

            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .heightIn(min = 120.dp, max = 420.dp)
            ) {
                itemsIndexed(frames) { index, f ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Frame ${index + 1}: ${f.timestamp}ms")
                                Text(
                                    "${f.zones.size} zones, B:${f.brightness}, D:${f.duration}ms",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = { deleteFrame(index) }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }

            Row(Modifier.padding(16.dp)) {
                Button(
                    onClick = { preview() },
                    enabled = frames.isNotEmpty() && !isPreviewRunning,
                    modifier = Modifier.weight(1f)
                ) { Text("Preview") }

                Spacer(Modifier.width(10.dp))

                OutlinedButton(
                    onClick = { stopPreview() },
                    enabled = isPreviewRunning,
                    modifier = Modifier.weight(1f)
                ) { Text("Stop") }

                Spacer(Modifier.width(10.dp))

                Button(
                    onClick = { showSaveDialog() },
                    enabled = frames.isNotEmpty() && !isPreviewRunning,
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TemplateButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.weight(1f)) {
        Text(text)
    }
}