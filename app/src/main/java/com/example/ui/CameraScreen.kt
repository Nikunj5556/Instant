package com.example.ui

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CameraBottomBar
import com.example.ui.components.CameraTopBar
import com.example.ui.components.CameraViewfinder
import com.example.ui.components.GallerySheet
import com.example.ui.components.SettingsSheet
import com.example.ui.theme.AmberPrimary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val captures by viewModel.captures.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Request permissions (Camera & Audio)
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera Viewfinder (Full screen)
        CameraViewfinder(
            cameraManager = viewModel.cameraManager,
            currentLens = uiState.currentLens,
            flashState = uiState.currentFlash,
            settings = settings,
            onDoubleTapToFlip = { viewModel.flipCamera() },
            onBoundChange = { bound -> viewModel.setHardwareBound(bound) },
            modifier = Modifier.fillMaxSize()
        )

        // Top Controls: Flash, Recording Status Indicator, Audio Gain, Flip Camera, Privacy
        CameraTopBar(
            flashState = uiState.currentFlash,
            isRecording = uiState.isRecordingVideo,
            recordingDurationSeconds = uiState.recordingDurationSeconds,
            currentLens = uiState.currentLens,
            settings = settings,
            onToggleFlash = { viewModel.toggleFlash() },
            onFlipCamera = { viewModel.flipCamera() },
            onPrivacyClick = { showPrivacyDialog = true },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )

        // Bottom Controls:
        // Left: Gallery Button (previous captures)
        // Mid: Click button (tap photo, long press video)
        // Right: Settings Toggle & Flip Camera Button
        CameraBottomBar(
            isRecording = uiState.isRecordingVideo,
            latestCapture = captures.firstOrNull(),
            capturesCount = captures.size,
            onPhotoClick = { viewModel.triggerShutterPhoto() },
            onVideoLongPress = { viewModel.startVideoRecording() },
            onStopRecordingClick = { viewModel.stopVideoRecording() },
            onGalleryClick = { viewModel.openGallery() },
            onSettingsClick = { viewModel.openSettings() },
            onFlipCameraClick = { viewModel.flipCamera() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Instant Shutter Flash Animation Overlay (White screen flash on photo snap)
        AnimatedVisibility(
            visible = uiState.isShutterFlashVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }

        // Permission advisory banner if camera permission is denied
        if (!permissionsState.allPermissionsGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xDD111827)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Camera Permission Required",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Grant camera and audio permissions to capture instant moments.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionsState.launchMultiplePermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = Color.Black)
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }

        // Gallery Sheet Overlay
        AnimatedVisibility(
            visible = uiState.isGalleryOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            GallerySheet(
                captures = captures,
                initialCapture = uiState.selectedCapture,
                onClose = { viewModel.closeGallery() },
                onToggleFavorite = { capture -> viewModel.toggleFavorite(capture) },
                onDeleteCapture = { capture -> viewModel.deleteCapture(capture) },
                onUploadToS3 = { capture -> viewModel.uploadCaptureToS3(capture) }
            )
        }

        // Settings Sheet Overlay
        AnimatedVisibility(
            visible = uiState.isSettingsOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            SettingsSheet(
                settings = settings,
                onUpdateSettings = { transform -> viewModel.settingsRepository.updateSettings(transform) },
                onTestAwsConnection = { callback -> viewModel.testAwsConnection(callback) },
                onSyncAllPending = { viewModel.backupAllPendingCaptures() },
                isBackingUp = uiState.isBackingUp,
                backupStatusMessage = uiState.backupStatusMessage,
                onClose = { viewModel.closeSettings() }
            )
        }

        // Toast Message notification
        uiState.toastMessage?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp, start = 16.dp, end = 16.dp),
                containerColor = Color(0xEE1E232B),
                contentColor = Color.White,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = msg, fontSize = 13.sp)
            }
        }

        // Privacy Reassurance Dialog
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "100% Local & Safe Privacy Guarantee",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Instant Camera is built specifically for travellers with absolute privacy in mind.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "• All photos and videos remain strictly local on your device.\n• No tracking, no third-party telemetry, no cloud accounts required.\n• If Developer Mode AWS S3 backup is enabled, credentials stay offline on your phone and upload directly to your private bucket.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) {
                        Text("Got it", color = AmberPrimary)
                    }
                },
                containerColor = Color(0xFF16191E)
            )
        }
    }
}
