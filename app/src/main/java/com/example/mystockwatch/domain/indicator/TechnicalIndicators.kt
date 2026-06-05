package com.example.mystockwatch.domain.indicator

import kotlin.math.pow
import kotlin.math.sqrt

// ── RSI ────────────────────────────────────────────────────────────────────

/** RSI(14) 계산. closes는 오래된 것부터 최신 순서 */
fun calculateRsi(closes: List<Double>, period: Int = 14): Double {
    if (closes.size < period + 1) return 50.0
    val changes = closes.zipWithNext { a, b -> b - a }
    val gains = changes.map { if (it > 0) it else 0.0 }
    val losses = changes.map { if (it < 0) -it else 0.0 }

    // 초기 평균 (단순)
    var avgGain = gains.take(period).average()
    var avgLoss = losses.take(period).average()

    // Wilder smoothing
    for (i in period until changes.size) {
        avgGain = (avgGain * (period - 1) + gains[i]) / period
        avgLoss = (avgLoss * (period - 1) + losses[i]) / period
    }
    if (avgLoss == 0.0) return 100.0
    val rs = avgGain / avgLoss
    return 100.0 - (100.0 / (1.0 + rs))
}

fun rsiInterpretation(rsi: Double): Triple<String, RsiZone, String> {
    return when {
        rsi >= 70 -> Triple("과매수", RsiZone.OVERBOUGHT, "⚠️ 매도 고려 구간")
        rsi <= 30 -> Triple("과매도", RsiZone.OVERSOLD,   "💡 매수 고려 구간")
        rsi >= 60 -> Triple("강세",   RsiZone.NEUTRAL,    "상승 압력 우세")
        rsi <= 40 -> Triple("약세",   RsiZone.NEUTRAL,    "하락 압력 우세")
        else      -> Triple("중립",   RsiZone.NEUTRAL,    "방향성 탐색 중")
    }
}

enum class RsiZone { OVERBOUGHT, NEUTRAL, OVERSOLD }

// ── MACD ───────────────────────────────────────────────────────────────────

data class MacdResult(
    val macdLine: Double,
    val signalLine: Double,
    val histogram: Double,
)

fun calculateMacd(
    closes: List<Double>,
    fastPeriod: Int = 12,
    slowPeriod: Int = 26,
    signalPeriod: Int = 9,
): MacdResult? {
    if (closes.size < slowPeriod + signalPeriod) return null

    val ema12 = emaList(closes, fastPeriod)
    val ema26 = emaList(closes, slowPeriod)

    // MACD 라인: EMA12 - EMA26 (공통 인덱스 기준)
    val offset = slowPeriod - fastPeriod
    val macdLine = ema12.drop(offset).zip(ema26) { a, b -> a - b }

    if (macdLine.size < signalPeriod) return null

    val signalLine = emaList(macdLine, signalPeriod).last()
    val lastMacd   = macdLine.last()
    return MacdResult(lastMacd, signalLine, lastMacd - signalLine)
}

fun macdInterpretation(result: MacdResult): Pair<String, String> {
    return when {
        result.histogram > 0 && result.macdLine > 0 -> "강한 상승 추세 ▲" to "MACD·히스토그램 모두 양수"
        result.histogram > 0                         -> "상승 추세 ▲"     to "히스토그램 확대 중"
        result.histogram < 0 && result.macdLine < 0  -> "강한 하락 추세 ▼" to "MACD·히스토그램 모두 음수"
        result.histogram < 0                         -> "하락 추세 ▼"     to "히스토그램 축소 중"
        else                                         -> "보합 ─"          to "추세 전환 탐색 중"
    }
}

// ── 볼린저밴드 ──────────────────────────────────────────────────────────────

data class BollingerResult(
    val upper: Double,
    val middle: Double,
    val lower: Double,
    val currentPrice: Double,
    val bandwidth: Double,     // (upper-lower)/middle * 100
    val percentB: Double,      // (price-lower)/(upper-lower) * 100
)

fun calculateBollinger(closes: List<Double>, period: Int = 20, multiplier: Double = 2.0): BollingerResult? {
    if (closes.size < period) return null
    val slice  = closes.takeLast(period)
    val middle = slice.average()
    val stdDev = sqrt(slice.sumOf { (it - middle).pow(2) } / period)
    val upper  = middle + multiplier * stdDev
    val lower  = middle - multiplier * stdDev
    val price  = closes.last()
    val bw     = if (middle != 0.0) (upper - lower) / middle * 100 else 0.0
    val pctB   = if (upper != lower) (price - lower) / (upper - lower) * 100 else 50.0
    return BollingerResult(upper, middle, lower, price, bw, pctB)
}

fun bollingerInterpretation(result: BollingerResult): Pair<String, String> {
    return when {
        result.percentB >= 95 -> "상단 밴드 돌파 ⚠️"  to "강한 과매수 — 조정 가능성"
        result.percentB >= 80 -> "상단 밴드 근접 ⚠️"  to "과매수 구간 진입"
        result.percentB <= 5  -> "하단 밴드 이탈 💡"  to "강한 과매도 — 반등 가능성"
        result.percentB <= 20 -> "하단 밴드 근접 💡"  to "과매도 구간 진입"
        result.percentB in 45.0..55.0 -> "중간 밴드 근처" to "추세 중립"
        else                  -> "밴드 중간"          to "정상 변동 구간"
    }
}

// ── 공통 유틸 ──────────────────────────────────────────────────────────────

/** EMA 시리즈 반환 (period 이후부터) */
private fun emaList(data: List<Double>, period: Int): List<Double> {
    if (data.size < period) return emptyList()
    val k = 2.0 / (period + 1)
    val result = mutableListOf(data.take(period).average())
    for (i in period until data.size) {
        result.add(data[i] * k + result.last() * (1 - k))
    }
    return result
}
