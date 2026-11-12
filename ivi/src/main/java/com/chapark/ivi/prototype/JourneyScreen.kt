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
import com.chapark.app.data.DemoPlaces
import com.chapark.app.domain.*
import com.chapark.ivi.design.*
import com.chapark.prototype.*

private val steps=listOf("시작","조건 확인","장소 비교","준비 계획","이동","수면 준비","철수")
private val names=mapOf("demo-a" to "숲가 쉼터", "demo-b" to "강변 쉼터", "demo-c" to "고요한 언덕")
private val sleepItems=linkedMapOf("parking" to "주차 위치와 현장 숙박 조건 확인", "ventilation" to "환기 방법과 비상 이동 동선 확인", "climate" to "차량 기본 설정에서 공조 직접 확인", "lights" to "차량 기본 설정에서 조명 직접 확인")
private val exitItems=linkedMapOf("settings" to "직접 변경한 차량 설정 확인", "gear" to "침구와 개인 장비 모두 챙기기", "waste" to "쓰레기 수거와 주변 정리", "surroundings" to "출발 전 차량 주변 확인")
private fun condition(value:Condition)=when(value){Condition.ALLOWED->"허용";Condition.RESTRICTED->"제한";Condition.UNKNOWN->"미확인"}

@Composable fun JourneyScreen(ui:JourneyUi,send:(JourneyEvent)->Unit) {
    val s=ui.journey
    var settings by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }
    BackHandler(s.step>0 && !s.completed && !s.restricted && !settings && confirm==null) { send(JourneyEvent.Back) }
    IviTheme(s.dark) {
        val p=LocalIviColors.current
        Surface(Modifier.fillMaxSize(),color=p.background) {
            if(!ui.loaded) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Text("저장된 여행을 불러오고 있어요",style=IviType.title) }
            else Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal=40.dp,vertical=24.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                    Text("차Park",style=IviType.title,modifier=Modifier.weight(1f))
                    IviButton("설정",size=IviButtonSize.MEDIUM,onClick={settings=true})
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                    Text(if(ui.saving)"저장 중" else if(ui.error!=null)"저장 오류" else "기기에 저장됨",style=IviType.label,color=p.tertiary)
                }
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
                        IviButton("정차했어요",filled=true) { send(JourneyEvent.Driving(false)) }
                    }
                } else {
                    JourneyProgress(s)
                    Spacer(Modifier.height(24.dp))
                    BoxWithConstraints(Modifier.weight(1f)) {
                        val wide=maxWidth>=1300.dp
                        Row(horizontalArrangement=Arrangement.spacedBy(32.dp)) {
                            key(s.step,s.completed) {
                                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).testTag("journey-content"),verticalArrangement=Arrangement.spacedBy(24.dp)) {
                                    ui.notice?.let{Text(it,style=IviType.label,color=p.tertiary)}
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
                    JourneyFooter(s,ui.saving) {
                            when {
                                s.completed -> send(JourneyEvent.NewTrip)
                                s.step==3 && s.checkedCount<s.decisions.size -> confirm="depart"
                                s.step==6 -> confirm="finish"
                                s.step==4 -> send(when(s.route) {
                                    RoutePhase.NONE,RoutePhase.FAILED->JourneyEvent.RequestRoute
                                    RoutePhase.REQUESTED->return@JourneyFooter
                                    RoutePhase.GUIDING->JourneyEvent.Arrive
                                    RoutePhase.ARRIVED->if(s.parked)JourneyEvent.Next else JourneyEvent.Park
                                })
                                else -> send(JourneyEvent.Next)
                            }
                    }
                }
            }
        }
        if(settings) JourneySettings(s,send,onClose={settings=false},onReset={settings=false;confirm="reset"})
        when(confirm) {
            "depart" -> IviConfirm("미확인 항목이 남아 있어요","${s.decisions.size-s.checkedCount}개 항목은 아직 확인하지 않았습니다. 출발 전 필요한 준비를 확인해 주세요.","이동 계획 보기",{confirm=null;send(JourneyEvent.Next)},{confirm=null})
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
    s.step==3 -> "준비 계획 확인 · 이동" to true
    s.step==4 -> when(s.route) {
        RoutePhase.NONE -> "이동 시작" to true
        RoutePhase.REQUESTED -> "이동 정보 준비 중" to false
        RoutePhase.GUIDING -> "도착했어요" to true
        RoutePhase.FAILED -> "다시 시도" to true
        RoutePhase.ARRIVED -> (if(s.parked)"수면 준비로" else "주차했어요") to true
    }
    s.step==5 -> "수면 준비 확인 · 다음 날" to (s.canSleep && s.sleepChecks.containsAll(Journey.SLEEP_IDS))
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
            Heading("오늘 밤,\n내 차로 떠나는 쉼.","금요일 퇴근 후, 어디서 어떻게 쉴지 함께 준비해요.")
            Panel {
                Text("오늘의 여행",style=IviType.title)
                Text("회사 주차장 · 1박 차박 · 전기차",style=IviType.body)
                Text("배터리 ${s.soc?.let{"$it%"} ?: "미확인"}",style=IviType.smallTitle)
                Text("${s.input.people}인 · ${if(s.input.cooking)"간단 조리 예정" else "준비한 식사"} · ${if(s.input.sleepingBag)"침낭 보유" else "침낭 미보유"}",style=IviType.body)
            }
            Text("출발 전 충전 계획과 현장 운영 시간을 확인해 주세요.",style=IviType.label,color=p.tertiary)
        }
        1 -> {
            Heading("누구와, 어떻게\n보낼 예정인가요?","저장된 기본 조건을 이번 여행에 맞게 바꿔주세요.")
            Panel {
                Text("함께하는 인원",style=IviType.smallTitle)
                IviSegments(listOf("1명","2명"),s.input.people-1) {send(JourneyEvent.Conditions(s.input.copy(people=it+1)))}
                IviToggleRow("현장에서 간단히 조리할 예정",s.input.cooking) {send(JourneyEvent.Conditions(s.input.copy(cooking=it)))}
                IviToggleRow("반려동물과 함께",s.input.pets) {send(JourneyEvent.Conditions(s.input.copy(pets=it)))}
                IviToggleRow("침낭을 보유하고 있어요",s.input.sleepingBag) {send(JourneyEvent.Conditions(s.input.copy(sleepingBag=it)))}
            }
        }
        2 -> {
            Heading("오늘의 쉼터를 비교해요","숙박 조건과 필요한 준비를 비교하고, 오늘의 쉼터를 골라보세요.")
            BoxWithConstraints {
                if(maxWidth>=1100.dp) Row(horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                    DemoPlaces.all.forEachIndexed{index,place->PlaceCard(s,place,index,Modifier.weight(1f),send)}
                } else Column(verticalArrangement=Arrangement.spacedBy(24.dp)) {
                    DemoPlaces.all.forEachIndexed{index,place->PlaceCard(s,place,index,Modifier,send)}
                }
            }
        }
        3 -> {
            Heading("오늘의 준비 계획","${names[s.placeId]} · 조건을 바꾸면 필요한 행동도 달라져요.")
            Panel {
                Text("현재 배터리",style=IviType.smallTitle)
                Text(s.soc?.let { "$it%" } ?: "확인 필요",style=IviType.title)
                Text("${s.checkedCount} / ${s.decisions.size}개 직접 확인",style=IviType.label,color=p.tertiary)
            }
            Group.entries.forEach { group ->
                val items=s.decisions.filter{it.group==group}.sortedBy { when(it.id){"charge-review"->0;"meal-ready"->1;else->2} }
                if(items.isNotEmpty()) Panel {
                    Text(group.label,style=IviType.title)
                    items.forEach {item-> IviCheckRow(item.title,item.id in s.checked,item.reason){send(JourneyEvent.Check(item.id))} }
                }
            }
        }
        4 -> {
            Heading("${names[s.placeId]}까지","출발 준비를 마치고, 목적지에 도착하면 알려주세요.")
            Panel {
                Text(when(s.route){RoutePhase.NONE->"이동 준비";RoutePhase.REQUESTED->"이동 정보를 준비하고 있어요";RoutePhase.GUIDING->"목적지로 이동 중";RoutePhase.FAILED->"이동 정보를 불러오지 못했어요";RoutePhase.ARRIVED->if(s.parked)"도착 · 주차 완료" else "목적지 도착 · 주차 확인 필요"},style=IviType.title)
                Text(if(s.decisions.any{it.id=="charge-review"})"배터리가 부족할 수 있어요. 차량 내비에서 충전소와 경로를 확인해 주세요." else "차량 내비에서 경로를 확인하고, 현장 운영 안내를 따라주세요.",style=IviType.body)
                if(s.route==RoutePhase.FAILED) Text("잠시 후 다시 시도해 주세요. 선택한 목적지와 준비 계획은 저장되어 있어요.",style=IviType.smallBody)
                if(s.route==RoutePhase.ARRIVED && !s.parked) Text("안전한 곳에 주차한 뒤 주차 완료를 확인해 주세요.",style=IviType.smallBody)
            }
        }
        5 -> {
            Heading("잠들기 전, 하나씩 확인해요","편안히 쉴 수 있도록 주변과 차량 설정을 확인해 주세요.")
            Panel {
                Text("편안한 휴식 준비",style=IviType.smallTitle)
                Text("차량의 공조와 조명을 원하는 상태로 맞춘 뒤 체크해 주세요.",style=IviType.smallBody)
                sleepItems.forEach{(id,title)->IviCheckRow(title,id in s.sleepChecks){send(JourneyEvent.SleepCheck(id))}}
            }
            Text("차내 화기를 사용하지 마세요. 실제 사용은 차량 사용 설명서와 현장 운영 안내를 따르세요.",style=IviType.label,color=p.tertiary)
        }
        6 -> if(s.completed) {
            Heading("오늘의 차박을 마쳤어요","이번 여행의 준비와 확인 기록을 저장했어요.")
            Panel {
                Text(names[s.placeId] ?: "오늘의 쉼터",style=IviType.title)
                Text("수면 준비 ${s.sleepChecks.size}/4 · 철수 확인 ${s.exitChecks.size}/4",style=IviType.body)
                Text("직접 확인한 준비 항목 ${s.checkedCount}/${s.decisions.size}",style=IviType.body)
                Text("다음 여행도 차Park와 함께 준비해 보세요.",style=IviType.smallBody)
            }
        } else {
            Heading("떠나기 전, 한 번 더.","직접 바꾼 설정과 가져온 물건을 확인하고 마무리해요.")
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
        Text("거리 ${listOf(42,35,58)[index]}km\n예상 기온 ${place.demoTemperatureC}°C",style=IviType.label,color=p.tertiary)
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
        Text(s.soc?.let{"$it%"} ?: "미확인",style=IviType.headline)
        Text("배터리",style=IviType.label,color=p.tertiary)
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
