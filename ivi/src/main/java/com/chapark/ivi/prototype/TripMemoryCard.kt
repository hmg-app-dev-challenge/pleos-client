package com.chapark.ivi.prototype

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chapark.ivi.design.*
import kotlin.random.Random

/** Decorative cover chosen deterministically, never interpreted as observed weather. */
@Composable internal fun TripMemoryCard(trip:ArchivedTrip,onDelete:(()->Unit)?=null) {
    var expanded by rememberSaveable(trip.id){mutableStateOf(false)}
    val p=LocalIviColors.current
    val stars=(trip.id.hashCode() and 1)==0
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(p.surface)
        .testTag("memory-card-${trip.id}")) {
        IviTheme(isDark=true) {
            Box(Modifier.fillMaxWidth().heightIn(min=300.dp).testTag("memory-cover-${trip.id}")) {
                MemoryArtwork(stars,Modifier.matchParentSize())
                Column(Modifier.fillMaxWidth().padding(IviSpacing.section),verticalArrangement=Arrangement.spacedBy(IviSpacing.control)) {
                    Text(trip.date,style=IviType.label,color=Color.White.copy(alpha=.8f))
                    Text(trip.placeName,Modifier.fillMaxWidth(.78f),style=IviType.title,color=Color.White,maxLines=2,overflow=TextOverflow.Ellipsis)
                    Text(if(trip.memory?.completed==true)"내 차와 보낸 하룻밤" else "내 차에 담긴 여행의 기억",
                        style=IviType.label,color=Color.White.copy(alpha=.8f))
                    Spacer(Modifier.height(8.dp))
                    IviButton(if(expanded)"접기" else "더보기",Modifier.testTag("memory-more-${trip.id}")
                        .semantics{stateDescription=if(expanded)"상세정보 펼쳐짐" else "상세정보 접힘"},
                        size=IviButtonSize.MEDIUM,filled=true){expanded=!expanded}
                }
            }
        }
        if(expanded) Column(Modifier.fillMaxWidth().padding(IviSpacing.section).testTag("memory-details-${trip.id}"),
            verticalArrangement=Arrangement.spacedBy(IviSpacing.related)) {
            Text("여행 상세",style=IviType.smallTitle,color=p.ink)
            Text("${trip.date} · ${trip.placeName}",style=IviType.label,color=p.secondary)
            JourneyMemorySummary(trip.records,trip.memory)
            if(trip.forgotten.isNotEmpty())Text("다음에 챙길 것 · ${trip.forgotten.joinToString()}",style=IviType.smallBody)
            HorizontalDivider(color=p.divider)
            Text("시간별 기록",style=IviType.smallTitle)
            val zone=runCatching{java.time.ZoneId.of(trip.zone)}.getOrDefault(java.time.ZoneOffset.UTC)
            trip.records.filter{it.subject==null || it.kind.startsWith("ROUTE_CALL_")}
                .sortedBy{it.eventAtMillis ?: it.observedAtMillis}.forEach{record->
                    val time=java.time.Instant.ofEpochMilli(record.eventAtMillis ?: record.observedAtMillis).atZone(zone)
                        .format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                    val label=when(record.kind){
                        "PLACE_CONTEXT"->"장소·풍경 기록";"PLACE_INFO"->"장소 자료 · 당시 확인 정보"
                        "SUNSET"->"일몰 · 계산";"SUNRISE"->"일출 · 계산";"PLACE_SELECTED"->"장소 선택"
                        "CHARGING_PLAN"->"충전 경유 계획";"PREPARATION_COMPLETED"->"출발 준비 확인"
                        "ROUTE_REQUESTED"->"길 안내 요청";"ROUTE_CALL_RETURNED"->"내비 요청 호출 완료"
                        "ROUTE_CALL_FAILED"->"내비 요청 호출 실패";"ROUTE_FAILED"->"길 안내 확인 실패"
                        "GUIDANCE_OBSERVED"->"길 안내 확인";"ARRIVAL"->if(record.origin=="SDK")"도착 알림 수신" else "도착 직접 확인"
                        "PARKING_CONFIRMED"->"주차 직접 확인";"TRIP_COMPLETED"->"철수 확인 완료";else->"여행 상태 확인"
                    }
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("$time · $label",style=IviType.smallBody)
                        record.label?.let{Text(it,style=IviType.label,color=p.secondary)}
                    }
                }
            if(trip.records.isEmpty())Text("이전 여행의 상세 시각은 수집되지 않았어요.",style=IviType.label)
            Text("기록 시간대 · ${zone.id}",style=IviType.label,color=p.secondary)
            val checks=trip.records.filter{it.subject!=null && !it.kind.startsWith("ROUTE_CALL_")}
            if(checks.isNotEmpty()) {
                HorizontalDivider(color=p.divider)
                Text("확인 항목",style=IviType.smallTitle)
                checks.forEach{record->Text("${if(record.kind=="PREPARATION_OMITTED") {if(record.confirmed==true)"누락" else "누락 취소"} else if(record.confirmed==true)"확인" else "확인 해제"} · ${record.label ?: record.subject}",style=IviType.smallBody)}
            }
            if(onDelete!=null)IviButton("이 기록 삭제",Modifier.testTag("memory-delete-${trip.id}"),size=IviButtonSize.MEDIUM,onClick=onDelete)
        }
    }
}

