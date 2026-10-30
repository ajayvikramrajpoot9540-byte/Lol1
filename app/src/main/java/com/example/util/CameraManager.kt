package com.example.util

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ui.theme.GreenPrimary

/**
 * ActivityResultContract for full-resolution camera captures.
 * Sets ClipData and explicit URI grant flags to ensure camera applications
 * can reliably write to the provided FileProvider content URI without security exceptions.
 */
class CapturePhotoWithUriContract : ActivityResultContract<Uri, Boolean>() {
    override fun createIntent(context: Context, input: Uri): Intent {
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, input)
            clipData = ClipData.newRawUri("FlashDropPhoto", input)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}

/**
 * Holds state and dispatch trigger for launching camera operations.
 */
class CameraLauncherState(
    val launchCamera: () -> Unit,
    val isLaunching: Boolean
)

/**
 * Unified Jetpack Compose camera manager that handles:
 * 1. Camera hardware & application presence detection.
 * 2. Runtime camera permissions (graceful denial, rationale, and Settings fallback).
 * 3. FileProvider content URI creation with read/write grant permissions.
 * 4. Full-resolution photo capture with preview fallback.
 * 5. Debouncing against rapid button clicks.
 */
@Composable
fun rememberCameraManager(
    onImageCaptured: (Uri) -> Unit,
    onError: (String) -> Unit = {}
): CameraLauncherState {
    val context = LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isLaunching by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionDeniedPermanently by remember { mutableStateOf(false) }

    // Primary launcher: TakePicture with FileProvider URI
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = CapturePhotoWithUriContract()
    ) { success: Boolean ->
        isLaunching = false
        Log.d("FlashDropCamera", "Camera TakePicture result: success=$success, uri=$tempCameraUri")
        if (success && tempCameraUri != null) {
            onImageCaptured(tempCameraUri!!)
        } else {
            Log.d("FlashDropCamera", "Camera capture cancelled or failed to produce output")
        }
    }

    // Fallback launcher: TakePicturePreview (returns Bitmap if file URI fails)
    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        isLaunching = false
        if (bitmap != null) {
            val pair = ImageUtils.saveBitmapToCameraTempUri(context, bitmap)
            if (pair != null) {
                Log.d("FlashDropCamera", "Preview bitmap saved to: ${pair.first}")
                onImageCaptured(pair.first)
            } else {
                onError("Failed to save captured photo.")
            }
        } else {
            Log.d("FlashDropCamera", "Camera preview cancelled")
        }
    }

    fun startCameraIntent() {
        if (!ImageUtils.isCameraAvailable(context)) {
            isLaunching = false
            onError("No camera application found on this device.")
            return
        }

        try {
            val pair = ImageUtils.createCameraTempUri(context)
            if (pair != null) {
                tempCameraUri = pair.first
                ImageUtils.grantUriPermissionsForCamera(context, pair.first)
                Log.d("FlashDropCamera", "Launching full-res camera with URI: ${pair.first}")
                takePictureLauncher.launch(pair.first)
            } else {
                Log.w("FlashDropCamera", "Could not create temp URI, using preview fallback")
                takePicturePreviewLauncher.launch(null)
            }
        } catch (e: Exception) {
            isLaunching = false
            Log.e("FlashDropCamera", "Exception launching camera: ${e.message}", e)
            try {
                takePicturePreviewLauncher.launch(null)
            } catch (e2: Exception) {
                onError("Unable to launch camera: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    // Permission launcher for CAMERA runtime permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("FlashDropCamera", "Camera permission granted by user")
            startCameraIntent()
        } else {
            isLaunching = false
            Log.w("FlashDropCamera", "Camera permission denied by user")
            val activity = context as? Activity
            val canShowRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
            permissionDeniedPermanently = !canShowRationale
            showPermissionDialog = true
            onError("Camera permission is required to capture photos.")
        }
    }

    val launchCamera: () -> Unit = {
        if (!isLaunching) {
            isLaunching = true
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                startCameraIntent()
            } else {
                val activity = context as? Activity
                if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) && permissionDeniedPermanently) {
                    isLaunching = false
                    showPermissionDialog = true
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showPermissionDialog = false
                isLaunching = false
            },
            icon = {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = GreenPrimary)
            },
            title = {
                Text("Camera Permission Required")
            },
            text = {
                Text(
                    if (permissionDeniedPermanently) {
                        "FlashDrop needs camera permission to take photos. Camera access was previously denied. Please enable camera access in Android Settings to continue."
                    } else {
                        "FlashDrop needs access to your device camera so you can take product and profile photos. Please grant camera permission."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        isLaunching = false
                        if (permissionDeniedPermanently) {
                            ImageUtils.openAppSettings(context)
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text(if (permissionDeniedPermanently) "Open Settings" else "Grant Permission")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showPermissionDialog = false
                        isLaunching = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    return CameraLauncherState(
        launchCamera = launchCamera,
        isLaunching = isLaunching
    )
}
