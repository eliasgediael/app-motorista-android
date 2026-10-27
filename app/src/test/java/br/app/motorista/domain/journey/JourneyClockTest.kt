package br.app.motorista.domain.journey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class JourneyClockTest {
    private val s0 = JourneyState()

    @Test
    fun pauseAndResumeNeverResetWorkedTime() {
        val running = JourneyClock.start(s0, 1_000)!!
        val paused = JourneyClock.pause(running, 11_000)!!
        assertEquals(10_000, JourneyClock.workedMs(paused, 50_000))
        assertEquals(40_000, JourneyClock.pausedMs(paused, 51_000))
        val resumed = JourneyClock.resume(paused, 61_000)!!
        assertEquals(10_000, JourneyClock.workedMs(resumed, 61_000))
        assertEquals(15_000, JourneyClock.workedMs(resumed, 66_000))
        assertEquals(50_000, JourneyClock.pausedMs(resumed, 70_000))
    }

    @Test
    fun duplicateOrOutOfOrderActionsAreRefused() {
        val running = JourneyClock.start(s0, 0)!!
        assertNull(JourneyClock.start(running, 5))
        assertNull(JourneyClock.resume(running, 5))
        val paused = JourneyClock.pause(running, 10)!!
        assertNull(JourneyClock.pause(paused, 20))
        assertNull(JourneyClock.end(s0, 5))
    }

    @Test
    fun endIsSeparateAndFreezesWorkedTime() {
        val running = JourneyClock.start(s0, 0)!!
        val ended = JourneyClock.end(running, 30_000)!!
        assertEquals(JourneyStatus.Ended, ended.status)
        assertEquals(30_000, JourneyClock.workedMs(ended, 99_000))
        assertNull(JourneyClock.end(ended, 100_000))
        assertNotNull(JourneyClock.start(ended, 100_000))
    }

    @Test
    fun endWhilePausedClosesPauseInterval() {
        val running = JourneyClock.start(s0, 0)!!
        val paused = JourneyClock.pause(running, 10_000)!!
        val ended = JourneyClock.end(paused, 25_000)!!
        assertEquals(10_000, JourneyClock.workedMs(ended, 25_000))
        assertEquals(15_000, JourneyClock.pausedMs(ended, 99_000))
    }

    @Test
    fun formatsDuration() {
        assertEquals("00:18", JourneyClock.format(18_000))
        assertEquals("1:02:03", JourneyClock.format(3_723_000))
    }
}
