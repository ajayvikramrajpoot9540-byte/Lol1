package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import com.example.data.model.Shop
import com.example.data.model.ShopkeeperAccount
import com.example.data.repository.FlashDropRepository
import com.example.ui.auth.AuthManager
import com.example.ui.navigation.Screen
import com.example.ui.screens.AdminCustomersScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminLoginScreen
import com.example.ui.screens.AdminTransactionsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.DocumentPrintingUploadScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OrderDetailScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProductListScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ShopDetailScreen
import com.example.ui.screens.ShopRegistrationScreen
import com.example.ui.screens.ShopkeeperDashboardScreen
import com.example.ui.screens.ShopkeeperPendingApprovalScreen
import com.example.ui.screens.ShopkeeperSignInScreen
import com.example.ui.screens.ShopkeeperStatusScreen
import com.example.ui.screens.EditCustomerProfileScreen
import com.example.ui.screens.EditShopProfileScreen
import com.example.ui.screens.EditAdminProfileScreen
import com.example.ui.screens.PrintingPressShopsScreen
import com.example.ui.screens.AdminAddShopScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.UpiPaymentScreen
import com.example.ui.screens.PaymentProcessingScreen
import com.example.ui.screens.PaymentSuccessScreen
import com.example.ui.screens.OrderConfirmationScreen
import com.example.ui.screens.flow.FlowCanvasScreen
import com.example.ui.theme.FlashDropTheme
import com.example.ui.theme.BackgroundWarm

class MainActivity : ComponentActivity() {

    private lateinit var repository: FlashDropRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.util.configureAppCheck(this, intent)
        enableEdgeToEdge()
        repository = FlashDropRepository(applicationContext)

        setContent {
            FlashDropTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundWarm
                ) {
                    FlashDropApp(repository = repository)
                }
            }
        }
    }
}

