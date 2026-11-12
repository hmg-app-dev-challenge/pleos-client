# 차Park IVI 실행 계획과 작업 기록

> 최신 개편: 폰·IVI 모두 개발용 배너·상태 조작·진단 UI를 제거하고 제품 사용 흐름으로 변경했다. 현재 진행 순서와 구현 범위는 [제품 흐름 계획](../PRODUCT_FLOW_PLAN.md)을 기준으로 한다. 아래 기존 시연 조작 안내는 이전 기록이다.


작성: 2026-11-11. 기준: 통합 기획안, 개발 계획 v1.1, SDK 개발자 가이드, Pleos Figma 디자인 시스템. 사용자 요청에 따라 폰 데모 `:app`을 유지하고 차량 제품은 독립 `:ivi` 모듈로 개발한다. 문서의 예시·가설을 실제 SDK 계약이나 승인으로 간주하지 않는다.

## 작업 순서와 인수 조건

| ID | 세부 작업 | 의존 | 완료 조건 | 현황 |
| --- | --- | --- | --- | --- |
| A1 | 독립 IVI 앱·applicationId·Automotive manifest | — | 폰/IVI APK 각각 빌드 | 완료 |
| A2 | Vehicle 2.3.2 / NaviHelper 3.0.5 의존성 고정 | A1 | 공식 저장소 해석·실제 클래스 컴파일 | 완료 |
| A3 | AAR 시그니처·권한 계약 기록 | A2 | 초기화·SOC 타입·capability·해제 대조 | 조회 기반 완료, 단위 미확인 |
| A4 | 읽기 전용 SDK 연결 진단 화면 | A3 | 권한 없음/미지원/오류/시간 초과·재조회 | 실제 SDK READY/READ/raw 100 확인; 실패 주입은 후속 |
| A5 | Pleos 2.1.0 AVD 생성·실행 | A2 | API34/2560×1440/mdpi 실제 창 크기 기록 | 완료: 전용 AVD, 2560×1440/160dpi; 앱 창 1682×1210 |
| A6 | My Project·기기 권한 확인 | A2 | 프로젝트별 기능 사용 상태 기록 | 기기 CAR_ENERGY normal·READ 확인, My Project 승인 미확인 |
| B1 | 원본 Light/Dark 색상·Asta Sans IVI 타입 | A1 | 폰 토큰과 분리, 원본 대응 | 시작: 공통 색상·6개 타입 역할 |
| B2 | 버튼·선택 컨트롤·제목·팝업 상태별 구현 | B1 | Figma 노드/토큰/자산/상태 대응표 | 시연에 필요한 버튼·스위치·체크박스·분할 선택·팝업 구현; 전체 라이브러리 상태 검수는 후속 |
| B3 | 컴포넌트 검수 화면 | B2,A5 | 양 테마·enabled/pressed/disabled·긴 문구 대조 | 버튼 검수 화면 및 여행 화면 Light/Dark 캡처; 모든 컴포넌트 상태 카탈로그는 후속 |
| C1 | 플랫폼 UX 제한·주차 신호 어댑터 | A5,A6 | UNKNOWN 상세 잠금, 주차와 도착 분리 | 잠금/선택적 AAOS 어댑터 구현; Pleos 라이브러리 호환 경로 확인 필요 |
| C2 | 여행 모델/규칙 공통 모듈 분리 | A1 | 폰 회귀 테스트 유지·가상/실데이터 분리 | 완료: :shared 공통 규칙·여행 모델, 폰 회귀 7개 통과 |
| C3 | S1 시작·S2 조건 입력 | B3,C1,C2 | 원본 IVI 글자·터치 규격, 가로/좁은 창 | 시연 완료: IVI 규격, 가로/800dp 좁은 창 |
| C4 | S3 후보 비교·S4 준비 계획 | C3 | 조건 변경에 따른 행동·체크 갱신 | 시연 완료: 3개 가상 후보, SOC·취사·보온 분기·체크 갱신 |
| C5 | DataStore 여행 저장·복원 | C2 | 재시작/오류 복구·스키마 버전 | 완료: DataStore·스키마·오류 처리, 기기 재실행 복원 확인 |
| D1 | SOC 단위·freshness·구독 검증 | A4,A5,A6 | 단위 근거, 수신/센서시각 구분, stale 제거 | 원시값 100 실수신; 단위·구독·stale 런타임은 대기 |
| D2 | 검증 장소 최소 3곳·진입구 좌표 | C2 | 운영 근거·확인일·unknown 처리 | 대기 |
| D3 | NaviHelper 목적지 요청·이벤트 상태 머신 | A4,A6,D2 | 좌표 순서·요청/탐색/안내/도착 분리 | 실제 초기화 READY; 목적지/경로/도착 연동 대기 |
| D4 | S5 이동·내비 복귀 | D3,C5 | 중복 탭/오래된 이벤트/복귀 상태 보존 | 시연 완료: 요청/실패/재시도/안내/도착/주차 분리; 실제 내비 연동 후속 |
| E1 | S6 수면 준비·S7 종료 안내 | C4,C5 | 수동 안내로 완주 가능 | 완료: 수동 수면 준비·철수 각 4개 확인, 종료 기록 |
| E2 | 선택적 공조·조명 제어 | A6,C1,E1 | 승인·READ_WRITE·범위·재조회·부분 실패 | 조건부, 쓰기 API 미구현 |
| F1 | 실패/권한/미지원/주행 전환 테스트 | C1,D1,D3,E1 | 자동 성공·0% 대체·자동 재전송 없음 | 시연 실패/재시도/잠금·늦은 이벤트 테스트 완료; 실제 권한 실패 주입 후속 |
| F2 | S1~S7 × Light/Dark × 두 창 크기 검수 | B3,E1 | 폰트·색상·간격·자산·잘림 검수표 통과 | 좁은 창 7단계×2테마 테스트, 가로 여정·완료 Dark 캡처 검수; 모든 상태 시각 검수 후속 |
| F3 | 설치 가능한 release·재현 문서·영상 | F1,F2 | 깨끗한 Pleos AVD에서 재현 | debug APK·시연 안내 완료; 배포 release·영상 후속 |

