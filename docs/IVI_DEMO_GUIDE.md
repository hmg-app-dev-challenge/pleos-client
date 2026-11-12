# 차Park 프로토타입 사용 안내

> 이 문서는 현재 설치된 기존 앱의 사용 안내다. v2.1 목표 흐름과 신규 기능의 개발 순서는 [제품 흐름](PRODUCT_FLOW_PLAN.md)과 [개발 계획](IVI_WORK_PLAN.md)을 따른다. 첫 개발 단계의 출발 체크 관문과 IVI 인원·배터리 기준·철수 시각 입력을 반영했다. 나머지 신규 요구 구현 완료를 뜻하지 않는다.

2026-11-12 · 0.3.0-prototype · 폰/IVI 공통

이 문서는 개발자·검토자용이다. 사용자 화면의 시연 표기와 개발용 상태 조작을 제거했다. 내부 데모 데이터와 미연결 범위는 [제품 흐름 계획](PRODUCT_FLOW_PLAN.md)에 기록한다.

## 기본 사용 순서

1. **오늘 밤 차박 시작**: 배터리와 여행 조건을 확인한다.
2. **장소 후보 비교**: 인원·조리·반려동물·침낭 조건을 정한다. IVI는 배터리 확인 기준과 다음 날 철수 시각도 선택한다.
3. **선택한 장소로 계획**: IVI는 중랑캠핑숲·난지캠핑장·강동그린웨이 가족캠핑장 중 선택한다. 폰은 기존 후보를 사용한다.
4. **준비 계획 확인 · 이동**: 출발 전 행동과 준비물을 모두 체크해야 이동할 수 있다. 현장 운영 안내는 도착 후 확인한다.
5. IVI에서 **길 안내 시작**: NaviHelper에 좌표를 전달하면 차량 내비가 열린다. 차Park로 돌아오면 **내비게이션 열기**로 안내를 다시 열 수 있다. 폰은 기존 **이동 시작** 흐름을 유지한다.
6. **도착했어요 → 주차했어요 → 수면 준비로**: 도착과 주차를 각각 확인한다.
7. 공조·조명·주변 조건 네 항목을 확인하고 **수면 준비 확인 · 다음 날**을 누른다.
8. 철수 네 항목을 확인한 뒤 **차박 종료 → 종료**로 기록을 마무리한다.

설정에는 어두운 화면과 새 여행 계획이 있다. `설정 → 새 여행 계획 → 처음부터`는 기본 조건으로 새로 시작한다. 완료 화면의 `새 여행 준비`는 사용자 조건과 테마를 유지한다.

## 설치·실행

Android Studio에서 폰은 `app`, Pleos는 `ivi` 모듈을 선택한다. 현재 환경의 Pleos 독립 ADB 클라이언트:

```sh
.local-avd/tools-venv/bin/python scripts/pleos-device.py install ivi/build/outputs/apk/debug/ivi-debug.apk
.local-avd/tools-venv/bin/python scripts/pleos-device.py shell 'am start --user 10 -n com.chapark.ivi/.IviActivity'
```

기기 테스트는 각 모듈의 `assembleDebugAndroidTest` 후 test APK를 설치하고 instrumentation runner를 실행한다. [SDK와 AVD 설치 기록](IVI_RUNTIME_VERIFICATION.md)은 초기 연결의 기술 기록이다.

## 범위와 검증

UI에서 배터리를 임의로 바꾸거나 내비 성공·실패를 주입하지 않는다. 조건 분기는 인원·조리·장소·침낭 등 실제 입력으로 확인한다. 주행 제한·SOC 미수신·내비 실패는 내부 모델 테스트에서 검증한다.

IVI는 실제 길 안내 요청을 전송한다. 도착은 SDK 도착 알림 또는 사용자 확인이며 주차·수면·철수 체크는 사용자 확인 기록이다. 차량 제어 결과가 아니다. SOC 등 일부 데이터는 로컬 fixture를 유지한다. 에뮬레이터의 지도 타일·자동 이동 재생 제한은 [내비 연동 문서](NAVIGATION_INTEGRATION.md)를 참고한다.

최신 검증 결과와 캡처는 [제품 흐름 계획](PRODUCT_FLOW_PLAN.md)의 검증 기록 및 `verification/product/`를 참고한다. 기존 `verification/prototype/`는 개편 이전 화면이다.
