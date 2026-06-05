package com.example.mystockwatch.ui.indicators

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mystockwatch.data.db.WatchlistEntity
import com.example.mystockwatch.domain.indicator.*

private val colorUp   = Color(0xFFE53935)
private val colorDown = Color(0xFF1E88E5)
private val colorWarn = Color(0xFFFFA726)
private val colorGood = Color(0xFF66BB6A)
private val colorNeutral = Color(0xFF78909C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorsScreen(vm: IndicatorsViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("기술지표", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = vm::refresh, enabled = !state.isLoading) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                }
            }
        }

        // ── 종목 선택 드롭다운
        StockSelector(
            watchlist = state.watchlist,
            selected  = state.selectedStock,
            onSelect  = vm::selectStock,
        )

        if (state.watchlist.isEmpty()) {
            EmptyWatchlist()
            return@Column
        }

        // ── 에러
        state.error?.let { err ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text("⚠️ $err", modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        // ── RSI 카드
        state.rsi?.let { rsi ->
            val (label, zone, desc) = rsiInterpretation(rsi)
            IndicatorCard(
                title    = "RSI (14)",
                subtitle = desc,
                badge    = label,
                badgeColor = when (zone) {
                    RsiZone.OVERBOUGHT -> colorWarn
                    RsiZone.OVERSOLD   -> colorGood
                    RsiZone.NEUTRAL    -> colorNeutral
                },
            ) {
                Spacer(Modifier.height(12.dp))
                RsiGauge(rsi = rsi.toFloat())
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("과매도 (30)", style = MaterialTheme.typography.labelSmall, color = colorGood)
                    Text("%.1f".format(rsi), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("과매수 (70)", style = MaterialTheme.typography.labelSmall, color = colorWarn)
                }
            }
        }

        // ── MACD 카드
        state.macd?.let { macd ->
            val (trend, trendDesc) = macdInterpretation(macd)
            val histColor = if (macd.histogram >= 0) colorUp else colorDown
            IndicatorCard(
                title    = "MACD (12·26·9)",
                subtitle = trendDesc,
                badge    = trend,
                badgeColor = if (macd.histogram >= 0) colorUp else colorDown,
            ) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    MacdValueItem("MACD",      macd.macdLine,   histColor)
                    VerticalDivider(modifier = Modifier.height(48.dp))
                    MacdValueItem("시그널",    macd.signalLine, colorNeutral)
                    VerticalDivider(modifier = Modifier.height(48.dp))
                    MacdValueItem("히스토그램", macd.histogram, histColor)
                }
            }
        }

        // ── 볼린저밴드 카드
        state.bollinger?.let { bb ->
            val (bbLabel, bbDesc) = bollingerInterpretation(bb)
            val bbColor = when {
                bb.percentB >= 80 -> colorWarn
                bb.percentB <= 20 -> colorGood
                else              -> colorNeutral
            }
            IndicatorCard(
                title    = "볼린저밴드 (20·2σ)",
                subtitle = bbDesc,
                badge    = bbLabel,
                badgeColor = bbColor,
            ) {
                Spacer(Modifier.height(12.dp))
                BollingerBar(percentB = bb.percentB.toFloat())
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    BbValueItem("하단", bb.lower)
                    BbValueItem("중간", bb.middle)
                    BbValueItem("상단", bb.upper)
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "밴드폭  ${"%.1f".format(bb.bandwidth)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        "%B  ${"%.1f".format(bb.percentB)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }

        // 로딩 중이고 데이터 아직 없을 때
        if (state.isLoading && state.rsi == null) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ─────────────────────────────────────────────────────────────
//  종목 선택 드롭다운
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockSelector(
    watchlist: List<WatchlistEntity>,
    selected: WatchlistEntity?,
    onSelect: (WatchlistEntity) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (watchlist.isNotEmpty()) expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected?.let { "${it.name}  (${it.code})" } ?: "종목을 선택하세요",
            onValueChange = {},
            readOnly = true,
            label = { Text("분석 종목") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            watchlist.forEach { stock ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(stock.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${stock.code}  ·  ${if (stock.market == "KOREA") "국내" else "미국"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    },
                    onClick = { onSelect(stock); expanded = false },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  지표 카드 공통 컨테이너
// ─────────────────────────────────────────────────────────────
@Composable
private fun IndicatorCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        badge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  RSI 게이지 바 (Canvas)
// ─────────────────────────────────────────────────────────────
@Composable
private fun RsiGauge(rsi: Float) {
    val animatedRsi by animateFloatAsState(
        targetValue = rsi,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "rsiAnim",
    )
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
        val trackH = 10f
        val trackY = size.height / 2 - trackH / 2
        val radius = trackH / 2

        // 배경 그라디언트 트랙
        drawRoundRect(
            brush = Brush.horizontalGradient(
                0f   to colorGood,
                0.3f to colorGood,
                0.5f to colorNeutral,
                0.7f to colorWarn,
                1f   to colorWarn,
            ),
            topLeft = Offset(0f, trackY),
            size    = Size(size.width, trackH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
            alpha = 0.3f,
        )

        // 30 / 70 구분선
        listOf(0.3f, 0.7f).forEach { pos ->
            drawLine(
                color = surfaceColor,
                start = Offset(size.width * pos, trackY),
                end   = Offset(size.width * pos, trackY + trackH),
                strokeWidth = 2f,
            )
        }

        // 현재 RSI 마커
        val markerX = size.width * (animatedRsi / 100f)
        val markerColor = when {
            animatedRsi >= 70 -> colorWarn
            animatedRsi <= 30 -> colorGood
            else              -> colorNeutral
        }
        drawCircle(color = markerColor, radius = 9f, center = Offset(markerX, size.height / 2))
        drawCircle(color = Color.White, radius = 5f, center = Offset(markerX, size.height / 2))
    }
}

// ─────────────────────────────────────────────────────────────
//  볼린저밴드 위치 바 (Canvas)
// ─────────────────────────────────────────────────────────────
@Composable
private fun BollingerBar(percentB: Float) {
    val clamped = percentB.coerceIn(0f, 100f)
    val animatedPct by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "bbAnim",
    )

    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
        val trackH = 10f
        val trackY = size.height / 2 - trackH / 2
        val radius = trackH / 2

        drawRoundRect(
            color      = surfaceVariantColor,
            topLeft    = Offset(0f, trackY),
            size       = Size(size.width, trackH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
        )

        // 중간 밴드 (50%) 표시
        drawLine(
            color = outlineColor.copy(alpha = 0.5f),
            start = Offset(size.width * 0.5f, trackY),
            end   = Offset(size.width * 0.5f, trackY + trackH),
            strokeWidth = 2f,
        )

        val markerX = size.width * (animatedPct / 100f)
        val markerColor = when {
            animatedPct >= 80 -> colorWarn
            animatedPct <= 20 -> colorGood
            else              -> colorNeutral
        }
        drawCircle(color = markerColor, radius = 9f, center = Offset(markerX, size.height / 2))
        drawCircle(color = Color.White, radius = 5f, center = Offset(markerX, size.height / 2))

        // 하단(0%)·상단(100%) 레이블 영역
        drawLine(
            color = colorGood.copy(alpha = 0.6f),
            start = Offset(0f, trackY), end = Offset(0f, trackY + trackH), strokeWidth = 3f,
        )
        drawLine(
            color = colorWarn.copy(alpha = 0.6f),
            start = Offset(size.width, trackY), end = Offset(size.width, trackY + trackH), strokeWidth = 3f,
        )
    }
}

// ── 작은 헬퍼 컴포저블 ──────────────────────────────────────────

@Composable
private fun MacdValueItem(label: String, value: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(4.dp))
        Text(
            "%+.4f".format(value),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun BbValueItem(label: String, value: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(2.dp))
        Text("%,.0f".format(value), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyWatchlist() {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            "요약 탭에서 종목을 먼저 추가해주세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
