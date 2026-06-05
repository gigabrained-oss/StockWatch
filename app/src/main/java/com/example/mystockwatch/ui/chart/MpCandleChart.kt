package com.example.mystockwatch.ui.chart

import android.graphics.Color
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@Composable
fun MpCandleChart(
    candles: List<CandleEntry>,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            CombinedChart(ctx).apply {
                description.isEnabled = false
                setBackgroundColor(Color.TRANSPARENT)
                setDrawGridBackground(false)
                setDrawBorders(false)

                // X축
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = Color.parseColor("#8A8FB5")
                    gridColor = Color.parseColor("#1E2140")
                    setDrawAxisLine(false)
                    granularity = 1f
                    labelRotationAngle = -30f
                }
                // Y축 (오른쪽)
                axisRight.apply {
                    textColor = Color.parseColor("#8A8FB5")
                    gridColor = Color.parseColor("#1E2140")
                    setDrawAxisLine(false)
                }
                axisLeft.isEnabled = false

                legend.apply {
                    textColor = Color.parseColor("#8A8FB5")
                    form = Legend.LegendForm.LINE
                }

                isDoubleTapToZoomEnabled = true
                setPinchZoom(true)
            }
        },
        update = { chart ->
            if (candles.isEmpty()) return@AndroidView

            val labels = candles.map { it.label }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

            // ── 캔들 데이터
            val candleEntries = candles.map { c ->
                com.github.mikephil.charting.data.CandleEntry(
                    c.index.toFloat(), c.high, c.low, c.open, c.close
                )
            }
            val candleDataSet = CandleDataSet(candleEntries, "").apply {
                shadowColor = Color.parseColor("#888888")
                shadowWidth = 0.8f
                decreasingColor = Color.parseColor("#1E88E5")   // 하락 파랑
                decreasingPaintStyle = Paint.Style.FILL
                increasingColor = Color.parseColor("#E53935")   // 상승 빨강
                increasingPaintStyle = Paint.Style.FILL
                neutralColor = Color.GRAY
                setDrawValues(false)
            }

            // ── 이동평균선
            fun ma(n: Int, color: Int, label: String): LineDataSet {
                val closes = candles.map { it.close }
                val entries = closes.mapIndexedNotNull { i, _ ->
                    if (i >= n - 1) {
                        val avg = closes.subList(i - n + 1, i + 1).average().toFloat()
                        Entry(i.toFloat(), avg)
                    } else null
                }
                return LineDataSet(entries, label).apply {
                    this.color = color
                    lineWidth = 1.5f
                    setDrawCircles(false)
                    setDrawValues(false)
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                }
            }

            val ma5  = ma(5,  Color.parseColor("#FF6B6B"), "MA5")
            val ma20 = ma(20, Color.parseColor("#FFD700"), "MA20")
            val ma60 = ma(60, Color.parseColor("#5FB3FF"), "MA60")

            // ── 거래량 바
            val barEntries = candles.map { c ->
                BarEntry(c.index.toFloat(), c.volume.toFloat())
            }
            val barDataSet = BarDataSet(barEntries, "거래량").apply {
                colors = candles.map { c ->
                    if (c.close >= c.open) Color.parseColor("#4DE53935") else Color.parseColor("#4D1E88E5")
                }
                setDrawValues(false)
                axisDependency = com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT
            }
            chart.axisLeft.apply {
                isEnabled = true
                textColor = Color.TRANSPARENT
                gridColor = Color.TRANSPARENT
                setDrawAxisLine(false)
                axisMaximum = candles.maxOf { it.volume }.toFloat() * 5f  // 거래량을 차트 하단 1/5에 표시
            }

            chart.data = CombinedData().apply {
                setData(CandleData(candleDataSet))
                setData(LineData(ma5, ma20, ma60))
                setData(BarData(barDataSet))
            }
            chart.invalidate()
        }
    )
}
