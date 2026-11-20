/*
 * Eon Launcher (modified fork of Zalith Launcher 2)
 * This file is part of a program licensed under the GNU General Public License v3.0 or later.
 */

package com.movtery.zalithlauncher.ui.screens.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.bouncyOr
import com.movtery.zalithlauncher.ui.components.bouncyPress

/**
 * Eon navigation rail: Home, Versions, Downloads, Settings, About Eon.
 *
 * It is a [BackgroundCard], so it automatically follows the interface style (glass or normal card).
 * Navigation itself stays in MainScreen; this composable only draws the rail and reports taps.
 * Mod Library and Optimization Center will be added here when those screens exist.
 */
@Composable
fun EonNavigationRail(
    homeSelected: Boolean,
    versionsSelected: Boolean,
    downloadsSelected: Boolean,
    settingsSelected: Boolean,
    aboutSelected: Boolean,
    onHome: () -> Unit,
    onVersions: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackgroundCard(
        modifier = modifier
            .width(84.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RailItem(homeSelected, R.drawable.ic_home_filled, R.string.nav_home, onHome)
            RailItem(versionsSelected, R.drawable.ic_folder_zip_filled, R.string.nav_versions, onVersions)
            RailItem(downloadsSelected, R.drawable.ic_download_2_filled, R.string.nav_downloads, onDownloads)
            RailItem(settingsSelected, R.drawable.ic_settings_filled, R.string.nav_settings, onSettings)
            RailItem(aboutSelected, R.drawable.ic_info_filled, R.string.nav_about_eon, onAbout)
        }
    }
}

@Composable
private fun RailItem(
    selected: Boolean,
    iconRes: Int,
    labelRes: Int,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(labelRes)
    val interactionSource = remember { MutableInteractionSource() }

    //the selection pill springs in from narrow to full width; the icon pops slightly when selected
    val pillScale = animateFloatAsState(
        targetValue = if (selected) 1f else 0.3f,
        animationSpec = bouncyOr<Float>(tween(durationMillis = 180)),
        label = "RailPillScale"
    )
    val pillAlpha = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "RailPillAlpha"
    )
    val iconScale = animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = bouncyOr<Float>(tween(durationMillis = 180)),
        label = "RailIconScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyPress(interactionSource, pressedScale = 0.9f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = pillScale.value
                        alpha = pillAlpha.value.coerceIn(0f, 1f)
                    }
                    .background(color = scheme.primaryContainer, shape = CircleShape)
            )
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    }
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
