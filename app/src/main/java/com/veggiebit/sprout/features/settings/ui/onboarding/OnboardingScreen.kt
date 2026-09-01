package com.veggiebit.sprout.features.settings.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veggiebit.sprout.app.theme.LocalStemColors
import com.veggiebit.sprout.app.theme.StemCardShape
import com.veggiebit.sprout.app.theme.StemGeometricIcon
import com.veggiebit.sprout.app.theme.StemIconType
import com.veggiebit.sprout.app.theme.StemIndicatorShape
import com.veggiebit.sprout.app.theme.StemLogoMark
import com.veggiebit.sprout.app.theme.StemMonoBadge
import com.veggiebit.sprout.app.theme.StemMonoCode
import com.veggiebit.sprout.app.theme.StemSharpShape
import com.veggiebit.sprout.features.enhancement.data.models.DiffToken
import com.veggiebit.sprout.features.enhancement.data.models.DiffType
import com.veggiebit.sprout.features.enhancement.ui.components.DiffViewer

/**
 * Stem 4-Step Onboarding Flow:
 * Step 0: Welcome & Features
 * Step 1: Accessibility Service Permission
 * Step 2: Overlay Permission
 * Step 3: Done & Try It Demo
 * Matches Stem.dc.html design specification.
 */
@Composable
fun OnboardingScreen(
    hasAccessibilityPermission: Boolean,
    hasOverlayPermission: Boolean,
    onRequestAccessibilityPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 4
    val stemTheme = LocalStemColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(stemTheme.bg)
            .systemBarsPadding()
            .imePadding()
            .padding(24.dp)
    ) {
        // Top Bar: 4-Segment Progress Indicator & Skip Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
                    .clearAndSetSemantics { contentDescription = "Step ${step + 1} of $totalSteps" },
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(totalSteps) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(StemIndicatorShape)
                            .background(
                                if (index <= step) stemTheme.ink
                                else stemTheme.surface3
                            )
                    )
                }
            }

            if (step < totalSteps - 1) {
                Text(
                    text = "SKIP",
                    style = StemMonoBadge,
                    color = stemTheme.inkMuted,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onFinish)
                        .padding(4.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(36.dp))
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Content Area
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(animationSpec = tween(240)).togetherWith(fadeOut(animationSpec = tween(160)))
                },
                label = "onboardingStep"
            ) { currentStep ->
                when (currentStep) {
                    0 -> WelcomeStep()
                    1 -> AccessibilityStep(
                        isGranted = hasAccessibilityPermission,
                        onGrant = onRequestAccessibilityPermission
                    )
                    2 -> OverlayStep(
                        isGranted = hasOverlayPermission,
                        onGrant = onRequestOverlayPermission
                    )
                    else -> DoneStep()
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bottom CTA Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(StemSharpShape)
                .background(stemTheme.ink)
                .clickable(
                    role = Role.Button,
                    onClick = {
                        if (step < totalSteps - 1) step++ else onFinish()
                    }
                )
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (step) {
                    0 -> "Get started"
                    1 -> if (hasAccessibilityPermission) "Continue" else "Next"
                    2 -> if (hasOverlayPermission) "Continue" else "Next"
                    else -> "Open Stem"
                },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = stemTheme.onInk
            )
        }
    }
}

