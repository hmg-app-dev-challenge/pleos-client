# Pleos 폰 데모 적용 기록 — 2026-11-11

기준 파일: https://www.figma.com/design/zHQP956O40m7ra3Bobyr4H/Pleos-App-Market-Design-System?node-id=7-2

사용자의 폰 데모 요청에 따라 IVI의 픽셀 치수를 그대로 축소하지 않고, 원본의 색상·Asta Sans·형태·선택 상태를 유지하면서 모바일 치수를 별도로 적용했다. 차Park 화면 구성은 서비스에 맞게 만든 것이며 Figma에 존재하는 완성 화면을 복제한 것은 아니다.

| 원본 | 앱 적용 | 폰 치수 |
| --- | --- | --- |
| Foundations Color Light/Dark 21:2 / 21:529 | 배경, 텍스트, 구분선, 채움 버튼, 스위치, 선택 배경 | 원본 semantic 색상 |
| Asta Sans 21:1056 | 앱 내 Regular / Bold / ExtraBold | 제목 30/40sp, 섹션 22/28sp, 본문 16/24sp, 보조 13/18sp |
| Basic Button 16:293 / 16:569 / 16:592 | 기본·채움·누름·비활성 상태 | 최소 56dp, 반경 18dp, 줄바꿈 허용 |
| Switch 17:429 / 17:435 | 조건·주행 재현·테마 설정 | 54×34dp 시각 요소, 60×48dp 터치 |
| Checkbox 18:3350 / 18:3356 | 준비물 체크 | 28dp 상자, 48dp 터치, 원본 체크 SVG |
| Segmented Solid 18:3346 | 인원·배터리 선택 | 각 항목 최소 48dp, 외곽 반경 16dp |
| System Popup 18:1697 | 데모 설정 Dialog | 화면 너비에 맞춤, 반경 28dp, 내부 스크롤 |

7단계 전체에 같은 테마와 공통 컨트롤을 적용. 24dp 좌우 여백, 최대 520dp 콘텐츠 폭, 시스템 안전 영역, 단계별 스크롤 초기화. 원본의 라디오/음악/전화/키보드/차량 공조 전용 컴포넌트는 차Park 폰 흐름에 없으므로 넣지 않았다. 단계 진행 막대·카드는 앱 구성 요소이며 원본 컴포넌트라고 주장하지 않는다.

Asta Sans는 Google Fonts 공식 배포 파일을 앱에 포함했다. 출처: https://github.com/google/fonts/tree/main/ofl/astasans
라이선스: app/src/main/assets/licenses/AstaSans-OFL.txt (OFL 1.1).
체크 아이콘: Figma 18:3356의 다운로드 자산을 assets/figma/check.svg에 보관, AndroidSVG로 렌더링. 임시 원격 URL은 런타임에 사용하지 않는다. 선택적 버튼 prefix/suffix는 사용하지 않는다.

검증: assembleDebug / testDebugUnitTest / lintDebug / connectedDebugAndroidTest 성공. 단위 7개, UI 2개 통과. Lint 오류 0, 기존 버전 관련 경고 8개.

실제 차량·위치·날씨·내비 연결은 없는 로컬 데모다. 이 문서는 실제 Pleos 기기 인증이나 디자인 시스템의 모든 차량용 컴포넌트 구현 완료를 의미하지 않는다.

시각 확인: Android Studio의 Pixel 6 API 30에서 최종 시작 화면 렌더링과 설치 성공 확인. 원본 폰트 기본 축이 가늘게 렌더링되어 Regular를 wght=400으로 명시 후 재빌드·UI 테스트 재통과. 다른 단계의 동작은 자동 UI 테스트로 확인했으며, 전체 단계의 Light/Dark 시각 대조와 큰 글자·가로 화면 검수는 아직 완료하지 않았다. Galaxy S9 AVD는 실행 중 종료되어 검증에 사용하지 않았다.
