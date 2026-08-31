package com.veggiebit.sprout.core.version

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerTest {

    @Test
    fun testSemVerValues() {
        assertEquals(1, AppVersion.major)
        assertEquals(0, AppVersion.minor)
        assertEquals(0, AppVersion.patch)
        assertTrue(AppVersion.versionName.startsWith("1.0.0"))
        assertTrue(AppVersion.displayString.startsWith("v1.0.0"))
    }
}
