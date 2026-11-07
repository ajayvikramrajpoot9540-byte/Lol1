package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.Product
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.util.ImageUtils
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "CameraVisualSearch"

/**
 * UI State for Camera Photo Search in the Search Bar.
 */
sealed interface VisualSearchUiState {
    data object Idle : VisualSearchUiState
    data class Loading(val capturedUri: Uri) : VisualSearchUiState
    data class Success(
        val capturedUri: Uri,
        val detectedItemName: String,
        val searchQuery: String,
        val matchedProductIds: List<String>,
        val matchedProductsSortedByLowestPrice: List<Product>,
        val lowestPriceProduct: Product?
    ) : VisualSearchUiState
    data class Error(
        val capturedUri: Uri?,
        val message: String
    ) : VisualSearchUiState
}

/**
 * Engine that uses Firebase AI Logic (`gemini-3.8-flash`) with multimodal image input
 * and structured JSON output (`Schema.obj`) to identify products from a customer's photo
 * and rank available store products with the LEAST RATE / LOWEST PRICE at the top/first.
 */
object CameraVisualSearchEngine {

    private val visualSearchSchema = Schema.obj(
        properties = mapOf(
            "detectedItemName" to Schema.string(
                description = "Short human-readable name of the product seen in the photo (e.g. 'Atta / Wheat Flour', 'Parle-G Biscuits', 'Amul Butter', 'Maggi Noodles', 'Notebook', 'Medicine')"
            ),
            "searchQuery" to Schema.string(
                description = "Best concise search keyword matching the catalog (e.g. 'Atta', 'Biscuit', 'Butter', 'Maggi', 'Salt', 'Pedha', 'Print')"
            ),
            "keywords" to Schema.array(
                items = Schema.string(),
                description = "Additional related product keywords, category names, or brand terms"
            ),
            "matchedProductIds" to Schema.array(
                items = Schema.string(),
                description = "IDs of matching products from the provided store catalog"
            ),
            "category" to Schema.string(
                description = "Closest store category (e.g. 'Groceries', 'Sweets', 'Vegetables', 'Medicine', 'Stationery', 'Printing Press')"
            )
        ),
        optionalProperties = listOf("category")
    )

