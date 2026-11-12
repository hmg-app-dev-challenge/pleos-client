# 개발 작업표

## 0.3.0 제품 흐름 개편 완료

폰/IVI의 개발용 배너·진단·데이터 조작을 제거했다. 공통 여행 모델, 자동 이동 정보 준비, 사용자 도착·주차 확인, 수면·철수 체크 관문을 적용했다. JVM 19개, IVI 기기 7개, 폰 기기 3개 통과. [현재 계획과 증거](PRODUCT_FLOW_PLAN.md) 참조.


> 최신 개편: 폰·IVI 모두 개발용 배너·상태 조작·진단 UI를 제거하고 제품 사용 흐름으로 변경했다. 현재 진행 순서와 구현 범위는 [제품 흐름 계획](PRODUCT_FLOW_PLAN.md)을 기준으로 한다. 아래 기존 시연 조작 안내는 이전 기록이다.


> 최신 상태 — 2026-11-11: IVI 7단계 시연 프로토타입 완성. DataStore 저장, 조건별 행동, 내비 실패/재시도·주차 분리, 수면·철수 수동 확인, Pleos 디자인 컴포넌트를 구현했다. JVM 19개·기기 7개 통과. [시연 안내·실제 연결 범위·증거](IVI_DEMO_GUIDE.md)를 현재 기준으로 사용한다. 아래는 단계별 이전 검증 기록이다.


기준: 개발 계획 v1.1 · 시작일 2026-11-11

| 단계 | 작업 | 현재 상태 | 통과 조건 |
| --- | --- | --- | --- |
| 1 | 환경·빌드 기반 | 완료 | Android Studio 동기화·실행, Gradle wrapper, debug APK 빌드 성공 |
| 2 | 모델·판단 규칙 | 완료 | SOC 경계/누락·제한 조건·체크 유지 단위 테스트 7개 통과 |
| 3 | 로컬 데모 화면 | 기본 흐름 검증 완료 | Pixel 6 API 30에서 S1~S7·주행 제한·Activity 재생성 테스트 2개 통과. 가로/테마 시각 QA는 남음 |
| 4 | 상태 저장·Pleos 디자인 | 폰 디자인 적용, 저장은 대기 | Asta Sans·Light/Dark 토큰·공통 선택 컨트롤 적용. DataStore는 미구현 |
| 5 | 실제 데이터·SDK | 대기 | 검증 장소 3곳, My Project 상태, Vehicle/Navi AAR·단위·권한·에뮬레이터 |
| 6 | 실패 처리·제출 | 대기 | 실연동/데모 구분, 통합 테스트, release APK·영상·README |

## 환경 확인

- Android Studio 설치·실행 확인. 사용자 기존 프로젝트는 유지.
- SDK platforms: 31/34/35/36, Build Tools: 34.0.0/35.0.0.
- Gradle 8.9 캐시 존재, Android Studio JBR 21.0.3.
- 기존 AVD: Galaxy S9 API 30, Pixel 6 API 30.
- Android Studio에서 Pleos Connect 2.1.0 ARM64/API 34 이미지 다운로드 진행을 관찰. 설치 완료·AVD 실행은 아직 검증 전.
- My Project SDK 사용 상태·제어 승인 미확인. 실제 SDK 의존성/차량 권한은 아직 추가하지 않음.

## 이번 구현의 경계

7단계 데모 화면과 순수 규칙을 먼저 구현한다. 가상 fixture에는 실제 내비로 보낼 좌표가 없으며 내비·차량 설정 요청은 항상 미연동 결과를 반환한다. 주행 토글은 Fake 상태에만 영향을 주고 실제 차량 제어를 활성화하지 않는다.

규칙 데이터는 이번 첫 구현에서 Kotlin의 타입 모델로 작성했다. 규칙·검증 장소의 버전 관리 JSON과 DataStore는 4~5단계에서 도입한다. 현재 저장은 SavedStateHandle이며 영속 저장 완료로 표시하지 않는다.

