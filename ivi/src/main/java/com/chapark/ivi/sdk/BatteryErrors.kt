package com.chapark.ivi.sdk

import com.chapark.app.domain.ReadingStatus

/** Vehicle 2.3.2 maps numeric service errors to exact ErrorCode names in Exception.message.
 * Verified from the pinned AAR; do not substring-match arbitrary exception descriptions.
 */
internal object BatteryErrors {
    fun status(error:Exception):ReadingStatus = when {
        error is SecurityException -> ReadingStatus.PERMISSION_DENIED
        else -> when(error.message) {
            "ERROR_SECURITY_PERMISSION" -> ReadingStatus.PERMISSION_DENIED
            "ERROR_NOT_SUPPORTED_PROPERTY" -> ReadingStatus.UNSUPPORTED
            "ERROR_NOT_SUPPORTED_VALUE_TYPE", "ERROR_NOT_SUPPORTED_AREA", "ERROR_EMPTY_RESULT" -> ReadingStatus.INVALID
            "ERROR_DRIVING_RESTRICTED" -> ReadingStatus.DRIVING_RESTRICTED
            "ERROR_SERVICE_CONNECTION_FAILURE" -> ReadingStatus.DISCONNECTED
            else -> ReadingStatus.DISCONNECTED
        }
    }
}
