package com.voyagerfiles.ui.theme
import org.junit.Assert.*
import org.junit.Test
class FinderMotionTest {
 @Test fun systemDisableWinsOverFull() { assertEquals(0, FinderMotion.resolve(FinderMotionMode.FULL, false).duration(200)); assertFalse(FinderMotion.resolve(FinderMotionMode.FULL, false).spatial) }
 @Test fun reducedKeepsShortFadesWithoutMovement() { val m = FinderMotion.resolve(FinderMotionMode.REDUCED, true); assertEquals(80, m.duration(200)); assertFalse(m.spatial) }
 @Test fun offSnapsAndFullKeepsDuration() { assertEquals(0, FinderMotion.resolve(FinderMotionMode.OFF, true).duration(200)); assertEquals(200, FinderMotion.resolve(FinderMotionMode.FULL, true).duration(200)); assertTrue(FinderMotion.resolve(FinderMotionMode.FULL, true).spatial) }
}
