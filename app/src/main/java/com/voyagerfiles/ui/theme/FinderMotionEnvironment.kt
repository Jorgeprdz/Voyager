package com.voyagerfiles.ui.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

val LocalFinderMotion = staticCompositionLocalOf { FinderMotion(FinderMotionMode.FULL) }

@Composable
fun rememberFinderMotion(mode: FinderMotionMode): FinderMotion {
    val resolver = LocalContext.current.contentResolver
    fun enabled() = runCatching {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }.getOrDefault(true)
    var systemEnabled by remember(resolver) { mutableStateOf(enabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { systemEnabled = enabled() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        systemEnabled = enabled()
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return FinderMotion.resolve(mode, systemEnabled)
}
