package com.voyagerfiles.desktop

/**
 * Geometry-only desktop shell tokens. No file, navigation, or transfer behavior belongs here.
 *
 * Rounded content is drawn inside the app window; DeX owns the actual outer window decoration.
 * The shadow is therefore best-effort and can be clipped by the system window boundary.
 */
internal object FinderWindowGeometry {
    const val CORNER_RADIUS_DP = 28f
    const val COMPACT_CORNER_RADIUS_DP = 24f
    const val BORDER_WIDTH_DP = 1f
    const val SHADOW_ELEVATION_DP = 14f

    const val SIDEBAR_DEFAULT_WIDTH_DP = 250f
    const val SIDEBAR_MIN_WIDTH_DP = 190f
    const val SIDEBAR_MAX_WIDTH_DP = 420f
    const val SIDEBAR_RAIL_WIDTH_DP = 64f
    const val SIDEBAR_COLLAPSE_BELOW_DP = 700f
    const val CONTENT_MIN_WIDTH_DP = 340f
    const val NARROW_CONTENT_MIN_WIDTH_DP = 180f
    const val DIVIDER_HIT_WIDTH_DP = 12f

    const val TOOLBAR_HEIGHT_DP = 54f
    const val TOOLBAR_GAP_DP = 12f
    const val CAPTION_CLEARANCE_DP = 10f
    const val STATUS_BAR_HEIGHT_DP = 38f
    const val STATUS_HORIZONTAL_PADDING_DP = 14f

    const val COMPACT_TOOLBAR_BELOW_DP = 1150f
    const val MINIMAL_TOOLBAR_CONTENT_DP = 330f
    const val PREVIEW_WIDTH_DP = 230f
    const val PREVIEW_MIN_WINDOW_WIDTH_DP = 1180f

    fun resolve(
        widthDp: Float,
        heightDp: Float,
        preferredSidebarDp: Float,
        nativeTrailingDp: Float = 0f,
    ): FinderShellLayout {
        val width = widthDp.coerceAtLeast(0f)
        val height = heightDp.coerceAtLeast(0f)
        val rail = width < SIDEBAR_COLLAPSE_BELOW_DP ||
            width < SIDEBAR_MIN_WIDTH_DP + DIVIDER_HIT_WIDTH_DP + CONTENT_MIN_WIDTH_DP
        val sidebar = if (rail) {
            SIDEBAR_RAIL_WIDTH_DP.coerceAtMost((width - NARROW_CONTENT_MIN_WIDTH_DP).coerceAtLeast(0f))
        } else {
            preferredSidebarDp.coerceIn(SIDEBAR_MIN_WIDTH_DP, SIDEBAR_MAX_WIDTH_DP)
                .coerceAtMost((width - DIVIDER_HIT_WIDTH_DP - CONTENT_MIN_WIDTH_DP).coerceAtLeast(0f))
        }
        val usableToolbar = (width - sidebar - DIVIDER_HIT_WIDTH_DP -
            nativeTrailingDp.coerceAtLeast(0f)).coerceAtLeast(0f)
        val minimal = usableToolbar < MINIMAL_TOOLBAR_CONTENT_DP
        val compact = minimal || width < COMPACT_TOOLBAR_BELOW_DP || usableToolbar < 650f
        return FinderShellLayout(
            sidebarWidthDp = sidebar,
            sidebarRail = rail,
            compactToolbar = compact,
            minimalToolbar = minimal,
            showPreview = !rail && width >= PREVIEW_MIN_WINDOW_WIDTH_DP &&
                (width - sidebar - DIVIDER_HIT_WIDTH_DP - PREVIEW_WIDTH_DP) >= 570f,
            cornerRadiusDp = if (width < SIDEBAR_COLLAPSE_BELOW_DP) COMPACT_CORNER_RADIUS_DP
                else CORNER_RADIUS_DP,
            statusBarHeightDp = if (height < 260f) 26f else STATUS_BAR_HEIGHT_DP,
        )
    }
}

internal data class FinderShellLayout(
    val sidebarWidthDp: Float,
    val sidebarRail: Boolean,
    val compactToolbar: Boolean,
    val minimalToolbar: Boolean,
    val showPreview: Boolean,
    val cornerRadiusDp: Float,
    val statusBarHeightDp: Float,
)
