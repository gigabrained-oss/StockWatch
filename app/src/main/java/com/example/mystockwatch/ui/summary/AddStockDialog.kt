package com.example.mystockwatch.ui.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mystockwatch.domain.model.Market
import com.example.mystockwatch.domain.model.StockSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AddStockDialog(
    onDismiss: () -> Unit,
    onSearch: suspend (String) -> List<StockSearchResult>,
    onConfirm: (code: String, name: String, market: Market) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<StockSearchResult?>(null) }
    var results by remember { mutableStateOf<List<StockSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun onQueryChange(newQuery: String) {
        query = newQuery
        selected = null
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            results = emptyList()
            isSearching = false
            return
        }
        searchJob = scope.launch {
            delay(300)
            isSearching = true
            results = runCatching { onSearch(newQuery) }.getOrDefault(emptyList())
            isSearching = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("종목 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = ::onQueryChange,
                    label = { Text("종목명 또는 코드") },
                    placeholder = { Text("예: 삼성전자, 005930, AAPL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                when {
                    selected != null -> {
                        val s = selected!!
                        Text(
                            "선택됨: ${s.name} (${s.code}) · ${s.exchangeLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    isSearching -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    query.isNotBlank() && results.isEmpty() -> {
                        Text(
                            "검색 결과 없음",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    results.isNotEmpty() -> {
                        LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                            items(results, key = { it.code }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            searchJob?.cancel()
                                            selected = item
                                            query = item.name
                                            results = emptyList()
                                            isSearching = false
                                        }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            item.exchangeLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                    Text(
                                        item.code,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selected?.let { onConfirm(it.code, it.name, it.market) } },
                enabled = selected != null,
            ) { Text("추가") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}
