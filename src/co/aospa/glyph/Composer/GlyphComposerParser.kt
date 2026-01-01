package co.aospa.glyph.Composer

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader

object GlyphComposerParser {

    private const val TAG = "GlyphComposerParser"
    private const val DEBUG = true

    @JvmStatic
    fun parseFromFile(filePath: String?): GlyphPattern? {
        if (filePath.isNullOrBlank()) {
            if (DEBUG) Log.e(TAG, "Invalid file path")
            return null
        }

        val file = File(filePath)
        if (!file.exists() || !file.canRead()) {
            if (DEBUG) Log.e(TAG, "File does not exist or cannot be read: $filePath")
            return null
        }

        return try {
            BufferedReader(FileReader(file)).use { reader ->
                val json = buildString {
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        append(line)
                    }
                }
                val pattern = parseJson(json)
                if (DEBUG) Log.d(TAG, "Successfully parsed pattern from: $filePath")
                pattern
            }
        } catch (e: IOException) {
            if (DEBUG) Log.e(TAG, "Error reading file: $filePath", e)
            null
        } catch (e: JSONException) {
            if (DEBUG) Log.e(TAG, "Invalid JSON format in: $filePath", e)
            null
        }
    }

    @JvmStatic
    fun parseFromUri(context: Context, uri: Uri?): GlyphPattern? {
        if (uri == null) {
            if (DEBUG) Log.e(TAG, "Invalid URI")
            return null
        }

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    val json = buildString {
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            append(line)
                        }
                    }
                    val pattern = parseJson(json)
                    if (DEBUG) Log.d(TAG, "Successfully parsed pattern from URI: $uri")
                    pattern
                }
            }
        } catch (e: IOException) {
            if (DEBUG) Log.e(TAG, "Error reading URI: $uri", e)
            null
        } catch (e: JSONException) {
            if (DEBUG) Log.e(TAG, "Invalid JSON format from URI: $uri", e)
            null
        }
    }

    @Throws(JSONException::class)
    private fun parseJson(jsonString: String): GlyphPattern {
        val json = JSONObject(jsonString)

        val pattern = GlyphPattern().apply {
            version = json.optInt("version", 1)
            audioFile = json.optString("audio_file", "")
            duration = json.optLong("duration", 0L)
        }

        val framesArray: JSONArray = json.getJSONArray("frames")
        val frames = ArrayList<GlyphPattern.GlyphFrame>(framesArray.length())

        for (i in 0 until framesArray.length()) {
            val frameJson = framesArray.getJSONObject(i)
            val frame = GlyphPattern.GlyphFrame().apply {
                timestamp = frameJson.getLong("timestamp")
                brightness = frameJson.getInt("brightness")
                duration = frameJson.getInt("duration")

                val zonesArray = frameJson.getJSONArray("zones")
                val zones = IntArray(zonesArray.length())
                for (j in 0 until zonesArray.length()) {
                    zones[j] = zonesArray.getInt(j)
                }
                this.zones = zones
            }
            frames.add(frame)
        }

        pattern.frames = frames
        return pattern
    }

    @JvmStatic
    fun getGlyphPatternPath(audioPath: String?): String? {
        if (audioPath == null) return null

        var basePath = audioPath
        val lastDot = audioPath.lastIndexOf('.')
        if (lastDot > 0) {
            basePath = audioPath.substring(0, lastDot)
        }

        val glyphPath = "$basePath.glyphring"
        val glyphFile = File(glyphPath)

        return if (glyphFile.exists() && glyphFile.canRead()) {
            if (DEBUG) Log.d(TAG, "Found Glyph pattern file: $glyphPath")
            glyphPath
        } else {
            if (DEBUG) Log.d(TAG, "No Glyph pattern found for: $audioPath")
            null
        }
    }

    @JvmStatic
    fun isValid(pattern: GlyphPattern?): Boolean {
        if (pattern == null) return false
        val frames = pattern.frames
        if (frames.isNullOrEmpty()) return false
        if (pattern.duration <= 0) return false

        for (frame in frames) {
            val zones = frame.zones
            if (zones == null || zones.isEmpty()) return false
            if (frame.brightness < 0 || frame.brightness > 4095) return false
            if (frame.timestamp < 0) return false
        }

        return true
    }
}
