package com.voyagerfiles.desktop
import org.junit.Assert.*
import org.junit.Test
class DesktopInteractionPolicyTest {
 @Test fun mouseEnablesDesktopInNarrowWindow() { assertTrue(DesktopInteractionPolicy.useDesktop(InterfaceMode.AUTOMATIC, true, false)) }
 @Test fun explicitPreferenceWins() { assertFalse(DesktopInteractionPolicy.useDesktop(InterfaceMode.MOBILE, true, true)); assertTrue(DesktopInteractionPolicy.useDesktop(InterfaceMode.DESKTOP, false, false)) }
 @Test fun keyboardAloneDoesNotForceDesktop() { assertFalse(DesktopInteractionPolicy.useDesktop(InterfaceMode.AUTOMATIC, false, false)) }
}
