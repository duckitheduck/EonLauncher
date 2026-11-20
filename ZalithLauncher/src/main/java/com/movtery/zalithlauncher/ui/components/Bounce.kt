/*
 * Eon Launcher (modified fork of Zalith Launcher 2)
 * This file is part of a program licensed under the GNU General Public License v3.0 or later.
 */

package com.movtery.zalithlauncher.ui.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.movtery.zalithlauncher.setting.LauncherSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp

/**
 * Damped-oscillation easing used for page/item entrances when bouncy motion is on.
 * It overshoots the target by roughly 19%, dips slightly under it and settles. The endpoints are exact
 * (f(0) = 0, f(1) = 1), so it is a drop-in replacement for the existing tween easing and keeps its timing and stagger.
 */
val BounceEnterEasing = Easing { t ->
    val w = 3f * PI.toFloat()
    val g = 1f - exp(-5f * t) * cos(w * t)
    val gEnd = 1f - exp(-5f) * cos(w)
    g + (1f - gEnd) * t
}

/**
 * A bouncy spring when the "Bouncy animations" setting is on, otherwise [fallback].
 */
@Composable
fun <T> bouncyOr(fallback: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> {
    return if (LauncherSettings.launcherBouncyMotion.state) {
        spring<T>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    } else {
        fallback
    }
}

/**
 * Scales the element down while it is pressed and springs back with a bounce.
 * The scale is applied in the graphics layer, so it never triggers recomposition or relayout.
 */
@Composable
fun Modifier.bouncyPress(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.94f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = bouncyOr<Float>(tween(durationMillis = 120)),
        label = "BouncyPress"
    )
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
