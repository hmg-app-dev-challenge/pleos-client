# 차Park 프로토타입 사용 안내

> 2026-11-13: IVI 0.4.0 산출물과 최신 사용 범위는 [최종 인수 기록](V2_1_1_ACCEPTANCE.md), [실행 기록](V2_1_1_EXECUTION.md)을 우선 참고하세요. 아래 이전 실행 흐름은 유지되며 장소/경유/기록/계획 동기화가 확장됐습니다.

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

## M3 Pro / macOS 에뮬레이터 운영

2026-11-14 실제 환경: M3 Pro 11코어·36GB, macOS 26.6.2(Darwin 25.6.0), Android Emulator 35.2.10, Pleos Connect 2.1.0/API 34 ARM64. Hypervisor.Framework 가속은 정상이다. AVD의 4 vCPU·4GB RAM·2560×1440 해상도를 유지했다.

기존 `-no-window -no-audio` 실행은 config.ini의 `hw.gpu.mode=host`에도 불구하고 실제 `swangle_indirect`로 실행됐다. 시작 로그와 macOS sample에서 ANGLE/SwiftShader의 CPU 렌더링을 확인했고, Pleos DrivingView(libUnreal)도 약 5초 간격으로 재시작했다. 실행 인자에 `-gpu host`를 명시하자 `Apple M3 Pro / 4.1 Metal` 렌더러가 선택됐고 DrivingView의 반복 충돌이 관찰 구간에서 사라졌다. 앱과 시스템 패키지를 비활성화하거나 화면 크기를 낮추지 않았다.

실행 스크립트에 이 기본값을 반영했다. 별도 `-gpu` 옵션을 직접 전달하면 해당 진단용 선택을 존중한다. `hardware-qemu.ini`는 생성 파일이므로 직접 수정하지 않는다.

```sh
# 화면을 보면서 개발: 종료는 이 터미널에서 Ctrl+C
scripts/run-pleos-emulator.sh -port 5556

# 자동 시험: GPU 가속은 동일, 창과 오디오만 생략
scripts/run-pleos-emulator.sh -port 5556 -no-window -no-audio
```

두 명령을 동시에 실행하지 않는다. 일반 모델·서버 코드 수정 중에는 AVD를 종료하고 JVM/서버 시험을 우선 실행한다. UI·SDK 검증을 시작할 때 한 대만 켜고, 반복 APK 설치는 기존 AVD에 `install -r` 방식으로 수행한다. 검증을 마쳤으면 종료한다. `-no-window` 자체는 VM 일시 정지나 절전이 아니며 Pleos의 3D DrivingView와 내비 렌더링은 계속된다.

