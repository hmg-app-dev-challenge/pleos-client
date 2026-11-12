package com.chapark.ivi.prototype

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.chapark.app.data.NavigationPlaces
import com.chapark.app.domain.*
import com.chapark.ivi.design.*
import com.chapark.prototype.*

private val steps=listOf("시작","조건 확인","장소 비교","준비 계획","이동","수면 준비","철수")
private val names=com.chapark.app.data.PlaceCatalog.all.associate { it.id to it.name }
private val sleepItems=linkedMapOf("parking" to "주차 위치와 현장 숙박 조건 확인", "ventilation" to "환기 방법과 비상 이동 동선 확인", "climate" to "차량 기본 설정에서 공조 직접 확인", "lights" to "차량 기본 설정에서 조명 직접 확인")
private val exitItems=linkedMapOf("settings" to "직접 변경한 차량 설정 확인", "gear" to "침구와 개인 장비 모두 챙기기", "waste" to "쓰레기 수거와 주변 정리", "surroundings" to "출발 전 차량 주변 확인")
private fun condition(value:Condition)=when(value){Condition.ALLOWED->"허용";Condition.RESTRICTED->"제한";Condition.UNKNOWN->"미확인"}

@Composable fun JourneyScreen(ui:JourneyUi,send:(JourneyEvent)->Unit,onNavigation:()->Unit={},onDeleteHistory:(String)->Unit={}) {
    val s=ui.journey
    var history by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var settings by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }
    BackHandler(s.step>0 && !s.completed && !s.restricted && !settings && !history && deleteId==null && confirm==null) { send(JourneyEvent.Back) }
    IviTheme(s.dark,nightMode=s.step==5 && s.restMode!="REST" && !s.restricted) {
        val p=LocalIviColors.current
        Surface(Modifier.fillMaxSize(),color=p.background) {
            if(!ui.loaded) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Text("저장된 여행을 불러오고 있어요",style=IviType.title) }
            else Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal=40.dp,vertical=24.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                    Text("차Park",style=IviType.title,modifier=Modifier.weight(1f))
                    IviButton("지난 기록",size=IviButtonSize.MEDIUM,enabled=!s.restricted,onClick={history=true})
                    IviButton("설정",size=IviButtonSize.MEDIUM,enabled=!s.restricted,onClick={settings=true})
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                    Text(if(ui.saving)"저장 중" else if(ui.error!=null)"저장 오류" else "기기에 저장됨",style=IviType.label,color=p.tertiary)
                }
                if(ui.syncStatus!="기기에 저장됨") Text(ui.syncStatus,style=IviType.label,color=p.tertiary)
                Spacer(Modifier.height(24.dp))
                if(ui.error!=null) {
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(24.dp)) {
                        Text(ui.error,style=IviType.title)
                        IviButton("초기화 후 시작",filled=true) { confirm="reset" }
                    }
                } else if(s.restricted) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(32.dp)) {
                        Text("정차 후 이어서\n확인할 수 있어요",style=IviType.headline)
                        Text("작성한 계획은 저장되어 있어요. 안전한 곳에 정차한 뒤 확인해 주세요.",style=IviType.body)
                        Text("${s.step+1} / 7 · ${steps[s.step]}",style=IviType.title)
                        if(s.batteryVisible) Text("배터리 ${s.soc?.let{"$it%"} ?: "확인 필요"} · ${sourceLabel(s.socSource)}",style=IviType.title)
                        Text(if(s.route==RoutePhase.GUIDING)"길 안내 중" else "길 안내 상태 확인 필요",style=IviType.title)
                    }
                } else {
                    JourneyProgress(s)
                    Spacer(Modifier.height(24.dp))
                    BoxWithConstraints(Modifier.weight(1f)) {
                        val wide=maxWidth>=1300.dp
                        Row(horizontalArrangement=Arrangement.spacedBy(32.dp)) {
                            key(s.step,s.completed) {
                                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).testTag("journey-content"),verticalArrangement=Arrangement.spacedBy(24.dp)) {
                                    if(s.batteryStatus!=ReadingStatus.AVAILABLE)Text(when(s.batteryStatus){ReadingStatus.UNSUPPORTED->"이 차량은 배터리 조회를 지원하지 않아요.";ReadingStatus.PERMISSION_DENIED->"배터리 조회 권한이 필요해요.";ReadingStatus.STALE->"배터리 정보를 다시 확인하고 있어요.";ReadingStatus.DRIVING_RESTRICTED->"정차 후 배터리를 다시 확인해 주세요.";ReadingStatus.INVALID->"배터리 값을 확인하지 못했어요.";else->"배터리 연결 상태를 확인해 주세요."},style=IviType.label)
                                    ui.notice?.let{Text(it,style=IviType.label,color=p.tertiary)}
                                    ui.navigationMessage?.let{Text(it,style=IviType.body,color=p.secondary)}
                                    JourneyContent(s,send)
                                    if(s.step>0 && !s.completed) IviButton("이전 단계",size=IviButtonSize.MEDIUM,enabled=!ui.saving){send(JourneyEvent.Back)}
                                    Spacer(Modifier.height(24.dp))
                                }
                            }
                            if(wide) JourneySummary(s,Modifier.width(340.dp).fillMaxHeight())
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color=p.divider)
                    if(s.step==3) s.departureBlockReason?.let {
                        Text(it,style=IviType.label,color=p.secondary,modifier=Modifier.padding(top=12.dp).testTag("departure-block-reason"))
                    }
                    JourneyFooter(s,ui.saving) {
                            when {
                                s.completed -> send(JourneyEvent.NewTrip)
                                s.step==6 -> confirm="finish"
                                s.step==4 -> send(when(s.route) {
                                    RoutePhase.NONE,RoutePhase.FAILED->JourneyEvent.RequestRoute
                                    RoutePhase.REQUESTED->return@JourneyFooter
                                    RoutePhase.GUIDING->{onNavigation();return@JourneyFooter}
                                    RoutePhase.ARRIVED->if(s.parked)JourneyEvent.Next else JourneyEvent.Park
                                })
                                else -> send(JourneyEvent.Next)
                            }
                    }
                }
            }
        }
        if(history && !s.restricted) androidx.compose.ui.window.Dialog(onDismissRequest={history=false},properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
            Panel(Modifier.widthIn(max=1100.dp).fillMaxHeight(.85f).verticalScroll(rememberScrollState())) {
                Text("지난 여행 기록",style=IviType.title)
                if(ui.history.isEmpty())Text("아직 저장된 여행 기록이 없어요.",style=IviType.body)
                ui.history.forEach { trip->
                    Text("${trip.date} · ${trip.placeName}",style=IviType.smallTitle)
                    Text("확인 기록 ${trip.records.size}개 · 시간대 ${trip.zone}",style=IviType.label)
                    trip.records.filter{it.subject==null}.forEach{record->
                        val time=java.time.Instant.ofEpochMilli(record.observedAtMillis).atZone(runCatching{java.time.ZoneId.of(trip.zone)}.getOrDefault(java.time.ZoneOffset.UTC)).toLocalTime().withNano(0)
                        val label=when(record.kind){"PREPARATION_COMPLETED"->"출발 준비 확인";"ROUTE_REQUESTED"->"길 안내 요청";"GUIDANCE_OBSERVED"->"길 안내 확인";"ARRIVAL"->"도착 확인";"PARKING_CONFIRMED"->"주차 확인";"TRIP_COMPLETED"->"철수 완료";else->"상태 확인"}
                        Text("$time · $label",style=IviType.smallBody)
                    }
                    if(trip.forgotten.isNotEmpty())Text("다음에 챙길 것 · ${trip.forgotten.joinToString()}",style=IviType.smallBody)
                    IviButton("이 기록 삭제",size=IviButtonSize.MEDIUM){deleteId=trip.id}
                    HorizontalDivider()
                }
                IviButton("기록 닫기"){history=false}
            }
        }
        deleteId?.takeIf{!s.restricted}?.let{id->IviConfirm("여행 기록을 삭제할까요?","이 기기에 보관한 해당 여행 기록을 삭제합니다.","삭제",{deleteId=null;onDeleteHistory(id)},{deleteId=null})}
        if(settings && !s.restricted) JourneySettings(s,send,onClose={settings=false},onReset={settings=false;confirm="reset"})
        when(if(s.restricted)null else confirm) {
            "finish" -> IviConfirm("오늘의 차박을 마칠까요?","확인한 준비와 철수 항목을 여행 기록에 저장합니다.","종료",{confirm=null;send(JourneyEvent.Finish)},{confirm=null})
            "reset" -> IviConfirm("여행 계획을 새로 만들까요?","현재 여행의 준비·확인 기록을 지우고 새 계획을 시작합니다.","처음부터",{confirm=null;send(JourneyEvent.ResetScenario)},{confirm=null})
        }
    }
}
@Composable private fun JourneyProgress(s:Journey) {
    val p=LocalIviColors.current
    BoxWithConstraints {
        val compact=maxWidth<1000.dp
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(compact) Text("${s.step+1} / 7 · ${steps[s.step]}",style=IviType.label)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                steps.forEachIndexed{i,label->
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        if(!compact) Text("${i+1} $label",style=IviType.label,color=if(i==s.step)p.ink else p.tertiary,maxLines=1)
                        Box(Modifier.fillMaxWidth().height(4.dp).background(if(i<=s.step)p.green else p.divider))
                    }
                }
            }
        }
    }
}
@Composable private fun JourneyFooter(s:Journey,saving:Boolean,onNext:()->Unit) {
    val p=LocalIviColors.current
    // The parent supplies previous navigation through the screen; footer adapts the primary action.
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top=20.dp)) {
        val action=primary(s)
        if(maxWidth<900.dp) Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("${s.step+1} / 7 · ${steps[s.step]}",style=IviType.label,color=p.tertiary)
            IviButton(action.first,Modifier.fillMaxWidth().testTag("journey-next"),filled=true,enabled=action.second && !saving,onClick=onNext)
        } else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("${s.step+1} / 7 · ${steps[s.step]}",Modifier.weight(1f),style=IviType.label,color=p.tertiary)
            IviButton(action.first,Modifier.widthIn(max=620.dp).testTag("journey-next"),filled=true,enabled=action.second && !saving,onClick=onNext)
        }
    }
}
private fun primary(s:Journey):Pair<String,Boolean> = when {
    s.completed -> "새 여행 준비" to true
    s.step==0 -> "오늘 밤 차박 시작" to true
    s.step==1 -> "장소 후보 비교" to true
    s.step==2 -> "선택한 장소로 계획" to true
    s.step==3 -> "준비 계획 확인 · 이동" to s.canDepart
    s.step==4 -> when(s.route) {
        RoutePhase.NONE -> "길 안내 시작" to true
        RoutePhase.REQUESTED -> "경로를 찾고 있어요" to false
        RoutePhase.GUIDING -> "내비게이션 열기" to true
        RoutePhase.FAILED -> "다시 시도" to true
        RoutePhase.ARRIVED -> (if(s.parked)"수면 준비로" else "주차했어요") to true
    }
    s.step==5 -> "수면 준비 확인 · 다음 날" to (s.canSleep && s.siteReady && s.sleepChecks.containsAll(Journey.SLEEP_IDS))
    else -> "차박 종료" to s.canFinish
}
@Composable private fun Panel(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    Column(modifier.fillMaxWidth().background(LocalIviColors.current.surface,RoundedCornerShape(32.dp)).padding(28.dp),verticalArrangement=Arrangement.spacedBy(20.dp),content=content)
}
@Composable private fun Heading(title:String,subtitle:String) {
    Text(title,style=IviType.headline)
    Text(subtitle,style=IviType.body,color=LocalIviColors.current.secondary)
}
@Composable private fun JourneyContent(s:Journey,send:(JourneyEvent)->Unit) {
    val p=LocalIviColors.current
    when(s.step) {
        0 -> {
            Heading("오늘 밤,\n내 차로 떠나는 쉼.","어디서 어떻게 쉴지, 오늘 밤의 작은 여행을 준비해요.")
            Panel {
                Text("오늘의 여행",style=IviType.title)
                Text("내 차와 함께하는 하룻밤",style=IviType.body)
                if(s.batteryVisible) Text("배터리 ${s.soc?.let{"$it%"} ?: "미확인"} · ${sourceLabel(s.socSource)}",style=IviType.smallTitle)
                Text("${s.input.people}인 · ${if(s.input.cooking)"간단 조리 예정" else "준비한 식사"} · ${if(s.input.sleepingBag)"침낭 보유" else "침낭 미보유"}",style=IviType.body)
            }
            Text("출발 전 충전 계획과 현장 운영 시간을 확인해 주세요.",style=IviType.label,color=p.tertiary)
        }
        1 -> {
            Heading("누구와, 어떻게\n보낼 예정인가요?","저장된 기본 조건을 이번 여행에 맞게 바꿔주세요.")
            Panel {
                Text("여행 날짜 · ${s.input.schedule?.date ?: "선택 전"}",style=IviType.smallTitle)
                Text("시간대 · ${s.input.schedule?.zoneId ?: "선택 전"}",style=IviType.label)
                IviButton("오늘 날짜로 계획",size=IviButtonSize.MEDIUM){
                    val zone=java.time.ZoneId.systemDefault()
                    send(JourneyEvent.Conditions(s.input.copy(schedule=TripSchedule(java.time.LocalDate.now(zone).toString(),zone.id))))
                }
                s.input.schedule?.let { schedule->
                    AdjustValue("여행 날짜",schedule.date,true,true,
                        {schedule.shiftDays(-1)?.let{send(JourneyEvent.Conditions(s.input.copy(schedule=it)))}},
                        {schedule.shiftDays(1)?.let{send(JourneyEvent.Conditions(s.input.copy(schedule=it)))}})
                    IviButton("기기 시간대 적용",size=IviButtonSize.MEDIUM){send(JourneyEvent.Conditions(s.input.copy(schedule=schedule.copy(zoneId=java.time.ZoneId.systemDefault().id))))}
                    Text(schedule.departureEnd(s.input.endHour)?.atZone(java.time.ZoneId.of(schedule.zoneId))?.let{"철수 예정 · ${it.toLocalDate()} ${it.hour}시"}
                        ?: "철수 시각을 확인해 주세요. 시간대 변경으로 존재하지 않거나 겹치는 시각일 수 있어요.",style=IviType.smallBody)
                }
                Text("함께하는 인원",style=IviType.smallTitle)
                AdjustValue("인원", "${s.input.people}명",s.input.people>1,s.input.people<Int.MAX_VALUE,
                    {send(JourneyEvent.Conditions(s.input.copy(people=s.input.people-1)))},
                    {send(JourneyEvent.Conditions(s.input.copy(people=s.input.people+1)))})
                if(s.batteryVisible) AdjustValue("배터리 확인 기준", "${s.input.batteryThreshold}%",s.input.batteryThreshold>0,s.input.batteryThreshold<100,
                    {send(JourneyEvent.Conditions(s.input.copy(batteryThreshold=(s.input.batteryThreshold-5).coerceAtLeast(0))))},
                    {send(JourneyEvent.Conditions(s.input.copy(batteryThreshold=(s.input.batteryThreshold+5).coerceAtMost(100))))})
                AdjustValue("철수 예정", "다음 날 ${s.input.endHour}시",true,true,
                    {send(JourneyEvent.Conditions(s.input.copy(endHour=(s.input.endHour+23)%24)))},
                    {send(JourneyEvent.Conditions(s.input.copy(endHour=(s.input.endHour+1)%24)))})
                IviToggleRow("현장에서 간단히 조리할 예정",s.input.cooking) {send(JourneyEvent.Conditions(s.input.copy(cooking=it)))}
                IviToggleRow("반려동물과 함께",s.input.pets) {send(JourneyEvent.Conditions(s.input.copy(pets=it)))}
                IviToggleRow("침낭을 보유하고 있어요",s.input.sleepingBag) {send(JourneyEvent.Conditions(s.input.copy(sleepingBag=it)))}
            }
        }
        2 -> {
            Heading("오늘의 쉼터를 비교해요","숙박 조건과 필요한 준비를 비교하고, 오늘의 쉼터를 골라보세요.")
            BoxWithConstraints {
                if(maxWidth>=1100.dp) Row(horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                    NavigationPlaces.all.map{it.place}.forEachIndexed{index,place->PlaceCard(s,place,index,Modifier.weight(1f),send)}
                } else Column(verticalArrangement=Arrangement.spacedBy(24.dp)) {
                    NavigationPlaces.all.map{it.place}.forEachIndexed{index,place->PlaceCard(s,place,index,Modifier,send)}
                }
            }
        }
        3 -> {
            SolarSummary(s)
            Heading("오늘의 준비 계획","${names[s.placeId]} · 조건을 바꾸면 필요한 행동도 달라져요.")
            Panel {
                if(s.batteryVisible) BatteryReading(s,send)
                Text("출발 전 ${s.departureCheckedCount} / ${s.departureItems.size}개 확인 · 날짜와 준비 항목 확인 후 이동할 수 있어요",style=IviType.label,color=p.tertiary)
            }
            if(s.input.schedule?.departureEnd(s.input.endHour)==null) Panel {
                Text(s.departureBlockReason ?: "여행 일정을 확인해 주세요.",style=IviType.smallTitle)
                val zone=java.time.ZoneId.systemDefault()
                val today=java.time.LocalDate.now(zone)
                Text("$today · ${zone.id} · 다음 날 ${s.input.endHour}시 철수",style=IviType.smallBody)
                IviButton("오늘 날짜로 계획",size=IviButtonSize.MEDIUM){
                    send(JourneyEvent.Conditions(s.input.copy(schedule=TripSchedule(today.toString(),zone.id))))
                }
                Text("다른 날짜는 조건 확인 단계에서 변경할 수 있어요.",style=IviType.label)
            }
            Text("현장 운영 안내는 도착 후 확인해요. 체크는 이용 허가를 뜻하지 않아요.",style=IviType.smallBody)
            Group.entries.filter{it!=Group.SITE}.forEach { group ->
                val items=s.decisions.filter{it.group==group}.sortedBy { when(it.id){"charge-review"->0;"meal-ready"->1;else->2} }
                if(items.isNotEmpty()) Panel {
                    Text(group.label,style=IviType.title)
                    items.forEach {item-> IviCheckRow(item.title,item.id in s.checked,item.reason){send(JourneyEvent.Check(item.id))} }
                }
            }
        }
        4 -> {
            Heading("${names[s.placeId]}까지","차량 내비게이션에서 길 안내를 이어가세요.")
            Panel {
                Text(when(s.route){RoutePhase.NONE->"이동 준비";RoutePhase.REQUESTED->"경로를 탐색하고 있어요";RoutePhase.GUIDING->"길 안내 중";RoutePhase.FAILED->"이동 정보를 불러오지 못했어요";RoutePhase.ARRIVED->if(s.parked)"도착 · 주차 완료" else "목적지 도착 · 주차 확인 필요"},style=IviType.title)
                Text(if(s.decisions.any{it.id=="charge-review"})"배터리가 부족할 수 있어요. 출발 전 충전 계획을 확인해 주세요." else "목적지와 경로를 확인하고, 현장 운영 안내를 따라주세요.",style=IviType.body)
                if(s.route==RoutePhase.GUIDING) IviButton("도착했어요",size=IviButtonSize.MEDIUM){send(JourneyEvent.Arrive)}
                if(s.route==RoutePhase.FAILED) Text("잠시 후 다시 시도해 주세요. 선택한 목적지와 준비 계획은 저장되어 있어요.",style=IviType.smallBody)
                if(s.route==RoutePhase.ARRIVED && !s.parked) Text("안전한 곳에 주차한 뒤 주차 완료를 확인해 주세요.",style=IviType.smallBody)
            }
        }
        5 -> {
            Panel { s.siteItems.forEach {item->IviCheckRow(item.title,item.id in s.checked,item.reason){send(JourneyEvent.Check(item.id))}} }
            IviSegments(listOf("휴식","수면","별 보기"),listOf("REST","SLEEP","STARS").indexOf(s.restMode).coerceAtLeast(0)){send(JourneyEvent.RestMode(listOf("REST","SLEEP","STARS")[it]))}
            SolarSummary(s)
            Heading("잠들기 전, 하나씩 확인해요","편안히 쉴 수 있도록 주변과 차량 설정을 확인해 주세요.")
            Panel {
                Text("편안한 휴식 준비",style=IviType.smallTitle)
                Text("차량의 공조와 조명을 원하는 상태로 맞춘 뒤 체크해 주세요.",style=IviType.smallBody)
                sleepItems.forEach{(id,title)->IviCheckRow(title,id in s.sleepChecks){send(JourneyEvent.SleepCheck(id))}}
            }
            Text("차내 화기를 사용하지 마세요. 실제 사용은 차량 사용 설명서와 현장 운영 안내를 따르세요.",style=IviType.label,color=p.tertiary)
        }
        6 -> if(s.completed) {
            Heading("오늘의 차박을 마쳤어요","내 차와 보낸 하룻밤의 기억을 저장했어요.")
            Panel {
                Text(names[s.placeId] ?: "오늘의 쉼터",style=IviType.title)
                Text("수면 준비 ${s.sleepChecks.size}/4 · 철수 확인 ${s.exitChecks.size}/4",style=IviType.body)
                Text("직접 확인한 준비 항목 ${s.checkedCount}/${s.decisions.size}",style=IviType.body)
                Text("내 차와 보낸 하룻밤 기억",style=IviType.smallTitle)
                val zone=runCatching{java.time.ZoneId.of(s.input.schedule?.zoneId ?: "UTC")}.getOrDefault(java.time.ZoneOffset.UTC)
                s.records.filter{it.subject==null}.forEach { record->
                    val label=when(record.kind){
                        "PREPARATION_COMPLETED"->"출발 준비 확인";"ROUTE_REQUESTED"->"길 안내 요청"
                        "GUIDANCE_OBSERVED"->"길 안내 확인";"ARRIVAL"->if(record.origin=="SDK")"도착 알림 수신" else "도착 직접 확인"
                        "PARKING_CONFIRMED"->"주차 직접 확인";"TRIP_COMPLETED"->"철수 확인 완료";else->"여행 상태 확인"
                    }
                    val time=java.time.Instant.ofEpochMilli(record.observedAtMillis).atZone(zone).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                    Text("$time · $label",style=IviType.smallBody)
                    if(record.kind in setOf("PREPARATION_COMPLETED","TRIP_COMPLETED") && (s.batteryVisible || record.soc!=null))
                        Text("배터리 ${record.soc?.let{"$it%"} ?: "미확인"} · ${sourceLabel(record.socSource)}",style=IviType.label)
                }
                if(s.records.isEmpty())Text("이전 여행의 상세 시각은 수집되지 않았어요.",style=IviType.label)
                Text("기록 시간대 · ${zone.id} · 사용자가 확인한 기록이며 안전을 보장하지 않아요.",style=IviType.label)
                Text("다음 여행도 차Park와 함께 준비해 보세요.",style=IviType.smallBody)
            }
        } else {
            Heading("떠나기 전, 한 번 더.","직접 바꾼 설정과 가져온 물건을 확인하고 마무리해요.")
            var missing by remember { mutableStateOf("") }
            Panel {
                Text("다음에 챙기고 싶은 준비물",style=IviType.smallTitle)
                OutlinedTextField(missing,{if(it.length<=100)missing=it},label={Text("빠뜨린 준비물")},singleLine=true,modifier=Modifier.fillMaxWidth())
                IviButton("준비물 기록",size=IviButtonSize.MEDIUM,enabled=missing.isNotBlank()){
                    if(missing.trim() !in s.forgotten)send(JourneyEvent.Forgotten(missing))
                    missing=""
                }
                s.forgotten.sorted().forEach{name->IviButton("$name · 기록 취소",size=IviButtonSize.MEDIUM){send(JourneyEvent.Forgotten(name))}}
            }
            Panel {
                Text("출발 전 확인",style=IviType.smallTitle)
                exitItems.forEach{(id,title)->IviCheckRow(title,id in s.exitChecks){send(JourneyEvent.ExitCheck(id))}}
            }
        }
    }
}
@Composable private fun PlaceCard(s:Journey,place:Place,index:Int,modifier:Modifier,send:(JourneyEvent)->Unit) {
    val p=LocalIviColors.current
    Panel(modifier.then(if(s.placeId==place.id)Modifier.border(2.dp,p.green,RoundedCornerShape(32.dp))else Modifier)) {
        Text("${('A'+index)} · ${names[place.id]}",style=IviType.smallTitle)
        Text(NavigationPlaces.find(place.id)?.address ?: "주소 확인 필요",style=IviType.label,color=p.tertiary)
        HorizontalDivider(color=p.divider)
        Text("차내 숙박 · ${condition(place.overnight)}",style=IviType.smallBody)
        Text("야간 화장실 · ${condition(place.nightToilet)}",style=IviType.smallBody)
        Text("취사 · ${condition(place.cooking)}\n화기 · ${condition(place.fire)}",style=IviType.smallBody)
        Text("반려동물 · ${condition(place.pets)}",style=IviType.smallBody)
        Text("방문 전 운영 시간 확인 필요",style=IviType.label,color=p.tertiary)
        IviButton(if(s.placeId==place.id)"선택됨" else "선택",filled=s.placeId==place.id,size=IviButtonSize.MEDIUM,modifier=Modifier.fillMaxWidth().testTag("place-${place.id}")) {send(JourneyEvent.Place(place.id))}
    }
}
@Composable private fun JourneySummary(s:Journey,modifier:Modifier) {
    val p=LocalIviColors.current
    Column(modifier.background(p.surface,RoundedCornerShape(32.dp)).padding(28.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(28.dp)) {
        Text("오늘 밤의 계획",style=IviType.smallTitle)
        Text(names[s.placeId] ?: "오늘의 쉼터",style=IviType.title)
        Text("오늘의 목적지",style=IviType.label,color=p.tertiary)
        HorizontalDivider(color=p.divider)
        if(s.batteryVisible){
            Text(s.soc?.let{"$it%"} ?: "미확인",style=IviType.headline)
            Text("배터리 · ${sourceLabel(s.socSource)}",style=IviType.label,color=p.tertiary)
        }
        Text("${s.input.people}인\n${if(s.input.cooking)"간단 조리 예정" else "준비한 식사"}\n${if(s.input.sleepingBag)"침낭 보유" else "침낭 미보유"}",style=IviType.body)
        HorizontalDivider(color=p.divider)
        Text("${s.checkedCount}/${s.decisions.size}개 준비 확인",style=IviType.smallTitle)
        Text("준비 항목을 확인하고\n편안하게 출발하세요.",style=IviType.label,color=p.tertiary)
    }
}
@Composable private fun JourneySettings(s:Journey,send:(JourneyEvent)->Unit,onClose:()->Unit,onReset:()->Unit) {
    val p=LocalIviColors.current
    androidx.compose.ui.window.Dialog(onDismissRequest=onClose,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.widthIn(max=900.dp).fillMaxWidth(.92f).heightIn(max=1000.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(48.dp)).background(p.surface)
            .verticalScroll(rememberScrollState()).padding(48.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
            Text("설정",style=IviType.title)
            Text("화면과 여행 계획을 관리하세요.",style=IviType.smallBody)
            IviToggleRow("어두운 화면",s.dark){send(JourneyEvent.Dark(it))}
            IviButton("새 여행 계획",onClick=onReset)
            IviButton("설정 닫기",filled=true,onClick=onClose)
        }
    }
}

private fun sourceLabel(source:Source)=when(source){Source.SDK->"차량";Source.MANUAL->"직접 입력";Source.EMULATOR->"에뮬레이터";Source.DEMO->"예시";Source.UNKNOWN->"출처 미확인"}
@Composable private fun AdjustValue(label:String,value:String,canDecrease:Boolean,canIncrease:Boolean,onDecrease:()->Unit,onIncrease:()->Unit){
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("$label · $value",style=IviType.smallTitle)
        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){
            IviButton("$label 줄이기",size=IviButtonSize.MEDIUM,enabled=canDecrease,onClick=onDecrease)
            IviButton("$label 늘리기",size=IviButtonSize.MEDIUM,enabled=canIncrease,onClick=onIncrease)
        }
    }
}

