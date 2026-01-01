package co.aospa.glyph.Settings.Composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.Utils.ResourceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun InlineGlyphAnimationPreview(
    animationName: String?,
    playing: Boolean,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    timeBetweenLoopsMs: Int = 0,
) {
    val device = remember { Constants.getDevice() }

    // Slugs used by preview renderer
    val slugs: List<String> = remember(device) {
        if (device == "phone2") {
            listOf(
                "camera1", "camera2", "slant1",
                "center1", "center2", "center3", "center4", "center5", "center6",
                "bar1", "dot1"
            )
        } else {
            listOf("camera", "slant", "center", "bar", "dot")
        }
    }

    // brightness per slug in [0..maxBrightness]
    val brightnessBySlug = remember { mutableStateMapOf<String, Int>() }

    fun clear() {
        slugs.forEach { brightnessBySlug[it] = 0 }
    }

    LaunchedEffect(slugs) {
        clear()
    }

    // Player loop
    LaunchedEffect(animationName, playing, device) {
        clear()

        val name = animationName
        if (!playing || name.isNullOrBlank()) return@LaunchedEffect

        while (isActive && playing) {
            val lines: List<String> = withContext(Dispatchers.IO) {
                try {
                    val input = ResourceUtils.getAnimation(name)
                    BufferedReader(InputStreamReader(input)).use { br ->
                        buildList {
                            var line: String?
                            while (br.readLine().also { line = it } != null) {
                                val l = line!!.trim()
                                if (l.isNotEmpty()) add(l)
                            }
                        }
                    }
                } catch (_: Throwable) {
                    emptyList()
                }
            }

            if (lines.isEmpty()) {
                delay(250)
                continue
            }

            for (raw in lines) {
                if (!isActive || !playing) break

                val cleaned = raw.replace(" ", "").let { if (it.endsWith(",")) it.dropLast(1) else it }
                val split = cleaned.split(",")

                // Map CSV frame to preview LEDs
                when {
                    device == "phone1" && split.size == 5 -> {
                        // camera, slant, center, bar, dot
                        brightnessBySlug["camera"] = split[0].toIntOrNull() ?: 0
                        brightnessBySlug["slant"] = split[1].toIntOrNull() ?: 0
                        brightnessBySlug["center"] = split[2].toIntOrNull() ?: 0
                        brightnessBySlug["bar"] = split[3].toIntOrNull() ?: 0
                        brightnessBySlug["dot"] = split[4].toIntOrNull() ?: 0
                    }

                    device == "phone2" && split.size == 5 -> {
                        val cam = split[0].toIntOrNull() ?: 0
                        brightnessBySlug["camera1"] = cam
                        brightnessBySlug["camera2"] = cam

                        brightnessBySlug["slant1"] = split[1].toIntOrNull() ?: 0

                        val center = split[2].toIntOrNull() ?: 0
                        brightnessBySlug["center1"] = center
                        brightnessBySlug["center2"] = center
                        brightnessBySlug["center3"] = center
                        brightnessBySlug["center4"] = center
                        brightnessBySlug["center5"] = center
                        brightnessBySlug["center6"] = center

                        brightnessBySlug["bar1"] = split[3].toIntOrNull() ?: 0
                        brightnessBySlug["dot1"] = split[4].toIntOrNull() ?: 0
                    }

                    device == "phone2" && split.size == 33 -> {
                        val idxMap = intArrayOf(0, 1, 2, 3, 19, 20, 21, 22, 23, 25, 24)
                        slugs.forEachIndexed { i, slug ->
                            val v = split.getOrNull(idxMap[i])?.toIntOrNull() ?: 0
                            brightnessBySlug[slug] = v
                        }
                    }
                }

                delay(16L)
            }

            if (!isActive || !playing) break

            clear()
            if (timeBetweenLoopsMs > 0) delay(timeBetweenLoopsMs.toLong())
        }
    }

    GlyphAnimationPreview(
        brightnessBySlug = brightnessBySlug,
        modifier = modifier
    )
}
