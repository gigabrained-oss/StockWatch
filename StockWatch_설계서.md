# StockWatch — Android 앱 최종 설계서

> 작성일: 2026-05-31  
> 상태: 설계 확정 (구현 전)

---

## 1. 앱 개요

관심 주식(국내/미국)의 가격 추이와 투자 참고 지표를 한눈에 확인하고, 조건 충족 시 알림을 받는 Android 주식 모니터링 앱.

---

## 2. 기술 스택

| 항목 | 선택 |
|------|------|
| 언어 | Kotlin |
| UI | Jetpack Compose |
| 아키텍처 | MVVM (ViewModel + StateFlow) |
| 네트워크 | Retrofit + OkHttp |
| 로컬 DB | Room |
| 차트 | MPAndroidChart |
| 백그라운드 | WorkManager |
| 최소 SDK | Android 8.0 (API 26) 이상 |

---

## 3. 데이터 소스 전략

### 소스별 역할 분담

| 데이터 | 1순위 | 2순위 (폴백) |
|--------|-------|-------------|
| 국내 주식 (KOSPI/KOSDAQ) | Naver Finance | Yahoo Finance |
| 미국 주식 (NYSE/NASDAQ) | Yahoo Finance | — |
| USD/KRW 환율 | Naver Finance | Yahoo Finance |
| 글로벌 지수 (S&P500, 나스닥, 코스피) | Yahoo Finance | — |

### 티커 형식

| 종류 | 예시 |
|------|------|
| 국내 주식 (Naver) | `005930` (삼성전자) |
| 국내 주식 (Yahoo) | `005930.KS` / `035720.KQ` |
| 미국 주식 | `AAPL`, `TSLA` |
| USD/KRW | `KRW=X` (Yahoo) / `FX_USDKRW` (Naver) |
| 글로벌 지수 | `^KS11`, `^GSPC`, `^IXIC` |

### Naver Finance 엔드포인트 (비공식)

```
국내 주식:
https://polling.finance.naver.com/api/realtime?query=SERVICE_ITEM:{종목코드}

환율:
https://polling.finance.naver.com/api/realtime?query=SERVICE_ITEM:FX_USDKRW
```

> ⚠️ 비공식 엔드포인트로 향후 변경 가능. Naver 실패 시 Yahoo로 자동 전환.

---

## 4. 시간대(Timezone) 처리

### 핵심 원칙
**단말기 로컬 시간을 기준으로 쓰지 않는다.** 해외 방문 중이어도 각 시장은 고정 타임존으로 독립 판단한다.

### 시장별 고정 타임존

| 시장 | 타임존 | 장 시간 (현지) |
|------|--------|--------------|
| 국내 | `Asia/Seoul` (KST, UTC+9) | 09:00 ~ 15:30 |
| 미국 | `America/New_York` (ET, UTC-5/-4) | 09:30 ~ 16:00 |

### 구현 방식

```kotlin
object MarketTimezone {
    val KOREA = ZoneId.of("Asia/Seoul")
    val USA   = ZoneId.of("America/New_York")
}

fun isMarketOpen(market: Market): Boolean {
    val zone = when (market) {
        Market.KOREA -> MarketTimezone.KOREA
        Market.USA   -> MarketTimezone.USA
    }
    val now = ZonedDateTime.now(zone)
    // 요일(월~금) + 시간 범위 체크
}
```

---

## 5. 장외 시간 데이터 처리

### 표시 전략

| 상태 | 표시 내용 |
|------|----------|
| 장중 | 실시간 데이터 (Naver/Yahoo polling) |
| 장외 | Room DB에 저장된 직전 장 마감 데이터 + "전일 종가" 레이블 |

### 화면 표시 예시

```
삼성전자  ₩71,200  ▲ +1.2%
[전일 종가]  05/30 15:30 KST 기준

AAPL  $189.50  ▼ -0.8%
[전일 종가]  05/30 16:00 ET 기준
```

### 마감 데이터 저장

- 국내: 15:30 KST에 WorkManager가 마감가 스냅샷 → Room DB 저장
- 미국: 06:00 KST (다음날)에 동일하게 저장
- Room DB 필드: `closePrice`, `closeDate`, `changeRate`, `market`

### 공휴일 처리

- **1단계 (초기)**: API 응답 없음 또는 거래량 = 0 → 자동으로 전일 종가 모드 전환
- **2단계 (추후 확장)**: 공휴일 캘린더 API 연동

---

## 6. 데이터 갱신 주기

| 상황 | 갱신 주기 | 소스 |
|------|----------|------|
| 국내 장중 (09:00~15:30 KST) | 30초~1분 | Naver |
| 미국 장중 (23:30~06:00 KST) | 5분 | Yahoo |
| 장외 (앱 포그라운드) | polling 중단, DB 데이터 표시 | — |
| 백그라운드 (앱 종료) | 15분 (WorkManager) | Naver/Yahoo |

---

## 7. 화면 구조

### 전체 레이아웃

```
┌─────────────────────────────┐
│  StockWatch          [+] [⋮] │  ← 상단 앱바 (종목 추가 / 메뉴)
├─────────────────────────────┤
│                             │
│        현재 탭 패널          │
│                             │
├─────────────────────────────┤
│ [요약] [차트] [지표] [환율] [알림] │  ← 하단 탭 바
└─────────────────────────────┘
```

---

## 8. 탭별 상세 설계

### 탭 1 — 요약 (Summary)

등록된 전체 종목을 카드 리스트로 표시. 포트폴리오 전체 상태를 한눈에 파악.

**카드 표시 정보**
- 종목명 + 코드
- 현재가 (또는 전일 종가)
- 등락 금액 및 등락률 (%)
- 거래량
- 데이터 출처 [N=Naver / Y=Yahoo] + 기준 시각

