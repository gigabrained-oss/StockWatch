# StockWatch — Requirements (extracted from stockMonitor)

> Source: `~/stockMonitor` (Flask web app + PWA: `app.py`, `stock_monitor.py`, `db.py`, `static/js/app.js`, `templates/index.html`), as of commit `a499b2b` (2026-11-03).
> Goal: a **stand-alone Android app** (no server) that does everything stockMonitor does. Every network call, cache and computation that stockMonitor's Flask server did must now happen on the device.
> Requirement IDs are stable, so issues and commits can reference them (e.g. `WL-07`).

Priority: **P0** = needed for parity with daily stockMonitor use · **P1** = parity, less used · **P2** = nice-to-have / beyond stockMonitor.

---

## 1. Scope

| In stockMonitor (must port) | Not in stockMonitor (StockWatch extras, keep as P2) |
|---|---|
| Watchlist (관심종목) with live quotes, targets (목표가), gap %, sort, reorder | US individual stocks (AAPL…) |
| 요약 tab: big movers (stocks ±2 %, FX ±0.1 %) with mini charts | Technical indicators tab (RSI / MACD / Bollinger) |
| KOSPI / KOSDAQ tabs: index chart + market table | Candlestick chart + moving averages |
| 미국 시장 tab: DOW & NASDAQ chart, OHLC, 동향 뉴스 | Push notifications for alert conditions |
| USD/KRW tab (24h / 30d / 1y) + header badge | |
| ROI/HPR Sim tab (multi-ticker return comparison, up to 10 years) | |
| 종목 검색 with investment-info panel | |
| Local history cache (immutable closed sessions) | |

stockMonitor is **Korea-first**: tickers are 6-character KRX codes (`^[A-Za-z0-9]{6}$`, alphanumeric, because new ETF codes look like `0080G0`), and prices are in KRW.

---

## 2. Functional requirements

### 2.1 Global / shell

| ID | Pri | Requirement |
|---|---|---|
| GL-01 | P0 | Navigation contains: ⭐ 관심종목 (default), 📊 요약, KOSPI, KOSDAQ, 🇺🇸 미국 시장, USD/KRW, 💹 ROI/HPR Sim, 🔍 종목 검색. |
| GL-02 | P0 | The header always shows the **USD/KRW badge**: rate (2 decimals, `₩1,382.50`) + `▲/▼ change (±pct%)`, refreshed every **60 s**. Tapping it opens the USD/KRW tab. |
| GL-03 | P1 | The header shows a live clock (KST). |
| GL-04 | P0 | Color convention (Korean): **up = red**, **down = blue**, flat = gray. Arrows `▲ ▼ —`. |
| GL-05 | P0 | Number format: `ko-KR` thousands separators; `0`/null renders as `—`. Change format: `+1,200 (+1.23%)`. |
| GL-06 | P1 | When offline or a fetch fails, keep showing the last data, with a banner: "오프라인 상태입니다 — 마지막 캐시 데이터를 표시합니다" (watchlist: "⚠ 재연결 중..."). Never blank a screen that already had data. |
| GL-07 | P1 | Each panel shows "업데이트: HH:MM:SS · N초 자동갱신" and a manual ↻ 새로고침 button. |
| GL-08 | P0 | Auto-refresh runs only for the visible tab. Leaving a tab cancels its timer. |
| GL-09 | P1 | **Tap-to-mark line on every chart**: tapping inside the plot drops a dashed yellow (`#ffd33d`) horizontal line at that value, labeled with the value at the right edge. Tapping within ~4 px of the line removes it. One line per chart. |
| GL-10 | P1 | Dark theme (background `#0d1117`). |

### 2.2 ⭐ 관심종목 (Watchlist)

