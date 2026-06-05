package com.example.mystockwatch.ui.summary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.mystockwatch.domain.model.Market

@Composable
fun AddStockDialog(
    onDismiss: () -> Unit,
    onConfirm: (code: String, name: String, market: Market) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var market by remember { mutableStateOf(Market.KOREA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("종목 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("종목 코드") },
                    placeholder = { Text("예: 005930 / AAPL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("종목명") },
                    placeholder = { Text("예: 삼성전자") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("시장 선택", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = market == Market.KOREA,
                        onClick = { market = Market.KOREA },
                    )
                    Text("국내 (KOSPI / KOSDAQ)", modifier = Modifier.padding(end = 16.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = market == Market.USA,
                        onClick = { market = Market.USA },
                    )
                    Text("미국 (NYSE / NASDAQ)")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(code, name, market) },
                enabled = code.isNotBlank() && name.isNotBlank(),
            ) { Text("추가") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}
