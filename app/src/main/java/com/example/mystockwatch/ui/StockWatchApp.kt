package com.example.mystockwatch.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mystockwatch.ui.alerts.AlertsScreen
import com.example.mystockwatch.ui.chart.ChartScreen
import com.example.mystockwatch.ui.indicators.IndicatorsScreen
import com.example.mystockwatch.ui.market.MarketScreen
import com.example.mystockwatch.ui.summary.SummaryScreen

sealed class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    object Summary    : Tab("summary",    "요약", Icons.Default.Home)
    object Chart      : Tab("chart",      "차트", Icons.AutoMirrored.Filled.ShowChart)
    object Indicators : Tab("indicators", "지표", Icons.Default.BarChart)
    object Market     : Tab("market",     "환율", Icons.Default.CurrencyExchange)
    object Alerts     : Tab("alerts",     "알림", Icons.Default.NotificationsNone)
}

private val TABS = listOf(
    Tab.Summary,
    Tab.Chart,
    Tab.Indicators,
    Tab.Market,
    Tab.Alerts,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockWatchApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StockWatch") },
                actions = {
                    IconButton(onClick = { /* TODO: 종목 추가 다이얼로그 */ }) {
                        Icon(Icons.Default.Add, contentDescription = "종목 추가")
                    }
                    IconButton(onClick = { /* TODO: 설정 메뉴 */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "메뉴")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(imageVector = tab.icon, contentDescription = tab.label)
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Summary.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Tab.Summary.route)    { SummaryScreen() }
            composable(Tab.Chart.route)      { ChartScreen() }
            composable(Tab.Indicators.route) { IndicatorsScreen() }
            composable(Tab.Market.route)     { MarketScreen() }
            composable(Tab.Alerts.route)     { AlertsScreen() }
        }
    }
}
