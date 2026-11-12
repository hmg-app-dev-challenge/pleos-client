# IVI 2차 실행 검증 — 2026-11-11

> 최신 상태 — 2026-11-11: IVI 7단계 시연 프로토타입 완성. DataStore 저장, 조건별 행동, 내비 실패/재시도·주차 분리, 수면·철수 수동 확인, Pleos 디자인 컴포넌트를 구현했다. JVM 19개·기기 7개 통과. [시연 안내·실제 연결 범위·증거](IVI_DEMO_GUIDE.md)를 현재 기준으로 사용한다. 아래는 단계별 이전 검증 기록이다.


## 확인 결과

| 항목 | 관측/판정 |
| --- | --- |
| 기기 | 설치된 Pleos Connect 2.1.0 API 34 arm64 이미지, 프로젝트 전용 AVD |
| 화면 | 전체 2560×1440, 160dpi; 현재 앱 창 캡처 1682×1210 |
| 앱 | com.chapark.ivi, debug; 설치·실행 확인 |
| Vehicle | SDK 2.3.2, 설치 서비스 2.3.22; 실제 응답 READY |
| SOC | capability READ, 원시 Int 100 수신; 단위 미확인, %/에너지 예측에 사용하지 않음 |
| NaviHelper | SDK 3.0.5, 초기화 READY; 경로 요청·안내·도착은 미구현 |
| 주행 제한 | android.car 공유 라이브러리 없음. ai.pleos.playground.car 존재. UNKNOWN으로 상세 조작 잠금 |
| 테스트 | JVM 5개 + Pleos 기기 3개; 결과 파일 참조 |
| Lint | 오류 0, 경고 7: 고정 의존성·targetSdk 최신 버전 안내 |

[기기 테스트 원문](verification/ivi/device-tests.txt). SDK 프로브는 기기 테스트 하네스에서 읽기만 수행한다. 잠긴 제품 UI의 제한을 해제하거나 공조·조명·내비 설정을 변경하지 않는다. 에뮬레이터에서 읽기에 성공했다는 사실은 My Project 배포 승인이나 실차 지원 증거가 아니다.

## 구현 및 수정

- `UxPolicy`: 신호 미확인/최적화 필요/제한 비트 존재 시 상세 조작 잠금. UX 허용을 주차·도착으로 변환하지 않음.
- `UxMonitor`: 플랫폼 지원 검사 후 AAOS 어댑터 생성; 미지원이면 UNKNOWN 유지. 화면 종료 시 해제. 이전 세션과 이전 서비스 연결의 지연 콜백 차단.
- `CarUxMonitor`: 현재 제한 + 변경 리스너 구현. 실제 AAOS 지원 플랫폼에서 서비스 중단/재연결/주행 변화 검증은 후속.
- `android.car` 필수 선언으로 Pleos 설치가 실패한 것을 확인하고 선택적으로 변경. 현재 설치 이미지에서의 원인을 기록하며 모든 Pleos 버전에 일반화하지 않음.
- `IviButton`: Large 100/32, Medium 80/24; Asta Sans Bold 32/40 또는 30/38; 기본/채움, 눌림, 비활성 색상과 2dp 테두리. 비활성 intrinsic 너비에 양쪽 테두리 폭 반영. 긴 문구 한 줄 말줄임.
- `ButtonCatalog`: Light/Dark, 크기·타입별 활성/비활성, 눌림·긴 문구 검수. 일반 진단 화면에서는 UX 허용 상태에서만 진입. 독립 기기 테스트에서는 카탈로그만 렌더링.

## 디자인 대응 및 시각 확인

