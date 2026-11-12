package com.chapark.ivi.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.EvBattery
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import com.chapark.app.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatterySnapshot(val value:Int?=null,val status:ReadingStatus=ReadingStatus.UNKNOWN,val source:Source=Source.UNKNOWN,val receivedAt:Long?=null,val delivery:String="none")
/** Public SOC API only, verified against Virtual Console's 0..100 percent contract. */
class BatterySession(context:Context):AutoCloseable {
    private val context=context.applicationContext
    private val handler=Handler(Looper.getMainLooper())
    private var vehicle:Vehicle?=null
    private var battery:EvBattery?=null
    private var listener:EvBatteryStateOfChargeListener?=null
    private var generation=0
    private val mutable=MutableStateFlow(BatterySnapshot())
    val state=mutable.asStateFlow()
    fun start(){
        close();val token=generation
        val source=if(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"))Source.EMULATOR else Source.SDK
        fun post(block:()->Unit){handler.post{if(token==generation)block()}}
        fun receive(value:Int?,delivery:String)=post{
            mutable.value=if(value!=null && value in 0..100)BatterySnapshot(value,ReadingStatus.AVAILABLE,source,System.currentTimeMillis(),delivery)
                else BatterySnapshot(status=ReadingStatus.INVALID,source=source)
        }
        fun failed(e:Exception)=post{mutable.value=BatterySnapshot(status=BatteryErrors.status(e),source=source)}
        if(context.checkSelfPermission("pleos.car.permission.CAR_ENERGY")!=PackageManager.PERMISSION_GRANTED){
            mutable.value=BatterySnapshot(status=ReadingStatus.PERMISSION_DENIED,source=source);return
        }
        try {
            val client=Vehicle(context);vehicle=client;client.initialize()
            val b=client.getEvBattery();battery=b
            b.checkEvBatteryStateOfChargeCapabilities({cap->post{
                if(cap.accessType !in setOf(AccessType.READ,AccessType.READ_WRITE)){
                    mutable.value=BatterySnapshot(status=ReadingStatus.UNSUPPORTED,source=source)
                }else{
                    val l=object:EvBatteryStateOfChargeListener {
                        override fun onEvBatteryStateOfChargeUpdated(value:Int?)=receive(value,"subscription")
                        override fun onFailed(e:Exception)=failed(e)
                    }
                    listener=l
                    try{b.registerEvBatteryStateOfCharge(l)}catch(e:Exception){failed(e)}
                    fun poll(){
                        if(token!=generation)return
                        try{b.getEvBatteryStateOfCharge({receive(it,"read")},{failed(it)})}catch(e:Exception){failed(e)}
                        handler.postDelayed({poll()},10_000)
                    }
                    poll()
                }
            }},{failed(it)})
            fun watchdog(){
                if(token!=generation)return
                val value=mutable.value
                if(value.status==ReadingStatus.UNKNOWN || (value.receivedAt!=null && System.currentTimeMillis()-value.receivedAt>45_000))
                    mutable.value=BatterySnapshot(status=ReadingStatus.STALE,source=source)
                handler.postDelayed({watchdog()},10_000)
            }
            handler.postDelayed({watchdog()},10_000)
        }catch(e:Exception){failed(e)}
    }
    override fun close(){
        generation++;handler.removeCallbacksAndMessages(null)
        runCatching{listener?.let{battery?.unregisterEvBatteryStateOfCharge(it)}}
        runCatching{vehicle?.release()};listener=null;battery=null;vehicle=null
    }
}
