# IVI NaviHelper 연동

2026-11-12 · Pleos Connect 2.1 / API 34 ARM64 에뮬레이터 · NaviHelper 3.0.5

## 구현 범위

1. IVI 장소를 공식 좌표가 있는 중랑캠핑숲·난지캠핑장·강동그린웨이 가족캠핑장으로 구성했다. 좌표와 출처는 `NavigationPlaces.kt`에서 관리한다. 좌표는 지도 지점이며 차량 진입구를 검증한 것은 아니다. 숙박·취사·화기·반려동물·야간 화장실 조건은 미확인이다.
2. `NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`, `NAVI_CUSTOM_MAP` 권한으로 NaviHelper를 연결한다. `RouteInfo`에 실제 위도·경도·이름·주소를 전달한다. POI 식별자는 빈 문자열로 전달하여 다른 POI의 좌표가 우선되는 일을 피한다.
3. 서비스의 목적지 좌표가 요청 좌표 주변(위도·경도 각각 0.002도 미만)에 있고 안내 상태가 활성일 때만 성공 처리한다. 예제 POI나 고정 지연으로 성공을 만들지 않는다. 경로 완료 콜백만 기다리지 않고 목적지·안내 상태를 2초마다 조회한다.
4. 에뮬레이터는 경로 요청 후 내비 앱으로 자동 전환한다. 차Park로 돌아온 뒤 `내비게이션 열기`는 `NavigationScreenRequest.Foreground`를 사용한다.
5. 실패·권한 없음·약관 미동의·30초 응답 초과는 재시도 가능한 메시지로 표시한다. 다른 목적지로 변경되거나 안내가 종료되면 기존 성공 표시를 해제한다. 세션 종료 후 이전 콜백은 무시한다.
6. 저장된 안내 중 여행은 새 경로를 전송하지 않고 현재 목적지·상태를 다시 확인한다. 차Park를 닫거나 여행을 초기화해도 운전자의 내비 안내를 임의로 취소하지 않는다.
7. SDK 도착 알림을 받거나 사용자가 `도착했어요`를 누르면 도착으로 전환한다. 주차는 반드시 별도 확인한다. 수면·철수 체크와 제품 언어는 유지했다.

## 검증

- 최종 IVI 기기 테스트 8개 통과: 실제 경로 요청·재연결, 전체 여행, 저장, 좁은 창 양 테마, 기존 SDK 프로브 포함. [최종 기기 테스트](verification/navigation/ivi-tests.txt)
- 실제 난지캠핑장 경로 요청: `GUIDING`, 목적지 좌표 일치 확인. [기기 테스트](verification/navigation/camp-route-test.txt)
- 앱의 시작 → 조건 → 장소 → 준비 → 실제 경로 요청 → 내비에서 복귀 → 도착 → 주차 → 수면 → 철수 → 완료 및 Activity 재생성 흐름 통과. [전체 흐름 테스트](verification/navigation/journey-flow-test.txt)
- 공통/폰/IVI JVM 테스트 21개 통과. IVI Lint 오류 0, 기존 경고 9.
- [차Park 안내 중 화면](verification/navigation/chapark-guiding.png)
- [내비 화면 캡처](verification/navigation/camp-navigation.png): 난지캠핑장, 회전 안내, 경로선, 내비가 표시하는 거리·예상 시간 확인.
- 개발용 시연·데모·가상·재현·SDK·미연동 문구가 여행 화면에 없는지 기기 테스트에서 검사한다.

## 확인된 제한

현재 에뮬레이터의 내비는 경로선과 회전 안내를 그리지만 배경 지도 타일이 회색으로 표시된다. 지도 제공 앱의 렌더링·데이터 환경 문제는 아직 원인을 특정하지 않았다. 차Park의 안내 성공은 지도 타일 로딩 성공을 의미하지 않는다.

공개 NaviHelper의 자동 모의주행 시작 API는 확인되지 않았다. 차량 위치가 움직이며 안내가 진행되는 자동 재생은 구현·검증하지 않았다. 실제 도착 이벤트는 코드에 연결했으나 이번 검증의 도착은 사용자 확인 경로다.

SOC·예상 기온 등 기존 로컬 fixture와 폰의 로컬 이동 흐름은 유지한다. 내비 자체 화면의 배터리·도착 예상 정보는 차Park의 실차 센서 검증 결과가 아니다. 실차 UX 제한·서비스 가용성·운영 권한 및 캠핑장 이용 조건은 별도 확인이 필요하다.

## 공식 SDK 문서

- [NaviHelper 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro)
- [목적지 요청](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations)
- [권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/declaring-permissions)
- [이벤트](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/event-listeners)
- [오류 처리](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/error-handling)
