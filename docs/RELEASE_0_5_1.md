# 차Park IVI 0.5.1 — 신호 대기 복구·새 AVD·오프라인 인수

2026-11-14 · versionCode 4 · 0.5.0 기능/저장 형식(schema 11)을 유지하는 수정 릴리스.

## 수정

UX 제한 응답을 기다리는 8초 안내가 먼저 수신한 P단 정보까지 초기화하던 경로를 수정했다. 대기 동안 조작 잠금은 유지하고 독립적인 기어 관측만 보존한다. 뒤늦게 UX 허용이 도착하면 P단과 함께 잠금이 풀리며, 기어 결측/비P/UX 제한일 때는 계속 잠긴다. 이미 도착한 UX 결정도 늦은 타이머가 덮어쓰지 않는다.

[정책 회귀](verification/mvp-v051/unit-tests.txt)는 shared 53/IVI 9개 통과. 추가된 2개는 신호 도착 순서·결측·이미 결정된 상태를 검사한다. 지연은 순수 정책 시험이며 실차 지연 재현 결과가 아니다. [release 빌드·Lint](verification/mvp-v051/build.txt), [최종 시험 APK/Lint](verification/mvp-v051/test-build.txt)도 통과했다.

## 새 Pleos AVD 검증

호스트 공간이 약14GB로 늘어 이전에 생성하지 못한 `ChaPark_Acceptance_050`을 새 userdata로 부팅했다. 기존 개발/업데이트 AVD의 데이터를 옮기지 않았다. 한 번에 AVD 하나, `-gpu host`, 4코어/4GB, snapshot 없는 부팅을 사용했다.

- 0.5.0 기준 APK: [기존 패키지 없음](verification/mvp-v02-clean-avd/before-install.txt), [저장 파일 없음/P단/SOC100 READ 3개](verification/mvp-v02-clean-avd/first-install-tests.txt), [실제 내비 포함 전체 여행 1개](verification/mvp-v02-clean-avd/online-flow-tests.txt) 통과. 초기화된 내비 앱을 사용했다.
- 0.5.1 최종 APK: 같은 새 AVD의 **시험 앱 데이터만** 초기화한 뒤 [초기 저장/P단 2개](verification/mvp-v051/fresh-storage-tests.txt), [전체 여행 1개](verification/mvp-v051/online-flow-tests.txt) 통과. 계획·준비·실제 안내·복귀/재생성·수면6/철수5·기록·Dark 완료까지 16.825초, 수동 초점 개입 없음.
- 내비 앱은 최초 사용/부팅 후 MainActivity를 열어 SDK 응답을 준비했다. 앱만 설치하면 이 초기화가 언제나 생략 가능한 것으로 보지 않는다.

## 실제 오프라인 실패

모바일 데이터/Wi-Fi를 끄고 Android의 `Active default network: none`을 확인했다. 전용 시험 AVD의 이전 시험 경로를 SDK로 취소하고 SAFE_GUIDING을 확인한 뒤 새 요청을 보냈다. 따라서 기존 안내가 남아 있는 것을 새 요청 성공으로 취급하지 않는다. 경로 취소는 명시 opt-in 시험에서만 실행되며 배포 앱은 운전자의 경로를 취소하지 않는다.

[경로 초기화 1개](verification/mvp-v051/offline-route-reset.txt), [최종 오프라인 시험 1개](verification/mvp-v051/offline-final-tests.txt) 통과. 실제 SDK 요청이 실패했을 때 다음을 확인했다.

- FAILED 상태·실패 기록, 미완료 유지.
- 난지 목적지·3명 조건·출발 준비 체크 전부 보존.
- 화면 재생성 후 실패 화면과 활성 재시도 버튼 유지.
- 시험 시작과 실패 확인 후 모두 기본 네트워크 없음 확인.

최종 오프라인 시험은 23.394초에 통과했다. 시험은 원래 활성 여행을 보관/복원한다. 이 결과는 실패 처리 인수이며 오프라인 경로 검색 성공을 뜻하지 않는다.

## 재연결의 제한

모바일 데이터·비행기 모드 원복 후에도 기본 네트워크가 none인 상태를 관측했다. cold boot 후에는 [기본 네트워크100과 0.5.1](verification/mvp-v051/after-cold-boot.txt)을 확인했고 온라인 전체 흐름이 다시 통과했다. 중간 실시간 복구 시도는 [SDK idle 관측을 받지 못해 시작 조건에서 중단](verification/mvp-v051/network-recovery-attempt.txt)됐다. 재부팅 없이 같은 실패 여행의 재시도로 안내가 복구되는 전체 경로는 미검증으로 남긴다. 원인을 앱 또는 Pleos 플랫폼 한쪽으로 단정하지 않는다.

[네트워크 설정](verification/mvp-v051/restored-network-settings.txt)은 원래 모바일 데이터1/Wi-Fi1/비행기모드 해제로 원복하고 AVD를 종료했다. Console heartbeat도 종료했다. 실차 권한/단절/EV 잔여시간 및 대표 장소 3곳의 운영·조망 근거는 [기존 인수의 남은 관문](MVP_V0_2_ACCEPTANCE.md) 그대로다.

## 산출물

- `~/Desktop/ChaPark-IVI-0.5.1-release.apk` — 26,339,482 bytes.
- `~/Desktop/ChaPark-IVI-0.5.1-release.apk.sha256`.
- 로컬 빌드 `ivi/build/outputs/apk/release/ivi-release.apk`, 제출 복사 `apk/차박차박_차Park_release.apk`는 동일 바이트.
- SHA-256: `95b31f5e26ab2f6f90978bdfb6b05a6bf68c74c2292518d717b3c76903ff9e08`.
- [v2 서명 검증](verification/mvp-v051/signature.txt), [해시](verification/mvp-v051/sha256.txt). 기존 release 인증서 유지, 개발 서버 토큰 없음 확인.

0.5.0 APK/영상은 원래 이름으로 유지한다. 0.5.1의 별도 발표/검증 영상은 만들지 않았으며 이전 영상을 새 APK의 증거로 이름만 바꾸지 않았다.
