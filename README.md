# 차Park — 내 차로 오늘 밤

차박 장소의 조건과 차량 배터리를 확인하고, 출발 준비부터 길 안내·차박·철수와 기록까지 이어주는 Pleos Connect 차량 앱입니다.

- **팀명:** 차박차박
- **앱 버전:** 0.5.5 (versionCode 8)
- **제출 APK:** [apk/차박차박_차Park_release.apk](apk/차박차박_차Park_release.apk)

## 실행 방법

1. Pleos Connect 에뮬레이터를 실행하고 위 APK를 설치합니다.
2. 기본 내비게이션 앱을 한 번 열어 초기화합니다.
3. 앱 목록에서 **차Park IVI**를 실행합니다.
4. 차박 계획 → 출발 준비 → 이동 → 차박 → 철수와 기록 순서로 이용합니다.

Android SDK의 `platform-tools`가 PATH에 설정되어 있다면 다음 명령으로 설치·실행할 수 있습니다.

```sh
adb install -r 'apk/차박차박_차Park_release.apk'
adb shell am start -n com.chapark.ivi/.IviActivity
```

## 테스트 환경 및 필요한 설정

| 항목 | 환경 |
| --- | --- |
| Pleos Connect 에뮬레이터 | **2.1.0**, Android API 34, ARM64 |
| Android Emulator | 35.2.10 |
| 화면 규격 | **2560 × 1440 px, 160 dpi**, 가로 화면 |
| AVD 설정 | 4 vCPU, RAM 4 GB, GPU host, cold boot |
| 개발 환경 | Apple M3 Pro, macOS 26.6.2 |

- 상세 조작에는 차량 **P단과 플랫폼 UX 허용 신호**가 필요합니다.
- 길 안내에는 **네트워크 연결과 초기화된 Pleos 내비 서비스**가 필요합니다.
- 여행 데이터는 기기에 저장됩니다. 별도 개발 서버나 토큰 설정은 필요하지 않습니다.

## 실행 확인

2026-11-15 제출 APK의 서명·해시, Pleos 에뮬레이터 설치·실행, 저장된 여행 복원과 홈 화면 전환을 확인했습니다.

복원된 이동 화면에서 ‘이동 정보를 불러오지 못했어요’ 안내가 관측되었습니다. 이번 확인은 내비 경로 성공이나 전체 여행 흐름 재검증을 포함하지 않습니다. [실행 검증 기록](docs/SUBMISSION_APK_VERIFICATION.md)
