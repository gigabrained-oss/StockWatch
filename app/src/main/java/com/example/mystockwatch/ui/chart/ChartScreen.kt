package com.example.mystockwatch.ui.chart

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mystockwatch.domain.model.ChartLibrary
import com.example.mystockwatch.ui.chart.ComposeCandleChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(vm: ChartViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {

        // ── 라이브러리 전환 바 ─────────────────────────────────
        LibrarySelectorBar(
            selected = state.chartLibrary,
            onSelect = vm::selectChartLibrary,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── 기간 선택 칩 ──────────────────────────────────────
        PeriodChipRow(
            selected = state.selectedPeriod,
            onSelect = vm::selectPeriod,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // ── 차트 영역 ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                AnimatedContent(
                    targetState = state.chartLibrary,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "chartLibrarySwitch",
                ) { lib ->
                    when (lib) {
                        ChartLibrary.COMPOSE -> ComposeCandleChart(
                            candles = state.candles,
                            modifier = Modifier.fillMaxSize(),
                        )
                        ChartLibrary.MP -> MpCandleChart(
                            candles = state.candles,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        // ── 범례 ──────────────────────────────────────────────
        LegendRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

// ─────────────────────────────────────────────────────────────
//  라이브러리 전환 바  (SegmentedButton 스타일)
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibrarySelectorBar(
    selected: ChartLibrary,
    onSelect: (ChartLibrary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "차트 엔진",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(1f),
        )
        SingleChoiceSegmentedButtonRow {
            ChartLibrary.entries.forEachIndexed { index, lib ->
                SegmentedButton(
                    selected = selected == lib,
                    onClick = { onSelect(lib) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ChartLibrary.entries.size,
                    ),
                    icon = {
                        SegmentedButtonDefaults.ActiveIcon()
                        if (selected != lib) {
                            Icon(
                                imageVector = if (lib == ChartLibrary.COMPOSE) Icons.AutoMirrored.Filled.ShowChart
                                              else Icons.Default.BarChart,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                            )
                        }
                    },
                    label = {
                        Text(lib.label, style = MaterialTheme.typography.labelMedium)
                    },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  기간 선택 칩
// ─────────────────────────────────────────────────────────────
@Composable
private fun PeriodChipRow(
    selected: Period,
    onSelect: (Period) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(Period.entries) { period ->
            FilterChip(
                selected = selected == period,
                onClick = { onSelect(period) },
                label = { Text(period.label, style = MaterialTheme.typography.labelMedium) },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  범례
// ─────────────────────────────────────────────────────────────
@Composable
private fun LegendRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LegendItem("MA5",  0xFFFF6B6B)
        LegendItem("MA20", 0xFFFFD700)
        LegendItem("MA60", 0xFF5FB3FF)
        Spacer(Modifier.weight(1f))
        LegendItem("▲ 상승", 0xFFE53935)
        LegendItem("▼ 하락", 0xFF1E88E5)
    }
}

@Composable
private fun LegendItem(label: String, colorLong: Long) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            modifier = Modifier.size(width = 16.dp, height = 3.dp),
            color = androidx.compose.ui.graphics.Color(colorLong),
            shape = MaterialTheme.shapes.extraSmall,
        ) {}
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}
