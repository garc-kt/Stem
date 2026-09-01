package com.veggiebit.sprout.core.version

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerTest {

    @Test
    fun testSemVerValues() {
        assertEquals(1, AppVersion.major)
        assertEquals(6, AppVersion.minor)
        assertEquals(1, AppVersion.patch)
        assertTrue(AppVersion.versionName.startsWith("1.6.1"))
        assertTrue(AppVersion.displayString.startsWith("v1.6.1"))
    }
}
