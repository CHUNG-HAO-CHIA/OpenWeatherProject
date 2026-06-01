package com.app.openweather.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ── Cloud colour theme — varies per weather condition ─────────────────────────

private data class CloudTheme(
    val body: Color,        // main fill
    val outline: Color,     // stroke outline
    val shadowAlpha: Float, // drop-shadow opacity multiplier
)

private fun cloudTheme(iconCode: String): CloudTheme {
    val isDay = iconCode.endsWith("d")
    return when (iconCode.take(2)) {
        "02" -> if (isDay)
            CloudTheme(Color(0xFFF2F6FB), Color.White,           shadowAlpha = 0.07f)  // bright white puffs
        else
            CloudTheme(Color(0xFF8898B0), Color(0xFF99AABB),     shadowAlpha = 0.14f)  // silver-blue night
        "03" -> if (isDay)
            CloudTheme(Color(0xFFE0EAF5), Color.White,           shadowAlpha = 0.11f)  // day mid-white
        else
            CloudTheme(Color(0xFF5C6E90), Color(0xFF7080A8),     shadowAlpha = 0.20f)  // night slate-blue
        "04" -> if (isDay)
            CloudTheme(Color(0xFFB0BECF), Color(0xFFCCD4DF),     shadowAlpha = 0.22f)  // heavy grey overcast
        else
            CloudTheme(Color(0xFF353D52), Color(0xFF454D62),     shadowAlpha = 0.28f)  // dark night overcast
        "09", "10" -> if (isDay)
            CloudTheme(Color(0xFF6B7A90), Color(0xFF7A8AA0),     shadowAlpha = 0.24f)  // rain-day dark grey
        else
            CloudTheme(Color(0xFF3A4255), Color(0xFF4A5265),     shadowAlpha = 0.30f)  // rain-night very dark
        "11" ->
            CloudTheme(Color(0xFF252B3A), Color(0xFF353B4A),     shadowAlpha = 0.35f)  // thunderstorm purple-black
        "13" -> if (isDay)
            CloudTheme(Color(0xFFDEEAFF), Color.White,           shadowAlpha = 0.05f)  // snow icy white
        else
            CloudTheme(Color(0xFF7080A0), Color(0xFF8898B5),     shadowAlpha = 0.16f)  // snow night
        else ->  // fallback
            CloudTheme(Color(0xFFE8EFF8), Color.White,           shadowAlpha = 0.12f)
    }
}

@Composable
fun WeatherEffectCanvas(iconCode: String, modifier: Modifier = Modifier) {
    val prefix = iconCode.take(2)
    val isDay = iconCode.endsWith("d")

    when (prefix) {
        "01" -> if (isDay) SunRaysEffect(modifier) else MoonAndStarsEffect(modifier)
        "02" -> {
            if (isDay) SunRaysEffect(modifier) else MoonAndStarsEffect(modifier)
            DriftingCloudsEffect(modifier, count = 1, alpha = 0.45f, theme = cloudTheme(iconCode))
        }
        "03" -> {
            if (isDay) SunRaysEffect(modifier) else MoonAndStarsEffect(modifier)
            DriftingCloudsEffect(modifier, count = 3, alpha = 0.70f, theme = cloudTheme(iconCode))
        }
        "04" -> {
            if (!isDay) StarfieldEffect(modifier)
            DriftingCloudsEffect(modifier, count = 5, alpha = 0.90f, theme = cloudTheme(iconCode))
        }
        "09" -> RainEffect(modifier, intensity = 20, speed = 1800)
        "10" -> RainEffect(modifier, intensity = 45, speed = 1400)
        "11" -> ThunderstormEffect(modifier, rainIntensity = 70)
        "13" -> SnowEffect(modifier)
        "50" -> FogEffect(modifier)
    }
}

// ── Clear Day: sun glow + soft corona petals ──────────────────────────────────

@Composable
private fun SunRaysEffect(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "sun")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing)),
        label = "sunAngle",
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawSun(angle)
    }
}