@Composable private fun MemoryArtwork(stars:Boolean,modifier:Modifier) {
    val points=remember { val r=Random(2055);List(70){Triple(r.nextFloat(),r.nextFloat(),r.nextFloat())} }
    Canvas(modifier) {
        val w=size.width;val h=size.height
        val sky=if(stars)listOf(Color(0xFF101B32),Color(0xFF293967),Color(0xFF5C5375))
            else listOf(Color(0xFF28243E),Color(0xFF9E5868),Color(0xFFE9A06C))
        drawRect(Brush.verticalGradient(sky))
        val focus=Offset(w*.81f,h*.44f)
        val glow=if(stars)Color(0xFFACBFE8) else Color(0xFFFFCE91)
        drawCircle(Brush.radialGradient(listOf(glow.copy(alpha=.38f),glow.copy(alpha=0f)),focus,h*.63f),h*.63f,focus)
        if(stars) {
            points.forEach{(x,y,r)->
                val c=Offset(w*(.38f+x*.60f),h*(.05f+y*.62f))
                drawCircle(Color.White.copy(alpha=.35f+r*.6f),(1f+r*1.4f).dp.toPx(),c)
            }
            val c=Offset(w*.81f,h*.26f);val ray=11.dp.toPx()
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha=.5f),Color.Transparent),c,ray*2),ray*2,c)
            drawLine(Color.White.copy(alpha=.8f),c-Offset(ray,0f),c+Offset(ray,0f),1.dp.toPx())
            drawLine(Color.White.copy(alpha=.8f),c-Offset(0f,ray),c+Offset(0f,ray),1.dp.toPx())
            drawCircle(Color.White,2.5.dp.toPx(),c)
        } else drawCircle(Color(0xFFFFD9A3),h*.15f,focus)
        val mountains=Path().apply {
            moveTo(0f,h*.78f);cubicTo(w*.20f,h*.63f,w*.31f,h*.9f,w*.50f,h*.67f)
            cubicTo(w*.65f,h*.44f,w*.69f,h*.75f,w*.78f,h*.64f)
            cubicTo(w*.87f,h*.52f,w*.94f,h*.66f,w,h*.58f)
            lineTo(w,h);lineTo(0f,h);close()
        }
        drawPath(mountains,if(stars)Color(0xFF26314C) else Color(0xFF73556A))
        val shore=Path().apply {
            moveTo(0f,h*.89f);cubicTo(w*.22f,h*.69f,w*.39f,h*.92f,w*.60f,h*.82f)
            cubicTo(w*.79f,h*.73f,w*.92f,h*.89f,w,h*.8f)
            lineTo(w,h);lineTo(0f,h);close()
        }
        drawPath(shore,if(stars)Color(0xFF15243A) else Color(0xFF453D54))
        // Opaque left stop keeps white type readable in both artwork variants.
        drawRect(Brush.horizontalGradient(0f to Color(0xFF141B2B),.48f to Color(0xFF141B2B).copy(alpha=.85f),1f to Color.Transparent))
    }
}
