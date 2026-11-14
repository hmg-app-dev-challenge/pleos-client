# 차Park IVI 0.5.0 인수 기록

2026-11-14 · versionCode 3 · 기획 v2.1.1 + MVP PRD v0.2 · Pleos 디자인 시스템 유지. **앱 구현·로컬 실행 가능한 서명 APK를 제공한다. 대표 장소의 모든 운영 근거와 실차 검증까지 완료한 상태는 아니다.**

> 후속 [0.5.1 변경·인수](RELEASE_0_5_1.md)에서 새 AVD와 실제 오프라인 실패 검증을 추가했다. 아래는 0.5.0 당시의 산출물과 검증 이력이다.

## 산출물

- 서명 APK: `ivi/build/outputs/apk/release/ivi-release.apk` (26,339,482 bytes)
- 데스크톱: `~/Desktop/ChaPark-IVI-0.5.0-release.apk`, 동일 이름의 `.apk.sha256`
- 제출용 로컬 복사: `apk/차박차박_차Park_release.apk` (바이너리는 Git 제외)
- 실제 release 자동 검증 영상: `~/Desktop/ChaPark-IVI-0.5.0-release-flow.mp4` (26.71초). 발표용 편집 영상이 아니라 실제 앱 전환·체크·완료의 검증 녹화다.
- APK SHA-256: `ea4a170bc163b94a849733a72de8b1a8f0f4fbf5129351cc672a91f78af75426`
- 서명: APK v2 검증 통과. 기존 0.4.0과 같은 로컬 release 키, 인증서 SHA-256 `82cb670f09da74d119056530179cf6c4b37388cfb785ec8d67101fcae18341bf`.
- [서명](verification/mvp-v02-release/signature.txt) · [파일 해시](verification/mvp-v02-release/sha256.txt) · [빌드](verification/mvp-v02-release/build.txt) · [마지막 문구 보완 후 빌드/Lint](verification/mvp-v02-release/final-build.txt)

release APK에는 개발 서버 토큰이 없음을 실제 ZIP 내용으로 검사했다. 로컬 키/토큰/AVD는 Git 제외다. release는 개발 서버 동기화가 비활성화되어 서버 없이 여행을 저장하고 완주한다. 내비 경로 검색까지 완전히 네트워크를 끊고 성공한다는 의미는 아니다.

## 구현 결과와 검증

| 범위 | 결과 | 증거·제한 |
|---|---|---|
| 빌드·규칙 | shared 53 + phone 7 + IVI 7 = 단위 67개, 서버 7개 통과. 폰 debug/IVI release 빌드·Lint 통과 | [단위](verification/mvp-v02-release/unit-tests.txt), [서버](verification/mvp-v02-release/server-tests.txt) |
| 저장 업그레이드 | 0.4.0(2) 위 0.5.0(3) 설치, 기존 파일 존재/550 bytes/step0 유지. schema 11 왕복 및 이전 1~10 fixture 검증 | [업데이트 전](verification/mvp-v02-release/upgrade-before.txt), [4개 시험](verification/mvp-v02-release/upgrade-tests.txt). 이 실제 이전 설치의 archive 수는 0. 완료 archive 보존은 별도 fixture 회귀로 검증 |
| 업데이트 환경 완주 | 3명·취사·SOC20/기준40·준비 13개 → 단독 Navi 안내 → 복귀/재생성 → 수면6/철수5 → 기록/다크 완료 | [release 전체 흐름](verification/mvp-v02-release/upgrade-flow-tests.txt), 수동 초점 개입 없음. 이 실행 뒤 최종 APK에는 진단 용어와 timeout 안내 문구만 추가 보완 |
| 데이터 없는 첫 설치 | 임시 Android 사용자11에서 기존 파일 없음/0 bytes, 기본 여행 생성과 P단 잠금 확인 | [새 사용자](verification/mvp-v02-release/fresh-user-tests.txt), [최종 APK 초기 저장](verification/mvp-v02-release/final-fresh-storage.txt). 기존 사용자10 데이터는 유지. 시험 사용자11은 종료 시 삭제되는 ephemeral 사용자 |
| 최종 APK 완주 | 새 사용자에서 내비 앱 초기화 후 전체 흐름 통과, 최종 SHA의 APK로 다시 통과 | [재시도](verification/mvp-v02-release/fresh-user-flow-tests.txt), [최종 26.524초](verification/mvp-v02-release/final-flow-tests.txt), [Light](verification/mvp-v02-release/completed-light.png), [Dark](verification/mvp-v02-release/completed-dark.png). 영상에서 추출한 실제 화면 |
| 차량 경계 | P단+UX 허용만 조작 가능. 실제 Console D단은 NOT_PARK/UX RESTRICTED. 종료/누락은 잠금 | [차량 15개](verification/mvp-v02-vehicle/device-tests.txt), [비P](verification/mvp-v02-vehicle/non-park-test.txt). P단을 속도·도착·주차 브레이크로 추정하지 않음 |
| SOC | READ capability, 실제20→37 subscription, 미지원/오류 수동 입력·상태/출처 분리·기준 명시 확인 | [구독](verification/mvp-v02-final/soc-subscription.txt), [저장](verification/mvp-v02-vehicle/capability-tests.txt). 화면은 ‘차량 연결’로 표시하고 내부 EMULATOR 출처는 유지 |
| 내비 경유 | 실제 단독/재연결 성공, 경유 이름+0,0 좌표는 자동 확정 차단 후 사용자 확인 | [SDK 경유](verification/mvp-v02-final/device-tests.txt). raw Unit 반환은 요청 호출 기록일 뿐 안내 성공 아님 |
| 화면/접근성 | 5단계·홈·자료·충전·Light/Dark/밤·800dp·글자1.3배 회귀 통과 | [최종 12개](verification/mvp-v02-final/metadata-ui-tests.txt), [큰 글자 계획](verification/mvp-v02-final/large-dark-plan.png). 스크롤 내용과 고정 CTA를 구분 |
| 예외 | 새 사용자 첫 Navi 요청이 30초 응답 없음으로 실패. 주소·준비/목적지 유지·재시도 안내 확인 | [실패 기록](verification/mvp-v02-release/fresh-user-flow-attempt.txt). 내비 앱을 한 번 열고 재요청해 성공. timeout 문구에 앱을 먼저 열도록 안내 추가 |

