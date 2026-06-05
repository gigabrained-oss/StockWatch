package com.example.mystockwatch.ui.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mystockwatch.data.db.AlertEntity
import com.example.mystockwatch.data.db.AlertType
import com.example.mystockwatch.data.db.WatchlistEntity

@Composable
fun AlertsScreen(vm: AlertsViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddAlertDialog(
            watchlist = state.watchlist,
            onDismiss = { showAddDialog = false },
            onConfirm = { stock, type, threshold ->
                vm.addAlert(stock, type, threshold)
                showAddDialog = false
            },
        )
    }

    Scaffold(
        floatingActionButton = {
            if (state.watchlist.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("알림 추가") },
                )
            }
        },
    ) { innerPadding ->
        if (state.watchlist.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "요약 탭에서 종목을 먼저 추가해주세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            return@Scaffold
        }

        if (state.alertsByStock.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("설정된 알림이 없습니다", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "+ 알림 추가 버튼으로 조건을 등록하세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 종목 그룹별로 표시
            state.watchlist.forEach { stock ->
                val alerts = state.alertsByStock[stock.code].orEmpty()
                if (alerts.isEmpty()) return@forEach

                item(key = "header_${stock.code}") {
                    Text(
                        "${stock.name}  (${stock.code})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                    )
                }
                items(alerts, key = { it.id }) { alert ->
                    AlertRow(
                        alert    = alert,
                        onToggle = { vm.toggleAlert(alert.id, it) },
                        onDelete = { vm.deleteAlert(alert.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── 알림 조건 행
@Composable
private fun AlertRow(
    alert: AlertEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = runCatching { AlertType.valueOf(alert.type) }.getOrNull()

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                checked = alert.isEnabled,
                onCheckedChange = onToggle,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    typeLabel(type, alert.threshold, alert.market),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    typeDesc(type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// ── 알림 추가 다이얼로그
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAlertDialog(
    watchlist: List<WatchlistEntity>,
    onDismiss: () -> Unit,
    onConfirm: (WatchlistEntity, AlertType, Double) -> Unit,
) {
    var selectedStock by remember { mutableStateOf(watchlist.first()) }
    var selectedType  by remember { mutableStateOf(AlertType.PRICE_ABOVE) }
    var threshold     by remember { mutableStateOf("") }
    var stockExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("알림 조건 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // 종목 선택
                ExposedDropdownMenuBox(
                    expanded = stockExpanded,
                    onExpandedChange = { stockExpanded = it },
                ) {
                    OutlinedTextField(
                        value = "${selectedStock.name} (${selectedStock.code})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("종목") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(stockExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = stockExpanded, onDismissRequest = { stockExpanded = false }) {
                        watchlist.forEach { stock ->
                            DropdownMenuItem(
                                text = { Text("${stock.name} (${stock.code})") },
                                onClick = { selectedStock = stock; stockExpanded = false },
                            )
                        }
                    }
                }

                // 조건 유형 선택
                Text("조건 유형", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        AlertType.PRICE_ABOVE  to "현재가 ≥ X원 (목표가 도달)",
                        AlertType.PRICE_BELOW  to "현재가 ≤ X원 (손절가 도달)",
                        AlertType.CHANGE_ABOVE to "등락률 ≥ +X% (급등)",
                        AlertType.CHANGE_BELOW to "등락률 ≤ −X% (급락)",
                    ).forEach { (type, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick  = { selectedType = type },
                            )
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // 임계값 입력
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    label = {
                        Text(if (selectedType == AlertType.PRICE_ABOVE || selectedType == AlertType.PRICE_BELOW)
                            "가격 (원 또는 달러)" else "등락률 (%)")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = threshold.toDoubleOrNull() ?: return@TextButton
                    onConfirm(selectedStock, selectedType, value)
                },
                enabled = threshold.toDoubleOrNull() != null,
            ) { Text("추가") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

// ── 헬퍼
private fun typeLabel(type: AlertType?, threshold: Double, market: String): String {
    val currency = if (market == "KOREA") "₩" else "$"
    return when (type) {
        AlertType.PRICE_ABOVE  -> "현재가 ≥ $currency%,.0f".format(threshold)
        AlertType.PRICE_BELOW  -> "현재가 ≤ $currency%,.0f".format(threshold)
        AlertType.CHANGE_ABOVE -> "등락률 ≥ +%.1f%%".format(threshold)
        AlertType.CHANGE_BELOW -> "등락률 ≤ −%.1f%%".format(threshold)
        null                   -> "알 수 없는 조건"
    }
}

private fun typeDesc(type: AlertType?): String = when (type) {
    AlertType.PRICE_ABOVE  -> "목표가 도달 시 알림"
    AlertType.PRICE_BELOW  -> "손절가 도달 시 알림"
    AlertType.CHANGE_ABOVE -> "당일 급등 시 알림"
    AlertType.CHANGE_BELOW -> "당일 급락 시 알림"
    null                   -> ""
}
