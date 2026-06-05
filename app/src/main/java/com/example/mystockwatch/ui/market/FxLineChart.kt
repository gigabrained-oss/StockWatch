package com.example.mystockwatch.ui.market

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect

/**
 * 환율 전용 라인차트 (Canvas 기반)
 * - 그라디언트 필 영역
 * - 등장 애니메이션
 * - 최고/최저가 점 표시
 */
@Composable
fun FxLineChart(
    points: List<Float>,
    isUp: Boolean,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) return

    val lineColor   = if (isUp) Color(0xFFE53935) else Color(0xFF1E88E5)
    val fillColorTop = if (isUp) Color(0x44E53935) else Color(0x441E88E5)

    // 등장 애니메이션
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(points) { triggered = true }
    val progress by animateFloatAsState(
        targetValue = if (triggered) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "fxChartAnim",
    )

    Canvas(modifier = modifier) {
        val maxVal = points.max()
        val minVal = points.min()
        val range  = (maxVal - minVal).takeIf { it > 0f } ?: 1f

        val stepX  = size.width / (points.size - 1)

        fun x(i: Int)   = stepX * i
        fun y(v: Float) = size.height * (1f - (v - minVal) / range)

        val visibleCount = (points.size * progress).toInt().coerceAtLeast(2)
        val visiblePoints = points.take(visibleCount)

        clipRect(right = x(visibleCount - 1) + 1f) {

            // ── 그라디언트 필 영역
            val fillPath = Path().apply {
                moveTo(x(0), size.height)
                lineTo(x(0), y(visiblePoints[0]))
                visiblePoints.forEachIndexed { i, v ->
                    if (i > 0) lineTo(x(i), y(v))
                }
                lineTo(x(visiblePoints.size - 1), size.height)
                close()
            }
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(fillColorTop, Color.Transparent),
                    startY = 0f, endY = size.height,
                ),
            )

            // ── 라인
            val linePath = Path().apply {
                moveTo(x(0), y(visiblePoints[0]))
                visiblePoints.forEachIndexed { i, v ->
                    if (i > 0) lineTo(x(i), y(v))
                }
            }
            drawPath(
                linePath,
                color = lineColor,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            // ── 최고/최저 점 표시
            if (progress >= 1f) {
                val maxIdx = visiblePoints.indexOfFirst { it == maxVal }
                val minIdx = visiblePoints.indexOfFirst { it == minVal }
                drawCircle(lineColor, radius = 5f, center = Offset(x(maxIdx), y(maxVal)))
                drawCircle(Color.White, radius = 3f, center = Offset(x(maxIdx), y(maxVal)))
                drawCircle(lineColor, radius = 5f, center = Offset(x(minIdx), y(minVal)))
                drawCircle(Color.White, radius = 3f, center = Offset(x(minIdx), y(minVal)))
            }
        }

        // ── 기준선 (최고/최저)
        drawLine(
            Color(0xFF1E2140), Offset(0f, y(maxVal)), Offset(size.width, y(maxVal)),
            strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
        )
        drawLine(
            Color(0xFF1E2140), Offset(0f, y(minVal)), Offset(size.width, y(minVal)),
            strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
        )

        // ── 최고/최저 레이블
        drawYLabel("▲ ₩%,.0f".format(maxVal), x = size.width, y = y(maxVal) - 8f, lineColor)
        drawYLabel("▼ ₩%,.0f".format(minVal), x = size.width, y = y(minVal) + 28f, lineColor)
    }
}

private fun DrawScope.drawYLabel(text: String, x: Float, y: Float, color: Color) {
    drawContext.canvas.nativeCanvas.drawText(
        text, x, y,
        android.graphics.Paint().apply {
            this.color   = color.toArgb()
            textSize     = 26f
            textAlign    = android.graphics.Paint.Align.RIGHT
            isAntiAlias  = true
            typeface     = android.graphics.Typeface.DEFAULT_BOLD
        },
    )
}
