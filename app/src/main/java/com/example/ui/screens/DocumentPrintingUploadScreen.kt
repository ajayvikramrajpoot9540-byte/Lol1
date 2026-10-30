package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.view.DragEvent
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.model.PrintFileItem
import com.example.data.model.PrintingOrderSpec
import com.example.data.model.PrintingPriceCalculator
import com.example.data.repository.ApnaBazRepository
import com.example.ui.navigation.Screen
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.BorderLight
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Customer Printing Journey Steps matching the 5-screen UI reference:
 * Screen 3: UPLOAD_AREA (Drag & Drop, Choose Files, Price Table)
 * Screen 4: FILE_SELECTION (Selected Files list, thumbnails, status, page range/copies pickers, Total Prints 45, View Details)
 * Screen 5: SELECTED_SETTINGS (Printing Settings, Total Prints 45, File Details with chevron, Printing Subtotal, Review Order)
 */
enum class PrintingFlowStep {
    UPLOAD_AREA,
    FILE_SELECTION,
    SELECTED_SETTINGS
}

enum class UploadZoneState {
    IDLE,
    DRAGGING_OVER,
    UPLOADING,
    UPLOADED,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPrintingUploadScreen(
    repository: ApnaBazRepository,
    shopId: String = "hemu_printing",
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val cartItems by repository.cartItems.collectAsState()
    val shops by repository.shops.collectAsState()

    val printingShop = shops.find { it.id == shopId } ?: shops.find { it.category == "Printing Press" }
    val resolvedShopId = printingShop?.id ?: "hemu_printing"
    val resolvedShopName = printingShop?.name ?: "Hemu Printing Press"

    val uploadedFiles = remember { mutableStateListOf<PrintFileItem>() }
    var customerInstructions by remember { mutableStateOf("") }

    // Navigation state across the 3 screens
    var currentStep by remember {
        mutableStateOf(if (uploadedFiles.isNotEmpty()) PrintingFlowStep.FILE_SELECTION else PrintingFlowStep.UPLOAD_AREA)
    }

    // Upload & Drag-and-Drop state tracking
    var zoneState by remember { mutableStateOf(UploadZoneState.IDLE) }
    var isDraggingOver by remember { mutableStateOf(false) }
    var uploadStatusMessage by remember { mutableStateOf("") }
    var uploadProgressPercent by remember { mutableIntStateOf(0) }
    var lastErrorMessage by remember { mutableStateOf<String?>(null) }
    var isAddingToCart by remember { mutableStateOf(false) }

    // Dialog state for modifying copies/page range
    var fileToEdit by remember { mutableStateOf<PrintFileItem?>(null) }
    var editDialogType by remember { mutableStateOf<String?>(null) } // "pages" or "copies"
    var tempPageRange by remember { mutableStateOf("") }
    var tempCopies by remember { mutableIntStateOf(1) }

    // Synchronize zoneState with drag and file count
    LaunchedEffect(isDraggingOver, uploadedFiles.size, lastErrorMessage) {
        zoneState = when {
            lastErrorMessage != null -> UploadZoneState.ERROR
            isDraggingOver -> UploadZoneState.DRAGGING_OVER
            uploadedFiles.isNotEmpty() -> UploadZoneState.UPLOADED
            else -> UploadZoneState.IDLE
        }
    }

    // Pre-load reference mockup files (summing to 45 prints as shown in Screen 4 & Screen 5)
    val loadMockupFiles: () -> Unit = {
        uploadedFiles.clear()
        uploadedFiles.addAll(
            listOf(
                PrintFileItem(
                    id = "file_notes",
                    uriString = "content://sample/notes.pdf",
                    fileName = "Notes.pdf",
                    fileExtension = "pdf",
                    fileSizeBytes = (2.4 * 1024 * 1024).toLong(),
                    fileSizeFormatted = "2.4 MB",
                    mimeType = "application/pdf",
                    detectedPageCount = 10,
                    pageRangeInput = "1-5",
                    selectedPagesCount = 5,
                    copies = 2,
                    isDoubleSided = false,
                    uploadStatus = "uploaded"
                ),
                PrintFileItem(
                    id = "file_photo",
                    uriString = "content://sample/photo.jpg",
                    fileName = "Photo.jpg",
                    fileExtension = "jpg",
                    fileSizeBytes = (1.2 * 1024 * 1024).toLong(),
                    fileSizeFormatted = "1.2 MB",
                    mimeType = "image/jpeg",
                    detectedPageCount = 1,
                    pageRangeInput = "1",
                    selectedPagesCount = 1,
                    copies = 5,
                    isDoubleSided = false,
                    uploadStatus = "uploaded"
                ),
                PrintFileItem(
                    id = "file_report",
                    uriString = "content://sample/report.pdf",
                    fileName = "Report.pdf",
                    fileExtension = "pdf",
                    fileSizeBytes = (3.1 * 1024 * 1024).toLong(),
                    fileSizeFormatted = "3.1 MB",
                    mimeType = "application/pdf",
                    detectedPageCount = 12,
                    pageRangeInput = "1-12",
                    selectedPagesCount = 10, // 5*2 (10) + 5 (5) + 10*3 (30) = 45 prints
                    copies = 3,
                    isDoubleSided = false,
                    uploadStatus = "uploading"
                )
            )
        )
        currentStep = PrintingFlowStep.FILE_SELECTION
    }

    // Function to handle incoming file URIs from Drag & Drop or System File Picker
    val handleSelectedUris: (List<Uri>) -> Unit = { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                zoneState = UploadZoneState.UPLOADING
                uploadStatusMessage = "Analyzing and validating ${uris.size} file(s)..."
                uploadProgressPercent = 10

                var addedCount = 0
                val errorList = mutableListOf<String>()

                for ((index, uri) in uris.withIndex()) {
                    uploadProgressPercent = 10 + ((index + 1) * 80 / uris.size)
                    val result = processAndValidateUri(context, uri, uploadedFiles)
                    if (result.isSuccess) {
                        val fileItem = result.getOrThrow()
                        uploadedFiles.add(fileItem)
                        addedCount++
                    } else {
                        val err = result.exceptionOrNull()?.message ?: "Failed to process file"
                        errorList.add(err)
                    }
                }

                uploadProgressPercent = 100
                if (errorList.isNotEmpty()) {
                    lastErrorMessage = errorList.joinToString(", ")
                    zoneState = UploadZoneState.ERROR
                    snackbarHostState.showSnackbar(errorList.first())
                } else {
                    lastErrorMessage = null
                    zoneState = UploadZoneState.UPLOADED
                    if (addedCount > 0) {
                        snackbarHostState.showSnackbar("Successfully added $addedCount file(s) for printing.")
                        currentStep = PrintingFlowStep.FILE_SELECTION
                    }
                }
            }
        }
    }

    // Launcher for selecting PDF, JPEG, JPG, and PNG files via system file picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        handleSelectedUris(uris)
    }

    // Strict printing price calculations
    val totalPhysicalPrints = PrintingPriceCalculator.calculateTotalPrints(uploadedFiles)
    val totalPhysicalSheets = PrintingPriceCalculator.calculateTotalSheets(uploadedFiles)
    val applicableRate = PrintingPriceCalculator.getApplicableRate(totalPhysicalPrints)
    val printingSubtotal = PrintingPriceCalculator.calculatePrintingSubtotal(totalPhysicalPrints)
    val platformFee = PrintingPriceCalculator.FIXED_PLATFORM_FEE
    val totalEstimatedPayable = printingSubtotal + if (uploadedFiles.isNotEmpty()) platformFee else 0.0

    // Confirm and Add to Cart function
    val onConfirmAndAddToCart: () -> Unit = {
        if (uploadedFiles.isEmpty()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Please upload at least one file to continue.")
            }
        } else if (!isAddingToCart) {
            isAddingToCart = true
            coroutineScope.launch {
                try {
                    val updatedFilesList = mutableListOf<PrintFileItem>()
                    for ((idx, fileItem) in uploadedFiles.withIndex()) {
                        uploadStatusMessage = "Securing file ${idx + 1} of ${uploadedFiles.size}..."
                        val fileBytes = withContext(Dispatchers.IO) {
                            try {
                                context.contentResolver.openInputStream(Uri.parse(fileItem.uriString))?.use {
                                    it.readBytes()
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (fileBytes != null) {
                            val uploadRes = repository.uploadPrintingDocument(
                                shopId = resolvedShopId,
                                fileName = fileItem.fileName,
                                fileBytes = fileBytes,
                                mimeType = fileItem.mimeType.ifBlank {
                                    if (fileItem.fileExtension == "pdf") "application/pdf" else "image/jpeg"
                                }
                            )
                            val storageUrl = uploadRes.getOrDefault("")
                            updatedFilesList.add(
                                fileItem.copy(
                                    storageUrl = storageUrl,
                                    uploadStatus = if (storageUrl.isNotBlank()) "uploaded" else "ready"
                                )
                            )
                        } else {
                            updatedFilesList.add(fileItem)
                        }
                    }

                    val spec = PrintingOrderSpec(
                        shopId = resolvedShopId,
                        shopName = resolvedShopName,
                        files = updatedFilesList,
                        paperSize = "A4",
                        colorMode = "Black & White (B&W)",
                        customerInstructions = customerInstructions.trim(),
                        totalPhysicalPrints = totalPhysicalPrints,
                        totalSheets = totalPhysicalSheets,
                        appliedRatePerPrint = applicableRate,
                        printingSubtotal = printingSubtotal,
                        platformFee = platformFee
                    )

                    repository.addPrintingOrderToCart(spec)
                    isAddingToCart = false
                    onNavigate(Screen.Cart.route)
                } catch (e: Exception) {
                    isAddingToCart = false
                    snackbarHostState.showSnackbar("Failed to submit printing order: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Document Printing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when (currentStep) {
                                PrintingFlowStep.UPLOAD_AREA -> onBack()
                                PrintingFlowStep.FILE_SELECTION -> currentStep = PrintingFlowStep.UPLOAD_AREA
                                PrintingFlowStep.SELECTED_SETTINGS -> currentStep = PrintingFlowStep.FILE_SELECTION
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                actions = {
                    when (currentStep) {
                        PrintingFlowStep.UPLOAD_AREA -> {
                            if (uploadedFiles.isNotEmpty()) {
                                TextButton(onClick = { currentStep = PrintingFlowStep.FILE_SELECTION }) {
                                    Text("Files (${uploadedFiles.size})", color = GreenPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        PrintingFlowStep.FILE_SELECTION -> {
                            TextButton(
                                onClick = {
                                    filePickerLauncher.launch(
                                        arrayOf("application/pdf", "image/jpeg", "image/jpg", "image/png")
                                    )
                                }
                            ) {
                                Text(
                                    text = "Add More",
                                    color = GreenPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        PrintingFlowStep.SELECTED_SETTINGS -> {
                            Button(
                                onClick = onConfirmAndAddToCart,
                                enabled = uploadedFiles.isNotEmpty() && !isAddingToCart,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp).testTag("review_order_button")
                            ) {
                                if (isAddingToCart) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = "Review Order",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            when (currentStep) {
                PrintingFlowStep.UPLOAD_AREA -> {
                    // No fixed bottom bar in Screen 3, matching reference image
                }
                PrintingFlowStep.FILE_SELECTION -> {
                    // Screen 4 Bottom Bar: Total Physical Prints + View Details
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = GreenPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Total Physical Prints",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "$totalPhysicalPrints",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenPrimary
                                    )
                                }
                            }

                            Button(
                                onClick = { currentStep = PrintingFlowStep.SELECTED_SETTINGS },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("view_details_button")
                            ) {
                                Text(
                                    text = "View Details",
                                    color = GreenPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
                PrintingFlowStep.SELECTED_SETTINGS -> {
                    // Screen 5 Bottom Bar: Printing Subtotal (₹45.00)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Printing Subtotal",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${"%.2f".format(printingSubtotal)}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPrimary
                                )
                                Text(
                                    text = "($totalPhysicalPrints prints × ₹" + "%.2f".format(applicableRate) + ")",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        when (currentStep) {
            // Screen 3: Drag & Drop Upload Area
            PrintingFlowStep.UPLOAD_AREA -> {
                Screen3UploadAreaContent(
                    paddingValues = paddingValues,
                    isDraggingOver = isDraggingOver,
                    onDraggingChanged = { isDraggingOver = it },
                    onFilesDropped = { clipData ->
                        val uris = mutableListOf<Uri>()
                        for (i in 0 until clipData.itemCount) {
                            val uri = clipData.getItemAt(i).uri
                            if (uri != null) uris.add(uri)
                        }
                        handleSelectedUris(uris)
                    },
                    onChooseFiles = {
                        filePickerLauncher.launch(
                            arrayOf("application/pdf", "image/jpeg", "image/jpg", "image/png")
                        )
                    },
                    onLoadMockupFiles = loadMockupFiles,
                    hasFiles = uploadedFiles.isNotEmpty(),
                    onViewFiles = { currentStep = PrintingFlowStep.FILE_SELECTION }
                )
            }

            // Screen 4: File Selection & Upload Progress
            PrintingFlowStep.FILE_SELECTION -> {
                Screen4FileSelectionContent(
                    paddingValues = paddingValues,
                    uploadedFiles = uploadedFiles,
                    onRemoveFile = { file -> uploadedFiles.remove(file) },
                    onEditPageRange = { file ->
                        fileToEdit = file
                        editDialogType = "pages"
                        tempPageRange = file.pageRangeInput
                    },
                    onEditCopies = { file ->
                        fileToEdit = file
                        editDialogType = "copies"
                        tempCopies = file.copies
                    }
                )
            }

            // Screen 5: Selected Files & Settings
            PrintingFlowStep.SELECTED_SETTINGS -> {
                Screen5SelectedSettingsContent(
                    paddingValues = paddingValues,
                    uploadedFiles = uploadedFiles,
                    totalPhysicalPrints = totalPhysicalPrints,
                    onFileClick = {
                        currentStep = PrintingFlowStep.FILE_SELECTION
                    }
                )
            }
        }
    }

    // Modal dialog to adjust Page Range or Copies
    if (fileToEdit != null && editDialogType != null) {
        val target = fileToEdit!!
        AlertDialog(
            onDismissRequest = {
                fileToEdit = null
                editDialogType = null
            },
            title = {
                Text(
                    text = if (editDialogType == "pages") "Select Pages for ${target.fileName}" else "Copies for ${target.fileName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                if (editDialogType == "pages") {
                    Column {
                        Text(
                            text = "Total document pages: ${target.detectedPageCount}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = tempPageRange,
                            onValueChange = { tempPageRange = it },
                            label = { Text("Page Range (e.g., 1-5 or All)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { tempPageRange = "All" }) {
                                Text("All (${target.detectedPageCount})")
                            }
                            if (target.detectedPageCount >= 5) {
                                TextButton(onClick = { tempPageRange = "1-5" }) {
                                    Text("1-5 (5)")
                                }
                            }
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Select number of copies/prints: ", fontSize = 13.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { if (tempCopies > 1) tempCopies-- },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                text = "$tempCopies",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            IconButton(
                                onClick = { tempCopies++ },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val index = uploadedFiles.indexOfFirst { it.id == target.id }
                        if (index != -1) {
                            if (editDialogType == "pages") {
                                val cleanInput = tempPageRange.trim()
                                val pagesCount = if (cleanInput.equals("All", ignoreCase = true) || cleanInput.isBlank()) {
                                    target.detectedPageCount
                                } else {
                                    PrintingPriceCalculator.parsePageRangeCount(cleanInput, target.detectedPageCount)
                                }
                                uploadedFiles[index] = target.copy(
                                    pageRangeInput = if (cleanInput.isBlank()) "All" else cleanInput,
                                    selectedPagesCount = pagesCount
                                )
                            } else {
                                uploadedFiles[index] = target.copy(copies = tempCopies.coerceAtLeast(1))
                            }
                        }
                        fileToEdit = null
                        editDialogType = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToEdit = null; editDialogType = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Screen 3: Drag & Drop Upload Area with Price Table
 */
@Composable
private fun Screen3UploadAreaContent(
    paddingValues: PaddingValues,
    isDraggingOver: Boolean,
    onDraggingChanged: (Boolean) -> Unit,
    onFilesDropped: (ClipData) -> Unit,
    onChooseFiles: () -> Unit,
    onLoadMockupFiles: () -> Unit,
    hasFiles: Boolean,
    onViewFiles: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isDraggingOver) GreenPrimary else Color(0xFF10B981),
        label = "borderColor"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isDraggingOver) Color(0xFFECFDF5) else Color.White,
        label = "bgColor"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWarm)
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Large Dashed Border Upload Area
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .drawBehind {
                        val stroke = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 14f), 0f)
                        )
                        drawRoundRect(
                            color = borderColor,
                            size = size,
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                            style = stroke
                        )
                    }
                    .testTag("upload_document_box")
            ) {
                // Native Android Drag Listener layer
                AndroidView(
                    modifier = Modifier.matchParentSize(),
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            isClickable = true
                            isFocusable = true
                            setOnClickListener { onChooseFiles() }
                            setOnDragListener { _, dragEvent ->
                                when (dragEvent.action) {
                                    DragEvent.ACTION_DRAG_STARTED -> true
                                    DragEvent.ACTION_DRAG_ENTERED -> {
                                        onDraggingChanged(true)
                                        true
                                    }
                                    DragEvent.ACTION_DRAG_LOCATION -> true
                                    DragEvent.ACTION_DRAG_EXITED -> {
                                        onDraggingChanged(false)
                                        true
                                    }
                                    DragEvent.ACTION_DROP -> {
                                        onDraggingChanged(false)
                                        val activity = ctx.findActivity()
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity != null) {
                                            try {
                                                activity.requestDragAndDropPermissions(dragEvent)
                                            } catch (_: Exception) {}
                                        }
                                        val clipData = dragEvent.clipData
                                        if (clipData != null) {
                                            onFilesDropped(clipData)
                                        }
                                        true
                                    }
                                    DragEvent.ACTION_DRAG_ENDED -> {
                                        onDraggingChanged(false)
                                        true
                                    }
                                    else -> false
                                }
                            }
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload",
                        tint = GreenPrimary,
                        modifier = Modifier.size(54.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Drag & Drop Your Files Here",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Drop your files here or tap to browse.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onChooseFiles,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("choose_files_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Files", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Convenient Mockup Preview loader button
                    TextButton(
                        onClick = onLoadMockupFiles,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⚡ Use Mockup Files (45 prints)",
                            fontSize = 12.sp,
                            color = OrangeSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Supported formats:",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "PDF, JPG, JPEG, PNG",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 2. Helper Cards (Drag & Drop | Tap to Browse)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Helper Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Drag & Drop", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Text("(Supported on some devices)", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }

                // Right Helper Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onChooseFiles),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Tap to Browse", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Text("(Open your phone storage)", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }
            }
        }

        // 3. Price Table Section matching Screen 3 of Reference Image
        item {
            Column {
                Text(
                    text = "Price Table",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column {
                        // Header Row with Dark Green
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GreenPrimary)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Physical Prints", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.3f))
                            Text("Price Per Print", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("Printing Cost", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                        }

                        val rows = listOf(
                            Triple("1 – 100", "₹1.00", "Quantity × rate"),
                            Triple("101 – 500", "₹1.00", "Quantity × rate"),
                            Triple("501 – 1,000", "₹1.00", "Quantity × rate"),
                            Triple("1,001 – 2,000", "₹0.70", "Quantity × rate"),
                            Triple("2,001 – 5,000", "₹0.70", "Quantity × rate"),
                            Triple("Above 5,000", "Custom (Admin)", "As configured")
                        )

                        rows.forEachIndexed { idx, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 1) Color(0xFFF9FAFB) else Color.White)
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(row.first, fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.3f))
                                Text(row.second, fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(row.third, fontSize = 12.sp, color = TextMuted, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                            }
                            if (idx < rows.size - 1) {
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Yellow info pill below price table
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = OrangeSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "When your order reaches 1,001 prints, ₹0.70 per print will be applied to the entire order.",
                            fontSize = 12.sp,
                            color = Color(0xFF92400E),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen 4: File Selection & Upload Progress matching Reference Image
 */
@Composable
private fun Screen4FileSelectionContent(
    paddingValues: PaddingValues,
    uploadedFiles: List<PrintFileItem>,
    onRemoveFile: (PrintFileItem) -> Unit,
    onEditPageRange: (PrintFileItem) -> Unit,
    onEditCopies: (PrintFileItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWarm)
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Selected Files (${uploadedFiles.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextDark
            )
        }

        items(uploadedFiles, key = { it.id }) { item ->
            val isPdf = item.fileExtension.lowercase() == "pdf"
            val isUploading = item.uploadStatus.equals("uploading", ignoreCase = true)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("file_card_${item.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge icon (Red for PDF, Blue for image)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPdf) Color(0xFFDC2626) else Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.fileName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val detailText = if (isPdf) {
                                "PDF • ${item.fileSizeFormatted} • ${item.detectedPageCount} pages"
                            } else {
                                "${item.fileExtension.uppercase()} • ${item.fileSizeFormatted}"
                            }
                            Text(
                                text = detailText,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Status Badge: Uploaded (Green) vs Uploading (Blue)
                        if (isUploading) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEFF6FF))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "⏳ Uploading...",
                                    color = Color(0xFF2563EB),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFDCFCE7))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "✓ Uploaded",
                                    color = Color(0xFF15803D),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { onRemoveFile(item) },
                            modifier = Modifier.size(28.dp).testTag("remove_file_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Progress bar if uploading
                    if (isUploading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { 0.65f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF2563EB),
                            trackColor = Color(0xFFDBEAFE)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isPdf) {
                            // Pages Dropdown Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .clickable { onEditPageRange(item) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Pages: ${item.pageRangeInput} (${item.selectedPagesCount}) ⌵",
                                        fontSize = 12.sp,
                                        color = TextDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Copies Dropdown Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .clickable { onEditCopies(item) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Copies: ${item.copies} ⌵",
                                        fontSize = 12.sp,
                                        color = TextDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            // Prints Dropdown Pill for Images
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .clickable { onEditCopies(item) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Prints: ${item.copies} ⌵",
                                        fontSize = 12.sp,
                                        color = TextDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 5: Selected Files & Settings matching Reference Image
 */
@Composable
private fun Screen5SelectedSettingsContent(
    paddingValues: PaddingValues,
    uploadedFiles: List<PrintFileItem>,
    totalPhysicalPrints: Int,
    onFileClick: (PrintFileItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWarm)
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Printing Settings Header
        item {
            Text(
                text = "Printing Settings",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3 Settings Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Printing Type
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Printing Type", fontSize = 10.sp, color = TextMuted)
                        Text("Black & White", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                }

                // Card 2: Paper Size
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Paper Size", fontSize = 10.sp, color = TextMuted)
                        Text("A4 (Compulsory)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                }

                // Card 3: Sides
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Sides", fontSize = 10.sp, color = TextMuted)
                        Text("Single-sided", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                }
            }
        }

        // 2. Total Physical Prints Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Total Physical Prints",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$totalPhysicalPrints",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    }
                }
            }
        }

        // 3. File Details Section
        item {
            Column {
                Text(
                    text = "File Details",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                ) {
                    Column {
                        uploadedFiles.forEachIndexed { idx, file ->
                            val isPdf = file.fileExtension.lowercase() == "pdf"
                            val impressions = file.selectedPagesCount * file.copies

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFileClick(file) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPdf) Icons.Default.Description else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.fileName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val summarySubtitle = if (isPdf) {
                                        "Pages: ${file.pageRangeInput} (${file.selectedPagesCount})  |  Copies: ${file.copies}  →  $impressions prints"
                                    } else {
                                        "Prints: ${file.copies}  →  $impressions prints"
                                    }
                                    Text(
                                        text = summarySubtitle,
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Edit",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (idx < uploadedFiles.size - 1) {
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Validates selected file URI against supported formats (PDF, JPG, JPEG, PNG),
 * duplicate additions, 150 MB size limit, and unreadable/corrupted files.
 */
private suspend fun processAndValidateUri(
    context: Context,
    uri: Uri,
    existingFiles: List<PrintFileItem>
): Result<PrintFileItem> = withContext(Dispatchers.IO) {
    try {
        var displayName = "document_${System.currentTimeMillis()}"
        var sizeBytes = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: "Document"
                if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
            }
        }

        // 1. Prevent duplicate file additions
        val isDuplicate = existingFiles.any {
            it.fileName.equals(displayName, ignoreCase = true) &&
                    (sizeBytes == 0L || it.fileSizeBytes == sizeBytes)
        }
        if (isDuplicate) {
            return@withContext Result.failure(
                IllegalArgumentException("File '$displayName' has already been added to your order.")
            )
        }

        // 2. Enforce 150 MB size limit
        if (sizeBytes > 150L * 1024L * 1024L) {
            return@withContext Result.failure(
                IllegalArgumentException(
                    "File '$displayName' (${PrintingPriceCalculator.formatFileSize(sizeBytes)}) exceeds the 150 MB limit. Please select a smaller file."
                )
            )
        }

        // 3. Inspect file magic bytes for real content validation
        val magicBytes = ByteArray(16)
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.read(magicBytes)
            }
        } catch (e: Exception) {
            return@withContext Result.failure(
                IllegalArgumentException("File '$displayName' is unreadable or inaccessible.")
            )
        }

        val isPdfMagic = magicBytes.size >= 4 &&
                magicBytes[0] == 0x25.toByte() && magicBytes[1] == 0x50.toByte() &&
                magicBytes[2] == 0x44.toByte() && magicBytes[3] == 0x46.toByte()

        val isJpegMagic = magicBytes.size >= 3 &&
                magicBytes[0] == 0xFF.toByte() && magicBytes[1] == 0xD8.toByte() && magicBytes[2] == 0xFF.toByte()

        val isPngMagic = magicBytes.size >= 8 &&
                magicBytes[0] == 0x89.toByte() && magicBytes[1] == 0x50.toByte() &&
                magicBytes[2] == 0x4E.toByte() && magicBytes[3] == 0x47.toByte()

        val extFromName = displayName.substringAfterLast('.', "").lowercase()

        val resolvedExtension = when {
            isPdfMagic || extFromName == "pdf" -> "pdf"
            isPngMagic || extFromName == "png" -> "png"
            isJpegMagic || extFromName in listOf("jpg", "jpeg") -> "jpg"
            else -> {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                var isImageValid = false
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, opts)
                        isImageValid = opts.outWidth > 0 && opts.outHeight > 0
                    }
                } catch (_: Exception) {}

                if (isImageValid) "jpg" else ""
            }
        }

        if (resolvedExtension.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Unsupported file type for '$displayName'. Only PDF (.pdf), JPG (.jpg), JPEG (.jpeg), and PNG (.png) files are supported.")
            )
        }

        val resolvedMimeType = when (resolvedExtension) {
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            else -> "image/jpeg"
        }

        // 4. Page count detection & corruption verification for PDF
        var detectedPages = 1
        if (resolvedExtension == "pdf") {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val renderer = PdfRenderer(pfd)
                    detectedPages = renderer.pageCount.coerceAtLeast(1)
                    renderer.close()
                }
            } catch (e: Exception) {
                return@withContext Result.failure(
                    IllegalArgumentException("PDF '$displayName' is unreadable, encrypted, or corrupted.")
                )
            }
        }

        val fileItem = PrintFileItem(
            id = UUID.randomUUID().toString().take(8),
            uriString = uri.toString(),
            fileName = displayName,
            fileExtension = resolvedExtension,
            fileSizeBytes = sizeBytes,
            fileSizeFormatted = PrintingPriceCalculator.formatFileSize(sizeBytes),
            mimeType = resolvedMimeType,
            detectedPageCount = detectedPages,
            pageRangeInput = "All",
            selectedPagesCount = detectedPages,
            copies = 1,
            isDoubleSided = false,
            uploadStatus = "ready"
        )

        Result.success(fileItem)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Context extension to safely retrieve parent Activity for requesting drag & drop permissions.
 */
private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
