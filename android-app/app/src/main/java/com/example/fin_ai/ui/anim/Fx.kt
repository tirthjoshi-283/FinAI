package com.example.fin_ai.ui.anim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Fades + slides an item up when it first appears; [index] staggers lists. */
fun Modifier.appear(index: Int = 0): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(40L * index.coerceAtMost(10))
        progress.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 36f
    }
}

/** A springy scale "pop" every time [key] changes (e.g. after adding funds). */
fun Modifier.popOn(key: Any?): Modifier = composed {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(key) {
        if (key == null) return@LaunchedEffect
        scale.snapTo(1.08f)
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}

/** Number that counts up/down to [target] with an ease-out. */
@Composable
fun CountUpText(target: Double, format: (Double) -> String, style: TextStyle, modifier: Modifier = Modifier, durationMs: Int = 900) {
    val v by animateFloatAsState(target.toFloat(), tween(durationMs, easing = FastOutSlowInEasing), label = "countUp")
    // Sized for the final value so the number never wraps or jumps size mid-animation.
    com.example.fin_ai.ui.util.AutoSizeText(format(v.toDouble()), style = style, modifier = modifier, sizingKey = format(target))
}

/** Circular progress ring with a gradient sweep and animated fill. */
@Composable
fun ProgressRing(
    progress: Float,
    size: Dp,
    stroke: Dp,
    colors: List<Color>,
    track: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(1000, easing = FastOutSlowInEasing), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = stroke.toPx()
            val inset = sw / 2
            val arc = Size(this.size.width - sw, this.size.height - sw)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(sw))
            if (animated > 0f) {
                rotate(-90f) {
                    drawArc(
                        Brush.sweepGradient(colors + colors.first()),
                        0f, 360f * animated, false, Offset(inset, inset), arc,
                        style = Stroke(sw, cap = StrokeCap.Round)
                    )
                }
            }
        }
        content()
    }
}

private class Particle(
    val x: Float, val vx: Float, val vy: Float, val color: Color,
    val size: Float, val spin: Float, val wobble: Float,
)

private val CONFETTI = listOf(
    Color(0xFF8B5CF6), Color(0xFF22D3EE), Color(0xFFF59E0B), Color(0xFF10B981),
    Color(0xFFF43F5E), Color(0xFFEC4899), Color(0xFFFFFFFF),
)

/**
 * Full-screen confetti burst, replayed whenever [trigger] increases. Draws nothing at rest,
 * so it can sit on top of a screen permanently without intercepting touches.
 */
@Composable
fun ConfettiOverlay(trigger: Int, modifier: Modifier = Modifier) {
    var t by remember { mutableFloatStateOf(1f) }
    val particles = remember(trigger) {
        List(140) {
            val angle = Random.nextDouble(-Math.PI * 0.95, -Math.PI * 0.05)
            val speed = Random.nextDouble(0.55, 1.35).toFloat()
            Particle(
                x = Random.nextFloat() * 0.4f + 0.3f,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed * 1.6f).toFloat(),
                color = CONFETTI.random(),
                size = Random.nextFloat() * 10f + 8f,
                spin = Random.nextFloat() * 720f - 360f,
                wobble = Random.nextFloat() * 6f,
            )
        }
    }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        val anim = Animatable(0f)
        launch { anim.animateTo(1f, tween(2600, easing = LinearEasing)) { t = value } }
    }
    if (trigger == 0 || t >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val g = 2.2f // gravity, in screen-heights per second²
        val sec = t * 2.6f
        particles.forEach { p ->
            val x = (p.x + p.vx * sec * 0.45f) * size.width + sin(sec * p.wobble) * 12f
            val y = (0.62f + p.vy * sec * 0.5f + 0.5f * g * sec * sec * 0.25f) * size.height
            if (y > size.height + 40f) return@forEach
            rotate(p.spin * sec, Offset(x, y)) {
                drawRect(
                    p.color.copy(alpha = (1f - t).coerceIn(0f, 1f)),
                    topLeft = Offset(x - p.size / 2, y - p.size / 4),
                    size = Size(p.size, p.size / 2),
                )
            }
        }
    }
}
