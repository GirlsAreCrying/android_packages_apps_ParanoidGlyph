package co.aospa.glyph.Utils

import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

object FileUtils {
    private const val TAG = "GlyphFileUtils"
    private const val DEBUG = true

    @JvmStatic
    fun readLine(fileName: String): String? {
        var reader: BufferedReader? = null
        return try {
            reader = BufferedReader(FileReader(fileName), 512)
            reader.readLine()
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "No such file $fileName for reading", e)
            null
        } catch (e: IOException) {
            Log.e(TAG, "Could not read from file $fileName", e)
            null
        } finally {
            try {
                reader?.close()
            } catch (_: IOException) {
            }
        }
    }

    @JvmStatic
    fun readLineInt(fileName: String): Int {
        val line = readLine(fileName) ?: return 0
        return try {
            line.replace("0x", "").toInt()
        } catch (e: NumberFormatException) {
            Log.e(TAG, "Could not convert string to int from file $fileName", e)
            0
        }
    }

    @JvmStatic
    fun writeLine(fileName: String, value: String) {
        val modePath = ResourceUtils.getString("glyph_settings_paths_mode_absolute")
        var writerMode: BufferedWriter? = null
        var writerValue: BufferedWriter? = null

        try {
            if (modePath.isNotBlank()) {
                writerMode = BufferedWriter(FileWriter(modePath))
                writerMode.write("1")
            }
            writerValue = BufferedWriter(FileWriter(fileName))
            writerValue.write(value)
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "No such file $fileName for writing", e)
        } catch (e: IOException) {
            Log.e(TAG, "Could not write to file $fileName", e)
        } finally {
            try {
                writerMode?.close()
                writerValue?.close()
            } catch (_: IOException) {
                // ignored
            }
        }
    }

    @JvmStatic
    fun writeLine(fileName: String, value: Int) = writeLine(fileName, value.toString())

    @JvmStatic
    fun writeLine(fileName: String, value: Float) = writeLine(fileName, value.toString())

    @JvmStatic
    fun writeAllLed(value: String) {
        writeLine(ResourceUtils.getString("glyph_settings_paths_all_absolute"), value)
    }

    @JvmStatic
    fun writeAllLed(value: Int) = writeAllLed(value.toString())

    @JvmStatic
    fun writeAllLed(value: Float) = writeAllLed(kotlin.math.round(value).toInt().toString())

    @JvmStatic
    fun writeFrameLed(value: String) {
        writeLine(ResourceUtils.getString("glyph_settings_paths_frame_absolute"), value)
    }

    @JvmStatic
    fun writeFrameLed(value: IntArray) {
        // Java: Arrays.toString(...).replaceAll("\\[|\\]", "").replace(", ", " ")
        val s = value.joinToString(separator = " ")
        writeFrameLed(s)
    }

    @JvmStatic
    fun writeFrameLed(value: FloatArray) {
        val ints = IntArray(value.size) { i -> kotlin.math.round(value[i]).toInt() }
        writeFrameLed(ints)
    }

    @JvmStatic
    fun writeSingleLed(led: String, value: String) {
        writeLine(ResourceUtils.getString("glyph_settings_paths_single_absolute"), "$led $value")
    }

    @JvmStatic
    fun writeSingleLed(led: Int, value: String) = writeSingleLed(led.toString(), value)

    @JvmStatic
    fun writeSingleLed(led: String, value: Int) = writeSingleLed(led, value.toString())

    @JvmStatic
    fun writeSingleLed(led: String, value: Float) =
        writeSingleLed(led, kotlin.math.round(value).toInt().toString())

    @JvmStatic
    fun writeSingleLed(led: Int, value: Float) =
        writeSingleLed(led.toString(), kotlin.math.round(value).toInt().toString())
}