@Composable private fun SolarSummary(s:Journey){
    val place=NavigationPlaces.find(s.placeId) ?: return
    val schedule=s.input.schedule ?: return
    val zone=runCatching{java.time.ZoneId.of(schedule.zoneId)}.getOrNull() ?: return
    val date=runCatching{java.time.LocalDate.parse(schedule.date)}.getOrNull() ?: return
    val today=runCatching{SolarTimes.calculate(date,zone,place.latitude,place.longitude)}.getOrNull() ?: return
    val tomorrow=runCatching{SolarTimes.calculate(date.plusDays(1),zone,place.latitude,place.longitude)}.getOrNull() ?: return
    fun describe(event:SolarEvent?)=event?.let{"${it.instant.atZone(zone).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))} · 방위 ${it.azimuthDegrees.toInt()}°"} ?: "해당 날짜에 없음"
    Panel {
        Text("해의 시간",style=IviType.smallTitle)
        Text("일몰 약 ${describe(today.sunset)}",style=IviType.body)
        Text("다음 날 일출 약 ${describe(tomorrow.sunrise)}",style=IviType.body)
        Text("장소 좌표로 계산 · 북쪽 0° / 동쪽 90° · ${zone.id}",style=IviType.label)
        Text("조망 방향 확인 필요 · 일몰 전 도착, 일출 이후 철수를 권장해요.",style=IviType.smallBody)
        Text("이동·충전 시간이 확인되지 않아 정확한 출발 시각은 안내하지 않아요.",style=IviType.label)
    }
}

