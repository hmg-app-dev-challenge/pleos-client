# MVP PRD v0.2 개발 실행 기록

2026-11-14 · `codex/v2.1.1-implementation` · 기준 계획: [V02-01~08](IVI_WORK_PLAN.md).

## V02-01A: 모델·저장 기반 및 취사 선택

- `CookingPlan`을 NONE/SIMPLE/COOKING으로 분리했다. 폰·IVI 조건 화면은 기존 Pleos 선택 버튼으로 세 값을 선택하며 요약 문구도 실제 선택값을 따른다. SIMPLE은 현장 취사 허가를 가정하지 않는 간편식 준비로 처리한다.
- IVI DataStore를 스키마 7로 올렸다. 형식 1~6의 취사 true는 COOKING으로 보존하되 재확인 대상으로 표시한다. 이전 진행 단계·내비 상태·같은 의미의 체크·완료 archive·삭제 표식을 유지한다. 새 형식의 알 수 없는 취사 값은 NONE+재확인으로 처리한다. 준비 화면에서도 바로 재확인할 수 있다.
- 준비 항목에 수량·필수 여부·category를 추가하고 PENDING/COMPLETED/OMITTED 상태를 구분한다. 필수 누락은 완료 수에 포함하지 않고 선택 항목은 필수 분모에서 제외한다. 누락은 체크와 동시 성립할 수 없으며 사건 기록과 다음 여행에 연결된다. **개별 누락 조작 UI와 PRD 준비 항목 전체 개편은 V02-05 후속이다.**
- 기존 내부 step에서 사용자 5단계를 해석하는 `JourneyStage`를 추가했다. 홈은 null이며 철수 화면 진입과 실제 completed 플래그를 분리했다. **현재 화면의 7단계 배치는 아직 유지되며 V02-03에서 통합한다.**
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
