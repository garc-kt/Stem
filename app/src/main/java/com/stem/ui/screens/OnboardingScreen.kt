package com.stem.ui.screens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stem.R
import com.stem.ui.theme.LocalStemColors
import com.stem.ui.theme.StemCardShape
import com.stem.ui.theme.StemGeometricIcon
import com.stem.ui.theme.StemIconType
import com.stem.ui.theme.StemIndicatorShape
import com.stem.ui.theme.StemLogoMark
import com.stem.ui.theme.StemMonoBadge
import com.stem.ui.theme.StemSharpShape
import com.stem.core.models.DiffToken
import com.stem.core.models.DiffType
import com.stem.ui.components.DiffViewer



/**
 * Stem 3-Step Onboarding Flow:
 * Step 0: Welcome & Features
 * Step 1: Accessibility Service Permission
 * Step 2: Ready & Inline Commands Guide
 */
@Composable
fun OnboardingScreen(
    hasAccessibilityPermission: Boolean,
    onRequestAccessibilityPermission: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 3
    val stemTheme = LocalStemColors.current
    // Resolved here, not inside clearAndSetSemantics { } below — that lambda runs in the
    // semantics DSL, not composition, so it can't call stringResource() directly.
    val stepDescription = stringResource(R.string.onboarding_step_indicator, step + 1, totalSteps)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(stemTheme.bg)
            .systemBarsPadding()
            .imePadding()
            .padding(24.dp)
    ) {
        // Top Bar: 3-Segment Progress Indicator & Skip Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
                    .clearAndSetSemantics { contentDescription = stepDescription },
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
                    text = stringResource(R.string.onboarding_skip_button),
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
                text = stringResource(
                    when (step) {
                        0 -> R.string.onboarding_cta_get_started
                        1 -> if (hasAccessibilityPermission) R.string.onboarding_cta_continue else R.string.onboarding_cta_next
                        else -> R.string.onboarding_cta_open_stem
                    }
                ),
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
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            color = stemTheme.ink
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.onboarding_tagline),
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
                    title = stringResource(R.string.onboarding_feature_fix)
                )
                FeatureRow(
                    iconType = StemIconType.TRIANGLE,
                    title = stringResource(R.string.onboarding_feature_tone)
                )
                FeatureRow(
                    iconType = StemIconType.CIRCLE_OUTLINE,
                    title = stringResource(R.string.onboarding_feature_privacy)
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
            text = stringResource(R.string.onboarding_accessibility_step_badge),
            style = StemMonoBadge,
            color = stemTheme.inkFaint
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.onboarding_accessibility_headline),
            style = MaterialTheme.typography.headlineMedium,
            color = stemTheme.ink
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.onboarding_accessibility_description),
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
                        text = stringResource(R.string.onboarding_accessibility_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = stemTheme.ink
                    )
                    if (isGranted) {
                        Text(
                            text = stringResource(R.string.onboarding_accessibility_granted),
                            style = StemMonoBadge,
                            color = stemTheme.add
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.onboarding_accessibility_card_description),
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
                            text = stringResource(R.string.onboarding_enable_in_settings_button),
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
            text = stringResource(R.string.onboarding_ready_badge),
            style = StemMonoBadge,
            color = stemTheme.add
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.onboarding_ready_headline),
            style = MaterialTheme.typography.headlineMedium,
            color = stemTheme.ink
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.onboarding_ready_description),
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
                    text = stringResource(R.string.onboarding_try_it_out_badge),
                    style = StemMonoBadge,
                    color = stemTheme.inkFaint
                )

                Spacer(modifier = Modifier.height(8.dp))

                DiffViewer(
                    diffTokens = listOf(
                        DiffToken(stringResource(R.string.onboarding_demo_prefix), DiffType.UNMODIFIED),
                        DiffToken(stringResource(R.string.onboarding_demo_wrong), DiffType.DELETED),
                        DiffToken(stringResource(R.string.onboarding_demo_right), DiffType.ADDED),
                        DiffToken(stringResource(R.string.onboarding_demo_suffix), DiffType.UNMODIFIED)
                    )
                )
            }
        }
    }
}


