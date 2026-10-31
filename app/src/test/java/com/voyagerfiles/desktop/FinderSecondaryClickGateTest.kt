package com.voyagerfiles.desktop

import org.junit.Assert.*
import org.junit.Test

class FinderSecondaryClickGateTest {
    @Test fun aSecondaryPressCannotDispatchThePrimarySelectionCallback() {
        val gate = FinderSecondaryClickGate()
        assertTrue(gate.allowsPrimaryClick())
        gate.recordPress(true)
        assertFalse(gate.allowsPrimaryClick())
        gate.recordPress(false)
        assertTrue(gate.allowsPrimaryClick())
    }

    @Test fun openingMenuOnSelectedFilePreservesTheWholeBatch() {
        val selected = linkedSetOf("a", "b", "c")
        assertSame(selected, finderContextSelection("b", selected))
        assertEquals(selected, finderContextSelection("b", selected))
        assertEquals(setOf("z"), finderContextSelection("z", selected))
    }

    @Test fun secondaryOrMixedMouseButtonsCannotClearEmptyCanvasSelection() {
        assertTrue(finderPrimarySelectionPress(true, false))
        assertFalse(finderPrimarySelectionPress(false, true))
        assertFalse(finderPrimarySelectionPress(true, true))
    }

    @Test fun emptySelectionContextSelectsOneFile() {
        assertEquals(setOf("a"), finderContextSelection("a", emptySet()))
    }
}
