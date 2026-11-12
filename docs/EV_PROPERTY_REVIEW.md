# EV 에너지 속성 검토

2026-11-12 · Pleos Connect 2.1 / Android 34 / Vehicle SDK 2.3.2 · 현재 설치 앱 com.chapark.ivi, user 10.

## 확인 결과

| 항목 | EV_BATTERY_LEVEL | EV_BATTERY_INSTANTANEOUS_CHARGE_RATE |
| --- | --- | --- |
| Android 속성 ID | 0x11600309 / 291504905 | 0x1160030c / 291504908 |
| 공식 타입·단위 | Float, Wh. SOC 퍼센트와 다름 | Float, mW. 양수 충전·음수 방전 |
| 에뮬레이터 VHAL 등록 | access=1 READ, changeMode=2 CONTINUOUS, 1~100 Hz 설정 | access=1 READ, changeMode=2 CONTINUOUS, 1~10 Hz 설정 |
| 직접 읽기 권한 | android.car.permission.CAR_ENERGY | android.car.permission.CAR_ENERGY |
| 현재 앱 직접 조회 | SecurityException: requires android.car.permission.CAR_ENERGY | 동일 SecurityException |
| 앱에서 config 조회 | null. 권한 필터링 가능하므로 미지원 증거로 해석하지 않음 | 동일 |
| Pleos SDK 대응 | getEvBatteryLevel 존재, deprecated; Google 속성 사용 안내 | EvBattery 공개 인터페이스에 대응 getter 확인되지 않음 |
| 실제 수신 값 | Pleos getter에서 Float 77400.0 수신. 직접 AAOS 값은 미수신 | 미수신. 0 또는 미지원으로 처리하지 않음 |

Google 계약상 EV_BATTERY_LEVEL은 Wh이며 77400 Wh라면 77.4 kWh에 해당한다. 이번 SDK 원시값을 같은 단위로 제공한다는 런타임 대조까지 완료한 것은 아니므로 제품의 에너지 계산에는 아직 투입하지 않는다. 순간 전력만으로 밤새 사용 가능 시간이나 귀가 가능성을 보장하지 않는다.

현재 앱에는 `pleos.car.permission.CAR_ENERGY`가 허용돼 있지만 `android.car.permission.CAR_ENERGY`는 허용되지 않았다. 두 권한은 별개다. Android 공식 계약에서 후자는 dangerous 권한이다. 이번 검증에서 manifest 권한을 추가하거나 권한을 강제로 부여하지 않았다. 진단 테스트의 OK는 진단 완료를 의미하며 속성 두 개의 읽기 성공을 뜻하지 않는다.

`pm list libraries`에는 `android.car` 대신 `ai.pleos.playground.car`가 등록돼 있다. 그러나 현재 앱 프로세스에서 `android.car.Car` 로드와 CarPropertyManager 획득은 성공했다. 따라서 기존의 라이브러리 이름만으로 AAOS 클래스 접근이 불가능하다고 단정할 수 없다. 기존 UX 어댑터의 보수적인 잠금은 이번 조회에서 변경하지 않았다.

## 기술 지원 확인 상태

공식 Pleos 가용성 문서는 SDK에서 제공하지 않는 특수 프로퍼티를 직접 구현하기 전에 기술 지원팀의 검토를 요청하도록 명시한다. 이 문구는 직접 접근에 대한 포괄적 승인이나 금지가 아니다. 앱의 현재 권한으로 수행한 읽기 진단과 제품에 직접 접근 기능을 구현·배포하는 것은 구분한다.

기술 지원팀의 개별 검토 답변은 아직 받지 않았다. 공모전 공식 문의 주소 `appdevchallenge@hyundai-partners.com`을 확인했으며, Pleos 기술 지원팀에 전달/담당 채널 안내를 요청할 [문의안](PLEOS_EV_SUPPORT_REQUEST.md)을 작성했다. 해당 주소가 Pleos 기술 지원팀 자체 주소라고 확인된 것은 아니다. 외부 메일은 아직 발송하지 않았다.

검토 답변 전에는 제품의 직접 접근 어댑터·추가 AAOS 권한·순간 전력 기반 시간 예측을 도입하지 않는다. 다음 검증은 지원팀이 안내한 라이브러리·권한 절차로 일반 서드파티 앱에서 두 속성의 config/status/timestamp/value를 확인하는 것이다. 실차 지원은 이번 에뮬레이터 등록과 별도다.

## 재현과 근거

- [일반 앱 읽기 진단](verification/ev-properties/app-probe.txt): 권한, 클래스 접근, config, SecurityException, SDK 반환값.
- [VHAL 속성 설정](verification/ev-properties/property-config.txt): 서비스 진단 중 대상 두 속성만 추출. shell 진단 결과는 앱 권한 증거가 아니다.
- [에뮬레이터 환경](verification/ev-properties/environment.txt), [공개 SDK 인터페이스](verification/ev-properties/sdk-api.txt).
- 테스트: `ivi/src/androidTest/java/com/chapark/ivi/EvPropertyProbeTest.kt`. 실행: `am instrument --user 10 -w -e class com.chapark.ivi.EvPropertyProbeTest com.chapark.ivi.test/androidx.test.runner.AndroidJUnitRunner`.

공식 출처:

- [Android VehiclePropertyIds](https://developer.android.com/reference/android/car/VehiclePropertyIds#EV_BATTERY_LEVEL): 두 속성의 ID·타입·단위·읽기 권한.
- [Android Car.PERMISSION_ENERGY](https://developer.android.com/reference/android/car/Car#PERMISSION_ENERGY): AAOS 에너지 읽기 권한.
- [Pleos API 호환성 및 가용성](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability): 지원 여부 개별 확인 및 기술 지원 검토 원칙.
- [공모전 도움말·문의](https://appdevchallenge.auto-hmg.io/guide/help): 공식 문의 주소.
