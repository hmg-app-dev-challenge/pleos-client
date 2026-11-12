package com.chapark.ivi.sdk

import org.junit.Assert.*
import org.junit.Test

class NavigationTargetTest {
    @Test fun roadSnappingIsAcceptedButUnrelatedAndInvalidDestinationsAreRejected(){
        assertTrue(NavigationSession.matchesDestination(37.57,126.87,37.5703,126.8698))
        assertFalse(NavigationSession.matchesDestination(37.57,126.87,37.41,127.09))
        assertFalse(NavigationSession.matchesDestination(37.57,126.87,Double.NaN,126.87))
        assertFalse(NavigationSession.matchesDestination(126.87,37.57,126.87,37.57))
    }
}