@Composable
private fun WelcomeStep() {
    val stemTheme = LocalStemColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        StemLogoMark(
            size = 40.dp,
            tint = stemTheme.ink
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Stem",
            style = MaterialTheme.typography.displayMedium,
            color = stemTheme.ink
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Ambient writing help, wherever you type.",
            style = MaterialTheme.typography.bodyLarge,
            color = stemTheme.inkMuted
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Feature Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(StemCardShape)
                .background(stemTheme.surface)
                .border(1.dp, stemTheme.border, StemCardShape)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FeatureRow(
                    iconType = StemIconType.SQUARE_OUTLINE,
                    title = "Fix typos & grammar in real time"
                )
                FeatureRow(
                    iconType = StemIconType.TRIANGLE,
                    title = "Transform tone: punchy, formal, friendly"
                )
                FeatureRow(
                    iconType = StemIconType.CIRCLE_OUTLINE,
                    title = "Never leaves your device (unless you ask)"
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    iconType: StemIconType,
    title: String
) {
    val stemTheme = LocalStemColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StemGeometricIcon(
            iconType = iconType,
            tint = stemTheme.inkMuted,
            size = 12.dp
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = stemTheme.ink
        )
    }
}

@Composable
private fun AccessibilityStep(
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    val stemTheme = LocalStemColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "STEP 1 OF 2",
            style = StemMonoBadge,
            color = stemTheme.inkFaint
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Read & replace text",
            style = MaterialTheme.typography.headlineMedium,
            color = stemTheme.ink
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Stem needs Accessibility permission to detect when you're typing and offer instant inline rewrites.",
            style = MaterialTheme.typography.bodyMedium,
            color = stemTheme.inkMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Permission Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(StemCardShape)
                .background(stemTheme.surface)
                .border(1.dp, stemTheme.border, StemCardShape)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Android Accessibility Service",
                        style = MaterialTheme.typography.titleMedium,
                        color = stemTheme.ink
                    )
                    if (isGranted) {
                        Text(
                            text = "Granted ✓",
                            style = StemMonoBadge,
                            color = stemTheme.add
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Only reads the active input field. Never sends keystrokes off-device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = stemTheme.inkMuted
                )

                if (!isGranted) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(StemSharpShape)
                            .background(stemTheme.surface2)
                            .border(1.dp, stemTheme.border, StemSharpShape)
                            .clickable(role = Role.Button, onClick = onGrant)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Enable in Settings",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = stemTheme.ink
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayStep(
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    val stemTheme = LocalStemColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "STEP 2 OF 2",
            style = StemMonoBadge,
            color = stemTheme.inkFaint
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Float near your cursor",
            style = MaterialTheme.typography.headlineMedium,
            color = stemTheme.ink
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "The overlay permission lets Stem show a tiny 40px helper next to any text field.",
            style = MaterialTheme.typography.bodyMedium,
            color = stemTheme.inkMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Permission Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(StemCardShape)
                .background(stemTheme.surface)
                .border(1.dp, stemTheme.border, StemCardShape)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Display over other apps",
                        style = MaterialTheme.typography.titleMedium,
                        color = stemTheme.ink
                    )
                    if (isGranted) {
                        Text(
                            text = "Granted ✓",
                            style = StemMonoBadge,
                            color = stemTheme.add
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Allows the floating stem icon and quick-transform menu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = stemTheme.inkMuted
                )

                if (!isGranted) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(StemSharpShape)
                            .background(stemTheme.surface2)
                            .border(1.dp, stemTheme.border, StemSharpShape)
                            .clickable(role = Role.Button, onClick = onGrant)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Allow overlay",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = stemTheme.ink
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoneStep() {
    val stemTheme = LocalStemColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "READY",
            style = StemMonoBadge,
            color = stemTheme.add
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "You're all set.",
            style = MaterialTheme.typography.headlineMedium,
            color = stemTheme.ink
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Type anywhere — a stem icon will appear near your cursor. Tap it or use text selection to rewrite.",
            style = MaterialTheme.typography.bodyMedium,
            color = stemTheme.inkMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Simulation Demo Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(StemCardShape)
                .background(stemTheme.surface)
                .border(1.dp, stemTheme.border, StemCardShape)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "TRY IT OUT",
                    style = StemMonoBadge,
                    color = stemTheme.inkFaint
                )

                Spacer(modifier = Modifier.height(8.dp))

                DiffViewer(
                    diffTokens = listOf(
                        DiffToken("Fix: ", DiffType.UNMODIFIED),
                        DiffToken("thier", DiffType.DELETED),
                        DiffToken("there", DiffType.ADDED),
                        DiffToken(" was no problem with the presentation", DiffType.UNMODIFIED)
                    )
                )
            }
        }
    }
}

