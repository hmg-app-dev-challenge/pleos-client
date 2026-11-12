package com.chapark.app.data

import com.chapark.app.domain.Place
import com.chapark.app.domain.Condition.UNKNOWN

/** Official public map coordinates, not surveyed vehicle entrances. No third-party POI IDs. */
data class NavigationPlace(val place:Place,val latitude:Double,val longitude:Double,val address:String,val source:String)
object NavigationPlaces {
    val all=listOf(
        NavigationPlace(Place("camp-jungnang","중랑캠핑숲","숲 가까이에서 준비하는 하루",UNKNOWN,UNKNOWN,UNKNOWN,UNKNOWN,UNKNOWN,12),
            37.6049551146885,127.109960818343,"서울 중랑구 망우로87길 110",
            "https://korean.visitseoul.net/nature/중랑캠핑숲/KOP035307"),
        NavigationPlace(Place("camp-nanji","난지캠핑장","한강 가까이에서 쉬어가는 곳",UNKNOWN,UNKNOWN,UNKNOWN,UNKNOWN,UNKNOWN,12),
            37.5699862,126.8720578,"서울 마포구 한강난지로 28",
            "https://www.gocamping.or.kr/bsite/camp/info/read.do?c_no=3397"),
        NavigationPlace(Place("camp-gangdong","강동그린웨이 가족캠핑장","일자산 자락에서 보내는 여유",UNKNOWN,UNKNOWN,UNKNOWN,UNKNOWN,UNKNOWN,6),
            37.5362195,127.1529466,"서울 강동구 천호대로206길 87",
            "https://www.seoul.go.kr/storyw/campingjang/gangdong.do")
    )
    const val DEFAULT_ID="camp-nanji"
    fun find(id:String)=all.firstOrNull{it.place.id==id}
}
object PlaceCatalog {
    val all get()=DemoPlaces.all+NavigationPlaces.all.map{it.place}
    fun find(id:String)=all.firstOrNull{it.id==id}
}
