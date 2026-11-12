# v2.1.1 구현 인수 기록

2026-11-13 · 앱 0.4.0 (2) · 브랜치 `codex/v2.1.1-implementation`.

## 통과한 범위

- JVM: shared 25, phone 7, IVI 7, 실패 0. [원문](verification/v211-integration/unit-tests.txt)
- Python API: 인증·재시도·revision·충돌·삭제·장소/규칙 계약 5개. [원문](verification/v211-integration/server.txt)
- 최신 Pleos 기기: 캐시 유효성·마이그레이션·계획 가져오기/재시도·기록 병합/삭제·설정/기록 색상·4단계 복구·7단계 좁은 창, 11개. [원문](verification/v211-integration/final-regression.txt)
- 대표 흐름 1개: 가족 3명, 날짜·조건 설정, 준비 체크, 실제 단일 목적지 Navi 안내, 재실행, 사용자 도착/주차, 수면·철수, 기억 저장·재실행·다크 전환. [원문](verification/v211-integration/full-flow.txt)
- Lint: IVI debug/release 오류 0, 경고 10. 고정 의존성/targetSdk 안내 및 `android.car.Car` 클래스 존재 여부 검사에 대한 PrivateApi 경고. 숨김 API 속성 접근은 구현하지 않았다.
- Release: 로컬 전용 RSA 3072 서명, v2/v3 검증. 별도 `ChaPark_Release` AVD의 새 설치, 사용자 10 실행·100% 에뮬레이터 SOC 수신 확인. 개발 AVD의 기록을 지우지 않았다. [서명](verification/v211-integration/release-signature.txt), [설치](verification/v211-integration/release-install.txt), [화면](verification/v211-integration/release-fresh.png)

전체 흐름 자동 시험과 영상은 debug 0.4.0이다. release는 동일 제품 코드에서 로컬 개발 서버 토큰/접속과 개발 전용 계획 가져오기를 제외한다. release 전체 내비/철수 자동 시험까지 통과했다고 확대하지 않는다.

## 디자인 대조

Figma 원본 `zHQP956O40m7ra3Bobyr4H`의 Medium Secondary Enabled `16:569`, Disabled `16:615`를 다시 조회했다. 높이 80, 반경 24, 가로 여백 32, 최소 너비 120, Asta Sans Bold 30/38, 비활성 테두리 2와 기존 `IviButton` 구현을 대조했다. 두 원본 상태의 prefix/suffix는 false이며 보이는 아이콘 자산은 없다. 폰용 치수를 IVI에 대입하지 않았다.

기억 카드와 시간 배경은 앱 자체 확장이다. 라이트/다크/따뜻한 밤의 팝업·입력·선택 색상과 800dp 창 하단 동작은 테스트했다. 전체 Figma 컴포넌트 구현이나 모든 접근성 크기 완료를 주장하지 않는다. 에뮬레이터의 날짜·시간대가 여행 날짜/표시에 반영되므로 계획에서 확인해야 한다.

## 외부 계약과 미완료 인수 항목

1. SDK 경유지는 이름이 일치해도 좌표 0,0을 반환했다. 자동 성공으로 바꾸지 않고 내비에서 위치 확인 후 사용자 확인으로 진행한다. [실패 원문과 fallback](V2_1_1_EXECUTION.md). 충전기 가용성·커넥터·충전시간을 보장하지 않는다.
2. 경로 시간 단위/경유 포함 여부와 충전 시간 계약, 현장 조망 방위가 미확보라 정확한 출발 시각을 만들지 않는다. 계산 시각·권장만 제공한다.
3. Place 필수 조건이 모두 확인된 대표 후보와 현장 조망 자료가 없다. 조건 확인 필터의 빈 결과는 정상이다. 체크 완료가 장소 허용 증명이 되지 않는다.
4. Console Park→Drive→Park의 플랫폼 UXR은 0→80→0이었다. Drive에서 플랫폼은 내비 화면을 노출했다. 이것을 앱 화면 전체의 실제 주행/서비스 단절 인수 통과로 보지 않는다. [Drive](verification/v211-integration/ux-drive.txt), [Park](verification/v211-integration/ux-park.txt)
5. 배터리 잔여시간은 기술지원·권한·실차 표본이 없어 보류한다. 문의는 발송하지 않았다. [문의 초안](PLEOS_EV_SUPPORT_REQUEST.md)
6. 외부 배포/계정/QR·인터뷰·My Project/스토어 승인·실차 인수는 하지 않았다. 서버 계획 조건은 새 ID로 가져오며 이전 진행/현장 체크를 자동 재개하지 않는다.

## 산출물과 재빌드

- `/Users/woongaaaa/Desktop/ChaPark-IVI-0.4.0-release.apk`
- `/Users/woongaaaa/Desktop/ChaPark-IVI-0.4.0-flow.mp4` (debug 흐름 녹화)
- 해시: [artifact manifest](verification/v211-integration/artifacts.json)

서명 키와 암호는 `.local-signing/`에만 보관하며 Git에 넣지 않는다. 이 키는 로컬 배포용이고 공식 발급/승인 키가 아니다. 향후 같은 앱 업데이트에 필요하므로 별도 안전 보관한다. 현재 debug 앱과 서명이 달라 같은 기기에 바로 업데이트 설치할 수 없으므로 기존 기록이 있는 앱을 임의 삭제하지 않는다. 새 AVD는 `CHAPARK_AVD_NAME=ChaPark_Release scripts/run-pleos-emulator.sh -port 5558`로 분리했다.

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :shared:test :app:testDebugUnitTest :ivi:testDebugUnitTest :ivi:assembleRelease :ivi:lintRelease
python3 -m unittest discover -s server -v
```
