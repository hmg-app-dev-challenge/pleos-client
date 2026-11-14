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
import com.chapark.app.data.PlaceEvidenceCatalog
import com.chapark.app.data.Scenery
import com.chapark.app.domain.*
import com.chapark.ivi.design.*
import com.chapark.prototype.*

private val steps=listOf("차박 계획","출발 준비","이동","차박","철수와 기록")
private fun stageLabel(s:Journey)=s.stage?.let{"${it.ordinal+1} / 5 · ${steps[it.ordinal]}"} ?: "오늘의 차박"
private val names get()=com.chapark.app.data.PlaceCatalog.all.associate { it.id to it.name }
private fun condition(value:Condition)=when(value){Condition.ALLOWED->"허용";Condition.RESTRICTED->"제한";Condition.UNKNOWN->"미확인"}

@Composable fun JourneyScreen(ui:JourneyUi,send:(JourneyEvent)->Unit,onNavigation:()->Unit={},onDeleteHistory:(String)->Unit={},onConfirmWaypoint:()->Unit={},onForkPlan:()->Unit={},onLoadPlans:()->Unit={},onImportPlan:(String)->Unit={}) {
    val s=ui.journey
    var showHome by androidx.compose.runtime.saveable.rememberSaveable(s.tripId) { mutableStateOf(false) }
    val atHome=s.step==0 || showHome
    var scenery by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var confirmedOnly by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val candidates=PlaceEvidenceCatalog.filter(scenery?.let{Scenery.valueOf(it)},confirmedOnly,java.time.LocalDate.now())
    var importId by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var settings by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }
    BackHandler(!atHome && !s.restricted && !settings && !history && deleteId==null && confirm==null) { showHome=true }
    IviTheme(s.dark,nightMode=!atHome && s.step==5 && s.restMode!="REST" && !s.restricted) {
        val p=LocalIviColors.current
        Surface(Modifier.fillMaxSize(),color=p.background) {
            if(!ui.loaded) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Text("저장된 여행을 불러오고 있어요",style=IviType.title) }
            else Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal=40.dp,vertical=24.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                    Text("차Park",style=IviType.title,modifier=Modifier.weight(1f))
                    if(!atHome) IviButton("홈",Modifier.testTag("journey-home"),size=IviButtonSize.MEDIUM,enabled=!s.restricted && !ui.saving){showHome=true}
                    IviButton("지난 기록",size=IviButtonSize.MEDIUM,enabled=!s.restricted,onClick={history=true})
                    IviButton("설정",size=IviButtonSize.MEDIUM,enabled=!s.restricted,onClick={settings=true})
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                    Text(if(ui.saving)"저장 중" else if(ui.error!=null)"저장 오류" else "기기에 저장됨",style=IviType.label,color=p.tertiary)
                }
                if(s.step==0 && !s.restricted && com.chapark.ivi.BuildConfig.DEBUG) {
                    IviButton("저장된 계획 가져오기",size=IviButtonSize.MEDIUM,onClick={importId=null;onLoadPlans()})
                }
                if(ui.catalogStatus.isNotBlank()) Text(ui.catalogStatus,style=IviType.label,color=p.tertiary)
                if(ui.syncStatus!="기기에 저장됨") Text(ui.syncStatus,style=IviType.label,color=p.tertiary)
                if("계획 충돌" in ui.syncStatus && !s.restricted && !s.completed) IviButton("이 기기 계획을 별도로 저장",size=IviButtonSize.MEDIUM,onClick={confirm="fork"})
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
                        Text(stageLabel(s),style=IviType.title)
                        if(s.batteryVisible) Text("배터리 ${s.soc?.let{"$it%"} ?: "확인 필요"} · ${sourceLabel(s.socSource)}",style=IviType.title)
                        Text(if(s.route==RoutePhase.GUIDING)"길 안내 중" else "길 안내 상태 확인 필요",style=IviType.title)
                    }
                } else if(atHome && s.step>0) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("journey-home-content"),verticalArrangement=Arrangement.spacedBy(24.dp)) {
                        Heading(if(s.completed)"다음 쉼을 준비해요" else "준비하던 여행을\n이어서 시작해요",if(s.completed)"지난 여행의 기억을 확인하고 새로운 계획을 세워보세요." else "선택한 장소와 확인한 항목을 저장해 두었어요.")
                        Panel {
                            Text(if(s.completed)"최근 완료한 여행" else "진행 중인 여행",style=IviType.smallTitle)
                            Text(s.place.name,style=IviType.title)
                            Text(s.input.schedule?.date ?: "여행 날짜 확인 필요",style=IviType.smallBody)
                            Text(if(s.completed)"철수 완료" else steps[s.stage!!.ordinal],style=IviType.smallTitle)
                            Text("${s.input.people}명 · ${s.input.cookingPlan.label}",style=IviType.smallBody)
                            if(s.batteryVisible)Text("배터리 ${s.soc?.let{"$it%"} ?: "미확인"} · ${sourceLabel(s.socSource)}",style=IviType.smallBody)
                            if(!s.completed)Text("출발 준비 ${s.departureCheckedCount} / ${s.departureProgress.total}",style=IviType.label)
                            if(s.route==RoutePhase.GUIDING && !s.completed)Text("길 안내 중",style=IviType.smallBody)
                        }
                        IviButton("새 차박 시작",size=IviButtonSize.MEDIUM,enabled=!ui.saving){confirm="new-trip"}
                    }
                    HorizontalDivider(color=p.divider)
                    IviButton(if(s.completed)"완료한 여행 보기" else "이어하기",Modifier.fillMaxWidth().padding(top=20.dp).testTag("journey-resume"),filled=true,enabled=!ui.saving){showHome=false}
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
                                    if(s.step==4 && ui.waypointNeedsConfirmation) Panel {
                                        Text("충전 경유지 위치 확인",style=IviType.smallTitle)
                                        Text("내비게이션에서 선택한 충전시설이 경유지로 표시되는지 확인해 주세요.",style=IviType.body)
                                        IviButton("경유지 확인을 위해 내비 열기",onClick=onNavigation)
                                        IviButton("경유지 위치를 확인했어요",filled=true,onClick=onConfirmWaypoint)
                                    }
                                    if(s.step in 1..2) {
                                        Text("차박 계획",style=IviType.title)
                                        IviSegments(listOf("여행 조건","장소 비교"),s.step-1){send(JourneyEvent.PlanningTab(it))}
                                    }
                                    JourneyContent(s,send,candidates,scenery,confirmedOnly,{scenery=it},{confirmedOnly=it})
                                    if(s.step>0 && s.step!=2 && !s.completed) IviButton("이전 단계",size=IviButtonSize.MEDIUM,enabled=!ui.saving){send(JourneyEvent.Back)}
                                    Spacer(Modifier.height(24.dp))
                                }
                            }
                            if(wide && s.step !in 1..2) JourneySummary(s,Modifier.width(340.dp).fillMaxHeight())
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color=p.divider)
                    if(s.step==3) s.departureBlockReason?.let {
                        Text(it,style=IviType.label,color=p.secondary,modifier=Modifier.padding(top=12.dp).testTag("departure-block-reason"))
                    }
                    JourneyFooter(s,ui.saving || (s.step==2 && candidates.none{it.place.id==s.placeId})) {
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
                    trip.records.filter{it.subject==null}.sortedBy{it.eventAtMillis ?: it.observedAtMillis}.forEach{record->
                        val time=java.time.Instant.ofEpochMilli(record.eventAtMillis ?: record.observedAtMillis).atZone(runCatching{java.time.ZoneId.of(trip.zone)}.getOrDefault(java.time.ZoneOffset.UTC)).toLocalTime().withNano(0)
                        val label=when(record.kind){"PLACE_INFO"->"장소 자료 · 당시 확인 정보";"SUNSET"->"일몰 · 계산";"SUNRISE"->"일출 · 계산";"PLACE_SELECTED"->"장소 선택";"CHARGING_PLAN"->"충전 경유 계획";"PREPARATION_COMPLETED"->"출발 준비 확인";"ROUTE_REQUESTED"->"길 안내 요청";"GUIDANCE_OBSERVED"->"길 안내 확인";"ARRIVAL"->"도착 확인";"PARKING_CONFIRMED"->"주차 확인";"TRIP_COMPLETED"->"철수 완료";else->"상태 확인"}
                        Text("$time · $label",style=IviType.smallBody)
                    record.label?.let{Text(it,style=IviType.label)}
                    }
                    if(trip.forgotten.isNotEmpty())Text("다음에 챙길 것 · ${trip.forgotten.joinToString()}",style=IviType.smallBody)
                    IviButton("이 기록 삭제",size=IviButtonSize.MEDIUM){deleteId=trip.id}
                    var details by androidx.compose.runtime.saveable.rememberSaveable(trip.id){mutableStateOf(false)}
                    IviButton(if(details)"확인 항목 접기" else "확인 항목 펼치기",size=IviButtonSize.MEDIUM){details=!details}
                    if(details)trip.records.filter{it.subject!=null}.forEach{record->
                        Text("${if(record.kind=="PREPARATION_OMITTED") { if(record.confirmed==true)"누락" else "누락 취소" } else if(record.confirmed==true)"확인" else "확인 해제"} · ${record.label ?: record.subject}",style=IviType.smallBody)
                    }
                    HorizontalDivider()
                }
                IviButton("기록 닫기"){history=false}
            }
        }
        if(ui.savedPlans.isNotEmpty() && importId==null && !s.restricted && s.step==0) androidx.compose.ui.window.Dialog(onDismissRequest={importId="dismissed"}) {
            Panel(Modifier.heightIn(max=800.dp).verticalScroll(rememberScrollState())){
                Text("계획 조건 가져오기",style=IviType.title)
                Text("선택한 조건으로 새 여행을 만듭니다. 완료 체크와 차량 상태는 가져오지 않습니다.",style=IviType.smallBody)
                ui.savedPlans.forEach{plan->IviButton(plan.title,size=IviButtonSize.MEDIUM,onClick={importId=plan.id})}
                IviButton("닫기",onClick={importId="dismissed"})
            }
        }
        importId?.takeIf{it!="dismissed" && !s.restricted}?.let{id->IviConfirm("이 조건으로 새 여행을 만들까요?","서버의 원본은 유지하고 준비 확인을 처음부터 진행합니다.","가져오기",{importId=null;onImportPlan(id)},{importId=null})}
        if(confirm=="fork" && !s.restricted) IviConfirm("계획을 별도로 보관할까요?","이 기기의 계획을 새 여행으로 보관합니다. 서버의 기존 계획은 덮어쓰지 않습니다.","별도로 보관",{confirm=null;onForkPlan()},{confirm=null})
        deleteId?.takeIf{!s.restricted}?.let{id->IviConfirm("여행 기록을 삭제할까요?","이 기기에 보관한 해당 여행 기록을 삭제합니다.","삭제",{deleteId=null;onDeleteHistory(id)},{deleteId=null})}
        if(settings && !s.restricted) JourneySettings(s,send,onClose={settings=false},onReset={settings=false;confirm="reset"})
        when(if(s.restricted)null else confirm) {
            "new-trip" -> IviConfirm("새 차박을 시작할까요?",if(s.completed)"완료한 기록은 보관하고 새 여행을 준비합니다." else "진행 중인 계획과 확인 항목을 새 여행으로 바꿉니다. 완료한 여행 기록은 유지합니다.","새로 시작",{confirm=null;showHome=false;send(JourneyEvent.NewTrip)},{confirm=null})
            "finish" -> IviConfirm("오늘의 차박을 마칠까요?","확인한 준비와 철수 항목을 여행 기록에 저장합니다.","종료",{confirm=null;send(JourneyEvent.Finish)},{confirm=null})
            "reset" -> IviConfirm("여행 계획을 새로 만들까요?","현재 여행의 준비·확인 기록을 지우고 새 계획을 시작합니다.","처음부터",{confirm=null;send(JourneyEvent.ResetScenario)},{confirm=null})
        }
    }
}
@Composable private fun JourneyProgress(s:Journey) {
    val stage=s.stage?.ordinal ?: return
    val p=LocalIviColors.current
    BoxWithConstraints {
        val compact=maxWidth<1000.dp
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(compact) Text(stageLabel(s),style=IviType.label)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                steps.forEachIndexed{i,label->
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        if(!compact) Text("${i+1} $label",style=IviType.label,color=if(i==stage)p.ink else p.tertiary,maxLines=1)
                        Box(Modifier.fillMaxWidth().height(4.dp).background(if(i<=stage)p.green else p.divider))
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
            Text(stageLabel(s),style=IviType.label,color=p.tertiary)
            IviButton(action.first,Modifier.fillMaxWidth().testTag("journey-next"),filled=true,enabled=action.second && !saving,onClick=onNext)
        } else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(stageLabel(s),Modifier.weight(1f),style=IviType.label,color=p.tertiary)
            IviButton(action.first,Modifier.widthIn(max=620.dp).testTag("journey-next"),filled=true,enabled=action.second && !saving,onClick=onNext)
        }
    }
}
private fun primary(s:Journey):Pair<String,Boolean> = when {
    s.completed -> "새 여행 준비" to true
    s.step==0 -> "오늘 밤 차박 시작" to true
    s.step==1 -> "장소 후보 비교" to (s.input.valid && s.input.schedule?.departureEnd(s.input.endHour)!=null && !s.input.cookingNeedsReview)
    s.step==2 -> "이 장소 선택" to (s.input.valid && s.input.schedule?.departureEnd(s.input.endHour)!=null && !s.input.cookingNeedsReview)
    s.step==3 -> "준비 완료" to s.canDepart
    s.step==4 -> when(s.route) {
        RoutePhase.NONE -> "길 안내 시작" to true
        RoutePhase.REQUESTED -> "경로를 찾고 있어요" to false
        RoutePhase.GUIDING -> "내비게이션 열기" to true
        RoutePhase.FAILED -> "다시 시도" to true
        RoutePhase.ARRIVED -> (if(s.parked)"수면 준비로" else "주차했어요") to true
    }
    s.step==5 -> "철수 시작" to (s.canSleep && s.siteReady && s.sleepChecks.containsAll(s.requiredSleepIds))
    else -> "철수 완료" to s.canFinish
}
@Composable private fun Panel(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    Column(modifier.fillMaxWidth().background(LocalIviColors.current.surface,RoundedCornerShape(32.dp)).padding(28.dp),verticalArrangement=Arrangement.spacedBy(20.dp),content=content)
}
@Composable private fun Heading(title:String,subtitle:String) {
    Text(title,style=IviType.headline)
    Text(subtitle,style=IviType.body,color=LocalIviColors.current.secondary)
}
@Composable private fun CookingSelection(s:Journey,send:(JourneyEvent)->Unit){
    Text("취사 계획",style=IviType.smallTitle)
    if(s.input.cookingNeedsReview)Text("이전 여행의 취사 계획을 다시 선택해 주세요.",style=IviType.label)
    CookingPlan.entries.forEach { plan->
        IviButton(plan.label,size=IviButtonSize.MEDIUM,filled=s.input.cookingPlan==plan) {
            send(JourneyEvent.Conditions(s.input.copy(cookingPlan=plan,cookingNeedsReview=false)))
        }
    }
}

