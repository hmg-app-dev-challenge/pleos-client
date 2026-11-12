package com.chapark.app.data

import com.chapark.app.domain.Condition
import java.time.LocalDate

enum class Scenery(val label:String){SEA("바다"),MOUNTAIN("산"),RIVER("강"),FOREST("숲")}
data class Evidence(val label:String,val detail:String,val url:String?,val checkedOn:String?,val condition:Condition=Condition.UNKNOWN){
    // Product freshness policy, not a guarantee that operators have not changed conditions.
    fun fresh(on:LocalDate)=checkedOn?.let{runCatching{val d=LocalDate.parse(it);!d.isAfter(on) && !d.plusDays(30).isBefore(on)}.getOrDefault(false)} ?: false
}
data class PlaceEvidence(val scenery:Scenery,val facts:List<Evidence>,val parking:Condition=Condition.UNKNOWN,
    val viewingDirectionDegrees:Double?=null,val viewingCheckedOn:String?=null){
    fun exclusions(place:NavigationPlace,on:LocalDate):List<String> = buildList {
        if(parking!=Condition.ALLOWED)add("주차 조건 확인 필요")
        if(place.place.overnight!=Condition.ALLOWED)add("차내 숙박 조건 확인 필요")
        if(place.place.nightToilet!=Condition.ALLOWED)add("야간 화장실 확인 필요")
        if(facts.isEmpty() || facts.any{!it.fresh(on)})add("최신 운영 근거 확인 필요")
    }
}
object PlaceEvidenceCatalog {
    const val VERSION="2026-11-13.1"
    private const val POLICY="https://reservation.knps.or.kr/contents/usagePolicy.do"
    private const val MONG="https://reservation.knps.or.kr/contents/C/serviceGuide.do?deptId=B081002&parkId=B08&prdDvcd=C"
    fun find(id:String):PlaceEvidence=AcceptedCatalog.snapshot?.evidence?.get(id) ?: when(id){
        "camp-mongsanpo"->PlaceEvidence(Scenery.SEA,listOf(
            Evidence("차내 숙박","B010·B011·B012·B028의 도킹텐트 차박만 안내돼 있어요. 차량만으로 숙박 가능한지는 예약 전 확인해 주세요.",POLICY,"2026-11-13"),
            Evidence("야간 화장실","화장실 시설은 있으나 야간 운영 시간은 확인이 필요해요.",MONG,"2026-11-13"),
            Evidence("주변 충전","운영 안내상 몽산포주차장에 2대. 충전 위치·커넥터·현재 이용 가능 여부는 확인해 주세요.",MONG,"2026-11-13"),
            Evidence("운영 기간","4~11월 운영 안내. 예약 가능한 영지와 방문일 운영 여부를 확인해 주세요.",MONG,"2026-11-13")))
        "camp-gangdong"->PlaceEvidence(Scenery.MOUNTAIN,listOf(
            Evidence("주차","오토캠핑장은 텐트 옆 1대 주차, 다른 영지는 주차장 이용으로 안내돼요.","https://www.seoul.go.kr/storyw/campingjang/gangdong.do","2026-11-13"),
            Evidence("취사","텐트 옆에서만 취사 가능해요. 차 안 취사 허가를 뜻하지 않아요.","https://www.seoul.go.kr/storyw/campingjang/gangdong.do","2026-11-13",Condition.ALLOWED)))
        "camp-nanji"->PlaceEvidence(Scenery.RIVER,listOf(Evidence("운영 조건","야간 화장실·차내 숙박·주차 조건은 운영처에 확인해 주세요.",NavigationPlaces.find(id)?.source,null)))
        else->PlaceEvidence(Scenery.FOREST,listOf(Evidence("운영 조건","차내 숙박과 야간 시설 운영 여부를 확인해 주세요.",NavigationPlaces.find(id)?.source,null)))
    }
    fun filter(scenery:Scenery?,confirmedOnly:Boolean,on:LocalDate)=NavigationPlaces.all.filter {
        val info=find(it.place.id)
        (scenery==null || info.scenery==scenery) && (!confirmedOnly || info.exclusions(it,on).isEmpty())
    }
}