```
┌─────────────────────────────┐
│ 삼성전자  005930            │
│ ₩71,200       ▲ +850 +1.2% │
│ 거래량: 12.3M   [N] 14:52 KST│
└─────────────────────────────┘
```

---

### 탭 2 — 차트 (Chart)

선택한 종목의 가격 추이를 시각적으로 확인.

**구성 요소**
- 종목 선택 드롭다운
- 기간 선택: `1일 | 1주 | 1개월 | 3개월 | 1년`
- 캔들차트
- 이동평균선: 5일(빨강), 20일(노랑), 60일(파랑)
- 하단 거래량 바 차트

---

### 탭 3 — 기술지표 (Indicators)

매수/매도 타이밍 힌트 제공. 초보자를 위한 해석 텍스트 포함.

**제공 지표**

| 지표 | 설명 | 해석 표시 |
|------|------|----------|
| RSI (14) | 과매수/과매도 측정 | 70 이상: 과매수 ⚠️ / 30 이하: 과매도 💡 |
| MACD | 추세 방향 | 상승 추세 ▲ / 하락 추세 ▼ |
| 볼린저밴드 | 가격 변동성 | 상단 근접: 고점 주의 / 하단 근접: 반등 가능 |

```
RSI (14)        62.3  ━━━━━●━━━  중립
MACD            +0.82  상승 추세 ▲
볼린저밴드      상단 밴드 근접 ⚠️
```

---

### 탭 4 — 환율/글로벌 (Market)

**환율 섹션**
- USD/KRW 현재 환율 + 등락
- 일/주/월 차트

**글로벌 지수 섹션**

| 지수 | 소스 |
|------|------|
| KOSPI | Yahoo (`^KS11`) |
| S&P 500 | Yahoo (`^GSPC`) |
| NASDAQ | Yahoo (`^IXIC`) |

```
💱 환율
USD/KRW   1,382.5  ▼ -0.3%
[일 | 주 | 월 차트]

🌍 글로벌 지수
KOSPI     2,731  ▲ +0.4%
S&P 500   5,304  ▲ +0.8%
NASDAQ   16,782  ▲ +1.1%
```

---

### 탭 5 — 알림 (Alerts)

종목별 가격/비율 조건 관리.

**알림 조건 유형**
- 가격 도달: 현재가 ≥ X원 / ≤ X원
- 등락률: 하루 변동률 ≥ +X% / ≤ -X%
- 복수 조건 설정 가능 (종목당 여러 개)

**동작 시간**: 장중에만 알림 발송 (장외 polling 없음)

```
┌─────────────────────────────┐
│ 삼성전자                    │
│ ☑ 가격 ≥ ₩75,000           │
│ ☑ 등락률 ≤ -3%             │
│ [+ 조건 추가]               │
└─────────────────────────────┘
```

---

## 9. 알림(Notification) 흐름

```
WorkManager (장중 15분 주기 polling)
    ↓
Naver/Yahoo API 호출
    ↓
Room DB 저장
    ↓
알림 조건 비교
    ↓ (조건 충족 시)
Android Notification 발송
("삼성전자 ₩75,200 — 설정 가격 도달")
    ↓
알림 탭 시 앱 열고 해당 종목으로 이동
```

---

## 10. 프로젝트 폴더 구조

```
app/src/main/
├── data/
│   ├── api/
│   │   ├── NaverFinanceApi.kt      ← 국내 주식, 환율
│   │   └── YahooFinanceApi.kt      ← 미국 주식, 글로벌 지수
│   ├── db/
│   │   ├── StockDao.kt
│   │   ├── AlertDao.kt
│   │   └── AppDatabase.kt
│   └── repository/
│       └── StockRepository.kt      ← 소스 선택 로직 (Naver 실패 시 Yahoo 폴백)
├── domain/
│   └── model/
│       ├── Stock.kt
│       ├── StockPrice.kt
│       ├── Alert.kt
│       └── ExchangeRate.kt
├── ui/
│   ├── summary/                    ← 탭 1
│   ├── chart/                      ← 탭 2
│   ├── indicators/                 ← 탭 3
│   ├── market/                     ← 탭 4
│   └── alerts/                     ← 탭 5
├── worker/
│   ├── PriceCheckWorker.kt         ← 가격 체크 + 알림
│   └── MarketCloseWorker.kt        ← 마감가 스냅샷 저장
└── util/
    └── MarketTimezone.kt           ← 타임존 고정 유틸
```

---

## 11. 개발 환경 (Windows → Android)

1. **Android Studio** 설치 (IDE + SDK + 에뮬레이터 포함)
2. **USB 드라이버** 설치 (제조사별: 삼성이면 Samsung USB Driver)
3. 핸드폰: **설정 → 개발자 옵션 → USB 디버깅** 활성화
4. USB 또는 WiFi로 연결 후 Android Studio에서 직접 빌드/설치

---

## 12. 개발 단계 (순서)

| 단계 | 내용 | 핵심 결과물 |
|------|------|------------|
| 1 | 개발 환경 세팅 + Hello World 폰에 설치 | 앱 실행 확인 |
| 2 | 종목 추가/저장 + 요약 탭 (탭 1) | Room DB + Naver/Yahoo 연동 |
| 3 | 차트 탭 (탭 2) | MPAndroidChart 연동 |
| 4 | 환율/글로벌 탭 (탭 4) | 환율 + 지수 표시 |
| 5 | 기술지표 탭 (탭 3) | RSI, MACD, 볼린저밴드 계산 |
| 6 | 알림 (탭 5 + WorkManager) | 조건 알림 수신 |

---

*본 설계서는 구현 진행 중 필요에 따라 업데이트될 수 있습니다.*