최신 시연 마일스톤은 완료했다. [IVI_DEMO_GUIDE.md](../IVI_DEMO_GUIDE.md)에 7단계 진행·실제/시연 구분·검증 증거를 기록했다. 다음 제품화 관문은 실제 플랫폼 UX 계약, SOC 단위·구독, 검증 장소와 실제 내비 연동이다.

## 이번 착수 산출물

- `ivi/build.gradle.kts`: 공식 SDK Maven 의존성, 기존 프로젝트와 동일한 AGP/Kotlin/Compose 기반.
- `PleosSession`: 사용자가 시작하는 읽기 진단. CAR_ENERGY 권한 → capability READ/READ_WRITE → SOC 원시값 조회. NaviHelper는 초기화 콜백만 확인. 8초 타임아웃은 앱 정책.
- 화면 종료 시 release, 이전 연결의 늦은 콜백 무시. 60초 후 조회값 삭제는 앱 freshness 정책이며 SDK가 보장한 값이 아니다.
- 원시 SOC 단위가 확인되지 않아 %로 변환하거나 충전 규칙에 넣지 않음. 내비 목적지·경로 취소·공조·조명 쓰기 API 호출 없음.
- IVI 서체: Asta Sans 400/700/800. Headline 60/80, Title40/52, SmallTitle32/40, Body30/38, SmallBody26/34, Label24/28. Large 버튼 최소 높이100/반경32/수평패딩40. 이는 원본 숫자를 dp/sp로 대응한 초기 구현이며 실제 창 밀도 대조는 A5에서 확정.
- 상태 카드는 차Park 진단 화면 구성이지 Figma의 명명된 카드 컴포넌트 복제라고 주장하지 않음. 런처는 기존 임시 차Park 아이콘 재사용.

## 디자인 완료 기준

“완벽히 준수”는 모든 단계 구현·기기 실행·원본 대조 후에만 완료로 기록한다. 토큰 일부 적용이나 빌드 성공만으로 디자인 완료라고 표시하지 않는다. 원본 컴포넌트에 없는 앱 고유 레이아웃은 별도 표기한다. 폰용 축소 치수를 IVI로 재사용하지 않으며 시스템 창 크기가 좁아지면 요소를 재배치하고 전체 화면을 축소하지 않는다.

