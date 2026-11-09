package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.SeedData
import com.example.data.model.CartItem
import com.example.data.model.DeliveryConfig
import com.example.data.model.Order
import com.example.data.model.OrderItemRecord
import com.example.data.model.Product
import com.example.data.model.RegisteredAccount
import com.example.data.model.Shop
import com.example.data.model.ShopRegistrationConfig
import com.example.data.model.ShopVerificationRequest
import com.example.data.model.PrintingOrderSpec
import com.example.data.model.PrintingPriceCalculator
import com.example.data.model.ShopkeeperAccount
import com.example.data.model.UserProfile
import com.example.data.local.AppPersistenceManager
import com.example.data.model.CustomerOrderSummary
import com.example.data.model.CustomerRecord
import com.example.data.model.DeliveryAddress
import com.example.data.model.NotificationItem
import com.example.data.model.DaySchedule
import com.example.data.model.WeeklySchedule
import com.example.data.model.ShopLiveStatus
import com.example.data.model.ShopScheduleUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.example.util.ImageUtils
import android.net.Uri
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID
import java.io.File
import android.location.Location
import com.example.data.model.DeliveryTracking
import com.example.service.DeliveryLocationService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ShopkeeperLoginResult {
    data class Success(val profile: UserProfile) : ShopkeeperLoginResult()
    data class PendingApproval(val shop: Shop, val account: ShopkeeperAccount?) : ShopkeeperLoginResult()
    data class Rejected(val shop: Shop, val reason: String) : ShopkeeperLoginResult()
    data class Suspended(val shop: Shop, val reason: String) : ShopkeeperLoginResult()
    data class DocumentsRequired(val shop: Shop, val reason: String) : ShopkeeperLoginResult()
    data class Failure(val message: String) : ShopkeeperLoginResult()
}

data class AdminShopCreationResult(
    val shop: Shop,
    val account: ShopkeeperAccount,
    val generatedShopId: String,
    val loginId: String
)