| ID | Pri | Requirement |
|---|---|---|
| WL-01 | P0 | An ordered list of favorites `{ticker, name}`, persisted on the device. Seed it from `stockMonitor/favorites.json` (import on first run, or from a bundled asset). |
| WL-02 | P0 | Stats bar: 종목 수 / 상승 ▲ / 하락 ▼ / 보합 — counts. |
| WL-03 | P0 | Per row: 종목명 (ticker), 시장 badge (KOSPI / KOSDAQ / ETF), 현재가, 전일종가 (= close − change), 고가 (red), 저가 (blue), 등락 (change + %), 거래량, 목표↑, 목표↓, 갭, delete ✕. |
| WL-04 | P0 | Refresh period = Naver's `pollingInterval`, clamped to **min 30 s**, and also **capped at 15 s** on the client (in effect 15 s while the screen is open). On failure, retry in 10 s. |
| WL-05 | P1 | Flash a row red or blue when its price changed since the previous refresh. |
| WL-06 | P0 | Tapping a column header sorts by it (asc/desc toggle). Sortable columns: name, market, close, prev close, high, low, change %, volume, target high, target low, gap. |
| WL-07 | P0 | Drag handle `⠿` to reorder; the order is persisted. A refresh must not overwrite an in-flight reorder. |
| WL-08 | P0 | **Targets (목표가)**: tap the 목표↑ or 목표↓ cell to edit it inline (numeric). Enter/blur = save, Esc/back = cancel; an empty or ≤0 value clears it. While typing, the target line on the open chart previews live. Auto-refresh pauses while editing. Seed targets from `stockMonitor/targets.json` (`{ticker: {high, low}}`, both nullable). |
| WL-09 | P0 | **Gap %**: if `high` is set and `high ≠ low`, then `(close − low) / (high − low) × 100`; otherwise `(close − low) / low × 100`. Requires `low`. Color: `< 0` blue, `≥ 100` red, otherwise gray. One decimal. |
| WL-10 | P0 | When `close ≥ target high`, show the stock name in red. |
| WL-11 | P0 | Tapping a row expands an inline chart (one open at a time), modes **오늘 (10분별) / 30일 / 1년**, default 오늘. The open chart and its mode survive auto-refresh. |
| WL-12 | P0 | Stock chart: line + fill, colored red if last ≥ first, otherwise blue. Dashed target lines (목표↑ red, 목표↓ blue) with a legend. The tooltip shows the price and its % vs the current (last) price. The 1년 mode marks the max (🔴최고) and min (🔵최저) points. The intraday title adds `· YYYY-MM-DD` when showing a previous session. |
| WL-13 | P1 | Restore the last-hovered tooltip point across a refresh. |
| WL-14 | P0 | Empty state: "관심 종목이 없습니다." plus a hint to add stocks from the KOSPI/KOSDAQ or 검색 tabs. |

### 2.3 📊 요약 (Summary / alerts)

| ID | Pri | Requirement |
|---|---|---|
| SU-01 | P0 | Show a card for **USD/KRW if |change %| ≥ 0.1** and for **each favorite with |change %| ≥ 2**. |
| SU-02 | P0 | Stock card: name (ticker), change, 현재 / 전일. FX card: change, 현재 ₩rate. |
| SU-03 | P0 | Each card has a mini chart with **24H / 30D / 1Y** toggles (24H default). Stocks: 24H = intraday. |
| SU-04 | P0 | Empty state: "✅ 현재 알림 없음 — USD/KRW ±0.1% 또는 관심종목 ±2% 이상 변동 시 표시됩니다." |
| SU-05 | P0 | Refresh every 60 s (30 s after an error). |
| SU-06 | P2 | Android extra: a background notification when a card would appear or a target is crossed (see AL-*). |

### 2.4 KOSPI / KOSDAQ

