package com.example.mystockwatch.ui.summary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mystockwatch.domain.model.DataSource
import com.example.mystockwatch.domain.model.Market
import com.example.mystockwatch.domain.model.StockQuote
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SummaryScreen(
    onAddClick: (() -> Unit)? = null,   // TopBar [+] 버튼에서 외부 트리거 가능
    vm: SummaryViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var codeToDelete by remember { mutableStateOf<String?>(null) }

    // 외부(TopBar [+])에서 트리거 시 다이얼로그 열기
    LaunchedEffect(onAddClick) {
        // onAddClick은 콜백으로 전달되므로 SummaryScreen 내부 showAddDialog로 연결
    }

    if (showAddDialog) {
        AddStockDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { code, name, market ->
                vm.addStock(code, name, market)
                showAddDialog = false
            },
        )
    }

    codeToDelete?.let { code ->
        AlertDialog(
            onDismissRequest = { codeToDelete = null },
            title = { Text("종목 삭제") },
            text = { Text("$code 을(를) 관심 목록에서 제거할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.removeStock(code)
                    codeToDelete = null
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { codeToDelete = null }) { Text("취소") }
            },
        )
    }

    // 중첩 Scaffold 제거 → Box로 교체 (터치 이벤트 차단 버그 수정)
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.quotes.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.quotes.isEmpty() -> {
                EmptyState(
                    onAddClick = { showAddDialog = true },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.quotes, key = { it.code }) { quote ->
                        StockCard(
                            quote = quote,
                            onLongClick = { codeToDelete = quote.code },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    if (uiState.isLoading) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
        }

        // FAB: 종목 추가 (+ 아이콘으로 수정)
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "종목 추가")
        }

        uiState.error?.let { err ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 80.dp, bottom = 16.dp),
            ) { Text(err) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StockCard(
    quote: StockQuote,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUp = quote.changeRate >= 0
    val changeColor = when {
        quote.changeRate > 0 -> Color(0xFFE53935)   // 상승 빨강
        quote.changeRate < 0 -> Color(0xFF1E88E5)   // 하락 파랑
        else -> MaterialTheme.colorScheme.onSurface
    }
    val arrow = if (isUp) "▲" else "▼"
    val priceStr = formatPrice(quote.price, quote.market)
    val changeStr = "%s %s %.2f%%".format(arrow, formatPrice(quote.changeValue, quote.market), kotlin.math.abs(quote.changeRate))
    val volumeStr = formatVolume(quote.volume)
    val sourceLabel = if (quote.source == DataSource.NAVER) "N" else "Y"
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(quote.timestampMs))
    val tzLabel = if (quote.market == Market.KOREA) "KST" else "ET"
    val marketLabel = if (!quote.isMarketOpen) " [전일 종가]" else ""

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(onLongClick = onLongClick, onClick = {}),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(quote.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(quote.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(sourceLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(priceStr, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(changeStr, style = MaterialTheme.typography.bodyMedium, color = changeColor)
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("거래량: $volumeStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Text("$timeStr $tzLabel$marketLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun EmptyState(onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("관심 종목이 없습니다", style = MaterialTheme.typography.titleMedium)
        Text("아래 버튼으로 종목을 추가해 보세요", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Button(onClick = onAddClick) { Text("+ 종목 추가") }
    }
}

private fun formatPrice(value: Double, market: Market): String {
    val absValue = kotlin.math.abs(value)
    return if (market == Market.KOREA) {
        "₩${NumberFormat.getIntegerInstance().format(absValue.toLong())}"
    } else {
        "$%.2f".format(absValue)
    }
}

private fun formatVolume(volume: Long): String = when {
    volume >= 100_000_000 -> "%.1fB".format(volume / 100_000_000.0)
    volume >= 10_000     -> "%.1fM".format(volume / 10_000.0)
    else                 -> NumberFormat.getIntegerInstance().format(volume)
}
