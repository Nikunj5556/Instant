package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.BackupStatus
import com.example.data.MediaCapture
import com.example.data.MediaType
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.RecordingRed
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GallerySheet(
    captures: List<MediaCapture>,
    initialCapture: MediaCapture?,
    onClose: () -> Unit,
    onToggleFavorite: (MediaCapture) -> Unit,
    onDeleteCapture: (MediaCapture) -> Unit,
    onUploadToS3: (MediaCapture) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isGridView by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<MediaCapture?>(null) }
    var showHeartBurst by remember { mutableStateOf(false) }

    if (captures.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D0F13))
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = AmberPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No moments captured yet",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap the shutter to take photos or hold for video.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onClose) {
                    Text("Return to Camera", color = AmberPrimary)
                }
            }
        }
        return
    }

    val initialIndex = remember(initialCapture, captures) {
        val found = captures.indexOfFirst { it.id == initialCapture?.id }
        if (found >= 0) found else 0
    }
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { captures.size })

    val currentCapture = captures.getOrNull(pagerState.currentPage) ?: captures.first()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (isGridView) {
            // Grid Overview
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Travel Memories (${captures.size})",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { isGridView = false }) {
                        Icon(
                            imageVector = Icons.Default.ViewCarousel,
                            contentDescription = "Pager View",
                            tint = AmberPrimary
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(captures, key = { it.id }) { capture ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    isGridView = false
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(capture.filePath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (capture.mediaType == MediaType.VIDEO) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            if (capture.isFavorite) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = AmberPrimary,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Fullscreen Gesture Pager Mode
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val capture = captures[page]
                var scale by remember { mutableFloatStateOf(1f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(capture.id) {
                            detectTapGestures(
                                onDoubleTap = {
                                    onToggleFavorite(capture)
                                    showHeartBurst = true
                                },
                                onTap = {
                                    // Toggle controls
                                }
                            )
                        }
                        .pointerInput(capture.id) {
                            detectTransformGestures { _, _, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(capture.filePath))
                            .crossfade(true)
                            .build(),
                        contentDescription = capture.fileName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                    )

                    // Video Play indicator overlay
                    if (capture.mediaType == MediaType.VIDEO) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .border(2.dp, AmberPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }
            }

            // Top Navigation Bar Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to camera",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${pagerState.currentPage + 1} of ${captures.size}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val dateFormatted = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US).format(Date(currentCapture.timestamp))
                    Text(
                        text = dateFormatted,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                IconButton(onClick = { isGridView = true }) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Grid View",
                        tint = Color.White
                    )
                }
            }

            // Bottom Action Bar Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorite Button
                IconButton(onClick = { onToggleFavorite(currentCapture) }) {
                    Icon(
                        imageVector = if (currentCapture.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (currentCapture.isFavorite) AmberPrimary else Color.White
                    )
                }

                // Cloud Backup Status & Manual Sync Button
                IconButton(onClick = { onUploadToS3(currentCapture) }) {
                    when (currentCapture.backupStatus) {
                        BackupStatus.SYNCED -> Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Backed up to S3",
                            tint = Color(0xFF4ADE80)
                        )
                        BackupStatus.UPLOADING -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AmberPrimary,
                            strokeWidth = 2.dp
                        )
                        BackupStatus.FAILED -> Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Backup Failed - Tap to retry",
                            tint = RecordingRed
                        )
                        else -> Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload to S3",
                            tint = Color.White
                        )
                    }
                }

                // Info / Metadata Button
                IconButton(onClick = { showInfoSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Details",
                        tint = Color.White
                    )
                }

                // Share Button
                IconButton(onClick = {
                    try {
                        val file = File(currentCapture.filePath)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = if (currentCapture.mediaType == MediaType.PHOTO) "image/jpeg" else "video/mp4"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Moment"))
                    } catch (e: Exception) {
                        // Fallback
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }

                // Delete Button
                IconButton(onClick = { showDeleteConfirm = currentCapture }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Animated Heart Burst on double-tap
            AnimatedVisibility(
                visible = showHeartBurst,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                LaunchedEffect(showHeartBurst) {
                    kotlinx.coroutines.delay(650)
                    showHeartBurst = false
                }
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier
                        .size(90.dp)
                        .scale(1.2f)
                )
            }
        }

        // Info Metadata Dialog / Sheet
        if (showInfoSheet) {
            AlertDialog(
                onDismissRequest = { showInfoSheet = false },
                title = {
                    Text(
                        text = "Moment Details",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("File: ${currentCapture.fileName}", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                        Text("Type: ${currentCapture.mediaType}", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        Text("Resolution: ${currentCapture.width} x ${currentCapture.height}", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        val sizeKb = currentCapture.sizeBytes / 1024
                        val sizeMb = sizeKb / 1024f
                        val sizeFormatted = if (sizeMb >= 1f) String.format("%.2f MB", sizeMb) else "$sizeKb KB"
                        Text("Size: $sizeFormatted", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        Text("Storage: Private Local Storage", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        Text("Backup Status: ${currentCapture.backupStatus.name}", color = AmberPrimary, fontSize = 13.sp)
                        if (currentCapture.cloudUrl != null) {
                            Text("S3 URL: ${currentCapture.cloudUrl}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoSheet = false }) {
                        Text("Close", color = AmberPrimary)
                    }
                },
                containerColor = Color(0xFF1A1E24)
            )
        }

        // Delete confirmation
        showDeleteConfirm?.let { target ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text("Delete this moment?", color = Color.White) },
                text = { Text("This will permanently delete ${target.fileName} from device storage.", color = Color.White.copy(alpha = 0.8f)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteCapture(target)
                            showDeleteConfirm = null
                        }
                    ) {
                        Text("Delete", color = RecordingRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1A1E24)
            )
        }
    }
}
