package com.stem.core

import com.stem.core.util.AppVersion
import org.junit.Assert.assertEquals
import org.junit.Test



class StructuredVersionCodeTest {

    @Test
    fun testStructuredVersionCode() {
        val expectedCode = (AppVersion.major * 1_000_000) + 
                           (AppVersion.minor * 10_000) + 
                           (AppVersion.patch * 100) + 
                           (AppVersion.buildCounter % 100)
        assertEquals(expectedCode, AppVersion.versionCode)
        assertEquals("Build ${AppVersion.versionCode}", AppVersion.displayString)
    }
}

