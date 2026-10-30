package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlin.math.max

object ImageUtils {
    private const val TAG = "ImageUtils"

    /**
     * Resolves a FileProvider content URI with fallback authorities for robustness.
     */
    fun getFileProviderUri(context: Context, file: File): Uri? {
        val authoritiesToTry = listOf(
            "${context.packageName}.fileprovider",
            "com.aistudio.apnabaz.mhbaz.fileprovider",
            "com.example.fileprovider"
        )
        for (authority in authoritiesToTry) {
            try {
                return FileProvider.getUriForFile(context, authority, file)
            } catch (e: Exception) {
                // Try next authority
            }
        }
        return try {
            Uri.fromFile(file)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Creates a temporary file in the app cache and returns a content URI via FileProvider
     * for launching the camera.
     */
    fun createCameraTempUri(context: Context): Pair<Uri, File>? {
        return try {
            val cameraDir = File(context.cacheDir, "camera_photos").apply {
                if (!exists()) mkdirs()
            }
            val tempFile = File.createTempFile("camera_img_${System.currentTimeMillis()}_", ".jpg", cameraDir)
            val uri = getFileProviderUri(context, tempFile)
            if (uri != null) {
                Pair(uri, tempFile)
            } else {
                Log.e(TAG, "Failed to generate FileProvider URI for camera temp file")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating camera temp file: ${e.message}", e)
            null
        }
    }

    /**
     * Saves a preview bitmap into an app cache temp file and returns its FileProvider content URI.
     */
    fun saveBitmapToCameraTempUri(context: Context, bitmap: Bitmap): Pair<Uri, File>? {
        return try {
            val cameraDir = File(context.cacheDir, "camera_photos").apply {
                if (!exists()) mkdirs()
            }
            val tempFile = File.createTempFile("camera_preview_${System.currentTimeMillis()}_", ".jpg", cameraDir)
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
            }
            val uri = getFileProviderUri(context, tempFile)
            if (uri != null) {
                Pair(uri, tempFile)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to camera temp URI: ${e.message}", e)
            null
        }
    }

    /**
     * Checks if the device has a camera or can handle the image capture intent.
     */
    fun isCameraAvailable(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val hasFeature = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val canResolve = intent.resolveActivity(pm) != null ||
                pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
            hasFeature || canResolve
        } catch (e: Exception) {
            Log.w(TAG, "Error checking camera availability: ${e.message}")
            true
        }
    }

    /**
     * Checks if camera runtime permission is granted.
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Grants read and write permissions on the given URI to any camera activity on the device.
     */
    fun grantUriPermissionsForCamera(context: Context, uri: Uri) {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resInfoList = context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    packageName,
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not grant URI permissions to camera apps: ${e.message}")
        }
    }

    /**
     * Opens the device Application Details Settings screen so the user can enable Camera permission.
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app settings: ${e.message}", e)
        }
    }

    /**
     * Verifies that the given URI points to a valid, readable, non-empty image file.
     */
    fun isUriValidAndNotEmpty(context: Context, uri: Uri?): Boolean {
        if (uri == null) return false
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.available() > 0 || stream.read() != -1
            } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "URI validation failed for $uri: ${e.message}")
            false
        }
    }

    /**
     * Resizes and compresses an image from a content URI to JPEG format.
     * Downscales the image so its max dimension is within [maxDimension] (default 1024px)
     * and compresses with JPEG [quality] (default 80%).
     * Handles EXIF orientation to ensure photos taken in portrait/landscape stay upright.
     */
    fun compressAndResize(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1024,
        quality: Int = 80
    ): ByteArray? {
        return try {
            val contentResolver = context.contentResolver

            // 1. Decode bounds only to compute inSampleSize
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            } ?: return null

            val originalWidth = boundsOptions.outWidth
            val originalHeight = boundsOptions.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) {
                return null
            }

            var sampleSize = 1
            var maxDim = max(originalWidth, originalHeight)
            while (maxDim / 2 >= maxDimension) {
                sampleSize *= 2
                maxDim /= 2
            }

            // 2. Decode the bitmap using inSampleSize
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Memory-efficient for photos
            }

            val decodedBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 3. Read EXIF orientation
            val orientationDegrees = getExifRotationDegrees(contentResolver.openInputStream(uri))

            // 4. Transform: Rotate and Scale if needed
            val matrix = Matrix()
            if (orientationDegrees != 0f) {
                matrix.postRotate(orientationDegrees)
            }

            val currentWidth = decodedBitmap.width
            val currentHeight = decodedBitmap.height
            val currentMax = max(currentWidth, currentHeight)

            val scaledBitmap = if (currentMax > maxDimension || orientationDegrees != 0f) {
                val scale = if (currentMax > maxDimension) maxDimension.toFloat() / currentMax else 1f
                matrix.postScale(scale, scale)
                Bitmap.createBitmap(
                    decodedBitmap,
                    0,
                    0,
                    currentWidth,
                    currentHeight,
                    matrix,
                    true
                )
            } else {
                decodedBitmap
            }

            // 5. Compress to JPEG ByteArray
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()

            if (scaledBitmap != decodedBitmap) {
                scaledBitmap.recycle()
            }
            decodedBitmap.recycle()

            bytes
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image: ${e.message}", e)
            null
        }
    }

    /**
     * Reads EXIF rotation from an input stream.
     */
    private fun getExifRotationDegrees(inputStream: InputStream?): Float {
        if (inputStream == null) return 0f
        return try {
            val exif = ExifInterface(inputStream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not determine EXIF orientation: ${e.message}")
            0f
        } finally {
            try {
                inputStream.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Saves compressed image bytes into local persistent filesDir as fallback or cache.
     */
    fun saveLocally(
        context: Context,
        shopId: String,
        fileName: String,
        bytes: ByteArray
    ): Uri? {
        return try {
            val shopDir = File(context.filesDir, "product_photos/$shopId").apply {
                if (!exists()) mkdirs()
            }
            val destinationFile = File(shopDir, fileName)
            FileOutputStream(destinationFile).use { out ->
                out.write(bytes)
                out.flush()
            }
            Uri.fromFile(destinationFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image locally: ${e.message}", e)
            null
        }
    }

    /**
     * Copies an image from any Uri (content://, file://, etc.) to internal permanent app storage
     * (context.filesDir/folderName/fileNamePrefix_timestamp.jpg).
     * This ensures the app maintains full access even after Activity restarts or cache cleaning.
     */
    fun persistImageToAppStorage(
        context: Context,
        sourceUri: Uri,
        folderName: String = "user_photos",
        fileNamePrefix: String = "profile"
    ): Uri? {
        return try {
            val targetDir = File(context.filesDir, folderName).apply {
                if (!exists()) mkdirs()
            }
            val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.jpg"
            val destFile = File(targetDir, fileName)

            // Compress and resize to standard dimensions first
            val compressedBytes = compressAndResize(context, sourceUri, maxDimension = 1024, quality = 85)
            if (compressedBytes != null && compressedBytes.isNotEmpty()) {
                destFile.writeBytes(compressedBytes)
            } else {
                // Fallback stream copy
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                } ?: return null
            }

            if (destFile.exists() && destFile.length() > 0) {
                Log.d(TAG, "Persisted image to internal app storage: ${destFile.absolutePath} (${destFile.length()} bytes)")
                Uri.fromFile(destFile)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist image to app storage: ${e.message}", e)
            null
        }
    }

    /**
     * Verifies if an image source is valid, non-empty, and accessible.
     */
    fun isValidImageSource(context: Context, imageSource: String?): Boolean {
        if (imageSource.isNullOrBlank()) return false
        if (imageSource.startsWith("http://") || imageSource.startsWith("https://")) return true
        return try {
            val uri = Uri.parse(imageSource)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                file.exists() && file.length() > 0
            } else if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.available() > 0 || stream.read() != -1
                } ?: false
            } else {
                val directFile = File(imageSource)
                directFile.exists() && directFile.length() > 0
            }
        } catch (e: Exception) {
            false
        }
    }
}