@Composable
fun FlashDropApp(repository: FlashDropRepository) {
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf<String>(Screen.Splash.route) }
    val backStack = remember { mutableStateListOf<String>() }

    var selectedShopId by remember { mutableStateOf<String?>(null) }
    var selectedProductId by remember { mutableStateOf<String?>(null) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var selectedOrderPaymentMethod by remember { mutableStateOf("cash_on_delivery") }
    var selectedOrderAmount by remember { mutableStateOf(315.0) }
    var selectedCategory by remember { mutableStateOf("All") }

    var pendingShop by remember { mutableStateOf<Shop?>(null) }
    var pendingAccount by remember { mutableStateOf<ShopkeeperAccount?>(null) }
    var statusShop by remember { mutableStateOf<Shop?>(null) }
    var statusType by remember { mutableStateOf("PENDING") }
    var statusReason by remember { mutableStateOf("") }

    fun navigateTo(route: String, clearBackStack: Boolean = false) {
        if (clearBackStack) {
            backStack.clear()
        } else if (currentScreen != route) {
            backStack.add(currentScreen)
        }
        currentScreen = route
    }

    fun navigateBack() {
        if (backStack.isNotEmpty()) {
            currentScreen = backStack.removeAt(backStack.size - 1)
        } else if (currentScreen != Screen.Home.route) {
            currentScreen = Screen.Home.route
        }
    }

    // Handle Android system back gesture / back button
    BackHandler(enabled = currentScreen != Screen.Home.route && currentScreen != Screen.Splash.route) {
        navigateBack()
    }

    when (currentScreen) {
        Screen.Splash.route -> {
            SplashScreen(
                onContinue = {
                    if (repository.isUserSignedIn()) {
                        val role = repository.currentProfile.value.role
                        val destination = when {
                            role.equals("MAIN_ADMIN", ignoreCase = true) && repository.isMainAdmin() -> Screen.AdminDashboard.route
                            role.equals("shopkeeper", ignoreCase = true) && repository.isShopkeeper() -> Screen.ShopkeeperDashboard.route
                            else -> Screen.Home.route
                        }
                        navigateTo(destination, clearBackStack = true)
                    } else {
                        navigateTo(Screen.Onboarding.route, clearBackStack = true)
                    }
                }
            )
        }

        Screen.Onboarding.route -> {
            OnboardingScreen(
                repository = repository,
                onGetStarted = {
                    navigateTo(Screen.Home.route, clearBackStack = true)
                },
                onAuthSuccess = {
                    repository.syncWithFirestore()
                    navigateTo(Screen.Home.route, clearBackStack = true)
                },
                onOpenAuth = {
                    navigateTo(Screen.Auth.route)
                },
                onAdminLogin = {
                    navigateTo(Screen.AdminDashboard.route, clearBackStack = true)
                },
                onShopkeeperLogin = {
                    navigateTo(Screen.ShopkeeperSignIn.route)
                }
            )
        }

        Screen.Auth.route -> {
            AuthScreen(
                repository = repository,
                onAuthSuccess = { role ->
                    when (role) {
                        "MAIN_ADMIN" -> navigateTo(Screen.AdminDashboard.route, clearBackStack = true)
                        "shopkeeper" -> navigateTo(Screen.ShopkeeperDashboard.route, clearBackStack = true)
                        else -> navigateTo(Screen.Home.route, clearBackStack = true)
                    }
                },
                onBack = { navigateBack() }
            )
        }

        Screen.Home.route -> {
            HomeScreen(
                repository = repository,
                onNavigate = { route -> navigateTo(route) },
                onShopSelected = { shopId ->
                    selectedShopId = shopId
                    navigateTo(Screen.ShopDetail.route)
                },
                onProductSelected = { prodId ->
                    selectedProductId = prodId
                    navigateTo(Screen.ProductDetail.route)
                },
                onCategorySelected = { category ->
                    selectedCategory = category
                    if (category.equals("Printing Press", ignoreCase = true)) {
                        navigateTo(Screen.PrintingPressShops.route)
                    } else {
                        selectedShopId = null
                        navigateTo(Screen.ProductList.route)
                    }
                }
            )
        }

        Screen.Categories.route -> {
            CategoriesScreen(
                onNavigate = { route -> navigateTo(route) },
                onCategorySelected = { cat ->
                    selectedCategory = cat
                    if (cat.equals("Printing Press", ignoreCase = true)) {
                        navigateTo(Screen.PrintingPressShops.route)
                    } else {
                        selectedShopId = null
                        navigateTo(Screen.ProductList.route)
                    }
                },
                onBack = { navigateBack() },
                repository = repository
            )
        }

        Screen.ProductList.route -> {
            ProductListScreen(
                repository = repository,
                categoryName = selectedCategory,
                shopId = selectedShopId,
                onProductSelected = { prodId ->
                    selectedProductId = prodId
                    navigateTo(Screen.ProductDetail.route)
                },
                onShopSelected = { shopId ->
                    selectedShopId = shopId
                    navigateTo(Screen.ShopDetail.route)
                },
                onNavigate = { route -> navigateTo(route) },
                onBack = { navigateBack() }
            )
        }

        Screen.ShopDetail.route -> {
            ShopDetailScreen(
                repository = repository,
                shopId = selectedShopId ?: "sharma_general",
                onProductSelected = { prodId ->
                    selectedProductId = prodId
                    navigateTo(Screen.ProductDetail.route)
                },
                onNavigate = { route -> navigateTo(route) },
                onBack = { navigateBack() }
            )
        }

        Screen.ProductDetail.route -> {
            ProductDetailScreen(
                repository = repository,
                productId = selectedProductId ?: "prod_atta",
                onBack = { navigateBack() },
                onViewCart = { navigateTo(Screen.Cart.route) },
                onUploadDocuments = { navigateTo(Screen.DocumentPrinting.route) }
            )
        }

        Screen.DocumentPrinting.route -> {
            DocumentPrintingUploadScreen(
                repository = repository,
                shopId = selectedShopId ?: "hemu_printing",
                onNavigate = { route -> navigateTo(route) },
                onBack = { navigateBack() }
            )
        }

        Screen.Cart.route -> {
            CartScreen(
                repository = repository,
                onNavigate = { route -> navigateTo(route) },
                onBack = { navigateBack() }
            )
        }

        Screen.Checkout.route -> {
            CheckoutScreen(
                repository = repository,
                onOrderPlaced = { orderId, paymentMethod ->
                    selectedOrderId = orderId
                    selectedOrderPaymentMethod = paymentMethod
                    val placed = repository.orders.value.find { it.id == orderId }
                    selectedOrderAmount = placed?.totalAmount ?: 315.0

                    when (paymentMethod) {
                        "cash_on_delivery" -> {
                            navigateTo(Screen.OrderConfirmation.route)
                        }
                        "debit_card" -> {
                            navigateTo(Screen.PaymentSuccess.route)
                        }
                        "upi" -> {
                            navigateTo(Screen.PaymentProcessing.route)
                        }
                        else -> {
                            navigateTo(Screen.OrderConfirmation.route)
                        }
                    }
                },
                onBack = { navigateBack() }
            )
        }

        Screen.UpiPayment.route -> {
            val order = repository.orders.value.find { it.id == selectedOrderId }
            UpiPaymentScreen(
                amount = order?.totalAmount ?: selectedOrderAmount,
                orderId = order?.orderNumber ?: selectedOrderId ?: "FD20261021A001",
                onProceedToProcessing = {
                    navigateTo(Screen.PaymentProcessing.route)
                },
                onBack = { navigateBack() }
            )
        }

        Screen.PaymentProcessing.route -> {
            PaymentProcessingScreen(
                onProcessingComplete = {
                    navigateTo(Screen.PaymentSuccess.route, clearBackStack = false)
                }
            )
        }

        Screen.PaymentSuccess.route -> {
            val order = repository.orders.value.find { it.id == selectedOrderId }
            PaymentSuccessScreen(
                amount = order?.totalAmount ?: selectedOrderAmount,
                orderId = order?.orderNumber ?: selectedOrderId ?: "FD20261021A001",
                paymentReference = order?.paymentReference?.ifBlank { "7HGF54J3K2" } ?: "7HGF54J3K2",
                transactionId = order?.transactionId?.ifBlank { "UPI1234567890" } ?: "UPI1234567890",
                onViewOrder = {
                    navigateTo(Screen.OrderConfirmation.route)
                },
                onBackToHome = {
                    navigateTo(Screen.Home.route, clearBackStack = true)
                }
            )
        }

        Screen.OrderConfirmation.route -> {
            val order = repository.orders.value.find { it.id == selectedOrderId }
            val methodLabel = when (order?.paymentMethod ?: selectedOrderPaymentMethod) {
                "cash_on_delivery" -> "Cash on Delivery (COD)"
                "debit_card" -> "Debit Card"
                else -> "UPI"
            }
            val status = order?.paymentStatus ?: if (selectedOrderPaymentMethod == "cash_on_delivery") "pending" else "paid"
            OrderConfirmationScreen(
                orderId = order?.orderNumber ?: selectedOrderId ?: "FD20261021A001",
                totalAmount = order?.totalAmount ?: selectedOrderAmount,
                paymentMethod = methodLabel,
                paymentStatus = status,
                expectedDeliverySlot = order?.confirmedDeliveryTime?.ifBlank { order.expectedDeliverySlot } ?: com.example.data.model.ShopScheduleUtils.calculateExact30MinForward(),
                onTrackOrder = {
                    navigateTo(Screen.OrderDetail.route)
                },
                onBackToHome = {
                    navigateTo(Screen.Home.route, clearBackStack = true)
                }
            )
        }

        Screen.FlowCanvas.route -> {
            FlowCanvasScreen(
                onNavigateToScreen = { route -> navigateTo(route) },
                onBack = { navigateBack() }
            )
        }

        Screen.Orders.route -> {
            OrdersScreen(
                repository = repository,
                onNavigate = { route -> navigateTo(route) },
                onOrderSelected = { orderId ->
                    selectedOrderId = orderId
                    navigateTo(Screen.OrderDetail.route)
                },
                onBack = { navigateBack() }
            )
        }

        Screen.OrderDetail.route -> {
            OrderDetailScreen(
                repository = repository,
                orderId = selectedOrderId ?: "ord_apb123455",
                onBack = { navigateTo(Screen.Orders.route, clearBackStack = true) }
            )
        }

        Screen.Profile.route -> {
            ProfileScreen(
                repository = repository,
                onNavigate = { route -> navigateTo(route) }
            )
        }

        Screen.ShopkeeperDashboard.route -> {
            ShopkeeperDashboardScreen(
                repository = repository,
                onNavigate = { route -> navigateTo(route) },
                onBack = { navigateTo(Screen.Home.route) }
            )
        }

        Screen.ShopkeeperSignIn.route -> {
            ShopkeeperSignInScreen(
                repository = repository,
                onSignInSuccess = { profile ->
                    navigateTo(Screen.ShopkeeperDashboard.route, clearBackStack = true)
                },
                onPendingApproval = { shop, account ->
                    pendingShop = shop
                    pendingAccount = account
                    navigateTo(Screen.ShopkeeperPendingApproval.route)
                },
                onAccountStatus = { shop, status, reason ->
                    statusShop = shop
                    statusType = status
                    statusReason = reason
                    navigateTo(Screen.ShopkeeperStatus.route)
                },
                onRegisterShop = {
                    navigateTo(Screen.ShopRegistration.route)
                },
                onBack = { navigateBack() }
            )
        }

        Screen.ShopkeeperPendingApproval.route -> {
            ShopkeeperPendingApprovalScreen(
                repository = repository,
                shop = pendingShop,
                account = pendingAccount,
                onApproved = {
                    navigateTo(Screen.ShopkeeperDashboard.route, clearBackStack = true)
                },
                onRejected = { reason ->
                    statusShop = pendingShop
                    statusType = "REJECTED"
                    statusReason = reason
                    navigateTo(Screen.ShopkeeperStatus.route, clearBackStack = true)
                },
                onSuspended = { reason ->
                    statusShop = pendingShop
                    statusType = "SUSPENDED"
                    statusReason = reason
                    navigateTo(Screen.ShopkeeperStatus.route, clearBackStack = true)
                },
                onBackToHome = {
                    navigateTo(Screen.Home.route, clearBackStack = true)
                },
                onSignInAnother = {
                    navigateTo(Screen.ShopkeeperSignIn.route)
                }
            )
        }

        Screen.ShopkeeperStatus.route -> {
            ShopkeeperStatusScreen(
                shop = statusShop,
                status = statusType,
                reason = statusReason,
                onReApply = {
                    navigateTo(Screen.ShopRegistration.route)
                },
                onSignInAnother = {
                    navigateTo(Screen.ShopkeeperSignIn.route)
                },
                onBackToHome = {
                    navigateTo(Screen.Home.route, clearBackStack = true)
                }
            )
        }

        Screen.ShopRegistration.route -> {
            ShopRegistrationScreen(
                repository = repository,
                onRegistrationSuccess = { registeredShop ->
                    pendingShop = registeredShop
                    navigateTo(Screen.ShopkeeperPendingApproval.route, clearBackStack = true)
                },
                onSignInAsShopkeeper = {
                    navigateTo(Screen.ShopkeeperSignIn.route)
                },
                onBack = { navigateBack() }
            )
        }

        Screen.AdminLogin.route -> {
            AdminLoginScreen(
                repository = repository,
                onLoginSuccess = {
                    navigateTo(Screen.AdminDashboard.route, clearBackStack = true)
                },
                onBack = { navigateBack() }
            )
        }

        Screen.AdminDashboard.route -> {
            AdminDashboardScreen(
                repository = repository,
                onNavigate = { route -> navigateTo(route) },
                onBack = { navigateTo(Screen.Home.route) }
            )
        }

        Screen.AdminTransactions.route -> {
            AdminTransactionsScreen(
                repository = repository,
                onBack = { navigateBack() }
            )
        }

        Screen.EditCustomerProfile.route -> {
            EditCustomerProfileScreen(
                repository = repository,
                onBack = { navigateBack() }
            )
        }

        Screen.EditShopProfile.route -> {
            EditShopProfileScreen(
                repository = repository,
                shopId = selectedShopId ?: repository.currentProfile.value.shopId,
                onBack = { navigateBack() }
            )
        }

        Screen.EditAdminProfile.route -> {
            EditAdminProfileScreen(
                repository = repository,
                onBack = { navigateBack() }
            )
        }

        Screen.PrintingPressShops.route -> {
            PrintingPressShopsScreen(
                repository = repository,
                onShopSelected = { shopId ->
                    selectedShopId = shopId
                    navigateTo(Screen.ShopDetail.route)
                },
                onDocumentPrintSelected = { shopId ->
                    selectedShopId = shopId
                    navigateTo(Screen.DocumentPrinting.route)
                },
                onNavigate = { route -> navigateTo(route) },
                onBack = { navigateBack() }
            )
        }

        Screen.AdminAddShop.route -> {
            AdminAddShopScreen(
                repository = repository,
                onShopCreated = { shop, account ->
                    navigateTo(Screen.AdminDashboard.route, clearBackStack = true)
                },
                onBack = { navigateBack() }
            )
        }

        Screen.ShopTimingSchedule.route -> {
            com.example.ui.screens.ShopTimingScheduleScreen(
                repository = repository,
                onBack = { navigateBack() }
            )
        }

        Screen.AdminCustomers.route -> {
            AdminCustomersScreen(
                repository = repository,
                onBack = { navigateBack() }
            )
        }
    }
}
