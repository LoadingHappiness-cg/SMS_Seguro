package com.smsguard.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityIntentTest {

    @Test
    fun openProtectionAction_selectsProtectionTab() {
        assertEquals(
            0,
            resolveSelectedTab(
                action = MainActivity.ACTION_OPEN_PROTECTION,
                tab = null,
            ),
        )
    }

    @Test
    fun protectionTabExtra_selectsProtectionTab() {
        assertEquals(
            0,
            resolveSelectedTab(
                action = null,
                tab = MainActivity.TAB_PROTECTION,
            ),
        )
    }

    @Test
    fun protecaoTabExtra_selectsProtectionTab() {
        assertEquals(
            0,
            resolveSelectedTab(
                action = null,
                tab = "protecao",
            ),
        )
    }

    @Test
    fun aboutTabExtra_selectsAboutTab() {
        assertEquals(
            2,
            resolveSelectedTab(
                action = null,
                tab = MainActivity.TAB_ABOUT,
            ),
        )
    }

    @Test
    fun unknownIntent_defaultsToProtectionTab() {
        assertEquals(0, resolveSelectedTab(action = null, tab = null))
        assertEquals(0, resolveSelectedTab(action = "other.action", tab = "history"))
    }
}
