package com.imageloader.core.size

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SizeTest {

    @Test
    fun pixels_createsFixedSize() {
        val size = Size.pixels(100, 200)
        assertEquals(Dimension.Pixels(100), size.width)
        assertEquals(Dimension.Pixels(200), size.height)
    }

    @Test
    fun original_and_automatic_singletons() {
        assertEquals(Dimension.Original, Size.ORIGINAL.width)
        assertEquals(Dimension.Original, Size.ORIGINAL.height)
        assertEquals(Dimension.Undefined, Size.AUTOMATIC.width)
        assertEquals(Dimension.Undefined, Size.AUTOMATIC.height)
        assertNotEquals(Size.ORIGINAL, Size.AUTOMATIC)
    }

    @Test(expected = IllegalArgumentException::class)
    fun pixels_rejectsNonPositive() {
        Dimension.Pixels(0)
    }
}
