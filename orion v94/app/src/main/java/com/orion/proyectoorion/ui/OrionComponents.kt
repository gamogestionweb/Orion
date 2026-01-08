package com.orion.proyectoorion.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================================
// ORION UI COMPONENTS - Professional Design System
// ==========================================================

/**
 * Primary action button with modern elevated design
 */
@Composable
fun OrionPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    accentColor: Color = OrionPurple,
    textColor: Color = OrionBlack,
    elevation: Dp = 4.dp
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = if (enabled) elevation else 0.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = accentColor.copy(alpha = 0.3f)
            )
            .height(54.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            disabledContainerColor = OrionMediumGrey
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OrionBodyFont,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Secondary outlined button
 */
@Composable
fun OrionSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    accentColor: Color = OrionPurple
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(2.dp, if (enabled) accentColor else OrionTextDisabled),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = accentColor.copy(alpha = 0.08f),
            contentColor = accentColor
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = text,
                color = accentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = OrionBodyFont,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Modern card with icon and gradient accent
 */
@Composable
fun OrionCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    accentColor: Color = OrionPurple,
    subtitle: String? = null,
    badge: String? = null,
    enabled: Boolean = true
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = accentColor.copy(alpha = 0.2f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = OrionDarkGrey
        ),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(18.dp),
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container with gradient background
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.2f),
                                    accentColor.copy(alpha = 0.08f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
                Spacer(Modifier.width(16.dp))
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = accentColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OrionBodyFont
                    )

                    badge?.let {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = accentColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = it,
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = OrionBodyFont,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                subtitle?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = it,
                        color = OrionTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = OrionMonoFont
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = description,
                    color = OrionTextSecondary,
                    fontSize = 13.sp,
                    fontFamily = OrionBodyFont,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * Animated loading indicator
 */
@Composable
fun OrionLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = OrionPurple,
    size: Dp = 40.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = 3.dp
        )
    }
}

/**
 * Section header with icon
 */
@Composable
fun OrionSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = OrionPurple,
    subtitle: String? = null
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
            }

            Text(
                text = title,
                color = accentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OrionDisplayFont,
                letterSpacing = 1.sp
            )
        }

        subtitle?.let {
            Spacer(Modifier.height(6.dp))
            Text(
                text = it,
                color = OrionTextSecondary,
                fontSize = 13.sp,
                fontFamily = OrionBodyFont
            )
        }
    }
}

/**
 * Info card for warnings or notices
 */
@Composable
fun OrionInfoCard(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = OrionOrange,
    backgroundColor: Color = accentColor.copy(alpha = 0.1f)
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
            }

            Text(
                text = text,
                color = accentColor,
                fontSize = 13.sp,
                fontFamily = OrionBodyFont,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Badge component
 */
@Composable
fun OrionBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = OrionPurple,
    textColor: Color = OrionBlack
) {
    Surface(
        modifier = modifier,
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = OrionBodyFont,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            letterSpacing = 0.5.sp
        )
    }
}
