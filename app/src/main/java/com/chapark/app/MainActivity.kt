package com.chapark.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chapark.app.data.DemoPlaces
import com.chapark.app.domain.*
import com.chapark.app.ui.*
import com.chapark.prototype.*

class MainActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState);setContent { ChaParkApp() } }
}
private val steps=listOf("오늘 밤 시작","조건 확인","장소 비교","오늘의 계획","이동","수면 준비","철수")

@Composable fun ChaParkApp(vm:TripViewModel=viewModel()) {
    val s by vm.state.collectAsState()
    var settings by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }
    val send=vm::send
    BackHandler(s.step>0 && !s.completed && !settings && confirm==null) {send(JourneyEvent.Back)}
    PleosTheme(dark=s.dark) {
        val p=LocalPleos.current
        Surface(Modifier.fillMaxSize(),color=p.background) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),contentAlignment=Alignment.TopCenter) {
                Column(Modifier.widthIn(max=520.dp).fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text("차Park",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
                        TextButton(onClick={settings=true}){Text("설정")}
                    }
                    Column(Modifier.padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text("${s.step+1} / 7 · ${steps[s.step]}",style=MaterialTheme.typography.labelMedium,color=p.secondary)
                        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) { repeat(7){i->Box(Modifier.weight(1f).height(3.dp).background(if(i<=s.step)p.green else p.divider))} }
                    }
                    key(s.step,s.completed) {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                            Content(s,send)
                            if(s.step>0 && !s.completed) PleosButton("이전 단계",filled=false){send(JourneyEvent.Back)}
                        }
                    }
                    val action=primary(s)
                    Column(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=12.dp)) {
                        PleosButton(action.first,modifier=Modifier.testTag("journey-next"),enabled=action.second) {
                            when {
                                s.completed->send(JourneyEvent.NewTrip)
                                s.step==6->confirm="finish"
                                s.step==4->when(s.route) {
                                    RoutePhase.NONE,RoutePhase.FAILED->send(JourneyEvent.RequestRoute)
                                    RoutePhase.REQUESTED->Unit
                                    RoutePhase.GUIDING->send(JourneyEvent.Arrive)
                                    RoutePhase.ARRIVED->send(if(s.parked)JourneyEvent.Next else JourneyEvent.Park)
                                }
                                else->send(JourneyEvent.Next)
                            }
                        }
                    }
                }
            }
        }
        if(settings) Dialog(onDismissRequest={settings=false}) {
            Panel {
                Text("설정",style=MaterialTheme.typography.headlineSmall)
                Choice("어두운 화면",s.dark){send(JourneyEvent.Dark(it))}
                PleosButton("새 여행 계획",filled=false){settings=false;confirm="reset"}
                PleosButton("설정 닫기"){settings=false}
            }
        }
        confirm?.let { kind ->
            val title=when(kind){"reset"->"여행 계획을 새로 만들까요?";"finish"->"오늘의 차박을 마칠까요?";else->"미확인 항목이 남아 있어요"}
            val body=when(kind){"reset"->"현재 여행의 준비·확인 기록을 지우고 새 계획을 시작합니다.";"finish"->"확인한 준비와 철수 항목을 여행 기록에 저장합니다.";else->"${s.decisions.size-s.checkedCount}개 항목을 아직 확인하지 않았습니다. 출발 전 필요한 준비를 확인해 주세요."}
            Dialog(onDismissRequest={confirm=null}) {
                Panel {
                    Text(title,style=MaterialTheme.typography.headlineSmall);Text(body)
                    PleosButton(when(kind){"reset"->"처음부터";"finish"->"종료";else->"확인"}) {
                        confirm=null;send(when(kind){"reset"->JourneyEvent.ResetScenario;"finish"->JourneyEvent.Finish;else->JourneyEvent.Back})
                    }
                    PleosButton("취소",filled=false){confirm=null}
                }
            }
        }
    }
}
private fun primary(s:Journey):Pair<String,Boolean> = when {
    s.completed->"새 여행 준비" to true
    s.step==0->"오늘 밤 차박 시작" to true
    s.step==1->"장소 후보 비교" to true
    s.step==2->"선택한 장소로 계획" to true
    s.step==3->"준비 계획 확인 · 이동" to s.canDepart
    s.step==4->when(s.route){
        RoutePhase.NONE->"이동 시작" to true;RoutePhase.REQUESTED->"이동 정보 준비 중" to false
        RoutePhase.GUIDING->"도착했어요" to true;RoutePhase.FAILED->"다시 시도" to true
        RoutePhase.ARRIVED->(if(s.parked)"수면 준비로" else "주차했어요") to true
    }
    s.step==5->"수면 준비 확인 · 다음 날" to (s.canSleep && s.siteReady && s.sleepChecks.containsAll(s.requiredSleepIds))
    else->"차박 종료" to s.canFinish
}
@Composable private fun Content(s:Journey,send:(JourneyEvent)->Unit) {
    when(s.step) {
        0->{
            Heading("오늘 밤,\n내 차로 떠나는 쉼.","장소부터 잠들기 전 준비까지 하나씩 확인하고 편안하게 출발하세요.")
            Panel {
                Text("오늘의 여행",style=MaterialTheme.typography.titleMedium)
                Text("내 차와 함께하는 하룻밤")
                Text("배터리 ${s.soc}% · 저장된 잔량",style=MaterialTheme.typography.titleLarge)
                Text("${s.input.people}인 · ${s.input.cookingPlan.label} · ${if(s.input.sleepingBag)"침낭 보유" else "침낭 미보유"}")
            }
        }
        1->{
            Heading("누구와, 어떻게\n보낼 예정인가요?","이번 여행에 맞는 조건을 알려주세요.")
            Panel {
                Text("여행 날짜 · ${s.input.schedule?.date ?: "선택 전"}")
                Text("시간대 · ${s.input.schedule?.zoneId ?: "선택 전"}")
                PleosButton("오늘 날짜로 계획"){
                    val zone=java.time.ZoneId.systemDefault()
                    send(JourneyEvent.Conditions(s.input.copy(schedule=com.chapark.app.domain.TripSchedule(java.time.LocalDate.now(zone).toString(),zone.id))))
                }
                Text("함께하는 인원");Text("${s.input.people}명")
                PleosButton("인원 줄이기",enabled=s.input.people>1){send(JourneyEvent.Conditions(s.input.copy(people=s.input.people-1)))}
                PleosButton("인원 늘리기",enabled=s.input.people<Int.MAX_VALUE){send(JourneyEvent.Conditions(s.input.copy(people=s.input.people+1)))}}
            if(s.input.cookingNeedsReview)Text("이전 여행의 취사 계획을 다시 선택해 주세요.")
            CookingPlan.entries.forEach { plan->
                Choice(plan.label,s.input.cookingPlan==plan){send(JourneyEvent.Conditions(s.input.copy(cookingPlan=plan,cookingNeedsReview=false)))}
            }
            Choice("반려동물과 함께",s.input.pets){send(JourneyEvent.Conditions(s.input.copy(pets=it)))}
            Choice("침낭을 보유하고 있어요",s.input.sleepingBag){send(JourneyEvent.Conditions(s.input.copy(sleepingBag=it)))}
        }
        2->{
            Heading("오늘의 쉼터를\n비교해 보세요.","숙박 조건과 필요한 준비를 비교해 보세요.")
            DemoPlaces.all.forEach {place->Panel {
                Text(place.name,style=MaterialTheme.typography.titleLarge);Text(place.description)
                Text("숙박 ${label(place.overnight)} · 야간 화장실 ${label(place.nightToilet)}")
                Text("취사 ${label(place.cooking)} · 화기 ${label(place.fire)}")
                Text("예상 기온 ${place.demoTemperatureC}°C · 방문 전 운영 시간 확인 필요")
                PleosButton(if(s.placeId==place.id)"선택됨" else "선택",filled=s.placeId==place.id){send(JourneyEvent.Place(place.id))}
            }}
        }
        3->{
            Heading("오늘의 준비 계획",s.place.name)
            Text("현재 배터리 ${s.soc?.let{"$it%"} ?: "확인 필요"} · 저장된 잔량 · 출발 전 ${s.departureCheckedCount}/${s.departureProgress.total}개 확인")
            s.departureBlockReason?.let { Text(it) }
            if(s.input.cookingNeedsReview) CookingPlan.entries.forEach { plan->
                PleosButton(plan.label,filled=s.input.cookingPlan==plan){send(JourneyEvent.Conditions(s.input.copy(cookingPlan=plan,cookingNeedsReview=false)))}
            }
            if(s.input.schedule?.departureEnd(s.input.endHour)==null) PleosButton("오늘 날짜로 계획") {
                val zone=java.time.ZoneId.systemDefault()
                send(JourneyEvent.Conditions(s.input.copy(schedule=com.chapark.app.domain.TripSchedule(java.time.LocalDate.now(zone).toString(),zone.id))))
            }
            Group.entries.filter{it!=Group.SITE}.forEach {group->
                val items=s.decisions.filter{it.group==group}
                if(items.isNotEmpty()) Panel {
                    Text(group.label,style=MaterialTheme.typography.titleMedium)
                    items.forEach{item->Check(item.title,item.id in s.checked){send(JourneyEvent.Check(item.id))};Text(item.reason,color=LocalPleos.current.secondary)}
                }
            }
        }
        4->{
            Heading("${s.place.name}까지","목적지에 도착하면 알려주세요.")
            Panel {
                Text(when(s.route){RoutePhase.NONE->"이동 준비";RoutePhase.REQUESTED->"이동 정보를 준비하고 있어요";RoutePhase.GUIDING->"목적지로 이동 중";RoutePhase.FAILED->"이동 정보를 불러오지 못했어요";RoutePhase.ARRIVED->if(s.parked)"도착 · 주차 완료" else "목적지 도착 · 주차 확인 필요"},style=MaterialTheme.typography.titleLarge)
                Text("차량 내비에서 경로를 확인하고, 현장 운영 안내를 따라주세요.")
                if(s.decisions.any{it.id=="charge-review"})Text("배터리가 부족할 수 있어요. 출발 전 충전 계획을 확인해 주세요.")
                if(s.route==RoutePhase.ARRIVED && !s.parked)Text("안전한 곳에 주차한 뒤 주차 완료를 확인해 주세요.")
            }
        }
        5->{
            Panel {s.siteItems.forEach{item->Check(item.title,item.id in s.checked){send(JourneyEvent.Check(item.id))}}}
            Heading("잠들기 전,\n하나씩 확인해요","편안히 쉴 수 있도록 주변과 차량 설정을 확인해 주세요.")
            Panel {Text("차량의 공조와 조명을 원하는 상태로 맞춘 뒤 체크해 주세요.");JourneyChecklists.sleep(s.checklistVersion).forEach{(id,title)->Check(title,id in s.sleepChecks){send(JourneyEvent.SleepCheck(id))}}}
            Text("차내 화기를 사용하지 마세요. 차량 사용 설명서와 현장 운영 안내를 따라주세요.")
        }
        6->if(s.completed){
            Heading("오늘의 차박을 마쳤어요","내 차와 보낸 하룻밤의 기억을 저장했어요.")
            Panel {Text(s.place.name,style=MaterialTheme.typography.titleLarge);Text("수면 준비 ${s.sleepChecks.intersect(s.requiredSleepIds).size}/${s.requiredSleepIds.size} · 철수 확인 ${s.exitChecks.intersect(s.requiredExitIds).size}/${s.requiredExitIds.size}");Text("다음 여행도 차Park와 함께 준비해 보세요.")}
        }else{
            Heading("떠나기 전,\n한 번 더.","직접 바꾼 설정과 가져온 물건을 확인하고 마무리해요.")
            Panel {JourneyChecklists.exit(s.checklistVersion).forEach{(id,title)->Check(title,id in s.exitChecks){send(JourneyEvent.ExitCheck(id))}}}
        }
    }
}
private fun label(value:Condition)=when(value){Condition.ALLOWED->"허용";Condition.RESTRICTED->"제한";Condition.UNKNOWN->"확인 필요"}
@Composable private fun Heading(title:String,body:String){Text(title,style=MaterialTheme.typography.headlineLarge);Text(body,color=LocalPleos.current.secondary)}
@Composable private fun Choice(title:String,checked:Boolean,change:(Boolean)->Unit){Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f));PleosSwitch(checked,change)}}
@Composable private fun Check(title:String,checked:Boolean,change:()->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){PleosCheck(checked,change,Modifier.testTag("check-$title").semantics { contentDescription=title });Text(title,Modifier.weight(1f))}}
@Composable private fun Panel(content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxWidth().background(LocalPleos.current.surface,RoundedCornerShape(24.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)}
