package com.voyagerfiles.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.voyagerfiles.ui.theme.FinderAppearance
import com.voyagerfiles.ui.theme.FinderBrightness
import com.voyagerfiles.ui.theme.FinderStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinderAppearanceSettingsTest {
    @Test fun styleBrightnessDynamicAndAccessibilitySurviveReopen() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = PreferencesManager(ctx)
        val original = manager.finderAppearance.first()
        try {
            for (style in FinderStyle.entries) {
                val changed = FinderAppearance(
                    style = style, brightness = FinderBrightness.DARK,
                    highContrast = true, reduceTransparency = true, dynamicColors = false,
                )
                manager.setFinderAppearance(changed)
                val reopened = PreferencesManager(ctx)
                assertEquals(changed, reopened.finderAppearance.first())
            }
        } finally {
            manager.setFinderAppearance(original)
        }
    }
}
