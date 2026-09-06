package com.example.ui.components

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.camera.CameraManager
import com.example.camera.FlashState
import com.example.data.CameraFacing
import com.example.data.CameraSettings
import com.example.ui.theme.AmberPrimary

@Composable
fun CameraViewfinder(
    cameraManager: CameraManager,
    currentLens: CameraFacing,
    flashState: FlashState,
    settings: CameraSettings,
    onDoubleTapToFlip: () -> Unit,
    onBoundChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isHardwareBound by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(settings.doubleTapToFlip) {
                detectTapGestures(
                    onDoubleTap = {
                        if (settings.doubleTapToFlip) {
                            onDoubleTapToFlip()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    zoomLevel = (zoomLevel * zoom).coerceIn(1f, 5f)
                    cameraManager.getCameraControl()?.setZoomRatio(zoomLevel)
                }
            }
    ) {
        // Live camera preview view
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            update = { previewView ->
                cameraManager.bindCamera(
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    lens = currentLens,
                    flash = flashState,
                    resolution = settings.videoResolution
                ) { bound ->
                    isHardwareBound = bound
                    onBoundChange(bound)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder Grid & Focus Reticle Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 3x3 Rule of Thirds Grid
            val gridColor = Color.White.copy(alpha = 0.12f)
            val stroke = Stroke(width = 1.dp.toPx())

            // Vertical grid lines
            drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = stroke.width)
            drawLine(gridColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), strokeWidth = stroke.width)

            // Horizontal grid lines
            drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = stroke.width)
            drawLine(gridColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), strokeWidth = stroke.width)

            // Center Focus Crosshairs
            val cx = w / 2f
            val cy = h / 2f
            val crossSize = 24.dp.toPx()
            val reticleColor = AmberPrimary.copy(alpha = 0.6f)

            drawLine(reticleColor, Offset(cx - crossSize, cy), Offset(cx - 8.dp.toPx(), cy), strokeWidth = 2.dp.toPx())
            drawLine(reticleColor, Offset(cx + 8.dp.toPx(), cy), Offset(cx + crossSize, cy), strokeWidth = 2.dp.toPx())
            drawLine(reticleColor, Offset(cx, cy - crossSize), Offset(cx, cy - 8.dp.toPx()), strokeWidth = 2.dp.toPx())
            drawLine(reticleColor, Offset(cx, cy + 8.dp.toPx()), Offset(cx, cy + crossSize), strokeWidth = 2.dp.toPx())

            drawCircle(
                color = reticleColor,
                radius = 32.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Lens & Zoom readout overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 110.dp)
        ) {
            Text(
                text = "${if (currentLens == CameraFacing.BACK) "24mm f/1.8" else "16mm f/2.2"}  •  ${String.format("%.1fx", zoomLevel)}",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
