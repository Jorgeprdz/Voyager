package com.voyagerfiles.desktop

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.core.app.ApplicationProvider
import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.ui.theme.FinderMotion
import com.voyagerfiles.ui.theme.FinderMotionMode
import com.voyagerfiles.ui.theme.LocalFinderMotion
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class FinderQuickLookMotionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun spaceClosesWithoutAnimation() = closesWithSpace(FinderMotionMode.OFF)
    @Test fun spaceClosesAfterFullTransition() = closesWithSpace(FinderMotionMode.FULL)

    private fun closesWithSpace(mode: FinderMotionMode) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File.createTempFile("quick-look-", ".txt", context.cacheDir).apply { writeText("Preview") }
        val visible = mutableStateOf(true)
        try {
            compose.setContent {
                MaterialTheme {
                    CompositionLocalProvider(LocalFinderMotion provides FinderMotion(mode)) {
                        if (visible.value) FinderQuickLookDialog(
                            FileItem(source.name, source.path, false), onDismiss = { visible.value = false })
                    }
                }
            }
            compose.onNodeWithText(source.name).performKeyInput { pressKey(Key.Spacebar) }
            compose.waitForIdle()
            compose.runOnIdle { assertFalse(visible.value) }
        } finally { source.delete() }
    }
}
