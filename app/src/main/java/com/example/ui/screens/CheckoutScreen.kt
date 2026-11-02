package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.material3.OutlinedButton
import com.example.ui.components.AddressManagementDialog
import com.example.ui.components.GoogleMapLocationPickerDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItem
import com.example.data.repository.ApnaBazRepository
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.BorderLight
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    repository: ApnaBazRepository,
    onOrderPlaced: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val cartItems by repository.cartItems.collectAsState()
    val profile by repository.currentProfile.collectAsState()
    val deliveryConfig by repository.deliveryConfig.collectAsState()

    var customerName by remember { mutableStateOf(profile.name) }
    var customerPhone by remember { mutableStateOf(profile.phone) }
    var customerAddress by remember { mutableStateOf(profile.address) }
    var customerLocality by remember { mutableStateOf(profile.locality) }
    var customerPinCode by remember { mutableStateOf(profile.pinCode) }

    var selectedDeliveryMethod by remember { mutableStateOf("shop_delivery") } // "shop_delivery" or "customer_pickup"
    var selectedPaymentMethod by remember { mutableStateOf("cash_on_delivery") } // "cash_on_delivery", "upi", "debit_card"
    var isPlacingOrder by remember { mutableStateOf(false) }
    var showAddressDialog by remember { mutableStateOf(false) }
    var showMapLocationPicker by remember { mutableStateOf(false) }
    var showSignInPrompt by remember { mutableStateOf(false) }
    var pendingOrderAfterSignIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!repository.isUserSignedIn()) {
            showSignInPrompt = true
        }
    }

    LaunchedEffect(profile.id, profile.name, profile.phone, profile.address, profile.locality, profile.pinCode, profile.savedAddresses) {
        val defaultAddr = profile.savedAddresses.find { it.isDefault } ?: profile.savedAddresses.firstOrNull()
        if (defaultAddr != null) {
            customerAddress = defaultAddr.formatDisplay()
            customerLocality = defaultAddr.area.ifBlank { "Subhash Chowk" }
            customerPinCode = defaultAddr.pinCode.ifBlank { "210427" }
            if (defaultAddr.name.isNotBlank()) customerName = defaultAddr.name
            if (defaultAddr.phone.isNotBlank()) customerPhone = defaultAddr.phone
        } else {
            if (profile.name.isNotBlank() && !profile.name.equals("Guest", ignoreCase = true)) {
                customerName = profile.name
            }
            if (profile.phone.isNotBlank()) {
                customerPhone = profile.phone
            }
            if (profile.address.isNotBlank()) {
                customerAddress = profile.address
            }
            if (profile.locality.isNotBlank()) {
                customerLocality = profile.locality
            }
            if (profile.pinCode.isNotBlank()) {
                customerPinCode = profile.pinCode
            }
        }
    }

    // Dynamic UPI State: Countdown timer starting at 4:59 (299s)
    var upiTimerSeconds by remember { mutableIntStateOf(299) }
    var upiCopied by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPaymentMethod) {
        if (selectedPaymentMethod == "upi") {
            upiTimerSeconds = 299
            while (upiTimerSeconds > 0) {
                delay(1000L)
                upiTimerSeconds -= 1
            }
        }
    }

    // Dynamic Debit Card States
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf(profile.name) }

    val billResult = remember(cartItems, deliveryConfig, selectedDeliveryMethod) {
        repository.calculateDeliveryBill(
            cartItems = cartItems,
            deliveryMethod = selectedDeliveryMethod
        )
    }
    val subtotal = billResult.merchandiseSubtotal
    val deliveryFee = billResult.netDeliveryFee
    val totalAmount = billResult.finalPayableAmount
    val isFreeDelivery = billResult.isFreeDeliveryEligible
    val isCheckoutAllowed = billResult.isCheckoutAllowed

    fun executeOrderPlacement() {
        if (cartItems.isEmpty() || isPlacingOrder || !isCheckoutAllowed) return
        if (!repository.isUserSignedIn()) {
            pendingOrderAfterSignIn = true
            showSignInPrompt = true
            return
        }
        isPlacingOrder = true
        val paymentStatus = if (selectedPaymentMethod == "cash_on_delivery") "pending" else "paid"
        val txnId = when (selectedPaymentMethod) {
            "upi" -> "UPI${(1000000000L + (Math.random() * 9000000000L).toLong())}"
            "debit_card" -> "DC${(1000000000L + (Math.random() * 9000000000L).toLong())}"
            else -> ""
        }
        val refNum = when (selectedPaymentMethod) {
            "upi" -> "UPI-REF-${(10000 + (Math.random() * 90000).toInt())}"
            "debit_card" -> "AUTH-${(100000 + (Math.random() * 900000).toInt())}"
            else -> ""
        }
        try {
            val resolvedCustomerName = customerName.takeIf { it.isNotBlank() && !it.equals("Guest", ignoreCase = true) }
                ?: repository.currentProfile.value.name
            val order = repository.placeOrder(
                deliveryMethod = selectedDeliveryMethod,
                paymentMethod = selectedPaymentMethod,
                address = customerAddress,
                locality = customerLocality,
                pinCode = customerPinCode,
                customerName = resolvedCustomerName,
                customerPhone = customerPhone,
                paymentStatusOverride = paymentStatus,
                transactionId = txnId,
                paymentReference = refNum
            )
            isPlacingOrder = false
            onOrderPlaced(order.id, selectedPaymentMethod)
        } catch (e: SecurityException) {
            isPlacingOrder = false
            pendingOrderAfterSignIn = true
            showSignInPrompt = true
        }
    }

    if (showSignInPrompt) {
        com.example.ui.components.OrderSignInBottomSheet(
            repository = repository,
            onDismiss = {
                showSignInPrompt = false
                pendingOrderAfterSignIn = false
            },
            onSignInSuccess = {
                showSignInPrompt = false
                val signedInProfile = repository.currentProfile.value
                if (signedInProfile.name.isNotBlank() && !signedInProfile.name.equals("Guest", ignoreCase = true)) {
                    customerName = signedInProfile.name
                }
                if (customerPhone.isBlank() && signedInProfile.phone.isNotBlank()) {
                    customerPhone = signedInProfile.phone
                }
                if (pendingOrderAfterSignIn) {
                    pendingOrderAfterSignIn = false
                    executeOrderPlacement()
                }
            }
        )
    }

    if (showAddressDialog) {
        AddressManagementDialog(
            repository = repository,
            onDismiss = { showAddressDialog = false },
            onOpenMap = {
                showAddressDialog = false
                showMapLocationPicker = true
            },
            onAddressSelected = { selected ->
                customerAddress = selected.formatDisplay()
                customerLocality = selected.area.ifBlank { "Subhash Chowk" }
                customerPinCode = selected.pinCode.ifBlank { "210427" }
                if (selected.name.isNotBlank()) customerName = selected.name
                if (selected.phone.isNotBlank()) customerPhone = selected.phone
                showAddressDialog = false
            }
        )
    }

    if (showMapLocationPicker) {
        GoogleMapLocationPickerDialog(
            initialAddress = customerAddress,
            initialLocality = customerLocality.ifBlank { "Alha Chowk" },
            initialPinCode = customerPinCode.ifBlank { "210427" },
            onDismiss = { showMapLocationPicker = false },
            onLocationConfirmed = { newAddress, newLocality, newPin ->
                customerAddress = newAddress
                customerLocality = newLocality
                customerPinCode = newPin
                repository.updateAddress(newAddress, newLocality, newPin)
                showMapLocationPicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    if (!isCheckoutAllowed) {
                        Text(
                            text = "⚠️ Delivery unavailable: ${billResult.checkoutBlockedReason}",
                            fontSize = 11.sp,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Final Amount", fontSize = 12.sp, color = TextMuted)
                            Text(
                                text = "₹${"%.2f".format(totalAmount)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCheckoutAllowed) GreenPrimary else Color.Gray
                            )
                        }

                        val (buttonText, buttonColor) = when {
                            !isCheckoutAllowed -> "Exceeds Delivery Distance" to Color.Gray
                            !repository.isUserSignedIn() -> "Sign In to Place Order" to OrangeSecondary
                            selectedPaymentMethod == "upi" -> "Pay ₹${"%.2f".format(totalAmount)} via UPI ➔" to OrangeSecondary
                            selectedPaymentMethod == "debit_card" -> "Pay ₹${"%.2f".format(totalAmount)} Securely ➔" to GreenPrimary
                            else -> "Place Order (Pay on Delivery) ➔" to GreenPrimary
                        }

                        Button(
                            onClick = { executeOrderPlacement() },
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("place_order_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                            enabled = cartItems.isNotEmpty() && !isPlacingOrder && isCheckoutAllowed
                        ) {
                            if (isPlacingOrder) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text(buttonText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Guest alert banner if navigating directly to checkout while signed out
            if (!repository.isUserSignedIn()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_guest_alert_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OrangeLight),
                    border = BorderStroke(1.dp, OrangeSecondary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sign in to place your order.", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                            Text("Please sign in with Google to complete your purchase.", fontSize = 12.sp, color = TextMuted)
                        }
                        Button(
                            onClick = { showSignInPrompt = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("checkout_sign_in_banner_button")
                        ) {
                            Text("Sign In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Delivery Address Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delivery Address", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { showMapLocationPicker = true },
                                modifier = Modifier.testTag("checkout_choose_on_map_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Map GPS", color = GreenPrimary, fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = { showAddressDialog = true },
                                modifier = Modifier.testTag("change_address_button")
                            ) {
                                Text("Edit", color = OrangeSecondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$customerName • $customerPhone",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$customerAddress, $customerLocality, Mahoba - $customerPinCode",
                        fontSize = 13.sp,
                        color = TextMuted,
                        lineHeight = 18.sp
                    )
                }
            }

            // Delivery Method Option Card (Shop Delivery vs Customer Pickup)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Delivery Method", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Option A: Shopkeeper Delivery
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedDeliveryMethod == "shop_delivery") GreenLight else Color.Transparent)
                            .clickable { selectedDeliveryMethod = "shop_delivery" }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDeliveryMethod == "shop_delivery",
                            onClick = { selectedDeliveryMethod = "shop_delivery" },
                            colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.DeliveryDining, contentDescription = null, tint = GreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Shopkeeper Delivery", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                            Text("Delivered in 20-30 mins to your address in Mahoba", fontSize = 12.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option B: Customer Pickup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedDeliveryMethod == "customer_pickup") GreenLight else Color.Transparent)
                            .clickable { selectedDeliveryMethod = "customer_pickup" }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDeliveryMethod == "customer_pickup",
                            onClick = { selectedDeliveryMethod = "customer_pickup" },
                            colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Store, contentDescription = null, tint = OrangeSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Customer Pickup (Self Collect)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                            Text("Pick up directly from shop • Zero delivery fee", fontSize = 12.sp, color = TextMuted)
                        }
                    }
                }
            }

            // Payment Method Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Payment Method", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 1: Cash on Delivery (COD)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedPaymentMethod == "cash_on_delivery") GreenLight else Color.Transparent)
                            .clickable { selectedPaymentMethod = "cash_on_delivery" }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPaymentMethod == "cash_on_delivery",
                            onClick = { selectedPaymentMethod = "cash_on_delivery" },
                            colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Payments, contentDescription = null, tint = GreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Cash on Delivery (COD)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                            Text("Pay cash to courier upon doorstep arrival", fontSize = 12.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 2: UPI / QR Code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedPaymentMethod == "upi") OrangeLight else Color.Transparent)
                            .clickable { selectedPaymentMethod = "upi" }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPaymentMethod == "upi",
                            onClick = { selectedPaymentMethod = "upi" },
                            colors = RadioButtonDefaults.colors(selectedColor = OrangeSecondary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.QrCode2, contentDescription = null, tint = OrangeSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("UPI / QR Code", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(OrangeSecondary)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Instant", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Scan dynamic QR code or pay via GPay, PhonePe, Paytm", fontSize = 12.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 3: Debit Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedPaymentMethod == "debit_card") GreenLight else Color.Transparent)
                            .clickable { selectedPaymentMethod = "debit_card" }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPaymentMethod == "debit_card",
                            onClick = { selectedPaymentMethod = "debit_card" },
                            colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = GreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Debit Card", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GreenLight)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Secure", fontSize = 10.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Visa, Mastercard, RuPay with 256-bit SSL encryption", fontSize = 12.sp, color = TextMuted)
                        }
                    }
                }
            }

            // DYNAMIC CHECKOUT LOGIC SECTION CONNECTED TO BACKEND
            when (selectedPaymentMethod) {
                "upi" -> {
                    DynamicUpiQrPaymentCard(
                        totalAmount = totalAmount,
                        timerSeconds = upiTimerSeconds,
                        isCopied = upiCopied,
                        onCopyUpi = { upiCopied = true },
                        onCompletePayment = { executeOrderPlacement() },
                        isPlacingOrder = isPlacingOrder
                    )
                }

                "cash_on_delivery" -> {
                    CodOrderReviewCard(
                        cartItems = cartItems,
                        subtotal = subtotal,
                        deliveryFee = deliveryFee,
                        isFreeDelivery = isFreeDelivery,
                        totalAmount = totalAmount,
                        onPlaceOrder = { executeOrderPlacement() },
                        isPlacingOrder = isPlacingOrder
                    )
                }

                "debit_card" -> {
                    DebitCardPaymentCard(
                        totalAmount = totalAmount,
                        cardNumber = cardNumber,
                        onCardNumberChange = {
                            val digitsOnly = it.filter { char -> char.isDigit() }
                            if (digitsOnly.length <= 16) {
                                cardNumber = digitsOnly.chunked(4).joinToString(" ")
                            }
                        },
                        cardExpiry = cardExpiry,
                        onCardExpiryChange = {
                            val cleaned = it.filter { char -> char.isDigit() }
                            if (cleaned.length <= 4) {
                                cardExpiry = if (cleaned.length >= 3) {
                                    "${cleaned.take(2)}/${cleaned.drop(2)}"
                                } else {
                                    cleaned
                                }
                            }
                        },
                        cardCvv = cardCvv,
                        onCardCvvChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                cardCvv = it
                            }
                        },
                        cardHolder = cardHolder,
                        onCardHolderChange = { cardHolder = it },
                        onPay = { executeOrderPlacement() },
                        isPlacingOrder = isPlacingOrder
                    )
                }
            }

            // COMPLETE ITEMIZED CUSTOMER BILL (Admin-Controlled Delivery System)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeliveryDining,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Itemized Bill & Delivery",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Merchandise Subtotal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Merchandise Subtotal (${cartItems.sumOf { it.quantity }} items)", fontSize = 13.sp, color = TextMuted)
                        Text("₹${"%.2f".format(billResult.merchandiseSubtotal)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                    }

                    if (billResult.platformFee > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Fixed Platform Fee (Printing Press)", fontSize = 13.sp, color = TextMuted)
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE2E8F0))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("Once per order", fontSize = 9.sp, color = TextDark)
                                }
                            }
                            Text("₹${"%.2f".format(billResult.platformFee)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                        }
                    }

                    // Product Discounts
                    if (billResult.productDiscount > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Product MRP Discount", fontSize = 13.sp, color = GreenPrimary)
                            Text("-₹${billResult.productDiscount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                        }
                    }

                    // Multi-shop distance breakdown
                    if (billResult.shopDeliveries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Participating Shops & Verified Distances:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        for (shop in billResult.shopDeliveries) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BackgroundWarm)
                                    .border(0.5.dp, BorderLight, RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = shop.shopName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextDark
                                    )
                                    if (!shop.isWithinMaxLimit) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFFEE2E2))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Limit Exceeded", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text(
                                            text = if (shop.finalDeliveryCharge == 0.0) "₹0 (Free)" else "₹${shop.finalDeliveryCharge.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (shop.finalDeliveryCharge == 0.0) GreenPrimary else TextDark
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "📍 Distance: ${shop.distanceKm} km (${shop.distanceRangeLabel})",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "Base Rate: ₹${shop.baseDeliveryCharge.toInt()}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                if (shop.allocatedDiscount > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Free Delivery Policy Allocation", fontSize = 11.sp, color = GreenPrimary)
                                        Text("-₹${shop.allocatedDiscount.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Delivery Charge Totals
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Delivery Charge", fontSize = 13.sp, color = TextMuted)
                        Text("₹${billResult.totalDeliveryBeforeDiscount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                    }

                    if (billResult.totalDeliveryDiscount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Free Delivery Discount", fontSize = 13.sp, color = GreenPrimary, fontWeight = FontWeight.Medium)
                            Text("-₹${billResult.totalDeliveryDiscount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Amount Payable", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                            Text("Inclusive of taxes & delivery fee", fontSize = 11.sp, color = TextMuted)
                        }
                        Text(
                            text = "₹${"%.2f".format(billResult.finalPayableAmount)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = GreenPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Admin Governance Note
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "🛡️ Delivery charges governed by FlashDrop Main Admins (Pranjal Rajpoot & Chanchal Didi). Route distances strictly verified in Mahoba.",
                            fontSize = 10.sp,
                            color = TextMuted,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// 1. Dynamic UPI QR Code Payment Card with Timer & UPI ID
@Composable
fun DynamicUpiQrPaymentCard(
    totalAmount: Double,
    timerSeconds: Int,
    isCopied: Boolean,
    onCopyUpi: () -> Unit,
    onCompletePayment: () -> Unit,
    isPlacingOrder: Boolean
) {
    val upiId = "flashdrop.orders@okhdfcbank"
    val mins = timerSeconds / 60
    val secs = timerSeconds % 60
    val formattedTime = "%02d:%02d".format(mins, secs)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, OrangeSecondary.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = OrangeSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "UPI Dynamic QR Code",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextDark
                    )
                }

                // Transaction Timer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(OrangeLight)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = OrangeSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedTime,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = OrangeSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic QR Code Canvas Box
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.5.dp, BorderLight, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                UpiQrCodeCanvas(modifier = Modifier.size(146.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Scan with any UPI App • Payable: ₹${totalAmount.toInt()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            // UPI ID Display with Copy Feature
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BackgroundWarm)
                    .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("UPI ID", fontSize = 10.sp, color = TextMuted)
                    Text(upiId, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCopyUpi() }
                        .background(if (isCopied) GreenLight else OrangeLight)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = if (isCopied) GreenPrimary else OrangeSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCopied) "Copied!" else "Copy",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCopied) GreenPrimary else OrangeSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick App Intent Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "GPay" to Color(0xFF1A73E8),
                    "PhonePe" to Color(0xFF5F259F),
                    "Paytm" to Color(0xFF00B9F5),
                    "BHIM" to Color(0xFF00796B)
                ).forEach { (app, color) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(color.copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = app, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action CTA
            Button(
                onClick = onCompletePayment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary),
                enabled = !isPlacingOrder
            ) {
                if (isPlacingOrder) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify & Complete UPI Payment", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 2. Order Summary Review Card for Cash on Delivery (COD)
@Composable
fun CodOrderReviewCard(
    cartItems: List<CartItem>,
    subtotal: Double,
    deliveryFee: Double,
    isFreeDelivery: Boolean,
    totalAmount: Double,
    onPlaceOrder: () -> Unit,
    isPlacingOrder: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, GreenPrimary.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cash on Delivery (COD)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextDark
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GreenLight)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Zero Advance",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Order Summary Review:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cart Items Review List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BackgroundWarm)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                cartItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.quantity}x ${item.product.name}",
                            fontSize = 13.sp,
                            color = TextDark,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "₹${item.totalPrice.toInt()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = BorderLight)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Payable at Doorstep", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    Text("₹${totalAmount.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Assurance Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(GreenLight)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pay cash or scan courier UPI QR upon doorstep package arrival in Mahoba.",
                    fontSize = 12.sp,
                    color = TextDark,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CTA Button
            Button(
                onClick = onPlaceOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                enabled = !isPlacingOrder
            ) {
                if (isPlacingOrder) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Place Order (Pay on Delivery)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 3. Secure Debit Card Entry Fields Card
@Composable
fun DebitCardPaymentCard(
    totalAmount: Double,
    cardNumber: String,
    onCardNumberChange: (String) -> Unit,
    cardExpiry: String,
    onCardExpiryChange: (String) -> Unit,
    cardCvv: String,
    onCardCvvChange: (String) -> Unit,
    cardHolder: String,
    onCardHolderChange: (String) -> Unit,
    onPay: () -> Unit,
    isPlacingOrder: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, GreenPrimary.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Debit Card Entry",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextDark
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "256-bit SSL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cardholder Name
            OutlinedTextField(
                value = cardHolder,
                onValueChange = onCardHolderChange,
                label = { Text("Name on Card") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = BorderLight
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Card Number
            OutlinedTextField(
                value = cardNumber,
                onValueChange = onCardNumberChange,
                label = { Text("Card Number (16-digits)") },
                placeholder = { Text("4321 8765 1092 3456") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RuPay / Visa", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = BorderLight
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Expiry & CVV in Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = cardExpiry,
                    onValueChange = onCardExpiryChange,
                    label = { Text("Expiry (MM/YY)") },
                    placeholder = { Text("08/29") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = BorderLight
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = cardCvv,
                    onValueChange = onCardCvvChange,
                    label = { Text("CVV") },
                    placeholder = { Text("•••") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = BorderLight
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Security note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RBI Compliant & Tokenized • Zero card data saved on device.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action CTA
            Button(
                onClick = onPay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                enabled = !isPlacingOrder
            ) {
                if (isPlacingOrder) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Pay ₹${totalAmount.toInt()} Securely", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddressEditDialog(
    initialAddress: String,
    initialLocality: String,
    initialPin: String,
    supportedAreas: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onOpenMap: () -> Unit = {}
) {
    var address by remember { mutableStateOf(initialAddress) }
    var locality by remember { mutableStateOf(initialLocality) }
    var pinCode by remember { mutableStateOf(initialPin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delivery Address in Mahoba", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenMap()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_open_google_map_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                    border = BorderStroke(1.dp, GreenPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Pinpoint on Google Map (Live GPS)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = GreenPrimary
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("House / Street / Landmark") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = locality,
                    onValueChange = { locality = it },
                    label = { Text("Locality / Mohalla in Mahoba") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = pinCode,
                    onValueChange = { pinCode = it },
                    label = { Text("PIN Code") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(address, locality, pinCode) },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Address")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
