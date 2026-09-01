package com.veggiebit.sprout.app.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Stem Brand Mark — Minimalist geometry: vertical stem bar with angled branch line at 38 degrees.
 * Faithful translation of Stem.dc.html logo specification.
 */
@Composable
fun StemLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = LocalStemColors.current.ink
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Scaling reference from 40x40 design:
        // Main vertical stem: x = 18/40, top = 4/40, width = 4/40, height = 32/40
        val stemLeft = w * (18f / 40f)
        val stemTop = h * (4f / 40f)
        val stemWidth = w * (4f / 40f)
        val stemHeight = h * (32f / 40f)

        drawRect(
            color = tint,
            topLeft = Offset(stemLeft, stemTop),
            size = androidx.compose.ui.geometry.Size(stemWidth, stemHeight)
        )

        // Angled branch: x = 7/40, top = 11/40, width = 22/40, height = 4/40 rotated 38 deg around left-center
        val branchOriginX = w * (7f / 40f)
        val branchOriginY = h * (13f / 40f) // Center of the 4px bar at top 11px
        val branchLength = w * (22f / 40f)
        val branchThickness = h * (4f / 40f)

        // Draw rotated branch
        val angleRad = Math.toRadians(38.0).toFloat()
        val cos = Math.cos(angleRad.toDouble()).toFloat()
        val sin = Math.sin(angleRad.toDouble()).toFloat()

        val halfThick = branchThickness / 2f
        val perpX = -sin * halfThick
        val perpY = cos * halfThick

        val dx = cos * branchLength
        val dy = sin * branchLength

        val path = Path().apply {
            moveTo(branchOriginX - perpX, branchOriginY - perpY)
            lineTo(branchOriginX + dx - perpX, branchOriginY + dy - perpY)
            lineTo(branchOriginX + dx + perpX, branchOriginY + dy + perpY)
            lineTo(branchOriginX + perpX, branchOriginY + perpY)
            close()
        }
        drawPath(path, color = tint, style = Fill)
    }
}

/**
 * Geometric Icons for Stem Presets & Providers (SquareOutline, Bar, SquareFilled, Triangle,
 * CircleOutline, Lines, Dots, Diamond, Plus).
 */
@Composable
fun StemGeometricIcon(
    iconType: StemIconType,
    modifier: Modifier = Modifier,
    tint: Color = LocalStemColors.current.ink,
    size: Dp = 12.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when (iconType) {
            StemIconType.SQUARE_OUTLINE -> {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .border(1.5.dp, tint, RoundedCornerShape(1.dp))
                )
            }
            StemIconType.BAR -> {
                Box(
                    modifier = Modifier
                        .size(width = 10.dp, height = 2.dp)
                        .background(tint)
                )
            }
            StemIconType.SQUARE_FILLED -> {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(tint)
                )
            }
            StemIconType.TRIANGLE -> {
                Canvas(modifier = Modifier.size(width = 10.dp, height = 9.dp)) {
                    val path = Path().apply {
                        moveTo(this@Canvas.size.width / 2f, 0f)
                        lineTo(this@Canvas.size.width, this@Canvas.size.height)
                        lineTo(0f, this@Canvas.size.height)
                        close()
                    }
                    drawPath(path, color = tint, style = Fill)
                }
            }
            StemIconType.CIRCLE_OUTLINE -> {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .border(1.5.dp, tint, CircleShape)
                )
            }
            StemIconType.LINES -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(width = 9.dp, height = 1.5.dp).background(tint))
                    Box(modifier = Modifier.size(width = 9.dp, height = 1.5.dp).background(tint))
                    Box(modifier = Modifier.size(width = 9.dp, height = 1.5.dp).background(tint))
                }
            }
            StemIconType.DOTS -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(tint))
                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(tint))
                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(tint))
                }
            }
            StemIconType.DIAMOND -> {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .rotate(45f)
                        .background(tint)
                )
            }
            StemIconType.PLUS -> {
                Box(modifier = Modifier.size(9.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(width = 9.dp, height = 1.5.dp).background(tint))
                    Box(modifier = Modifier.size(width = 1.5.dp, height = 9.dp).background(tint))
                }
            }
        }
    }
}

enum class StemIconType {
    SQUARE_OUTLINE,
    BAR,
    SQUARE_FILLED,
    TRIANGLE,
    CIRCLE_OUTLINE,
    LINES,
    DOTS,
    DIAMOND,
    PLUS
}

/**
 * Custom Tab Icons for the Bottom Navigation Bar:
 * - Home: 16x16 dp square outline with 2dp border
 * - Snippets: 3 horizontal bars (14x2, 10x2, 12x2 dp)
 * - History: 16x16 dp circle with center-top clock hand
 * - Settings: 3 vertical equalizer sliders (2x10, 2x16, 2x8 dp)
 */
@Composable
fun StemTabIcon(
    tab: StemTab,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (tab) {
            StemTab.HOME -> {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(2.dp, color, RoundedCornerShape(2.dp))
                )
            }
            StemTab.SNIPPETS -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.size(width = 14.dp, height = 14.dp)
                ) {
                    Box(modifier = Modifier.size(width = 14.dp, height = 2.dp).background(color))
                    Box(modifier = Modifier.size(width = 10.dp, height = 2.dp).background(color))
                    Box(modifier = Modifier.size(width = 12.dp, height = 2.dp).background(color))
                }
            }
            StemTab.HISTORY -> {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(2.dp, color, CircleShape),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 1.5.dp, height = 5.dp)
                            .background(color)
                    )
                }
            }
            StemTab.SETTINGS -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.size(width = 16.dp, height = 16.dp)
                ) {
                    Box(modifier = Modifier.size(width = 2.dp, height = 10.dp).background(color))
                    Box(modifier = Modifier.size(width = 2.dp, height = 16.dp).background(color))
                    Box(modifier = Modifier.size(width = 2.dp, height = 8.dp).background(color))
                }
            }
        }
    }
}

enum class StemTab(val title: String) {
    HOME("Home"),
    SNIPPETS("Snippets"),
    HISTORY("History"),
    SETTINGS("Settings")
}