## 확인된 SDK 계약

공식 Maven AAR를 다운로드하고 javap와 실제 Kotlin 컴파일로 확인했다.

| API | 확인 계약 | 미확인 |
| --- | --- | --- |
| Vehicle(context) | initialize(), release(), getEvBattery() | 서비스 연결/권한의 실제 응답 |
| checkEvBatteryStateOfChargeCapabilities | (CapabilityConfig)->Unit, (Exception)->Unit | 차종별 실제 capability |
| CapabilityConfig.accessType | NONE / READ / WRITE / READ_WRITE | 프로젝트 승인 |
| getEvBatteryStateOfCharge | (Int)->Unit, (Exception)->Unit; CAR_ENERGY | 원시값 스케일·단위 |
| SOC listener | onEvBatteryStateOfChargeUpdated(Integer), onFailed(Exception) | null·센서 시각·구독 런타임 |
| NaviHelper.initialize | 성공 ()->Unit / 실패 (Throwable)->Unit | 실제 서비스 상태 |
| NaviHelper.release | 리소스 정리 | 내비 런타임 |

SDK가 병합하는 manifest에 Vehicle binding 권한과 두 서비스 package visibility query가 포함됨. 앱은 CAR_ENERGY만 추가 선언했다. 선언과 배포 심사 승인은 별개이며 승인 상태는 미확인이다.

참조: [Vehicle 설치](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup), [도메인 획득](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object), [권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission), [NaviHelper 설치](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/importing-sdk), [에뮬레이터](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator).

## 재현

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :ivi:assembleDebug :ivi:testDebugUnitTest :ivi:lintDebug :app:assembleDebug
```

IVI APK: `ivi/build/outputs/apk/debug/ivi-debug.apk`. Android Studio Gradle Sync 후 `ivi` 모듈을 Pleos AVD에 실행한다. debug 에뮬레이터 첫 화면은 7단계 시연이며 실제 SDK 진단은 상단 버튼으로 연다. 아래 착수·2차 기록은 당시 결과다.


## IVI 착수 검증 — 2026-11-11

- 별도 `:ivi` 모듈에 Vehicle 2.3.2 / NaviHelper 3.0.5 실제 의존성을 적용했다. 기존 `:app` 폰 데모는 유지한다.
- IVI APK 빌드, 단위 테스트 2개(해제·재연결 뒤 늦은 콜백 차단), Lint 및 기존 폰 APK 빌드 성공.
- IVI Lint: 오류 0, 경고 4(고정 의존성 최신 버전·targetSdk 안내).
- Pleos 시스템 이미지는 설치되어 있으나 Pleos AVD 생성·실행, 서비스 연결, 승인/권한, SOC 단위는 아직 검증하지 않았다.
- 현재 IVI 화면은 읽기 전용 SDK 진단이다. 7단계 제품 화면과 디자인 시스템 전체 상태별 시각 검수는 남아 있다.


## 2차 작업 — Pleos 실행 및 검수

상세 증거와 재현은 [IVI_RUNTIME_VERIFICATION.md](../IVI_RUNTIME_VERIFICATION.md) 참조.

- 전용 로컬 AVD 부팅·IVI APK 설치·실행 완료. 기존 폰 데모/ADB 키 변경 없음.
- 읽기 전용 기기 프로브: Vehicle READY, NaviHelper READY, capability READ, rawSoc 100. 이 값은 에뮬레이터 관측이며 % 단위/실차/배포 승인 증거가 아님.
- `android.car` 필수 선언은 설치 실패를 일으켜 선택적으로 수정. 설치 이미지에 `ai.pleos.playground.car`만 보이는 차이를 기록. 현재 앱은 UX UNKNOWN에서 상세 조작을 잠금. 테스트 하네스에서만 독립적인 SDK 읽기와 버튼 검수 실행.
- 다음 우선 작업: 공식 주행 규제 가이드와 설치 이미지의 공유 라이브러리 연결 계약 대조 → C1 실제 제한 신호·변경 검증. 병행 가능 작업은 B2 선택 컨트롤/팝업과 C2 공통 여행 모델 분리.
