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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Stem Brand Mark — Minimalist geometry: bold vertical stem bar with angled branch line at 38 degrees.
 */
@Composable
fun StemLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tint: Color = LocalStemColors.current.ink
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Main vertical stem
        val stemLeft = w * (17f / 40f)
        val stemTop = h * (4f / 40f)
        val stemWidth = w * (5f / 40f)
        val stemHeight = h * (32f / 40f)

        drawRect(
            color = tint,
            topLeft = Offset(stemLeft, stemTop),
            size = androidx.compose.ui.geometry.Size(stemWidth, stemHeight)
        )

        // Angled branch: rotated 38 deg around left-center
        val branchOriginX = w * (7f / 40f)
        val branchOriginY = h * (12f / 40f)
        val branchLength = w * (24f / 40f)
        val branchThickness = h * (5f / 40f)

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
 * Enhanced Geometric Icons for Stem Presets (SquareOutline, Bar, SquareFilled, Triangle,
 * CircleOutline, Lines, Dots, Diamond, Plus).
 */
@Composable
fun StemGeometricIcon(
    iconType: StemIconType,
    modifier: Modifier = Modifier,
    tint: Color = LocalStemColors.current.ink,
    size: Dp = 14.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when (iconType) {
            StemIconType.SQUARE_OUTLINE -> {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .border(2.dp, tint, RoundedCornerShape(1.dp))
                )
            }
            StemIconType.BAR -> {
                Box(
                    modifier = Modifier
                        .size(width = 11.dp, height = 2.5.dp)
                        .background(tint)
                )
            }
            StemIconType.SQUARE_FILLED -> {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(tint)
                )
            }
            StemIconType.TRIANGLE -> {
                Canvas(modifier = Modifier.size(width = 11.dp, height = 10.dp)) {
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
                        .size(10.dp)
                        .border(2.dp, tint, CircleShape)
                )
            }
            StemIconType.LINES -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(width = 10.dp, height = 2.dp).background(tint))
                    Box(modifier = Modifier.size(width = 10.dp, height = 2.dp).background(tint))
                    Box(modifier = Modifier.size(width = 10.dp, height = 2.dp).background(tint))
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
                        .size(8.dp)
                        .rotate(45f)
                        .background(tint)
                )
            }
            StemIconType.PLUS -> {
                Box(modifier = Modifier.size(10.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(width = 10.dp, height = 2.dp).background(tint))
                    Box(modifier = Modifier.size(width = 2.dp, height = 10.dp).background(tint))
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
 * Enhanced Crisp Tab Icons for the Bottom Navigation Bar:
 * - Home: Minimalist house with pitched roof and doorway
 * - Snippets: Tiered code block lines
 * - History: Precision analog clock with hour and minute hands
 * - Settings: Precision equalizer sliders
 */
@Composable
fun StemTabIcon(
    tab: StemTab,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height

        when (tab) {
            StemTab.HOME -> {
                val path = Path().apply {
                    // Roof
                    moveTo(w * 0.5f, h * 0.22f)
                    lineTo(w * 0.82f, h * 0.48f)
                    lineTo(w * 0.82f, h * 0.78f)
                    lineTo(w * 0.18f, h * 0.78f)
                    lineTo(w * 0.18f, h * 0.48f)
                    close()
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            StemTab.SNIPPETS -> {
                // 3 code lines
                val strokeW = 2.2.dp.toPx()
                drawLine(
                    color = color,
                    start = Offset(w * 0.22f, h * 0.32f),
                    end = Offset(w * 0.78f, h * 0.32f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.22f, h * 0.50f),
                    end = Offset(w * 0.58f, h * 0.50f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.22f, h * 0.68f),
                    end = Offset(w * 0.70f, h * 0.68f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
            StemTab.HISTORY -> {
                val center = Offset(w * 0.5f, h * 0.5f)
                val radius = w * 0.32f
                val strokeW = 2.2.dp.toPx()
                drawCircle(
                    color = color,
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeW)
                )
                // Clock hands: 12 o'clock and 3 o'clock
                drawLine(
                    color = color,
                    start = center,
                    end = Offset(center.x, center.y - radius * 0.55f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = center,
                    end = Offset(center.x + radius * 0.45f, center.y),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
            StemTab.SETTINGS -> {
                val strokeW = 2.2.dp.toPx()
                // Track 1
                drawLine(
                    color = color,
                    start = Offset(w * 0.30f, h * 0.24f),
                    end = Offset(w * 0.30f, h * 0.76f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawCircle(color = color, radius = 2.8.dp.toPx(), center = Offset(w * 0.30f, h * 0.40f))

                // Track 2
                drawLine(
                    color = color,
                    start = Offset(w * 0.50f, h * 0.24f),
                    end = Offset(w * 0.50f, h * 0.76f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawCircle(color = color, radius = 2.8.dp.toPx(), center = Offset(w * 0.50f, h * 0.62f))

                // Track 3
                drawLine(
                    color = color,
                    start = Offset(w * 0.70f, h * 0.24f),
                    end = Offset(w * 0.70f, h * 0.76f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawCircle(color = color, radius = 2.8.dp.toPx(), center = Offset(w * 0.70f, h * 0.36f))
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