## 검증 기록

- 최초 샌드박스 Gradle 실행의 네이티브 캐시 오류는 승인된 환경에서 재실행하여 해결.
- `:app:testDebugUnitTest`: 7 tests, 0 failures/errors.
- `:app:connectedDebugAndroidTest`: Pixel 6 API 30 (Android 11)에서 2 tests, 0 failures/errors. 7단계 흐름, 미연동 표시, 주행 잠금, Activity 재생성 후 상태 유지 확인.
- 첫 UI 테스트의 토글 선택자 중복 실패는 전용 testTag로 수정 후 재실행 통과.
- `:app:assembleDebug`: 성공. `app/build/outputs/apk/debug/app-debug.apk` 생성.
- `:app:lintDebug`: 오류 0. 남은 경고는 고정 의존성의 최신 버전 안내와 targetSdk 34 안내이며, 앱 아이콘·백업 설정 경고는 수정.
- Android Studio에서 ChaPark 프로젝트 열기·Gradle 동기화·Pixel 6 기기 Run 실행 및 설치 성공 메시지 확인.
- Pleos 에뮬레이터 설치 완료/실행과 실제 SDK 연동은 아직 검증하지 않음. 일반 Android 실행 결과를 Pleos 검증으로 세지 않음.

## 다음 작업

1. DataStore 여행 저장·복원과 스키마 버전/오류 복구 구현.
2. 폰 디자인 적용 내역은 PHONE_DESIGN_MAPPING.md 참조. 가로/큰 글자 추가 검수.
3. Pleos 설치 완료 후 My Project 상태·Vehicle/Navi 단위/권한 계약 검증.

## 폰 디자인 개편

사용자의 모바일 요청에 따라 7단계를 폰 단일 열로 재구성. 데모 설정 팝업, Asta Sans 로컬 폰트, 원본 Light/Dark 색상, 회색 버튼, 초록 사각형 스위치/체크박스, 분할 선택 적용. 원본 IVI 치수와 모바일 치수는 구분한다. 개편 후 9개 테스트와 APK 빌드·Lint 통과.


## IVI 착수 검증 — 2026-11-11

- 별도 `:ivi` 모듈에 Vehicle 2.3.2 / NaviHelper 3.0.5 실제 의존성을 적용했다. 기존 `:app` 폰 데모는 유지한다.
- IVI APK 빌드, 단위 테스트 2개(해제·재연결 뒤 늦은 콜백 차단), Lint 및 기존 폰 APK 빌드 성공.
- IVI Lint: 오류 0, 경고 4(고정 의존성 최신 버전·targetSdk 안내).
- Pleos 시스템 이미지는 설치되어 있으나 Pleos AVD 생성·실행, 서비스 연결, 승인/권한, SOC 단위는 아직 검증하지 않았다.
- 현재 IVI 화면은 읽기 전용 SDK 진단이다. 7단계 제품 화면과 디자인 시스템 전체 상태별 시각 검수는 남아 있다.
- 상세 작업 순서·선행 조건·완료 기준: [IVI_WORK_PLAN.md](IVI_WORK_PLAN.md).


## IVI 2차 진행 — 2026-11-11

Pleos 전용 AVD 생성/부팅/앱 설치·실행을 완료했다. 실제 읽기 테스트에서 Vehicle/NaviHelper READY, SOC capability READ, 원시값 100을 확인했다. AAOS 라이브러리 이름 차이로 발생한 설치 실패는 선택적 어댑터로 해결했고, 주행 신호 미확인 시 상세 조작 잠금을 유지한다. Large/Medium 버튼 상태와 검수 화면을 보완했다. 최신 증거·제약은 [IVI_RUNTIME_VERIFICATION.md](IVI_RUNTIME_VERIFICATION.md) 참조. 위의 최초 착수 당시 'AVD 미생성/실행 미검증' 기록은 이번 결과로 갱신된다.