| ID | Pri | Requirement |
|---|---|---|
| MK-01 | P0 | Index chart with modes **오늘 / 30일 / 1년** (default 오늘). Intraday = 10-minute points 09:00–15:30 KST. If today has no session yet, show the latest finished session. |
| MK-02 | P0 | Market table: top **150 by market cap** (3 pages × 50), **sorted by |change %| desc** by default. Columns: 티커, 종목명, 현재가, 등락, 거래량, `+ 관심` button. |
| MK-03 | P0 | Sortable columns. Name/ticker sort ascending by default, numbers descending. |
| MK-04 | P0 | Tapping a row opens the same inline stock chart as WL-11. |
| MK-05 | P0 | Refresh every 60 s while the tab is visible. If a fetch fails, show the last good table. |

### 2.5 🇺🇸 미국 시장

| ID | Pri | Requirement |
|---|---|---|
| US-01 | P0 | Two sub-panels: **DOW Jones (`^DJI`)** and **NASDAQ Composite (`^IXIC`)**. |
| US-02 | P0 | Chart modes **당일** (5-minute intraday, last session, default) / **30일** (~22 daily bars). |
| US-03 | P0 | OHLC row: open / high / low / close / change / change %. |
| US-04 | P1 | **동향 뉴스**: 5 latest Korean headlines per index (title linked, source, KST time). See DS-09 for the query, filter and dedupe rules. Show "뉴스 없음" on failure. |

### 2.6 USD/KRW

| ID | Pri | Requirement |
|---|---|---|
| FX-01 | P0 | Ranges **24시간** (default) / **30일** / **1년**. |
| FX-02 | P0 | 24시간 = rolling 24 h of 1-minute data, thinned: last 30 min every point, 30–60 min every 5 min, older every 10 min. Labels `MM/DD HH:MM` **in KST** (a past bug showed UTC labels). |
| FX-03 | P0 | 24시간 stats line: 전일 종가 ₩x → 현재 ₩y \| ▲ change (pct). Prev close = the last point before the 24 h window. |
| FX-04 | P0 | Gold line (`#f0c040`), min/max points highlighted, y-axis padded (10 % of spread for 24 h, 2 % otherwise). |

### 2.7 💹 ROI/HPR Sim

| ID | Pri | Requirement |
|---|---|---|
| RO-01 | P0 | A separate persisted **Sim watchlist** (max **10** tickers) with a per-ticker `selected` flag. Tickers are added from search ("+ Sim 추가"). Adding a duplicate → toast "이미 추가된 종목입니다". |
| RO-02 | P0 | Ticker chips: tap to toggle selected, ✕ to remove. Each ticker has a fixed color in slot order: `#3987e5 #0d9488 #d95926 #199e70 #c98500 #d55181 #008300 #9085e9 #e66767 #be185d`. |
| RO-03 | P0 | Date-range **dual-handle slider** over the last **10 years** (365.25 d/yr). Default ≈ last 1 month. **Not** persisted. |
| RO-04 | P0 | Preset buttons: 10일, 30일, 60일, 90일, 6개월, 1년, 2년, 3년, 5년, 7년, 10년. The matching preset is highlighted. |
| RO-05 | P0 | Custom input: "최근 [N] 일 [적용]". |
| RO-06 | P1 | Slider UX: sensitivity scales with the window (finer when the window is short). A drag must not hijack vertical scrolling: touch needs ~8 px of movement before it locks direction, and a thumb grabs within 22 px. |
| RO-07 | P0 | Result table (all tickers, unselected ones dimmed): 종목, 시작가, 종료가, **HPR % = (end − start) / start × 100**. The start and end bars are the nearest trading day **at or before** the handle date (if none, the earliest bar). |
| RO-08 | P0 | Chart (selected tickers only): each series indexed to **100 at the start date**, a dashed gray **100 % baseline** (excluded from legend and tooltip), y-axis labels `NN%`, gaps spanned. |
| RO-09 | P0 | Load up to ~3660 days of daily bars per ticker (once, from cache — see CA-*). |

### 2.8 🔍 종목 검색 + 투자 정보

