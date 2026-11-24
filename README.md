# market-widget-data

갤럭시 홈 화면용 **시황 위젯**과 그 데이터.

## 데이터 (루트의 JSON 4개)
Morning Brief 작성 후 매일 교체합니다. 위젯은 이 중 `markets.json`, `fed.json`을 읽습니다.

- `markets.json`: 美 지수, 국채 2·10·30년 금리, WTI·브렌트 종가
- `stocks.json`: AI·반도체·빅테크 종목 등락률
- `indicators.json`: 美 경제지표
- `fed.json`: 기준금리, FOMC 일정·FedWatch

## 위젯 앱
- 소스: `app/`
- `app/` 이나 빌드 설정을 바꿔 올리면 GitHub Actions가 APK를 만들어 **Releases**에 `MarketWidget.apk`로 올립니다.
- JSON만 바꿀 때는 빌드가 돌지 않습니다.
- 위젯은 약 1시간마다, 또는 ↻를 누를 때 데이터를 다시 읽고, 본문을 누르면 시황 대시보드가 열립니다.

앱 코드를 바꿔 새로 빌드한 APK는 서명 키가 매번 달라, 기존 앱을 지운 뒤 설치해야 합니다.
