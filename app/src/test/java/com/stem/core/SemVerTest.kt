package com.stem.core

import com.stem.core.util.AppVersion
import org.junit.Assert.assertEquals
import org.junit.Test



class StructuredVersionCodeTest {

    @Test
    fun testStructuredVersionCode() {
        assertEquals(1, AppVersion.major)
        assertEquals(0, AppVersion.minor)
        assertEquals(0, AppVersion.patch)
        assertEquals(1, AppVersion.buildCounter)
        assertEquals(1000001, AppVersion.versionCode)
        assertEquals("Build 1000001", AppVersion.displayString)
    }
}