| ID | Pri | Requirement |
|---|---|---|
| SE-01 | P0 | Search by name substring or code (case-insensitive) across **all KOSPI + KOSDAQ + ETF** listings. Max 20 results. Enter submits. |
| SE-02 | P0 | Result row: 티커, 종목명, `+ 관심종목 추가`, `+ Sim 추가`. |
| SE-03 | P0 | Tapping a row expands a **투자 정보** panel (not a chart), one at a time: |
| | | • 기업개요 description (for an ETF, the API's `description`; for a stock, scraped `#summary_info p` paragraphs) |
| | | • `totalInfos` grid (key / value / valueDesc — 시가, 고가, 저가, 거래량, 시총, 52주 최고/최저, PER, PBR, EPS, BPS, 배당수익률, …) |
| | | • ETF only: 운용사, NAV, 총보수, 분배금수익률, 1개월 / 3개월 / 1년 수익률 |
| | | • 애널리스트 컨센서스: 투자의견 (1=매수 ~ 5=매도), 목표주가, date |
| | | • 최근 수급 동향 (last 5 days): 날짜, 종가, 외국인 / 기관 / 개인 net buy (signed and colored) |
| | | • 동일업종 비교 (up to 5 peers, excluding self): 종목명, 현재가, 등락률 |
| SE-04 | P0 | Adding a favorite → toast. Duplicates are rejected. |

### 2.9 Alerts (StockWatch extension — P2)

| ID | Pri | Requirement |
|---|---|---|
| AL-01 | P2 | A WorkManager job (≥ 15 min, **KRX session hours only**) checks favorites and sends a notification when a **target high/low is crossed** (reuses WL-08 data) or when SU-01's thresholds are hit. Notify once per condition per day. |
| AL-02 | P2 | Tapping the notification opens the app on that stock. |

---

## 3. Data sources (device-side)

All calls go directly from the phone. Send a browser `User-Agent` (`Mozilla/5.0 …`). Use a 10 s timeout with 2 retries. Prefer IPv4: stockMonitor saw 5 s DNS stalls on IPv6 for Naver and Yahoo.

| ID | Data | Endpoint | Notes |
|---|---|---|---|
| DS-01 | **Realtime quotes** (batch) | `https://polling.finance.naver.com/api/realtime?query=SERVICE_ITEM:{t1},{t2},…` | `result.areas[].datas[]`: `cd` ticker, `nm` name, **`nv` = current price**, **`sv` = previous close**, `ov / hv / lv` open / high / low, `tv` (or `aq`) volume. `change = nv − sv`, `pct = change / sv × 100`, `close = nv` or else `sv`. `result.pollingInterval` (ms). **Naver silently drops some ETFs from batch queries → re-fetch missing tickers one by one.** |
| DS-02 | Market cap ranking | `https://m.stock.naver.com/api/stocks/marketValue/{KOSPI\|KOSDAQ}?page=N&pageSize=50` (max 100) | `totalCount`, `stocks[]`: `itemCode`, `stockName`, `closePrice`, `compareToPreviousClosePrice`, `fluctuationsRatio`, `accumulatedTradingVolume`. The values are strings like `"1,089,000"` or `"+1.21"` → strip `,` and `+`. (The desktop HTML page was retired in Sep 2026.) |
| DS-03 | ETF master list | `https://finance.naver.com/api/sise/etfItemList.nhn` | `result.etfItemList[]`: `itemcode`, `itemname` (~1000 items). |
| DS-04 | Search universe | DS-03 + all pages of DS-02 for both markets (pageSize 100, ~45 requests) | Build it once and cache it for 10 min in memory / longer on disk. It also gives each ticker's market (KOSPI / KOSDAQ / ETF) for WL-03. (StockWatch's current `ac.stock.naver.com/ac` autocomplete is an acceptable faster alternative; it must still cover ETFs.) |
| DS-05 | Daily bars (stock **and** KOSPI/KOSDAQ index) | `https://fchart.stock.naver.com/sise.nhn?symbol={ticker\|KOSPI\|KOSDAQ}&timeframe=day&count={N}&requestType=0` | XML: `<item data="YYYYMMDD\|open\|high\|low\|close\|volume"/>`. Max ~3000 bars per response. Compute `change_pct` from the previous close. |
| DS-06 | Intraday stock | same as DS-05 with `timeframe=minute&count=400` | Spans ~7 sessions. Group by date; keep `HHMM % 10 == 0`, 09:00–15:30, dedupe. |
| DS-07 | Intraday index | `https://api.stock.naver.com/chart/domestic/index/{KOSPI\|KOSDAQ}/minute?startDateTime={YYYYMMDD}0000&endDateTime={YYYYMMDD}2359` (last 10 days) | JSON `[{localDateTime, currentPrice}]`. Same 10-minute sampling. Fallback 1: DS-06 with an index symbol (currently returns empty for indices). Fallback 2: scrape `finance.naver.com/sise/sise_index_time.naver?code=…&thistime=YYYYMMDDHHMM00`. |
| DS-08 | Investment info | `https://m.stock.naver.com/api/stock/{ticker}/integration` | `stockName`, `totalInfos`, `etfKeyIndicator`, `consensusInfo.{recommMean, priceTargetMean, createDate}`, `dealTrendInfos[].{bizdate, closePrice, foreignerPureBuyQuant, organPureBuyQuant, individualPureBuyQuant}`, `industryCompareInfo[]`, `description`. For a stock's 기업개요: HTML `https://finance.naver.com/item/main.naver?code={ticker}` → `#summary_info p` (needs Jsoup). |
| DS-09 | US index news | `https://news.google.com/rss/search?q={다우 뉴욕증시 \| 나스닥 뉴욕증시}&hl=ko&gl=KR&ceid=KR:ko` | Keep titles containing `다우` / `나스닥` (if none match, keep all). Strip a trailing ` - {source}`. Dedupe by normalized title (drop `[…]`/`(…)` tags, keep only `[0-9a-z가-힣]`, first 30 chars), preferring originators over aggregators (`v.daum.net`, `Nate News`, `news.nate.com`, `news.zum.com`). Newest first, 5 items. |
| DS-10 | US index daily / intraday | `https://query1.finance.yahoo.com/v8/finance/chart/{^DJI\|^IXIC}?interval=1d&range=1mo` / `?interval=5m&range=1d` | `chart.result[0].timestamp[]`, `indicators.quote[0].{open,high,low,close}`; skip nulls. Use only **v8 chart** — the v7 `quote` endpoint needs a crumb or cookie and fails. |
| DS-11 | USD/KRW current | Yahoo `v8/finance/chart/USDKRW=X?interval=1h&range=2d` | current = last non-null close; prev = last close dated before today (KST). Fallbacks: `https://open.er-api.com/v6/latest/USD` → `rates.KRW`, then `https://api.frankfurter.app/latest?from=USD&to=KRW`. |
| DS-12 | USD/KRW 24 h | Yahoo `USDKRW=X?interval=1m&range=5d` | See FX-02/03. |
| DS-13 | USD/KRW history | `https://api.frankfurter.app/{start}..{end}?from=USD&to=KRW` | `rates{date: {KRW}}`. |