private fun DrawScope.drawSun(rotationDeg: Float) {
    val cx = size.width * 0.78f
    val cy = size.height * 0.10f
    val discR = size.width * 0.055f

    // Bright yellow/gold colors
    val sunColor = Color(0xFFFDD835)
    val glowColor = Color(0xFFFFEB3B)

    drawCircle(color = glowColor.copy(alpha = 0.10f), radius = discR * 5.0f, center = Offset(cx, cy))
    drawCircle(color = glowColor.copy(alpha = 0.15f), radius = discR * 3.0f, center = Offset(cx, cy))

    val petalCount = 8
    val petalDist = discR * 1.5f
    val petalR    = discR * 0.9f

    rotate(rotationDeg, pivot = Offset(cx, cy)) {
        repeat(petalCount) { i ->
            val rad = i * (2f * PI.toFloat() / petalCount)
            drawCircle(
                color = glowColor.copy(alpha = 0.12f),
                radius = petalR,
                center = Offset(cx + petalDist * cos(rad), cy + petalDist * sin(rad)),
            )
        }
    }

    drawCircle(color = sunColor, radius = discR, center = Offset(cx, cy))
    drawCircle(color = Color.White.copy(alpha = 0.5f), radius = discR * 0.5f, center = Offset(cx, cy))
}

// ── Clear Night: Moon + Twinkling stars ──────────────────────────────────────

@Composable
private fun MoonAndStarsEffect(modifier: Modifier) {
    StarfieldEffect(modifier)
    Canvas(modifier = modifier.fillMaxSize()) {
        drawMoon()
    }
}

private fun DrawScope.drawMoon() {
    val cx = size.width * 0.78f
    val cy = size.height * 0.12f
    val discR = size.width * 0.045f

    // Moon glow
    drawCircle(
        color = Color(0xFFB0C4DE).copy(alpha = 0.15f),
        radius = discR * 2.2f,
        center = Offset(cx, cy)
    )

    // Create a crescent shape using Path Difference
    val moonPath = Path().apply {
        addOval(Rect(center = Offset(cx, cy), radius = discR))
    }
    val maskPath = Path().apply {
        addOval(Rect(center = Offset(cx - discR * 0.45f, cy - discR * 0.25f), radius = discR * 0.95f))
    }
    moonPath.op(moonPath, maskPath, PathOperation.Difference)

    drawPath(path = moonPath, color = Color(0xFFF0F0F0))
}

private data class Star(val x: Float, val y: Float, val radius: Float, val phase: Float)

@Composable
private fun StarfieldEffect(modifier: Modifier) {
    val stars = remember {
        List(60) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.7f,
                radius = Random.nextFloat() * 2.5f + 0.5f,
                phase = Random.nextFloat() * 2f * PI.toFloat(),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "stars")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "starTime",
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        stars.forEach { star ->
            val alpha = (0.4f + 0.5f * sin(time + star.phase)).coerceIn(0f, 1f)
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = star.radius,
                center = Offset(star.x * size.width, star.y * size.height),
            )
        }
    }
}

// ── Rain ──────────────────────────────────────────────────────────────────────

private data class RainDrop(
    val x: Float,
    val phase: Float,
    val speedFactor: Float,
    val length: Float,
    val alpha: Float,
)

@Composable
private fun RainEffect(modifier: Modifier, intensity: Int, speed: Int) {
    val drops = remember(intensity) {
        List(intensity) {
            RainDrop(
                x           = Random.nextFloat(),
                phase       = Random.nextFloat(),
                speedFactor = Random.nextFloat() * 0.5f + 0.7f,
                length      = Random.nextFloat() * 28f + 12f,
                alpha       = Random.nextFloat() * 0.30f + 0.15f,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "rain")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(speed, easing = LinearEasing)),
        label = "rainProgress",
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drops.forEach { drop ->
            val p = (progress * drop.speedFactor + drop.phase) % 1f
            val y = p * (size.height + drop.length) - drop.length
            val x = drop.x * size.width
            drawLine(
                color = Color(0xFFB0D4FF).copy(alpha = drop.alpha),
                start = Offset(x, y),
                end   = Offset(x - drop.length * 0.18f, y + drop.length),
                strokeWidth = 1.5f,
                cap   = StrokeCap.Round,
            )
        }
    }
}

// ── Thunderstorm ──────────────────────────────────────────────────────────────

@Composable
private fun ThunderstormEffect(modifier: Modifier, rainIntensity: Int = 80) {
    var lightningAlpha by remember { mutableFloatStateOf(0f) }
    var showBolt by remember { mutableStateOf(false) }
    var boltX by remember { mutableFloatStateOf(0.5f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2000, 5000))
            boltX = Random.nextFloat() * 0.6f + 0.2f
            showBolt = true
            lightningAlpha = 0.6f
            delay(80)
            lightningAlpha = 0f
            delay(120)
            lightningAlpha = 0.4f
            delay(60)
            lightningAlpha = 0f
            showBolt = false
        }
    }

    RainEffect(modifier, intensity = rainIntensity, speed = 1100)

    Canvas(modifier = modifier.fillMaxSize()) {
        if (lightningAlpha > 0f) {
            drawRect(color = Color(0xFFDDEEFF).copy(alpha = lightningAlpha * 0.4f), size = size)
            if (showBolt) drawLightningBolt(boltX, lightningAlpha)
        }
    }
}

