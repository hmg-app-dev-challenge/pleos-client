package com.chapark.ivi

import android.os.Bundle
import android.car.Car
import android.car.hardware.property.CarPropertyManager
import androidx.test.platform.app.InstrumentationRegistry
import ai.pleos.playground.vehicle.Vehicle
import org.junit.Test
import org.junit.Assert.assertTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Read-only public SDK probe. No direct VHAL implementation or permission elevation. */
class EvPropertyProbeTest {
    @Suppress("DEPRECATION")
    @Test fun reportEnergyPermissionsAndSdkBatteryLevel() {
        val i=InstrumentationRegistry.getInstrumentation()
        val context=i.targetContext
        fun report(s:String)=i.sendStatus(0,Bundle().apply{putString("stream","EV_PROPERTY: $s\n")})
        listOf("pleos.car.permission.CAR_ENERGY","android.car.permission.CAR_ENERGY").forEach {
            report("permission=$it result=${context.checkSelfPermission(it)} (0=granted,-1=denied)")
        }
        report("android.car.Car load="+runCatching{Class.forName("android.car.Car").name}.fold({it},{it.javaClass.simpleName}))
        // Probe only with the app's current grants; never elevate or alter vehicle properties.
        val car=Car.createCar(context)
        try {
            val manager=car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            listOf(0x11600309,0x1160030c).forEach { id->
                report("AAOS config id=0x${id.toString(16)} "+runCatching{manager.getCarPropertyConfig(id)?.toString() ?: "null (not proof of unsupported)"}.fold({it},{"${it.javaClass.name}: ${it.message}"}))
                report("AAOS read id=0x${id.toString(16)} "+runCatching{manager.getProperty<Float>(id,0).toString()}.fold({it},{"${it.javaClass.name}: ${it.message}"}))
            }
        }catch(e:Exception){report("AAOS connection=${e.javaClass.name}: ${e.message}")}
        finally {car.disconnect()}
        lateinit var vehicle:Vehicle
        val done=CountDownLatch(1)
        i.runOnMainSync {
            vehicle=Vehicle(context)
            try {
                vehicle.initialize()
                vehicle.getEvBattery().getEvBatteryLevel({ value->
                    report("SDK getEvBatteryLevel value=$value type=Float; raw value, unit interpretation requires contract")
                    done.countDown()
                },{e->report("SDK getEvBatteryLevel error=${e.javaClass.name}: ${e.message}");done.countDown()})
            }catch(e:Exception){report("SDK exception=${e.javaClass.name}: ${e.message}");done.countDown()}
        }
        try {assertTrue("Probe must terminate; an error is not a successful property read",done.await(15,TimeUnit.SECONDS))}
        finally {i.runOnMainSync{vehicle.release()}}
    }
}