---

## 4. Time & market-session rules

| ID | Requirement |
|---|---|
| TM-01 | Never use the device time zone. KR = `Asia/Seoul`, US = `America/New_York`. All labels are in KST unless they are US-market times. |
| TM-02 | KR regular session: Mon–Fri 09:00–15:30 KST. 15:30–18:00 = after-hours (prices still move slowly). US: Mon–Fri 09:30–16:00 ET. Holidays are not modelled; an empty or zero-volume response means closed. |
| TM-03 | **Poll period scaling** (stockMonitor `kr_poll_ttl` / `us_poll_ttl`): normal period in session; 300 s in KR after-hours; otherwise min(time until next open, **30 min**). |
| TM-04 | A session date is **final** (immutable) if it is before today, or if it is today and after the close (KR 15:30, US 16:00), or if today is a weekend. FX: only past dates are final. |

## 5. Local cache (port of `db.py`)

| ID | Requirement |
|---|---|
| CA-01 | A Room DB with versioned migrations (stockMonitor uses `PRAGMA user_version`). Tables: `daily_bars(source, symbol, date, close, change_pct, …)`, `intraday_bars(source, symbol, session_date, items, is_final)`, `symbol_coverage(source, symbol, oldest, newest, full_history)`, `roi_watchlist(ticker, name, selected, position)`, plus `favorites`, `targets`. Sources: `naver_stock`, `naver_index`, `yahoo_us_index`, `fx`. |
| CA-02 | **Read-through daily**: serve from the cache when it covers the request up to the expected latest session. Otherwise fetch only the missing **tail** and merge. Re-price the seam (recompute `change_pct` of the first new bar against the cached previous close). |
| CA-03 | If a live fetch returns less history than asked for, record `full_history = true` so the app doesn't re-fetch on every request (young listings). |
| CA-04 | Intraday: store finished sessions forever. Always fetch today's open session live. |
| CA-05 | Store dates internally as ISO `YYYY-MM-DD`. The display format is `YYYY.MM.DD` / `MM-DD`. |
| CA-06 | Keep the last good response per screen in memory (stale-while-error, GL-06). |

