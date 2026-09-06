package com.example.ui

import android.app.Application
import android.util.Log
import androidx.camera.video.VideoRecordEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.CameraManager
import com.example.camera.FlashState
import com.example.cloud.AwsS3BackupService
import com.example.data.AppDatabase
import com.example.data.BackupStatus
import com.example.data.CameraFacing
import com.example.data.CameraSettings
import com.example.data.MediaCapture
import com.example.data.MediaType
import com.example.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class CameraUiState(
    val currentFlash: FlashState = FlashState.AUTO,
    val currentLens: CameraFacing = CameraFacing.BACK,
    val isRecordingVideo: Boolean = false,
    val recordingDurationSeconds: Int = 0,
    val isShutterFlashVisible: Boolean = false,
    val isGalleryOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val selectedCapture: MediaCapture? = null,
    val isBackingUp: Boolean = false,
    val backupStatusMessage: String? = null,
    val toastMessage: String? = null,
    val isHardwareBound: Boolean = false,
    val isDeveloperModeOpen: Boolean = false
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val mediaDao = db.mediaDao()
    val settingsRepository = SettingsRepository(application)
    val cameraManager = CameraManager(application)
    private val s3Service = AwsS3BackupService()

    val settings: StateFlow<CameraSettings> = settingsRepository.settings

    val captures: StateFlow<List<MediaCapture>> = mediaDao.getAllCaptures()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(
        CameraUiState(
            currentLens = settingsRepository.settings.value.defaultCamera
        )
    )
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var recordingTimerJob: Job? = null
    private var activeVideoFile: File? = null
    private var recordingStartTime = 0L

    init {
        _uiState.value = _uiState.value.copy(
            currentLens = settings.value.defaultCamera
        )
    }

    fun setHardwareBound(bound: Boolean) {
        _uiState.value = _uiState.value.copy(isHardwareBound = bound)
    }

    fun toggleFlash() {
        val nextFlash = when (_uiState.value.currentFlash) {
            FlashState.AUTO -> FlashState.ON
            FlashState.ON -> FlashState.OFF
            FlashState.OFF -> FlashState.TORCH
            FlashState.TORCH -> FlashState.AUTO
        }
        _uiState.value = _uiState.value.copy(currentFlash = nextFlash)
        cameraManager.setFlashMode(nextFlash)
    }

    fun setFlash(flash: FlashState) {
        _uiState.value = _uiState.value.copy(currentFlash = flash)
        cameraManager.setFlashMode(flash)
    }

    fun flipCamera() {
        val newLens = if (_uiState.value.currentLens == CameraFacing.BACK) {
            CameraFacing.FRONT
        } else {
            CameraFacing.BACK
        }
        _uiState.value = _uiState.value.copy(currentLens = newLens)
    }

    fun triggerShutterPhoto() {
        if (_uiState.value.isRecordingVideo) {
            // If already recording, tap stops recording
            stopVideoRecording()
            return
        }

        // Shutter flash animation
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isShutterFlashVisible = true)
            delay(80)
            _uiState.value = _uiState.value.copy(isShutterFlashVisible = false)
        }

        cameraManager.takePhoto(
            storageDestination = settings.value.storageDestination,
            onSuccess = { file, width, height ->
                viewModelScope.launch(Dispatchers.IO) {
                    val capture = MediaCapture(
                        filePath = file.absolutePath,
                        fileName = file.name,
                        mediaType = MediaType.PHOTO,
                        timestamp = System.currentTimeMillis(),
                        width = width,
                        height = height,
                        sizeBytes = file.length(),
                        backupStatus = if (settings.value.autoCloudBackup) BackupStatus.PENDING else BackupStatus.LOCAL_ONLY
                    )
                    val id = mediaDao.insertCapture(capture)
                    val saved = capture.copy(id = id)

                    if (settings.value.autoCloudBackup) {
                        uploadCaptureToS3(saved)
                    }
                }
            },
            onError = { ex ->
                showToast("Photo capture error: ${ex.message}")
            }
        )
    }

    fun startVideoRecording() {
        if (_uiState.value.isRecordingVideo) return

        recordingStartTime = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(
            isRecordingVideo = true,
            recordingDurationSeconds = 0
        )

        recordingTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    recordingDurationSeconds = _uiState.value.recordingDurationSeconds + 1
                )
            }
        }

        activeVideoFile = cameraManager.startRecording(
            storageDestination = settings.value.storageDestination,
            enableAudio = !settings.value.isAudioMuted,
            audioSource = settings.value.audioSource,
            audioGainDb = settings.value.audioGainDb,
            onEvent = { event ->
                if (event is VideoRecordEvent.Finalize) {
                    onVideoFinalized(activeVideoFile, event.hasError(), event.cause)
                }
            },
            onError = { ex ->
                showToast("Failed to start video recording: ${ex.message}")
                stopVideoRecording()
            }
        )
    }

    fun stopVideoRecording() {
        if (!_uiState.value.isRecordingVideo) return

        recordingTimerJob?.cancel()
        recordingTimerJob = null

        val file = activeVideoFile
        cameraManager.stopRecording(simulatedFile = file) { savedFile ->
            onVideoFinalized(savedFile, false, null)
        }

        _uiState.value = _uiState.value.copy(
            isRecordingVideo = false
        )
    }

    private fun onVideoFinalized(file: File?, hasError: Boolean, cause: Throwable?) {
        val duration = System.currentTimeMillis() - recordingStartTime
        _uiState.value = _uiState.value.copy(
            isRecordingVideo = false,
            recordingDurationSeconds = 0
        )

        if (hasError && cause != null) {
            Log.e("CameraViewModel", "Video finalize error", cause)
        }

        if (file != null && file.exists()) {
            viewModelScope.launch(Dispatchers.IO) {
                val capture = MediaCapture(
                    filePath = file.absolutePath,
                    fileName = file.name,
                    mediaType = MediaType.VIDEO,
                    timestamp = System.currentTimeMillis(),
                    durationMs = duration,
                    width = settings.value.videoResolution.width,
                    height = settings.value.videoResolution.height,
                    sizeBytes = file.length(),
                    backupStatus = if (settings.value.autoCloudBackup) BackupStatus.PENDING else BackupStatus.LOCAL_ONLY
                )
                val id = mediaDao.insertCapture(capture)
                val saved = capture.copy(id = id)

                if (settings.value.autoCloudBackup) {
                    uploadCaptureToS3(saved)
                }
            }
        }
    }

    fun openGallery(selected: MediaCapture? = null) {
        _uiState.value = _uiState.value.copy(
            isGalleryOpen = true,
            selectedCapture = selected ?: captures.value.firstOrNull()
        )
    }

    fun closeGallery() {
        _uiState.value = _uiState.value.copy(
            isGalleryOpen = false,
            selectedCapture = null
        )
    }

    fun selectCapture(capture: MediaCapture) {
        _uiState.value = _uiState.value.copy(selectedCapture = capture)
    }

    fun openSettings() {
        _uiState.value = _uiState.value.copy(isSettingsOpen = true)
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(isSettingsOpen = false)
    }

    fun toggleFavorite(capture: MediaCapture) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = capture.copy(isFavorite = !capture.isFavorite)
            mediaDao.updateCapture(updated)
            if (_uiState.value.selectedCapture?.id == capture.id) {
                _uiState.value = _uiState.value.copy(selectedCapture = updated)
            }
        }
    }

    fun deleteCapture(capture: MediaCapture) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(capture.filePath)
                if (file.exists()) {
                    file.delete()
                }
                mediaDao.deleteCapture(capture)
                if (_uiState.value.selectedCapture?.id == capture.id) {
                    val remaining = captures.value.filter { it.id != capture.id }
                    _uiState.value = _uiState.value.copy(
                        selectedCapture = remaining.firstOrNull()
                    )
                }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error deleting capture", e)
            }
        }
    }

    fun testAwsConnection(onResult: (Boolean, String) -> Unit) {
        val s = settings.value
        if (s.awsAccessKeyId.isBlank() || s.awsSecretAccessKey.isBlank() || s.awsBucketName.isBlank()) {
            onResult(false, "Please provide Access Key, Secret Key, and S3 Bucket name.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBackingUp = true, backupStatusMessage = "Testing S3 connection...")
            val result = s3Service.testConnection(
                accessKey = s.awsAccessKeyId,
                secretKey = s.awsSecretAccessKey,
                bucket = s.awsBucketName,
                region = s.awsRegion
            )
            _uiState.value = _uiState.value.copy(isBackingUp = false, backupStatusMessage = null)
            if (result.isSuccess) {
                onResult(true, result.getOrDefault("Connection Succeeded!"))
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "S3 Connection Failed")
            }
        }
    }

    fun uploadCaptureToS3(capture: MediaCapture) {
        val s = settings.value
        if (s.awsAccessKeyId.isBlank() || s.awsSecretAccessKey.isBlank() || s.awsBucketName.isBlank()) {
            showToast("Cloud backup paused: Configure AWS S3 in Settings")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            mediaDao.updateCapture(capture.copy(backupStatus = BackupStatus.UPLOADING))
            val file = File(capture.filePath)
            val mimeType = if (capture.mediaType == MediaType.PHOTO) "image/jpeg" else "video/mp4"

            val result = s3Service.uploadFile(
                file = file,
                mimeType = mimeType,
                accessKey = s.awsAccessKeyId,
                secretKey = s.awsSecretAccessKey,
                bucket = s.awsBucketName,
                region = s.awsRegion,
                prefix = s.awsPrefix
            )

            if (result.isSuccess) {
                val cloudUrl = result.getOrNull()
                mediaDao.updateCapture(
                    capture.copy(
                        backupStatus = BackupStatus.SYNCED,
                        cloudUrl = cloudUrl
                    )
                )
                showToast("Backed up ${capture.fileName} to S3!")
            } else {
                mediaDao.updateCapture(capture.copy(backupStatus = BackupStatus.FAILED))
                val err = result.exceptionOrNull()?.message ?: "Upload failed"
                showToast("Backup failed: $err")
            }
        }
    }

    fun backupAllPendingCaptures() {
        val s = settings.value
        if (s.awsAccessKeyId.isBlank() || s.awsSecretAccessKey.isBlank() || s.awsBucketName.isBlank()) {
            showToast("Configure AWS credentials in Settings to start backup.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val pending = mediaDao.getPendingBackupCaptures()
            if (pending.isEmpty()) {
                showToast("All captures are already backed up!")
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isBackingUp = true,
                backupStatusMessage = "Backing up ${pending.size} captures..."
            )

            var successCount = 0
            for (item in pending) {
                val file = File(item.filePath)
                if (!file.exists()) continue
                val mimeType = if (item.mediaType == MediaType.PHOTO) "image/jpeg" else "video/mp4"

                val res = s3Service.uploadFile(
                    file = file,
                    mimeType = mimeType,
                    accessKey = s.awsAccessKeyId,
                    secretKey = s.awsSecretAccessKey,
                    bucket = s.awsBucketName,
                    region = s.awsRegion,
                    prefix = s.awsPrefix
                )
                if (res.isSuccess) {
                    successCount++
                    mediaDao.updateCapture(
                        item.copy(
                            backupStatus = BackupStatus.SYNCED,
                            cloudUrl = res.getOrNull()
                        )
                    )
                } else {
                    mediaDao.updateCapture(item.copy(backupStatus = BackupStatus.FAILED))
                }
            }

            _uiState.value = _uiState.value.copy(
                isBackingUp = false,
                backupStatusMessage = null
            )
            showToast("Backup complete: $successCount of ${pending.size} uploaded to S3.")
        }
    }

    fun showToast(msg: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(toastMessage = msg)
            delay(3000)
            if (_uiState.value.toastMessage == msg) {
                _uiState.value = _uiState.value.copy(toastMessage = null)
            }
        }
    }
}