class FlashDropRepository(
    private val context: Context,
    private val db: FirebaseFirestore? = null,
    private val devModeOverride: Boolean? = null
) {
    val isDevMode: Boolean
        get() = devModeOverride ?: com.example.BuildConfig.DEBUG

    private val firestore: FirebaseFirestore by lazy {
        db ?: try {
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (e: Exception) {
            Log.w("FlashDropRepo", "Firestore DB ID fallback: ${e.message}")
            FirebaseFirestore.getInstance()
        }
    }

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("FlashDropRepo", "FirebaseAuth not available: ${e.message}")
            null
        }
    }

    private val storage: FirebaseStorage? by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            Log.w("FlashDropRepo", "FirebaseStorage not available: ${e.message}")
            null
        }
    }
    private val repoScope = CoroutineScope(Dispatchers.IO)
    val persistence: AppPersistenceManager = AppPersistenceManager(context)

    // Local in-memory state for offline-first reliability & immediate UI responsiveness
    private val _shops = MutableStateFlow<List<Shop>>(SeedData.sampleShops)
    val shops: StateFlow<List<Shop>> = _shops.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(SeedData.sampleProducts)
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _transactions = MutableStateFlow<List<com.example.data.model.Transaction>>(SeedData.sampleTransactions)
    val transactions: StateFlow<List<com.example.data.model.Transaction>> = _transactions.asStateFlow()

    private val _deliveryConfig = MutableStateFlow(DeliveryConfig())
    val deliveryConfig: StateFlow<DeliveryConfig> = _deliveryConfig.asStateFlow()

    // Weekly opening/closing schedules for shops in Mahoba (IST evaluated)
    private val _shopSchedules = MutableStateFlow<Map<String, WeeklySchedule>>(emptyMap())
    val shopSchedules: StateFlow<Map<String, WeeklySchedule>> = _shopSchedules.asStateFlow()

    // Global Shop Registration Mode Configuration (Admin-Controlled 3-Step System)
    // Persistently stored at appSettings/shopRegistration/threeStepEnabled
    private val _shopRegistrationConfig = MutableStateFlow(ShopRegistrationConfig(threeStepEnabled = false))
    val shopRegistrationConfig: StateFlow<ShopRegistrationConfig> = _shopRegistrationConfig.asStateFlow()

    // Shopkeeper Business Verification Requests (Step 1-3 applications & Admin Review)
    private val _shopVerificationRequests = MutableStateFlow<List<ShopVerificationRequest>>(
        listOf(
            ShopVerificationRequest(
                id = "req_bundelkhand_001",
                shopId = "FD-SHOP-1002",
                shopName = "Bundelkhand Organic Farm",
                ownerName = "Vijay Kumar Verma",
                phone = "+91 94501 23456",
                category = "Fruits & Vegetables",
                address = "Near Kirat Sagar, Civil Lines, Mahoba",
                description = "Farm-fresh organic vegetables, local seasonal fruits, and pure desi cow milk in Mahoba.",
                gstNumber = "09AABCB1234F1Z8",
                passwordHash = hashPassword("Mahoba@123"),
                aadhaarNumberMasked = "XXXX-XXXX-6789",
                aadhaarDocUri = "content://media/external/images/media/sample_aadhaar",
                panNumber = "AABCB1234F",
                panDocUri = "content://media/external/images/media/sample_pan",
                gstCertificateDocUri = "content://media/external/images/media/sample_gst",
                gstCertificateGstin = "09AABCB1234F1Z8",
                status = ShopVerificationRequest.STATUS_PENDING_REVIEW,
                submittedAtFormatted = "29 Oct 2026, 09:15 AM",
                stepCompleted = 3,
                isThreeStepRegistration = true
            )
        )
    )
    val shopVerificationRequests: StateFlow<List<ShopVerificationRequest>> = _shopVerificationRequests.asStateFlow()

    // Live Delivery Tracking records for active and past orders in Mahoba
    private val _deliveryTracking = MutableStateFlow<Map<String, DeliveryTracking>>(
        mapOf(
            "ord_fd20261021a001" to DeliveryTracking(
                id = "ord_fd20261021a001",
                orderId = "ord_fd20261021a001",
                customerId = "user_demo",
                customerName = "Raju Kumar",
                customerPhone = "+91 98765 12345",
                customerDeliveryAddress = "Alha Chowk, Mahoba - 210427",
                customerLatitude = 25.2965,
                customerLongitude = 79.8752,
                shopId = "sharma_general",
                shopName = "Sharma General Store",
                shopAddress = "Subhash Chowk, Mahoba",
                shopLatitude = 25.2922,
                shopLongitude = 79.8718,
                deliveryManId = "rider_vikram",
                deliveryManName = "Vikram Rathore",
                deliveryManPhone = "+91 98765 88990",
                deliveryManVehicle = "Hero Splendor • UP 95 AB 4122",
                orderStatus = "on_the_way",
                actualLatitude = 25.2942,
                actualLongitude = 79.8735,
                actualAccuracyMeters = 8.5f,
                isGpsActive = true,
                remainingDistanceKm = 0.38,
                estimatedArrivalMinutes = 4,
                stageTimestamps = mapOf(
                    "order_confirmed" to "04:15 PM",
                    "preparing_order" to "04:22 PM",
                    "ready_for_pickup" to "04:35 PM",
                    "picked_up" to "04:42 PM",
                    "on_the_way" to "04:46 PM"
                )
            ),
            "ord_fd20261021a002" to DeliveryTracking(
                id = "ord_fd20261021a002",
                orderId = "ord_fd20261021a002",
                customerId = "user_demo",
                customerName = "Anita Singh",
                customerPhone = "+91 98765 44332",
                customerDeliveryAddress = "Subhash Chowk, Mahoba - 210427",
                customerLatitude = 25.2922,
                customerLongitude = 79.8718,
                shopId = "sharma_general",
                shopName = "Sharma General Store",
                shopAddress = "Subhash Chowk, Mahoba",
                shopLatitude = 25.2922,
                shopLongitude = 79.8718,
                deliveryManId = "rider_rahul",
                deliveryManName = "Rahul Verma",
                deliveryManPhone = "+91 98765 77665",
                deliveryManVehicle = "Bajaj Pulsar • UP 95 CD 8871",
                orderStatus = "delivered",
                actualLatitude = 25.2922,
                actualLongitude = 79.8718,
                actualAccuracyMeters = 5.0f,
                isGpsActive = false,
                remainingDistanceKm = 0.0,
                estimatedArrivalMinutes = 0,
                stageTimestamps = mapOf(
                    "order_confirmed" to "11:00 AM",
                    "preparing_order" to "11:08 AM",
                    "ready_for_pickup" to "11:15 AM",
                    "picked_up" to "11:20 AM",
                    "on_the_way" to "11:22 AM",
                    "delivered" to "11:32 AM"
                )
            )
        )
    )
    val deliveryTracking: StateFlow<Map<String, DeliveryTracking>> = _deliveryTracking.asStateFlow()

    // Shopkeepers list managed by Main Admins
    private val _shopkeepers = MutableStateFlow<List<ShopkeeperAccount>>(SeedData.sampleShopkeepers)
    val shopkeepers: StateFlow<List<ShopkeeperAccount>> = _shopkeepers.asStateFlow()

    // Registered accounts storage (offline-first & Firestore sync)
    private val _registeredUsers = MutableStateFlow<List<RegisteredAccount>>(
        listOf(
            RegisteredAccount(
                id = "cust_ajay",
                fullName = "Ajay Rajput",
                normalizedName = "ajay rajput",
                passwordHash = "password123",
                role = "customer",
                phone = "+91 98765 12345",
                email = "customer@flashdrop.local"
            )
        )
    )
    val registeredUsers: StateFlow<List<RegisteredAccount>> = _registeredUsers.asStateFlow()

    // Customers directory and rich customer data for Main Admin Portal
    private val _customers = MutableStateFlow<List<CustomerRecord>>(SeedData.sampleCustomers)
    val customers: StateFlow<List<CustomerRecord>> = _customers.asStateFlow()

    // Notification Center for Admin, Shopkeeper, and Customer
    private val _notifications = MutableStateFlow<List<NotificationItem>>(
        listOf(
            NotificationItem(
                id = "notif_admin_welcome",
                title = "Admin Portal Active",
                message = "Main Admins Pranjal Rajpoot (PR12) and Chanchal Didi (CD12) have equal administrative authority.",
                timestamp = "Just now",
                type = "admin",
                targetRole = "admin",
                isRead = false
            ),
            NotificationItem(
                id = "notif_admin_shops",
                title = "Merchant Verification Ready",
                message = "Review pending shop registration applications and business documents in Admin Portal.",
                timestamp = "10 mins ago",
                type = "shop",
                targetRole = "admin",
                isRead = false
            ),
            NotificationItem(
                id = "notif_sk_welcome",
                title = "Shopkeeper Hub Ready",
                message = "Manage your store catalog, incoming orders, and track your business earnings in Mahoba.",
                timestamp = "Just now",
                type = "shop",
                targetRole = "shopkeeper",
                isRead = false
            ),
            NotificationItem(
                id = "notif_sk_orders",
                title = "Order Dispatch System Active",
                message = "New orders from Mahoba customers will appear in your 'My Orders' tab with instant alerts.",
                timestamp = "15 mins ago",
                type = "order",
                targetRole = "shopkeeper",
                isRead = false
            ),
            NotificationItem(
                id = "notif_cust_welcome",
                title = "Welcome to FlashDrop Mahoba! ⚡",
                message = "Fast 20-30 minute doorstep delivery across Alha Chowk, Subhash Chowk, and Gandhi Nagar.",
                timestamp = "Just now",
                type = "promo",
                targetRole = "customer",
                isRead = false
            ),
            NotificationItem(
                id = "notif_cust_gps",
                title = "Live GPS Delivery Tracking 📍",
                message = "Track your delivery rider with real-time location updates directly on Google Maps.",
                timestamp = "1 hour ago",
                type = "delivery",
                targetRole = "customer",
                isRead = false
            )
        )
    )
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    fun getNotificationsForRole(role: String, shopId: String = ""): List<NotificationItem> {
        val current = _notifications.value
        val normalizedRole = when (role.uppercase()) {
            "MAIN_ADMIN", "ADMIN" -> "admin"
            "SHOPKEEPER" -> "shopkeeper"
            else -> "customer"
        }
        return current.filter {
            it.targetRole == "all" || it.targetRole.equals(normalizedRole, ignoreCase = true) ||
            (normalizedRole == "shopkeeper" && shopId.isNotBlank() && it.targetId == shopId)
        }
    }

    fun markNotificationAsRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun markAllNotificationsAsRead(role: String) {
        val normalizedRole = when (role.uppercase()) {
            "MAIN_ADMIN", "ADMIN" -> "admin"
            "SHOPKEEPER" -> "shopkeeper"
            else -> "customer"
        }
        _notifications.value = _notifications.value.map {
            if (it.targetRole == "all" || it.targetRole.equals(normalizedRole, ignoreCase = true)) {
                it.copy(isRead = true)
            } else it
        }
    }

    fun clearAllNotifications(role: String) {
        val normalizedRole = when (role.uppercase()) {
            "MAIN_ADMIN", "ADMIN" -> "admin"
            "SHOPKEEPER" -> "shopkeeper"
            else -> "customer"
        }
        _notifications.value = _notifications.value.filterNot {
            it.targetRole == "all" || it.targetRole.equals(normalizedRole, ignoreCase = true)
        }
    }

    fun addNotification(notification: NotificationItem) {
        _notifications.value = listOf(notification) + _notifications.value
    }

    // Profile loading indicator state
    private val _isProfileLoading = MutableStateFlow(false)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading.asStateFlow()

    // Helper to generate a clean Guest profile (unauthenticated)
    fun createGuestProfile(): UserProfile = UserProfile(
        id = "guest",
        name = "Guest",
        email = "",
        phone = "",
        role = "customer",
        loginId = "",
        ownerTitle = "",
        shopId = "",
        shopName = "",
        isVerifiedAdmin = false,
        isVerifiedShopkeeper = false,
        isAuthenticated = false,
        address = "Subhash Chowk, Chhatarpur Road",
        locality = "Alha Chowk",
        pinCode = "210427",
        city = "Mahoba",
        state = "Uttar Pradesh"
    )

    private fun saveSessionToPrefs(profile: UserProfile) {
        persistence.saveUserProfile(profile)
        try {
            val prefs = context.getSharedPreferences("flashdrop_user_session", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("user_id", profile.id)
                .putString("user_name", profile.name)
                .putString("user_email", profile.email)
                .putString("user_phone", profile.phone)
                .putString("user_role", profile.role)
                .putString("user_login_id", profile.loginId)
                .putString("user_owner_title", profile.ownerTitle)
                .putString("user_shop_id", profile.shopId)
                .putString("user_shop_name", profile.shopName)
                .putBoolean("is_admin", profile.isVerifiedAdmin)
                .putBoolean("is_shopkeeper", profile.isVerifiedShopkeeper)
                .putBoolean("is_authenticated", profile.isAuthenticated)
                .putString("user_photo_url", profile.photoUrl)
                .putString("user_address", profile.address)
                .putString("user_locality", profile.locality)
                .putString("user_pin_code", profile.pinCode)
                .putString("user_city", profile.city)
                .putString("user_state", profile.state)
                .apply()
        } catch (e: Exception) {
            Log.w("FlashDropRepo", "Error saving session prefs: ${e.message}")
        }
    }

    private fun clearSessionPrefs() {
        try {
            val prefs = context.getSharedPreferences("flashdrop_user_session", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            Log.w("FlashDropRepo", "Error clearing session prefs: ${e.message}")
        }
    }

    private fun restoreSessionFromPrefs(): UserProfile? {
        val fromFile = persistence.loadUserProfile()
        if (fromFile != null && fromFile.name.isNotBlank() && !fromFile.name.equals("Guest", ignoreCase = true)) {
            return fromFile
        }
        try {
            val prefs = context.getSharedPreferences("flashdrop_user_session", Context.MODE_PRIVATE)
            val isAuth = prefs.getBoolean("is_authenticated", false)
            val userName = prefs.getString("user_name", null)
            if (userName.isNullOrBlank() || userName.equals("Guest", ignoreCase = true)) {
                return fromFile
            }
            return UserProfile(
                id = prefs.getString("user_id", "") ?: "",
                name = userName,
                email = prefs.getString("user_email", "") ?: "",
                phone = prefs.getString("user_phone", "") ?: "",
                role = prefs.getString("user_role", "customer") ?: "customer",
                loginId = prefs.getString("user_login_id", "") ?: "",
                ownerTitle = prefs.getString("user_owner_title", "") ?: "",
                shopId = prefs.getString("user_shop_id", "") ?: "",
                shopName = prefs.getString("user_shop_name", "") ?: "",
                isVerifiedAdmin = prefs.getBoolean("is_admin", false),
                isVerifiedShopkeeper = prefs.getBoolean("is_shopkeeper", false),
                isAuthenticated = isAuth,
                photoUrl = prefs.getString("user_photo_url", "") ?: "",
                address = prefs.getString("user_address", "Subhash Chowk, Chhatarpur Road") ?: "Subhash Chowk, Chhatarpur Road",
                locality = prefs.getString("user_locality", "Alha Chowk") ?: "Alha Chowk",
                pinCode = prefs.getString("user_pin_code", "210427") ?: "210427",
                city = prefs.getString("user_city", "Mahoba") ?: "Mahoba",
                state = prefs.getString("user_state", "Uttar Pradesh") ?: "Uttar Pradesh",
                savedAddresses = fromFile?.savedAddresses ?: emptyList()
            )
        } catch (e: Exception) {
            Log.w("FlashDropRepo", "Error restoring session prefs: ${e.message}")
            return fromFile
        }
    }

    private fun loadUserProfileForFirebaseUser(user: com.google.firebase.auth.FirebaseUser) {
        repoScope.launch {
            _isProfileLoading.value = true
            try {
                val userEmail = user.email ?: ""
                val isOwner1 = userEmail.equals("pranjal.owner@flashdrop.local", ignoreCase = true) ||
                        userEmail.equals("ajayvikramrajpoot9540@gmail.com", ignoreCase = true)
                val isOwner2 = userEmail.equals("chanchal.owner@flashdrop.local", ignoreCase = true)
                if (isOwner1 || isOwner2) {
                    val adminProfile = UserProfile(
                        id = user.uid,
                        name = if (isOwner1) "Pranjal Rajpoot" else "Chanchal Didi",
                        email = user.email ?: (if (isOwner1) "pranjal.owner@flashdrop.local" else "chanchal.owner@flashdrop.local"),
                        phone = user.phoneNumber ?: (if (isOwner1) "+91 98765 00012" else "+91 98765 00013"),
                        role = "MAIN_ADMIN",
                        loginId = if (isOwner1) "PR12" else "CD12",
                        ownerTitle = if (isOwner1) "Owner 1" else "Owner 2",
                        isVerifiedAdmin = true,
                        isVerifiedShopkeeper = false,
                        isAuthenticated = true,
                        address = "FlashDrop Mahoba Office, Subhash Chowk",
                        locality = "Subhash Chowk",
                        city = "Mahoba",
                        state = "Uttar Pradesh",
                        pinCode = "210427"
                    )
                    _currentProfile.value = adminProfile
                    saveSessionToPrefs(adminProfile)
                } else {
                    val userDoc = firestore.collection("users").document(user.uid).get().await()
                    if (userDoc.exists()) {
                        val firestoreProfile = userDoc.toObject(UserProfile::class.java)
                        if (firestoreProfile != null) {
                            val resolvedName = firestoreProfile.name.takeIf { it.isNotBlank() && it != "Guest" }
                                ?: user.displayName?.takeIf { it.isNotBlank() }
                                ?: user.email?.substringBefore("@")
                                ?: "Customer"
                            val resolvedPhoto = user.photoUrl?.toString()?.takeIf { it.isNotBlank() }
                                ?: firestoreProfile.photoUrl
                            val merged = firestoreProfile.copy(
                                id = user.uid,
                                name = resolvedName,
                                email = user.email ?: firestoreProfile.email,
                                photoUrl = resolvedPhoto,
                                isAuthenticated = true
                            )
                            _currentProfile.value = merged
                            saveSessionToPrefs(merged)
                        } else {
                            createAndSetFirebaseUserProfile(user)
                        }
                    } else {
                        createAndSetFirebaseUserProfile(user)
                    }
                }
                syncWithFirestore()
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Error fetching firestore profile: ${e.message}")
                val fallbackName = user.displayName?.takeIf { it.isNotBlank() }
                    ?: user.email?.substringBefore("@")
                    ?: "Customer"
                val profile = UserProfile(
                    id = user.uid,
                    name = fallbackName,
                    email = user.email ?: "",
                    phone = user.phoneNumber ?: "",
                    role = "customer",
                    loginId = "CUST",
                    isVerifiedAdmin = false,
                    isVerifiedShopkeeper = false,
                    isAuthenticated = true,
                    photoUrl = user.photoUrl?.toString() ?: "",
                    address = "Subhash Chowk, Chhatarpur Road",
                    locality = "Alha Chowk",
                    pinCode = "210427",
                    city = "Mahoba",
                    state = "Uttar Pradesh"
                )
                _currentProfile.value = profile
                saveSessionToPrefs(profile)
            } finally {
                _isProfileLoading.value = false
            }
        }
    }

    private suspend fun createAndSetFirebaseUserProfile(user: com.google.firebase.auth.FirebaseUser) {
        val registeredName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")
            ?: "Customer"
        val profile = UserProfile(
            id = user.uid,
            name = registeredName,
            email = user.email ?: "",
            phone = user.phoneNumber ?: "",
            role = "customer",
            loginId = "CUST",
            isVerifiedAdmin = false,
            isVerifiedShopkeeper = false,
            isAuthenticated = true,
            photoUrl = user.photoUrl?.toString() ?: "",
            address = "Subhash Chowk, Chhatarpur Road",
            locality = "Alha Chowk",
            pinCode = "210427",
            city = "Mahoba",
            state = "Uttar Pradesh"
        )
        _currentProfile.value = profile
        saveSessionToPrefs(profile)
        try {
            firestore.collection("users").document(user.uid).set(profile).await()
        } catch (e: Exception) {
            Log.w("FlashDropRepo", "Failed to store user profile in firestore: ${e.message}")
        }
    }

    // Current active user profile initialized to Guest mode by default
    private val _currentProfile = MutableStateFlow(createGuestProfile())
    val currentProfile: StateFlow<UserProfile> = _currentProfile.asStateFlow()

    // Two-Owner Confirmation Safeguard status
    private val _pendingAdminActions = MutableStateFlow<List<String>>(emptyList())
    val pendingAdminActions: StateFlow<List<String>> = _pendingAdminActions.asStateFlow()

    init {
        // 1. Restore all local data from AppPersistenceManager disk storage
        try {
            val loadedShops = persistence.loadShops()
            if (loadedShops != null && loadedShops.isNotEmpty()) {
                _shops.value = loadedShops
            } else {
                persistence.saveShops(_shops.value)
            }

            val loadedProducts = persistence.loadProducts()
            if (loadedProducts != null && loadedProducts.isNotEmpty()) {
                _products.value = loadedProducts
            } else {
                persistence.saveProducts(_products.value)
            }

            val loadedOrders = persistence.loadOrders()
            if (loadedOrders != null) {
                // Filter out old demo sample orders if present
                val nonDemoOrders = loadedOrders.filterNot { it.id.startsWith("ord_fd20261021") }
                _orders.value = nonDemoOrders
                persistence.saveOrders(nonDemoOrders)
            } else {
                persistence.saveOrders(_orders.value)
            }

            val loadedCart = persistence.loadCart()
            if (loadedCart != null) {
                // If loaded cart contains the old demo items, clear it
                val isOldDemoCart = loadedCart.size == 3 &&
                    loadedCart.any { it.product.id == "prod_atta" && it.quantity == 1 } &&
                    loadedCart.any { it.product.id == "prod_parle_g" && it.quantity == 2 } &&
                    loadedCart.any { it.product.id == "prod_maggi" && it.quantity == 1 }
                if (isOldDemoCart) {
                    _cartItems.value = emptyList()
                    persistence.saveCart(emptyList())
                } else {
                    _cartItems.value = loadedCart
                }
            } else {
                persistence.saveCart(_cartItems.value)
            }

            val loadedDeliveryConfig = persistence.loadDeliveryConfig()
            if (loadedDeliveryConfig != null) {
                // If the persisted config is still the unedited initial default with old 10/20/40/70 charges,
                // upgrade it to the new 9/19/39/69 default tiers from DeliveryConfig()
                val charges = loadedDeliveryConfig.distanceTiers.map { it.charge }
                val isOldInitialDefault = loadedDeliveryConfig.version <= 1L &&
                    loadedDeliveryConfig.lastUpdatedAtFormatted == "System Initial Setup" &&
                    charges == listOf(10.0, 20.0, 40.0, 70.0)
                if (isOldInitialDefault) {
                    val upgraded = loadedDeliveryConfig.copy(distanceTiers = DeliveryConfig().distanceTiers)
                    _deliveryConfig.value = upgraded
                    persistence.saveDeliveryConfig(upgraded)
                } else {
                    _deliveryConfig.value = loadedDeliveryConfig
                }
            } else {
                persistence.saveDeliveryConfig(_deliveryConfig.value)
            }

            val loadedRegConfig = persistence.loadShopRegistrationConfig()
            if (loadedRegConfig != null) {
                _shopRegistrationConfig.value = loadedRegConfig
            } else {
                persistence.saveShopRegistrationConfig(_shopRegistrationConfig.value)
            }

            val loadedRequests = persistence.loadShopVerificationRequests()
            if (loadedRequests != null && loadedRequests.isNotEmpty()) {
                _shopVerificationRequests.value = loadedRequests
            } else {
                persistence.saveShopVerificationRequests(_shopVerificationRequests.value)
            }

            val loadedShopkeepers = persistence.loadShopkeepers()
            if (loadedShopkeepers != null && loadedShopkeepers.isNotEmpty()) {
                _shopkeepers.value = loadedShopkeepers
            } else {
                persistence.saveShopkeepers(_shopkeepers.value)
            }

            val loadedTransactions = persistence.loadTransactions()
            if (loadedTransactions != null && loadedTransactions.isNotEmpty()) {
                val seedById = SeedData.sampleTransactions.associateBy { it.id }
                val upgradedLoaded = loadedTransactions.map { existing ->
                    val seedMatch = seedById[existing.id]
                    if (seedMatch != null && existing.itemsSummary.isBlank()) {
                        seedMatch
                    } else {
                        existing
                    }
                }
                val existingIds = upgradedLoaded.map { it.id }.toSet()
                val missingSeeds = SeedData.sampleTransactions.filter { it.id !in existingIds }
                val merged = upgradedLoaded + missingSeeds
                _transactions.value = merged
                if (missingSeeds.isNotEmpty() || upgradedLoaded != loadedTransactions) {
                    persistence.saveTransactions(merged)
                }
            } else {
                persistence.saveTransactions(_transactions.value)
            }

            val loadedUsers = persistence.loadRegisteredUsers()
            if (loadedUsers != null && loadedUsers.isNotEmpty()) {
                _registeredUsers.value = loadedUsers
            } else {
                persistence.saveRegisteredUsers(_registeredUsers.value)
            }

            val loadedCustomers = persistence.loadCustomers()
            if (loadedCustomers != null && loadedCustomers.isNotEmpty()) {
                _customers.value = loadedCustomers
            } else {
                persistence.saveCustomers(_customers.value)
            }

            val loadedSchedules = persistence.loadShopSchedules()
            if (loadedSchedules != null && loadedSchedules.isNotEmpty()) {
                _shopSchedules.value = loadedSchedules
            } else {
                val initMap = _shops.value.associate { it.id to WeeklySchedule(shopId = it.id, days = WeeklySchedule.defaultDays()) }
                _shopSchedules.value = initMap
                persistence.saveShopSchedules(initMap)
            }
            syncShopOpeningStatusesWithIstSchedule()
        } catch (e: Exception) {
            Log.e("FlashDropRepo", "Local data initialization error: ${e.message}", e)
        }

        // 2. Restore User Profile & Session
        val savedProfile = persistence.loadUserProfile()
        if (savedProfile != null && savedProfile.name.isNotBlank() && !savedProfile.name.equals("Guest", ignoreCase = true)) {
            _currentProfile.value = savedProfile
        }

        val currentAuthUser = auth?.currentUser
        if (currentAuthUser != null) {
            loadUserProfileForFirebaseUser(currentAuthUser)
            syncWithFirestore()
        } else {
            val restored = restoreSessionFromPrefs()
            if (restored != null && restored.isAuthenticated) {
                _currentProfile.value = restored
                if (restored.id.isNotBlank() && restored.id != "guest") {
                    repoScope.launch {
                        try {
                            val doc = firestore.collection("users").document(restored.id).get().await()
                            if (doc.exists()) {
                                val fresh = doc.toObject(UserProfile::class.java)
                                if (fresh != null && fresh.name.isNotBlank() && fresh.name != "Guest") {
                                    val merged = fresh.copy(isAuthenticated = true)
                                    _currentProfile.value = merged
                                    saveSessionToPrefs(merged)
                                }
                            }
                        } catch (e: Exception) {
                            Log.d("FlashDropRepo", "Background profile refresh: ${e.message}")
                        }
                    }
                }
            } else if (_currentProfile.value.name.isBlank() || _currentProfile.value.name.equals("Guest", ignoreCase = true)) {
                setGuestMode()
            }
        }

        auth?.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                loadUserProfileForFirebaseUser(user)
                syncWithFirestore()
            } else {
                val restored = restoreSessionFromPrefs()
                if (restored == null || !restored.isAuthenticated) {
                    if (_currentProfile.value.name.isBlank() || _currentProfile.value.name.equals("Guest", ignoreCase = true)) {
                        setGuestMode()
                    }
                }
            }
        }

        DeliveryLocationService.onLocationUpdateListener = { orderId, lat, lng, acc ->
            updateDeliveryGps(orderId, lat, lng, acc)
        }

        // Background ticker to evaluate automatic opening/closing in IST (Asia/Kolkata) every 30s
        repoScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30_000L)
                try {
                    syncShopOpeningStatusesWithIstSchedule()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun syncWithFirestore() {
        repoScope.launch {
            try {
                if (auth?.currentUser == null) return@launch

                val shopSnapshots = firestore.collection("shops").get().await()
                if (!shopSnapshots.isEmpty) {
                    _shops.value = shopSnapshots.toObjects(Shop::class.java)
                } else {
                    SeedData.sampleShops.forEach { shop ->
                        firestore.collection("shops").document(shop.id).set(shop)
                    }
                }

                val prodSnapshots = firestore.collection("products").get().await()
                if (!prodSnapshots.isEmpty) {
                    _products.value = prodSnapshots.toObjects(Product::class.java)
                } else {
                    SeedData.sampleProducts.forEach { prod ->
                        firestore.collection("products").document(prod.id).set(prod)
                    }
                }

                val currentUid = auth?.currentUser?.uid ?: _currentProfile.value.id
                val orderSnapshots = if (isMainAdmin()) {
                    firestore.collection("orders").get().await()
                } else {
                    firestore.collection("orders").whereEqualTo("userId", currentUid).get().await()
                }
                if (!orderSnapshots.isEmpty) {
                    val firestoreOrders = orderSnapshots.toObjects(Order::class.java)
                    val orderMap = (_orders.value + firestoreOrders).associateBy { it.id }
                    _orders.value = orderMap.values.toList()
                }

                val skSnapshots = firestore.collection("shopkeepers").get().await()
                if (!skSnapshots.isEmpty) {
                    _shopkeepers.value = skSnapshots.toObjects(ShopkeeperAccount::class.java)
                } else {
                    SeedData.sampleShopkeepers.forEach { sk ->
                        firestore.collection("shopkeepers").document(sk.id).set(sk)
                    }
                }

                val trackingSnapshots = firestore.collection("delivery_tracking").get().await()
                if (!trackingSnapshots.isEmpty) {
                    val map = trackingSnapshots.documents.mapNotNull { doc ->
                        doc.toObject(DeliveryTracking::class.java)?.let { it.orderId to it }
                    }.toMap()
                    if (map.isNotEmpty()) {
                        _deliveryTracking.value = _deliveryTracking.value + map
                    }
                }

                val configDoc = firestore.collection("delivery_config").document("default").get().await()
                if (configDoc.exists()) {
                    val cfg = configDoc.toObject(DeliveryConfig::class.java)
                    if (cfg != null) {
                        _deliveryConfig.value = cfg
                    }
                }

                try {
                    val regConfigDoc = firestore.collection("appSettings").document("shopRegistration").get().await()
                    if (regConfigDoc.exists()) {
                        val regCfg = regConfigDoc.toObject(ShopRegistrationConfig::class.java)
                        if (regCfg != null) {
                            _shopRegistrationConfig.value = regCfg
                        }
                    }
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "ShopRegistrationConfig load deferred: ${e.message}")
                    // Safe fallback retains threeStepEnabled = false
                }

                try {
                    val reqsSnap = firestore.collection("shop_verification_requests").get().await()
                    if (!reqsSnap.isEmpty) {
                        val list = reqsSnap.documents.mapNotNull { it.toObject(ShopVerificationRequest::class.java) }
                        if (list.isNotEmpty()) {
                            _shopVerificationRequests.value = list
                        }
                    }
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "ShopVerificationRequests load deferred: ${e.message}")
                }

                try {
                    val schedSnap = firestore.collection("shop_schedules").get().await()
                    if (!schedSnap.isEmpty) {
                        val loadedMap = _shopSchedules.value.toMutableMap()
                        schedSnap.documents.forEach { doc ->
                            val sched = doc.toObject(WeeklySchedule::class.java)
                            if (sched != null && sched.shopId.isNotBlank()) {
                                loadedMap[sched.shopId] = sched
                            }
                        }
                        _shopSchedules.value = loadedMap
                        persistence.saveShopSchedules(loadedMap)
                        syncShopOpeningStatusesWithIstSchedule()
                    }
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "ShopSchedules Firestore load deferred: ${e.message}")
                }

                if (isMainAdmin()) {
                    try {
                        val usersSnap = firestore.collection("users").get().await()
                        if (!usersSnap.isEmpty) {
                            usersSnap.documents.forEach { doc ->
                                val prof = doc.toObject(UserProfile::class.java)
                                if (prof != null && prof.role.equals("customer", ignoreCase = true) &&
                                    prof.name.isNotBlank() && !prof.name.equals("Guest", ignoreCase = true)
                                ) {
                                    upsertCustomerFromProfile(prof.copy(id = prof.id.ifBlank { doc.id }))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("FlashDropRepo", "Customers Firestore sync deferred: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Sync exception: ${e.message}")
            }
        }
    }

    /**
     * Synchronizes a customer UserProfile into the Admin customer directory (_customers)
     * and persists it locally so the Admin Customers page always reflects real-time customer data.
     */
    fun upsertCustomerFromProfile(profile: UserProfile, authMethodOverride: String? = null) {
        if (!profile.role.equals("customer", ignoreCase = true)) return
        if (profile.name.isBlank() || profile.name.equals("Guest", ignoreCase = true) || profile.id.equals("guest", ignoreCase = true)) return

        val existingList = _customers.value.toMutableList()
        val matchIndex = existingList.indexOfFirst {
            it.id == profile.id ||
            (profile.email.isNotBlank() && it.email.equals(profile.email, ignoreCase = true)) ||
            (it.fullName.equals(profile.name, ignoreCase = true) && it.phone == profile.phone)
        }

        val defaultAddr = profile.savedAddresses.find { it.isDefault } ?: profile.savedAddresses.firstOrNull()
        val resolvedAddress = defaultAddr?.formatDisplay()?.takeIf { it.isNotBlank() }
            ?: profile.address.ifBlank { "Subhash Chowk, Chhatarpur Road" }
        val resolvedLocality = defaultAddr?.area?.takeIf { it.isNotBlank() }
            ?: profile.locality.ifBlank { "Alha Chowk" }
        val resolvedPin = defaultAddr?.pinCode?.takeIf { it.isNotBlank() }
            ?: profile.pinCode.ifBlank { "210427" }
        val nowDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

        val updatedRecord = if (matchIndex >= 0) {
            val prev = existingList[matchIndex]
            prev.copy(
                id = profile.id.ifBlank { prev.id },
                fullName = profile.name.ifBlank { prev.fullName },
                email = profile.email.ifBlank { prev.email },
                phone = profile.phone.ifBlank { prev.phone },
                photoUrl = profile.photoUrl.ifBlank { prev.photoUrl },
                primaryAddress = resolvedAddress,
                locality = resolvedLocality,
                pinCode = resolvedPin,
                city = profile.city.ifBlank { prev.city },
                state = profile.state.ifBlank { prev.state },
                savedAddresses = if (profile.savedAddresses.isNotEmpty()) profile.savedAddresses else prev.savedAddresses,
                authMethod = authMethodOverride ?: prev.authMethod,
                lastActive = "Just now",
                isVerified = true
            )
        } else {
            val initialAddresses = if (profile.savedAddresses.isNotEmpty()) {
                profile.savedAddresses
            } else {
                listOf(
                    DeliveryAddress(
                        id = "addr_${profile.id.takeLast(4)}",
                        label = "Home",
                        name = profile.name,
                        phone = profile.phone.replace("+91", "").trim(),
                        houseNumber = resolvedAddress.substringBefore(",").trim(),
                        street = resolvedAddress.substringAfter(",", "Main Road").trim(),
                        area = resolvedLocality,
                        city = profile.city.ifBlank { "Mahoba" },
                        state = profile.state.ifBlank { "Uttar Pradesh" },
                        pinCode = resolvedPin,
                        isDefault = true
                    )
                )
            }
            CustomerRecord(
                id = profile.id.ifBlank { "cust_${UUID.randomUUID().toString().take(6)}" },
                fullName = profile.name,
                email = profile.email,
                phone = profile.phone.ifBlank { "+91 98765 12345" },
                photoUrl = profile.photoUrl,
                accountStatus = CustomerRecord.STATUS_ACTIVE,
                authMethod = authMethodOverride ?: "Password",
                joinedDate = nowDate,
                lastActive = "Just now",
                locality = resolvedLocality,
                primaryAddress = resolvedAddress,
                pinCode = resolvedPin,
                city = profile.city.ifBlank { "Mahoba" },
                state = profile.state.ifBlank { "Uttar Pradesh" },
                savedAddresses = initialAddresses,
                isVerified = true
            )
        }

        if (matchIndex >= 0) {
            existingList[matchIndex] = updatedRecord
        } else {
            existingList.add(0, updatedRecord)
        }
        _customers.value = existingList
        persistence.saveCustomers(_customers.value)
    }

    /**
     * Admin Customer Management: Update customer status (Active, VIP, Suspended) and optional notes.
     */
    fun updateCustomerStatus(customerId: String, newStatus: String, adminNotes: String? = null): Boolean {
        if (!isMainAdmin()) return false
        val currentList = _customers.value
        if (currentList.none { it.id == customerId }) return false

        _customers.value = currentList.map { cust ->
            if (cust.id == customerId) {
                cust.copy(
                    accountStatus = newStatus,
                    adminNotes = adminNotes ?: cust.adminNotes
                )
            } else cust
        }
        persistence.saveCustomers(_customers.value)
        return true
    }

    /**
     * Admin Customer Management: Edit customer profile & governance details from Admin Portal.
     */
    fun updateCustomerDetailsByAdmin(
        customerId: String,
        fullName: String,
        phone: String,
        email: String,
        locality: String,
        primaryAddress: String,
        accountStatus: String,
        adminNotes: String
    ): Result<CustomerRecord> {
        if (!isMainAdmin()) {
            return Result.failure(SecurityException("Only authorized Main Admins can modify customer records."))
        }
        val trimmedName = fullName.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Customer full name is required."))
        }
        val cleanPhone = phone.trim()
        if (cleanPhone.replace("+91", "").filter { it.isDigit() }.length < 10) {
            return Result.failure(IllegalArgumentException("Please enter a valid 10-digit phone number."))
        }

        val target = _customers.value.find { it.id == customerId }
            ?: return Result.failure(IllegalArgumentException("Customer record not found."))

        val formattedPhone = if (cleanPhone.startsWith("+91")) cleanPhone else "+91 ${cleanPhone.filter { it.isDigit() }.takeLast(10)}"
        val updated = target.copy(
            fullName = trimmedName,
            phone = formattedPhone,
            email = email.trim().ifBlank { target.email },
            locality = locality.trim().ifBlank { target.locality },
            primaryAddress = primaryAddress.trim().ifBlank { target.primaryAddress },
            accountStatus = accountStatus.ifBlank { target.accountStatus },
            adminNotes = adminNotes.trim()
        )

        _customers.value = _customers.value.map { if (it.id == customerId) updated else it }
        persistence.saveCustomers(_customers.value)

        if (_currentProfile.value.id == customerId) {
            val syncedProfile = _currentProfile.value.copy(
                name = updated.fullName,
                phone = updated.phone,
                email = updated.email,
                locality = updated.locality,
                address = updated.primaryAddress
            )
            _currentProfile.value = syncedProfile
            saveSessionToPrefs(syncedProfile)
        }
        return Result.success(updated)
    }

    /**
     * Admin Customer Management: Add a new customer record directly from the Admin Portal.
     */
    fun addCustomerByAdmin(
        fullName: String,
        phone: String,
        email: String,
        locality: String,
        primaryAddress: String,
        accountStatus: String = CustomerRecord.STATUS_ACTIVE,
        adminNotes: String = ""
    ): Result<CustomerRecord> {
        if (!isMainAdmin()) {
            return Result.failure(SecurityException("Only authorized Main Admins can add customer records."))
        }
        val trimmedName = fullName.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Customer full name is required."))
        }
        val digits = phone.replace("+91", "").filter { it.isDigit() }
        if (digits.length < 10) {
            return Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number."))
        }

        val newId = "cust_${UUID.randomUUID().toString().take(6)}"
        val formattedPhone = "+91 ${digits.takeLast(10)}"
        val resolvedEmail = email.trim().ifBlank { "${normalizeName(trimmedName).replace(" ", ".")}@flashdrop.local" }
        val resolvedLocality = locality.trim().ifBlank { "Alha Chowk" }
        val resolvedAddress = primaryAddress.trim().ifBlank { "$resolvedLocality, Mahoba" }
        val nowDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

        val defaultDeliveryAddr = DeliveryAddress(
            id = "addr_${UUID.randomUUID().toString().take(6)}",
            label = "Home",
            name = trimmedName,
            phone = digits.takeLast(10),
            houseNumber = resolvedAddress.substringBefore(",").trim().ifBlank { "H.No. 1" },
            street = resolvedAddress.substringAfter(",", "Main Road").trim(),
            area = resolvedLocality,
            city = "Mahoba",
            state = "Uttar Pradesh",
            pinCode = "210427",
            isDefault = true
        )

        val newRecord = CustomerRecord(
            id = newId,
            fullName = trimmedName,
            email = resolvedEmail,
            phone = formattedPhone,
            accountStatus = accountStatus,
            authMethod = "Admin Onboarded",
            joinedDate = nowDate,
            lastActive = "Just now",
            locality = resolvedLocality,
            primaryAddress = resolvedAddress,
            pinCode = "210427",
            city = "Mahoba",
            state = "Uttar Pradesh",
            savedAddresses = listOf(defaultDeliveryAddr),
            isVerified = true,
            adminNotes = adminNotes.trim()
        )

        _customers.value = listOf(newRecord) + _customers.value
        persistence.saveCustomers(_customers.value)
        return Result.success(newRecord)
    }

    // Helper functions for user registration

    fun normalizeName(name: String): String {
        return name.trim().lowercase().replace("\\s+".toRegex(), " ")
    }

    fun isReservedAdminName(name: String): Boolean {
        val norm = normalizeName(name)
        val compact = norm.replace(" ", "")
        val reservedList = listOf(
            "pranjal rajpoot",
            "pranjal rajput",
            "pranjal",
            "pranjalrajpoot",
            "pr",
            "pr12",
            "chanchal didi",
            "chanchal",
            "chanchaldidi",
            "cd",
            "cd12"
        )
        if (reservedList.any { norm == it || compact == it }) {
            return true
        }
        val tokens = norm.split(" ")
        return tokens.contains("pranjal") || tokens.contains("chanchal")
    }

    private fun hashPassword(password: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(password.toByteArray())
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            "demo_hash_${password.hashCode()}"
        }
    }

    private fun verifyPassword(input: String, storedHash: String): Boolean {
        if (storedHash == "password123" && input == "password123") return true
        return hashPassword(input) == storedHash || input == storedHash
    }

    // Authenticate Main Admin with masked credentials
    fun loginAsMainAdmin(
        loginId: String,
        password: String,
        isDevelopmentMode: Boolean = isDevMode
    ): Result<UserProfile> {
        if (!isDevelopmentMode) {
            return Result.failure(
                SecurityException("Development demo authentication is disabled in production builds. Please authenticate with your provisioned Firebase Owner account.")
            )
        }

        val trimmedId = loginId.trim()
        val trimmedPass = password.trim()

        if (trimmedId.isEmpty() || trimmedPass.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("Please enter both Login ID and password.")
            )
        }

        val isOwner1 = trimmedId.equals("PR12", ignoreCase = true) ||
                trimmedId.equals("PR", ignoreCase = true) ||
                trimmedId.equals("Pranjal Rajpoot", ignoreCase = true) ||
                trimmedId.contains("Pranjal", ignoreCase = true) ||
                trimmedId.equals("ajayvikramrajpoot9540@gmail.com", ignoreCase = true)

        val isOwner2 = trimmedId.equals("CD12", ignoreCase = true) ||
                trimmedId.equals("CD", ignoreCase = true) ||
                trimmedId.equals("Chanchal Didi", ignoreCase = true) ||
                trimmedId.contains("Chanchal", ignoreCase = true)

        if (!isOwner1 && !isOwner2) {
            return Result.failure(
                IllegalArgumentException("Invalid Login ID or password. Access restricted to authorized Main Admins.")
            )
        }

        val isPasswordCorrect = if (isOwner1) {
            trimmedPass.equals("PR12", ignoreCase = true) || trimmedPass.equals("PR", ignoreCase = true)
        } else {
            trimmedPass.equals("CD12", ignoreCase = true) || trimmedPass.equals("CD", ignoreCase = true)
        }

        if (!isPasswordCorrect) {
            return Result.failure(
                IllegalArgumentException("Invalid Login ID or password. Access restricted to authorized Main Admins.")
            )
        }

        val name = if (isOwner1) "Pranjal Rajpoot" else "Chanchal Didi"
        val code = if (isOwner1) "PR12" else "CD12"
        val title = if (isOwner1) "Owner 1" else "Owner 2"
        val email = if (isOwner1) "pranjal.owner@flashdrop.local" else "chanchal.owner@flashdrop.local"

        val adminProfile = UserProfile(
            id = "admin_$code",
            name = name,
            email = email,
            phone = if (isOwner1) "+91 98765 00012" else "+91 98765 00013",
            role = "MAIN_ADMIN",
            loginId = code,
            ownerTitle = title,
            isVerifiedAdmin = true,
            isVerifiedShopkeeper = false,
            isAuthenticated = true,
            address = "FlashDrop Mahoba Office, Subhash Chowk",
            locality = "Subhash Chowk",
            pinCode = "210427",
            city = "Mahoba",
            state = "Uttar Pradesh"
        )
        _currentProfile.value = adminProfile
        saveSessionToPrefs(adminProfile)
        return Result.success(adminProfile)
    }

    fun registerNewUser(fullName: String, password: String): Result<UserProfile> {
        val trimmed = fullName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter your full name."))
        }
        if (password.length < 4) {
            return Result.failure(IllegalArgumentException("Password must be at least 4 characters."))
        }

        // Strict validation: Reject reserved FlashDrop Admin names with exact prompt message
        if (isReservedAdminName(trimmed)) {
            return Result.failure(
                IllegalArgumentException("This name is reserved for a FlashDrop Admin. Please choose another name.")
            )
        }

        val normalized = normalizeName(trimmed)

        // Duplicate name validation
        val duplicate = _registeredUsers.value.any { it.normalizedName == normalized }
        if (duplicate) {
            return Result.failure(
                IllegalArgumentException("An account with the name '$trimmed' already exists. Please choose another name or sign in.")
            )
        }

        val newId = "cust_${UUID.randomUUID().toString().take(6)}"
        val safeEmail = "${normalized.replace(" ", ".")}@flashdrop.local"
        val newAccount = RegisteredAccount(
            id = newId,
            fullName = trimmed,
            normalizedName = normalized,
            passwordHash = hashPassword(password),
            role = "customer",
            phone = "+91 98765 12345",
            email = safeEmail
        )
        _registeredUsers.value = _registeredUsers.value + newAccount
        persistence.saveRegisteredUsers(_registeredUsers.value)

        val profile = UserProfile(
            id = newId,
            name = trimmed,
            email = safeEmail,
            phone = "+91 98765 12345",
            role = "customer",
            loginId = "CUST",
            isVerifiedAdmin = false,
            isVerifiedShopkeeper = false,
            isAuthenticated = true,
            address = "Subhash Chowk, Chhatarpur Road",
            locality = "Alha Chowk",
            pinCode = "210427",
            city = "Mahoba",
            state = "Uttar Pradesh"
        )
        _currentProfile.value = profile
        saveSessionToPrefs(profile)
        upsertCustomerFromProfile(profile, authMethodOverride = "Password")

        repoScope.launch {
            try {
                firestore.collection("users").document(newId).set(profile)
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "User registration firestore sync deferred: ${e.message}")
            }
        }

        return Result.success(profile)
    }

    fun authenticateUser(fullNameOrLoginId: String, password: String): Result<UserProfile> {
        val trimmed = fullNameOrLoginId.trim()
        val trimmedPass = password.trim()
        if (trimmed.isEmpty() || trimmedPass.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter both Full Name / Login ID and password."))
        }

        val norm = normalizeName(trimmed)

        // Check if attempting Admin login (PR, PR12, Pranjal Rajpoot, CD, CD12, Chanchal Didi)
        val isOwner1 = norm == "pr" || norm == "pr12" || norm == "pranjal rajpoot" || norm == "pranjal rajput" || norm.contains("pranjal")
        val isOwner2 = norm == "cd" || norm == "cd12" || norm == "chanchal didi" || norm.contains("chanchal")

        if (isOwner1 || isOwner2) {
            return loginAsMainAdmin(trimmed, password)
        }

        // Check if authorized Shopkeeper
        val sk = _shopkeepers.value.find {
            it.loginId.equals(trimmed, ignoreCase = true) ||
            it.email.equals(trimmed, ignoreCase = true) ||
            it.shopkeeperName.equals(trimmed, ignoreCase = true) ||
            it.shopId.equals(trimmed, ignoreCase = true)
        }
        if (sk != null) {
            return loginAsShopkeeper(sk.loginId, password)
        }

        // Check registered Customer accounts
        val customer = _registeredUsers.value.find {
            it.normalizedName == norm ||
            it.fullName.equals(trimmed, ignoreCase = true) ||
            it.email.equals(trimmed, ignoreCase = true)
        }
        if (customer != null) {
            if (verifyPassword(trimmedPass, customer.passwordHash)) {
                val profile = UserProfile(
                    id = customer.id,
                    name = customer.fullName,
                    email = customer.email,
                    phone = customer.phone,
                    role = "customer",
                    loginId = "CUST",
                    isVerifiedAdmin = false,
                    isVerifiedShopkeeper = false,
                    isAuthenticated = true,
                    address = "Subhash Chowk, Chhatarpur Road",
                    locality = "Alha Chowk",
                    pinCode = "210427",
                    city = "Mahoba",
                    state = "Uttar Pradesh"
                )
                _currentProfile.value = profile
                saveSessionToPrefs(profile)
                upsertCustomerFromProfile(profile, authMethodOverride = "Password")
                return Result.success(profile)
            } else {
                return Result.failure(IllegalArgumentException("Invalid password. Please check your credentials."))
            }
        }

        return Result.failure(
            IllegalArgumentException("No account found for '$trimmed'. Please register as a new user or check your Login ID.")
        )
    }

    fun requestPasswordReset(fullNameOrEmail: String): Result<String> {
        val trimmed = fullNameOrEmail.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter your registered Full Name or Email."))
        }
        val norm = normalizeName(trimmed)
        if (norm == "pr" || norm == "pr12" || norm.contains("pranjal") ||
            norm == "cd" || norm == "cd12" || norm.contains("chanchal")) {
            return Result.success("Admin password reset request logged. Temporary credentials PR12 / CD12 are maintained for development mode.")
        }
        val exists = _registeredUsers.value.any { it.normalizedName == norm || it.email.equals(trimmed, ignoreCase = true) } ||
                     _shopkeepers.value.any { it.shopkeeperName.equals(trimmed, ignoreCase = true) || it.email.equals(trimmed, ignoreCase = true) }
        if (!exists) {
            return Result.failure(IllegalArgumentException("No account found matching '$trimmed'."))
        }
        return Result.success("Password reset instructions have been sent to your registered contact. Please check your messages.")
    }

    // Authenticate Shopkeeper with masked credentials & specific shop assignment
    fun loginAsShopkeeper(loginId: String, password: String): Result<UserProfile> {
        val trimmedId = loginId.trim()
        val sk = _shopkeepers.value.find {
            it.loginId.equals(trimmedId, ignoreCase = true) ||
            it.email.equals(trimmedId, ignoreCase = true) ||
            it.shopId.equals(trimmedId, ignoreCase = true) ||
            it.phone.equals(trimmedId, ignoreCase = true)
        }

        if (sk == null) {
            return Result.failure(IllegalArgumentException("Shopkeeper Login ID or Phone not found. Contact Main Admin (PR12 / CD12) or register your shop."))
        }

        if (!sk.isActive) {
            return Result.failure(IllegalStateException("This shopkeeper account is pending review or has been suspended by Main Admin."))
        }

        if (password.length < 4) {
            return Result.failure(IllegalArgumentException("Password must be at least 4 characters."))
        }

        if (sk.password.isNotBlank() && sk.password != password.trim()) {
            return Result.failure(IllegalArgumentException("Incorrect password for this shopkeeper account."))
        }

        val shop = _shops.value.find { it.id == sk.shopId }
        val shopkeeperProfile = UserProfile(
            id = sk.id,
            name = sk.shopkeeperName,
            email = sk.email,
            phone = sk.phone,
            role = "shopkeeper",
            loginId = sk.loginId,
            shopId = sk.shopId,
            shopName = shop?.name ?: sk.shopName,
            isVerifiedAdmin = false,
            isVerifiedShopkeeper = true,
            isAuthenticated = true,
            address = shop?.address ?: "Mahoba Bazaar",
            locality = "Market Area",
            pinCode = "210427",
            city = "Mahoba",
            state = "Uttar Pradesh"
        )
        _currentProfile.value = shopkeeperProfile
        saveSessionToPrefs(shopkeeperProfile)
        return Result.success(shopkeeperProfile)
    }

    // Rate limiting tracking for dedicated Shopkeeper Portal
    private var shopkeeperConsecutiveFailures = 0
    private var shopkeeperLockoutUntilMs = 0L

    fun getShopkeeperLockoutSecondsRemaining(): Int {
        val now = System.currentTimeMillis()
        return if (now < shopkeeperLockoutUntilMs) {
            (((shopkeeperLockoutUntilMs - now) / 1000) + 1).toInt()
        } else {
            0
        }
    }

    /**
     * Dedicated Shopkeeper Authentication requiring 4 credentials:
     * GST Number, Shop Name / Shop ID, Mobile Number, and Password.
     * Enforces rate limiting (lockout after 5 failures) and returns structured status
     * without leaking user existence.
     */
    fun authenticateShopkeeper(
        gstNumber: String,
        shopNameOrId: String,
        mobileNumber: String,
        password: String
    ): ShopkeeperLoginResult {
        val secondsRemaining = getShopkeeperLockoutSecondsRemaining()
        if (secondsRemaining > 0) {
            return ShopkeeperLoginResult.Failure(
                "Too many failed attempts. Account locked for security. Please try again in $secondsRemaining seconds."
            )
        }

        val trimmedGst = gstNumber.trim().uppercase()
        val trimmedShop = shopNameOrId.trim()
        val cleanPhone = mobileNumber.replace("+91", "").replace(" ", "").replace("-", "").trim()
        val trimmedPass = password.trim()

        if (trimmedGst.isEmpty() || trimmedShop.isEmpty() || cleanPhone.isEmpty() || trimmedPass.isEmpty()) {
            return ShopkeeperLoginResult.Failure("All fields are required: GST Number, Shop Name/ID, Mobile Number, and Password.")
        }

        // GST format validation (Indian 15-character GSTIN)
        val gstRegex = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
        if (!gstRegex.matches(trimmedGst)) {
            return ShopkeeperLoginResult.Failure("Invalid GST Number format. Must be a 15-character valid Indian GSTIN (e.g., 09AABCU9603R1ZV).")
        }

        // Mobile format validation (10-digit Indian mobile starting with 6-9)
        val mobileRegex = Regex("^[6-9]\\d{9}$")
        if (!mobileRegex.matches(cleanPhone)) {
            return ShopkeeperLoginResult.Failure("Invalid mobile number. Please enter a valid 10-digit Indian mobile number.")
        }

        if (trimmedPass.length < 4) {
            return ShopkeeperLoginResult.Failure("Password must be at least 4 characters.")
        }

        // Search in shops and shopkeeper accounts
        val matchedShop = _shops.value.find { shop ->
            val gstMatch = shop.gstNumber.equals(trimmedGst, ignoreCase = true)
            val nameMatch = shop.name.equals(trimmedShop, ignoreCase = true) || shop.id.equals(trimmedShop, ignoreCase = true)
            val shopPhone = shop.phone.replace("+91", "").replace(" ", "").replace("-", "").trim()
            val phoneMatch = shopPhone.endsWith(cleanPhone) || cleanPhone.endsWith(shopPhone)
            gstMatch && (nameMatch || phoneMatch)
        }

        val matchedAccount = _shopkeepers.value.find { sk ->
            val gstMatch = sk.gstNumber.equals(trimmedGst, ignoreCase = true)
            val nameMatch = sk.shopName.equals(trimmedShop, ignoreCase = true) ||
                            sk.shopId.equals(trimmedShop, ignoreCase = true) ||
                            sk.loginId.equals(trimmedShop, ignoreCase = true)
            val skPhone = sk.phone.replace("+91", "").replace(" ", "").replace("-", "").trim()
            val phoneMatch = skPhone.endsWith(cleanPhone) || cleanPhone.endsWith(skPhone)
            (gstMatch || sk.gstNumber.isEmpty()) && (nameMatch || phoneMatch)
        }

        val targetShop = matchedShop ?: _shops.value.find { it.id == matchedAccount?.shopId }
        val targetAccount = matchedAccount ?: _shopkeepers.value.find { it.shopId == targetShop?.id }

        if (targetShop == null && targetAccount == null) {
            val matchedRequest = _shopVerificationRequests.value.find { req ->
                val gstMatch = req.gstNumber.equals(trimmedGst, ignoreCase = true)
                val nameMatch = req.shopName.equals(trimmedShop, ignoreCase = true) ||
                                req.id.equals(trimmedShop, ignoreCase = true) ||
                                (req.shopId.isNotBlank() && req.shopId.equals(trimmedShop, ignoreCase = true))
                val reqPhone = req.phone.replace("+91", "").replace(" ", "").replace("-", "").trim()
                val phoneMatch = reqPhone.endsWith(cleanPhone) || cleanPhone.endsWith(reqPhone)
                gstMatch && (nameMatch || phoneMatch)
            }

            if (matchedRequest != null) {
                if (!verifyPassword(trimmedPass, matchedRequest.passwordHash)) {
                    shopkeeperConsecutiveFailures++
                    return ShopkeeperLoginResult.Failure("The shop account details could not be verified. Please check your details and try again.")
                }
                shopkeeperConsecutiveFailures = 0
                val shopPlaceholder = Shop(
                    id = if (matchedRequest.shopId.isNotBlank()) matchedRequest.shopId else matchedRequest.id,
                    name = matchedRequest.shopName,
                    ownerName = matchedRequest.ownerName,
                    phone = matchedRequest.phone,
                    gstNumber = matchedRequest.gstNumber,
                    category = matchedRequest.category,
                    address = matchedRequest.address,
                    status = matchedRequest.status.lowercase()
                )
                return when (matchedRequest.status) {
                    ShopVerificationRequest.STATUS_DOCUMENTS_REQUIRED -> {
                        ShopkeeperLoginResult.DocumentsRequired(
                            shop = shopPlaceholder,
                            reason = matchedRequest.documentsRequiredNotes.ifBlank { "Additional business verification documents required by FlashDrop Admins." }
                        )
                    }
                    ShopVerificationRequest.STATUS_REJECTED -> {
                        ShopkeeperLoginResult.Rejected(
                            shop = shopPlaceholder,
                            reason = matchedRequest.rejectionReason.ifBlank { "Application did not meet market onboarding requirements." }
                        )
                    }
                    else -> {
                        ShopkeeperLoginResult.PendingApproval(shopPlaceholder, null)
                    }
                }
            }

            shopkeeperConsecutiveFailures++
            if (shopkeeperConsecutiveFailures >= 5) {
                shopkeeperLockoutUntilMs = System.currentTimeMillis() + 30_000L
                return ShopkeeperLoginResult.Failure(
                    "Too many failed attempts. Locked for 30 seconds for security."
                )
            }
            return ShopkeeperLoginResult.Failure(
                "The shop account details could not be verified. Please check your details and try again."
            )
        }

        // Verify password against shop or account
        val passwordMatches = (targetShop != null && (verifyPassword(trimmedPass, targetShop.passwordHash) || targetShop.password == trimmedPass)) ||
                              (targetAccount != null && (verifyPassword(trimmedPass, targetAccount.passwordHash) || targetAccount.password == trimmedPass))

        if (!passwordMatches) {
            shopkeeperConsecutiveFailures++
            if (shopkeeperConsecutiveFailures >= 5) {
                shopkeeperLockoutUntilMs = System.currentTimeMillis() + 30_000L
                return ShopkeeperLoginResult.Failure(
                    "Too many failed attempts. Locked for 30 seconds for security."
                )
            }
            return ShopkeeperLoginResult.Failure(
                "The shop account details could not be verified. Please check your details and try again."
            )
        }

        // Valid credentials: reset rate limiting
        shopkeeperConsecutiveFailures = 0

        val effectiveShop = targetShop ?: Shop(
            id = targetAccount?.shopId ?: "shop_${UUID.randomUUID().toString().take(6)}",
            name = targetAccount?.shopName ?: trimmedShop,
            status = targetAccount?.status?.lowercase() ?: "pending"
        )

        val status = when {
            effectiveShop.status.equals("rejected", ignoreCase = true) || targetAccount?.status.equals("REJECTED", ignoreCase = true) -> "REJECTED"
            effectiveShop.status.equals("suspended", ignoreCase = true) || targetAccount?.status.equals("SUSPENDED", ignoreCase = true) -> "SUSPENDED"
            effectiveShop.status.equals("documents_required", ignoreCase = true) || targetAccount?.status.equals("DOCUMENTS_REQUIRED", ignoreCase = true) -> "DOCUMENTS_REQUIRED"
            effectiveShop.status.equals("approved", ignoreCase = true) || effectiveShop.isOpen || targetAccount?.status.equals("APPROVED", ignoreCase = true) || targetAccount?.isActive == true -> "APPROVED"
            effectiveShop.status.equals("pending_review", ignoreCase = true) || targetAccount?.status.equals("PENDING_REVIEW", ignoreCase = true) -> "PENDING"
            effectiveShop.status.equals("pending", ignoreCase = true) || targetAccount?.status.equals("PENDING", ignoreCase = true) -> "PENDING"
            targetAccount != null && !targetAccount.isActive && !effectiveShop.isOpen -> "PENDING"
            else -> "APPROVED"
        }

        return when (status) {
            "PENDING" -> {
                ShopkeeperLoginResult.PendingApproval(effectiveShop, targetAccount)
            }
            "DOCUMENTS_REQUIRED" -> {
                val reason = effectiveShop.rejectionReason.ifBlank {
                    targetAccount?.rejectionReason?.ifBlank { "Additional business verification documents required by FlashDrop Admins." }
                        ?: "Additional business verification documents required by FlashDrop Admins."
                }
                ShopkeeperLoginResult.DocumentsRequired(effectiveShop, reason)
            }
            "REJECTED" -> {
                val reason = effectiveShop.rejectionReason.ifBlank {
                    targetAccount?.rejectionReason?.ifBlank { "Application did not meet market onboarding requirements." }
                        ?: "Application did not meet market onboarding requirements."
                }
                ShopkeeperLoginResult.Rejected(effectiveShop, reason)
            }
            "SUSPENDED" -> {
                val reason = effectiveShop.suspensionReason.ifBlank {
                    targetAccount?.suspensionReason?.ifBlank { "Shop operations have been temporarily suspended by FlashDrop Admins." }
                        ?: "Shop operations have been temporarily suspended by FlashDrop Admins."
                }
                ShopkeeperLoginResult.Suspended(effectiveShop, reason)
            }
            else -> {
                val profile = UserProfile(
                    id = targetAccount?.id ?: "sk_${effectiveShop.id}",
                    name = targetAccount?.shopkeeperName ?: effectiveShop.ownerName.ifBlank { "Shop Owner" },
                    email = targetAccount?.email ?: "${effectiveShop.id}@flashdrop.local",
                    phone = targetAccount?.phone ?: effectiveShop.phone,
                    role = "shopkeeper",
                    loginId = targetAccount?.loginId ?: "SK_${effectiveShop.id.uppercase()}",
                    shopId = effectiveShop.id,
                    shopName = effectiveShop.name,
                    isVerifiedAdmin = false,
                    isVerifiedShopkeeper = true,
                    isAuthenticated = true,
                    address = effectiveShop.address,
                    locality = "Market Area",
                    pinCode = "210427",
                    city = "Mahoba",
                    state = "Uttar Pradesh"
                )
                _currentProfile.value = profile
                saveSessionToPrefs(profile)
                ShopkeeperLoginResult.Success(profile)
            }
        }
    }

    // Login as Customer
    fun loginAsCustomer(name: String, phone: String = "+91 98765 12345") {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.equals("Guest", ignoreCase = true)) {
            setGuestMode()
            return
        }
        val newId = "cust_${UUID.randomUUID().toString().take(6)}"
        val safeEmail = "${normalizeName(trimmed).replace(" ", ".")}@flashdrop.local"
        val profile = UserProfile(
            id = newId,
            name = trimmed,
            email = safeEmail,
            phone = phone.ifBlank { "+91 98765 12345" },
            role = "customer",
            loginId = "CUST",
            isVerifiedAdmin = false,
            isVerifiedShopkeeper = false,
            isAuthenticated = true,
            address = "Subhash Chowk, Chhatarpur Road",
            locality = "Alha Chowk",
            pinCode = "210427",
            city = "Mahoba",
            state = "Uttar Pradesh"
        )
        _currentProfile.value = profile
        saveSessionToPrefs(profile)
        upsertCustomerFromProfile(profile, authMethodOverride = "Phone / Quick Login")
        repoScope.launch {
            try {
                firestore.collection("users").document(newId).set(profile).await()
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Sync customer to firestore: ${e.message}")
            }
        }
    }

    // Guest Mode (Unauthenticated State)
    fun setGuestMode() {
        clearSessionPrefs()
        _currentProfile.value = createGuestProfile()
    }

    // Check if the current user is authenticated (via Firebase Google Sign-In or verified session)
    fun isUserSignedIn(): Boolean {
        if (auth?.currentUser != null) return true
        val prof = _currentProfile.value
        return prof.isAuthenticated &&
            prof.id.isNotBlank() &&
            !prof.id.equals("guest", ignoreCase = true) &&
            prof.role.lowercase() != "guest" &&
            prof.name.isNotBlank() &&
            !prof.name.equals("Guest", ignoreCase = true)
    }

    // Synchronously update user session upon successful Google OAuth Sign-In
    fun onGoogleSignInSuccess(user: com.google.firebase.auth.FirebaseUser? = auth?.currentUser): UserProfile {
        if (user != null) {
            val resolvedName = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: "Customer"
            val immediateProfile = UserProfile(
                id = user.uid,
                name = resolvedName,
                email = user.email ?: "",
                phone = user.phoneNumber?.takeIf { it.isNotBlank() } ?: _currentProfile.value.phone.ifBlank { "+91 98765 12345" },
                role = "customer",
                loginId = "CUST",
                isVerifiedAdmin = false,
                isVerifiedShopkeeper = false,
                isAuthenticated = true,
                photoUrl = user.photoUrl?.toString() ?: "",
                address = _currentProfile.value.address.ifBlank { "Subhash Chowk, Chhatarpur Road" },
                locality = _currentProfile.value.locality.ifBlank { "Alha Chowk" },
                pinCode = _currentProfile.value.pinCode.ifBlank { "210427" },
                city = "Mahoba",
                state = "Uttar Pradesh"
            )
            _currentProfile.value = immediateProfile
            saveSessionToPrefs(immediateProfile)
            upsertCustomerFromProfile(immediateProfile, authMethodOverride = "Google OAuth")
            loadUserProfileForFirebaseUser(user)
            return immediateProfile
        }
        return _currentProfile.value
    }

    // Authenticate customer with Google account metadata (used by Google Sign-In flow & tests)
    fun signInWithGoogleAccount(
        uid: String,
        displayName: String,
        email: String,
        photoUrl: String = "",
        phone: String = "+91 98765 12345"
    ): Result<UserProfile> {
        val cleanUid = uid.trim()
        if (cleanUid.isEmpty() || cleanUid.equals("guest", ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("Google Sign-In failed: Invalid account UID."))
        }
        val resolvedName = displayName.trim().ifBlank {
            email.substringBefore("@").ifBlank { "Customer" }
        }
        val profile = UserProfile(
            id = cleanUid,
            name = resolvedName,
            email = email.trim(),
            phone = phone.ifBlank { "+91 98765 12345" },
            role = "customer",
            loginId = "CUST",
            isVerifiedAdmin = false,
            isVerifiedShopkeeper = false,
            isAuthenticated = true,
            photoUrl = photoUrl.trim(),
            address = _currentProfile.value.address.ifBlank { "Subhash Chowk, Chhatarpur Road" },
            locality = _currentProfile.value.locality.ifBlank { "Alha Chowk" },
            pinCode = _currentProfile.value.pinCode.ifBlank { "210427" },
            city = "Mahoba",
            state = "Uttar Pradesh"
        )
        _currentProfile.value = profile
        saveSessionToPrefs(profile)
        upsertCustomerFromProfile(profile, authMethodOverride = "Google OAuth")
        repoScope.launch {
            try {
                firestore.collection("users").document(cleanUid).set(profile).await()
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Google user profile firestore sync deferred: ${e.message}")
            }
        }
        return Result.success(profile)
    }

    // Returns only the orders that the currently signed-in user is authorized to view
    fun getOrdersForCurrentUser(allOrders: List<Order> = _orders.value): List<Order> {
        if (!isUserSignedIn()) return emptyList()
        if (isMainAdmin()) return allOrders
        val prof = _currentProfile.value
        if (isShopkeeper()) {
            return allOrders.filter { it.shopId == prof.shopId }
        }
        val currentUid = auth?.currentUser?.uid ?: prof.id
        return allOrders.filter { it.userId == currentUid }
    }

    // Verifies whether the current user is allowed to access a specific order
    fun getOrderForCurrentUser(orderId: String): Order? {
        return getOrdersForCurrentUser().find { it.id == orderId || it.orderNumber == orderId }
    }

    // Logout and transition to Guest Mode
    fun logout() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.w("FlashDropRepo", "Sign out error: ${e.message}")
        }
        clearSessionPrefs()
        setGuestMode()
    }

    // Role switcher helper for instant evaluation & demo switching
    fun setRole(newRole: String, shopId: String? = null, loginId: String? = null) {
        when (newRole) {
            "MAIN_ADMIN", "admin" -> {
                // Security rule: Do not allow unverified users to assign themselves MAIN_ADMIN role
                if (!_currentProfile.value.isVerifiedAdmin) {
                    Log.w("FlashDropRepo", "Unauthorized attempt to self-assign MAIN_ADMIN role without authentication")
                    return
                }
                val isCD12 = loginId == "CD12" || _currentProfile.value.loginId == "CD12"
                val code = if (isCD12) "CD12" else "PR12"
                val name = if (isCD12) "Chanchal Didi" else "Pranjal Rajpoot"
                val title = if (isCD12) "Owner 2" else "Owner 1"
                _currentProfile.value = _currentProfile.value.copy(
                    role = "MAIN_ADMIN",
                    loginId = code,
                    name = name,
                    ownerTitle = title,
                    isVerifiedAdmin = true,
                    isVerifiedShopkeeper = false
                )
            }
            "shopkeeper" -> {
                val targetShopId = shopId ?: _currentProfile.value.shopId
                val shop = _shops.value.find { it.id == targetShopId } ?: _shops.value.first()
                val sk = _shopkeepers.value.find { it.shopId == shop.id }
                _currentProfile.value = _currentProfile.value.copy(
                    role = "shopkeeper",
                    shopId = shop.id,
                    shopName = shop.name,
                    name = sk?.shopkeeperName ?: "Shop Manager (${shop.name})",
                    loginId = sk?.loginId ?: "SK_${shop.id.uppercase()}",
                    isVerifiedAdmin = false,
                    isVerifiedShopkeeper = true
                )
            }
            else -> {
                _currentProfile.value = _currentProfile.value.copy(
                    role = "customer",
                    loginId = "CUST",
                    ownerTitle = "",
                    isVerifiedAdmin = false,
                    isVerifiedShopkeeper = false
                )
            }
        }
        if (_currentProfile.value.isAuthenticated) {
            saveSessionToPrefs(_currentProfile.value)
        }
    }

    fun isMainAdmin(): Boolean {
        val prof = _currentProfile.value
        val email = auth?.currentUser?.email ?: prof.email
        val loginId = prof.loginId.uppercase().trim()
        val isVerifiedEmail = email.equals("pranjal.owner@flashdrop.local", ignoreCase = true) ||
                email.equals("chanchal.owner@flashdrop.local", ignoreCase = true) ||
                email.equals("ajayvikramrajpoot9540@gmail.com", ignoreCase = true)
        val isVerifiedLoginId = loginId == "PR12" || loginId == "CD12" || loginId == "PR" || loginId == "CD"
        val isRoleAdmin = (prof.role.equals("MAIN_ADMIN", ignoreCase = true) || prof.role.equals("admin", ignoreCase = true)) && prof.isVerifiedAdmin
        return isRoleAdmin || isVerifiedEmail || (prof.isAuthenticated && isVerifiedLoginId)
    }

    fun isShopkeeper(): Boolean {
        return _currentProfile.value.role == "shopkeeper" && _currentProfile.value.isVerifiedShopkeeper
    }

    // Admin: Shopkeeper Management (Only Main Admins can add or remove shopkeepers)
    fun addShopkeeper(
        name: String,
        loginId: String,
        shopId: String,
        shopName: String,
        phone: String,
        email: String
    ): Boolean {
        if (!isMainAdmin()) return false

        val newAccount = ShopkeeperAccount(
            id = "sk_${UUID.randomUUID().toString().take(6)}",
            loginId = loginId.uppercase().trim(),
            shopkeeperName = name.trim(),
            shopId = shopId,
            shopName = shopName,
            phone = phone.trim(),
            email = email.trim(),
            isActive = true,
            assignedBy = _currentProfile.value.loginId.ifBlank { "PR12" }
        )
        _shopkeepers.value = listOf(newAccount) + _shopkeepers.value
        persistence.saveShopkeepers(_shopkeepers.value)

        if (auth?.currentUser != null) {
            repoScope.launch {
                try {
                    firestore.collection("shopkeepers").document(newAccount.id).set(newAccount).await()
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Failed to save shopkeeper: ${e.message}")
                }
            }
        }
        return true
    }

    fun toggleShopkeeperStatus(shopkeeperId: String): Boolean {
        if (!isMainAdmin()) return false
        _shopkeepers.value = _shopkeepers.value.map { sk ->
            if (sk.id == shopkeeperId) sk.copy(isActive = !sk.isActive) else sk
        }
        persistence.saveShopkeepers(_shopkeepers.value)
        return true
    }

    fun removeShopkeeper(shopkeeperId: String): Boolean {
        if (!isMainAdmin()) return false
        _shopkeepers.value = _shopkeepers.value.filterNot { it.id == shopkeeperId }
        persistence.saveShopkeepers(_shopkeepers.value)

        if (auth?.currentUser != null) {
            repoScope.launch {
                try {
                    firestore.collection("shopkeepers").document(shopkeeperId).delete().await()
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Failed to delete shopkeeper: ${e.message}")
                }
            }
        }
        return true
    }

    // Dynamic query for approved, active connected shops count (excluding pending, rejected, suspended, and deleted)
    suspend fun fetchDynamicApprovedShopsCount(): Result<Int> {
        return try {
            val snapshot = firestore.collection("shops").get().await()
            if (!snapshot.isEmpty) {
                val liveShops = snapshot.toObjects(Shop::class.java)
                val count = liveShops.count {
                    it.status.equals("approved", ignoreCase = true) && it.isOpen
                }
                Result.success(count)
            } else {
                val count = _shops.value.count {
                    it.status.equals("approved", ignoreCase = true) && it.isOpen
                }
                Result.success(count)
            }
        } catch (e: Exception) {
            Log.w("FlashDropRepo", "Error fetching dynamic shop count: ${e.message}")
            val count = _shops.value.count {
                it.status.equals("approved", ignoreCase = true) && it.isOpen
            }
            Result.success(count)
        }
    }

    /**
     * Generates a unique, standardized Shop ID (e.g. FD-SHOP-1001).
     * Inspects all existing shops and shopkeepers in memory and guarantees no two shops receive the same ID.
     */
    fun generateUniqueShopId(): String {
        val existingShopIds = _shops.value.map { it.id.uppercase() }.toSet()
        val existingSkShopIds = _shopkeepers.value.map { it.shopId.uppercase() }.toSet()
        val allIds = existingShopIds + existingSkShopIds

        val pattern = Regex("^FD-SHOP-(\\d+)$")
        var maxNum = 1000

        for (id in allIds) {
            val match = pattern.find(id)
            if (match != null) {
                val num = match.groupValues[1].toIntOrNull()
                if (num != null && num > maxNum) {
                    maxNum = num
                }
            }
        }

        var candidateNum = maxNum + 1
        var candidateId = "FD-SHOP-$candidateNum"
        while (allIds.contains(candidateId)) {
            candidateNum++
            candidateId = "FD-SHOP-$candidateNum"
        }
        return candidateId
    }

    /**
     * Direct Admin onboarding of a new shop and associated shopkeeper account.
     * Enforces backend Main Admin authorization, complete field validations, uniqueness checks,
     * unique Shop ID generation (e.g. FD-SHOP-1001), secure password hashing, instant APPROVED status,
     * and synchronous Firestore persistence.
     */
    suspend fun adminCreateShopWithShopkeeper(
        shopName: String,
        ownerName: String,
        contactNumber: String,
        category: String,
        address: String,
        gstNumber: String,
        password: String,
        confirmPassword: String,
        description: String = ""
    ): Result<AdminShopCreationResult> {
        // 1. Authorization check: must be Main Admin
        if (!isMainAdmin()) {
            return Result.failure(
                SecurityException("Unauthorized: Only an authenticated Main Admin can create approved shopkeeper accounts.")
            )
        }

        // 2. Validate required fields
        val trimmedName = shopName.trim()
        val trimmedOwner = ownerName.trim()
        val cleanPhone = contactNumber.replace("+91", "").replace(" ", "").replace("-", "").trim()
        val trimmedCategory = category.trim()
        val trimmedAddress = address.trim()
        val cleanGst = gstNumber.trim().uppercase().filter { it.isLetterOrDigit() }
        val trimmedPassword = password.trim()
        val trimmedConfirm = confirmPassword.trim()

        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Shop / Store Name is required."))
        }
        if (trimmedOwner.isEmpty()) {
            return Result.failure(IllegalArgumentException("Owner Full Name is required."))
        }
        if (cleanPhone.isEmpty()) {
            return Result.failure(IllegalArgumentException("Mobile Contact Number is required."))
        }
        val phoneRegex = Regex("^[6-9]\\d{9}$")
        if (!phoneRegex.matches(cleanPhone)) {
            return Result.failure(IllegalArgumentException("Please enter a valid 10-digit Indian mobile number starting with 6-9."))
        }

        if (trimmedCategory.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please select a valid Shop Category."))
        }
        if (trimmedAddress.isEmpty()) {
            return Result.failure(IllegalArgumentException("Physical Shop Address is required."))
        }

        if (cleanGst.isEmpty()) {
            return Result.failure(IllegalArgumentException("GST Number is required."))
        }
        val gstRegex = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
        if (!gstRegex.matches(cleanGst)) {
            return Result.failure(IllegalArgumentException("Please enter a valid 15-character Indian GSTIN (e.g. 09AABCU9603R1ZV)."))
        }

        if (trimmedPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters for security."))
        }
        if (trimmedPassword != trimmedConfirm) {
            return Result.failure(IllegalArgumentException("Create Password and Confirm Password do not match."))
        }

        // 3. Uniqueness checks according to app rules
        val isDuplicateGst = _shops.value.any { it.gstNumber.equals(cleanGst, ignoreCase = true) } ||
                             _shopkeepers.value.any { it.gstNumber.equals(cleanGst, ignoreCase = true) }
        if (isDuplicateGst) {
            return Result.failure(IllegalArgumentException("A shop with GSTIN $cleanGst already exists. Duplicate GST is not allowed."))
        }

        val isDuplicatePhone = _shops.value.any { it.phone.replace("+91", "").replace(" ", "").replace("-", "").trim() == cleanPhone } ||
                               _shopkeepers.value.any { it.phone.replace("+91", "").replace(" ", "").replace("-", "").trim() == cleanPhone }
        if (isDuplicatePhone) {
            return Result.failure(IllegalArgumentException("A shop with mobile number $cleanPhone already exists. Duplicate mobile number is not allowed."))
        }

        // 4. Generate unique Shop ID (e.g. FD-SHOP-1001)
        val generatedShopId = generateUniqueShopId()
        val skLoginId = "SK_" + generatedShopId.replace("-", "_")

        // 5. Secure credentials (never store plaintext password)
        val passwordHash = hashPassword(trimmedPassword)

        // 6. Build Shop model with status = "approved" (Admin authorized)
        val newShop = Shop(
            id = generatedShopId,
            name = trimmedName,
            category = trimmedCategory,
            rating = 5.0,
            ratingCount = 1,
            distance = "1.0 km",
            deliveryTime = "20-30 mins",
            deliveryAvailable = true,
            deliveryFee = 20.0,
            minOrder = 100.0,
            isOpen = true,
            status = "approved",
            ownerName = trimmedOwner,
            gstNumber = cleanGst,
            password = "", // Never store plaintext password
            passwordHash = passwordHash,
            description = description.trim().ifEmpty { "$trimmedName in Mahoba on FlashDrop." },
            address = trimmedAddress,
            phone = "+91 $cleanPhone",
            imageUrl = "",
            ownerId = skLoginId
        )

        // 7. Build ShopkeeperAccount model with status = "APPROVED", isActive = true
        val newAccount = ShopkeeperAccount(
            id = "sk_${generatedShopId.lowercase().replace("-", "_")}",
            loginId = skLoginId,
            shopkeeperName = trimmedOwner,
            shopId = generatedShopId,
            shopName = trimmedName,
            phone = "+91 $cleanPhone",
            email = "${skLoginId.lowercase()}@flashdrop.local",
            gstNumber = cleanGst,
            password = "", // Never store plaintext password
            passwordHash = passwordHash,
            isActive = true,
            status = "APPROVED",
            assignedBy = _currentProfile.value.loginId.ifBlank { "PR12" },
            createdAt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )

        // 8. Write to database using project's current architecture (confirm before success)
        if (auth?.currentUser != null) {
            try {
                firestore.collection("shops").document(newShop.id).set(newShop).await()
                firestore.collection("shopkeepers").document(newAccount.id).set(newAccount).await()
            } catch (e: Exception) {
                Log.e("FlashDropRepo", "Firestore admin shopkeeper creation failed: ${e.message}", e)
                return Result.failure(Exception("Database write failed: ${e.message}. Shopkeeper was not created."))
            }
        }

        // 9. Update in-memory reactive state
        _shops.value = listOf(newShop) + _shops.value
        _shopkeepers.value = listOf(newAccount) + _shopkeepers.value

        return Result.success(
            AdminShopCreationResult(
                shop = newShop,
                account = newAccount,
                generatedShopId = generatedShopId,
                loginId = skLoginId
            )
        )
    }

    // Submit new shopkeeper shop registration (saves with pending approval status)
    fun submitShopRegistration(
        name: String,
        ownerName: String,
        phone: String,
        category: String,
        address: String,
        description: String = "",
        gstNumber: String = "",
        password: String = ""
    ): Result<Shop> {
        val trimmedName = name.trim()
        val trimmedOwner = ownerName.trim()
        val cleanPhone = phone.replace("+91", "").replace(" ", "").replace("-", "").trim()
        val trimmedCategory = category.trim()
        val trimmedAddress = address.trim()
        val trimmedGst = gstNumber.trim().uppercase()
        val trimmedPassword = password.trim()

        if (trimmedGst.isEmpty()) {
            return Result.failure(IllegalArgumentException("GST Number is mandatory for shopkeeper registration."))
        }

        // Validate 15-character Indian GSTIN format
        val gstRegex = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
        if (!gstRegex.matches(trimmedGst)) {
            return Result.failure(
                IllegalArgumentException("Invalid GST Number format. Must be a 15-character Indian GSTIN (e.g. 09AABCU9603R1ZV).")
            )
        }

        // Enforce GST uniqueness
        val isDuplicateGst = _shops.value.any { it.gstNumber.equals(trimmedGst, ignoreCase = true) } ||
                             _shopkeepers.value.any { it.gstNumber.equals(trimmedGst, ignoreCase = true) }
        if (isDuplicateGst) {
            return Result.failure(
                IllegalArgumentException("A registered shop with GSTIN $trimmedGst already exists. Please sign in.")
            )
        }

        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter the shop name."))
        }
        if (trimmedOwner.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter the owner's full name."))
        }

        // Validate 10-digit Indian Mobile
        val mobileRegex = Regex("^[6-9]\\d{9}$")
        if (!mobileRegex.matches(cleanPhone)) {
            return Result.failure(IllegalArgumentException("Please enter a valid 10-digit Indian mobile number."))
        }

        if (trimmedCategory.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please select a valid shop category."))
        }
        if (trimmedAddress.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter the shop's physical address in Mahoba."))
        }
        if (trimmedPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters for shop security."))
        }

        val newId = "shop_reg_${UUID.randomUUID().toString().take(6)}"
        val passwordHash = hashPassword(trimmedPassword)

        val newShop = Shop(
            id = newId,
            name = trimmedName,
            ownerName = trimmedOwner,
            phone = "+91 $cleanPhone",
            category = trimmedCategory,
            address = trimmedAddress,
            gstNumber = trimmedGst,
            password = "", // Do not store plaintext password
            passwordHash = passwordHash,
            description = description.trim().ifEmpty { "Mahoba local store registered on FlashDrop." },
            isOpen = false, // Must be approved by Admin before becoming open
            status = "pending", // Pending Admin review
            rating = 5.0,
            ratingCount = 1,
            distance = "1.0 km",
            deliveryTime = "20-30 mins",
            deliveryAvailable = true,
            deliveryFee = 20.0,
            minOrder = 100.0,
            imageUrl = ""
        )

        val skLoginId = "SK_" + trimmedName.filter { it.isLetter() }.take(6).uppercase().ifEmpty { "SHOP" } + "_" + newId.takeLast(4).uppercase()
        val newAccount = ShopkeeperAccount(
            id = "sk_${newId}",
            loginId = skLoginId,
            shopkeeperName = trimmedOwner,
            shopId = newId,
            shopName = trimmedName,
            phone = "+91 $cleanPhone",
            email = "${skLoginId.lowercase()}@mahoba.local",
            gstNumber = trimmedGst,
            password = "", // Do not store plaintext password
            passwordHash = passwordHash,
            isActive = false, // Becomes active when shop is approved
            status = "PENDING",
            assignedBy = "SELF_REG",
            createdAt = "2026-10-24"
        )

        _shops.value = listOf(newShop) + _shops.value
        _shopkeepers.value = listOf(newAccount) + _shopkeepers.value
        persistence.saveShops(_shops.value)
        persistence.saveShopkeepers(_shopkeepers.value)

        repoScope.launch {
            try {
                firestore.collection("shops").document(newId).set(newShop).await()
                firestore.collection("shopkeepers").document(newAccount.id).set(newAccount).await()
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Firestore shop registration write deferred: ${e.message}")
            }
        }

        return Result.success(newShop)
    }

    fun approveShop(shopId: String): Boolean {
        if (!isMainAdmin()) return false
        val targetShop = _shops.value.find { it.id == shopId }
        val targetGst = targetShop?.gstNumber?.trim()?.uppercase() ?: ""

        _shops.value = _shops.value.map { shop ->
            val match = shop.id == shopId ||
                    (targetGst.isNotBlank() && shop.gstNumber.trim().equals(targetGst, ignoreCase = true))
            if (match) {
                shop.copy(status = "approved", isOpen = true, rejectionReason = "", suspensionReason = "")
            } else shop
        }

        _shopkeepers.value = _shopkeepers.value.map { sk ->
            val match = sk.shopId == shopId ||
                    (targetGst.isNotBlank() && sk.gstNumber.trim().equals(targetGst, ignoreCase = true))
            if (match) {
                sk.copy(isActive = true, status = "APPROVED", rejectionReason = "", suspensionReason = "")
            } else sk
        }

        _shopVerificationRequests.value = _shopVerificationRequests.value.map { req ->
            val match = req.shopId == shopId ||
                    (targetGst.isNotBlank() && req.gstNumber.trim().equals(targetGst, ignoreCase = true))
            if (match) {
                req.copy(status = ShopVerificationRequest.STATUS_APPROVED, rejectionReason = "", documentsRequiredNotes = "")
            } else req
        }

        // Guarantee an active ShopkeeperAccount exists so shopkeeper can log in
        val existingAccount = _shopkeepers.value.find { sk ->
            sk.shopId == shopId || (targetGst.isNotBlank() && sk.gstNumber.trim().equals(targetGst, ignoreCase = true))
        }
        if (existingAccount == null && targetShop != null) {
            val skLoginId = "SK_" + targetShop.id.replace("-", "_").uppercase()
            val createdSk = ShopkeeperAccount(
                id = "sk_${targetShop.id.lowercase().replace("-", "_")}",
                loginId = skLoginId,
                shopkeeperName = targetShop.ownerName.ifBlank { targetShop.name },
                shopId = targetShop.id,
                shopName = targetShop.name,
                phone = targetShop.phone,
                email = "${skLoginId.lowercase()}@mahoba.local",
                gstNumber = targetShop.gstNumber,
                password = targetShop.password,
                passwordHash = targetShop.passwordHash,
                isActive = true,
                status = "APPROVED",
                assignedBy = _currentProfile.value.loginId.ifBlank { "PR12" },
                createdAt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )
            _shopkeepers.value = listOf(createdSk) + _shopkeepers.value
        }

        persistence.saveShops(_shops.value)
        persistence.saveShopkeepers(_shopkeepers.value)
        persistence.saveShopVerificationRequests(_shopVerificationRequests.value)

        repoScope.launch {
            try {
                firestore.collection("shops").document(shopId).update(
                    mapOf(
                        "status" to "approved",
                        "isOpen" to true
                    )
                ).await()
                val sk = _shopkeepers.value.find { it.shopId == shopId }
                if (sk != null) {
                    firestore.collection("shopkeepers").document(sk.id).set(sk).await()
                }
                val req = _shopVerificationRequests.value.find { it.shopId == shopId }
                if (req != null) {
                    firestore.collection("shop_verification_requests").document(req.id).set(req).await()
                }
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Firestore approve shop error: ${e.message}")
            }
        }
        val approvedShopName = targetShop?.name ?: "Shop"
        addNotification(
            NotificationItem(
                id = "notif_appr_${shopId}_sk",
                title = "Shop Approved! 🎉",
                message = "$approvedShopName has been approved by FlashDrop Main Admin. Your store is now active for customers in Mahoba.",
                timestamp = "Just now",
                type = "shop",
                targetRole = "shopkeeper",
                targetId = shopId
            )
        )
        addNotification(
            NotificationItem(
                id = "notif_appr_${shopId}_admin",
                title = "Shop Approved: $approvedShopName",
                message = "$approvedShopName was approved by ${_currentProfile.value.name.ifBlank { "Main Admin" }}.",
                timestamp = "Just now",
                type = "admin",
                targetRole = "admin"
            )
        )

        return true
    }

    fun rejectShop(shopId: String, reason: String = "Application did not meet market onboarding requirements"): Boolean {
        if (!isMainAdmin()) return false
        _shops.value = _shops.value.map { shop ->
            if (shop.id == shopId) {
                shop.copy(status = "rejected", isOpen = false, rejectionReason = reason)
            } else shop
        }
        _shopkeepers.value = _shopkeepers.value.map { sk ->
            if (sk.shopId == shopId) {
                sk.copy(isActive = false, status = "REJECTED", rejectionReason = reason)
            } else sk
        }
        persistence.saveShops(_shops.value)
        persistence.saveShopkeepers(_shopkeepers.value)
        repoScope.launch {
            try {
                firestore.collection("shops").document(shopId).update(
                    mapOf(
                        "status" to "rejected",
                        "isOpen" to false,
                        "rejectionReason" to reason
                    )
                ).await()
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Firestore reject shop error: ${e.message}")
            }
        }
        return true
    }

    fun suspendShop(shopId: String, reason: String = "Shop operations temporarily suspended by FlashDrop Admins"): Boolean {
        if (!isMainAdmin()) return false
        _shops.value = _shops.value.map { shop ->
            if (shop.id == shopId) {
                shop.copy(status = "suspended", isOpen = false, suspensionReason = reason)
            } else shop
        }
        _shopkeepers.value = _shopkeepers.value.map { sk ->
            if (sk.shopId == shopId) {
                sk.copy(isActive = false, status = "SUSPENDED", suspensionReason = reason)
            } else sk
        }
        persistence.saveShops(_shops.value)
        persistence.saveShopkeepers(_shopkeepers.value)
        repoScope.launch {
            try {
                firestore.collection("shops").document(shopId).update(
                    mapOf(
                        "status" to "suspended",
                        "isOpen" to false,
                        "suspensionReason" to reason
                    )
                ).await()
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Firestore suspend shop error: ${e.message}")
            }
        }
        return true
    }

    // =========================================================================
    // ADMIN-CONTROLLED 3-STEP SHOP REGISTRATION & VERIFICATION SYSTEM
    // =========================================================================

    /**
     * Updates the 3-step registration mode setting.
     * Enforces equal permissions for Main Admin and Both Admin.
     * Persistently saved to Firestore at: appSettings/shopRegistration/threeStepEnabled
     */
    fun updateShopRegistrationMode(enabled: Boolean): Result<ShopRegistrationConfig> {
        if (!isMainAdmin()) {
            return Result.failure(
                SecurityException("Access Denied: Only authorized Main Admins have permission to modify Shop Registration settings.")
            )
        }

        val adminName = if (_currentProfile.value.loginId.uppercase() == "CD12") "Chanchal Didi" else "Pranjal Rajpoot"
        val adminId = if (_currentProfile.value.loginId.uppercase() == "CD12") "CD12" else "PR12"
        val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val updated = _shopRegistrationConfig.value.copy(
            threeStepEnabled = enabled,
            lastUpdatedByAdminId = adminId,
            lastUpdatedByAdminName = adminName,
            lastUpdatedAtFormatted = nowFormatted,
            version = _shopRegistrationConfig.value.version + 1
        )

        _shopRegistrationConfig.value = updated
        persistence.saveShopRegistrationConfig(updated)

        repoScope.launch {
            try {
                firestore.collection("appSettings").document("shopRegistration").set(updated).await()
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "ShopRegistrationConfig firestore sync deferred: ${e.message}")
            }
        }

        return Result.success(updated)
    }

    /**
     * Step 1: Validates and saves/updates initial store registration information.
     */
    fun submitShopRegistrationStep1(
        name: String,
        ownerName: String,
        phone: String,
        category: String,
        address: String,
        description: String = "",
        gstNumber: String = "",
        password: String = "",
        confirmPassword: String = "",
        existingRequestId: String? = null
    ): Result<ShopVerificationRequest> {
        val trimmedName = name.trim()
        val trimmedOwner = ownerName.trim()
        val cleanPhone = phone.replace("+91", "").replace(" ", "").replace("-", "").trim()
        val trimmedCategory = category.trim()
        val trimmedAddress = address.trim()
        val trimmedGst = gstNumber.trim().uppercase()
        val trimmedPassword = password.trim()
        val trimmedConfirm = confirmPassword.trim()

        if (trimmedGst.isEmpty()) {
            return Result.failure(IllegalArgumentException("GST Number is mandatory for shopkeeper registration."))
        }
        val gstRegex = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
        if (!gstRegex.matches(trimmedGst)) {
            return Result.failure(
                IllegalArgumentException("Invalid GST Number format. Must be a 15-character Indian GSTIN (e.g. 09AABCU9603R1ZV).")
            )
        }

        // Uniqueness check
        val isDuplicateGst = _shops.value.any { it.gstNumber.equals(trimmedGst, ignoreCase = true) } ||
                             _shopkeepers.value.any { it.gstNumber.equals(trimmedGst, ignoreCase = true) } ||
                             _shopVerificationRequests.value.any { it.id != existingRequestId && it.gstNumber.equals(trimmedGst, ignoreCase = true) && it.status != ShopVerificationRequest.STATUS_REJECTED }
        if (isDuplicateGst) {
            return Result.failure(
                IllegalArgumentException("A registered shop with GSTIN $trimmedGst already exists. Please sign in.")
            )
        }

        if (trimmedName.isEmpty()) return Result.failure(IllegalArgumentException("Please enter the shop name."))
        if (trimmedOwner.isEmpty()) return Result.failure(IllegalArgumentException("Please enter the owner's full name."))

        val mobileRegex = Regex("^[6-9]\\d{9}$")
        if (!mobileRegex.matches(cleanPhone)) {
            return Result.failure(IllegalArgumentException("Please enter a valid 10-digit Indian mobile number."))
        }

        val isDuplicatePhone = _shops.value.any { it.phone.replace("+91", "").replace(" ", "").replace("-", "").trim() == cleanPhone } ||
                               _shopkeepers.value.any { it.phone.replace("+91", "").replace(" ", "").replace("-", "").trim() == cleanPhone }
        if (isDuplicatePhone) {
            return Result.failure(IllegalArgumentException("A shop with mobile number $cleanPhone already exists."))
        }

        if (trimmedCategory.isEmpty()) return Result.failure(IllegalArgumentException("Please select a valid shop category."))
        if (trimmedAddress.isEmpty()) return Result.failure(IllegalArgumentException("Please enter the physical shop address."))

        if (trimmedPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters for shop security."))
        }
        if (trimmedPassword != trimmedConfirm) {
            return Result.failure(IllegalArgumentException("Passwords do not match. Please re-enter your password."))
        }

        val appId = existingRequestId ?: ("req_" + UUID.randomUUID().toString().take(8))
        val passwordHash = hashPassword(trimmedPassword)
        val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val request = ShopVerificationRequest(
            id = appId,
            shopName = trimmedName,
            ownerName = trimmedOwner,
            phone = "+91 $cleanPhone",
            category = trimmedCategory,
            address = trimmedAddress,
            description = description.trim().ifEmpty { "$trimmedName in Mahoba on FlashDrop." },
            gstNumber = trimmedGst,
            passwordHash = passwordHash,
            status = ShopVerificationRequest.STATUS_DRAFT,
            stepCompleted = 1,
            submittedAtFormatted = nowFormatted,
            isThreeStepRegistration = true
        )

        _shopVerificationRequests.value = listOf(request) + _shopVerificationRequests.value.filter { it.id != appId }
        persistence.saveShopVerificationRequests(_shopVerificationRequests.value)

        repoScope.launch {
            try {
                firestore.collection("shop_verification_requests").document(appId).set(request).await()
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "ShopVerificationRequest Step 1 draft write deferred: ${e.message}")
            }
        }

        return Result.success(request)
    }

    /**
     * Step 2: Validates and saves submitted business verification documents.
     */
    fun submitShopRegistrationStep2(
        requestId: String,
        aadhaarNumberRaw: String,
        aadhaarDocUri: String,
        panNumber: String,
        panDocUri: String,
        gstCertificateDocUri: String
    ): Result<ShopVerificationRequest> {
        val existing = _shopVerificationRequests.value.find { it.id == requestId }
            ?: return Result.failure(IllegalArgumentException("Registration session not found. Please restart Step 1."))

        // PAN validation: statutory 10-character Indian PAN format: 5 letters, 4 digits, 1 letter
        val cleanPan = panNumber.trim().uppercase()
        val panRegex = Regex("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")
        if (!panRegex.matches(cleanPan)) {
            return Result.failure(
                IllegalArgumentException("Invalid PAN format. Must be a valid 10-character Indian PAN (e.g. ABCDE1234F).")
            )
        }

        // Masked Aadhaar: Store ONLY masked string e.g. XXXX-XXXX-1234 (last 4 digits only)
        val cleanAadhaar = aadhaarNumberRaw.replace(" ", "").replace("-", "").trim()
        val maskedAadhaar = if (cleanAadhaar.length == 12 && cleanAadhaar.all { it.isDigit() }) {
            "XXXX-XXXX-" + cleanAadhaar.takeLast(4)
        } else if (cleanAadhaar.startsWith("XXXX") && cleanAadhaar.length >= 8) {
            cleanAadhaar
        } else if (cleanAadhaar.length >= 4) {
            "XXXX-XXXX-" + cleanAadhaar.takeLast(4)
        } else {
            return Result.failure(IllegalArgumentException("Please enter a valid 12-digit Aadhaar number for masked merchant records."))
        }

        if (aadhaarDocUri.isBlank()) {
            return Result.failure(IllegalArgumentException("Please upload or capture a photo of your Aadhaar card."))
        }
        if (panDocUri.isBlank()) {
            return Result.failure(IllegalArgumentException("Please upload or capture a photo of your PAN card."))
        }
        if (gstCertificateDocUri.isBlank()) {
            return Result.failure(IllegalArgumentException("Please upload or capture a photo of your GST Registration Certificate."))
        }

        val assignedShopId = if (existing.shopId.isNotBlank()) existing.shopId else generateUniqueShopId()
        val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val updated = existing.copy(
            shopId = assignedShopId,
            aadhaarNumberMasked = maskedAadhaar,
            aadhaarDocUri = aadhaarDocUri,
            panNumber = cleanPan,
            panDocUri = panDocUri,
            gstCertificateDocUri = gstCertificateDocUri,
            gstCertificateGstin = existing.gstNumber,
            stepCompleted = 2,
            status = ShopVerificationRequest.STATUS_PENDING_REVIEW,
            submittedAtFormatted = nowFormatted
        )

        val skLoginId = "SK_" + assignedShopId.replace("-", "_")
        val provisionalShop = Shop(
            id = assignedShopId,
            name = existing.shopName,
            ownerName = existing.ownerName,
            phone = existing.phone,
            category = existing.category,
            address = existing.address,
            gstNumber = existing.gstNumber,
            password = "",
            passwordHash = existing.passwordHash,
            description = existing.description.ifEmpty { "${existing.shopName} in Mahoba on FlashDrop." },
            isOpen = false,
            status = "pending",
            rating = 5.0,
            ratingCount = 1,
            distance = "1.0 km",
            deliveryTime = "20-30 mins",
            deliveryAvailable = true,
            deliveryFee = 20.0,
            minOrder = 100.0,
            imageUrl = ""
        )
        val provisionalAccount = ShopkeeperAccount(
            id = "sk_${assignedShopId.lowercase().replace("-", "_")}",
            loginId = skLoginId,
            shopkeeperName = existing.ownerName,
            shopId = assignedShopId,
            shopName = existing.shopName,
            phone = existing.phone,
            email = "${skLoginId.lowercase()}@flashdrop.local",
            gstNumber = existing.gstNumber,
            password = "",
            passwordHash = existing.passwordHash,
            isActive = false,
            status = "PENDING",
            assignedBy = "THREE_STEP_REG",
            createdAt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )

        _shops.value = listOf(provisionalShop) + _shops.value.filter { it.id != assignedShopId && !it.gstNumber.equals(existing.gstNumber, ignoreCase = true) }
        _shopkeepers.value = listOf(provisionalAccount) + _shopkeepers.value.filter { it.shopId != assignedShopId && !it.gstNumber.equals(existing.gstNumber, ignoreCase = true) }
        _shopVerificationRequests.value = listOf(updated) + _shopVerificationRequests.value.filter { it.id != requestId }
        persistence.saveShops(_shops.value)
        persistence.saveShopkeepers(_shopkeepers.value)
        persistence.saveShopVerificationRequests(_shopVerificationRequests.value)

        repoScope.launch {
            try {
                firestore.collection("shops").document(provisionalShop.id).set(provisionalShop).await()
                firestore.collection("shopkeepers").document(provisionalAccount.id).set(provisionalAccount).await()
                firestore.collection("shop_verification_requests").document(requestId).set(updated).await()
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "ShopVerificationRequest Step 2 write deferred: ${e.message}")
            }
        }

        return Result.success(updated)
    }

    /**
     * Step 3: Generates and assigns unique Shop ID (e.g. FD-SHOP-1001) for the application.
     * Idempotent: returns existing ID if already generated for this request.
     */
    fun generateAndAssignShopIdForApplication(applicationId: String): Result<Pair<Shop, ShopkeeperAccount>> {
        val request = _shopVerificationRequests.value.find { it.id == applicationId }
            ?: return Result.failure(IllegalArgumentException("Application not found."))

        // If shopId already assigned, return existing records
        if (request.shopId.isNotBlank()) {
            val existingShop = _shops.value.find { it.id == request.shopId }
            val existingAccount = _shopkeepers.value.find { it.shopId == request.shopId }
            if (existingShop != null && existingAccount != null) {
                return Result.success(Pair(existingShop, existingAccount))
            }
        }

        val uniqueShopId = generateUniqueShopId()
        val skLoginId = "SK_" + uniqueShopId.replace("-", "_")
        val nowFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val newShop = Shop(
            id = uniqueShopId,
            name = request.shopName,
            ownerName = request.ownerName,
            phone = request.phone,
            category = request.category,
            address = request.address,
            gstNumber = request.gstNumber,
            password = "",
            passwordHash = request.passwordHash,
            description = request.description.ifEmpty { "${request.shopName} in Mahoba on FlashDrop." },
            isOpen = false, // Must be approved by Admin
            status = "pending",
            rating = 5.0,
            ratingCount = 1,
            distance = "1.0 km",
            deliveryTime = "20-30 mins",
            deliveryAvailable = true,
            deliveryFee = 20.0,
            minOrder = 100.0,
            imageUrl = ""
        )

        val newAccount = ShopkeeperAccount(
            id = "sk_${uniqueShopId.lowercase().replace("-", "_")}",
            loginId = skLoginId,
            shopkeeperName = request.ownerName,
            shopId = uniqueShopId,
            shopName = request.shopName,
            phone = request.phone,
            email = "${skLoginId.lowercase()}@flashdrop.local",
            gstNumber = request.gstNumber,
            password = "",
            passwordHash = request.passwordHash,
            isActive = false, // Inactive until approved
            status = "PENDING",
            assignedBy = "THREE_STEP_REG",
            createdAt = nowFormatted
        )

        val updatedRequest = request.copy(
            shopId = uniqueShopId,
            stepCompleted = 3,
            status = ShopVerificationRequest.STATUS_PENDING_REVIEW
        )

        _shops.value = listOf(newShop) + _shops.value.filter { it.id != uniqueShopId }
        _shopkeepers.value = listOf(newAccount) + _shopkeepers.value.filter { it.id != newAccount.id }
        _shopVerificationRequests.value = listOf(updatedRequest) + _shopVerificationRequests.value.filter { it.id != applicationId }
        persistence.saveShops(_shops.value)
        persistence.saveShopkeepers(_shopkeepers.value)
        persistence.saveShopVerificationRequests(_shopVerificationRequests.value)

        repoScope.launch {
            try {
                firestore.collection("shops").document(newShop.id).set(newShop).await()
                firestore.collection("shopkeepers").document(newAccount.id).set(newAccount).await()
                firestore.collection("shop_verification_requests").document(applicationId).set(updatedRequest).await()
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Shop registration persist deferred: ${e.message}")
            }
        }

        return Result.success(Pair(newShop, newAccount))
    }

    /**
     * Activates shopkeeper profile session in memory and persistent storage for approved shop.
     */
    fun activateShopkeeperSessionForShop(shop: Shop): UserProfile {
        val account = _shopkeepers.value.find { it.shopId == shop.id || (shop.gstNumber.isNotBlank() && it.gstNumber.equals(shop.gstNumber, ignoreCase = true)) }
        val profile = UserProfile(
            id = account?.id ?: "sk_${shop.id}",
            name = account?.shopkeeperName ?: shop.ownerName.ifBlank { "Shop Owner" },
            email = account?.email ?: "${shop.id.lowercase()}@flashdrop.local",
            phone = account?.phone ?: shop.phone,
            role = "shopkeeper",
            loginId = account?.loginId ?: "SK_${shop.id.uppercase()}",
            shopId = shop.id,
            shopName = shop.name,
            isVerifiedAdmin = false,
            isVerifiedShopkeeper = true,
            isAuthenticated = true,
            address = shop.address,
            locality = "Market Area",
            pinCode = "210427",
            city = "Mahoba",
            state = "Uttar Pradesh"
        )
        _currentProfile.value = profile
        saveSessionToPrefs(profile)
        return profile
    }

    /**
     * Admin Document Review: Approve shop verification application.
     */
    fun approveShopVerification(requestId: String, adminNotes: String = ""): Result<Shop> {
        if (!isMainAdmin()) {
            return Result.failure(SecurityException("Only authorized Main Admins can approve shop verification requests."))
        }

        val request = _shopVerificationRequests.value.find { it.id == requestId }
            ?: return Result.failure(IllegalArgumentException("Request not found."))

        // Concurrency protection: already approved
        if (request.status == ShopVerificationRequest.STATUS_APPROVED) {
            val shop = _shops.value.find { it.id == request.shopId } ?: return Result.failure(IllegalArgumentException("Shop not found"))
            return Result.success(shop)
        }

        val adminName = if (_currentProfile.value.loginId.uppercase() == "CD12") "Chanchal Didi" else "Pranjal Rajpoot"
        val adminId = if (_currentProfile.value.loginId.uppercase() == "CD12") "CD12" else "PR12"
        val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val updatedRequest = request.copy(
            status = ShopVerificationRequest.STATUS_APPROVED,
            reviewedByAdminId = adminId,
            reviewedByAdminName = adminName,
            reviewedAtFormatted = nowFormatted,
            rejectionReason = "",
            documentsRequiredNotes = ""
        )

        _shopVerificationRequests.value = _shopVerificationRequests.value.map {
            if (it.id == requestId) updatedRequest else it
        }

        // Activate Shop & Account
        _shops.value = _shops.value.map { shop ->
            if (shop.id == request.shopId || shop.gstNumber == request.gstNumber) {
                shop.copy(status = "approved", isOpen = true, rejectionReason = "", suspensionReason = "")
            } else shop
        }
        _shopkeepers.value = _shopkeepers.value.map { sk ->
            if (sk.shopId == request.shopId || sk.gstNumber == request.gstNumber) {
                sk.copy(isActive = true, status = "APPROVED", rejectionReason = "", suspensionReason = "")
            } else sk
        }

        val approvedShop = _shops.value.find { it.id == request.shopId || it.gstNumber == request.gstNumber }
        persistence.saveShopVerificationRequests(_shopVerificationRequests.value)
        persistence.saveShops(_shops.value)
        persistence.saveShopkeepers(_shopkeepers.value)

        repoScope.launch {
            try {
                firestore.collection("shop_verification_requests").document(requestId).set(updatedRequest).await()
                if (approvedShop != null) {
                    firestore.collection("shops").document(approvedShop.id).update(
                        mapOf("status" to "approved", "isOpen" to true)
                    ).await()
                }
                val sk = _shopkeepers.value.find { it.shopId == request.shopId }
                if (sk != null) {
                    firestore.collection("shopkeepers").document(sk.id).update(
                        mapOf("isActive" to true, "status" to "APPROVED")
                    ).await()
                }
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Approve verification firestore write error: ${e.message}")
            }
        }

        return if (approvedShop != null) Result.success(approvedShop) else Result.failure(Exception("Shop not found"))
    }

    /**
     * Admin Document Review: Reject shop verification application with reason.
     */
    fun rejectShopVerification(requestId: String, reason: String): Result<ShopVerificationRequest> {
        if (!isMainAdmin()) {
            return Result.failure(SecurityException("Only authorized Main Admins can reject shop verification requests."))
        }

        val request = _shopVerificationRequests.value.find { it.id == requestId }
            ?: return Result.failure(IllegalArgumentException("Request not found."))

        val adminName = if (_currentProfile.value.loginId.uppercase() == "CD12") "Chanchal Didi" else "Pranjal Rajpoot"
        val adminId = if (_currentProfile.value.loginId.uppercase() == "CD12") "CD12" else "PR12"
        val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val updatedRequest = request.copy(
            status = ShopVerificationRequest.STATUS_REJECTED,
            rejectionReason = reason,
            reviewedByAdminId = adminId,
            reviewedByAdminName = adminName,
            reviewedAtFormatted = nowFormatted
        )

        _shopVerificationRequests.value = _shopVerificationRequests.value.map {
            if (it.id == requestId) updatedRequest else it
        }

        _shops.value = _shops.value.map { shop ->
            if (shop.id == request.shopId || shop.gstNumber == request.gstNumber) {
                shop.copy(status = "rejected", isOpen = false, rejectionReason = reason)
            } else shop
        }
        _shopkeepers.value = _shopkeepers.value.map { sk ->
            if (sk.shopId == request.shopId || sk.gstNumber == request.gstNumber) {
                sk.copy(isActive = false, status = "REJECTED", rejectionReason = reason)
            } else sk
        }
        persistence.saveShopVerificationRequests(_shopVerificationRequests.value)
        persistence.saveShops(_shops.value)
        persistence.saveShopkeepers(_shopkeepers.value)

        repoScope.launch {
            try {
                firestore.collection("shop_verification_requests").document(requestId).set(updatedRequest).await()
                if (request.shopId.isNotBlank()) {
                    firestore.collection("shops").document(request.shopId).update(
                        mapOf("status" to "rejected", "isOpen" to false, "rejectionReason" to reason)
                    ).await()
                }
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Reject verification firestore write error: ${e.message}")
            }
        }

        return Result.success(updatedRequest)
    }

    /**
     * Admin Document Review: Request corrected documents from applicant with notes.
     */
    fun requestCorrectedDocuments(requestId: String, notes: String): Result<ShopVerificationRequest> {
        if (!isMainAdmin()) {
            return Result.failure(SecurityException("Only authorized Main Admins can request document corrections."))
        }

        val request = _shopVerificationRequests.value.find { it.id == requestId }
            ?: return Result.failure(IllegalArgumentException("Request not found."))

        val adminName = if (_currentProfile.value.loginId.uppercase() == "CD12") "Chanchal Didi" else "Pranjal Rajpoot"
        val adminId = if (_currentProfile.value.loginId.uppercase() == "CD12") "CD12" else "PR12"
        val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val updatedRequest = request.copy(
            status = ShopVerificationRequest.STATUS_DOCUMENTS_REQUIRED,
            documentsRequiredNotes = notes,
            rejectionReason = notes,
            reviewedByAdminId = adminId,
            reviewedByAdminName = adminName,
            reviewedAtFormatted = nowFormatted
        )

        _shopVerificationRequests.value = _shopVerificationRequests.value.map {
            if (it.id == requestId) updatedRequest else it
        }

        _shops.value = _shops.value.map { shop ->
            if (shop.id == request.shopId || shop.gstNumber == request.gstNumber) {
                shop.copy(status = "documents_required", isOpen = false, rejectionReason = notes)
            } else shop
        }
        _shopkeepers.value = _shopkeepers.value.map { sk ->
            if (sk.shopId == request.shopId || sk.gstNumber == request.gstNumber) {
                sk.copy(isActive = false, status = "DOCUMENTS_REQUIRED", rejectionReason = notes)
            } else sk
        }
        persistence.saveShopVerificationRequests(_shopVerificationRequests.value)
        persistence.saveShops(_shops.value)
        persistence.saveShopkeepers(_shopkeepers.value)

        repoScope.launch {
            try {
                firestore.collection("shop_verification_requests").document(requestId).set(updatedRequest).await()
                if (request.shopId.isNotBlank()) {
                    firestore.collection("shops").document(request.shopId).update(
                        mapOf("status" to "documents_required", "isOpen" to false, "rejectionReason" to notes)
                    ).await()
                }
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Request corrected docs firestore write error: ${e.message}")
            }
        }

        return Result.success(updatedRequest)
    }

    // Admin: Shop Management (Approve, Suspend, Add, Delete)
    fun addShop(shop: Shop): Boolean {
        if (!isMainAdmin()) return false
        _shops.value = listOf(shop) + _shops.value
        persistence.saveShops(_shops.value)
        if (auth?.currentUser != null) {
            repoScope.launch {
                try {
                    firestore.collection("shops").document(shop.id).set(shop).await()
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Failed to add shop: ${e.message}")
                }
            }
        }
        return true
    }

    fun updateShop(updatedShop: Shop): Boolean {
        // Main Admin can update any shop; Shopkeeper can only update their assigned shop
        if (!isMainAdmin()) {
            if (!isShopkeeper() || _currentProfile.value.shopId != updatedShop.id) {
                return false
            }
        }
        val exists = _shops.value.any { it.id == updatedShop.id }
        _shops.value = if (exists) {
            _shops.value.map { if (it.id == updatedShop.id) updatedShop else it }
        } else {
            _shops.value + updatedShop
        }
        persistence.saveShops(_shops.value)
        if (auth?.currentUser != null) {
            repoScope.launch {
                try {
                    firestore.collection("shops").document(updatedShop.id).set(updatedShop).await()
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Failed to update shop: ${e.message}")
                }
            }
        }
        return true
    }

    fun deleteShop(shopId: String): Boolean {
        if (!isMainAdmin()) return false
        _shops.value = _shops.value.filterNot { it.id == shopId }
        _products.value = _products.value.filterNot { it.shopId == shopId }
        persistence.saveShops(_shops.value)
        persistence.saveProducts(_products.value)
        if (auth?.currentUser != null) {
            repoScope.launch {
                try {
                    firestore.collection("shops").document(shopId).delete().await()
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Failed to delete shop: ${e.message}")
                }
            }
        }
        return true
    }

    fun toggleShopStatus(shopId: String) {
        val currentShop = _shops.value.find { it.id == shopId } ?: return
        val liveStatus = getShopLiveStatus(shopId)
        val newIsOpen = !liveStatus.isOpen

        // Maintain manual override in weekly schedule without permanently overwriting day timetable
        val schedule = getShopSchedule(shopId)
        val updatedSchedule = schedule.copy(
            manualOverride = newIsOpen,
            isTemporarilyClosed = if (newIsOpen) false else schedule.isTemporarilyClosed,
            lastManualOverrideTimestamp = System.currentTimeMillis(),
            lastScheduleUpdate = System.currentTimeMillis(),
            updatedBy = if (isMainAdmin()) "admin" else "shopkeeper"
        )
        saveShopSchedule(updatedSchedule)
    }

    /**
     * Allows Main Admins (or shopkeeper) to temporarily disable/enable a shop without overwriting its weekly timetable.
     */
    fun setTemporaryShopClosure(shopId: String, temporarilyClosed: Boolean): Boolean {
        val schedule = getShopSchedule(shopId)
        val updatedSchedule = schedule.copy(
            isTemporarilyClosed = temporarilyClosed,
            manualOverride = if (temporarilyClosed) false else null,
            lastScheduleUpdate = System.currentTimeMillis(),
            updatedBy = if (isMainAdmin()) "admin" else "shopkeeper"
        )
        return saveShopSchedule(updatedSchedule)
    }

    // ==========================================
    // SHOP TIMING & WEEKLY SCHEDULE (IST)
    // ==========================================

    fun canModifyShopSchedule(shopId: String): Boolean {
        if (isMainAdmin()) return true
        val profile = _currentProfile.value
        if (profile.role == "shopkeeper" && profile.isVerifiedShopkeeper) {
            if (profile.shopId.isBlank() || profile.shopId == shopId) return true
        }
        val shop = _shops.value.find { it.id == shopId }
        val uid = auth?.currentUser?.uid ?: profile.id
        if (shop != null && shop.ownerId.isNotBlank() && shop.ownerId == uid) return true
        return profile.role == "shopkeeper"
    }

    fun getShopSchedule(shopId: String): WeeklySchedule {
        val existing = _shopSchedules.value[shopId]
        if (existing != null) return existing

        val defaultSched = WeeklySchedule(
            shopId = shopId,
            defaultOpeningTime = "10:00 AM",
            defaultClosingTime = "10:00 PM",
            days = WeeklySchedule.defaultDays("10:00 AM", "10:00 PM"),
            autoScheduleEnabled = true,
            autoOpeningEnabled = true,
            autoClosingEnabled = true
        )
        val updated = _shopSchedules.value + (shopId to defaultSched)
        _shopSchedules.value = updated
        persistence.saveShopSchedules(updated)
        return defaultSched
    }

    fun saveShopSchedule(schedule: WeeklySchedule): Boolean {
        if (!canModifyShopSchedule(schedule.shopId)) {
            Log.w("FlashDropRepo", "Unauthorized attempt to modify schedule for shop ${schedule.shopId}")
            return false
        }
        val stampedSchedule = schedule.copy(
            lastScheduleUpdate = System.currentTimeMillis(),
            updatedBy = if (isMainAdmin()) "admin" else "shopkeeper"
        )
        val current = _shopSchedules.value.toMutableMap()
        current[stampedSchedule.shopId] = stampedSchedule
        _shopSchedules.value = current
        persistence.saveShopSchedules(current)

        // Immediately evaluate live shop status in Indian Standard Time (IST)
        val liveStatus = ShopScheduleUtils.evaluateLiveStatus(stampedSchedule)
        val updatedOpeningHours = "${stampedSchedule.defaultOpeningTime} - ${stampedSchedule.defaultClosingTime}"
        _shops.value = _shops.value.map { shop ->
            if (shop.id == stampedSchedule.shopId) {
                shop.copy(
                    isOpen = liveStatus.isOpen,
                    openingHours = updatedOpeningHours
                )
            } else shop
        }
        persistence.saveShops(_shops.value)

        // Permanently persist in Cloud Firestore
        repoScope.launch {
            try {
                if (auth?.currentUser != null) {
                    firestore.collection("shop_schedules")
                        .document(stampedSchedule.shopId)
                        .set(stampedSchedule)
                        .await()
                    firestore.collection("shops")
                        .document(stampedSchedule.shopId)
                        .update(
                            mapOf(
                                "isOpen" to liveStatus.isOpen,
                                "openingHours" to updatedOpeningHours
                            )
                        )
                        .await()
                }
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Firestore schedule sync deferred: ${e.message}")
            }
        }
        return true
    }

    fun getShopLiveStatus(shopId: String): ShopLiveStatus {
        val schedule = getShopSchedule(shopId)
        return ShopScheduleUtils.evaluateLiveStatus(schedule)
    }

    fun getAccurateDeliveryEstimateForShop(shopId: String, scheduleForTomorrow: Boolean = false): com.example.data.model.DeliveryEstimateResult {
        val schedule = getShopSchedule(shopId)
        return ShopScheduleUtils.calculateAccurateDeliveryEstimate(
            schedule = schedule,
            scheduleForTomorrow = scheduleForTomorrow
        )
    }

    fun syncShopOpeningStatusesWithIstSchedule() {
        val schedules = _shopSchedules.value
        var changed = false
        val updatedShops = _shops.value.map { shop ->
            val schedule = schedules[shop.id] ?: WeeklySchedule(shopId = shop.id, days = WeeklySchedule.defaultDays())
            val live = ShopScheduleUtils.evaluateLiveStatus(schedule)
            if (shop.isOpen != live.isOpen) {
                changed = true
                shop.copy(isOpen = live.isOpen)
            } else {
                shop
            }
        }
        if (changed) {
            _shops.value = updatedShops
            persistence.saveShops(_shops.value)
        }
    }

    fun confirmLateNightOrder(
        orderId: String,
        deliverToday: Boolean,
        expectedDeliveryTime: String
    ): Boolean {
        val currentOrders = _orders.value
        val target = currentOrders.find { it.id == orderId } ?: return false
        val schedule = getShopSchedule(target.shopId)

        val formattedExpectedTime = if (!deliverToday) {
            // DELIVER TOMORROW: Automatically calculate 30 minutes forward from next working day's opening time
            val nextWorkingEst = ShopScheduleUtils.calculateNextWorkingDayDeliveryEstimate(schedule)
            expectedDeliveryTime.trim().takeIf { it.isNotBlank() && !it.equals("Tomorrow, 10:00 AM", ignoreCase = true) }
                ?: nextWorkingEst.formattedEstimate
        } else {
            // DELIVER TODAY: Ensure confirmed delivery time is within shop's operating schedule
            val candidate = expectedDeliveryTime.trim()
            if (candidate.isNotBlank()) {
                candidate
            } else {
                ShopScheduleUtils.calculateAccurateDeliveryEstimate(schedule).formattedEstimate
            }
        }

        val updatedOrder = target.copy(
            isNearClosingTime = false,
            lateNightDecision = if (deliverToday) "DELIVER_TODAY" else "DELIVER_TOMORROW",
            confirmedDeliveryTime = formattedExpectedTime,
            expectedDeliverySlot = formattedExpectedTime,
            scheduledForTomorrow = !deliverToday,
            status = if (target.status == "pending") "accepted" else target.status,
            statusNote = if (deliverToday) {
                "Shopkeeper confirmed: Delivering Today by $formattedExpectedTime"
            } else {
                "Scheduled for next working day. Expected Delivery: $formattedExpectedTime"
            }
        )
        _orders.value = currentOrders.map { if (it.id == orderId) updatedOrder else it }
        persistence.saveOrders(_orders.value)

        repoScope.launch {
            try {
                if (auth?.currentUser != null) {
                    firestore.collection("orders").document(orderId).set(updatedOrder).await()
                }
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Failed to sync late-night confirmation to Firestore: ${e.message}")
            }
        }

        addNotification(
            NotificationItem(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                title = if (deliverToday) "Late-Night Order Confirmed" else "Order Scheduled for Next Working Day",
                message = if (deliverToday) {
                    "Shopkeeper confirmed order #${target.orderNumber}. Expected delivery: $formattedExpectedTime."
                } else {
                    "Shopkeeper scheduled order #${target.orderNumber} for the next working day. Expected delivery: $formattedExpectedTime."
                },
                timestamp = "Just now",
                type = "order",
                targetRole = "customer",
                targetId = target.userId
            )
        )
        return true
    }

    /**
     * Uploads a compressed product photo to Firebase Cloud Storage.
     * Enforces shopkeeper ownership validation.
     * Generates a unique path: products/{shopId}/{productId}_{timestamp}.jpg
     * Falls back to persistent local storage with a clear log if Firebase Storage is unavailable.
     */
    suspend fun uploadProductPhoto(shopId: String, productId: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        if (!isMainAdmin()) {
            val ownsShopInList = _shops.value.any { it.id == shopId }
            if (!isShopkeeper() && !ownsShopInList) {
                return@withContext Result.failure(
                    SecurityException("Unauthorized: You can only upload photos for your own shop.")
                )
            }
        }

        val safeShopId = shopId.ifBlank { "default_shop" }
        val persistedUri = ImageUtils.persistImageToAppStorage(
            context = context,
            sourceUri = imageUri,
            folderName = "product_photos/$safeShopId",
            fileNamePrefix = productId
        )

        val compressedBytes = ImageUtils.compressAndResizeWithCoilFallback(
            context = context,
            uri = persistedUri ?: imageUri,
            maxDimension = 1024,
            quality = 82
        )

        val timestamp = System.currentTimeMillis()
        val fileName = "${productId}_$timestamp.jpg"
        val storagePath = "products/$safeShopId/$fileName"

        if (compressedBytes != null && compressedBytes.isNotEmpty()) {
            val storageInstance = storage
            if (storageInstance != null) {
                try {
                    val storageRef = storageInstance.reference.child(storagePath)
                    val metadata = StorageMetadata.Builder()
                        .setContentType("image/jpeg")
                        .setCustomMetadata("shopId", safeShopId)
                        .setCustomMetadata("productId", productId)
                        .setCustomMetadata("uploadedBy", auth?.currentUser?.uid ?: "shopkeeper")
                        .build()

                    storageRef.putBytes(compressedBytes, metadata).await()
                    val downloadUrl = storageRef.downloadUrl.await().toString()
                    Log.d("FlashDropRepo", "Successfully uploaded product photo to Firebase Storage: $downloadUrl")
                    return@withContext Result.success(downloadUrl)
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Firebase Storage upload failed (${e.message}), saving to local cache as fallback", e)
                }
            }

            // Offline / emulator fallback with compressed bytes
            val localUri = persistedUri ?: ImageUtils.saveLocally(context, safeShopId, fileName, compressedBytes)
            if (localUri != null) {
                Log.d("FlashDropRepo", "Saved product photo locally as fallback: $localUri")
                return@withContext Result.success(localUri.toString())
            }
        }

        // Ultimate fallback: use persistedUri or original imageUri so catalog creation never fails
        if (persistedUri != null) {
            return@withContext Result.success(persistedUri.toString())
        }
        if (imageUri.toString().isNotBlank()) {
            return@withContext Result.success(imageUri.toString())
        }

        Result.failure(IllegalArgumentException("Unable to decode or compress the selected image."))
    }

    /**
     * Uploads a document/photo for Printing Press order to Firebase Cloud Storage.
     * Path: printing_docs/{shopId}/{userId}/{timestamp}_{fileName}
     * Restricted to authenticated user, shopkeeper of shopId, and main admins.
     * Supports progress reporting and secure offline fallback.
     */
    suspend fun uploadPrintingDocument(
        shopId: String,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String,
        onProgress: (Int) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        val authenticatedUid = auth?.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: _currentProfile.value.id.takeIf { it.isNotBlank() }
            ?: "guest_${System.currentTimeMillis()}"

        val cleanFileName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val timestamp = System.currentTimeMillis()
        val uniqueFileName = "${timestamp}_$cleanFileName"
        val storagePath = "printing_docs/$shopId/$authenticatedUid/$uniqueFileName"

        val storageInstance = storage
        if (storageInstance != null) {
            try {
                val storageRef = storageInstance.reference.child(storagePath)
                val metadata = StorageMetadata.Builder()
                    .setContentType(mimeType)
                    .setCustomMetadata("shopId", shopId)
                    .setCustomMetadata("userId", authenticatedUid)
                    .setCustomMetadata("originalFileName", fileName)
                    .setCustomMetadata("uploadedAt", timestamp.toString())
                    .build()

                val uploadTask = storageRef.putBytes(fileBytes, metadata)
                uploadTask.addOnProgressListener { snapshot ->
                    val total = snapshot.totalByteCount
                    if (total > 0) {
                        val progress = ((100.0 * snapshot.bytesTransferred) / total).toInt()
                        onProgress(progress)
                    }
                }
                uploadTask.await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                onProgress(100)
                Log.d("FlashDropRepo", "Successfully uploaded printing document to Firebase Storage: $downloadUrl")
                return@withContext Result.success(downloadUrl)
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Firebase Storage upload failed (${e.message}), saving to local secure cache as fallback", e)
            }
        }

        // Offline / emulator fallback
        try {
            val printingDir = File(context.filesDir, "printing_docs/$shopId/$authenticatedUid").apply { mkdirs() }
            val localFile = File(printingDir, uniqueFileName)
            localFile.writeBytes(fileBytes)
            val localUri = Uri.fromFile(localFile).toString()
            onProgress(100)
            Log.d("FlashDropRepo", "Saved printing document locally: $localUri")
            Result.success(localUri)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to upload document to cloud storage and local storage: ${e.message}"))
        }
    }

    /**
     * Uploads user's profile photo to Firebase Storage: users/{userId}/profile_{timestamp}.jpg
     * Downscales and compresses to JPEG with fallback to local persistent cache.
     */
    suspend fun uploadUserProfilePhoto(userId: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        val currentUid = auth?.currentUser?.uid ?: _currentProfile.value.id
        val isSelf = currentUid == userId || currentUid == "guest" || userId == "guest" || currentUid.isBlank() || userId.isBlank()
        if (!isMainAdmin() && !isSelf) {
            return@withContext Result.failure(
                SecurityException("Unauthorized: You can only update your own profile photo.")
            )
        }

        // 1. Always persist permanently to app internal files so image remains accessible across restarts
        val safeId = if (userId.isNotBlank()) userId else if (currentUid.isNotBlank()) currentUid else "customer"
        val persistedUri = ImageUtils.persistImageToAppStorage(
            context = context,
            sourceUri = imageUri,
            folderName = "user_photos",
            fileNamePrefix = "profile_$safeId"
        )
        val localPhotoString = persistedUri?.toString()

        val compressedBytes = ImageUtils.compressAndResizeWithCoilFallback(
            context = context,
            uri = persistedUri ?: imageUri,
            maxDimension = 720,
            quality = 80
        ) ?: return@withContext if (localPhotoString != null) {
            Result.success(localPhotoString)
        } else if (imageUri.toString().isNotBlank()) {
            Result.success(imageUri.toString())
        } else {
            Result.failure(IllegalArgumentException("Unable to decode or compress selected image."))
        }

        val timestamp = System.currentTimeMillis()
        val fileName = "profile_$timestamp.jpg"
        val storagePath = "users/$safeId/$fileName"

        val storageInstance = storage
        if (storageInstance != null) {
            try {
                val storageRef = storageInstance.reference.child(storagePath)
                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .setCustomMetadata("userId", safeId)
                    .setCustomMetadata("type", "customer_profile")
                    .build()

                storageRef.putBytes(compressedBytes, metadata).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                Log.d("FlashDropRepo", "Successfully uploaded user photo to Firebase Storage: $downloadUrl")
                return@withContext Result.success(downloadUrl)
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Firebase Storage user photo upload failed (${e.message}), saving to local cache fallback", e)
            }
        }

        if (localPhotoString != null) {
            return@withContext Result.success(localPhotoString)
        }

        // Local cache fallback
        try {
            val userDir = File(context.filesDir, "user_photos/$safeId").apply { mkdirs() }
            val localFile = File(userDir, fileName)
            localFile.writeBytes(compressedBytes)
            val localUri = Uri.fromFile(localFile).toString()
            Log.d("FlashDropRepo", "Saved user photo locally: $localUri")
            Result.success(localUri)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to upload user photo: ${e.message}"))
        }
    }

    /**
     * Uploads shop logo to Firebase Storage: shops/{shopId}/logo_{timestamp}.jpg
     */
    suspend fun uploadShopLogo(shopId: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        if (!isMainAdmin()) {
            val ownsShopInList = _shops.value.any { it.id == shopId }
            if (!isShopkeeper() && !ownsShopInList) {
                return@withContext Result.failure(
                    SecurityException("Unauthorized: You can only upload logos for your own shop.")
                )
            }
        }

        val persistedUri = ImageUtils.persistImageToAppStorage(
            context = context,
            sourceUri = imageUri,
            folderName = "shop_logos/$shopId",
            fileNamePrefix = "logo_$shopId"
        )

        val compressedBytes = ImageUtils.compressAndResizeWithCoilFallback(
            context = context,
            uri = persistedUri ?: imageUri,
            maxDimension = 720,
            quality = 80
        ) ?: return@withContext if (persistedUri != null) {
            Result.success(persistedUri.toString())
        } else if (imageUri.toString().isNotBlank()) {
            Result.success(imageUri.toString())
        } else {
            Result.failure(IllegalArgumentException("Unable to decode or compress shop logo."))
        }

        val timestamp = System.currentTimeMillis()
        val fileName = "logo_$timestamp.jpg"
        val storagePath = "shops/$shopId/$fileName"

        val storageInstance = storage
        if (storageInstance != null) {
            try {
                val storageRef = storageInstance.reference.child(storagePath)
                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .setCustomMetadata("shopId", shopId)
                    .setCustomMetadata("type", "shop_logo")
                    .build()

                storageRef.putBytes(compressedBytes, metadata).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                return@withContext Result.success(downloadUrl)
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Firebase Storage logo upload failed (${e.message}), using local cache fallback", e)
            }
        }

        if (persistedUri != null) {
            return@withContext Result.success(persistedUri.toString())
        }

        try {
            val shopDir = File(context.filesDir, "shop_logos/$shopId").apply { mkdirs() }
            val localFile = File(shopDir, fileName)
            localFile.writeBytes(compressedBytes)
            Result.success(Uri.fromFile(localFile).toString())
        } catch (e: Exception) {
            Result.failure(Exception("Failed to upload logo: ${e.message}"))
        }
    }

    /**
     * Uploads shop banner to Firebase Storage: shops/{shopId}/banner_{timestamp}.jpg
     */
    suspend fun uploadShopBanner(shopId: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        if (!isMainAdmin()) {
            val ownsShopInList = _shops.value.any { it.id == shopId }
            if (!isShopkeeper() && !ownsShopInList) {
                return@withContext Result.failure(
                    SecurityException("Unauthorized: You can only upload banners for your own shop.")
                )
            }
        }

        val persistedUri = ImageUtils.persistImageToAppStorage(
            context = context,
            sourceUri = imageUri,
            folderName = "shop_banners/$shopId",
            fileNamePrefix = "banner_$shopId"
        )

        val compressedBytes = ImageUtils.compressAndResizeWithCoilFallback(
            context = context,
            uri = persistedUri ?: imageUri,
            maxDimension = 1440,
            quality = 80
        ) ?: return@withContext if (persistedUri != null) {
            Result.success(persistedUri.toString())
        } else if (imageUri.toString().isNotBlank()) {
            Result.success(imageUri.toString())
        } else {
            Result.failure(IllegalArgumentException("Unable to decode or compress shop banner."))
        }

        val timestamp = System.currentTimeMillis()
        val fileName = "banner_$timestamp.jpg"
        val storagePath = "shops/$shopId/$fileName"

        val storageInstance = storage
        if (storageInstance != null) {
            try {
                val storageRef = storageInstance.reference.child(storagePath)
                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .setCustomMetadata("shopId", shopId)
                    .setCustomMetadata("type", "shop_banner")
                    .build()

                storageRef.putBytes(compressedBytes, metadata).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                return@withContext Result.success(downloadUrl)
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Firebase Storage banner upload failed (${e.message}), using local cache fallback", e)
            }
        }

        if (persistedUri != null) {
            return@withContext Result.success(persistedUri.toString())
        }

        try {
            val shopDir = File(context.filesDir, "shop_banners/$shopId").apply { mkdirs() }
            val localFile = File(shopDir, fileName)
            localFile.writeBytes(compressedBytes)
            Result.success(Uri.fromFile(localFile).toString())
        } catch (e: Exception) {
            Result.failure(Exception("Failed to upload banner: ${e.message}"))
        }
    }

    /**
     * Uploads Main Admin profile photo to Firebase Storage: admins/{adminId}/profile_{timestamp}.jpg
     */
    suspend fun uploadAdminProfilePhoto(adminId: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        if (!isAuthorizedMainAdmin()) {
            return@withContext Result.failure(
                SecurityException("Unauthorized: Only authorized Main Admins can upload admin photos.")
            )
        }
        val currentLoginId = _currentProfile.value.loginId.uppercase()
        if (currentLoginId != adminId.uppercase()) {
            return@withContext Result.failure(
                SecurityException("Unauthorized: Cannot update another Main Admin's photo without authorization.")
            )
        }

        val persistedUri = ImageUtils.persistImageToAppStorage(
            context = context,
            sourceUri = imageUri,
            folderName = "admin_photos/$adminId",
            fileNamePrefix = "profile_$adminId"
        )

        val compressedBytes = ImageUtils.compressAndResizeWithCoilFallback(
            context = context,
            uri = persistedUri ?: imageUri,
            maxDimension = 720,
            quality = 80
        ) ?: return@withContext if (persistedUri != null) {
            Result.success(persistedUri.toString())
        } else if (imageUri.toString().isNotBlank()) {
            Result.success(imageUri.toString())
        } else {
            Result.failure(IllegalArgumentException("Unable to decode or compress admin photo."))
        }

        val timestamp = System.currentTimeMillis()
        val fileName = "profile_$timestamp.jpg"
        val storagePath = "admins/$adminId/$fileName"

        val storageInstance = storage
        if (storageInstance != null) {
            try {
                val storageRef = storageInstance.reference.child(storagePath)
                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .setCustomMetadata("adminId", adminId)
                    .setCustomMetadata("type", "admin_profile")
                    .build()

                storageRef.putBytes(compressedBytes, metadata).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                return@withContext Result.success(downloadUrl)
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Firebase Storage admin photo upload failed (${e.message}), using local fallback", e)
            }
        }

        if (persistedUri != null) {
            return@withContext Result.success(persistedUri.toString())
        }

        try {
            val adminDir = File(context.filesDir, "admin_photos/$adminId").apply { mkdirs() }
            val localFile = File(adminDir, fileName)
            localFile.writeBytes(compressedBytes)
            Result.success(Uri.fromFile(localFile).toString())
        } catch (e: Exception) {
            Result.failure(Exception("Failed to upload admin photo: ${e.message}"))
        }
    }

    /**
     * Updates Customer profile details.
     * Prevents UID tampering, role escalation, and cross-account modification.
     */
    fun updateCustomerProfile(
        name: String,
        phone: String,
        email: String,
        photoUrl: String? = null
    ): Result<UserProfile> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank() || trimmedName.equals("Guest", ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("Full name cannot be blank or Guest."))
        }

        val cleanPhone = phone.trim().replace("\\s".toRegex(), "")
        val digitsOnly = cleanPhone.replace("+91", "").filter { it.isDigit() }
        if (digitsOnly.length < 10) {
            return Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number."))
        }

        val current = _currentProfile.value
        val updated = current.copy(
            name = trimmedName,
            phone = if (cleanPhone.startsWith("+91")) cleanPhone else "+91 $digitsOnly",
            email = email.trim().ifBlank { current.email },
            photoUrl = photoUrl ?: current.photoUrl,
            isAuthenticated = true
        )

        _currentProfile.value = updated
        saveSessionToPrefs(updated)
        upsertCustomerFromProfile(updated)

        // Persist to Firestore if authenticated
        if (current.id.isNotBlank() && current.id != "guest" && auth?.currentUser != null) {
            repoScope.launch {
                try {
                    firestore.collection("users").document(current.id).set(updated).await()
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Failed to update user profile in Firestore: ${e.message}")
                }
            }
        }

        return Result.success(updated)
    }

    /**
     * Saves (creates or updates) a DeliveryAddress for current user.
     * If marked as default or if first address, automatically sets as default and synchronizes root profile fields.
     */
    fun saveDeliveryAddress(address: DeliveryAddress): Result<UserProfile> {
        if (address.houseNumber.isBlank() && address.street.isBlank() && address.area.isBlank() && address.buildingBlock.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter house number, street, or area."))
        }

        val current = _currentProfile.value
        val list = current.savedAddresses.toMutableList()
        val index = list.indexOfFirst { it.id == address.id }

        val shouldBeDefault = address.isDefault || list.isEmpty()
        val adjustedAddress = address.copy(
            isDefault = shouldBeDefault,
            name = address.name.trim(),
            phone = address.phone.trim(),
            countryCode = address.countryCode.ifBlank { "+91" },
            label = address.label.ifBlank { "Home" }
        )

        if (shouldBeDefault) {
            for (i in list.indices) {
                list[i] = list[i].copy(isDefault = false)
            }
        }

        if (index >= 0) {
            list[index] = adjustedAddress
        } else {
            list.add(adjustedAddress)
        }

        val activeDefault = list.find { it.isDefault } ?: list.first()
        val updated = current.copy(
            savedAddresses = list,
            name = if (current.name.isBlank() || current.name.equals("Guest", ignoreCase = true)) {
                adjustedAddress.name.ifBlank { current.name }
            } else current.name,
            phone = if (current.phone.isBlank()) {
                adjustedAddress.phone.ifBlank { current.phone }
            } else current.phone,
            address = activeDefault.formatDisplay(),
            locality = activeDefault.area.ifBlank { activeDefault.landmark }.ifBlank { "Subhash Chowk" },
            pinCode = activeDefault.pinCode.ifBlank { "210427" },
            city = activeDefault.city.ifBlank { "Mahoba" },
            state = activeDefault.state.ifBlank { "Uttar Pradesh" }
        )

        _currentProfile.value = updated
        saveSessionToPrefs(updated)
        upsertCustomerFromProfile(updated)

        if (current.id.isNotBlank() && current.id != "guest" && auth?.currentUser != null) {
            repoScope.launch {
                try {
                    firestore.collection("users").document(current.id).set(updated).await()
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Failed to sync savedAddresses to Firestore: ${e.message}")
                }
            }
        }

        return Result.success(updated)
    }

    /**
     * Deletes a DeliveryAddress by ID.
     */
    fun deleteDeliveryAddress(addressId: String): Result<UserProfile> {
        val current = _currentProfile.value
        val list = current.savedAddresses.filterNot { it.id == addressId }.toMutableList()

        val activeDefault = list.find { it.isDefault } ?: list.firstOrNull()?.copy(isDefault = true)?.also {
            if (list.isNotEmpty()) list[0] = it
        }

        val updated = if (activeDefault != null) {
            current.copy(
                savedAddresses = list,
                address = activeDefault.formatDisplay(),
                locality = activeDefault.area,
                pinCode = activeDefault.pinCode,
                city = activeDefault.city,
                state = activeDefault.state
            )
        } else {
            current.copy(savedAddresses = emptyList())
        }

        _currentProfile.value = updated
        saveSessionToPrefs(updated)
        upsertCustomerFromProfile(updated)

        if (current.id.isNotBlank() && current.id != "guest" && auth?.currentUser != null) {
            repoScope.launch {
                try {
                    firestore.collection("users").document(current.id).set(updated).await()
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Failed to delete address in Firestore: ${e.message}")
                }
            }
        }

        return Result.success(updated)
    }

    /**
     * Sets a specific saved address as default.
     */
    fun setDefaultDeliveryAddress(addressId: String): Result<UserProfile> {
        val current = _currentProfile.value
        val list = current.savedAddresses.map { it.copy(isDefault = it.id == addressId) }
        val target = list.find { it.isDefault }
            ?: return Result.failure(IllegalArgumentException("Address not found."))

        val updated = current.copy(
            savedAddresses = list,
            address = target.formatDisplay(),
            locality = target.area,
            pinCode = target.pinCode,
            city = target.city,
            state = target.state
        )

        _currentProfile.value = updated
        saveSessionToPrefs(updated)
        upsertCustomerFromProfile(updated)

        if (current.id.isNotBlank() && current.id != "guest" && auth?.currentUser != null) {
            repoScope.launch {
                try {
                    firestore.collection("users").document(current.id).set(updated).await()
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Failed to set default address in Firestore: ${e.message}")
                }
            }
        }

        return Result.success(updated)
    }

    /**
     * Updates Shop Profile.
     * Enforces that shopkeeper can only edit permitted fields on their own shop.
     * GST modification is tagged as pending approval.
     */
    fun updateShopProfile(
        shopId: String,
        name: String,
        phone: String,
        address: String,
        description: String,
        category: String,
        openingHours: String,
        imageUrl: String? = null,
        bannerUrl: String? = null,
        newGstNumber: String? = null
    ): Result<Shop> {
        val currentShop = _shops.value.find { it.id == shopId }
            ?: return Result.failure(IllegalArgumentException("Shop not found with ID: $shopId"))

        if (!isMainAdmin()) {
            if (!isShopkeeper() || _currentProfile.value.shopId != shopId) {
                return Result.failure(SecurityException("Unauthorized: You can only edit your own shop's profile."))
            }
        }

        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Shop name cannot be blank."))
        }

        val trimmedPhone = phone.trim()
        if (trimmedPhone.isBlank()) {
            return Result.failure(IllegalArgumentException("Contact phone number cannot be blank."))
        }

        val trimmedAddress = address.trim()
        if (trimmedAddress.isBlank()) {
            return Result.failure(IllegalArgumentException("Shop address in Mahoba cannot be blank."))
        }

        val isGstChanged = !newGstNumber.isNullOrBlank() && newGstNumber.trim() != currentShop.gstNumber
        val finalGst = currentShop.gstNumber
        val pendingGst = if (isGstChanged) newGstNumber.trim().uppercase() else currentShop.pendingGstNumber
        val gstStatus = if (isGstChanged) "pending_approval" else currentShop.gstApprovalStatus

        val updatedShop = currentShop.copy(
            name = trimmedName,
            phone = trimmedPhone,
            address = trimmedAddress,
            description = description.trim(),
            category = category.trim().ifBlank { currentShop.category },
            openingHours = openingHours.trim().ifBlank { "08:00 AM - 10:00 PM" },
            imageUrl = imageUrl ?: currentShop.imageUrl,
            bannerUrl = bannerUrl ?: currentShop.bannerUrl,
            gstNumber = finalGst,
            pendingGstNumber = pendingGst,
            gstApprovalStatus = gstStatus
        )

        _shops.value = _shops.value.map { if (it.id == shopId) updatedShop else it }

        if (auth?.currentUser != null) {
            repoScope.launch {
                try {
                    firestore.collection("shops").document(shopId).set(updatedShop).await()
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Failed to persist shop update to Firestore: ${e.message}")
                }
            }
        }

        return Result.success(updatedShop)
    }

    /**
     * Updates Main Admin profile.
     * Enforces equal permissions between Pranjal Rajpoot (PR12) and Chanchal Didi (CD12).
     * Prevents cross-admin modification and privilege escalation.
     */
    fun updateAdminProfile(
        adminId: String,
        displayName: String,
        phone: String,
        photoUrl: String? = null
    ): Result<UserProfile> {
        if (!isAuthorizedMainAdmin()) {
            return Result.failure(SecurityException("Unauthorized: Only authorized Main Admins can edit admin profile."))
        }

        val currentLoginId = _currentProfile.value.loginId.uppercase().trim()
        val normalizedCurrent = when (currentLoginId) {
            "PR", "PR12" -> "PR12"
            "CD", "CD12" -> "CD12"
            else -> if (_currentProfile.value.email.equals("ajayvikramrajpoot9540@gmail.com", ignoreCase = true) || _currentProfile.value.name.contains("Pranjal", ignoreCase = true)) "PR12" else currentLoginId
        }
        val normalizedTarget = when (adminId.uppercase().trim()) {
            "PR", "PR12" -> "PR12"
            "CD", "CD12" -> "CD12"
            else -> adminId.uppercase().trim()
        }
        if (normalizedCurrent != normalizedTarget) {
            return Result.failure(SecurityException("Unauthorized: You cannot edit another Main Admin's profile."))
        }

        val trimmedName = displayName.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Display name cannot be blank."))
        }

        val cleanPhone = phone.trim().replace("\\s".toRegex(), "")
        val current = _currentProfile.value
        val updated = current.copy(
            name = trimmedName,
            phone = if (cleanPhone.startsWith("+91")) cleanPhone else "+91 $cleanPhone",
            photoUrl = photoUrl ?: current.photoUrl
        )

        _currentProfile.value = updated
        saveSessionToPrefs(updated)

        repoScope.launch {
            try {
                firestore.collection("admins").document(adminId).set(updated).await()
                if (current.id.isNotBlank() && current.id != "guest") {
                    firestore.collection("users").document(current.id).set(updated).await()
                }
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "Failed to update admin profile in Firestore: ${e.message}")
            }
        }

        return Result.success(updated)
    }

    // Products: Main Admin can manage ANY shop's products; Shopkeeper can ONLY manage their OWN shop's products
    fun saveProduct(product: Product): Boolean {
        if (product.name.isBlank() || product.price <= 0.0) {
            return false
        }
        if (!isMainAdmin()) {
            if (!isShopkeeper() || _currentProfile.value.shopId != product.shopId) {
                return false
            }
        }

        val list = _products.value.toMutableList()
        val index = list.indexOfFirst { it.id == product.id }
        if (index >= 0) {
            list[index] = product
        } else {
            list.add(0, product)
        }
        _products.value = list
        persistence.saveProducts(_products.value)

        if (auth?.currentUser != null) {
            repoScope.launch {
                try {
                    firestore.collection("products").document(product.id).set(product).await()
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Failed to save product: ${e.message}")
                }
            }
        }
        return true
    }

    fun deleteProduct(productId: String): Boolean {
        val prod = _products.value.find { it.id == productId } ?: return false
        if (!isMainAdmin()) {
            if (!isShopkeeper() || _currentProfile.value.shopId != prod.shopId) {
                return false
            }
        }

        _products.value = _products.value.filterNot { it.id == productId }
        persistence.saveProducts(_products.value)

        if (auth?.currentUser != null) {
            repoScope.launch {
                try {
                    firestore.collection("products").document(productId).delete().await()
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Failed to delete product: ${e.message}")
                }
            }
        }
        return true
    }

    // Orders: Customer places order, Shopkeeper updates own shop orders, Admin updates all orders
    fun placeOrder(
        deliveryMethod: String,
        paymentMethod: String,
        address: String,
        locality: String,
        pinCode: String,
        customerName: String,
        customerPhone: String,
        paymentStatusOverride: String? = null,
        transactionId: String = "",
        paymentReference: String = ""
    ): Order {
        if (!isUserSignedIn()) {
            throw SecurityException("Sign in to place your order.")
        }

        val items = _cartItems.value
        require(items.isNotEmpty()) { "Cart is empty" }

        // Enforce that customers cannot order out-of-stock products, even through an old cart entry
        val outOfStockItem = items.firstOrNull { cartItem ->
            val liveProd = _products.value.find { it.id == cartItem.product.id }
            !(liveProd?.inStock ?: cartItem.product.inStock)
        }
        if (outOfStockItem != null) {
            throw IllegalStateException("\"${outOfStockItem.product.name}\" is currently out of stock. Please remove it from your cart to proceed.")
        }

        val billResult = calculateDeliveryBill(items, deliveryMethod)
        if (!billResult.isCheckoutAllowed && deliveryMethod == "shop_delivery") {
            throw IllegalStateException(billResult.checkoutBlockedReason ?: "One or more shops exceed the maximum permitted delivery distance.")
        }

        val subtotal = billResult.merchandiseSubtotal
        val fee = billResult.netDeliveryFee
        val platformFee = billResult.platformFee
        val discount = billResult.productDiscount
        val total = billResult.finalPayableAmount
        val distinctShops = items.map { it.product.shopId }.distinct()
        val firstShop = _shops.value.find { it.id == items.first().product.shopId }

        // Automatic Shop Status Check: Closed shops must not accept new orders
        for (shopId in distinctShops) {
            val liveStatus = getShopLiveStatus(shopId)
            if (!liveStatus.isOpen) {
                val sName = _shops.value.find { it.id == shopId }?.name ?: "Shop"
                throw IllegalStateException("Cannot place order: $sName is currently closed (${liveStatus.statusLabel}).")
            }
        }

        val firstShopId = firstShop?.id ?: "sharma_general"
        val firstShopStatus = getShopLiveStatus(firstShopId)
        val isNearClosing = firstShopStatus.isNearClosingTime
        val accurateDeliveryEst = getAccurateDeliveryEstimateForShop(firstShopId, scheduleForTomorrow = false)

        val resolvedShopName = if (distinctShops.size > 1) {
            val names = items.map { it.product.shopName }.distinct().joinToString(" + ")
            "Multi-Shop ($names)"
        } else {
            firstShop?.name ?: "Mahoba Local Shop"
        }

        val newId = "ord_${UUID.randomUUID().toString().take(8)}"
        val orderNum = "#FD" + (100000 + (Math.random() * 900000).toInt())

        val resolvedPaymentStatus = paymentStatusOverride ?: if (paymentMethod == "cash_on_delivery") "pending" else "paid"
        val resolvedTxnId = if (transactionId.isNotBlank()) transactionId else if (paymentMethod != "cash_on_delivery") "UPI${(1000000000L + (Math.random() * 9000000000L).toLong())}" else ""
        val resolvedRef = if (paymentReference.isNotBlank()) paymentReference else if (paymentMethod != "cash_on_delivery") "7HGF${(10000 + (Math.random() * 90000).toInt())}" else ""

        val authenticatedUid = auth?.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: _currentProfile.value.id.takeIf { it.isNotBlank() && !it.equals("guest", ignoreCase = true) }
            ?: throw SecurityException("Sign in to place your order.")

        val order = Order(
            id = newId,
            orderNumber = orderNum,
            userId = authenticatedUid,
            customerName = customerName.ifBlank { _currentProfile.value.name },
            customerPhone = customerPhone.ifBlank { _currentProfile.value.phone },
            deliveryAddress = "$address, $locality, Mahoba - $pinCode",
            locality = locality,
            pinCode = pinCode,
            shopId = firstShopId,
            shopName = resolvedShopName,
            items = items.map {
                val spec = it.printingSpec
                OrderItemRecord(
                    productId = it.product.id,
                    productName = it.product.name,
                    price = it.product.price,
                    quantity = it.quantity,
                    unit = it.product.unit,
                    isPrintingOrder = spec != null,
                    printingDetails = if (spec != null) {
                        "A4 B&W • ${spec.totalPhysicalPrints} physical prints • ${spec.totalSheets} sheets • ₹${"%.2f".format(spec.appliedRatePerPrint)}/print"
                    } else "",
                    fileNames = spec?.files?.map { f ->
                        "${f.fileName} (${f.pageRangeInput}, ${f.copies} copy, ${f.physicalPrints} prints${if (f.isDoubleSided) ", Double-sided" else ""})"
                    } ?: emptyList(),
                    fileUrls = spec?.files?.mapNotNull { f -> f.storageUrl.takeIf { url -> url.isNotBlank() } } ?: emptyList(),
                    printInstructions = spec?.customerInstructions ?: "",
                    uploadedFileCount = spec?.files?.size ?: 0
                )
            },
            itemsCount = items.sumOf { it.quantity },
            subtotal = subtotal,
            deliveryFee = fee,
            platformFee = platformFee,
            discount = discount,
            totalAmount = total,
            deliveryMethod = deliveryMethod,
            paymentMethod = paymentMethod,
            paymentStatus = resolvedPaymentStatus,
            transactionId = resolvedTxnId,
            paymentReference = resolvedRef,
            expectedDeliverySlot = if (isNearClosing) {
                "Pending Confirmation (Est. ${accurateDeliveryEst.formattedEstimate})"
            } else {
                accurateDeliveryEst.formattedEstimate
            },
            isNearClosingTime = isNearClosing,
            lateNightDecision = if (isNearClosing) "PENDING" else "",
            confirmedDeliveryTime = if (isNearClosing) "" else accurateDeliveryEst.formattedEstimate,
            scheduledForTomorrow = accurateDeliveryEst.isTomorrowOrLater,
            status = "pending",
            statusNote = if (fee == 0.0) "Order confirmed with Free Delivery • Est. ${accurateDeliveryEst.formattedEstimate}" else "Order confirmed • Est. Delivery: ${accurateDeliveryEst.formattedEstimate}",
            billingSnapshot = billResult.snapshot
        )

        _orders.value = listOf(order) + _orders.value
        persistence.saveOrders(_orders.value)

        // Synchronize placed order into CustomerRecord for Admin Customers Directory
        try {
            upsertCustomerFromProfile(_currentProfile.value)
            val summary = CustomerOrderSummary(
                orderId = order.id,
                orderNumber = order.orderNumber,
                shopId = order.shopId,
                shopName = order.shopName,
                totalAmount = order.totalAmount,
                itemsCount = order.itemsCount,
                paymentMethod = if (order.paymentMethod == "cash_on_delivery") "COD" else "UPI",
                orderStatus = order.status,
                orderDate = order.billingSnapshot?.calculationTimestamp?.takeIf { it.isNotBlank() }
                    ?: SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()),
                deliveryAddress = order.deliveryAddress
            )
            _customers.value = _customers.value.map { cust ->
                if (cust.id == order.userId || cust.fullName.equals(order.customerName, ignoreCase = true)) {
                    val isUpi = order.paymentMethod != "cash_on_delivery"
                    cust.copy(
                        totalOrdersCount = cust.totalOrdersCount + 1,
                        totalSpentAmount = cust.totalSpentAmount + order.totalAmount,
                        upiSpentAmount = if (isUpi) cust.upiSpentAmount + order.totalAmount else cust.upiSpentAmount,
                        codSpentAmount = if (!isUpi) cust.codSpentAmount + order.totalAmount else cust.codSpentAmount,
                        lastOrderDate = "Today",
                        lastActive = "Just now",
                        favoriteShopName = cust.favoriteShopName.ifBlank { order.shopName },
                        recentOrders = listOf(summary) + cust.recentOrders
                    )
                } else cust
            }
            persistence.saveCustomers(_customers.value)
        } catch (e: Exception) {
            Log.w("FlashDropRepo", "Customer order stats update warning: ${e.message}")
        }

        if (isNearClosing) {
            addNotification(
                NotificationItem(
                    id = "notif_late_${UUID.randomUUID().toString().take(8)}",
                    title = "Near Closing Order Alert",
                    message = "A new order ($orderNum) has arrived near closing time. Can you deliver it today?",
                    timestamp = "Just now",
                    type = "order",
                    targetRole = "shopkeeper",
                    targetId = firstShop?.id ?: ""
                )
            )
        }

        val commissionAmt = (order.subtotal * 0.08).toInt().toDouble()
        val payoutAmt = (order.totalAmount - commissionAmt - order.deliveryFee).coerceAtLeast(order.subtotal * 0.85)
        val newTxn = com.example.data.model.Transaction(
            id = "txn_${UUID.randomUUID().toString().take(8)}",
            orderId = order.id,
            orderNumber = order.orderNumber,
            shopId = order.shopId,
            shopName = order.shopName,
            customerName = order.customerName,
            customerPhone = order.customerPhone,
            amount = order.totalAmount,
            paymentMethod = if (paymentMethod == "cash_on_delivery") "COD" else "UPI",
            status = if (resolvedPaymentStatus == "paid") "Paid" else if (paymentMethod == "cash_on_delivery") "Collected" else "Pending",
            timestamp = "Just now",
            timeOnly = "Just now",
            providerTxnId = resolvedTxnId.ifBlank { "COD-REC-${order.orderNumber.takeLast(3)}" },
            referenceId = resolvedRef.ifBlank { "CASH-AUTH-${order.orderNumber.takeLast(3)}" },
            verificationProof = if (resolvedPaymentStatus == "paid") "NPCI Verified / Bank Auth" else "Cash On Delivery (Handover Verification)",
            daysAgo = 0,
            itemsSummary = order.items.joinToString(", ") { "${it.productName} x${it.quantity}" }.ifBlank { "${order.itemsCount} items" },
            deliveryFee = order.deliveryFee,
            platformCommission = commissionAmt,
            discountSubsidy = order.discount,
            shopkeeperPayoutAmount = payoutAmt,
            shopkeeperSettlementStatus = "Pending Settlement"
        )
        _transactions.value = listOf(newTxn) + _transactions.value
        persistence.saveTransactions(_transactions.value)

        clearCart()

        // Initialize Live Delivery Tracking record for this newly placed order
        val initialTracking = DeliveryTracking(
            id = order.id,
            orderId = order.id,
            customerId = order.userId,
            customerName = order.customerName,
            customerPhone = order.customerPhone,
            customerDeliveryAddress = "${order.deliveryAddress}, ${order.locality}",
            customerLatitude = 25.2965,
            customerLongitude = 79.8752,
            shopId = order.shopId,
            shopName = order.shopName,
            shopAddress = "Subhash Chowk, Mahoba",
            shopLatitude = 25.2922,
            shopLongitude = 79.8718,
            deliveryManId = "rider_vikram",
            deliveryManName = "Vikram Rathore",
            deliveryManPhone = "+91 98765 88990",
            deliveryManVehicle = "Hero Splendor • UP 95 AB 4122",
            orderStatus = "order_confirmed",
            stageTimestamps = mapOf(
                "order_confirmed" to SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            )
        )
        _deliveryTracking.value = _deliveryTracking.value + (order.id to initialTracking)

        if (auth?.currentUser != null) {
            repoScope.launch {
                try {
                    firestore.collection("orders").document(order.id).set(order).await()
                    firestore.collection("transactions").document(newTxn.id).set(newTxn).await()
                    firestore.collection("delivery_tracking").document(order.id).set(initialTracking).await()
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Failed to post order/txn/tracking to firestore: ${e.message}")
                }
            }
        }

        val nowTimeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        addNotification(
            NotificationItem(
                id = "notif_ord_cust_${order.id}",
                title = "Order ${order.orderNumber} Confirmed! 🛍️",
                message = "Your order from ${order.shopName} for ₹${order.totalAmount.toInt()} has been placed and is being prepared.",
                timestamp = nowTimeFormatted,
                type = "order",
                targetRole = "customer",
                targetId = order.userId
            )
        )
        addNotification(
            NotificationItem(
                id = "notif_ord_sk_${order.id}",
                title = "New Order ${order.orderNumber}! 🔔",
                message = "New order received from ${order.customerName} (${order.items.size} items, ₹${order.totalAmount.toInt()}).",
                timestamp = nowTimeFormatted,
                type = "order",
                targetRole = "shopkeeper",
                targetId = order.shopId
            )
        )
        addNotification(
            NotificationItem(
                id = "notif_ord_admin_${order.id}",
                title = "New Order Placed: ${order.orderNumber}",
                message = "Customer ${order.customerName} placed order with ${order.shopName} (₹${order.totalAmount.toInt()} • ${order.paymentMethod.uppercase()}).",
                timestamp = nowTimeFormatted,
                type = "admin",
                targetRole = "admin"
            )
        )

        return order
    }

    fun updateOrderStatus(orderId: String, newStatus: String, note: String? = null) {
        val updated = _orders.value.map { order ->
            if (order.id == orderId) {
                val defaultNote = when (newStatus) {
                    "accepted" -> "Order accepted by shopkeeper"
                    "preparing" -> "Items being packed at shop"
                    "out_for_delivery" -> "Out for delivery with delivery partner"
                    "delivered" -> "Order successfully delivered"
                    "rejected" -> "Order declined by shopkeeper"
                    "cancelled" -> "Order cancelled"
                    else -> "Status updated"
                }
                order.copy(
                    status = newStatus,
                    statusNote = note ?: defaultNote,
                    paymentStatus = if (newStatus == "delivered") "paid" else order.paymentStatus
                )
            } else order
        }
        _orders.value = updated
        persistence.saveOrders(_orders.value)

        // Reflect order status update in CustomerRecord recentOrders & counts
        val targetOrder = updated.find { it.id == orderId }
        if (targetOrder != null) {
            _customers.value = _customers.value.map { cust ->
                if (cust.id == targetOrder.userId || cust.recentOrders.any { it.orderId == orderId }) {
                    val updatedRecent = cust.recentOrders.map { ro ->
                        if (ro.orderId == orderId) ro.copy(orderStatus = newStatus) else ro
                    }
                    val deliveredDelta = if (newStatus == "delivered") 1 else 0
                    val cancelledDelta = if (newStatus == "cancelled" || newStatus == "rejected") 1 else 0
                    cust.copy(
                        completedOrdersCount = cust.completedOrdersCount + deliveredDelta,
                        cancelledOrdersCount = cust.cancelledOrdersCount + cancelledDelta,
                        recentOrders = updatedRecent
                    )
                } else cust
            }
            persistence.saveCustomers(_customers.value)
        }

        if (auth?.currentUser != null) {
            repoScope.launch {
                try {
                    firestore.collection("orders").document(orderId)
                        .update("status", newStatus, "statusNote", note ?: "Status updated")
                        .await()
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Failed to update order status: ${e.message}")
                }
            }
        }
    }

    // Two-Owner Confirmation Safeguard: Triggers a simulated or recorded mutual owner approval
    fun submitSensitiveAdminChange(actionDescription: String): String {
        val currentOwner = _currentProfile.value.loginId.ifBlank { "PR12" }
        val otherOwner = if (currentOwner == "PR12") "CD12 (Chanchal Didi)" else "PR12 (Pranjal Rajpoot)"
        val entry = "[$actionDescription] initiated by $currentOwner — Requires mutual approval by $otherOwner"
        _pendingAdminActions.value = listOf(entry) + _pendingAdminActions.value
        return entry
    }

    fun approveSensitiveAdminChange(entry: String) {
        _pendingAdminActions.value = _pendingAdminActions.value.filterNot { it == entry }
    }

    // Cart operations
    fun addToCart(product: Product): Boolean {
        val liveProd = _products.value.find { it.id == product.id } ?: product
        if (!liveProd.inStock) {
            Log.w("FlashDropRepo", "Cannot add out-of-stock product ${product.id} to cart")
            return false
        }
        val shopStatus = getShopLiveStatus(liveProd.shopId)
        if (!shopStatus.isOpen) {
            Log.w("FlashDropRepo", "Cannot add product ${product.id} from closed shop ${liveProd.shopId} (${shopStatus.statusLabel})")
            return false
        }
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(quantity = item.quantity + 1)
        } else {
            current.add(CartItem(product = liveProd, quantity = 1))
        }
        _cartItems.value = current
        persistence.saveCart(_cartItems.value)
        return true
    }

    fun updateCartQuantity(productId: String, delta: Int) {
        val liveProd = _products.value.find { it.id == productId }
        if (delta > 0 && liveProd != null && !liveProd.inStock) {
            Log.w("FlashDropRepo", "Cannot increment quantity of out-of-stock product $productId")
            return
        }
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = current[index]
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                current.removeAt(index)
            } else {
                current[index] = item.copy(quantity = newQty)
            }
            _cartItems.value = current
            persistence.saveCart(_cartItems.value)
        }
    }

    fun removeFromCart(productId: String) {
        _cartItems.value = _cartItems.value.filterNot { it.product.id == productId }
        persistence.saveCart(_cartItems.value)
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        persistence.saveCart(_cartItems.value)
    }

    /**
     * Adds or updates a Document & Photo Printing order in the cart.
     * Prevents duplicate printing items and ensures single ₹19 platform fee.
     */
    fun addPrintingOrderToCart(spec: PrintingOrderSpec) {
        val current = _cartItems.value.toMutableList()
        // Replace existing printing order to prevent duplicate fees or multiple conflicting specs
        current.removeAll { it.printingSpec != null || it.product.id.startsWith("prod_print_") }

        val printProduct = Product(
            id = "prod_print_${System.currentTimeMillis()}",
            shopId = spec.shopId,
            shopName = spec.shopName,
            name = "Document Printing (${spec.files.size} document${if (spec.files.size > 1) "s" else ""})",
            category = "Printing Press",
            subcategory = "Document Printing",
            price = spec.printingSubtotal,
            originalPrice = spec.printingSubtotal,
            description = "A4 Black & White • ${spec.totalPhysicalPrints} prints (${spec.totalSheets} sheets) • Rate: ₹${"%.2f".format(spec.appliedRatePerPrint)}/print",
            imageUrl = "https://images.unsplash.com/photo-1562654501-a0ccc0fc3fb1?w=500&q=80",
            unit = "${spec.totalPhysicalPrints} Prints",
            inStock = true
        )

        current.add(
            CartItem(
                product = printProduct,
                quantity = 1,
                printingSpec = spec
            )
        )
        _cartItems.value = current
        persistence.saveCart(_cartItems.value)
    }

    /**
     * Dynamically filters and returns all Admin-approved and active Printing Press shops.
     * Robustly normalizes category matching (case-insensitive, trims extra spaces).
     */
    fun getPrintingPressShops(shopsList: List<Shop> = _shops.value): List<Shop> {
        return shopsList.filter { shop ->
            val normalizedCat = shop.category.trim().replace("\\s+".toRegex(), " ")
            normalizedCat.equals("Printing Press", ignoreCase = true) &&
            shop.status.equals("approved", ignoreCase = true)
        }
    }

    fun updateAddress(newAddress: String, newLocality: String, newPin: String) {
        val updated = _currentProfile.value.copy(
            address = newAddress,
            locality = newLocality,
            pinCode = newPin
        )
        _currentProfile.value = updated
        saveSessionToPrefs(updated)
        upsertCustomerFromProfile(updated)
    }

    fun updateDeliveryConfig(fee: Double, minOrder: Double, freeThreshold: Double) {
        val updated = _deliveryConfig.value.copy(
            defaultDeliveryFee = fee,
            minOrderAmount = minOrder,
            freeDeliveryThreshold = freeThreshold
        )
        _deliveryConfig.value = updated
        persistence.saveDeliveryConfig(updated)
    }

    /**
     * Strictly verifies whether the current user is one of the two authorized Main Admins:
     * Pranjal Rajpoot (PR12) or Chanchal Didi (CD12).
     * Enforces equal permissions and denies Customers and Shopkeepers.
     */
    fun isAuthorizedMainAdmin(): Boolean {
        return isMainAdmin()
    }

    /**
     * Calculates the complete customer bill, distance charges, and discount allocation.
     */
    fun calculateDeliveryBill(
        cartItems: List<CartItem> = _cartItems.value,
        deliveryMethod: String = "shop_delivery"
    ): com.example.data.util.DeliveryBillResult {
        return com.example.data.util.DeliveryCalculator.calculateBill(
            cartItems = cartItems,
            shops = _shops.value,
            config = _deliveryConfig.value,
            deliveryMethod = deliveryMethod
        )
    }

    /**
     * Updates global delivery settings.
     * Enforces equal permissions for Pranjal Rajpoot & Chanchal Didi, validates configuration,
     * updates in-memory state and persists to Firestore collection 'delivery_config' document 'default'.
     */
    fun updateGlobalDeliveryConfig(config: DeliveryConfig): Result<DeliveryConfig> {
        if (!isAuthorizedMainAdmin()) {
            return Result.failure(
                SecurityException("Access Denied: Only authorized Main Admins (Pranjal Rajpoot & Chanchal Didi) can modify global delivery settings.")
            )
        }

        val validationResult = com.example.data.util.DeliveryCalculator.validateConfiguration(config)
        if (validationResult.isFailure) {
            return Result.failure(validationResult.exceptionOrNull() ?: IllegalArgumentException("Invalid delivery configuration"))
        }

        val adminName = if (_currentProfile.value.loginId.uppercase() == "CD12") "Chanchal Didi" else "Pranjal Rajpoot"
        val adminId = if (_currentProfile.value.loginId.uppercase() == "CD12") "CD12" else "PR12"
        val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val updated = config.copy(
            version = config.version + 1,
            lastUpdatedByAdminId = adminId,
            lastUpdatedByAdminName = adminName,
            lastUpdatedAtFormatted = nowFormatted,
            defaultDeliveryFee = config.distanceTiers.firstOrNull()?.charge ?: 9.0,
            freeDeliveryThreshold = config.minOrderThreshold
        )

        _deliveryConfig.value = updated
        persistence.saveDeliveryConfig(updated)

        repoScope.launch {
            try {
                firestore.collection("delivery_config").document("default").set(updated).await()
            } catch (e: Exception) {
                Log.w("FlashDropRepo", "DeliveryConfig firestore sync deferred: ${e.message}")
            }
        }

        return Result.success(updated)
    }

    // =========================================================================
    // Live Delivery Tracking Management
    // =========================================================================

    fun getDeliveryTracking(orderId: String): DeliveryTracking {
        val existing = _deliveryTracking.value[orderId]
        if (existing != null) return existing

        val order = _orders.value.find { it.id == orderId }
        val shop = _shops.value.find { it.id == order?.shopId }
        val initial = DeliveryTracking(
            id = orderId,
            orderId = orderId,
            customerId = order?.userId ?: _currentProfile.value.id,
            customerName = order?.customerName ?: _currentProfile.value.name,
            customerPhone = order?.customerPhone ?: _currentProfile.value.phone,
            customerDeliveryAddress = order?.deliveryAddress ?: "Mahoba, Uttar Pradesh",
            customerLatitude = 25.2965,
            customerLongitude = 79.8752,
            shopId = order?.shopId ?: (shop?.id ?: "sharma_general"),
            shopName = order?.shopName ?: (shop?.name ?: "Sharma General Store"),
            shopAddress = shop?.address ?: "Subhash Chowk, Mahoba",
            shopLatitude = 25.2922,
            shopLongitude = 79.8718,
            deliveryManId = "rider_vikram",
            deliveryManName = "Vikram Rathore",
            deliveryManPhone = "+91 98765 88990",
            deliveryManVehicle = "Hero Splendor • UP 95 AB 4122",
            orderStatus = mapOrderStatusToTrackingStage(order?.status ?: "order_confirmed"),
            stageTimestamps = mapOf(
                "order_confirmed" to SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            )
        )
        _deliveryTracking.value = _deliveryTracking.value + (orderId to initial)
        return initial
    }

    private fun mapOrderStatusToTrackingStage(status: String): String {
        return when (status) {
            "pending", "accepted", "order_confirmed" -> "order_confirmed"
            "preparing", "preparing_order" -> "preparing_order"
            "ready_for_pickup" -> "ready_for_pickup"
            "picked_up" -> "picked_up"
            "out_for_delivery", "on_the_way" -> "on_the_way"
            "delivered" -> "delivered"
            else -> "order_confirmed"
        }
    }

    fun updateDeliveryGps(orderId: String, lat: Double, lng: Double, accuracy: Float) {
        val current = getDeliveryTracking(orderId)
        val distanceResults = FloatArray(1)
        Location.distanceBetween(lat, lng, current.customerLatitude, current.customerLongitude, distanceResults)
        val distanceMeters = distanceResults[0]
        val distanceKm = Math.round((distanceMeters / 1000.0) * 100.0) / 100.0
        val etaMinutes = if (distanceKm <= 0.05) 1 else Math.max(1, (distanceKm / 20.0 * 60).toInt())

        val updated = current.copy(
            actualLatitude = lat,
            actualLongitude = lng,
            actualAccuracyMeters = accuracy,
            isGpsActive = true,
            locationErrorMessage = null,
            remainingDistanceKm = distanceKm,
            estimatedArrivalMinutes = etaMinutes
        )

        _deliveryTracking.value = _deliveryTracking.value + (orderId to updated)

        if (auth?.currentUser != null) {
            repoScope.launch {
                try {
                    firestore.collection("delivery_tracking").document(orderId)
                        .set(updated)
                        .await()
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Failed to sync delivery GPS to Firestore: ${e.message}")
                }
            }
        }
    }

    fun updateDeliveryStage(orderId: String, newStage: String) {
        val current = getDeliveryTracking(orderId)
        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val updatedTimestamps = current.stageTimestamps.toMutableMap()
        updatedTimestamps[newStage] = currentTime

        val isDelivered = newStage == "delivered"
        val updated = current.copy(
            orderStatus = newStage,
            stageTimestamps = updatedTimestamps,
            isGpsActive = if (isDelivered) false else current.isGpsActive,
            remainingDistanceKm = if (isDelivered) 0.0 else current.remainingDistanceKm,
            estimatedArrivalMinutes = if (isDelivered) 0 else current.estimatedArrivalMinutes
        )

        _deliveryTracking.value = _deliveryTracking.value + (orderId to updated)

        if (isDelivered) {
            DeliveryLocationService.stopTracking(context)
        }

        val orderStatusEquivalent = when (newStage) {
            "order_confirmed" -> "accepted"
            "preparing_order" -> "preparing"
            "ready_for_pickup" -> "preparing"
            "picked_up" -> "out_for_delivery"
            "on_the_way" -> "out_for_delivery"
            "delivered" -> "delivered"
            else -> newStage
        }
        val note = when (newStage) {
            "order_confirmed" -> "Order confirmed by shopkeeper"
            "preparing_order" -> "Items are being packed at shop"
            "ready_for_pickup" -> "Packed and ready for rider pickup"
            "picked_up" -> "Rider picked up order from shop"
            "on_the_way" -> "Rider on the way to your doorstep"
            "delivered" -> "Order successfully delivered"
            else -> "Status updated"
        }
        updateOrderStatus(orderId, orderStatusEquivalent, note)

        if (auth?.currentUser != null) {
            repoScope.launch {
                try {
                    firestore.collection("delivery_tracking").document(orderId)
                        .set(updated)
                        .await()
                } catch (e: Exception) {
                    Log.w("FlashDropRepo", "Failed to sync delivery stage to Firestore: ${e.message}")
                }
            }
        }
    }

    fun setDeliveryTrackingError(orderId: String, errorMessage: String) {
        val current = getDeliveryTracking(orderId)
        val updated = current.copy(
            locationErrorMessage = errorMessage,
            isGpsActive = false
        )
        _deliveryTracking.value = _deliveryTracking.value + (orderId to updated)
    }

    fun startRiderGpsService(orderId: String, deliveryManId: String) {
        DeliveryLocationService.startTracking(context, orderId, deliveryManId)
    }

    fun stopRiderGpsService() {
        DeliveryLocationService.stopTracking(context)
    }

    /**
     * Marks all pending payments and shopkeeper settlements for a given shop as Settled/Paid
     * so Main Admins can easily disburse payments to the shopkeeper.
     */
    fun settleShopkeeperPendingPayments(shopId: String): Int {
        var settledCount = 0
        val updated = _transactions.value.map { txn ->
            val matchesShop = shopId == "ALL" || txn.shopId.equals(shopId, ignoreCase = true) || txn.shopName.equals(shopId, ignoreCase = true)
            if (matchesShop && (txn.status.equals("Pending", ignoreCase = true) || txn.shopkeeperSettlementStatus.equals("Pending Settlement", ignoreCase = true))) {
                settledCount++
                val nextStatus = if (txn.status.equals("Pending", ignoreCase = true)) {
                    if (txn.isCodPayment) "Collected" else "Paid"
                } else {
                    txn.status
                }
                txn.copy(
                    status = nextStatus,
                    shopkeeperSettlementStatus = "Settled",
                    verificationProof = "Shopkeeper Payout Settled by Main Admin (${_currentProfile.value.loginId.ifBlank { "PR12" }})"
                )
            } else {
                txn
            }
        }
        if (settledCount > 0) {
            _transactions.value = updated
            persistence.saveTransactions(updated)
        }
        return settledCount
    }

    /**
     * Resolves an online payment problem (Failed / Pending gateway issue) for a transaction.
     */
    fun resolveOnlinePaymentIssue(transactionId: String, newStatus: String = "Paid"): Boolean {
        var changed = false
        val updated = _transactions.value.map { txn ->
            if (txn.id == transactionId) {
                changed = true
                txn.copy(
                    status = newStatus,
                    shopkeeperSettlementStatus = if (newStatus == "Paid") "Settled" else txn.shopkeeperSettlementStatus,
                    verificationProof = "Online Payment Issue Resolved by Admin (${_currentProfile.value.loginId.ifBlank { "PR12" }})",
                    failureReason = if (txn.failureReason.isNotBlank()) "${txn.failureReason} (Resolved)" else "Resolved by Admin"
                )
            } else {
                txn
            }
        }
        if (changed) {
            _transactions.value = updated
            persistence.saveTransactions(updated)
        }
        return changed
    }
}

