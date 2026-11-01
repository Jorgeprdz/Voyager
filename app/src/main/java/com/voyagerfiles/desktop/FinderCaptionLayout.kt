package com.voyagerfiles.desktop

/** Window-local horizontal bounds reported by Android's caption API. */
data class CaptionBounds(val left: Int, val right: Int)
data class FinderCaptionLayout(val leading: Int = 0, val trailing: Int = 0, val topInset: Int = 0, val integrated: Boolean = false)

fun finderCaptionLayout(width: Int, height: Int, bounds: List<CaptionBounds>): FinderCaptionLayout {
    if (height <= 0) return FinderCaptionLayout()
    val controls = bounds.filter { it.right > it.left && it.right - it.left < width / 2 }
    if (controls.isEmpty()) return FinderCaptionLayout(topInset = height)
    val leading = controls.filter { it.left < width / 3 }.maxOfOrNull { it.right } ?: 0
    val trailing = controls.filter { it.right > width * 2 / 3 }.maxOfOrNull { width - it.left } ?: 0
    return FinderCaptionLayout(leading.coerceAtLeast(0), trailing.coerceAtLeast(0), integrated = true)
}
