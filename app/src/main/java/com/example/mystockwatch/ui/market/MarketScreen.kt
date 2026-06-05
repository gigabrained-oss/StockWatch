package com.example.mystockwatch.ui.market

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val colorUp   = Color(0xFFE53935)
private val colorDown = Color(0xFF1E88E5)
private val colorFlat = Color(0xFF9E9E9E)

@Composable
fun MarketScreen(vm: MarketViewModel = viewModel()) {
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
            Text(
                "시장 현황",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = vm::refresh, enabled = !state.isLoading) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                }
            }
        }

        // ── 환율 카드
        SectionCard(title = "💱 환율") {
            if (state.exchangeRate != null) {
                val fx = state.exchangeRate!!
                ExchangeRateRow(fx)

                Spacer(Modifier.height(12.dp))

                // 기간 선택 칩
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FxChartPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = state.fxChartPeriod == period,
                            onClick  = { vm.selectFxChartPeriod(period) },
                            label    = { Text(period.label, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 라인차트
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isFxChartLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else if (state.fxChartPoints.isNotEmpty()) {
                        FxLineChart(
                            points = state.fxChartPoints,
                            isUp   = fx.isUp,
                            modifier = Modifier.fillMaxSize().padding(end = 72.dp, top = 4.dp, bottom = 4.dp),
                        )
                    } else {
                        Text(
                            "차트 데이터 없음",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }

                // 최고/최저 요약
                if (state.fxChartPoints.isNotEmpty()) {
                    val max = state.fxChartPoints.max()
                    val min = state.fxChartPoints.min()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "최저  ₩%,.2f".format(min),
                            style = MaterialTheme.typography.labelSmall,
                            color = colorDown,
                        )
                        Text(
                            "최고  ₩%,.2f".format(max),
                            style = MaterialTheme.typography.labelSmall,
                            color = colorUp,
                        )
                    }
                }
            } else if (!state.isLoading) {
                EmptyRow("환율 데이터를 불러오는 중...")
            }
        }

        // ── 글로벌 지수 카드
        SectionCard(title = "🌍 글로벌 지수") {
            if (state.indices.isNotEmpty()) {
                state.indices.forEachIndexed { idx, item ->
                    IndexRow(item)
                    if (idx < state.indices.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )
                    }
                }
            } else if (!state.isLoading) {
                EmptyRow("지수 데이터를 불러오는 중...")
            }
        }

        // 에러 카드
        state.error?.let { err ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "⚠️ $err",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ── 섹션 카드 컨테이너
@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            content()
        }
    }
}

// ── 환율 수치 행
@Composable
private fun ExchangeRateRow(item: MarketItem) {
    val changeColor = changeColor(item)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("미국 달러 / 한국 원", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "₩%,.2f".format(item.price),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(arrowIcon(item), contentDescription = null, tint = changeColor, modifier = Modifier.size(16.dp))
                Text(
                    "%+.2f  (%+.2f%%)".format(item.changeValue, item.changeRate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = changeColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ── 지수 행
@Composable
private fun IndexRow(item: MarketItem) {
    val changeColor = changeColor(item)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(formatIndexPrice(item), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(arrowIcon(item), contentDescription = null, tint = changeColor, modifier = Modifier.size(14.dp))
                Text("%+.2f%%".format(item.changeRate), style = MaterialTheme.typography.bodyMedium, color = changeColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmptyRow(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable private fun changeColor(item: MarketItem) = when {
    item.changeRate > 0 -> colorUp
    item.changeRate < 0 -> colorDown
    else                -> colorFlat
}

@Composable private fun arrowIcon(item: MarketItem) = when {
    item.changeRate > 0 -> Icons.Default.TrendingUp
    item.changeRate < 0 -> Icons.Default.TrendingDown
    else                -> Icons.Default.TrendingFlat
}

private fun formatIndexPrice(item: MarketItem) = when {
    item.symbol == "^KS11" -> "%,.2f".format(item.price)
    item.price >= 1_000    -> "%,.0f".format(item.price)
    else                   -> "%.2f".format(item.price)
}