새 사용자 최초 저장 시험은 사용자 전환/잠금 해제가 끝나기 전에 실행되어 디렉터리 생성 오류가 났다. RUNNING_UNLOCKED를 확인한 후 **시험 사용자11의 앱 데이터만** 초기화하고 재실행해 `existed=false`를 확인했다. 이는 제품 저장 손상 복구 시험이 아니다.

별도 새 AVD는 호스트 여유 약 3.6GB 대비 userdata 생성에 약7.4GB가 필요해 생성하지 못했다. 새 사용자 첫 설치는 앱 데이터 격리 검증이며 새 시스템 이미지 전체 인수의 대체 증거로 표현하지 않는다. 검증은 한 번에 AVD 한 개, `-gpu host`, 4코어/4GB, snapshot 없는 cold boot로 수행했다. 새 사용자 내비 앱은 초기 실행이 필요했다. 최종 녹화는 Asia/Seoul에서 수행했다.

## AC-01~10과 남은 관문

AC-01~04 SOC·임계값·취사 규칙, AC-06 준비 필수 관문, AC-08 앱 복귀·보존, AC-09 밤 모드 제한, AC-10 기록은 해당 단위/기기 범위에서 통과했다. AC-05는 UNKNOWN·근거 만료·빈 결과·필터 구현을 검증했지만 대표 운영 조건의 실제 자료가 부족하다. AC-07은 단독 및 사용자 확인을 포함한 경유를 검증했으며 좌표 자동 대조는 에뮬레이터 한계가 남는다.

1. 대표 시나리오가 요구하는 서해안·야간 화장실·차내 숙박·취사 금지·바다 조망 조건이 모두 근거로 확인된 장소 3곳은 미확보다. 기존 4곳의 모르는 사실은 UNKNOWN, 추천 시간은 빈 목록, 방위는 null로 둔다. 충전 후보는 난지 지역 1곳이다.
2. 실차별 capability/권한·서비스 단절·주행 신호 전체 조합, 차량 위치/이동 시간, EV 잔여시간은 미검증이다. 직접 차량 제어나 미지원 EV 속성 접근을 확대하지 않았다. 예상 시간을 꾸며 표시하지 않는다.
3. 새 AVD 전체 흐름 및 완전한 네트워크 단절의 경로 오류 행렬은 미완료다. 서버 없이 로컬 앱 완주는 확인했다.
4. 외부 공개/스토어 업로드/접수 메일/기술지원 문의 발송은 수행하지 않았다. 이번 작업 산출물과 개발 브랜치 커밋을 제공한다.

## 재현

JDK: Android Studio JBR 21, AGP8.7.3/Gradle8.9/Kotlin2.0.21, compile35/target34/min28, Pleos API34 arm64, Vehicle2.3.2/NaviHelper3.0.5. 상세 실행은 [README](../README.md)와 [운영 안내](IVI_DEMO_GUIDE.md)를 따른다. 루트 소유 개인 adbkey를 바꾸지 않고 프로젝트 Python ADB 도구를 사용했다.

```sh
# 원래 개발 AVD. 같은 이름의 AVD를 중복 실행하지 않는다.
scripts/run-pleos-emulator.sh -port 5556
# release 대상 시험 APK 생성
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew -PiviTestBuildType=release :ivi:assembleReleaseAndroidTest
```

SDK 시험 전 Console의 `/browser/session` heartbeat로 활성 상태를 확인하고 SOC를 20으로 맞춘다. inactive HTTP503이면 값이 변경되지 않은 것이다. `console-api.py`는 비2xx를 실패로 종료한다. 새 사용자의 RUNNING_UNLOCKED와 내비 앱 초기 실행을 확인한다. 시험의 UI 대기/캡처 지연은 검증 APK에만 있고 배포 APK의 플로우에는 없다.
