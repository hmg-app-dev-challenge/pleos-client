package com.chapark.ivi.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.*
import ai.pleos.playground.navi.constants.*
import com.chapark.app.data.NavigationPlace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

enum class NavigationPhase { IDLE, CONNECTING, SEARCHING, GUIDING, ARRIVED, FAILED }
data class NavigationSnapshot(val phase:NavigationPhase=NavigationPhase.IDLE,val message:String?=null,
    val destinationName:String?=null,val routeConfirmed:Boolean=false)

/** Public SDK only. Releasing this observer never cancels a driver's route. */
class NavigationSession(context:Context):AutoCloseable {
    private val context=context.applicationContext
    private val handler=Handler(Looper.getMainLooper())
    private var generation=0L
    private var client:NaviHelper?=null
    private var listener:NaviHelperEventListener?=null
    private var guidance=false
    private var destinationMatches=false
    private var ready=false
    private val mutable=MutableStateFlow(NavigationSnapshot())
    val state=mutable.asStateFlow()

    fun connect(place:NavigationPlace,requestRoute:Boolean) {
        close()
        val token=generation
        mutable.value=NavigationSnapshot(NavigationPhase.CONNECTING)
        fun fail(message:String){
            if(token!=generation)return
            generation++
            handler.removeCallbacksAndMessages(null)
            runCatching{listener?.let{client?.removeListener(it)}}
            runCatching{client?.release()};client=null;listener=null;ready=false
            mutable.value=NavigationSnapshot(NavigationPhase.FAILED,message)
        }
        fun post(action:()->Unit){handler.post{
            if(token==generation)try{action()}catch(e:Exception){fail("내비게이션 연결을 확인해 주세요.")}
        }}
        fun confirm(){
            if(guidance && destinationMatches && mutable.value.phase!=NavigationPhase.ARRIVED)
                mutable.value=NavigationSnapshot(NavigationPhase.GUIDING,destinationName=place.place.name,routeConfirmed=true)
        }
        if(!listOf("NAVI_ROUTE","NAVI_ROUTE_SEARCH","NAVI_CUSTOM_MAP").all{
                context.checkSelfPermission("pleos.car.permission.$it")==PackageManager.PERMISSION_GRANTED}){
            fail("내비게이션 권한을 확인해 주세요.");return
        }
        val n=NaviHelper(context);client=n
        val l=object:NaviHelperEventListener {
            override fun onRouteStarted(routeStartInfo:RouteStartInfo)=post{
                Log.i("ChaParkNavi","route started: ${routeStartInfo.name}")
                n.getDestinationInfo();n.getRouteStateInfo()
            }
            override fun onRoutingCompleted()=post{n.getDestinationInfo();n.getRouteStateInfo()}
            override fun onDestinationChanged()=post{n.getDestinationInfo();n.getRouteStateInfo()}
            override fun onNavigationStatus(navigationStatus:NavigationStatus)=post{
                Log.i("ChaParkNavi","navigation status: $navigationStatus")
                guidance=navigationStatus==NavigationStatus.ROUTE_DRIVE
                n.getDestinationInfo();confirm()
            }
            override fun onRouteStateInfo(routeStateInfo:RouteStateInfo)=post{
                guidance=routeStateInfo.naviMode in setOf(NaviMode.ROUTE_GUIDING,NaviMode.SIMULATION_GUIDING)
                if(!routeStateInfo.termsAgreement)fail("내비게이션 앱에서 이용 안내를 확인해 주세요.")
                else if(!guidance && mutable.value.phase==NavigationPhase.GUIDING)fail("길 안내가 종료됐어요. 목적지를 다시 확인해 주세요.")
                else confirm()
            }
            override fun onDestinationInfo(destinationInfo:DestinationInfo)=post{
                destinationMatches=matchesDestination(place.latitude,place.longitude,destinationInfo.latitude,destinationInfo.longitude)
                Log.i("ChaParkNavi","destination=${destinationInfo.name}, matches=$destinationMatches")
                if(!destinationMatches && mutable.value.phase==NavigationPhase.GUIDING)fail("내비게이션 목적지가 변경됐어요. 다시 확인해 주세요.") else confirm()
            }
            override fun onDestinationArrived(destinationArrivedInfo:DestinationArrivedInfo)=post{
                if(destinationMatches && mutable.value.phase==NavigationPhase.GUIDING)
                    mutable.value=NavigationSnapshot(NavigationPhase.ARRIVED,destinationName=place.place.name,routeConfirmed=true)
            }
            override fun onRouteCancelled()=post{if(destinationMatches && mutable.value.phase==NavigationPhase.GUIDING)fail("길 안내가 취소됐어요.")}
            override fun onError(errorCode:NaviErrorCode)=post{fail(when(errorCode){
                NaviErrorCode.CAUSE_LOW_BATTERY->"배터리가 부족해 경로를 찾지 못했어요. 충전 계획을 확인해 주세요."
                NaviErrorCode.CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE->"목적지가 너무 가까워요. 위치를 확인해 주세요."
                NaviErrorCode.CAUSE_SEARCH_ROUTE_OVER_DISTANCE->"경로를 찾을 수 있는 거리를 넘었어요. 다른 장소를 선택해 주세요."
                else->"경로를 찾지 못했어요. 연결 상태를 확인하고 다시 시도해 주세요."
            })}
        }
        listener=l
        handler.postDelayed({if(token==generation && mutable.value.phase !in setOf(NavigationPhase.GUIDING,NavigationPhase.ARRIVED))
            fail("내비게이션 응답이 늦어지고 있어요. 다시 시도해 주세요.")},30_000)
        try {n.initialize({post{
            ready=true;n.addListener(l)
            mutable.value=NavigationSnapshot(NavigationPhase.SEARCHING)
            try {
                if(requestRoute)n.requestRoute(RouteInfo(latitude=place.latitude,longitude=place.longitude,
                    displayLatitude=null,displayLongitude=null,poiName=place.place.name,poiId="",poiSubId="",address=place.address))
                fun poll(){
                    if(token!=generation || mutable.value.phase==NavigationPhase.ARRIVED)return
                    try {n.getDestinationInfo();n.getRouteStateInfo()} catch(e:Exception){fail("내비게이션 연결을 확인해 주세요.");return}
                    handler.postDelayed({poll()},2_000)
                }
                poll()
            } catch(e:Exception){fail("내비게이션 요청을 보내지 못했어요. 다시 시도해 주세요.")}
        }},{post{fail("내비게이션에 연결하지 못했어요. 앱을 열고 다시 시도해 주세요.")}})}
        catch(e:Exception){fail("내비게이션에 연결하지 못했어요.")}
    }
    fun openScreen(){
        if(ready)try {client?.requestNavigationScreen(NavigationScreenRequest.Foreground)}
        catch(e:Exception){mutable.value=mutable.value.copy(message="내비게이션 화면을 열지 못했어요. 다시 시도해 주세요.")}
    }
    override fun close(){
        generation++;handler.removeCallbacksAndMessages(null)
        runCatching{listener?.let{client?.removeListener(it)}};runCatching{client?.release()}
        client=null;listener=null;guidance=false;destinationMatches=false;ready=false
        mutable.value=NavigationSnapshot()
    }
    companion object {
        // Permit road snapping near an official map point, but reject another destination.
        fun matchesDestination(lat:Double,lon:Double,actualLat:Double,actualLon:Double)=
            lat.isFinite() && lon.isFinite() && actualLat.isFinite() && actualLon.isFinite() &&
                lat in -90.0..90.0 && lon in -180.0..180.0 && abs(lat-actualLat)<0.002 && abs(lon-actualLon)<0.002
    }
}
