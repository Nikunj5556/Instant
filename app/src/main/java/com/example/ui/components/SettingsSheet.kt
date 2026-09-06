package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppThemeMode
import com.example.data.AudioSource
import com.example.data.CameraFacing
import com.example.data.CameraSettings
import com.example.data.StorageDestination
import com.example.data.VideoFrameRate
import com.example.data.VideoResolution
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.RecordingRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    settings: CameraSettings,
    onUpdateSettings: ((CameraSettings) -> CameraSettings) -> Unit,
    onTestAwsConnection: ((Boolean, String) -> Unit) -> Unit,
    onSyncAllPending: () -> Unit,
    isBackingUp: Boolean,
    backupStatusMessage: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Camera & Video", "Audio & Input", "Privacy & Theme", "Developer / AWS")

    var showSecretKey by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Instant Camera Settings",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Box(modifier = Modifier.size(48.dp))
        }

        // Tab Navigation
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = AmberPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AmberPrimary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 1: Camera & Video
                    SettingSectionTitle(title = "Video Resolution & Quality", icon = Icons.Default.Videocam)
                    Text(
                        text = "Optimized for instant travel captures. Higher resolutions use more local storage.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VideoResolution.entries.forEach { res ->
                            FilterChip(
                                selected = settings.videoResolution == res,
                                onClick = {
                                    onUpdateSettings { it.copy(videoResolution = res) }
                                },
                                label = { Text(res.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    SettingSectionTitle(title = "Frame Rate (FPS)", icon = Icons.Default.Tune)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VideoFrameRate.entries.forEach { fps ->
                            FilterChip(
                                selected = settings.videoFrameRate == fps,
                                onClick = {
                                    onUpdateSettings { it.copy(videoFrameRate = fps) }
                                },
                                label = { Text(fps.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    SettingSectionTitle(title = "Storage Destination", icon = Icons.Default.Storage)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StorageDestination.entries.forEach { dest ->
                            Card(
                                onClick = {
                                    onUpdateSettings { it.copy(storageDestination = dest) }
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (settings.storageDestination == dest) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = dest.label,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = dest.description,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (settings.storageDestination == dest) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = AmberPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SettingSectionTitle(title = "Default Camera on Open", icon = Icons.Default.Videocam)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CameraFacing.entries.forEach { facing ->
                            FilterChip(
                                selected = settings.defaultCamera == facing,
                                onClick = {
                                    onUpdateSettings { it.copy(defaultCamera = facing) }
                                },
                                label = { Text(facing.label) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 2: Audio & Controls
                    SettingSectionTitle(title = "Microphone & Audio Input", icon = Icons.Default.Mic)
                    Text(
                        text = "Select audio routing for external lavalier or camcorder microphones.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AudioSource.entries.forEach { src ->
                            FilterChip(
                                selected = settings.audioSource == src,
                                onClick = {
                                    onUpdateSettings { it.copy(audioSource = src) }
                                },
                                label = { Text(src.label, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    SettingSectionTitle(title = "Real-Time Audio Gain Adjustment", icon = Icons.Default.Tune)
                    Text(
                        text = "Adjust microphone preamp gain (-12 dB to +18 dB): ${if (settings.audioGainDb >= 0) "+${settings.audioGainDb.toInt()} dB" else "${settings.audioGainDb.toInt()} dB"}",
                        color = AmberPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Slider(
                        value = settings.audioGainDb,
                        onValueChange = { newGain ->
                            onUpdateSettings { it.copy(audioGainDb = newGain) }
                        },
                        valueRange = -12f..18f,
                        steps = 30,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberPrimary,
                            activeTrackColor = AmberPrimary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mute Audio During Recording", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        Switch(
                            checked = settings.isAudioMuted,
                            onCheckedChange = { muted ->
                                onUpdateSettings { it.copy(isAudioMuted = muted) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = RecordingRed
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    SettingSectionTitle(title = "Shortcuts & Gestures", icon = Icons.Default.Gesture)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Volume Keys Shutter Shortcut", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Press volume keys to take instant photo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.volumeKeyShortcut,
                            onCheckedChange = { enabled ->
                                onUpdateSettings { it.copy(volumeKeyShortcut = enabled) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = AmberPrimary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Double-Tap Viewfinder to Flip Camera", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Quickly swap between front and back lens", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.doubleTapToFlip,
                            onCheckedChange = { enabled ->
                                onUpdateSettings { it.copy(doubleTapToFlip = enabled) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = AmberPrimary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gesture Controls for Playback", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Swipe captures, pinch-to-zoom, double-tap to favorite", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.gesturesEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateSettings { it.copy(gesturesEnabled = enabled) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = AmberPrimary
                            )
                        )
                    }
                }

                2 -> {
                    // TAB 3: Privacy & Theme
                    SettingSectionTitle(title = "App Theme & Night Mode", icon = Icons.Default.Palette)
                    Text(
                        text = "Low-light environments are ideal for night travel photography.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppThemeMode.entries.forEach { mode ->
                            Card(
                                onClick = {
                                    onUpdateSettings { it.copy(themeMode = mode) }
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (settings.themeMode == mode) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = mode.label,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (settings.themeMode == mode) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = AmberPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Local Privacy Guarantee Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF132018)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "100% Local & Private Sandbox",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4ADE80),
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "All photos and videos taken in this app remain strictly on your device. Zero telemetry, zero analytics, zero external servers. Your media is completely private to you.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Automatic Cloud Backup Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Cloud Backup", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Auto-upload new photos & videos to your own AWS S3 bucket", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.autoCloudBackup,
                            onCheckedChange = { enabled ->
                                onUpdateSettings { it.copy(autoCloudBackup = enabled) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = AmberPrimary
                            )
                        )
                    }
                }

                3 -> {
                    // TAB 4: Developer Mode & AWS S3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Developer Mode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Enables direct AWS S3 cloud storage configuration",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.developerModeEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateSettings { it.copy(developerModeEnabled = enabled) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = AmberPrimary
                            ),
                            modifier = Modifier.testTag("developer_mode_switch")
                        )
                    }

                    if (settings.developerModeEnabled) {
                        // Offline Privacy Reassurance
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Reassurance: Your AWS IAM Access Key ID and Secret Access Key remain 100% offline on your device in secure local storage. They are never transmitted to any third-party service, and are strictly used for direct S3 REST API calls from your phone.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        // Form Inputs
                        OutlinedTextField(
                            value = settings.awsAccessKeyId,
                            onValueChange = { key ->
                                onUpdateSettings { it.copy(awsAccessKeyId = key) }
                            },
                            label = { Text("AWS IAM User Access Key ID") },
                            placeholder = { Text("e.g. AKIAIOSFODNN7EXAMPLE") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberPrimary,
                                focusedLabelColor = AmberPrimary
                            )
                        )

                        OutlinedTextField(
                            value = settings.awsSecretAccessKey,
                            onValueChange = { secret ->
                                onUpdateSettings { it.copy(awsSecretAccessKey = secret) }
                            },
                            label = { Text("AWS Secret Access Key (with s3 upload access)") },
                            placeholder = { Text("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY") },
                            singleLine = true,
                            visualTransformation = if (showSecretKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showSecretKey = !showSecretKey }) {
                                    Icon(
                                        imageVector = if (showSecretKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle key visibility"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberPrimary,
                                focusedLabelColor = AmberPrimary
                            )
                        )

                        OutlinedTextField(
                            value = settings.awsBucketName,
                            onValueChange = { b ->
                                onUpdateSettings { it.copy(awsBucketName = b) }
                            },
                            label = { Text("AWS S3 Bucket Name") },
                            placeholder = { Text("e.g. my-travel-instant-captures") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberPrimary,
                                focusedLabelColor = AmberPrimary
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = settings.awsRegion,
                                onValueChange = { r ->
                                    onUpdateSettings { it.copy(awsRegion = r) }
                                },
                                label = { Text("AWS Region") },
                                placeholder = { Text("us-east-1") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AmberPrimary,
                                    focusedLabelColor = AmberPrimary
                                )
                            )

                            OutlinedTextField(
                                value = settings.awsPrefix,
                                onValueChange = { p ->
                                    onUpdateSettings { it.copy(awsPrefix = p) }
                                },
                                label = { Text("S3 Folder Prefix") },
                                placeholder = { Text("travel-captures/") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AmberPrimary,
                                    focusedLabelColor = AmberPrimary
                                )
                            )
                        }

                        // Test Connection Button
                        Button(
                            onClick = {
                                isTestingConnection = true
                                testResultText = null
                                onTestAwsConnection { success, message ->
                                    isTestingConnection = false
                                    testResultText = Pair(success, message)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Testing AWS S3 Connection...")
                            } else {
                                Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test AWS S3 Upload Connectivity")
                            }
                        }

                        testResultText?.let { (success, msg) ->
                            Text(
                                text = msg,
                                color = if (success) Color(0xFF4ADE80) else RecordingRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Sync All Now Button
                        OutlinedButton(
                            onClick = onSyncAllPending,
                            enabled = !isBackingUp,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AmberPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(backupStatusMessage ?: "Backing up...", color = AmberPrimary)
                            } else {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sync All Pending Captures Now", color = AmberPrimary)
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Toggle Developer Mode above to set up automatic or manual AWS S3 cloud backups using your IAM user credentials.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
