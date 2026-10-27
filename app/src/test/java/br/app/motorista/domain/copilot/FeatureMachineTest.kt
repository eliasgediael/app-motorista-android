package br.app.motorista.domain.copilot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureMachineTest {
    @Test
    fun neverActiveWithoutConfirmation() {
        val m = FeatureMachine()
        assertTrue(m.beginStart())
        assertEquals(FeatureStatus.Starting, m.status)
        assertTrue(m.confirmActive())
        assertEquals(FeatureStatus.Active, m.status)
    }

    @Test
    fun confirmWithoutRequestIsIgnored() {
        val m = FeatureMachine()
        assertFalse(m.confirmActive())
        assertEquals(FeatureStatus.Off, m.status)
    }

    @Test
    fun doubleTapIsBlockedWhileStartingOrStopping() {
        val m = FeatureMachine()
        assertTrue(m.beginStart())
        assertFalse(m.beginStart())
        m.confirmActive()
        assertTrue(m.beginStop())
        assertEquals(FeatureStatus.Stopping, m.status)
        assertFalse(m.beginStop())
        assertFalse(m.beginStart())
        m.confirmOff()
        assertEquals(FeatureStatus.Off, m.status)
    }

    @Test
    fun failureNeverLeavesActive() {
        val m = FeatureMachine()
        m.beginStart()
        m.fail(FeatureStatus.Error, "câmera ocupada")
        assertEquals(FeatureStatus.Error, m.status)
        assertEquals("câmera ocupada", m.detail)
        assertFalse(m.confirmActive())
        assertTrue(m.beginStart())
    }

    @Test
    fun permissionDeniedAllowsRetryButIsNotOn() {
        val m = FeatureMachine()
        m.beginStart()
        m.fail(FeatureStatus.PermissionNeeded, "câmera não autorizada")
        assertEquals(FeatureStatus.PermissionNeeded, m.status)
        assertTrue(m.beginStart())
    }

    @Test
    fun cancelWhileStartingGoesBackToOff() {
        val m = FeatureMachine()
        m.beginStart("Aguardando confirmação")
        assertTrue(m.beginStop())
        assertEquals(FeatureStatus.Off, m.status)
    }

    @Test
    fun serviceLossForcesOff() {
        val m = FeatureMachine()
        m.beginStart()
        m.confirmActive()
        m.forceOff("serviço encerrado")
        assertEquals(FeatureStatus.Off, m.status)
    }

    @Test
    fun notImplementedCannotBeStartedOrForced() {
        val m = FeatureMachine.notImplemented("sem fonte")
        assertFalse(m.beginStart())
        m.forceOff()
        assertEquals(FeatureStatus.NotImplemented, m.status)
        assertFalse(m.confirmActive())
    }
}
