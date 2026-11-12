package com.chapark.ivi.sdk
import org.junit.Assert.*
import org.junit.Test
class SessionGateTest {
    @Test fun rejectsCallbacksAfterRelease() {
        val gate=SessionGate();val token=gate.open();assertTrue(gate.accepts(token));gate.close();assertFalse(gate.accepts(token))
    }
    @Test fun reconnectDoesNotAcceptPreviousSessionResults() {
        val gate=SessionGate();val old=gate.open();val current=gate.open();assertFalse(gate.accepts(old));assertTrue(gate.accepts(current))
    }
}
