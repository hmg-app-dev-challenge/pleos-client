# 제출 APK 실행 확인

2026-11-15 (Asia/Seoul). 제출 폴더의 `apk/차박차박_차Park_release.apk`를 직접 설치해 확인했다.

- [x] 기존 APK 검증 기록과 SHA-256 일치: `0a2a07cb4bd3b1e57370bc62fcb7805ad9524915db6ef13d67c3a8c54ef9cf7e`
- [x] APK v2 서명 검증 통과, 버전 0.5.5(8), debuggable 플래그 없음
- [x] Pleos API 34 ARM64 기존 `ChaPark_IVI` AVD에 업데이트 설치: `Success`
- [x] 사용자 10에서 `com.chapark.ivi/.IviActivity` 실행: `Status: ok`, `LaunchState: COLD`, 296ms
- [x] 2560×1440·160dpi 화면 렌더링 및 포그라운드 Activity 확인
- [x] 저장된 몽산포 여행 복원, 배터리 100%·차량 연결 표시 확인
- [x] 홈 버튼 입력 후 홈 화면 전환과 이어하기 버튼 표시 확인
- [x] 확인 시점에 앱 PID 6753 유지, 해당 PID의 crash 로그와 앱 종료 이력 항목 없음

## 증거

- [부팅·화면 규격·실행 결과](verification/submission-0.5.5/launch.txt)
- [실행 화면](verification/submission-0.5.5/launch.png)
- [홈 화면](verification/submission-0.5.5/home.png)
- [앱 상태·시스템 crash 로그](verification/submission-0.5.5/runtime.txt)
- [앱 프로세스 crash 로그·종료 이력 조회](verification/submission-0.5.5/app-health.txt)

## 범위와 관측 사항

기존 AVD 데이터를 보존한 업데이트 설치 및 실행 확인이다. 신규 설치·전체 여행 흐름·내비 경로 성공·실차 검증은 이번 범위에 포함하지 않았다. 기존 검증 기록은 [0.5.5](RELEASE_0_5_5.md)와 [0.5.1](RELEASE_0_5_1.md)을 참고한다.

복원된 이동 화면에는 **‘이동 정보를 불러오지 못했어요’** 및 다시 시도 안내가 표시되었다. 목적지·준비 내용은 유지됐고 홈 전환은 정상 동작했다. 원인을 확정하거나 내비 정상 동작으로 판정하지 않는다.

전체 crash 버퍼에는 앱 실행 전 부팅 중 Pleos placesense/profile 서비스의 native crash가 있었다. 차Park 앱 PID의 crash 로그는 비어 있었다. 시스템 전체가 오류 없이 동작했다고 해석하지 않는다.

확인 후 에뮬레이터는 일시정지했다. 앱 삭제·데이터 초기화·여행 완료 처리는 수행하지 않았다.
