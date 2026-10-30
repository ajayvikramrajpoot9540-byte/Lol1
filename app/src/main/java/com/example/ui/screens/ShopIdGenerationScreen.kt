package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Shop
import com.example.data.model.ShopkeeperAccount
import com.example.data.repository.FlashDropRepository
import com.example.ui.components.FlashDropLogo
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopIdGenerationScreen(
    repository: FlashDropRepository,
    applicationId: String,
    onSignInAsShopkeeper: () -> Unit,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 20-Second Countdown Timer State
    var secondsRemaining by remember { mutableIntStateOf(20) }
    var isCountdownComplete by remember { mutableStateOf(false) }

    // Generation states
    var generatedShop by remember { mutableStateOf<Shop?>(null) }
    var generatedAccount by remember { mutableStateOf<ShopkeeperAccount?>(null) }
    var generationError by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var hasAttemptedGeneration by remember { mutableStateOf(false) }

    // Trigger ID Generation idempotently once accepted
    fun startIdGeneration() {
        if (isGenerating || generatedShop != null) return
        isGenerating = true
        generationError = null
        hasAttemptedGeneration = true

        coroutineScope.launch {
            val result = repository.generateAndAssignShopIdForApplication(applicationId)
            isGenerating = false
            if (result.isSuccess) {
                val pair = result.getOrThrow()
                generatedShop = pair.first
                generatedAccount = pair.second
            } else {
                generationError = result.exceptionOrNull()?.message
                    ?: "Could not generate Shop ID. Please retry."
            }
        }
    }

    // Launch generation on initial composition
    LaunchedEffect(applicationId) {
        startIdGeneration()
    }

    // Lifecycle-aware 20-second countdown ticker
    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
        }
        isCountdownComplete = true
    }

    // Subtle pulsing animation for waiting state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isCountdownComplete && generatedShop != null) "Shop Registration Completed" else "Generating Your FlashDrop Shop ID",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextDark
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // STEP PROGRESS HEADER
            RegistrationProgressHeader(currentStep = 3)

            Spacer(modifier = Modifier.height(20.dp))

            if (!isCountdownComplete || generatedShop == null && generationError == null) {
                // WAITING & COUNTDOWN STATE (20 to 0 seconds)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generating_shop_id_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .scale(pulseScale)
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(OrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            FlashDropLogo(iconSize = 64.dp, showTagline = false)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Generating Your FlashDrop Shop ID",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Please wait while we securely process your registration and business documents.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        // 20-SECOND CIRCULAR TIMER
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { (20 - secondsRemaining) / 20f },
                                modifier = Modifier.size(110.dp),
                                strokeWidth = 7.dp,
                                color = OrangeSecondary,
                                trackColor = OrangeLight.copy(alpha = 0.5f)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$secondsRemaining",
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OrangeSecondary
                                )
                                Text(
                                    text = "seconds",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Clear compliance and waiting notice
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BackgroundWarm)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = OrangeSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "This timer indicates registration processing. Generating a Shop ID does not grant immediate Shopkeeper Dashboard access; Admin review of your documents is required before store activation.",
                                    fontSize = 11.sp,
                                    color = TextDark,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            } else if (generationError != null) {
                // ERROR STATE WITH RETRY
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ID Generation Paused",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = generationError ?: "Could not assign a unique Shop ID.",
                            fontSize = 13.sp,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { startIdGeneration() },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry ID Generation")
                        }
                    }
                }
            } else {
                // SUCCESS STATE: SHOP ID ASSIGNED + PENDING REVIEW NOTICE
                val shop = generatedShop!!
                val account = generatedAccount

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_id_success_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(GreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Registration Submitted!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextDark
                        )
                        Text(
                            text = "Your business documents are submitted under review.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                        )

                        // UNIQUE SHOP ID DISPLAY CARD
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = OrangeLight.copy(alpha = 0.5f)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, OrangeSecondary)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "YOUR OFFICIAL FLASHDROP SHOP ID",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OrangeSecondary,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = shop.id,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextDark,
                                        modifier = Modifier.testTag("generated_shop_id_text")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("FlashDrop Shop ID", shop.id)
                                            clipboard.setPrimaryClip(clip)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Shop ID copied to clipboard: ${shop.id}")
                                            }
                                        },
                                        modifier = Modifier.size(32.dp).testTag("copy_shop_id_btn")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Shop ID", tint = OrangeSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Text(
                                    text = "Save this ID. You will need it along with your GSTIN and password to sign in.",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Status Badge: PENDING REVIEW
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFFEF3C7))
                                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "APPLICATION STATUS: PENDING REVIEW",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Application Summary Details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BackgroundWarm)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                SummaryRow(label = "Store Name", value = shop.name)
                                SummaryRow(label = "Owner Name", value = shop.ownerName)
                                SummaryRow(label = "Registered GSTIN", value = shop.gstNumber)
                                SummaryRow(label = "Contact Phone", value = shop.phone)
                                if (account != null) {
                                    SummaryRow(label = "Login Handle", value = account.loginId)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Important Warning Notice
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2).copy(alpha = 0.7f))
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Notice: Having a generated Shop ID does not grant Shopkeeper Dashboard access while approval is pending. Once an Admin reviews and approves your documents, your account will be activated.",
                                    fontSize = 10.sp,
                                    color = Color(0xFF991B1B),
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Navigation Buttons
                        Button(
                            onClick = onSignInAsShopkeeper,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("step3_go_to_signin_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary)
                        ) {
                            Text(
                                text = "Go to Shopkeeper Sign In",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onBackToHome,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("step3_back_to_home_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Return to FlashDrop Home",
                                color = TextDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = TextMuted)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
    }
}
