# 숫자야구 게임

소프트웨어 시스템 설계 개인 프로젝트 (정시완, 251423)
Java Swing으로 만든 숫자야구 게임이다.

## 실행

```
java -jar number-baseball.jar
```

Java 17 이상이 필요하다. 전적 파일(stats.csv)은 실행한 위치에 생성된다.

## 게임 규칙

- 1~9 중 서로 다른 숫자 3자리를 맞힌다.
- 결과: 숫자와 위치가 같으면 스트라이크(S), 숫자만 같으면 볼(B), 하나도 없으면 OUT!!
- 모드
  - 1:1 PvP: 2명이 서로의 숫자를 먼저 맞히면 승리
  - 다인 PvP: 3~5명. 상대를 골라 공격하고, 숫자를 맞히면 그 상대가 탈락. 마지막 1명이 우승
  - PvE: 컴퓨터의 숫자를 10회 안에 맞히면 승리
- 턴 제한 시간은 60초. 기권하거나 시간이 지나면 PvE는 실패, 1:1은 상대 승리, 다인은 본인 탈락
- 비밀 숫자 입력을 취소하거나 게임 중 창을 닫으면 그 게임은 무효이며 전적에 반영하지 않는다.
- 전적은 이름 기준으로 모드별 플레이 수, 승리 수, 승률을 기록한다. 다인은 우승만 승리로 집계한다.

## 구성

| 패키지 | 내용 |
|---|---|
| model | 게임 규칙과 진행 상태 (SecretNumber, GameSession 등) |
| controller | 화면과 모델 연결, 턴 타이머 (GameController, TurnTimer) |
| view | Swing 화면 (MainFrame, StartPanel, SetupPanel, GamePanel 등) |
| stats | 전적 저장·조회 (StatsRepository, PlayerStats) |
