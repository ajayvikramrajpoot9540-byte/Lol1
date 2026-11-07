package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.Coil
import coil.request.ImageRequest
import coil.size.Size
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
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
     * Reads raw bytes from any Uri (file://, content://, FileProvider, or direct path) in a single pass.
     * Reading once avoids failing on single-consumption PhotoPicker / CloudMediaProvider streams.
     */
    fun readRawBytesFromUri(context: Context, uri: Uri): ByteArray? {
        // 1. Check direct file:// or raw path
        try {
            if (uri.scheme == "file" || uri.scheme.isNullOrBlank()) {
                val path = uri.path ?: uri.toString()
                val file = File(path)
                if (file.exists() && file.length() > 0) {
                    return file.readBytes()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct file read failed for $uri: ${e.message}")
        }

        // 2. Check if it is an internal FileProvider URI pointing to cacheDir or filesDir
        try {
            val lastSegment = uri.lastPathSegment
            if (!lastSegment.isNullOrBlank()) {
                val candidateDirs = listOf(
                    File(context.cacheDir, "camera_photos"),
                    context.cacheDir,
                    context.filesDir
                )
                for (dir in candidateDirs) {
                    val f = File(dir, lastSegment)
                    if (f.exists() && f.length() > 0) {
                        return f.readBytes()
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Standard ContentResolver InputStream (single pass)
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.isNotEmpty()) return bytes
            }
        } catch (e: Exception) {
            Log.w(TAG, "openInputStream failed for $uri: ${e.message}")
        }

        // 4. Fallback: ParcelFileDescriptor
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { fis ->
                    val bytes = fis.readBytes()
                    if (bytes.isNotEmpty()) return bytes
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "openFileDescriptor failed for $uri: ${e.message}")
        }

        return null
    }

    /**
     * Verifies that the given URI points to a valid, readable, non-empty image file.
     */
    fun isUriValidAndNotEmpty(context: Context, uri: Uri?): Boolean {
        if (uri == null) return false
        return try {
            val raw = readRawBytesFromUri(context, uri)
            raw != null && raw.isNotEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "URI validation failed for $uri: ${e.message}")
            false
        }
    }

    /**
     * Resizes and compresses an image from a URI to JPEG format.
     * Uses a single-read in-memory byte buffer + ARGB_8888 + ImageDecoder + FileDescriptor fallbacks
     * so modern Android Gallery / PhotoPicker / HEIC / WebP / PNG / Camera images never fail.
     */
    fun compressAndResize(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1024,
        quality: Int = 80
    ): ByteArray? {
        val rawBytes = readRawBytesFromUri(context, uri)

        // Attempt 1: Decode from in-memory rawBytes using BitmapFactory (ARGB_8888)
        if (rawBytes != null && rawBytes.isNotEmpty()) {
            try {
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)
                val originalWidth = boundsOptions.outWidth
                val originalHeight = boundsOptions.outHeight

                if (originalWidth > 0 && originalHeight > 0) {
                    var sampleSize = 1
                    var maxDim = max(originalWidth, originalHeight)
                    while (maxDim / 2 >= maxDimension) {
                        sampleSize *= 2
                        maxDim /= 2
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val decodedBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                    if (decodedBitmap != null) {
                        val orientationDegrees = getExifRotationDegrees(ByteArrayInputStream(rawBytes))
                        val compressed = transformAndCompressBitmap(decodedBitmap, orientationDegrees, maxDimension, quality)
                        if (compressed != null && compressed.isNotEmpty()) {
                            return compressed
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "BitmapFactory byte decode fallback triggered: ${e.message}")
            }
        }

        // Attempt 2: Use Android ImageDecoder (API 28+) for HEIC, AVIF, WebP, or special ContentProviders
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = when {
                    rawBytes != null && rawBytes.isNotEmpty() -> ImageDecoder.createSource(ByteBuffer.wrap(rawBytes))
                    uri.scheme == "file" && !uri.path.isNullOrBlank() -> ImageDecoder.createSource(File(uri.path!!))
                    else -> ImageDecoder.createSource(context.contentResolver, uri)
                }
                val decodedBitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                    val w = info.size.width
                    val h = info.size.height
                    val currentMax = max(w, h)
                    if (currentMax > maxDimension) {
                        val scale = maxDimension.toFloat() / currentMax
                        decoder.setTargetSize(
                            (w * scale).toInt().coerceAtLeast(1),
                            (h * scale).toInt().coerceAtLeast(1)
                        )
                    }
                }
                val compressed = compressBitmapToJpegBytes(decodedBitmap, quality)
                decodedBitmap.recycle()
                if (compressed != null && compressed.isNotEmpty()) {
                    return compressed
                }
            } catch (e: Exception) {
                Log.w(TAG, "ImageDecoder fallback failed: ${e.message}")
            }
        }

        // Attempt 3: Decode via ParcelFileDescriptor
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val decodedBitmap = BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)
                if (decodedBitmap != null) {
                    val compressed = transformAndCompressBitmap(decodedBitmap, 0f, maxDimension, quality)
                    if (compressed != null && compressed.isNotEmpty()) {
                        return compressed
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "FileDescriptor decode fallback failed: ${e.message}")
        }

        // Attempt 4: If we have rawBytes from the file/stream, return rawBytes directly rather than failing!
        if (rawBytes != null && rawBytes.isNotEmpty()) {
            Log.d(TAG, "Returning raw image bytes (${rawBytes.size} bytes) as direct pass-through")
            return rawBytes
        }

        return null
    }

    /**
     * Suspend version of [compressAndResize] that also queries Coil's ImageLoader cache/decoder
     * as an additional fallback (e.g. if a one-time PhotoPicker URI stream was already consumed by AsyncImage).
     */
    suspend fun compressAndResizeWithCoilFallback(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1024,
        quality: Int = 80
    ): ByteArray? {
        val standardBytes = compressAndResize(context, uri, maxDimension, quality)
        if (standardBytes != null && standardBytes.isNotEmpty()) {
            return standardBytes
        }

        return try {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .size(Size(maxDimension, maxDimension))
                .build()
            val result = Coil.imageLoader(context).execute(request)
            val drawable = result.drawable
            val bitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                null -> null
                else -> {
                    val w = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(maxDimension)
                    val h = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(maxDimension)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bmp
                }
            }
            if (bitmap != null) {
                compressBitmapToJpegBytes(bitmap, quality)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Coil fallback image extraction failed: ${e.message}", e)
            null
        }
    }

    private fun transformAndCompressBitmap(
        decodedBitmap: Bitmap,
        orientationDegrees: Float,
        maxDimension: Int,
        quality: Int
    ): ByteArray? {
        return try {
            val currentWidth = decodedBitmap.width
            val currentHeight = decodedBitmap.height
            if (currentWidth <= 0 || currentHeight <= 0) return null

            val currentMax = max(currentWidth, currentHeight)
            val matrix = Matrix()
            if (orientationDegrees != 0f) {
                matrix.postRotate(orientationDegrees)
            }

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

            val bytes = compressBitmapToJpegBytes(scaledBitmap, quality)
            if (scaledBitmap != decodedBitmap) {
                scaledBitmap.recycle()
            }
            decodedBitmap.recycle()
            bytes
        } catch (e: Exception) {
            Log.e(TAG, "Error transforming bitmap: ${e.message}", e)
            null
        }
    }

    /**
     * Draws bitmap onto an opaque white surface before JPEG compression so transparent PNG/WebP
     * images don't get black backgrounds or fail encoding.
     */
    private fun compressBitmapToJpegBytes(bitmap: Bitmap, quality: Int): ByteArray? {
        return try {
            val workingBitmap = if (bitmap.hasAlpha() || bitmap.config != Bitmap.Config.ARGB_8888) {
                val argb = Bitmap.createBitmap(
                    bitmap.width.coerceAtLeast(1),
                    bitmap.height.coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(argb)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                argb
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            val ok = workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            if (!ok || outputStream.size() == 0) {
                outputStream.reset()
                workingBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            if (workingBitmap != bitmap) {
                workingBitmap.recycle()
            }
            val bytes = outputStream.toByteArray()
            if (bytes.isNotEmpty()) bytes else null
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing bitmap to JPEG: ${e.message}", e)
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
            val safeShopId = shopId.ifBlank { "default_shop" }.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val shopDir = File(context.filesDir, "product_photos/$safeShopId").apply {
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
     * This ensures the app maintains full access even after Activity restarts or PhotoPicker permission expiry.
     */
    fun persistImageToAppStorage(
        context: Context,
        sourceUri: Uri,
        folderName: String = "user_photos",
        fileNamePrefix: String = "profile"
    ): Uri? {
        return try {
            // If sourceUri is already a file in filesDir that exists and is non-empty, return it directly
            if (sourceUri.scheme == "file" && !sourceUri.path.isNullOrBlank()) {
                val existingFile = File(sourceUri.path!!)
                if (existingFile.exists() && existingFile.length() > 0 &&
                    existingFile.absolutePath.startsWith(context.filesDir.absolutePath)
                ) {
                    return sourceUri
                }
            }

            val targetDir = File(context.filesDir, folderName).apply {
                if (!exists()) mkdirs()
            }
            val safePrefix = fileNamePrefix.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val fileName = "${safePrefix}_${System.currentTimeMillis()}.jpg"
            val destFile = File(targetDir, fileName)

            // Compress and resize to standard dimensions first (single stream read inside)
            val compressedBytes = compressAndResize(context, sourceUri, maxDimension = 1024, quality = 85)
            if (compressedBytes != null && compressedBytes.isNotEmpty()) {
                destFile.writeBytes(compressedBytes)
            } else {
                // Direct raw byte copy fallback
                val rawBytes = readRawBytesFromUri(context, sourceUri)
                if (rawBytes != null && rawBytes.isNotEmpty()) {
                    destFile.writeBytes(rawBytes)
                } else {
                    return null
                }
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
            isUriValidAndNotEmpty(context, uri)
        } catch (e: Exception) {
            false
        }
    }
}
