# Pleos 기술 지원 검토 요청안

상태: 미발송. 수신 후보는 공모전 공식 문의 창구 appdevchallenge@hyundai-partners.com이며 기술 지원 담당팀 전달을 요청한다.

제목: [차Park] AAOS EV 배터리 속성 직접 읽기 허용 범위 및 권한 검토 요청

안녕하세요. Pleos 차량 앱 차Park를 개발하고 있습니다. 기술 지원 담당팀에 아래 내용을 전달하거나 적절한 문의 채널을 안내해 주시면 감사하겠습니다.

Pleos API 호환성 및 가용성 문서에서 SDK가 제공하지 않는 특수 프로퍼티는 직접 구현 전 기술 지원 검토가 필요하다고 안내하고 있어 문의드립니다. 제품에 직접 접근을 구현하기 전, 읽기 전용 이용 가능 범위를 확인하려 합니다.

환경은 Pleos Connect Emulator 2.1, Android API 34 ARM64, Vehicle SDK 2.3.2, targetSdk 34입니다. 확인한 내용은 다음과 같습니다.

- EV_BATTERY_LEVEL(0x11600309)과 EV_BATTERY_INSTANTANEOUS_CHARGE_RATE(0x1160030c)가 에뮬레이터 VHAL에서 READ/CONTINUOUS로 등록되어 있습니다.
- 현재 앱은 pleos.car.permission.CAR_ENERGY가 허용됐으며 android.car.permission.CAR_ENERGY는 허용되지 않았습니다. 기존 권한 상태에서 두 속성을 읽으면 android.car.permission.CAR_ENERGY를 요구하는 SecurityException이 발생합니다.
- Vehicle SDK의 deprecated getEvBatteryLevel은 Float 77400.0을 반환하며 Google EV_BATTERY_LEVEL 사용을 안내합니다. 순간 충·방전률 대응 getter는 공개 EvBattery 인터페이스에서 찾지 못했습니다.
- 공유 라이브러리는 ai.pleos.playground.car로 등록되어 있고 현재 앱에서 android.car.Car 클래스를 로드할 수 있었습니다.

다음 항목의 검토를 요청드립니다.

1. 일반 서드파티 앱에서 위 두 속성을 CarPropertyManager로 직접 조회·구독해도 되는지, 개별 승인 또는 SDK 사용 신청이 필요한지요?
2. 허용 시 권장 SDK/라이브러리 선언과 정확한 권한, 사용자 런타임 동의 및 앱 마켓 심사 절차를 안내 부탁드립니다. Pleos 권한과 AAOS 권한의 관계도 확인 부탁드립니다.
3. 에뮬레이터와 실차 각각의 지원 범위, AVAILABLE/UNAVAILABLE 상태, 단위(Wh/mW), 부호, 타임스탬프와 갱신 주기 계약은 무엇인가요? deprecated getter의 77400.0도 Wh인가요?
4. Virtual Console에서 두 속성 값을 검증할 공식 방법이 있는지요? 에뮬레이터에서 등록만 된 속성과 실제 갱신을 제공하는 속성을 구분하는 방법도 부탁드립니다.
5. 직접 접근이 허용되지 않는다면 권장 대체 API 또는 SDK 제공 계획을 안내 부탁드립니다.

차량 값을 조회하는 용도이며 차량 제어는 요청하지 않습니다. 현재 제품 범위에서 도착 SOC, 밤새 공조 사용 가능 시간, 귀가 가능성을 보장하는 계산은 제외하고 있습니다. 권한 강제 부여나 시스템 앱 전환 없이 일반 앱에서 검증할 수 있는 절차를 부탁드립니다.

감사합니다.
