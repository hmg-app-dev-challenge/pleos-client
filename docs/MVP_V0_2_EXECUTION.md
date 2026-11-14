# MVP PRD v0.2 개발 실행 기록

2026-11-14 · `codex/v2.1.1-implementation` · 기준 계획: [V02-01~08](IVI_WORK_PLAN.md).

## V02-01A: 모델·저장 기반 및 취사 선택

- `CookingPlan`을 NONE/SIMPLE/COOKING으로 분리했다. 폰·IVI 조건 화면은 기존 Pleos 선택 버튼으로 세 값을 선택하며 요약 문구도 실제 선택값을 따른다. SIMPLE은 현장 취사 허가를 가정하지 않는 간편식 준비로 처리한다.
- IVI DataStore를 스키마 7로 올렸다. 형식 1~6의 취사 true는 COOKING으로 보존하되 재확인 대상으로 표시한다. 이전 진행 단계·내비 상태·같은 의미의 체크·완료 archive·삭제 표식을 유지한다. 새 형식의 알 수 없는 취사 값은 NONE+재확인으로 처리한다. 준비 화면에서도 바로 재확인할 수 있다.
- 준비 항목에 수량·필수 여부·category를 추가하고 PENDING/COMPLETED/OMITTED 상태를 구분한다. 필수 누락은 완료 수에 포함하지 않고 선택 항목은 필수 분모에서 제외한다. 누락은 체크와 동시 성립할 수 없으며 사건 기록과 다음 여행에 연결된다. **개별 누락 조작 UI와 PRD 준비 항목 전체 개편은 V02-05 후속이다.**
- 기존 내부 step에서 사용자 5단계를 해석하는 `JourneyStage`를 추가했다. 홈은 null이며 철수 화면 진입과 실제 completed 플래그를 분리했다. **이 커밋에서는 화면의 7단계 배치를 유지했다. 아래 V02-03A에서 표시 단계를 통합했다.**
- 계획 API data schema 2는 cookingPlan/omitted/ruleVersion을 보존한다. schema 1 가져오기는 계속 지원하며 이전 취사 값은 재확인한다. 규칙 계약은 `mvp-v02-model-1`로 분리했고 비호환 캐시는 기존 정상 번들로 복귀한다. 서버는 형식 1/2를 모두 받으며 다른 취사 enum은 거절한다.
- 정책 기본값은 기존 호환값(인원 입력 상한 Int.MAX_VALUE, SOC 35%, 철수 10시)을 `TripPolicy`로 분리했다. 서버 가져오기의 20명 상한 불일치를 제거했다. 이는 D-01~03 최종 확정이 아니다. 사용자 SOC 기준 명시 확인 상태·시간 선택 정책은 V02-01 후속에서 다룬다.

## 검증 범위

JVM shared 31 + phone 7 + IVI 7 = 45개 통과. 새 계약 시험 6개는 취사 모드/필수·선택·누락/의미 변경/5단계 매핑/이전 취사 재확인을 검사한다. Python 서버는 이전 계약과 data schema 2 포함 6개 통과. 폰·IVI debug 빌드와 Lint 통과.

에뮬레이터 초기 묶음 시험은 ADB 60초 무응답으로 중단됐으며 통과 수에 포함하지 않는다. 당시 시스템 DrivingView 재시작이 반복 관측됐다. 기존 설정 RAM 4GB로 다시 부팅하고 저장 시험과 화면 시험을 분리했다. 사용자 저장 파일은 `.local-avd/backups/v02-model/`에 백업했고 Git에서 제외했다.

- [JVM 결과](verification/mvp-v02-model/unit-tests.txt)
- [서버 결과](verification/mvp-v02-model/server-tests.txt)
- [저장·마이그레이션·캐시 7개](verification/mvp-v02-model/storage-tests.txt)
- [화면 회귀 6개](verification/mvp-v02-model/ui-tests.txt): 이전 취사 재확인·체크 보존, 날짜 복구, Light/Dark/밤 글자색, 7단계 좁은 창. Pleos 런처에서 일시 정지된 테스트 Activity를 전경으로 복귀시킨 후 완료했으며 무인 실행 안정성까지 검증한 것은 아니다.
- [계획 API/기록 동기화](verification/mvp-v02-model/sync-tests.txt)

이번 결과는 새 PRD 전체 완료나 새 release 인수 결과가 아니다. 데스크톱 release APK는 이전 0.4.0 그대로다. 입력 정책 정리, 5단계 화면 통합, 장소별 충전소, 준비 누락 UI·전체 규칙, 차박/철수 항목 확장 및 새 release 대표 흐름은 남아 있다.

## V02-03A: 사용자 5단계와 통합 계획 화면

