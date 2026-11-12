# 차Park

차량·장소·사용자 조건을 준비 행동으로 연결하는 Android 앱 프로토타입. 현재 버전은 `0.3.0-prototype`이다.

## 현재 사용 흐름

시작 → 조건 확인 → 장소 비교 → 준비 계획 → 이동 → 도착·주차 확인 → 수면 준비 → 철수·완료.

폰과 IVI 모두 제품 언어로 구성했다. 개발용 배너·데이터 조작·SDK 진단 버튼은 사용자 화면에 없다. 이동 시작 후 로컬 정보 준비를 자동 처리하며, 도착·주차는 사용자가 확인한다. 수면과 철수는 각각 네 항목을 확인해야 완료할 수 있다.

- [현재 개발 계획·구현 범위](docs/PRODUCT_FLOW_PLAN.md)
- [사용 순서·실행 방법](docs/IVI_DEMO_GUIDE.md)
- [전체 IVI 작업표](docs/IVI_WORK_PLAN.md)

## 모듈과 데이터

- `:app`: 폰 UI, Pleos 색상·서체·선택 컨트롤을 모바일 크기로 적용. SavedStateHandle 복원.
- `:ivi`: 차량 UI, Pleos 원본 IVI 치수·Light/Dark·좁은 창 재배치. DataStore 영속 저장.
- `:shared`: 공통 여행 상태·조건별 준비 규칙·내부 fixture.

장소·SOC·거리·기온은 내부 데모 데이터다. 실제 길 안내·위치 추적·차량 설정 변경은 연결하지 않았다. Vehicle/NaviHelper 초기화와 SOC 읽기 검증은 사용자 화면 밖의 기기 테스트에 유지한다. 원시 SOC 단위·실제 UX 신호·검증 장소·실제 내비·배포 승인은 후속 관문이다.

## 실행

Android Studio에서 이 폴더를 열고 폰은 `app`, Pleos는 `ivi`를 선택한다. SDK 경로는 `local.properties`, JDK는 Android Studio 내장 JBR을 사용한다.

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :shared:test :app:testDebugUnitTest :ivi:testDebugUnitTest :app:assembleDebug :ivi:assembleDebug :app:lintDebug :ivi:lintDebug
scripts/run-pleos-emulator.sh -port 5556
```

실행 중인 AVD는 다시 실행하지 않는다. APK는 `app/build/outputs/apk/debug/app-debug.apk`, `ivi/build/outputs/apk/debug/ivi-debug.apk`다. 서명된 제출용 release가 아닌 개발용 APK다.

고정 조합: AGP 8.7.3 / Gradle 8.9 / Kotlin 2.0.21 / Compose BOM 2024.12.01 / compileSdk 35 / targetSdk 34 / minSdk 28.
