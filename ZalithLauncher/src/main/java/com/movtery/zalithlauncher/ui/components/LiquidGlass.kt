/*
 * Eon Launcher (modified fork of Zalith Launcher 2)
 * This file is part of a program licensed under the GNU General Public License v3.0 or later.
 */

package com.movtery.zalithlauncher.ui.components

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.setting.LauncherSettings
import com.movtery.zalithlauncher.setting.enums.GlassQuality
import com.movtery.zalithlauncher.setting.enums.UiStyle
import com.movtery.zalithlauncher.setting.enums.isLauncherInDarkTheme
import com.movtery.zalithlauncher.viewmodel.LocalBackgroundViewModel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/*
 * Liquid Glass.
 *
 * Cost model (why the two quality levels exist):
 *  - OPTIMIZED: per card, one cached vertical-gradient fill and one 1dp gradient rim. Plain draw ops only:
 *    no blur, no offscreen layer, no shadow, no animation, and nothing is re-recorded unless size/theme/settings change.
 *    The backdrop is three static radial gradients, drawn once.
 *  - FANCY: adds a faint top highlight per card, a fourth backdrop blob and a very slow backdrop drift,
 *    stepped at 10 Hz (not every frame) and disabled when the system's animator scale is 0 or Battery Saver is on.
 *
 * With a custom wallpaper the card opacity follows the user's existing "background opacity" setting and
 * the launcher's existing background blur keeps working underneath the glass fill.
 *
 * Text contrast of the default (no wallpaper) look was checked against the Eon palettes (>= 4.5:1).
 */

/**
 * Resolved glass configuration for the current composition.
 */
@Immutable
data class GlassSpec(
    val enabled: Boolean,
    val fancy: Boolean,
    val dark: Boolean,
    val topAlpha: Float,
    val bottomAlpha: Float,
    val sheenAlpha: Float,
    val rowOverlayAlpha: Float
)

@Composable
fun rememberGlassSpec(): GlassSpec {
    val enabled = LauncherSettings.launcherUiStyle.state == UiStyle.LIQUID_GLASS
    val fancy = LauncherSettings.liquidGlassQuality.state == GlassQuality.FANCY
    val dark = isLauncherInDarkTheme()
    val customBackground = LocalBackgroundViewModel.current?.isValid == true
    val userOpacity = LauncherSettings.launcherBackgroundOpacity.state / 100f

    return remember(enabled, fancy, dark, customBackground, userOpacity) {
        val (top, bottom) = when {
            customBackground -> userOpacity to (userOpacity * 0.75f)
            dark && fancy -> 0.72f to 0.62f
            dark -> 0.68f to 0.58f
            fancy -> 0.66f to 0.44f
            else -> 0.74f to 0.52f
        }
        GlassSpec(
            enabled = enabled,
            fancy = fancy,
            dark = dark,
            topAlpha = top,
            bottomAlpha = bottom,
            sheenAlpha = if (!fancy) 0f else if (dark) 0.03f else 0.32f,
            rowOverlayAlpha = if (dark) 0.04f else 0.05f
        )
    }
}

/**
 * Glass fill (drawn under the content) plus a bright-to-faint gradient rim (drawn over it).
 * Returns the receiver unchanged when [GlassSpec.enabled] is false.
 * @param base tint of the glass; its own alpha is ignored
 * @param shape must match the shape of the surrounding card so the rim follows its corners
 */
@Composable
fun Modifier.liquidGlass(
    base: Color,
    shape: Shape,
    spec: GlassSpec = rememberGlassSpec()
): Modifier {
    if (!spec.enabled) return this
    val rim = remember(spec.dark, spec.fancy) { glassRimBrush(spec.dark, spec.fancy) }
    return this
        .drawWithCache {
            val fill = Brush.verticalGradient(
                colors = listOf(
                    base.copy(alpha = spec.topAlpha),
                    base.copy(alpha = spec.bottomAlpha)
                ),
                startY = 0f,
                endY = size.height
            )
            val sheen = if (spec.sheenAlpha > 0f) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = spec.sheenAlpha),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.5f
                )
            } else null
            onDrawBehind {
                drawRect(brush = fill)
                if (sheen != null) {
                    drawRect(brush = sheen, size = Size(size.width, size.height * 0.5f))
                }
            }
        }
        .border(
            width = if (spec.fancy) 1.25.dp else 1.dp,
            brush = rim,
            shape = shape
        )
}

private fun glassRimBrush(dark: Boolean, fancy: Boolean): Brush {
    return if (dark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (fancy) 0.46f else 0.38f),
                Color.White.copy(alpha = 0.07f),
                Color.White.copy(alpha = 0.03f),
                Color.White.copy(alpha = if (fancy) 0.24f else 0.18f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFF9DB2D6).copy(alpha = 0.40f),
                Color.White.copy(alpha = 0.60f)
            )
        )
    }
}

/**
 * Soft colored backdrop that the glass sits on. Only used when no custom background is set.
 * Colors come from the active theme, so it follows light/dark mode and the chosen palette.
 */
@Composable
fun LiquidBackdrop(modifier: Modifier = Modifier) {
    val spec = rememberGlassSpec()
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    val animate = spec.fancy && remember(context) { canAnimateBackdrop(context) }
    val phase = remember { mutableFloatStateOf(0f) }
    if (animate) {
        LaunchedEffect(Unit) {
            //10 Hz steps: a full drift cycle is ~40 s, so each step moves the blobs by only a pixel or two
            while (true) {
                delay(100L)
                phase.floatValue = (phase.floatValue + 0.0025f) % 1f
            }
        }
    }

    val dark = spec.dark
    val fancy = spec.fancy
    val c1 = if (dark) scheme.primaryContainer else scheme.primary
    val c2 = if (dark) scheme.tertiaryContainer else scheme.tertiary
    val c3 = if (dark) scheme.secondaryContainer else scheme.secondary
    val strength = if (dark) 0.50f else 0.24f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .drawBehind {
                //read inside the draw phase: drift only redraws this layer, it never recomposes anything
                val a = phase.floatValue * 2f * PI.toFloat()
                glassBlob(c1, 0.12f + 0.06f * sin(a), 0.08f + 0.05f * cos(a), 0.75f, strength)
                glassBlob(c2, 0.92f - 0.06f * cos(a), 0.30f + 0.06f * sin(a), 0.65f, strength * 0.9f)
                glassBlob(c3, 0.35f + 0.07f * sin(a + 2f), 1.02f - 0.05f * cos(a), 0.70f, strength * 0.8f)
                if (fancy) {
                    glassBlob(c1, 0.75f + 0.05f * cos(a + 1f), 0.85f, 0.45f, strength * 0.7f)
                }
            }
    )
}

private fun DrawScope.glassBlob(
    color: Color,
    centerX: Float,
    centerY: Float,
    radius: Float,
    alpha: Float
) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = Offset(size.width * centerX, size.height * centerY),
            radius = max(size.width, size.height) * radius
        )
    )
}

/**
 * Drift is skipped when the user turned system animations off or Battery Saver is active.
 */
private fun canAnimateBackdrop(context: Context): Boolean {
    val animatorScale = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    val powerSave = runCatching {
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
    }.getOrDefault(false)
    return animatorScale > 0f && !powerSave
}
