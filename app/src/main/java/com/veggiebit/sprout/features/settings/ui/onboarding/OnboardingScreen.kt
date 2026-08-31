package com.veggiebit.sprout.features.settings.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.veggiebit.sprout.features.settings.ui.components.PermissionStepCard

/**
 * First-run onboarding: welcome -> accessibility permission -> overlay permission -> done.
 * plan.md Phase 1 calls for a step-by-step permission flow; previously the two permission
 * cards were just sitting in the settings list with no guided first-run path.
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
    val totalSteps = 3

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (index <= step) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(260))).togetherWith(fadeOut(animationSpec = tween(180)))
                },
                label = "onboardingStep"
            ) { currentStep ->
                when (currentStep) {
                    0 -> WelcomeStep()
                    1 -> PermissionStep(
                        icon = Icons.Rounded.TouchApp,
                        title = "Accessibility Service",
                        description = "Sprout uses Android's Accessibility Service to detect focused text fields and offer inline replacements — it never reads content outside text you're actively editing.",
                        stepCard = {
                            PermissionStepCard(
                                stepNumber = 1,
                                title = "Accessibility Service",
                                description = "Monitors text focus & provides inline replacement across apps.",
                                icon = Icons.Rounded.TouchApp,
                                isGranted = hasAccessibilityPermission,
                                onGrantClick = onRequestAccessibilityPermission
                            )
                        }
                    )
                    else -> PermissionStep(
                        icon = Icons.Rounded.Layers,
                        title = "Display Over Other Apps",
                        description = "This lets the floating pill and suggestion panel appear above whatever you're typing in. You can turn it off any time and use text-selection triggering instead.",
                        stepCard = {
                            PermissionStepCard(
                                stepNumber = 2,
                                title = "Display Over Other Apps",
                                description = "Shows the floating 36dp pill & expanded suggestion capsule.",
                                icon = Icons.Rounded.Layers,
                                isGranted = hasOverlayPermission,
                                onGrantClick = onRequestOverlayPermission
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onFinish) {
                Text("Skip for now")
            }

            Button(
                onClick = {
                    if (step < totalSteps - 1) step++ else onFinish()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (step < totalSteps - 1) "Continue" else "Get Started")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (step < totalSteps - 1) Icons.Rounded.ArrowForward else Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Spa,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Welcome to Sprout",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "A floating writing assistant that fixes, polishes, and transforms text right where you're typing — in any app.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Two permissions get it running. Both are explained on the next screens.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PermissionStep(
    icon: ImageVector,
    title: String,
    description: String,
    stepCard: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        stepCard()
    }
}