원본: [Pleos 디자인 시스템](https://www.figma.com/design/zHQP956O40m7ra3Bobyr4H/Pleos-App-Market-Design-System?node-id=7-2).

| 구현 | 확인 노드 |
| --- | --- |
| Large Primary Disabled | 16:339 |
| Large Secondary Disabled | 16:546 |
| Medium Secondary Enabled / Disabled | 16:569 / 16:615 |
| 기존 Large Primary / Secondary | 16:293 / 16:500 |
| Light/Dark semantic colors | docs/pleos-source-colors.json, 원본 21:2 / 21:529 |

옵션 아이콘 prefix/suffix는 원본 기본값 false이며 이번 버튼 호출에서도 사용하지 않는다. 표시되는 정적 이미지 자산은 없다. Asta Sans는 기존 로컬 OFL 폰트를 재사용한다. 캡처에서 비활성 테두리/글자 농도와 버튼 치수를 대조했고 Light/Dark 및 긴 문구 스크롤을 확인했다. 전체 디자인 시스템, 모든 pressed 조합, 작은 창·큰 글자 검수 완료를 의미하지 않는다.

- [Light 버튼](verification/ivi/buttons-light.png)
- [눌림 상태](verification/ivi/buttons-pressed.png)
- [Dark 버튼](verification/ivi/buttons-dark.png)
- [Dark 하단·긴 문구](verification/ivi/buttons-dark-bottom.png)
- [Pleos 시스템 안의 진단 잠금 화면](verification/ivi/pleos-locked-screen.png)

## 실행 재현

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :ivi:assembleDebug :ivi:assembleDebugAndroidTest :ivi:testDebugUnitTest :ivi:lintDebug
./scripts/run-pleos-emulator.sh -port 5556
```

AVD/개발 키는 `.local-avd`에만 저장하고 버전 관리에서 제외한다. 초기 이미지 userdata 크기로 인해 실제 가상 디스크 크기는 설정값보다 클 수 있다. 기존 폰 AVD·사용자의 ADB 키는 수정하지 않았다.

현재 Mac의 기존 `~/.android/adbkey`는 root 소유·읽기 불가로 기본 ADB 서버가 인증 초기화에 실패했다. 이 환경에서는 전용 키를 지정하는 격리 클라이언트로 실행한다:

```sh
python3 -m venv .local-avd/tools-venv
.local-avd/tools-venv/bin/pip install adb-shell==0.4.4
.local-avd/tools-venv/bin/python scripts/pleos-device.py install ivi/build/outputs/apk/debug/ivi-debug.apk
.local-avd/tools-venv/bin/python scripts/pleos-device.py install ivi/build/outputs/apk/androidTest/debug/ivi-debug-androidTest.apk
.local-avd/tools-venv/bin/python scripts/pleos-device.py shell 'am instrument -w com.chapark.ivi.test/androidx.test.runner.AndroidJUnitRunner'
.local-avd/tools-venv/bin/python scripts/pleos-device.py shell 'am start -n com.chapark.ivi/.IviActivity'
```

첫 연결은 새 AVD 안의 USB 디버깅 승인 창에서 이 프로젝트 키를 승인해야 한다. 승인 없이 진행하지 않는다. adb-shell 0.4.4의 큰 FileSync 패킷 정체는 64KiB로 제한해 해결했으며, 설치 임시 파일은 매번 고유 이름을 사용한다. Pleos 활성 사용자는 이번 실행에서 10이므로 캡처는 `pull-app-file ... --user 10`으로 읽는다. 다른 기기는 `am get-current-user` 결과를 사용한다.

## 다음 우선 확인

1. [공식 주행 규제 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)가 요구하는 CarUxRestrictions 접근과 설치 이미지의 라이브러리 연결 계약 대조. 현재 UNKNOWN을 속도 0이나 화면의 P 표시로 해제하지 않음.
2. SOC 단위의 공식 근거 및 구독/60초 stale/연결 중단 기기 검증.
3. 스위치·체크박스·분할 선택·팝업 상태 구현과 작은 창 검수. 여행 공통 모델을 분리한 뒤 S1~S4와 저장을 연결.

참조: [공식 에뮬레이터 설정](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator), [AAOS UX API](https://developer.android.com/reference/android/car/drivingstate/CarUxRestrictionsManager), [개발용 adb-shell](https://github.com/JeffLIrion/adb_shell).
