package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.HabitCatalog
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun HabitSegmentedRing(
    completedParts: Int,
    targetParts: Int,
    accentColor: Color,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
    strokeWidth: Dp = 5.dp,
    centerContent: @Composable () -> Unit = {}
) {
    val targetProgress = (completedParts.toFloat() / targetParts.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ring_progress"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check_scale"
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)

            if (targetParts in 2..12) {
                val gapDegrees = 10f
                val segmentSweep = (360f - gapDegrees * targetParts) / targetParts
                for (i in 0 until targetParts) {
                    val startAngle = -90f + i * (segmentSweep + gapDegrees) + gapDegrees / 2f
                    val segColor = if (i < completedParts) accentColor else trackColor
                    drawArc(
                        color = segColor,
                        startAngle = startAngle,
                        sweepAngle = segmentSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            } else {
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                if (animatedProgress > 0f) {
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }
        }

        if (isCompleted) {
            Box(
                modifier = Modifier
                    .size(size - strokeWidth * 2 - 4.dp)
                    .scale(checkScale)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.44f)
                )
            }
        } else {
            centerContent()
        }
    }
}

@Composable
fun SegmentedPartsBar(
    completedParts: Int,
    targetParts: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (targetParts <= 1) return
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    if (targetParts <= 10) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0 until targetParts) {
                val filled = i < completedParts
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (filled) accentColor else trackColor)
                )
            }
        }
    } else {
        val animatedRatio by animateFloatAsState(
            targetValue = (completedParts.toFloat() / targetParts).coerceIn(0f, 1f),
            animationSpec = tween(260),
            label = "parts_bar"
        )
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedRatio)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accentColor)
            )
        }
    }
}

private data class ConfettiParticle(
    val angleRad: Float,
    val speed: Float,
    val radius: Float,
    val color: Color
)

@Composable
fun CelebrationConfettiOverlay(
    triggerKey: Int,
    modifier: Modifier = Modifier
) {
    if (triggerKey <= 0) return
    val progress = remember(triggerKey) { Animatable(0f) }
    val particles = remember(triggerKey) {
        val rng = Random(triggerKey)
        val palette = HabitCatalog.colors.map { it.primary }
        List(32) {
            ConfettiParticle(
                angleRad = (rng.nextFloat() * 2f * PI).toFloat(),
                speed = 180f + rng.nextFloat() * 340f,
                radius = 5f + rng.nextFloat() * 7f,
                color = palette[rng.nextInt(palette.size)]
            )
        }
    }

    LaunchedEffect(triggerKey) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1050, easing = FastOutSlowInEasing)
        )
    }

    val p = progress.value
    if (p < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val origin = Offset(size.width / 2f, size.height * 0.25f)
            val alpha = (1f - p).coerceIn(0f, 1f)
            val gravity = 280f * p * p
            particles.forEach { pt ->
                val dx = cos(pt.angleRad) * pt.speed * p
                val dy = sin(pt.angleRad) * pt.speed * p + gravity
                drawCircle(
                    color = pt.color.copy(alpha = alpha),
                    radius = pt.radius * (1f - p * 0.35f),
                    center = Offset(origin.x + dx, origin.y + dy)
                )
            }
        }
    }
}
