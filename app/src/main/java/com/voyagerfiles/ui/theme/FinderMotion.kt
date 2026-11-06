package com.voyagerfiles.ui.theme

enum class FinderMotionMode { FULL, REDUCED, OFF }

data class FinderMotion(val mode: FinderMotionMode) {
    val spatial: Boolean get() = mode == FinderMotionMode.FULL
    fun duration(normal: Int): Int = when (mode) {
        FinderMotionMode.FULL -> normal
        FinderMotionMode.REDUCED -> minOf(normal, 80)
        FinderMotionMode.OFF -> 0
    }
    companion object {
        fun resolve(preference: FinderMotionMode, systemEnabled: Boolean) =
            FinderMotion(if (systemEnabled) preference else FinderMotionMode.OFF)
    }
}
