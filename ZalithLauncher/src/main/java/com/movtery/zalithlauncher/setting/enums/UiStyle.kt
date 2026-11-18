/*
 * Eon Launcher (modified fork of Zalith Launcher 2)
 * This file is part of a program licensed under the GNU General Public License v3.0 or later.
 */

package com.movtery.zalithlauncher.setting.enums

import com.movtery.zalithlauncher.R

/**
 * Overall interface style.
 */
enum class UiStyle(val textRes: Int) {
    /** The standard flat Material look. */
    NORMAL(R.string.ui_style_normal),

    /** Translucent, glossy "liquid glass" surfaces over a soft backdrop. */
    LIQUID_GLASS(R.string.ui_style_liquid_glass)
}

/**
 * Cost/quality trade-off of [UiStyle.LIQUID_GLASS].
 */
enum class GlassQuality(val textRes: Int) {
    /** Static translucency and rim only: no animation, no blur, no offscreen layers. */
    OPTIMIZED(R.string.glass_quality_optimized),

    /** Adds a top highlight and a slowly drifting backdrop. Costs more GPU and battery. */
    FANCY(R.string.glass_quality_fancy)
}
