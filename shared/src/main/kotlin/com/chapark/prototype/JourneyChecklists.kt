package com.chapark.prototype

/** Version 1 is retained only for completed historical trips. No vehicle actuation is implied. */
object JourneyChecklists {
    const val CURRENT_VERSION=2
    val legacySleep=linkedMapOf("parking" to "주차 위치와 현장 숙박 조건 확인", "ventilation" to "환기 방법과 비상 이동 동선 확인",
        "climate" to "차량 기본 설정에서 공조 직접 확인", "lights" to "차량 기본 설정에서 조명 직접 확인")
    val legacyExit=linkedMapOf("settings" to "직접 변경한 차량 설정 확인", "gear" to "침구와 개인 장비 모두 챙기기",
        "waste" to "쓰레기 수거와 주변 정리", "surroundings" to "출발 전 차량 주변 확인")
    val sleep=legacySleep+linkedMapOf("door-lock" to "문 잠금 직접 확인", "personal-items" to "개인 준비물 정리 확인")
    val exit=linkedMapOf("settings" to "직접 변경한 차량 설정 확인", "equipment" to "실내·외 장비 모두 회수",
        "waste" to "쓰레기 수거와 주변 정리", "left-behind" to "놓고 가는 물품 확인", "site-condition" to "출발 전 장소 상태 확인")
    fun sleep(version:Int)=if(version==1)legacySleep else sleep
    fun exit(version:Int)=if(version==1)legacyExit else exit
}