@Composable private fun JourneyContent(s:Journey,send:(JourneyEvent)->Unit,
    candidates:List<com.chapark.app.data.NavigationPlace>,scenery:String?,confirmedOnly:Boolean,
    onScenery:(String?)->Unit,onConfirmed:(Boolean)->Unit) {
    val p=LocalIviColors.current
    when(s.step) {
        0 -> {
            Heading("오늘 밤,\n내 차로 떠나는 쉼.","어디서 어떻게 쉴지, 오늘 밤의 작은 여행을 준비해요.")
            Panel {
                Text("오늘의 여행",style=IviType.title)
                if(s.followUps.isNotEmpty())Text("지난 여행의 미확인 정보 · ${s.followUps.joinToString()}",style=IviType.smallBody)
                Text("내 차와 함께하는 하룻밤",style=IviType.body)
                if(s.batteryVisible) Text("배터리 ${s.soc?.let{"$it%"} ?: "미확인"} · ${sourceLabel(s.socSource)}",style=IviType.smallTitle)
                Text("${s.input.people}인 · ${s.input.cookingPlan.label} · ${if(s.input.sleepingBag)"침낭 보유" else "침낭 미보유"}",style=IviType.body)
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
                AdjustValue("인원", "${s.input.people}명",s.input.people>1,s.input.people<TripPolicy.MAX_PEOPLE,
                    {send(JourneyEvent.Conditions(s.input.copy(people=s.input.people-1)))},
                    {send(JourneyEvent.Conditions(s.input.copy(people=s.input.people+1)))})
                if(s.batteryVisible) AdjustValue("배터리 확인 기준", "${s.input.batteryThreshold}%",s.input.batteryThreshold>0,s.input.batteryThreshold<100,
                    {send(JourneyEvent.Conditions(s.input.copy(batteryThreshold=(s.input.batteryThreshold-5).coerceAtLeast(0))))},
                    {send(JourneyEvent.Conditions(s.input.copy(batteryThreshold=(s.input.batteryThreshold+5).coerceAtMost(100))))})
                AdjustValue("철수 예정", "다음 날 ${s.input.endHour}시",true,true,
                    {send(JourneyEvent.Conditions(s.input.copy(endHour=(s.input.endHour+23)%24)))},
                    {send(JourneyEvent.Conditions(s.input.copy(endHour=(s.input.endHour+1)%24)))})
                CookingSelection(s,send)
                IviToggleRow("반려동물과 함께",s.input.pets) {send(JourneyEvent.Conditions(s.input.copy(pets=it)))}
                IviToggleRow("침낭을 보유하고 있어요",s.input.sleepingBag) {send(JourneyEvent.Conditions(s.input.copy(sleepingBag=it)))}
            }
        }
        2 -> {
            Heading("오늘의 쉼터를 비교해요","숙박 조건과 필요한 준비를 비교하고, 오늘의 쉼터를 골라보세요.")
            IviSegments(listOf("전체","바다","산","강","숲"),listOf(null,"SEA","MOUNTAIN","RIVER","FOREST").indexOf(scenery)){
                onScenery(listOf(null,"SEA","MOUNTAIN","RIVER","FOREST")[it])
            }
            IviToggleRow("이용 조건이 확인된 곳만",confirmedOnly,onConfirmed)
            Text("주차·차내 숙박·야간 화장실과 최근 근거가 모두 확인된 후보를 골라요.",style=IviType.label)
            if(candidates.isEmpty()) Panel {
                Text("조건에 맞는 장소가 없어요",style=IviType.title)
                Text("풍경 또는 이용 조건 필터를 바꿔 비교할 수 있어요.",style=IviType.body)
                NavigationPlaces.all.filter{scenery==null || PlaceEvidenceCatalog.find(it.place.id).scenery.name==scenery}.forEach{
                    Text("${it.place.name} · ${PlaceEvidenceCatalog.find(it.place.id).exclusions(it,java.time.LocalDate.now()).joinToString()}",style=IviType.smallBody)
                }
                IviButton("전체 장소 보기",size=IviButtonSize.MEDIUM){onScenery(null);onConfirmed(false)}
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if(maxWidth>=1200.dp) Column(verticalArrangement=Arrangement.spacedBy(20.dp)) {
                    candidates.chunked(3).forEach { row->
                        Row(horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                            row.forEach{place->PlaceOverview(s,place,Modifier.weight(1f),send)}
                            repeat(3-row.size){Spacer(Modifier.weight(1f))}
                        }
                    }
                } else Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                    candidates.forEach{place->PlaceOverview(s,place,Modifier.width(420.dp),send)}
                }
            }
            candidates.find{it.place.id==s.placeId}?.let { selected->
                Text("선택한 장소의 상세 정보",style=IviType.title)
                PlaceCard(s,selected.place,candidates.indexOf(selected),Modifier,send)
            }
            if(candidates.none{it.place.id==s.placeId} && candidates.isNotEmpty())Text("목록에서 이번 여행의 장소를 선택해 주세요.",style=IviType.label)
        }

        3 -> {
            SolarSummary(s)
            Heading("오늘의 준비 계획","${names[s.placeId] ?: s.place.name} · 조건을 바꾸면 필요한 행동도 달라져요.")
            Panel {
                if(s.batteryVisible) BatteryReading(s,send)
                Text("출발 전 필수 ${s.departureCheckedCount} / ${s.departureProgress.total}개 확인 · 날짜와 준비 항목 확인 후 이동할 수 있어요",style=IviType.label,color=p.tertiary)
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
            if(s.input.cookingNeedsReview) Panel { CookingSelection(s,send) }
            if(s.place.nightToilet!=Condition.ALLOWED) Panel {
                Text(if(s.place.nightToilet==Condition.UNKNOWN)"야간 운영 여부 확인 필요" else "야간 화장실 이용 불가",style=IviType.smallTitle)
                IviButton("다른 장소 비교",size=IviButtonSize.MEDIUM){send(JourneyEvent.Back)}
            }
            SelectedCharger(s,send)
            if(s.needsChargingReview) Panel(Modifier.testTag("charging-review")) {
                Text("충전시설 후보",style=IviType.smallTitle)
                Text("목적지 지역에 연결한 후보예요. 경로와 우회 거리, 커넥터와 운영 조건을 내비와 출처에서 확인해 주세요.",style=IviType.smallBody)
                IviButton("경유 없이 이동",size=IviButtonSize.MEDIUM,filled=s.chargerId==null){send(JourneyEvent.Charger(null))}
                val uri=androidx.compose.ui.platform.LocalUriHandler.current
                val chargers=com.chapark.app.data.ChargingPlaces.forPlace(s.placeId)
                if(chargers.isEmpty())Text("이 장소에 연결된 충전 후보가 아직 없어요. 내비게이션에서 충전시설을 확인해 주세요.",style=IviType.smallBody)
                chargers.forEach{charger->
                    Text(charger.name,style=IviType.smallTitle)
                    Text(charger.details,style=IviType.smallBody)
                    Text("${charger.address} · 자료 확인 ${charger.checkedOn}",style=IviType.label)
                    IviButton("충전시설 운영 정보",size=IviButtonSize.MEDIUM){runCatching{uri.openUri(charger.source)}}
                    IviButton(if(s.chargerId==charger.id)"경유지 선택됨" else "충전 경유지로 선택",filled=s.chargerId==charger.id,size=IviButtonSize.MEDIUM){send(JourneyEvent.Charger(charger.id))}
                }
            }
            Text("현장 운영 안내는 도착 후 확인해요. 체크는 이용 허가를 뜻하지 않아요.",style=IviType.smallBody)
            val reminders=s.departureItems.filter{it.id.startsWith("reminder-")}
            if(reminders.isNotEmpty()) Panel {
                Text("지난번에 빠뜨린 준비물",style=IviType.title)
                reminders.forEach{PreparationItem(s,it,send)}
            }
            Group.entries.filter{it!=Group.SITE}.forEach { group ->
                val items=s.decisions.filter{it.group==group && !it.id.startsWith("reminder-")}.sortedBy { when(it.id){"charge-review"->0;"meal-ready"->1;else->2} }
                if(items.isNotEmpty()) Panel {
                    Text(group.label,style=IviType.title)
                    items.forEach {item-> PreparationItem(s,item,send) }
                }
            }
        }
        4 -> {
            Heading("${names[s.placeId] ?: s.place.name}까지","차량 내비게이션에서 길 안내를 이어가세요.")
            Text(NavigationPlaces.find(s.placeId)?.address ?: "주소 확인 필요",style=IviType.smallBody)
            SelectedCharger(s,send)
            Panel {
                Text(when(s.route){RoutePhase.NONE->"이동 준비";RoutePhase.REQUESTED->"경로를 탐색하고 있어요";RoutePhase.GUIDING->"길 안내 중";RoutePhase.FAILED->"이동 정보를 불러오지 못했어요";RoutePhase.ARRIVED->if(s.parked)"도착 · 주차 완료" else "목적지 도착 · 주차 확인 필요"},style=IviType.title)
                Text(if(s.decisions.any{it.id=="charge-review"})"배터리가 부족할 수 있어요. 출발 전 충전 계획을 확인해 주세요." else "목적지와 경로를 확인하고, 현장 운영 안내를 따라주세요.",style=IviType.body)
                if(s.route==RoutePhase.GUIDING) IviButton("도착했어요",size=IviButtonSize.MEDIUM){send(JourneyEvent.Arrive)}
                if(s.route==RoutePhase.FAILED) Text("잠시 후 다시 시도해 주세요. 선택한 목적지와 준비 계획은 저장되어 있어요.",style=IviType.smallBody)
                if(s.route==RoutePhase.ARRIVED && !s.parked) Text("안전한 곳에 주차한 뒤 주차 완료를 확인해 주세요.",style=IviType.smallBody)
            }
        }
        5 -> {
            Panel { s.siteItems.forEach {item->PreparationItem(s,item,send)} }
            IviSegments(listOf("휴식","수면","별 보기"),listOf("REST","SLEEP","STARS").indexOf(s.restMode).coerceAtLeast(0)){send(JourneyEvent.RestMode(listOf("REST","SLEEP","STARS")[it]))}
            SolarSummary(s)
            Heading("잠들기 전, 하나씩 확인해요","편안히 쉴 수 있도록 주변과 차량 설정을 확인해 주세요.")
            Panel {
                Text("편안한 휴식 준비",style=IviType.smallTitle)
                Text("차량의 공조와 조명을 원하는 상태로 맞춘 뒤 체크해 주세요.",style=IviType.smallBody)
                JourneyChecklists.sleep(s.checklistVersion).forEach{(id,title)->IviCheckRow(title,id in s.sleepChecks){send(JourneyEvent.SleepCheck(id))}}
            }
            Text("차내 화기를 사용하지 마세요. 실제 사용은 차량 사용 설명서와 현장 운영 안내를 따르세요.",style=IviType.label,color=p.tertiary)
        }
        6 -> if(s.completed) {
            Heading("오늘의 차박을 마쳤어요","내 차와 보낸 하룻밤의 기억을 저장했어요.")
            Panel {
                Text(names[s.placeId] ?: "오늘의 쉼터",style=IviType.title)
                Text("수면 준비 ${s.sleepChecks.intersect(s.requiredSleepIds).size}/${s.requiredSleepIds.size} · 철수 확인 ${s.exitChecks.intersect(s.requiredExitIds).size}/${s.requiredExitIds.size}",style=IviType.body)
                Text("직접 확인한 준비 항목 ${s.checkedCount}/${s.decisions.size}",style=IviType.body)
                val missingItems=s.forgotten+s.decisions.filter{it.id in s.omitted}.map{it.title}
                if(missingItems.isNotEmpty())Text("다음에 챙길 것 · ${missingItems.sorted().joinToString()}",style=IviType.smallBody)
                Text("내 차와 보낸 하룻밤 기억",style=IviType.smallTitle)
                val zone=runCatching{java.time.ZoneId.of(s.input.schedule?.zoneId ?: "UTC")}.getOrDefault(java.time.ZoneOffset.UTC)
                s.records.filter{it.subject==null}.sortedBy{it.eventAtMillis ?: it.observedAtMillis}.forEach { record->
                    val label=when(record.kind){"PLACE_INFO"->"장소 자료 · 당시 확인 정보";"SUNSET"->"일몰 · 계산";"SUNRISE"->"일출 · 계산";"PLACE_SELECTED"->"장소 선택";"CHARGING_PLAN"->"충전 경유 계획";
                        "PREPARATION_COMPLETED"->"출발 준비 확인";"ROUTE_REQUESTED"->"길 안내 요청"
                        "GUIDANCE_OBSERVED"->"길 안내 확인";"ARRIVAL"->if(record.origin=="SDK")"도착 알림 수신" else "도착 직접 확인"
                        "PARKING_CONFIRMED"->"주차 직접 확인";"TRIP_COMPLETED"->"철수 확인 완료";else->"여행 상태 확인"
                    }
                    val time=java.time.Instant.ofEpochMilli(record.eventAtMillis ?: record.observedAtMillis).atZone(zone).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                    Text("$time · $label",style=IviType.smallBody)
                    record.label?.let{Text(it,style=IviType.label)}
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
                JourneyChecklists.exit(s.checklistVersion).forEach{(id,title)->IviCheckRow(title,id in s.exitChecks){send(JourneyEvent.ExitCheck(id))}}
            }
        }
    }
}
@Composable private fun SelectedCharger(s:Journey,send:(JourneyEvent)->Unit) {
    com.chapark.app.data.ChargingPlaces.find(s.chargerId)?.let { charger->
        Panel(Modifier.testTag("selected-charger")) {
            Text("선택한 경유지 · ${charger.name}",style=IviType.smallTitle)
            Text(charger.address,style=IviType.smallBody)
            Text("조건이나 목적지가 바뀌어도 이전 선택은 유지돼요. 이번 경로에 맞는지 확인해 주세요.",style=IviType.label)
            if(com.chapark.app.data.ChargingPlaces.forPlace(s.placeId).none{it.id==charger.id})Text("현재 목적지의 연결 후보가 아니에요. 내비에서 경로를 다시 확인해 주세요.",style=IviType.label)
            if(s.step==3 || (s.step==4 && s.route in setOf(RoutePhase.NONE,RoutePhase.FAILED)))
                IviButton("선택한 경유지 해제",Modifier.testTag("clear-charger"),size=IviButtonSize.MEDIUM){send(JourneyEvent.Charger(null))}
        }
    }
}

@Composable internal fun PreparationItem(s:Journey,item:DecisionItem,send:(JourneyEvent)->Unit) {
    val omitted=item.id in s.omitted
    Column(Modifier.fillMaxWidth().testTag("preparation-${item.id}"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("${if(item.required)"필수" else "선택 · 필수 진행률에서 제외"}${item.quantity?.let{" · 수량 $it"} ?: ""}",style=IviType.label,color=LocalIviColors.current.secondary)
        IviCheckRow(item.title,item.id in s.checked && !omitted,item.reason){send(JourneyEvent.Check(item.id))}
        if(omitted) {
            Text(if(item.group==Group.GEAR)"이번에는 챙기지 못함" else "이번에는 확인하지 못함",Modifier.testTag("omitted-${item.id}"),style=IviType.smallBody)
            if(item.required)Text("필수 항목은 누락 상태로 다음 단계에 갈 수 없어요. 항목을 확인하거나 여행 조건을 다시 검토해 주세요.",style=IviType.label)
        }
        IviButton(if(omitted)"누락 표시 취소" else if(item.group==Group.GEAR)"이번에는 챙기지 못함" else "이번에는 확인하지 못함",Modifier.testTag("omit-${item.id}"),size=IviButtonSize.MEDIUM){send(JourneyEvent.Omit(item.id))}
        HorizontalDivider(color=LocalIviColors.current.divider)
    }
}

@Composable private fun PlaceOverview(s:Journey,entry:com.chapark.app.data.NavigationPlace,modifier:Modifier,send:(JourneyEvent)->Unit) {
    val place=entry.place
    val evidence=PlaceEvidenceCatalog.find(place.id)
    val p=LocalIviColors.current
    Panel(modifier.testTag("place-overview-${place.id}").then(if(s.placeId==place.id)Modifier.border(2.dp,p.green,RoundedCornerShape(32.dp)) else Modifier)) {
        Text(place.name,style=IviType.smallTitle)
        Text(entry.address,style=IviType.label,color=p.secondary)
        Text("주차 · ${condition(evidence.parking)}\n차내 숙박 · ${condition(place.overnight)}\n야간 화장실 · ${condition(place.nightToilet)}\n취사 · ${condition(place.cooking)}",style=IviType.label)
        Text("${evidence.scenery.label} · 조망 확인 필요",style=IviType.label)
        Text(if(com.chapark.app.data.ChargingPlaces.forPlace(place.id).isEmpty())"충전시설 · 연결 후보 확인 필요" else "충전시설 · 지역 후보 ${com.chapark.app.data.ChargingPlaces.forPlace(place.id).size}곳 · 경로 확인 필요",style=IviType.label,color=p.secondary)
        val dates=evidence.facts.mapNotNull{it.checkedOn}.distinct()
        Text("자료 확인 · ${dates.joinToString().ifBlank{"미확인"}}${if(evidence.facts.any{it.checkedOn==null})" · 일부 미확인" else ""}",style=IviType.label)
        Text("출처 · ${runCatching{java.net.URI(entry.source).host}.getOrNull() ?: "확인 필요"}",style=IviType.label,color=p.secondary)
        IviButton(if(s.placeId==place.id)"상세 확인 중" else "상세 비교",size=IviButtonSize.MEDIUM,filled=s.placeId==place.id){send(JourneyEvent.Place(place.id))}
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
        val evidence=PlaceEvidenceCatalog.find(place.id)
        Text("주변 풍경 · ${evidence.scenery.label} · 조망 방향 확인 필요",style=IviType.label)
        Text("지도 대표 위치 · 차량 진입구 확인 필요",style=IviType.label,color=p.tertiary)
        val uri=androidx.compose.ui.platform.LocalUriHandler.current
        evidence.facts.forEach{fact->
            Text("${fact.label} · ${fact.detail}",style=IviType.smallBody)
            Text("자료 확인 · ${fact.checkedOn ?: "미확인"}${if(!fact.fresh(java.time.LocalDate.now()))" · 최신 운영 정보 확인 필요" else ""}",style=IviType.label,color=p.tertiary)
        }
        evidence.facts.mapNotNull{it.url}.distinct().forEach{url->
            Text("운영 정보 출처 · ${java.net.URI(url).host}",style=IviType.label,color=p.tertiary)
            IviButton("운영 근거 보기",size=IviButtonSize.MEDIUM){runCatching{uri.openUri(url)}}
        }
        NavigationPlaces.find(place.id)?.source?.let{url->
            IviButton("공식 장소 정보",size=IviButtonSize.MEDIUM){runCatching{uri.openUri(url)}}
        }
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
        Text("${s.input.people}인\n${s.input.cookingPlan.label}\n${if(s.input.sleepingBag)"침낭 보유" else "침낭 미보유"}",style=IviType.body)
        HorizontalDivider(color=p.divider)
        Text("출발 전 필수 ${s.departureCheckedCount}/${s.departureProgress.total}개 확인",style=IviType.smallTitle)
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
    SolarSky(date,zone,today)
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
