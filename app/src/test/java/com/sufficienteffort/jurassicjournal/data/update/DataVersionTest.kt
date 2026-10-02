package com.sufficienteffort.jurassicjournal.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataVersionTest {

    @Test
    fun `parses well-formed tags`() {
        assertEquals(DataVersion(9, 27), DataVersion.parse("data-v9.27"))
        assertEquals(DataVersion(10, 0), DataVersion.parse("data-v10.00"))
    }

    @Test
    fun `rejects other tag shapes`() {
        assertNull(DataVersion.parse("v1.7.18"))
        assertNull(DataVersion.parse("test-local"))
        assertNull(DataVersion.parse(null))
        assertNull(DataVersion.parse("data-v9.27-rc1"))
    }

    @Test
    fun `isNewer only within the same schema`() {
        assertTrue(DataVersion.isNewer("data-v9.28", "data-v9.27"))
        assertFalse(DataVersion.isNewer("data-v9.27", "data-v9.27"))
        assertFalse(DataVersion.isNewer("data-v9.26", "data-v9.27"))
        assertFalse(DataVersion.isNewer("data-v10.00", "data-v9.27"))
        assertFalse(DataVersion.isNewer("garbage", "data-v9.27"))
    }

    @Test
    fun `stored version is stale when schema differs or tag is unparseable`() {
        assertFalse(DataVersion.isStale("data-v9.30", "data-v9.27"))
        assertTrue(DataVersion.isStale("data-v9.30", "data-v10.00"))
        assertTrue(DataVersion.isStale("test-local", "data-v9.27"))
        assertTrue(DataVersion.isStale(null, "data-v9.27"))
    }
}
