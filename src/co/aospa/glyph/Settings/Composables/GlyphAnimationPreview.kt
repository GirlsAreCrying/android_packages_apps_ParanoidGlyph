package co.aospa.glyph.Settings.Composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.aospa.glyph.Constants.Constants
import co.aospa.glyph.R

@Composable
fun GlyphAnimationPreview(
    brightnessBySlug: Map<String, Int>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.bg_protection_background),
            contentDescription = null,
            modifier = Modifier.matchParentSize()
        )

        when (Constants.getDevice()) {
            "phone2" -> Phone2Preview(brightnessBySlug)
            else -> Phone1Preview(brightnessBySlug)
        }
    }
}

private fun ledAlpha(brightness: Int): Float {
    if (brightness <= 0) return 0.30f
    val max = Constants.getMaxBrightness().coerceAtLeast(1)
    val t = (brightness.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    return (0.40f + 0.60f * t).coerceIn(0f, 1f)
}

@Composable
private fun Phone1Preview(brightnessBySlug: Map<String, Int>) {
    DeviceBox(width = 107.2.dp, height = 225.0.dp) {
        NamedImage(name = "bg_device_background", modifier = Modifier.matchParentSize())

        Led("glyph_led_a", ledAlpha(brightnessBySlug["camera"] ?: 0), Alignment.TopStart, 5.51.dp, 5.89.dp)
        Led("glyph_led_b", ledAlpha(brightnessBySlug["slant"] ?: 0), Alignment.TopEnd, (-12.78).dp, 12.70.dp)
        Led("glyph_led_c", ledAlpha(brightnessBySlug["center"] ?: 0), Alignment.TopCenter, 0.dp, 51.1.dp)
        Led("glyph_led_d", ledAlpha(brightnessBySlug["bar"] ?: 0), Alignment.BottomCenter, 0.dp, (-14.18).dp)
        Led("glyph_led_e", ledAlpha(brightnessBySlug["dot"] ?: 0), Alignment.BottomCenter, 0.dp, (-5.89).dp)
    }
}

@Composable
private fun Phone2Preview(brightnessBySlug: Map<String, Int>) {
    DeviceBox(width = 105.43.dp, height = 225.0.dp) {
        NamedImage(name = "bg_device_background", modifier = Modifier.matchParentSize())

        Led("glyph_led_a1", ledAlpha(brightnessBySlug["camera1"] ?: 0), Alignment.TopStart, 6.43.dp, 5.30.dp)
        Led("glyph_led_a2", ledAlpha(brightnessBySlug["camera2"] ?: 0), Alignment.TopStart, 11.61.dp, 29.48.dp)
        Led("glyph_led_b1", ledAlpha(brightnessBySlug["slant1"] ?: 0), Alignment.TopEnd, (-12.91).dp, 11.16.dp)

        Led("glyph_led_c1", ledAlpha(brightnessBySlug["center1"] ?: 0), Alignment.TopEnd, (-6.64).dp, 53.30.dp)
        Led("glyph_led_c2", ledAlpha(brightnessBySlug["center2"] ?: 0), Alignment.TopStart, 6.33.dp, 58.41.dp)
        Led("glyph_led_c3", ledAlpha(brightnessBySlug["center3"] ?: 0), Alignment.TopStart, 5.30.dp, 84.60.dp)
        Led("glyph_led_c6", ledAlpha(brightnessBySlug["center6"] ?: 0), Alignment.TopEnd, (-5.61).dp, 111.75.dp)
        Led("glyph_led_c4", ledAlpha(brightnessBySlug["center4"] ?: 0), Alignment.TopStart, 3.72.dp, 145.25.dp)
        Led("glyph_led_c5", ledAlpha(brightnessBySlug["center5"] ?: 0), Alignment.TopEnd, (-6.64).dp, 145.23.dp)

        Led("glyph_led_d1", ledAlpha(brightnessBySlug["bar1"] ?: 0), Alignment.BottomCenter, 0.dp, (-13.33).dp)
        Led("glyph_led_e1", ledAlpha(brightnessBySlug["dot1"] ?: 0), Alignment.BottomCenter, 0.dp, (-4.97).dp)
    }
}

@Composable
private fun DeviceBox(
    width: Dp,
    height: Dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier.size(width = width, height = height),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun NamedImage(
    name: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val id = context.resources.getIdentifier(name, "drawable", context.packageName)
    if (id != 0) {
        Image(painter = painterResource(id), contentDescription = null, modifier = modifier)
    }
}

@Composable
private fun BoxScope.Led(
    drawableName: String,
    alpha: Float,
    align: Alignment,
    dx: Dp,
    dy: Dp,
) {
    val context = LocalContext.current
    val id = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
    if (id == 0) return

    Image(
        painter = painterResource(id),
        contentDescription = null,
        modifier = Modifier
            .align(align)
            .offset(x = dx, y = dy)
            .graphicsLayer(alpha = alpha)
    )
}
