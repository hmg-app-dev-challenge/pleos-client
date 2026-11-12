package com.chapark.ivi

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.ivi.sdk.BatterySession
import com.chapark.app.domain.*
import org.junit.Test
import org.junit.Assert.*
class BatterySessionTest {
    @Test fun publicSocMatchesConsolePercent(){
        val i=InstrumentationRegistry.getInstrumentation()
        lateinit var session:BatterySession
        i.runOnMainSync{session=BatterySession(i.targetContext);session.start()}
        try{
            val until=System.currentTimeMillis()+8000
            while(System.currentTimeMillis()<until && session.state.value.status!=ReadingStatus.AVAILABLE)Thread.sleep(100)
            val value=session.state.value
            i.sendStatus(0,Bundle().apply{putString("stream","BATTERY: $value\n")})
            assertEquals(ReadingStatus.AVAILABLE,value.status)
            val expected=InstrumentationRegistry.getArguments().getString("expectedSoc")?.toInt()
            if(expected!=null)assertEquals(expected,value.value)
            assertEquals(Source.EMULATOR,value.source)
        }finally{i.runOnMainSync{session.close()}}
    }
}
