package com.keralapsc.prep.data.content

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ContentPackInstallationConcurrencyTest {

    @Test
    fun installationMutex_serializesConcurrentInstallations() = runBlocking {
        val mutex = Mutex()
        val activeConcurrentInstallations = AtomicInteger(0)
        val maxObservedConcurrency = AtomicInteger(0)
        val completedInstallations = AtomicInteger(0)

        suspend fun simulateInstall(packId: String) {
            mutex.withLock {
                val current = activeConcurrentInstallations.incrementAndGet()
                maxObservedConcurrency.updateAndGet { max -> maxOf(max, current) }
                delay(50) // simulate disk I/O and DB transaction
                activeConcurrentInstallations.decrementAndGet()
                completedInstallations.incrementAndGet()
            }
        }

        val job1 = async { simulateInstall("pack_background") }
        val job2 = async { simulateInstall("pack_manual") }

        job1.await()
        job2.await()

        assertEquals(2, completedInstallations.get())
        assertEquals(0, activeConcurrentInstallations.get())
        // Max observed concurrency must be strictly 1 because Mutex serializes execution
        assertEquals(1, maxObservedConcurrency.get())
    }

    @Test
    fun idempotentInstallation_skipsIdenticalPackVersionAndChecksum() {
        var installCount = 0
        fun checkAndInstall(installedVersion: Int?, incomingVersion: Int, existingChecksum: String?, incomingChecksum: String): Boolean {
            if (installedVersion != null && incomingVersion == installedVersion) {
                if (existingChecksum.equals(incomingChecksum, ignoreCase = true)) {
                    // Idempotent no-op
                    return false
                }
            }
            installCount++
            return true
        }

        // First attempt installs
        val firstResult = checkAndInstall(null, 1, null, "HASH123")
        assertTrue(firstResult)
        assertEquals(1, installCount)

        // Duplicate worker execution with identical version and checksum skips
        val duplicateResult = checkAndInstall(1, 1, "HASH123", "HASH123")
        assertTrue(!duplicateResult)
        assertEquals(1, installCount)
    }
}
