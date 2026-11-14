# 차Park

처음 가는 차박 장소의 조건과 차량 배터리를 함께 확인하고, 준비·길 안내·철수를 ‘내 차와 보낸 하룻밤 기억’으로 연결하는 Pleos 차량 앱 프로토타입. 핵심 메시지는 **내 차로 오늘 밤**이다.

기획 기준은 **v2.1.1**, IVI 앱 버전은 `0.5.1`이다. 문서 개정은 전체 기능 구현 완료를 의미하지 않는다. [2.1.1 상세 변경 검토](docs/V2_1_1_CHANGE_REVIEW.md)와 [구현 현황](docs/IMPLEMENTATION_STATUS.md)을 참고한다.

## 최신 개발 기준

최신 APK는 `~/Desktop/ChaPark-IVI-0.5.1-release.apk`다. [0.5.1 변경·인수](docs/RELEASE_0_5_1.md)에 UX 응답 지연 복구, 새 Pleos AVD 전체 흐름, 실제 오프라인 실패와 남은 재연결 제한을 기록했다.

2026-11-14 첨부 MVP PRD v0.2를 [개발 계획](docs/IVI_WORK_PLAN.md)에 반영했다. [기능별 차이·결정 대장](docs/MVP_PRD_V0_2_REVIEW.md)과 [목표 5단계 흐름](docs/PRODUCT_FLOW_PLAN.md)을 따른다. 디자인은 기존 현대차 Pleos 시스템을 유지한다. 서명된 0.5.0은 5단계·취사 3종·준비/누락·밤 전환·차량 경계·기록을 구현했다. [0.5.0 인수 범위](docs/MVP_V0_2_ACCEPTANCE.md)의 남은 자료·실차 조건을 포함한 PRD 전체 인수 완료로 보지는 않는다. 단계별 구현과 시험은 [MVP 실행 기록](docs/MVP_V0_2_EXECUTION.md)에 남긴다.

## 현재 구현된 사용 흐름

홈 → 차박 계획(여행 조건/장소 비교) → 출발 준비 → 이동 → 차박 → 철수와 기록.

폰과 IVI 모두 제품 언어로 구성했다. 개발용 배너·데이터 조작·SDK 진단 버튼은 사용자 화면에 없다. IVI는 선택한 장소를 NaviHelper로 경로 요청하고 차량 내비 화면을 연다. 폰은 로컬 이동 흐름을 유지한다. 도착은 SDK 알림 또는 사용자 확인, 주차는 사용자 확인으로 처리한다. 수면 전 6개, 철수 5개의 직접 확인 항목을 모두 완료해야 종료할 수 있다. P단과 플랫폼 UX 허용이 함께 확인되어야 상세 조작할 수 있다.

- [v2.1.1 제품 기획서](docs/PRODUCT_SPEC_V2_1.md)
- [v2.1.1 화면·사용자 흐름](docs/PRODUCT_FLOW_PLAN.md)
- [현재 구현 상태와 차이](docs/IMPLEMENTATION_STATUS.md)
- [사용 순서·실행 방법](docs/IVI_DEMO_GUIDE.md)
- [전체 IVI 작업표](docs/IVI_WORK_PLAN.md)

## 모듈과 데이터

- `:app`: 폰 UI, Pleos 색상·서체·선택 컨트롤을 모바일 크기로 적용. SavedStateHandle 복원.
- `:ivi`: 차량 UI, Pleos 원본 IVI 치수·Light/Dark·좁은 창 재배치. DataStore 영속 저장.
- `:shared`: 공통 여행 상태·조건별 준비 규칙·내부 fixture.

