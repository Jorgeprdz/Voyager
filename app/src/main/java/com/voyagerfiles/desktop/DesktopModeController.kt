package com.voyagerfiles.desktop

enum class DesktopMode { COMPACT, ADAPTED, FULL }
object DesktopModeController {
    fun mode(widthDp: Float, heightDp: Float): DesktopMode = when {
        widthDp < 600 || heightDp < 360 -> DesktopMode.COMPACT
        widthDp < 840 -> DesktopMode.ADAPTED
        else -> DesktopMode.FULL
    }
}
