package com.chapark.app.data

data class ChargingPlace(val id:String,val name:String,val latitude:Double,val longitude:Double,
    val address:String,val source:String,val checkedOn:String,val details:String)
object ChargingPlaces {
    // Operator's facility map point, not an independently surveyed charger/vehicle entrance.
    val all=listOf(ChargingPlace("emart-mapo","이마트 마포점 충전시설",37.5424095,126.953336,
        "서울 마포구 백범로 212","https://eapp.emart.com/branch/view.do?id=1123&mallGbn=E","2026-11-13",
        "지하 5·6층 완속 충전 안내 · 10~23시, 점포 휴점일 제외. 커넥터·주차요금·실시간 이용 가능 여부 확인 필요. 건물 대표 위치로 안내해요."))
    fun find(id:String?)=all.firstOrNull{it.id==id}
}
