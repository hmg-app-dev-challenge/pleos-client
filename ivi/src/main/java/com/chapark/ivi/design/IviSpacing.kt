package com.chapark.ivi.design

import androidx.compose.ui.unit.dp

/** ChaPark layout roles using Pleos component dimensions, not additional Figma global tokens.
 * 16:293: 16px control gap; 18:1697: 24px related / 40px section gap.
 * 32px card inset follows the existing medium-button horizontal inset (16:569).
 */
object IviSpacing {
    val control = 16.dp
    val related = 24.dp
    val section = 40.dp
    val cardInset = 32.dp
}
