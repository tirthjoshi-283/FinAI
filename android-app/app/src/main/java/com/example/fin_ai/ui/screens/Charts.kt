package com.example.fin_ai.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.hypot

data class Slice(val label: String, val value: Double, val color: Color)

/** Animated donut; tapping a segment reports its label. Centre shows [centerTop]/[centerBottom]. */
@Composable
fun DonutChart(
    slices: List<Slice>,
    selected: String?,
    onSelect: (String?) -> Unit,
    centerTop: String,
    centerBottom: String,
    size: Dp = 200.dp,
) {
    val total = slices.sumOf { it.value }.takeIf { it > 0 } ?: 1.0
    val sweep = remember(slices) { Animatable(0f) }
    LaunchedEffect(slices) { sweep.snapTo(0f); sweep.animateTo(1f, tween(700)) }
    val track = MaterialTheme.colorScheme.surfaceContainer

    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .size(size)
                .pointerInput(slices) {
                    detectTapGestures { p ->
                        val c = Offset(this.size.width / 2f, this.size.height / 2f)
                        val r = hypot(p.x - c.x, p.y - c.y)
                        if (r < this.size.width * 0.28f || r > this.size.width / 2f) { onSelect(null); return@detectTapGestures }
                        var angle = Math.toDegrees(atan2((p.y - c.y).toDouble(), (p.x - c.x).toDouble())) + 90
                        if (angle < 0) angle += 360
                        var acc = 0.0
                        val hit = slices.firstOrNull { s -> acc += s.value / total * 360; angle <= acc }
                        onSelect(hit?.label?.takeIf { it != selected })
                    }
                }
        ) {
            val stroke = this.size.width * 0.16f
            val inset = stroke / 2 + 6f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            var start = -90f
            slices.forEach { s ->
                val angle = (s.value / total * 360f).toFloat() * sweep.value
                val emphasised = selected == null || selected == s.label
                drawArc(
                    s.color.copy(alpha = if (emphasised) 1f else 0.25f),
                    start, (angle - 1.2f).coerceAtLeast(0.5f), false,
                    Offset(inset, inset), arcSize,
                    style = Stroke(if (selected == s.label) stroke * 1.12f else stroke)
                )
                start += angle
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            com.example.fin_ai.ui.util.AutoSizeText(
                centerTop, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                textAlign = TextAlign.Center, modifier = Modifier.width(size * 0.62f)
            )
            Text(centerBottom, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

data class BarGroup(val label: String, val values: List<Double>)

/** Grouped vertical bars (e.g. money out vs in per month), animated on first draw. */
@Composable
fun GroupedBarChart(groups: List<BarGroup>, colors: List<Color>, height: Dp = 170.dp, highlightLast: Boolean = true) {
    val max = groups.flatMap { it.values }.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val grow = remember(groups) { Animatable(0f) }
    LaunchedEffect(groups) { grow.snapTo(0f); grow.animateTo(1f, tween(650)) }
    val grid = MaterialTheme.colorScheme.surfaceContainer

    Column {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            for (i in 1..3) {
                val y = this.size.height * i / 4f
                drawLine(grid, Offset(0f, y), Offset(this.size.width, y), strokeWidth = 1f)
            }
            val slot = this.size.width / groups.size.coerceAtLeast(1)
            val barCount = groups.firstOrNull()?.values?.size ?: 1
            val barW = (slot * 0.62f) / barCount
            groups.forEachIndexed { gi, g ->
                val left = gi * slot + slot * 0.19f
                g.values.forEachIndexed { vi, v ->
                    val h = (v / max * this.size.height * grow.value).toFloat()
                    val dim = highlightLast && gi != groups.lastIndex
                    drawRoundRect(
                        colors[vi % colors.size].copy(alpha = if (dim) 0.55f else 1f),
                        topLeft = Offset(left + vi * barW + 2f, this.size.height - h),
                        size = Size(barW - 4f, h),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            groups.forEach { g ->
                Text(
                    g.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/** Thin daily bars for one month with a dashed average line. */
@Composable
fun DailyBars(values: List<Double>, color: Color, average: Double, height: Dp = 110.dp) {
    val max = (values.maxOrNull() ?: 0.0).coerceAtLeast(average).takeIf { it > 0 } ?: 1.0
    val avgColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.fillMaxWidth().height(height)) {
        val slot = this.size.width / values.size.coerceAtLeast(1)
        values.forEachIndexed { i, v ->
            val h = (v / max * this.size.height).toFloat()
            if (h > 0f) drawRoundRect(
                color.copy(alpha = if (v > average) 1f else 0.6f),
                topLeft = Offset(i * slot + slot * 0.2f, this.size.height - h),
                size = Size(slot * 0.6f, h),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
        if (average > 0) {
            val y = (this.size.height - average / max * this.size.height).toFloat()
            var x = 0f
            while (x < this.size.width) {
                drawLine(avgColor, Offset(x, y), Offset((x + 10f).coerceAtMost(this.size.width), y), strokeWidth = 2f)
                x += 18f
            }
        }
    }
}

@Composable
fun Legend(items: List<Pair<String, Color>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(10.dp)) { drawCircle(color) }
                Text("  $label", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}
