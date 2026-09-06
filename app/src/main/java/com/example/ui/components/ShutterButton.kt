package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.RecordingRed

@Composable
fun ShutterButton(
    isRecording: Boolean,
    onPhotoClick: () -> Unit,
    onVideoLongPress: () -> Unit,
    onStopRecordingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recordingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val ringColor by animateColorAsState(
        targetValue = if (isRecording) RecordingRed else Color.White,
        animationSpec = tween(300),
        label = "ringColor"
    )

    val innerButtonColor by animateColorAsState(
        targetValue = if (isRecording) RecordingRed else Color.White,
        animationSpec = tween(300),
        label = "innerButtonColor"
    )

    val innerScale by animateFloatAsState(
        targetValue = if (isRecording) 0.55f else 0.82f,
        animationSpec = tween(250),
        label = "innerScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(88.dp)
            .testTag("shutter_button")
            .pointerInput(isRecording) {
                detectTapGestures(
                    onTap = {
                        if (isRecording) {
                            onStopRecordingClick()
                        } else {
                            onPhotoClick()
                        }
                    },
                    onLongPress = {
                        if (!isRecording) {
                            onVideoLongPress()
                        }
                    }
                )
            }
    ) {
        // Outer pulsing ring during recording
        if (isRecording) {
            Canvas(
                modifier = Modifier
                    .size(88.dp)
                    .scale(pulseScale)
            ) {
                drawCircle(
                    color = RecordingRed.copy(alpha = 0.35f),
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }

        // Main outer ring
        Canvas(modifier = Modifier.size(80.dp)) {
            drawCircle(
                color = ringColor,
                radius = (size.minDimension / 2f) - 2.dp.toPx(),
                style = Stroke(width = 4.5.dp.toPx())
            )
        }

        // Center Shutter Pad (circle for photo, morphs to stop square for recording)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .scale(innerScale)
                .clip(if (isRecording) MaterialTheme.shapes.medium else CircleShape)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(color = innerButtonColor)
            }

            if (isRecording) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop Recording",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
