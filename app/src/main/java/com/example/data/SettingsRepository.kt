package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VideoResolution(val label: String, val width: Int, val height: Int) {
    UHD_4K("4K UHD (2160p)", 3840, 2160),
    FHD_1080P("Full HD (1080p)", 1920, 1080),
    HD_720P("HD (720p)", 1280, 720),
    SD_480P("SD (480p)", 854, 480)
}

enum class VideoFrameRate(val label: String, val fps: Int) {
    FPS_24("24 fps (Cinematic)", 24),
    FPS_30("30 fps (Standard)", 30),
    FPS_60("60 fps (Smooth)", 60)
}

enum class StorageDestination(val label: String, val description: String) {
    INTERNAL_PRIVATE("Private App Storage", "Safest for travellers: zero external access"),
    PICTURES_SHARED("Pictures / InstantCam", "Visible in system gallery & file managers"),
    DCIM_TRAVEL("DCIM / TravelMoments", "Native camera roll integration")
}

enum class AppThemeMode(val label: String) {
    SYSTEM("System Default"),
    DARK("Dark Minimal"),
    OLED_BLACK("OLED True Black (Night Travel)"),
    LIGHT("Daylight High Contrast")
}

enum class CameraFacing(val label: String) {
    BACK("Back Camera"),
    FRONT("Front Selfie Camera")
}

enum class AudioSource(val label: String) {
    AUTO("Automatic / Smart Selection"),
    INTERNAL("Built-in Device Mic"),
    EXTERNAL("External / Headset / Bluetooth Mic"),
    CAMCORDER("Camcorder Directional Mic")
}

data class CameraSettings(
    val videoResolution: VideoResolution = VideoResolution.FHD_1080P,
    val videoFrameRate: VideoFrameRate = VideoFrameRate.FPS_30,
    val storageDestination: StorageDestination = StorageDestination.INTERNAL_PRIVATE,
    val autoCloudBackup: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val volumeKeyShortcut: Boolean = true,
    val doubleTapToFlip: Boolean = true,
    val gesturesEnabled: Boolean = true,
    val audioSource: AudioSource = AudioSource.AUTO,
    val audioGainDb: Float = 0f, // -12dB to +18dB
    val isAudioMuted: Boolean = false,
    val defaultCamera: CameraFacing = CameraFacing.BACK,
    val developerModeEnabled: Boolean = false,
    val awsAccessKeyId: String = "",
    val awsSecretAccessKey: String = "",
    val awsBucketName: String = "",
    val awsRegion: String = "us-east-1",
    val awsPrefix: String = "travel-captures/"
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("instant_camera_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<CameraSettings> = _settings.asStateFlow()

    private fun loadSettings(): CameraSettings {
        return CameraSettings(
            videoResolution = VideoResolution.entries.getOrElse(
                prefs.getInt("video_res_idx", VideoResolution.FHD_1080P.ordinal)
            ) { VideoResolution.FHD_1080P },
            videoFrameRate = VideoFrameRate.entries.getOrElse(
                prefs.getInt("video_fps_idx", VideoFrameRate.FPS_30.ordinal)
            ) { VideoFrameRate.FPS_30 },
            storageDestination = StorageDestination.entries.getOrElse(
                prefs.getInt("storage_dest_idx", StorageDestination.INTERNAL_PRIVATE.ordinal)
            ) { StorageDestination.INTERNAL_PRIVATE },
            autoCloudBackup = prefs.getBoolean("auto_cloud_backup", false),
            themeMode = AppThemeMode.entries.getOrElse(
                prefs.getInt("theme_mode_idx", AppThemeMode.DARK.ordinal)
            ) { AppThemeMode.DARK },
            volumeKeyShortcut = prefs.getBoolean("volume_key_shortcut", true),
            doubleTapToFlip = prefs.getBoolean("double_tap_to_flip", true),
            gesturesEnabled = prefs.getBoolean("gestures_enabled", true),
            audioSource = AudioSource.entries.getOrElse(
                prefs.getInt("audio_source_idx", AudioSource.AUTO.ordinal)
            ) { AudioSource.AUTO },
            audioGainDb = prefs.getFloat("audio_gain_db", 0f),
            isAudioMuted = prefs.getBoolean("is_audio_muted", false),
            defaultCamera = CameraFacing.entries.getOrElse(
                prefs.getInt("default_camera_idx", CameraFacing.BACK.ordinal)
            ) { CameraFacing.BACK },
            developerModeEnabled = prefs.getBoolean("dev_mode_enabled", false),
            awsAccessKeyId = prefs.getString("aws_access_key", "") ?: "",
            awsSecretAccessKey = prefs.getString("aws_secret_key", "") ?: "",
            awsBucketName = prefs.getString("aws_bucket", "") ?: "",
            awsRegion = prefs.getString("aws_region", "us-east-1") ?: "us-east-1",
            awsPrefix = prefs.getString("aws_prefix", "travel-captures/") ?: "travel-captures/"
        )
    }

    fun updateSettings(transform: (CameraSettings) -> CameraSettings) {
        val newSettings = transform(_settings.value)
        _settings.value = newSettings
        prefs.edit().apply {
            putInt("video_res_idx", newSettings.videoResolution.ordinal)
            putInt("video_fps_idx", newSettings.videoFrameRate.ordinal)
            putInt("storage_dest_idx", newSettings.storageDestination.ordinal)
            putBoolean("auto_cloud_backup", newSettings.autoCloudBackup)
            putInt("theme_mode_idx", newSettings.themeMode.ordinal)
            putBoolean("volume_key_shortcut", newSettings.volumeKeyShortcut)
            putBoolean("double_tap_to_flip", newSettings.doubleTapToFlip)
            putBoolean("gestures_enabled", newSettings.gesturesEnabled)
            putInt("audio_source_idx", newSettings.audioSource.ordinal)
            putFloat("audio_gain_db", newSettings.audioGainDb)
            putBoolean("is_audio_muted", newSettings.isAudioMuted)
            putInt("default_camera_idx", newSettings.defaultCamera.ordinal)
            putBoolean("dev_mode_enabled", newSettings.developerModeEnabled)
            putString("aws_access_key", newSettings.awsAccessKeyId)
            putString("aws_secret_key", newSettings.awsSecretAccessKey)
            putString("aws_bucket", newSettings.awsBucketName)
            putString("aws_region", newSettings.awsRegion)
            putString("aws_prefix", newSettings.awsPrefix)
            apply()
        }
    }
}
