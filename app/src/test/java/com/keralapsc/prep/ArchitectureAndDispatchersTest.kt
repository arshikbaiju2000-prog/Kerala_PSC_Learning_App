package com.keralapsc.prep

import com.keralapsc.prep.core.dispatcher.DefaultCoroutineDispatchers
import com.keralapsc.prep.core.model.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureAndDispatchersTest {

    @Test
    fun coroutineDispatchers_areProperlyInitialized() {
        val dispatchers = DefaultCoroutineDispatchers()
        assertNotNull(dispatchers.main)
        assertNotNull(dispatchers.io)
        assertNotNull(dispatchers.default)
    }

    @Test
    fun appResult_handlesSuccessState() {
        val result: AppResult<String> = AppResult.Success("Kerala PSC")
        assertTrue(result is AppResult.Success)
        assertEquals("Kerala PSC", (result as AppResult.Success).data)
    }

    @Test
    fun appResult_handlesErrorState() {
        val exception = IllegalStateException("Test error")
        val result: AppResult<Nothing> = AppResult.Error(exception)
        assertTrue(result is AppResult.Error)
        assertEquals("Test error", (result as AppResult.Error).message)
    }
}