## 6. Non-functional

| ID | Requirement |
|---|---|
| NF-01 | Stand-alone: no backend. Works on mobile data. minSdk 26. |
| NF-02 | Cold start → watchlist with cached prices in < 1 s, live prices within ~3 s. |
| NF-03 | Network concurrency ≤ ~20 parallel requests (stockMonitor's thread-pool cap). Back off on HTTP 429. |
| NF-04 | Every unofficial endpoint sits behind a repository interface with a fallback chain (DS-07, DS-11) so a source can change without UI changes. Log failures in debug builds. |
| NF-05 | Unit tests for: realtime parsing (DS-01), gap % (WL-09), HPR / bar-at-or-before (RO-07), poll TTL (TM-03), session finality (TM-04), news dedupe (DS-09), fchart XML parsing (DS-05). |

---

## 7. Gaps found in current StockWatch code (2026-11-03)

Checked against the requirements above — likely causes of "doesn't work properly":

1. **Wrong price field** — `NaverFinanceApi.kt` maps `sv` to `price`. `sv` is the *previous close*; the current price is `nv` (DS-01). Also `cv`/`cr`/`rf` are not what stockMonitor relies on — compute change from `nv − sv`.
2. **Yahoo v7 `quote`** (`YahooFinanceApi.getQuote`) generally returns 401 without a crumb → use v8 `chart` only (DS-10).
3. **No ETF batch-drop handling** (DS-01) and **no session-aware polling** (TM-03).
4. Missing screens and features vs stockMonitor: targets + gap (WL-08..10), reorder, KOSPI/KOSDAQ market tables, US index news, ROI/HPR Sim, investment-info panel, history cache.
5. The tab set differs from the design doc (`StockWatch_설계서.md`): this document is the parity target. The design doc's indicators, candle and US-stock features are P2 extras.

## 8. Suggested build order

1. DS-01 fix + WL-01..07 (watchlist with correct prices, seeded from `favorites.json`).
2. WL-08..12 targets, gap, inline chart (DS-05/06) + CA cache.
3. GL-02 / FX-* (USD/KRW).
4. SU-* summary.
5. MK-* KOSPI/KOSDAQ.
6. SE-* search + info.
7. US-* US market.
8. RO-* ROI/HPR Sim.
9. AL-* notifications.