@Composable private fun BatteryReading(s:Journey,send:(JourneyEvent)->Unit){
    Text("현재 배터리 · ${sourceLabel(s.socSource)}",style=IviType.smallTitle)
    Text(s.soc?.let{"$it%"} ?: "확인 필요",style=IviType.title)
    if(s.socSource==Source.MANUAL) {
        Text("직접 확인한 잔량이에요. 차량의 현재 잔량이 달라지면 다시 입력해 주세요.",style=IviType.label)
        s.socReceivedAtMillis?.let{Text("입력 시각 · ${java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))}",style=IviType.label)}
    }
    if(s.batteryStatus!=ReadingStatus.AVAILABLE || s.socSource==Source.MANUAL){
        var value by androidx.compose.runtime.saveable.rememberSaveable(s.tripId){mutableStateOf("")}
        OutlinedTextField(value,{if(it.length<=3 && it.all(Char::isDigit))value=it},
            label={Text("직접 확인한 배터리 잔량 (%)")},singleLine=true,modifier=Modifier.fillMaxWidth())
        val parsed=value.toIntOrNull()
        IviButton("직접 입력한 잔량 사용",size=IviButtonSize.MEDIUM,enabled=parsed!=null && parsed in 0..100){
            send(JourneyEvent.Soc(parsed));value=""
        }
    }
}