    private val modelConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = visualSearchSchema
    }

    // Lazy initialization per firebase-ai-logic-android requirements
    private val visionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = modelConfig
        )
    }

    /**
     * Sorts any list of matching products so that:
     * 1. Available (`inStock == true`) products come first.
     * 2. Within available products, the LEAST RATE / LOWEST PRICE (`price` ascending) is at the very top/first!
     */
    fun sortProductsLowestPriceFirst(products: List<Product>): List<Product> {
        return products.sortedWith(
            compareBy<Product> { !it.inStock }
                .thenBy { it.price }
                .thenBy { it.name.lowercase() }
        )
    }

    /**
     * Filters and ranks catalog products matching a text query or visual search result,
     * placing the available product with the LEAST RATE / LOWEST PRICE at the top/first.
     */
    fun filterAndRankByLowestPrice(
        allProducts: List<Product>,
        query: String,
        visualState: VisualSearchUiState = VisualSearchUiState.Idle
    ): List<Product> {
        if (visualState is VisualSearchUiState.Success) {
            if (visualState.matchedProductsSortedByLowestPrice.isNotEmpty()) {
                return visualState.matchedProductsSortedByLowestPrice
            }
            if (query.isBlank()) {
                return emptyList()
            }
        }

        if (query.isBlank()) {
            return allProducts
        }

        val cleanQuery = query.trim().lowercase()
        val queryTokens = cleanQuery.split("\\s+".toRegex()).filter { it.length >= 2 }

        val matched = allProducts.filter { product ->
            val nameLower = product.name.lowercase()
            val catLower = product.category.lowercase()
            val subLower = product.subcategory.lowercase()
            val descLower = product.description.lowercase()

            nameLower.contains(cleanQuery) ||
                catLower.contains(cleanQuery) ||
                subLower.contains(cleanQuery) ||
                descLower.contains(cleanQuery) ||
                (queryTokens.isNotEmpty() && queryTokens.any { token ->
                    nameLower.contains(token) || catLower.contains(token) || subLower.contains(token)
                })
        }

        return sortProductsLowestPriceFirst(matched)
    }

    /**
     * Analyzes a captured product photo using Firebase AI Logic (`gemini-3.8-flash`),
     * matches against the store's product catalog, and returns matching products sorted
     * with the least rate/price product at the top/first.
     */
    suspend fun analyzeProductPhoto(
        context: Context,
        imageUri: Uri,
        catalogProducts: List<Product>
    ): VisualSearchUiState {
        return try {
            val bitmap: Bitmap = withContext(Dispatchers.IO) {
                val compressedBytes = ImageUtils.compressAndResizeWithCoilFallback(
                    context = context,
                    uri = imageUri,
                    maxDimension = 768,
                    quality = 82
                )
                if (compressedBytes != null && compressedBytes.isNotEmpty()) {
                    BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
                } else {
                    context.contentResolver.openInputStream(imageUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
            } ?: return VisualSearchUiState.Error(
                capturedUri = imageUri,
                message = "Could not read the captured photo. Please try taking the photo again."
            )

            val catalogSummary = catalogProducts.joinToString("\n") { p ->
                "- id=\"${p.id}\", name=\"${p.name}\", category=\"${p.category}\", subcategory=\"${p.subcategory}\", price=₹${p.price}, inStock=${p.inStock}"
            }

            val promptText = """
                You are a visual product search assistant for FlashDrop in Mahoba.
                A customer clicked a photo instead of typing in the search bar to check if this product is available.
                
                1. Identify the product shown in the image.
                2. Compare it against the store catalog below and list all matching or equivalent product IDs in `matchedProductIds`.
                3. Provide a short `detectedItemName`, a concise `searchQuery`, and relevant `keywords`.
                
                Store Catalog:
                $catalogSummary
            """.trimIndent()

            val response = withTimeout(60_000L) {
                visionModel.generateContent(
                    content {
                        image(bitmap)
                        text(promptText)
                    }
                )
            }

            val jsonString = response.text
            if (jsonString.isNullOrBlank()) {
                return VisualSearchUiState.Error(
                    capturedUri = imageUri,
                    message = "AI visual search returned an empty response. Please try again."
                )
            }

            val json = JSONObject(jsonString)
            val detectedItemName = json.optString("detectedItemName", "Scanned Product").trim()
            val searchQuery = json.optString("searchQuery", detectedItemName).trim()
            val category = json.optString("category", "").trim()

            val keywordsList = mutableListOf<String>()
            json.optJSONArray("keywords")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val kw = arr.optString(i, "").trim().lowercase()
                    if (kw.isNotBlank()) keywordsList.add(kw)
                }
            }

            val matchedIds = mutableSetOf<String>()
            json.optJSONArray("matchedProductIds")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val id = arr.optString(i, "").trim()
                    if (id.isNotBlank()) matchedIds.add(id)
                }
            }

            // Combine direct ID matches from Gemini + keyword/category matches from catalog
            val allTokens = (listOf(searchQuery.lowercase(), detectedItemName.lowercase()) + keywordsList)
                .flatMap { it.split("\\s+|,|/|-".toRegex()) }
                .map { it.trim() }
                .filter { it.length >= 3 && it !in setOf("the", "and", "for", "pack", "with", "scanned", "product", "item") }
                .distinct()

            val matchedProducts = catalogProducts.filter { product ->
                if (product.id in matchedIds) return@filter true
                val searchableText = "${product.name} ${product.category} ${product.subcategory} ${product.description}".lowercase()
                allTokens.any { token -> searchableText.contains(token) } ||
                    (category.isNotBlank() && product.category.equals(category, ignoreCase = true) && allTokens.isEmpty())
            }

            // Strictly sort with available products first and LEAST RATE / LOWEST PRICE at the top/first!
            val sortedByLowestPrice = sortProductsLowestPriceFirst(matchedProducts)
            val lowestPriceAvailable = sortedByLowestPrice.firstOrNull { it.inStock } ?: sortedByLowestPrice.firstOrNull()

            VisualSearchUiState.Success(
                capturedUri = imageUri,
                detectedItemName = detectedItemName.ifBlank { "Product" },
                searchQuery = searchQuery,
                matchedProductIds = sortedByLowestPrice.map { it.id },
                matchedProductsSortedByLowestPrice = sortedByLowestPrice,
                lowestPriceProduct = lowestPriceAvailable
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Camera visual search timed out", e)
            VisualSearchUiState.Error(
                capturedUri = imageUri,
                message = "Photo analysis timed out after 60 seconds. Please try again."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Camera visual search failed", e)
            VisualSearchUiState.Error(
                capturedUri = imageUri,
                message = e.localizedMessage ?: "Failed to analyze product photo."
            )
        }
    }
}

