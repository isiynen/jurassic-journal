package com.sufficienteffort.jurassicjournal.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatAbilityNameTest {
    @Test fun `small words lowercase except first`() {
        assertEquals("Flock of One", formatAbilityName("Flock Of One"))
        assertEquals("Cloak of Rage", formatAbilityName("Cloak Of Rage"))
        assertEquals("Alert Buff and Strike", formatAbilityName("Alert Buff And Strike"))
        assertEquals("Defend the Herd", formatAbilityName("Defend The Herd"))
        assertEquals("Head to Head", formatAbilityName("Head To Head"))
    }

    @Test fun `first word stays capitalised and in-on are kept`() {
        assertEquals("The Great Escape", formatAbilityName("The Great Escape"))
        assertEquals("Alert Swap In Stun", formatAbilityName("Alert Swap In Stun"))
        assertEquals("On Escape Rampage", formatAbilityName("On Escape Rampage"))
    }
}