private fun DrawScope.drawLightningBolt(normalizedX: Float, alpha: Float) {
    val startX = normalizedX * size.width
    val color = Color(0xFFFFFFCC).copy(alpha = alpha)
    val strokeW = 3f
    val pts = listOf(
        Offset(startX, 0f),
        Offset(startX - size.width * 0.04f, size.height * 0.18f),
        Offset(startX + size.width * 0.03f, size.height * 0.22f),
        Offset(startX - size.width * 0.05f, size.height * 0.40f),
    )
    for (i in 0 until pts.size - 1) {
        drawLine(color, pts[i], pts[i + 1], strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color.copy(alpha = alpha * 0.3f), pts[i], pts[i + 1], strokeWidth = strokeW * 6f, cap = StrokeCap.Round)
    }
}

// ── Snow ──────────────────────────────────────────────────────────────────────

private data class Snowflake(
    val x: Float,
    val drift: Float,
    val radius: Float,
    val speedFactor: Float,
    val phase: Float,
)

@Composable
private fun SnowEffect(modifier: Modifier) {
    val flakes = remember {
        List(50) {
            Snowflake(
                x = Random.nextFloat(),
                drift = (Random.nextFloat() - 0.5f) * 0.04f,
                radius = Random.nextFloat() * 4f + 2f,
                speedFactor = Random.nextFloat() * 0.5f + 0.5f,
                phase = Random.nextFloat(),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "snow")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)),
        label = "snowProgress",
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        flakes.forEach { flake ->
            val p = ((progress * flake.speedFactor) + flake.phase) % 1f
            val y = p * (size.height + flake.radius * 2) - flake.radius
            val swingX = sin(p * 2f * PI.toFloat() * 3f + flake.phase * PI.toFloat()) * size.width * 0.03f
            val x = (flake.x + flake.drift * p) * size.width + swingX
            val alpha = if (p > 0.85f) (1f - p) / 0.15f * 0.8f else 0.8f
            drawCircle(color = Color.White.copy(alpha = alpha), radius = flake.radius, center = Offset(x, y))
        }
    }
}

// ── Clouds drifting ───────────────────────────────────────────────────────────

private data class CloudPuff(val dx: Float, val dy: Float, val r: Float, val layer: Int = 0)

private val CLOUD_A = listOf(
    CloudPuff(0.0f,  0.0f, 1.00f, 0),
    CloudPuff(1.5f,  0.1f, 0.90f, 0),
    CloudPuff(2.9f,  0.0f, 0.85f, 0),
    CloudPuff(4.1f,  0.1f, 0.75f, 0),
    CloudPuff(0.7f, -0.75f, 0.80f, 1),
    CloudPuff(1.9f, -0.95f, 0.95f, 1),
    CloudPuff(3.1f, -0.80f, 0.78f, 1),
    CloudPuff(1.4f, -1.55f, 0.60f, 2),
    CloudPuff(2.4f, -1.65f, 0.58f, 2),
)

private val CLOUD_B = listOf(
    CloudPuff(0.0f,  0.0f, 0.85f, 0),
    CloudPuff(1.4f,  0.05f, 0.90f, 0),
    CloudPuff(2.7f,  0.0f, 0.80f, 0),
    CloudPuff(0.6f, -0.70f, 0.75f, 1),
    CloudPuff(1.7f, -0.90f, 0.88f, 1),
    CloudPuff(2.8f, -0.68f, 0.72f, 1),
    CloudPuff(1.5f, -1.45f, 0.55f, 2),
)

private val CLOUD_TEMPLATES = listOf(CLOUD_A, CLOUD_B)

private data class CloudInstance(
    val templateIdx: Int,
    val baseX: Float,
    val yFrac: Float,
    val unit: Float,
    val speed: Float,
    val alpha: Float,
)

