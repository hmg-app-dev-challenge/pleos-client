package com.chapark.ivi.sdk
import com.chapark.app.domain.ReadingStatus
import org.junit.Assert.*
import org.junit.Test
class BatteryErrorsTest {
    @Test fun exactSdkCodesStayDistinct(){
        assertEquals(ReadingStatus.PERMISSION_DENIED,BatteryErrors.status(SecurityException()))
        val codes=mapOf("ERROR_SECURITY_PERMISSION" to ReadingStatus.PERMISSION_DENIED,
            "ERROR_NOT_SUPPORTED_PROPERTY" to ReadingStatus.UNSUPPORTED,
            "ERROR_NOT_SUPPORTED_AREA" to ReadingStatus.INVALID,
            "ERROR_DRIVING_RESTRICTED" to ReadingStatus.DRIVING_RESTRICTED,
            "ERROR_SERVICE_CONNECTION_FAILURE" to ReadingStatus.DISCONNECTED)
        codes.forEach{(code,status)->assertEquals(status,BatteryErrors.status(Exception(code)))}
        assertEquals(ReadingStatus.DISCONNECTED,BatteryErrors.status(Exception("possible ERROR_NOT_SUPPORTED_PROPERTY")))
    }
}
