# 버스 알리미

우테코 크루가 자주 이용하는 정류장과 버스를 Slack 명령어로 조회하고,
설정한 시간대에 버스가 곧 도착하면 Slack DM으로 알려주는 퇴근 보조 서비스입니다.

## 핵심 기능

- `/조회 [정류장]`
- `/조회 [정류장] [버스번호]`
- `/알림 [정류장] [버스번호] [몇 분 전] [시작시간] [종료시간]`
- `/알림목록`
- `/알림삭제 [정류장] [버스번호]`
- `/도움말`
- 1분 주기 알림 스케줄링
- 같은 알림 10분 내 중복 발송 방지
- 같은 사용자, 정류장, 버스번호 조합의 중복 등록 시 기존 알림 업데이트

## 기술 스택

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- H2 file mode
- Slack Slash Command
- Slack `chat.postMessage`
- 경기버스 API
- Gradle

## 실행 준비

필수 환경변수:

```bash
export SLACK_BOT_TOKEN="<slack-bot-token>"
export SLACK_SIGNING_SECRET="..."
export GBIS_SERVICE_KEY="..."
```

주의:

- Slack bot token, signing secret, 경기버스 API key는 커밋하지 않습니다.
- 노출된 Slack token은 Slack App 설정에서 즉시 폐기하고 새로 발급합니다.
- 로컬 데모에서는 ngrok URL을 Slack Slash Command Request URL로 연결합니다.

## 실행

로컬 설정 파일을 생성합니다.

```bash
cp src/main/resources/application-example.properties src/main/resources/application.properties
```

`src/main/resources/application.properties`에는 로컬 실행용 기본값이 들어 있습니다.
Slack token, signing secret, 경기버스 API key는 환경변수로만 주입합니다.

```bash
./gradlew bootRun
```

기본 설정:

- 서버 timezone: `Asia/Seoul`
- DB: `jdbc:h2:file:./data/bus-alert`
- H2 Console: `/h2-console`
- 알림 쿨타임: 10분
- 알림 체크 주기: 60초

## 테스트

```bash
./gradlew test
```

테스트는 `src/test/resources/application.properties`의 H2 in-memory 설정을 사용합니다.
실행 중 생성되는 file mode DB와 분리되어 있습니다.

## Slack Slash Command 설정

Slack App에서 아래 명령어를 생성하고, Request URL을 ngrok 주소와 연결합니다.

| Slack 명령어 | Request URL |
| --- | --- |
| `/조회` | `POST /slack/commands/search` |
| `/알림` | `POST /slack/commands/alert` |
| `/알림목록` | `POST /slack/commands/alert-list` |
| `/알림삭제` | `POST /slack/commands/alert-delete` |
| `/도움말` | `POST /slack/commands/help` |

예시:

```text
https://{ngrok-domain}/slack/commands/search
https://{ngrok-domain}/slack/commands/alert
https://{ngrok-domain}/slack/commands/alert-list
https://{ngrok-domain}/slack/commands/alert-delete
https://{ngrok-domain}/slack/commands/help
```

## 명령어 예시

정류장 도착 정보 조회:

```text
/조회 텔레칩스
```

특정 버스 도착 정보 조회:

```text
/조회 텔레칩스 310
```

알림 등록:

```text
/알림 텔레칩스 310 5 17:45 23:30
```

알림 목록 조회:

```text
/알림목록
```

알림 삭제:

```text
/알림삭제 텔레칩스 310
```

도움말:

```text
/도움말
```

## 지원 정류장과 버스

MVP에서는 정류장 이름 중복을 피하기 위해 아래 정류장만 지원합니다.

- 텔레칩스
- 벤처타운(북문)

지원 정류장과 노선 매핑은 DB에서 관리합니다.
앱 시작 시 정류장 기본 데이터와 해당 정류장의 경유 노선을 경기버스 API에서 읽어 자동으로 저장합니다.

| 정류장 | stationId | mobileNo | 지역 |
| --- | --- | --- | --- |
| 텔레칩스 | `204000158` | `05341` | 성남 |
| 벤처타운(북문) | `204000159` | `05342` | 성남 |
실행 후 `bus_routes` 테이블에 경유 노선이 채워집니다.

## 알림 정책

알림은 아래 조건을 모두 만족할 때 발송됩니다.

- 현재 시간이 알림 시작 시간 이상입니다.
- 현재 시간이 알림 종료 시간 이하입니다.
- 등록된 정류장과 버스번호 매핑이 존재합니다.
- 첫 번째 버스 도착 예정 시간이 `notifyBeforeMinutes` 이하입니다.
- 같은 사용자, 정류장, 버스번호 조합의 알림이 최근 10분 이내 발송되지 않았습니다.

MVP에서는 자정을 넘기는 알림 시간을 지원하지 않습니다.
예를 들어 `23:00 01:00`은 등록할 수 없습니다.

## 입력 검증

정류장:

- 지원 정류장만 허용합니다.

버스번호:

- 정류장별로 설정된 버스번호만 허용합니다.

알림 기준 시간:

- 1분 이상 30분 이하만 허용합니다.

시간:

- `HH:mm` 형식만 허용합니다.
- 예: `09:05`, `17:45`, `23:30`
- 비허용: `5:45`, `17`, `24:00`

시작/종료 시간:

- 종료 시간은 시작 시간보다 늦어야 합니다.
- 자정을 넘기는 시간대는 지원하지 않습니다.

## 패키지 구조

도메인 우선 패키지 구조를 사용합니다.

```text
com.woowa.bus
├── alert
│   ├── application
│   ├── domain
│   └── infrastructure
├── arrival
│   ├── domain
│   └── infrastructure
├── route
│   ├── domain
│   └── infrastructure
├── search
│   └── application
├── slack
│   ├── application
│   ├── infrastructure
│   └── presentation
└── global
    └── config
```

레이어 책임:

- `presentation`: HTTP 요청/응답과 Slack command text 변환
- `application`: 유스케이스 흐름 조율
- `domain`: 핵심 규칙과 저장소 포트
- `infrastructure`: JPA, 경기버스 API, Slack API 구현

## 주요 설정 파일

- `src/main/resources/application-example.properties`: 로컬 실행 설정 예시
- `src/main/resources/application.properties`: 로컬 실행 설정
- `src/test/resources/application.properties`: 테스트 실행 설정
- `build.gradle`: 의존성과 Java 버전

## 데모 체크리스트

- [ ] Slack App 생성
- [ ] Slash Command 4개 생성
- [ ] ngrok URL 연결
- [ ] `SLACK_BOT_TOKEN` 설정
- [ ] `SLACK_SIGNING_SECRET` 설정
- [ ] `GBIS_SERVICE_KEY` 설정
- [x] 텔레칩스 `stationId` 확보
- [x] 벤처타운(북문) `stationId` 확보
- [ ] `bus_routes` 테이블 자동 동기화 확인
- [ ] `/조회 텔레칩스` 응답 확인
- [ ] `/알림 텔레칩스 310 5 17:45 23:30` 등록 확인
- [ ] `/알림목록` 확인
- [ ] Slack DM 알림 발송 확인
