package com.smsguard.ui

import com.smsguard.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionActivationFullScreenTest {

    @Test
    fun seniorActivationSteps_includeCoreInstructionsInOrder() {
        assertEquals(
            listOf(
                R.string.protection_activation_step_1,
                R.string.protection_activation_step_2,
                R.string.protection_activation_step_3,
                R.string.protection_activation_step_4,
            ),
            seniorActivationStepResIds(includeXiaomiNote = false),
        )
    }

    @Test
    fun seniorActivationSteps_includeXiaomiNoteWhenRequested() {
        val steps = seniorActivationStepResIds(includeXiaomiNote = true)

        assertTrue(steps.contains(R.string.protection_activation_step_xiaomi))
        assertEquals(R.string.protection_activation_step_xiaomi, steps.last())
    }
}
