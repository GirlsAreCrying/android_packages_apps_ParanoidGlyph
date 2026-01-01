package co.aospa.glyph.Settings

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

import android.app.AlertDialog
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import co.aospa.glyph.Composer.GlyphComposerParser
import co.aospa.glyph.Composer.GlyphPattern
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

class GlyphPatternSelectorActivity : ComponentActivity() {

    companion object {
        private const val TAG = "GlyphPatternSelector"
        private const val DEBUG = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { PatternSelectorScreen(onBack = { finish() }) } }
    }
}

@Composable
private fun PatternSelectorScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var ringtoneUri by remember { mutableStateOf<Uri?>(null) }
    var ringtoneFile by remember { mutableStateOf<File?>(null) }
    var currentPatternFile by remember { mutableStateOf<File?>(null) }

    var status by remember { mutableStateOf("Select a pattern to apply to your current ringtone") }
    var currentRingtoneLabel by remember { mutableStateOf("Loading...") }
    var currentPatternLabel by remember { mutableStateOf("None") }

    var patterns by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

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

    fun patternsMatch(p1: GlyphPattern, p2: GlyphPattern): Boolean {
        val f1 = p1.frames ?: return false
        val f2 = p2.frames ?: return false
        if (f1.size != f2.size) return false
        if (p1.duration != p2.duration) return false
        return true
    }

    fun findOriginalPatternName(appliedPattern: File): String {
        val savedPatternDir = File(Environment.getExternalStorageDirectory(), "Ringtones/SavedPattern")
        if (!savedPatternDir.exists()) return appliedPattern.name
        val saved = savedPatternDir.listFiles { _, name -> name.endsWith(".glyphring") } ?: return appliedPattern.name

        return try {
            val appliedParsed = GlyphComposerParser.parseFromFile(appliedPattern.absolutePath) ?: return appliedPattern.name
            for (f in saved) {
                val savedParsed = GlyphComposerParser.parseFromFile(f.absolutePath) ?: continue
                if (patternsMatch(appliedParsed, savedParsed)) return f.name
            }
            appliedPattern.name
        } catch (e: Exception) {
            Log.e("GlyphPatternSelector", "Error comparing patterns", e)
            appliedPattern.name
        }
    }

    fun scanPatterns() {
        val dir = File(Environment.getExternalStorageDirectory(), "Ringtones/SavedPattern")
        if (!dir.exists()) {
            patterns = emptyList()
            status = "No patterns found. Create some patterns first!"
            return
        }
        val files = dir.listFiles { _, name -> name.endsWith(".glyphring") }?.toList().orEmpty()
        patterns = files
        status = if (files.isEmpty()) "No patterns found in SavedPattern folder"
        else "Found ${files.size} pattern(s). Select one to apply."
    }

    fun loadCurrentRingtone() {
        ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
        val uri = ringtoneUri
        if (uri == null) {
            currentRingtoneLabel = "No ringtone set"
            status = "Please set a ringtone first"
            return
        }

        val audioPath = getRealPathFromUri(uri)
        if (audioPath == null) {
            currentRingtoneLabel = uri.lastPathSegment ?: "Unknown"
            status = "⚠  Cannot access ringtone file"
            return
        }

        ringtoneFile = File(audioPath)
        currentRingtoneLabel = ringtoneFile?.name ?: "Unknown"

        val patternPath = GlyphComposerParser.getGlyphPatternPath(audioPath)
        if (patternPath != null) {
            currentPatternFile = File(patternPath)
            val info = findOriginalPatternName(currentPatternFile!!)
            currentPatternLabel = "✓ Applied: $info"
        } else {
            currentPatternFile = null
            currentPatternLabel = "None (using fallback animation)"
        }
    }

    fun copyFile(source: File, dest: File) {
        FileChannel.open(source.toPath()).use { src ->
            FileChannel.open(dest.toPath(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING, java.nio.file.StandardOpenOption.WRITE).use { dst ->
                dst.transferFrom(src, 0, src.size())
            }
        }
    }

    fun applyPattern(patternFile: File) {
        val ring = ringtoneFile
        if (ring == null) {
            Toast.makeText(context, "No ringtone file found", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val dir = ring.parentFile
            val baseName = ring.name.substringBeforeLast('.', ring.name)
            val target = File(dir, "$baseName.glyphring")
            copyFile(patternFile, target)
            currentPatternFile = target
            currentPatternLabel = "✓ ${target.name}"
            Toast.makeText(context, "Pattern applied successfully!", Toast.LENGTH_LONG).show()
            Log.d(TAG, "Pattern applied: ${target.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying pattern", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun removeCurrentPattern() {
        val f = currentPatternFile
        if (f == null || !f.exists()) {
            Toast.makeText(context, "No pattern to remove", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(context)
            .setTitle("Remove Pattern")
            .setMessage(
                "Remove pattern from current ringtone?\n\n" +
                    "The pattern file in SavedPattern folder will NOT be deleted."
            )
            .setPositiveButton("Remove") { _, _ ->
                if (f.delete()) {
                    currentPatternFile = null
                    currentPatternLabel = "None (using fallback animation)"
                    Toast.makeText(context, "Pattern removed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to remove pattern", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    LaunchedEffect(Unit) {
        loadCurrentRingtone()
        scanPatterns()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apply Glyph Pattern") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(status, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("Current Ringtone:", Modifier.padding(horizontal = 16.dp, vertical = 6.dp), style = MaterialTheme.typography.titleSmall)
            Text(currentRingtoneLabel, Modifier.padding(horizontal = 16.dp, vertical = 2.dp))

            Text("Current Pattern:", Modifier.padding(horizontal = 16.dp, vertical = 6.dp), style = MaterialTheme.typography.titleSmall)
            Text(currentPatternLabel, Modifier.padding(horizontal = 16.dp, vertical = 2.dp))

            Spacer(Modifier.height(12.dp))

            Text("Available Patterns:", Modifier.padding(horizontal = 16.dp, vertical = 6.dp), style = MaterialTheme.typography.titleSmall)

            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f, fill = true)
            ) {
                itemsIndexed(patterns) { idx, file ->
                    val parsed = remember(file.absolutePath) { GlyphComposerParser.parseFromFile(file.absolutePath) }
                    val info = if (parsed != null) "${file.name}\n  ${parsed.frames?.size ?: 0} frames, ${(parsed.duration / 1000)}s" else file.name

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedIndex = idx }
                            .padding(vertical = 10.dp)
                    ) {
                        RadioButton(selected = selectedIndex == idx, onClick = { selectedIndex = idx })
                        Spacer(Modifier.width(8.dp))
                        Text(info)
                    }
                    Divider()
                }
            }

            Row(Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        if (selectedIndex == -1) {
                            Toast.makeText(context, "Please select a pattern", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val ring = ringtoneFile
                        if (ring == null) {
                            Toast.makeText(context, "No ringtone file found", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val selected = patterns[selectedIndex]
                        AlertDialog.Builder(context)
                            .setTitle("Apply Pattern")
                            .setMessage("Apply pattern '${selected.name}' to ringtone '${ring.name}'?")
                            .setPositiveButton("Apply") { _, _ -> applyPattern(selected) }
                            .setNegativeButton("Cancel", null)
                            .show()
                    },
                    enabled = selectedIndex != -1,
                    modifier = Modifier.weight(1f)
                ) { Text("Apply Selected") }

                Spacer(Modifier.width(12.dp))

                OutlinedButton(
                    onClick = { removeCurrentPattern() },
                    modifier = Modifier.weight(1f)
                ) { Text("Remove Current") }
            }
        }
    }
}