/**
 * Trailing icon row for Search Bars across FlashDrop:
 * Includes a Clear button (when query or photo search is active), a Microphone Voice Search button,
 * a Gallery Photo Picker button, and a prominent Camera button so customers can search by voice or photo.
 */
@Composable
fun SearchBarCameraTrailingActions(
    hasActiveQueryOrPhoto: Boolean,
    isAnalyzingPhoto: Boolean,
    onClear: () -> Unit,
    onLaunchCamera: () -> Unit,
    onPickFromGallery: (() -> Unit)? = null,
    onVoiceSearchResult: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var showVoiceDialog by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Open voice search dialog whether granted or on emulator fallback
        showVoiceDialog = true
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 6.dp)
    ) {
        if (hasActiveQueryOrPhoto) {
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("search_bar_clear_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear Search",
                    tint = TextMuted,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        if (onVoiceSearchResult != null) {
            IconButton(
                onClick = {
                    val hasMicPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasMicPerm) {
                        showVoiceDialog = true
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                enabled = !isAnalyzingPhoto,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(OrangeLight)
                    .testTag("search_bar_mic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Search with Microphone",
                    tint = OrangeSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        if (onPickFromGallery != null) {
            IconButton(
                onClick = onPickFromGallery,
                enabled = !isAnalyzingPhoto,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("search_bar_gallery_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Search by Gallery Photo",
                    tint = GreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        IconButton(
            onClick = onLaunchCamera,
            enabled = !isAnalyzingPhoto,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(GreenLight)
                .testTag("search_bar_camera_button")
        ) {
            if (isAnalyzingPhoto) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = GreenPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Click Photo to Search Product",
                    tint = GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showVoiceDialog && onVoiceSearchResult != null) {
        VoiceSearchListeningDialog(
            onDismiss = { showVoiceDialog = false },
            onVoiceQueryRecognized = { recognizedText ->
                showVoiceDialog = false
                onVoiceSearchResult(recognizedText)
            }
        )
    }
}

/**
 * Interactive Voice Search Listening Dialog using Android SpeechRecognizer + System RecognizerIntent
 * + Instant Voice Phrase Chips so users can search by speaking any product name and see the
 * Lowest Rate / Price product at the top/first.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceSearchListeningDialog(
    onDismiss: () -> Unit,
    onVoiceQueryRecognized: (String) -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Listening... Speak a product name (e.g. Milk, Atta, Tomatoes)") }
    var partialSpokenText by remember { mutableStateOf("") }

    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val bestMatch = matches?.firstOrNull()?.trim()
            if (!bestMatch.isNullOrBlank()) {
                onVoiceQueryRecognized(bestMatch)
            }
        }
    }

    DisposableEffect(Unit) {
        val recognizer = if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }

        if (recognizer != null) {
            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    statusMessage = "Listening now... Speak product name clearly"
                }

                override fun onBeginningOfSpeech() {
                    isListening = true
                    statusMessage = "Hearing your voice..."
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                    statusMessage = "Processing spoken product..."
                }

                override fun onError(error: Int) {
                    isListening = false
                    statusMessage = "Tap 'Retry Mic' to speak again, or tap a voice phrase below:"
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()?.trim()
                    if (!best.isNullOrBlank()) {
                        partialSpokenText = best
                        onVoiceQueryRecognized(best)
                    } else {
                        statusMessage = "Could not catch that. Try speaking again or tap a product below:"
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                    if (!partial.isNullOrBlank()) {
                        partialSpokenText = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            }

            recognizer.setRecognitionListener(listener)
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                recognizer.startListening(intent)
                isListening = true
            } catch (e: Exception) {
                Log.w(TAG, "SpeechRecognizer start failed: ${e.message}")
                isListening = false
                statusMessage = "Speak using System Voice or tap a voice command below:"
            }
        } else {
            statusMessage = "Tap 'System Voice Prompt' or select a spoken product below:"
        }

        onDispose {
            try {
                recognizer?.stopListening()
                recognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    val quickVoicePhrases = remember {
        listOf(
            "Milk" to "🥛 \"Milk\"",
            "Atta" to "🌾 \"Atta\"",
            "Biscuit" to "🍪 \"Biscuit\"",
            "Tomato" to "🍅 \"Tomato\"",
            "Potato" to "🥔 \"Potato\"",
            "Pedha" to "🍬 \"Pedha\"",
            "Notebook" to "📓 \"Notebook\"",
            "Medicine" to "💊 \"Medicine\""
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("voice_search_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pulsing Mic Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(if (isListening) GreenLight else OrangeLight)
                        .border(
                            width = 2.dp,
                            color = if (isListening) GreenPrimary else OrangeSecondary,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone Active",
                        tint = if (isListening) GreenPrimary else OrangeSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isListening) "Listening for Product Name..." else "Voice Product Search",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = statusMessage,
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                if (partialSpokenText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = GreenLight,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "\"$partialSpokenText\"",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GreenPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // System Speech Intent Launcher Button
                Button(
                    onClick = {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                                putExtra(
                                    RecognizerIntent.EXTRA_PROMPT,
                                    "Speak product name (Lowest price shown first)"
                                )
                            }
                            systemSpeechLauncher.launch(intent)
                        } catch (_: Exception) {
                            statusMessage = "System voice activity not available on this emulator. Tap any voice phrase below:"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("launch_system_voice_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Speak with System Microphone", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Or tap a spoken phrase to test Voice Search (Lowest Price First):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickVoicePhrases.forEach { (query, label) ->
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onVoiceQueryRecognized(query) }
                                .testTag("voice_phrase_chip_${query.lowercase()}")
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = TextDark, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Banner shown below the search bar when a customer searches by clicking a photo,
 * highlighting availability and the least rate/price product at the top.
 */
@Composable
fun VisualSearchStatusBanner(
    state: VisualSearchUiState,
    onClear: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (state) {
        is VisualSearchUiState.Idle -> Unit

        is VisualSearchUiState.Loading -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("visual_search_loading_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = state.capturedUri,
                        contentDescription = "Captured product photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, GreenPrimary, RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scanning Your Photo...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GreenPrimary
                        )
                        Text(
                            text = "Checking product availability & sorting lowest price first...",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = GreenPrimary
                    )
                }
            }
        }

        is VisualSearchUiState.Success -> {
            val hasMatches = state.matchedProductsSortedByLowestPrice.isNotEmpty()
            val lowestProd = state.lowestPriceProduct
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("visual_search_result_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasMatches) GreenLight.copy(alpha = 0.7f) else OrangeLight
                ),
                border = BorderStroke(
                    1.dp,
                    if (hasMatches) GreenPrimary.copy(alpha = 0.5f) else OrangeSecondary.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = state.capturedUri,
                        contentDescription = state.detectedItemName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, if (hasMatches) GreenPrimary else OrangeSecondary, RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (hasMatches) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (hasMatches) GreenPrimary else OrangeSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Photo Search: ${state.detectedItemName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (hasMatches && lowestProd != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "✓ Available! Least Rate First: ₹${lowestProd.price.toInt()} (${lowestProd.name})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                            Text(
                                text = "Showing ${state.matchedProductsSortedByLowestPrice.size} matching product(s) sorted from lowest to highest price",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        } else {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "This product is currently not available in Mahoba stores.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                    TextButton(
                        onClick = onClear,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                }
            }
        }

        is VisualSearchUiState.Error -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("visual_search_error_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Camera Search Failed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFB91C1C)
                        )
                        Text(
                            text = state.message,
                            fontSize = 11.sp,
                            color = Color(0xFF991B1B),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (onRetry != null && state.capturedUri != null) {
                        TextButton(
                            onClick = onRetry,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                        }
                    }
                    IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Badge shown on the top / first product when search results are sorted by lowest price.
 */
@Composable
fun LowestPriceFirstBadge(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFDCFCE7))
            .border(1.dp, GreenPrimary, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
            .testTag("lowest_price_top_badge")
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
            contentDescription = null,
            tint = GreenPrimary,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "★ Least Rate / Lowest Price First",
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GreenPrimary
        )
    }
}
