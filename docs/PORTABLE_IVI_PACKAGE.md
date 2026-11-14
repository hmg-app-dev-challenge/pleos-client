# IVI 0.5.5 macOS 실행 패키지

2026-11-15, 기존 서명 APK 0.5.5(8)를 소스 프로젝트 없이 설치·실행하는 ZIP을 만들었다. 앱 코드와 APK는 변경하지 않았다.

산출물: `~/Desktop/ChaPark-IVI-0.5.5-macOS-AppleSilicon.zip` (11,324,839 bytes). SHA-256과 구성 파일 목록은 [검사 결과](verification/portable-0.5.5/package-verification.json)에 기록했다.

## 사용

Apple Silicon Mac, macOS 13 이상, Android SDK의 emulator/platform-tools, Pleos Connect API 34 ARM64(42dot) 이미지가 필요하다. 에뮬레이터와 이미지는 ZIP에 포함하지 않는다. Java, Python, Gradle 및 프로젝트 소스는 실행에 필요하지 않다.

1. ZIP 전체를 푼다. `support` 폴더와 APK를 함께 보관한다.
2. `시작.command`를 실행한다. 기존 Pleos AVD를 찾거나 설치된 이미지로 AVD를 생성하고, 현재 Android 사용자에 APK를 설치한 뒤 앱을 연다.
3. 사용 후 `종료.command`로 정상 종료한다. 다시 사용할 때도 `시작.command`를 실행한다.
4. 잠깐 쉬는 동안은 `일시정지.command`, 돌아오면 `재개.command`를 사용한다.

SDK 자동 검색에 실패하면 압축을 푼 폴더에 `SDK_PATH.txt`를 만들고 첫 줄에 SDK 절대 경로를 적는다. 사용자별 데이터와 선택한 AVD 경로는 `~/Library/Application Support/ChaPark IVI Launcher/`에 저장한다. 새 AVD 생성은 여유 공간 10GB 이상을 요구한다.

macOS 다운로드 보안 확인과 Android USB 디버깅 승인이 필요할 수 있다. 네이티브 도구는 ad-hoc 서명이며 Developer ID 공증 배포본은 아니다. 세부 안내는 ZIP의 `사용안내.txt`를 따른다.

## 동작과 데이터

- 실행 중인 AVD를 재사용하고, 종료된 AVD는 기존 데이터 경로로 부팅한다. APK는 `pm install -r`로 교체하며 자동 삭제·데이터 초기화를 수행하지 않는다.
- 터미널 종료 뒤에도 에뮬레이터가 유지되도록 작은 네이티브 실행 도구로 프로세스를 분리한다.
- SDK ADB의 전역 키 접근 문제를 피하기 위해 로컬 TCP ADB 연결 도구를 동봉했다. 정상 ADB 인증을 사용하며 사용자 키 파일의 권한, 전역 ADB 서버는 변경하지 않는다. 개인 키는 ZIP에 포함하지 않는다.
- `-gpu host`, 스냅샷 비활성화로 부팅한다. 일시정지는 QEMU에 SIGSTOP, 재개는 SIGCONT, 종료는 SIGTERM을 사용한다. Java 빌드 데몬을 실행하지 않는다. 실행 중인 Pleos 자체의 CPU 사용을 없애는 것은 아니다.

## 검증과 한계

M3 Pro의 기존 Pleos AVD에서 공백이 있는 별도 경로로 ZIP을 풀어 다음을 확인했다.

- 기존 에뮬레이터 재사용, APK 설치 성공 및 IviActivity 전면 실행.
- 일시정지·재개·정상 종료 및 종료 후 콜드 부팅 → 재설치 → 앱 실행.
- 실행 커맨드 종료 뒤 QEMU가 독립 프로세스로 유지됨.
- 서명 APK 0.5.5와 SHA-256 일치, ZIP CRC, 다섯 셸 파일 구문과 실행 권한, 네이티브 도구 서명 및 잘못된 인자 처리.
- 검증 후 에뮬레이터를 정상 종료함.

[실행 로그](verification/portable-0.5.5/final-restart.txt), [전면 앱·버전 확인](verification/portable-0.5.5/restored.txt), [종료 로그](verification/portable-0.5.5/marker-stop.txt)를 남겼다. 전면 앱 로그에는 Pleos의 다른 디스플레이에 표시되는 차량 화면도 함께 포함된다.

새 AVD 자동 생성은 디스크 여유 부족으로 실제 부팅 검증하지 않았다. 기존 AVD 경로 재사용과 비파괴 설치는 확인했지만 앱 내부 여행 데이터 전체의 전후 일치 검사는 수행하지 않았다. 다른 Mac/macOS 13 실기기, 최초 USB 승인, 실차 기능은 별도 검증 범위다. 새 Pleos 사용자의 내비 초기화와 온라인 지도 서비스는 기존 안내를 따른다.

## 패키지 재생성

```sh
# 네이티브 도구 변경 시에만 실행: Xcode Command Line Tools 필요
scripts/build-portable-tools.sh
# 이미 빌드된 서명 APK와 실행 파일을 묶음. Gradle은 실행하지 않음
python3 scripts/package-ivi-macos.py
```

패키저는 검증한 0.5.5 APK 해시를 고정한다. 버전 변경 시 런처·안내·패키저의 버전과 해시를 함께 갱신하고 재검증한다. 네이티브 소스는 `tools/portable-adb/`, 배포 파일은 `distribution/macos/`에 있다.
