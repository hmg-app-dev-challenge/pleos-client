# IVI 0.5.2 — 내비 화면 전환과 준비 간격

2026-11-14 · versionCode 5 · 저장 스키마 11 유지.

## 변경

- `길 안내 시작`은 경로 요청뿐 아니라 SDK Foreground 요청과 설치된 Pleos 내비 앱의 공개 launcher를 실행한다. SDK가 초기화되지 않았어도 실패 화면의 `내비게이션 앱 열기`는 작동한다. 다른 내비 제공자는 기존 SDK 경로를 사용한다. 앱 재생성에 따른 관찰용 재연결은 화면을 빼앗지 않는다.
- 내비 화면이 열리는 것과 경로 안내 성공은 별개다. 실제 안내 상태와 목적지 일치를 확인한 경우에만 GUIDING을 저장한다. 경로 실패를 성공으로 바꾸거나 임의로 안내를 취소하지 않는다.
- 준비 조건을 일정 / 동행 / 식사·잠자리 / 배터리 카드로 분리했다. 제목 중복, 불규칙 패딩을 정리하고 기존 Pleos 버튼·폰트·색상으로 수치 조절을 정렬했다. [간격의 원본 근거와 앱 적용](IVI_LAYOUT_SPACING.md).
- [CPU 운영 지침](IVI_DEMO_GUIDE.md)에 일시정지·재개를 추가했다. 3D 앱을 끄는 실험은 홈 화면 오류 때문에 원복했다. 에뮬레이터를 상시 실행해서 생기는 부하는 일시정지로 피한다.

## 검증

- Release 빌드, lintRelease 성공. JVM IVI 9 + shared 53 = 62개, 실패 0.
- [실기기 계측 형식의 Pleos 에뮬레이터 내비 회귀 2개](verification/release-0.5.2/navigation-tests.txt): 표시된 3단계 CTA → 실제 내비 창, 실제 SDK GUIDING, Activity 재생성 시 차Park 유지, 다시 내비 열기, SDK 미연결 실패 상태에서 launcher 열기. Pleos 다중 창에서는 클러스터가 RESUMED일 수 있어 내비 task의 visible=true 및 차Park task의 숨김을 함께 검사한다.
- [준비 배치·조작 회귀 1개](verification/release-0.5.2/planning-tests.txt): 실제 IviActivity 안에서 넓은 창과 800×1000dp / fontScale 1.3, 라이트·다크를 검사했다. 4개 수치 컨트롤의 화면 내 배치·80dp 터치 높이, 실제 값 변경, 취사 선택, 하단 CTA 표시를 확인했다. 여행 저장값은 각 테스트 후 복원했다.
- [내비 안내](verification/release-0.5.2/navigation.png), [넓은 라이트](verification/release-0.5.2/planning-wide-light.png), [넓은 다크](verification/release-0.5.2/planning-wide-dark.png), [좁은 라이트](verification/release-0.5.2/planning-narrow-light.png), [좁은 다크](verification/release-0.5.2/planning-narrow-dark.png).
- 0.5.2를 기존 Release AVD에 데이터 삭제 없이 업데이트했다. 시스템 DrivingView 활성화를 확인했고 차Park를 전경에 둔 뒤 일시정지했다. 정지 후 CPU는 0.0%였다.

초기 장시간 실행 환경에서는 내비 서비스 연결 후 콜백이 오지 않아 안내 검증이 실패했고 정상 재부팅 후 통과했다. 최초 창 검증은 `topResumedActivity`만 확인해 화면이 보이는 다중 창 상태를 실패로 오판했으므로 task 가시성 판정으로 수정했다. 지도 배경 타일이 비어 있는 기존 내비 앱 제한은 남아 있다. 실차와 전체 여행 재인수, 장시간 일시정지 뒤 SDK 자동 복구를 이번 결과로 주장하지 않는다.

## 산출물

- `~/Desktop/ChaPark-IVI-0.5.2-release.apk` 및 `.apk.sha256`
- 로컬 제출 복사본 `apk/차박차박_차Park_release.apk`
- 26,339,522 bytes
- SHA-256: `7572795f8588dd8b01341d738943eb36dfd573bbcf6042b88d38b3de973c779d`
- 기존 release 키로 서명. 개발 서버 토큰 없는 release 구성 유지.
