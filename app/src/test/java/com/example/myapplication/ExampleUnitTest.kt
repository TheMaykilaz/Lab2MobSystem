package com.example.myapplication

import com.example.myapplication.helpers.MockCloudControl
import com.example.myapplication.wash.HandWashEngine
import com.example.myapplication.wash.WashPhase
import com.example.myapplication.wash.WashThresholds
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun handWashEngine_remoteStartAndCountdown() {
        val engine = HandWashEngine()
        val t0 = 1000L

        // Дистанційний запуск
        engine.start(t0)
        var snap = engine.tick(t0)
        assertEquals(WashPhase.WASHING, snap.phase)
        assertEquals(0L, snap.washedMs)

        // Через 10 секунд (миття триває)
        snap = engine.tick(t0 + 10_000L)
        assertEquals(WashPhase.WASHING, snap.phase)
        assertEquals(10_000L, snap.washedMs)

        // Через 20 секунд (таймер завершено)
        snap = engine.tick(t0 + WashThresholds.TARGET_WASH_MS)
        assertEquals(WashPhase.DONE, snap.phase)
        assertEquals(WashThresholds.TARGET_WASH_MS, snap.washedMs)

        // Скидання
        engine.reset()
        snap = engine.tick(t0 + 30_000L)
        assertEquals(WashPhase.IDLE, snap.phase)
        assertEquals(0L, snap.washedMs)
    }

    @Test
    fun mockCloudControl_sendAndFetchWashCommand() = runBlocking {
        val before = System.currentTimeMillis()
        MockCloudControl.sendRemoteWashCommand()
        val commandId = MockCloudControl.fetchWashCommand()

        assertTrue("Command ID should be set after sending", commandId >= before)
    }
}