2026-11-14. 진행 표시·하단 단계·주행 요약은 JourneyStage 기반 5단계로 바꿨다. 홈에는 번호/진행 막대를 표시하지 않는다. 내부 step 1/2는 그대로 저장하면서 ‘차박 계획’ 안의 여행 조건/장소 비교 탭으로 전환한다. 탭 전환은 조건·장소·체크·기록을 초기화하지 않으며 주행/다른 단계에서는 거부한다.

새 여행을 홈에서 시작할 때 날짜가 없으면 기기 시간대의 오늘 날짜를 한 번 설정한다. 저장된 날짜를 복귀 때 덮어쓰지 않는다. 유효 일정과 취사 재확인이 끝나야 장소 확정으로 출발 준비에 들어간다. 준비·철수의 필수 관문은 유지한다.

계획 화면에서는 우측 중복 요약을 제거했다. 충분한 폭에서 3개 장소 요약 카드를 한 행에 표시하고 좁은 창에서는 같은 화면 안 가로 탐색을 사용한다. 선택 장소의 상세·출처만 아래에 표시한다. 주차/차내 숙박/야간 화장실/취사·풍경·확인일·출처를 비교하고, 아직 없는 장소별 충전 후보는 확인 필요로 표시한다. 기존 Pleos 토큰·IviButton·IviSegments를 재사용했다.

검증 파일은 `verification/mvp-v02-planning/`에 보관한다. 새 대표 흐름 release 검증, 별도 홈으로 돌아가 이어하기, 장소별 충전 자료, 준비 누락 UI는 아직 후속이다. 이번 변경을 V02-03 전체 완료나 PRD 전체 인수 완료로 보지 않는다.

검증: shared 32 + phone 7 + IVI 7 = JVM 46개 통과, IVI debug 및 시험 APK 빌드·Lint 통과. [계획 탭/3개 카드 배치/미완성 계획 차단/800dp Light·Dark 회귀 3개](verification/mvp-v02-planning/ui-tests.txt)가 통과했다. [넓은 창 비교 화면](verification/mvp-v02-planning/planning-wide.png)을 확인했다.

[전체 여행 debug 시험 1개](verification/mvp-v02-planning/flow-tests.txt)는 3명 조건 → 장소 확정 → 준비 → 실제 NaviHelper 안내 관측 → 앱 복귀/재실행 → 사용자 도착·주차 → 수면 확인 → 철수 완료·archive·재실행·다크 전환까지 통과했다. 시험 전 활성 여행을 보관하고 종료 후 복원한다. 최초 실행은 ADB 60초 무응답과 시스템 DrivingView 반복 ANR로 중단됐다. 데이터 삭제 없이 재부팅했고, 후속 실행의 테마 스위치 터치 시험은 재구성 중 노드 소멸로 실패해 의미 동작 호출로 변경했다. 최종 성공 실행에서 앱 task에 두 차례 초점을 복귀시켰으므로 완전 무인 실행 안정성이나 테마 스위치의 실제 터치 인수로 해석하지 않는다.

[실제 창의 계획 탭](verification/mvp-v02-planning/planning-runtime.png), [내비 복귀](verification/mvp-v02-planning/navigation-return.png), [다크 완료 화면](verification/mvp-v02-planning/completed-dark.png)을 보관했다. 이번 APK는 debug 검증본이며 기존 데스크톱 release를 교체하지 않았다.

## V02-03B: 홈과 이어하기

2026-11-14. 각 여행 화면의 홈 버튼과 시스템 뒤로가기는 여행 step·내비 상태를 변경하지 않고 홈 요약을 연다. 진행 중 여행의 장소·날짜·단계·조건·준비 진행·배터리 출처를 표시하고 `이어하기`로 원래 화면에 복귀한다. 홈에는 진행 번호가 없다. 완료한 여행은 `완료한 여행 보기`로 열 수 있다. `새 차박 시작`은 기존 계획 교체 확인 후 NewTrip을 적용하며 완료 archive를 유지한다. 실제 단계 수정은 기존 `이전 단계` 버튼으로 구분한다.

홈 화면 여부는 Compose 저장 가능한 화면 상태이며 여행 데이터 스키마를 바꾸지 않는다. 화면 재생성 복원을 검증했으며 프로세스 종료 후 홈 화면 자체 복원은 별도 인수 범위다. 주행 제한 중에는 홈/이어하기·새 여행 조작을 노출하지 않는다. 새 색상·서체 없이 Pleos 버튼·패널·테마를 재사용했다.

IVI debug/시험 APK 빌드·Lint 통과. [홈 3개와 800dp 양 테마 회귀 1개](verification/mvp-v02-home/ui-tests.txt): 내부 1~6 단계 왕복 시 이벤트 미발생·체크/누락/내비 보존, 저장 화면 복원, 주행 잠금, 완료 기록 재열기와 새 여행 확인을 검증했다. V02-03의 큰 글자/초점 전체 인수와 새 release 검증은 남아 있다.