IVI 목적지는 공식 지도 좌표가 있는 중랑캠핑숲·난지캠핑장·강동그린웨이 가족캠핑장·몽산포다. 선택 충전 경유, 풍경/조건 필터, 근거 캐시와 실제/계산 사건 기억을 지원한다. 확인되지 않은 운영 조건은 미확인으로 표시한다. 충전 후보는 현재 난지 지역에만 연결되어 있다. IVI의 실제 NaviHelper 경로 요청과 내비 화면 전환을 Pleos 에뮬레이터에서 검증했다. IVI SOC는 공개 Vehicle SDK 공급자에 연결했고 에뮬레이터 20→37→20 구독 변화를 확인했다. 폰 SOC·기온과 폰 장소는 내부 fixture이며 차량 설정 변경은 연결하지 않았다. 지도 배경 타일 표시·자동 이동 재생·실차 UX/권한은 별도 검증이 필요하다. [내비 연동 및 검증](docs/NAVIGATION_INTEGRATION.md)을 참고한다.

## 실행

Android Studio에서 이 폴더를 열고 폰은 `app`, Pleos는 `ivi`를 선택한다. SDK 경로는 `local.properties`, JDK는 Android Studio 내장 JBR을 사용한다.

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :shared:test :app:testDebugUnitTest :ivi:testDebugUnitTest :app:assembleDebug :ivi:assembleDebug :app:lintDebug :ivi:lintDebug
scripts/run-pleos-emulator.sh -port 5556
```

최신 서명 release와 검증 범위는 [0.5.0 인수 기록](docs/MVP_V0_2_ACCEPTANCE.md), 단계별 커밋은 [MVP 실행 기록](docs/MVP_V0_2_EXECUTION.md)을 참고한다.

실행 중인 AVD는 다시 실행하지 않는다. APK는 `app/build/outputs/apk/debug/app-debug.apk`, `ivi/build/outputs/apk/debug/ivi-debug.apk`다. 서명된 제출용 release가 아닌 개발용 APK다.

M3 Pro에서는 실행 스크립트가 `-gpu host`를 명시한다. 창 없는 실행도 `scripts/run-pleos-emulator.sh -port 5556 -no-window -no-audio`로 실행한다. `-no-window`만으로 CPU 사용량이 줄어드는 것은 아니다. 2026-11-14 확인한 CPU 렌더링 전환 문제와 측정 결과, 일상 개발·SDK 검증 운영 방법은 [에뮬레이터 운영 안내](docs/IVI_DEMO_GUIDE.md#m3-pro--macos-에뮬레이터-운영)를 참고한다.

고정 조합: AGP 8.7.3 / Gradle 8.9 / Kotlin 2.0.21 / Compose BOM 2024.12.01 / compileSdk 35 / targetSdk 34 / minSdk 28.

## 로컬 개발 서버

기록 저장 API와 IVI 동기화: [실행 방법·API 계약](server/README.md), [구현·검증 상태](docs/V21_LOCAL_SERVER.md). 서버 첫 실행 후 debug APK를 빌드한다. 전체 v2.1.1의 남은 항목은 개발 계획에서 관리한다.

## release 빌드와 0.5.0 기준 산출물

`ivi/build/outputs/apk/release/ivi-release.apk`가 서명된 IVI APK다. 데스크톱 복사본은 `ChaPark-IVI-0.5.0-release.apk`와 `.sha256`, 실제 release 검증 영상은 `ChaPark-IVI-0.5.0-release-flow.mp4`다. release는 로컬 여행 저장으로 동작하며 개발 서버 토큰/동기화가 비활성화된다. SDK 내비 서비스와 차량 신호는 Pleos 환경이 필요하다.

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :ivi:assembleRelease :ivi:lintRelease
# 동일 서명의 release 대상 instrumentation APK
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew -PiviTestBuildType=release :ivi:assembleReleaseAndroidTest
```

서명키는 Git 밖의 `.local-signing/`을 사용한다. 다른 개발 환경에서는 자신의 서명 설정이 필요하다. Pleos의 새 사용자에서는 내비 앱을 한 번 열어 초기화한 뒤 경로 요청을 진행한다. 응답이 없으면 앱의 주소/재시도 안내를 따른다. 전체 네트워크 단절에서의 경로 검색이나 실차 기능을 보장하지 않는다.
