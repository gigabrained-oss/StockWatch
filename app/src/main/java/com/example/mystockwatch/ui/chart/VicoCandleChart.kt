package com.example.mystockwatch.ui.chart

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val colorUp   = Color(0xFFE53935)
private val colorDown = Color(0xFF1E88E5)
private val colorMa5  = Color(0xFFFF6B6B)
private val colorMa20 = Color(0xFFFFD700)
private val colorMa60 = Color(0xFF5FB3FF)
private val colorGrid = Color(0xFF1E2140)
private val colorText = Color(0xFF8A8FB5)

/**
 * 외부 라이브러리 없이 Jetpack Compose Canvas로만 그린 캔들차트.
 * - 캔들 + 위아래 수염
 * - 이동평균선 3개 (MA5 / MA20 / MA60)
 * - 하단 거래량 바
 * - 핀치 줌 / 드래그 스크롤
 */
@Composable
fun ComposeCandleChart(
    candles: List<CandleEntry>,
    modifier: Modifier = Modifier,
) {
    if (candles.isEmpty()) return

    // ── 줌/스크롤 상태
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    // ── 등장 애니메이션 (0→1)
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "chartAnim",
    )

    Canvas(
        modifier = modifier
            .pointerInput(candles) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    offsetX += pan.x
                }
            },
    ) {
        val candleAreaH  = size.height * 0.65f
        val gapH         = size.height * 0.04f
        val volumeAreaTop = candleAreaH + gapH
        val volumeAreaH  = size.height * 0.22f
        val labelAreaH   = size.height * 0.09f

        val baseSlotW    = size.width / candles.size.coerceAtLeast(1)
        val slotW        = baseSlotW * scale
        val bodyW        = (slotW * 0.55f).coerceAtLeast(2f)

        // 스크롤 클램핑
        val maxOffset = 0f
        val minOffset = -(slotW * candles.size - size.width).coerceAtLeast(0f)
        offsetX = offsetX.coerceIn(minOffset, maxOffset)

        fun xCenter(i: Int) = slotW * i + slotW / 2 + offsetX

        // 가격/볼륨 범위
        val maxPrice  = candles.maxOf { it.high }
        val minPrice  = candles.minOf { it.low }
        val priceSpan = (maxPrice - minPrice).coerceAtLeast(1f)
        val maxVol    = candles.maxOf { it.volume }.toFloat().coerceAtLeast(1f)

        fun priceY(p: Float) = candleAreaH * (1f - (p - minPrice) / priceSpan) * animProgress

        // ── 가격 그리드 & Y축 레이블
        val gridLines = 4
        for (i in 0..gridLines) {
            val price = minPrice + priceSpan * i / gridLines
            val y = priceY(price)
            drawLine(colorGrid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(
                "₩${"%,.0f".format(price)}",
                size.width - 4f,
                y - 6f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#8A8FB5")
                    textSize = 28f
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                },
            )
        }

        // ── 캔들 + 수염 + 거래량
        candles.forEachIndexed { i, c ->
            val x = xCenter(i)
            if (x < -slotW || x > size.width + slotW) return@forEachIndexed  // 화면 밖 스킵

            val isUp   = c.close >= c.open
            val color  = if (isUp) colorUp else colorDown

            val highY  = priceY(c.high)
            val lowY   = priceY(c.low)
            val openY  = priceY(c.open)
            val closeY = priceY(c.close)
            val bodyTop    = minOf(openY, closeY)
            val bodyBottom = maxOf(openY, closeY)
            val bodyH  = (bodyBottom - bodyTop).coerceAtLeast(2f)

            // 수염
            drawLine(color, Offset(x, highY), Offset(x, lowY), strokeWidth = 1.5f, cap = StrokeCap.Round)
            // 몸통
            drawRect(color, Offset(x - bodyW / 2, bodyTop), Size(bodyW, bodyH))

            // 거래량 바
            val volH = volumeAreaH * (c.volume / maxVol) * animProgress
            drawRect(
                color.copy(alpha = 0.45f),
                Offset(x - bodyW / 2, volumeAreaTop + volumeAreaH - volH),
                Size(bodyW, volH),
            )
        }

        // ── 이동평균선
        val closes = candles.map { it.close }
        fun drawMa(n: Int, color: Color) {
            val pts = closes.mapIndexedNotNull { i, _ ->
                if (i >= n - 1) {
                    val avg = closes.subList(i - n + 1, i + 1).average().toFloat()
                    Offset(xCenter(i), priceY(avg))
                } else null
            }
            for (j in 1 until pts.size) {
                drawLine(color, pts[j - 1], pts[j], strokeWidth = 1.8f, cap = StrokeCap.Round)
            }
        }
        drawMa(5,  colorMa5)
        drawMa(20, colorMa20)
        drawMa(60, colorMa60)

        // ── X축 레이블 (날짜)
        val step = (candles.size / 6).coerceAtLeast(1)
        candles.forEachIndexed { i, c ->
            if (i % step == 0) {
                val x = xCenter(i)
                if (x in 0f..size.width) {
                    drawContext.canvas.nativeCanvas.drawText(
                        c.label,
                        x,
                        size.height - 8f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#8A8FB5")
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        },
                    )
                }
            }
        }

        // ── 볼륨 구분선
        drawLine(
            colorGrid,
            Offset(0f, volumeAreaTop),
            Offset(size.width, volumeAreaTop),
            strokeWidth = 1f,
        )
    }
}
