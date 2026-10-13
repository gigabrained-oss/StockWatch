package com.example.mystockwatch.data.repository

import com.example.mystockwatch.data.api.NaverFinanceApi
import com.example.mystockwatch.data.api.StockSearchApi
import com.example.mystockwatch.data.api.YahooFinanceApi
import com.example.mystockwatch.data.db.WatchlistDao
import com.example.mystockwatch.data.db.WatchlistEntity
import com.example.mystockwatch.domain.model.DataSource
import com.example.mystockwatch.domain.model.Market
import com.example.mystockwatch.domain.model.StockQuote
import com.example.mystockwatch.domain.model.StockSearchResult
import kotlinx.coroutines.flow.Flow

class StockRepository(
    private val dao: WatchlistDao,
    private val naverApi: NaverFinanceApi,
    private val yahooApi: YahooFinanceApi,
    private val searchApi: StockSearchApi,
) {
    fun observeWatchlist(): Flow<List<WatchlistEntity>> = dao.observeAll()

    suspend fun addStock(code: String, name: String, market: Market) {
        dao.insert(WatchlistEntity(code = code, name = name, market = market.name))
    }

    suspend fun removeStock(code: String) = dao.delete(code)

    /** Search by name or code (Korean or English) across domestic and US listings. */
    suspend fun searchStocks(query: String): List<StockSearchResult> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val items = searchApi.search(trimmed).items
        return items.mapNotNull { item ->
            val market = when (item.nationCode) {
                "KOR" -> Market.KOREA
                "USA" -> Market.USA
                else -> return@mapNotNull null
            }
            StockSearchResult(code = item.code, name = item.name, market = market, exchangeLabel = item.typeName)
        }
    }

    suspend fun fetchQuote(entity: WatchlistEntity): StockQuote {
        val market = Market.valueOf(entity.market)
        return when (market) {
            Market.KOREA -> fetchKorean(entity)
            Market.USA   -> fetchYahoo(entity.code, entity.name, market)
        }
    }

    private suspend fun fetchKorean(entity: WatchlistEntity): StockQuote {
        return try {
            val response = naverApi.getQuote("SERVICE_ITEM:${entity.code}")
            val data = response.result?.areas?.firstOrNull()?.datas?.firstOrNull()
                ?: error("Naver: empty result")
            StockQuote(
                code = entity.code,
                name = data.name,
                market = Market.KOREA,
                price = data.price,
                changeValue = if (data.riseFlag == "2") -data.changeValue else data.changeValue,
                changeRate = if (data.riseFlag == "2") -data.changeRate else data.changeRate,
                volume = data.volume,
                source = DataSource.NAVER,
                timestampMs = data.timestamp * 1000,
                isMarketOpen = true,
            )
        } catch (e: Exception) {
            // Naver 실패 시 Yahoo 폴백 (코스피: .KS, 코스닥: .KQ)
            val yahooCode = "${entity.code}.KS"
            fetchYahoo(yahooCode, entity.name, Market.KOREA)
        }
    }

    private suspend fun fetchYahoo(code: String, fallbackName: String, market: Market): StockQuote {
        val response = yahooApi.getQuote(code)
        val data = response.quoteResponse?.result?.firstOrNull()
            ?: error("Yahoo: empty result for $code")
        return StockQuote(
            code = code,
            name = data.longName ?: data.shortName ?: fallbackName,
            market = market,
            price = data.price,
            changeValue = data.changeValue,
            changeRate = data.changeRate,
            volume = data.volume,
            source = DataSource.YAHOO,
            timestampMs = data.timestamp * 1000L,
            isMarketOpen = data.marketState == "REGULAR",
        )
    }
}
