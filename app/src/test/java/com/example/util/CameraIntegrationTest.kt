package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CameraIntegrationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testCameraTempUriCreation() {
        val pair = ImageUtils.createCameraTempUri(context)
        assertNotNull("Camera temp URI and File must be created successfully", pair)
        assertNotNull("Temp URI must not be null", pair?.first)
        assertTrue("Temp File must exist in cache directory", pair?.second?.exists() == true)
        assertTrue("File path must contain camera_photos", pair?.second?.absolutePath?.contains("camera_photos") == true)
    }

    @Test
    fun testBitmapToCameraTempUri() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val pair = ImageUtils.saveBitmapToCameraTempUri(context, bitmap)
        assertNotNull("Saved bitmap pair must not be null", pair)
        assertNotNull("URI must be generated", pair?.first)
        assertTrue("File must exist on disk", pair?.second?.exists() == true)
        assertTrue("File must have positive size after compression", (pair?.second?.length() ?: 0L) > 0)
    }

    @Test
    fun testCapturePhotoWithUriContract() {
        val contract = CapturePhotoWithUriContract()
        val tempPair = ImageUtils.createCameraTempUri(context)
        assertNotNull(tempPair)
        val testUri = tempPair!!.first

        val intent = contract.createIntent(context, testUri)
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, intent.action)
        assertEquals(testUri, intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT))
        assertNotNull("ClipData must be set for FileProvider URI grants", intent.clipData)
        assertEquals(testUri, intent.clipData?.getItemAt(0)?.uri)
        assertTrue(
            "Intent flags must include FLAG_GRANT_WRITE_URI_PERMISSION",
            (intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0
        )
        assertTrue(
            "Intent flags must include FLAG_GRANT_READ_URI_PERMISSION",
            (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0
        )
    }

    @Test
    fun testGrantUriPermissionsDoesNotCrash() {
        val pair = ImageUtils.createCameraTempUri(context)
        assertNotNull(pair)
        // Should execute smoothly without throwing exceptions
        ImageUtils.grantUriPermissionsForCamera(context, pair!!.first)
    }

    @Test
    fun testCompressAndResizeWithSampleBitmap() {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val pair = ImageUtils.saveBitmapToCameraTempUri(context, bitmap)
        assertNotNull(pair)

        val compressedBytes = ImageUtils.compressAndResize(context, pair!!.first, maxDimension = 150, quality = 85)
        assertNotNull("Compressed bytes should not be null", compressedBytes)
        assertTrue("Compressed image bytes should be non-empty", compressedBytes!!.isNotEmpty())
    }

    @Test
    fun testCameraAvailability() {
        val isAvailable = ImageUtils.isCameraAvailable(context)
        // Check runs without throwing
        assertTrue(isAvailable || !isAvailable)
    }

    @Test
    fun testPersistAndCompressProductImage() {
        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val pair = ImageUtils.saveBitmapToCameraTempUri(context, bitmap)
        assertNotNull(pair)

        // Persist immediately as ShopkeeperDashboardScreen now does upon gallery/camera selection
        val persistedUri = ImageUtils.persistImageToAppStorage(
            context = context,
            sourceUri = pair!!.first,
            folderName = "product_photos/test_shop",
            fileNamePrefix = "prod_test"
        )
        assertNotNull("Persisted URI should not be null", persistedUri)

        // Subsequent compressAndResize (as called by uploadProductPhoto) must succeed reliably
        val compressedBytes = ImageUtils.compressAndResize(context, persistedUri!!, maxDimension = 1024, quality = 82)
        assertNotNull("Compressed bytes from persisted URI must not be null", compressedBytes)
        assertTrue("Compressed bytes from persisted URI must not be empty", compressedBytes!!.isNotEmpty())
    }
}