// Full pool of 8 clouds ordered by visual priority.
// count=3 → first 3, count=5 → first 5, count=8 → all 8.
// Each row: templateIdx, baseX, yFrac, unit, speed, alphaMultiplier
private val CLOUD_POOL = listOf(
    // ── foreground (3 essential clouds) ──────────────────────────────────
    CloudInstance(0, 0.05f, 0.30f, 0.125f, 0.013f, 1.00f),  // [0] large left-center
    CloudInstance(1, 0.82f, 0.25f, 0.100f, 0.011f, 1.00f),  // [1] large right
    CloudInstance(0, 0.48f, 0.35f, 0.115f, 0.015f, 1.00f),  // [2] large center
    // ── additional mid-ground (brings total to 5) ─────────────────────
    CloudInstance(1, 0.25f, 0.15f, 0.085f, 0.009f, 0.80f),  // [3] mid left-upper
    CloudInstance(0, 0.68f, 0.18f, 0.078f, 0.008f, 0.78f),  // [4] mid right-upper
    // ── background (brings total to 8) ───────────────────────────────────
    CloudInstance(1, 0.00f, 0.08f, 0.068f, 0.007f, 0.65f),  // [5] bg far-left
    CloudInstance(0, 0.55f, 0.10f, 0.072f, 0.006f, 0.62f),  // [6] bg center
    CloudInstance(1, 0.88f, 0.06f, 0.065f, 0.008f, 0.60f),  // [7] bg far-right
)

@Composable
private fun DriftingCloudsEffect(modifier: Modifier, count: Int, alpha: Float, theme: CloudTheme) {
    val clouds = remember(count) {
        CLOUD_POOL.take(count)
    }
    val transition = rememberInfiniteTransition(label = "clouds")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing)),
        label = "cloudProgress",
    )
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(
                alpha = alpha,
                compositingStrategy = CompositingStrategy.Offscreen
            )
    ) {
        clouds.forEach { cloud ->
            val unit = cloud.unit * size.width
            val cx = ((cloud.baseX + progress * cloud.speed) % 1.35f - 0.18f) * size.width
            val cy = cloud.yFrac * size.height
            drawCloud(cx, cy, unit, CLOUD_TEMPLATES[cloud.templateIdx], cloud.alpha, theme)
        }
    }
}

private fun DrawScope.drawCloud(cx: Float, cy: Float, unit: Float, puffs: List<CloudPuff>, alpha: Float, theme: CloudTheme) {
    // 1. Create a union Path for the entire cloud to get a clean outer outline and body
    val cloudPath = Path()
    puffs.forEach { p ->
        val pPath = Path().apply {
            addOval(Rect(center = Offset(cx + p.dx * unit, cy + p.dy * unit), radius = p.r * unit))
        }
        cloudPath.op(cloudPath, pPath, PathOperation.Union)
    }

    // 2. Create union Path for shadow
    val shadowPath = Path()
    puffs.forEach { p ->
        val pPath = Path().apply {
            addOval(Rect(
                center = Offset(cx + p.dx * unit + unit * 0.12f, cy + p.dy * unit + unit * 0.20f),
                radius = p.r * unit * 1.08f
            ))
        }
        shadowPath.op(shadowPath, pPath, PathOperation.Union)
    }

    // Draw shadow
    drawPath(shadowPath, color = Color.Black.copy(alpha = alpha * theme.shadowAlpha))

    // Cloud body — single merged path, colour from theme
    drawPath(cloudPath, color = theme.body.copy(alpha = alpha))

    // Outline
    drawPath(
        path = cloudPath,
        color = theme.outline.copy(alpha = alpha * 0.75f),
        style = Stroke(width = 1.8.dp.toPx())
    )
}

// ── Fog / Mist ────────────────────────────────────────────────────────────────

@Composable
private fun FogEffect(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "fog")
    val offset1 by transition.animateFloat(
        initialValue = -0.3f, targetValue = 0.1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "fog1",
    )
    val offset2 by transition.animateFloat(
        initialValue = 0.1f, targetValue = -0.2f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse),
        label = "fog2",
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawFogBand(yFraction = 0.30f + offset1, alpha = 0.07f)
        drawFogBand(yFraction = 0.55f + offset2, alpha = 0.05f)
        drawFogBand(yFraction = 0.75f + offset1 * 0.5f, alpha = 0.06f)
    }
}

private fun DrawScope.drawFogBand(yFraction: Float, alpha: Float) {
    val y = yFraction * size.height
    val bandH = size.height * 0.18f
    drawRect(
        color = Color.White.copy(alpha = alpha),
        topLeft = Offset(0f, y - bandH / 2f),
        size = Size(size.width, bandH),
    )
}
