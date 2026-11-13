# 차Park

처음 가는 차박 장소의 조건과 차량 배터리를 함께 확인하고, 준비·길 안내·철수를 ‘내 차와 보낸 하룻밤 기억’으로 연결하는 Pleos 차량 앱 프로토타입. 핵심 메시지는 **내 차로 오늘 밤**이다.

기획 기준은 **v2.1.1**, IVI 앱 버전은 `0.4.0`이다. 문서 개정은 전체 기능 구현 완료를 의미하지 않는다. [2.1.1 상세 변경 검토](docs/V2_1_1_CHANGE_REVIEW.md)와 [구현 현황](docs/IMPLEMENTATION_STATUS.md)을 참고한다.

## 최신 개발 기준

2026-11-14 첨부 MVP PRD v0.2를 [개발 계획](docs/IVI_WORK_PLAN.md)에 반영했다. [기능별 차이·결정 대장](docs/MVP_PRD_V0_2_REVIEW.md)과 [목표 5단계 흐름](docs/PRODUCT_FLOW_PLAN.md)을 따른다. 디자인은 기존 현대차 Pleos 시스템을 유지한다. 아래 0.4.0 구현·산출물은 아직 새 PRD 인수 완료본이 아니다.

## 현재 구현된 사용 흐름

시작 → 조건 확인 → 장소 비교 → 준비 계획 → 이동 → 도착·주차 확인 → 수면 준비 → 철수·완료.

폰과 IVI 모두 제품 언어로 구성했다. 개발용 배너·데이터 조작·SDK 진단 버튼은 사용자 화면에 없다. IVI는 선택한 장소를 NaviHelper로 경로 요청하고 차량 내비 화면을 연다. 폰은 로컬 이동 흐름을 유지한다. 도착은 SDK 알림 또는 사용자 확인, 주차는 사용자 확인으로 처리한다. 수면과 철수는 각각 네 항목을 확인해야 완료할 수 있다.

- [v2.1.1 제품 기획서](docs/PRODUCT_SPEC_V2_1.md)
- [v2.1.1 화면·사용자 흐름](docs/PRODUCT_FLOW_PLAN.md)
- [현재 구현 상태와 차이](docs/IMPLEMENTATION_STATUS.md)
- [사용 순서·실행 방법](docs/IVI_DEMO_GUIDE.md)
- [전체 IVI 작업표](docs/IVI_WORK_PLAN.md)

## 모듈과 데이터

- `:app`: 폰 UI, Pleos 색상·서체·선택 컨트롤을 모바일 크기로 적용. SavedStateHandle 복원.
- `:ivi`: 차량 UI, Pleos 원본 IVI 치수·Light/Dark·좁은 창 재배치. DataStore 영속 저장.
- `:shared`: 공통 여행 상태·조건별 준비 규칙·내부 fixture.

IVI 목적지는 공식 지도 좌표가 있는 중랑캠핑숲·난지캠핑장·강동그린웨이 가족캠핑장·몽산포다. 선택 충전 경유, 풍경/조건 필터, 근거 캐시와 실제/계산 사건 기억을 지원한다. 숙박·취사 등의 운영 조건은 미확인으로 처리한다. IVI의 실제 NaviHelper 경로 요청과 내비 화면 전환을 Pleos 에뮬레이터에서 검증했다. IVI SOC는 공개 Vehicle SDK 공급자에 연결했고 에뮬레이터 20→37→20 구독 변화를 확인했다. 폰 SOC·기온과 폰 장소는 내부 fixture이며 차량 설정 변경은 연결하지 않았다. 지도 배경 타일 표시·자동 이동 재생·실차 UX/권한은 별도 검증이 필요하다. [내비 연동 및 검증](docs/NAVIGATION_INTEGRATION.md)을 참고한다.

## 실행

Android Studio에서 이 폴더를 열고 폰은 `app`, Pleos는 `ivi`를 선택한다. SDK 경로는 `local.properties`, JDK는 Android Studio 내장 JBR을 사용한다.

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :shared:test :app:testDebugUnitTest :ivi:testDebugUnitTest :app:assembleDebug :ivi:assembleDebug :app:lintDebug :ivi:lintDebug
scripts/run-pleos-emulator.sh -port 5556
```

최신 서명 release와 검증 범위는 [인수 기록](docs/V2_1_1_ACCEPTANCE.md), 단계별 커밋은 [실행 기록](docs/V2_1_1_EXECUTION.md)을 참고한다.

실행 중인 AVD는 다시 실행하지 않는다. APK는 `app/build/outputs/apk/debug/app-debug.apk`, `ivi/build/outputs/apk/debug/ivi-debug.apk`다. 서명된 제출용 release가 아닌 개발용 APK다.

고정 조합: AGP 8.7.3 / Gradle 8.9 / Kotlin 2.0.21 / Compose BOM 2024.12.01 / compileSdk 35 / targetSdk 34 / minSdk 28.

## 로컬 개발 서버

기록 저장 API와 IVI 동기화: [실행 방법·API 계약](server/README.md), [구현·검증 상태](docs/V21_LOCAL_SERVER.md). 서버 첫 실행 후 debug APK를 빌드한다. 전체 v2.1.1의 남은 항목은 개발 계획에서 관리한다.