Quick Boot로 전환하지 않는다. [Pleos 공식 설정](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/configure-pleos-connect-emulator)은 VHAL 사용을 위해 Cold Boot를 요구한다. RAM 4GB도 [공식 설치 설정](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)에 맞춰 유지한다. [Android 가속 문서](https://developer.android.com/studio/run/emulator-acceleration)는 ARM64 이미지·호스트 GPU 가속을 설명한다. Vulkan을 끄는 옵션은 [공식 그래픽 문제 해결](https://developer.android.com/studio/run/emulator-troubleshooting)에 소개돼 있지만 이번에는 필요하지 않아 적용하지 않았다. SwiftShader 전환·CPU 제한·RAM 축소·시스템 앱 비활성화·다른 VM 도입도 적용하지 않았다.

### 측정 및 검증

macOS `top`의 프로세스 CPU(100%=논리 코어 하나)를 사용했고 첫 번째 0% 표본은 제외했다. 변경 전 2초 간격 2개 표본은 780.1~954.2%, 평균 867.2%였다. 변경 후 앱 전경 7개 표본은 평균 244.1%, 추가 30초/10개 표본은 240.8~259.1%, 평균 246.4%였다. 관찰값 기준 약 72% 감소이며, 긴 시간 부하 시험이나 동일 장면을 재생한 벤치마크는 아니다. 기존 반복 충돌 상태와 정상 GPU 실행을 비교한 현장 진단이다. Pleos 시스템 전체를 구동하므로 CPU가 0%에 가깝게 내려간다는 의미는 아니다.

- [변경 전 CPU](verification/emulator-performance/before.txt), [변경 후 앱 전경](verification/emulator-performance/host-app.txt), [추가 30초](verification/emulator-performance/host-steady.txt), [실제 렌더러](verification/emulator-performance/renderer.txt).
- [기기 시험 5개](verification/emulator-performance/host-smoke.txt) 통과: SDK 연결 종료·해제, 기존 car library 조건 검사, 실제 SOC 100% 조회, NaviHelper 안내 시작/재연결/내비 화면 열기, 전체 내부 단계의 좁은 창 Light/Dark 배치. car library 부재 경로는 이 Pleos 이미지에서 실행하지 않는다. 수동 task 초점 복귀 없이 완료했다.
- [첫 전체 흐름 시도](verification/emulator-performance/initial-fixture-failure.txt)는 Cold Boot 후 SOC가 100%로 돌아와 기존 시험의 SOC 20% 전제(충전 검토 표시)에서 실패했다. GPU 변경 후 전체 여행 성공으로 계산하지 않는다. 전체 흐름 재실행 전 Virtual Console에서 시험용 차량 조건을 다시 설정해야 한다.
- 저장 파일을 `.local-avd/backups/performance-20261114/`에 보관하고 데이터 삭제 없이 재시작했다. 수정은 실행 스크립트와 문서에 한정한다. 폰/IVI APK와 앱 저장 스키마는 변경하지 않았다.

### 추가 CPU 대응 — 2026-11-14 저녁

`-gpu host` 상태에서도 장시간 실행 후 QEMU 173.7~205.3%, Android 내부 `ai.umos.drivingview` 약 103~111%를 관찰했다. 시스템 3D 차량 화면이 주요 지속 부하였다. DrivingView 비활성화 시 QEMU가 약 77~84%로 감소했지만 Pleos Home이 해당 Activity를 다시 실행하다 예외를 냈다. **이 방법은 채택하지 않았고 사용자 10의 패키지를 다시 활성화했다.** 정상 재부팅 후 DrivingView 실행, 내비 경로·화면 전환, 차Park 화면을 다시 확인했다.

현재 방식은 기능을 삭제하거나 프로세스에 강제 CPU 비율 제한을 거는 대신 **편집·빌드 중 일시정지, UI/SDK 확인 중 재개**다. `scripts/pleos-runtime.py`는 지정한 프로젝트 AVD의 QEMU 프로세스에만 SIGSTOP/SIGCONT를 보낸다. AVD 디스크 데이터와 메모리는 유지하지만 정지 중 화면·ADB도 멈춘다. 메모리 반환이 필요하면 정지 대신 정상 종료한다. 긴 정지 후 SDK 연결이 회복되지 않으면 데이터 삭제 없이 Cold Boot한다.

```sh
python3 scripts/pleos-runtime.py pause --avd ChaPark_Release
python3 scripts/pleos-runtime.py resume --avd ChaPark_Release
python3 scripts/pleos-runtime.py status --avd ChaPark_Release
```

데스크톱의 `Pleos 일시정지.command` / `Pleos 재개.command`는 같은 명령을 실행한다. 현재 실행 중인 Release AVD에 사용하며 종료한 AVD를 새로 만드는 명령은 아니다. 테스트 실행 도중에는 정지하지 않는다. 측정에서 정지 후 QEMU CPU는 세 표본 모두 0.0%였다. 이는 **실행 중 3D 부하 자체를 해결했다는 의미가 아니다.** [실행 중 측정](verification/release-0.5.2/cpu-active.txt), [정지 측정](verification/release-0.5.2/cpu-paused.txt).

### Android Studio와 ADB (기존 환경 제한)

현재 기본 ADB 35.0.2는 `~/.android/adbkey`가 root 소유·0600이어서 인증 초기화가 실패한다. 따라서 Studio용 기본 ADB의 `unauthorized`는 GPU와 별개다. 개인 키 삭제·교체·소유권 변경은 하지 않았다. `ADB_VENDOR_KEYS`와 `ANDROID_USER_HOME` 지정만으로는 이 버전의 사용자 키 초기화 실패를 해결하지 못했고 기본 서버 실행으로 되돌렸다. 이번 작업에서 Studio 중단점 연결까지 검증했다고 보지 않는다.

기존 프로젝트 키를 사용하는 독립 클라이언트로 설치·실행·로그·instrumentation은 검증했다. 이 경로는 Studio의 JDWP 중단점 디버거를 대체하지 않는다. Studio 연결이 필요하면 Mac 소유자가 기존 개인 키의 소유권을 확인·복구한 후 기본 ADB 인증을 별도로 점검해야 한다.

```sh
.local-avd/tools-venv/bin/python scripts/pleos-device.py shell 'logcat -d -t 300'
.local-avd/tools-venv/bin/python scripts/pleos-device.py shell 'dumpsys SurfaceFlinger | grep GLES'
```

## 범위와 검증

UI에서 배터리를 임의로 바꾸거나 내비 성공·실패를 주입하지 않는다. 조건 분기는 인원·조리·장소·침낭 등 실제 입력으로 확인한다. 주행 제한·SOC 미수신·내비 실패는 내부 모델 테스트에서 검증한다.

IVI는 실제 길 안내 요청을 전송한다. 도착은 SDK 도착 알림 또는 사용자 확인이며 주차·수면·철수 체크는 사용자 확인 기록이다. 차량 제어 결과가 아니다. SOC 등 일부 데이터는 로컬 fixture를 유지한다. 에뮬레이터의 지도 타일·자동 이동 재생 제한은 [내비 연동 문서](NAVIGATION_INTEGRATION.md)를 참고한다.

최신 검증 결과와 캡처는 [제품 흐름 계획](PRODUCT_FLOW_PLAN.md)의 검증 기록 및 `verification/product/`를 참고한다. 기존 `verification/prototype/`는 개편 이전 화면이다.
