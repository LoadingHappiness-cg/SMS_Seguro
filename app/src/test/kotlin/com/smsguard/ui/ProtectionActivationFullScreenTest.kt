package com.smsguard.ui

import com.smsguard.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionActivationFullScreenTest {

    @Test
    fun activationPromptSteps_includeCoreInstructionsInOrder() {
        assertEquals(
            listOf(
                R.string.protection_activation_step_1,
                R.string.protection_activation_step_2,
                R.string.protection_activation_step_3,
            ),
            activationPromptStepResIds(includeXiaomiNote = false),
        )
    }

    @Test
    fun activationPromptSteps_includeXiaomiNoteWhenRequested() {
        val steps = activationPromptStepResIds(includeXiaomiNote = true)

        assertTrue(steps.contains(R.string.protection_activation_step_xiaomi))
        assertEquals(R.string.protection_activation_step_xiaomi, steps.last())
    }
}
