package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.data.model.CartItem
import com.example.data.model.CustomerOrderSummary
import com.example.data.model.CustomerRecord
import com.example.data.model.DeliveryAddress
import com.example.data.model.DeliveryConfig
import com.example.data.model.DeliveryTracking
import com.example.data.model.DistanceTier
import com.example.data.model.Order
import com.example.data.model.OrderBillingSnapshot
import com.example.data.model.OrderItemRecord
import com.example.data.model.PrintingOrderSpec
import com.example.data.model.Product
import com.example.data.model.RegisteredAccount
import com.example.data.model.Shop
import com.example.data.model.ShopDeliveryBreakdown
import com.example.data.model.ShopRegistrationConfig
import com.example.data.model.ShopVerificationRequest
import com.example.data.model.ShopkeeperAccount
import com.example.data.model.Transaction
import com.example.data.model.DaySchedule
import com.example.data.model.UserProfile
import com.example.data.model.WeeklySchedule
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Robust, offline-first local persistence manager for FlashDrop.
 *
 * Saves and restores all critical application data locally on device disk:
 * - User Profiles & Profile Photos (names, addresses, phone numbers, avatars)
 * - Shops (including Printing Press shops, details, ratings, logos)
 * - Products (custom added or edited products, prices, stock)
 * - Orders (items, totals, billing snapshots, statuses, delivery charges)
 * - Cart (current items, quantities)
 * - Global Delivery Configuration (rates, free delivery policy)
 * - Admin Shop Registration Mode (3-Step ON/OFF setting, timestamps)
 * - Shopkeeper Business Verification Requests (applications, document paths, review states)
 * - Shopkeeper Accounts & Credentials
 * - Transactions & Delivery Tracking
 *
 * All files are stored under app-internal filesDir/flashdrop_data/ ensuring they survive
 * app backgrounding, task termination, and restarts.
 */
class AppPersistenceManager(private val context: Context) {

    private val dataDir = File(context.filesDir, "flashdrop_data").apply {
        if (!exists()) mkdirs()
    }

    private val lock = Any()

    companion object {
        private const val TAG = "FlashDropPersistence"
        private const val FILE_PROFILE = "user_profile.json"
        private const val FILE_SHOPS = "shops.json"
        private const val FILE_PRODUCTS = "products.json"
        private const val FILE_ORDERS = "orders.json"
        private const val FILE_CART = "cart.json"
        private const val FILE_DELIVERY_CONFIG = "delivery_config.json"
        private const val FILE_REGISTRATION_CONFIG = "registration_config.json"
        private const val FILE_VERIFICATION_REQUESTS = "verification_requests.json"
        private const val FILE_SHOPKEEPERS = "shopkeepers.json"
        private const val FILE_TRANSACTIONS = "transactions.json"
        private const val FILE_DELIVERY_TRACKING = "delivery_tracking.json"
        private const val FILE_REGISTERED_USERS = "registered_users.json"
        private const val FILE_SHOP_SCHEDULES = "shop_schedules.json"
        private const val FILE_CUSTOMERS = "customers.json"
    }

    private fun writeJsonFile(fileName: String, content: String) {
        synchronized(lock) {
            try {
                val targetFile = File(dataDir, fileName)
                val tempFile = File(dataDir, "$fileName.tmp")
                tempFile.writeText(content, Charsets.UTF_8)
                if (tempFile.renameTo(targetFile)) {
                    // Success
                } else {
                    targetFile.writeText(content, Charsets.UTF_8)
                    tempFile.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing $fileName: ${e.message}", e)
            }
        }
    }

    private fun readJsonFile(fileName: String): String? {
        synchronized(lock) {
            return try {
                val file = File(dataDir, fileName)
                if (file.exists() && file.length() > 0) {
                    file.readText(Charsets.UTF_8)
                } else null
            } catch (e: Exception) {
                Log.e(TAG, "Error reading $fileName: ${e.message}", e)
                null
            }
        }
    }

    // ==========================================
    // 1. User Profile
    // ==========================================
    fun saveUserProfile(profile: UserProfile) {
        try {
            val json = JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("email", profile.email)
                put("phone", profile.phone)
                put("role", profile.role)
                put("loginId", profile.loginId)
                put("ownerTitle", profile.ownerTitle)
                put("shopId", profile.shopId)
                put("shopName", profile.shopName)
                put("isVerifiedAdmin", profile.isVerifiedAdmin)
                put("isVerifiedShopkeeper", profile.isVerifiedShopkeeper)
                put("isAuthenticated", profile.isAuthenticated)
                put("photoUrl", profile.photoUrl)
                put("address", profile.address)
                put("locality", profile.locality)
                put("pinCode", profile.pinCode)
                put("city", profile.city)
                put("state", profile.state)

                val addrArray = JSONArray()
                profile.savedAddresses.forEach { addr ->
                    addrArray.put(JSONObject().apply {
                        put("id", addr.id)
                        put("name", addr.name)
                        put("phone", addr.phone)
                        put("countryCode", addr.countryCode)
                        put("label", addr.label)
                        put("houseNumber", addr.houseNumber)
                        put("buildingBlock", addr.buildingBlock)
                        put("street", addr.street)
                        put("area", addr.area)
                        put("city", addr.city)
                        put("state", addr.state)
                        put("pinCode", addr.pinCode)
                        put("landmark", addr.landmark)
                        put("isDefault", addr.isDefault)
                    })
                }
                put("savedAddresses", addrArray)
            }
            writeJsonFile(FILE_PROFILE, json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user profile: ${e.message}", e)
        }
    }

    fun loadUserProfile(): UserProfile? {
        val raw = readJsonFile(FILE_PROFILE) ?: return null
        return try {
            val json = JSONObject(raw)
            val addrList = mutableListOf<DeliveryAddress>()
            val addrArray = json.optJSONArray("savedAddresses")
            if (addrArray != null) {
                for (i in 0 until addrArray.length()) {
                    val a = addrArray.getJSONObject(i)
                    addrList.add(
                        DeliveryAddress(
                            id = a.optString("id"),
                            name = a.optString("name", json.optString("name", "")),
                            phone = a.optString("phone", json.optString("phone", "")),
                            countryCode = a.optString("countryCode", "+91"),
                            label = a.optString("label", "Home"),
                            houseNumber = a.optString("houseNumber"),
                            buildingBlock = a.optString("buildingBlock"),
                            street = a.optString("street"),
                            area = a.optString("area", "Subhash Chowk"),
                            city = a.optString("city", "Mahoba"),
                            state = a.optString("state", "Uttar Pradesh"),
                            pinCode = a.optString("pinCode", "210427"),
                            landmark = a.optString("landmark"),
                            isDefault = a.optBoolean("isDefault", false)
                        )
                    )
                }
            }

            UserProfile(
                id = json.optString("id", "guest"),
                name = json.optString("name", "Guest"),
                email = json.optString("email", ""),
                phone = json.optString("phone", ""),
                role = json.optString("role", "customer"),
                loginId = json.optString("loginId", ""),
                ownerTitle = json.optString("ownerTitle", ""),
                shopId = json.optString("shopId", ""),
                shopName = json.optString("shopName", ""),
                isVerifiedAdmin = json.optBoolean("isVerifiedAdmin", false),
                isVerifiedShopkeeper = json.optBoolean("isVerifiedShopkeeper", false),
                isAuthenticated = json.optBoolean("isAuthenticated", false),
                photoUrl = json.optString("photoUrl", ""),
                address = json.optString("address", "Subhash Chowk, Chhatarpur Road"),
                locality = json.optString("locality", "Alha Chowk"),
                pinCode = json.optString("pinCode", "210427"),
                city = json.optString("city", "Mahoba"),
                state = json.optString("state", "Uttar Pradesh"),
                savedAddresses = addrList
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse user profile: ${e.message}", e)
            null
        }
    }

    // ==========================================
    // 2. Shops
    // ==========================================
    fun saveShops(shops: List<Shop>) {
        try {
            val array = JSONArray()
            shops.forEach { s ->
                array.put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("category", s.category)
                    put("rating", s.rating)
                    put("ratingCount", s.ratingCount)
                    put("distance", s.distance)
                    put("deliveryTime", s.deliveryTime)
                    put("deliveryAvailable", s.deliveryAvailable)
                    put("deliveryFee", s.deliveryFee)
                    put("minOrder", s.minOrder)
                    put("isOpen", s.isOpen)
                    put("status", s.status)
                    put("ownerName", s.ownerName)
                    put("gstNumber", s.gstNumber)
                    put("password", s.password)
                    put("passwordHash", s.passwordHash)
                    put("rejectionReason", s.rejectionReason)
                    put("suspensionReason", s.suspensionReason)
                    put("description", s.description)
                    put("address", s.address)
                    put("phone", s.phone)
                    put("imageUrl", s.imageUrl)
                    put("bannerUrl", s.bannerUrl)
                    put("openingHours", s.openingHours)
                    put("pendingGstNumber", s.pendingGstNumber)
                    put("gstApprovalStatus", s.gstApprovalStatus)
                    put("ownerId", s.ownerId)
                })
            }
            writeJsonFile(FILE_SHOPS, array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save shops: ${e.message}", e)
        }
    }

    fun loadShops(): List<Shop>? {
        val raw = readJsonFile(FILE_SHOPS) ?: return null
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<Shop>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    Shop(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        category = o.optString("category", "Grocery"),
                        rating = o.optDouble("rating", 4.5),
                        ratingCount = o.optInt("ratingCount", 50),
                        distance = o.optString("distance", "1.2 km"),
                        deliveryTime = o.optString("deliveryTime", "20-30 mins"),
                        deliveryAvailable = o.optBoolean("deliveryAvailable", true),
                        deliveryFee = o.optDouble("deliveryFee", 20.0),
                        minOrder = o.optDouble("minOrder", 100.0),
                        isOpen = o.optBoolean("isOpen", true),
                        status = o.optString("status", "approved"),
                        ownerName = o.optString("ownerName"),
                        gstNumber = o.optString("gstNumber"),
                        password = o.optString("password"),
                        passwordHash = o.optString("passwordHash"),
                        rejectionReason = o.optString("rejectionReason"),
                        suspensionReason = o.optString("suspensionReason"),
                        description = o.optString("description"),
                        address = o.optString("address", "Mahoba, Uttar Pradesh"),
                        phone = o.optString("phone", "+91 98765 43210"),
                        imageUrl = o.optString("imageUrl"),
                        bannerUrl = o.optString("bannerUrl"),
                        openingHours = o.optString("openingHours", "08:00 AM - 10:00 PM"),
                        pendingGstNumber = o.optString("pendingGstNumber"),
                        gstApprovalStatus = o.optString("gstApprovalStatus", "verified"),
                        ownerId = o.optString("ownerId")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse shops: ${e.message}", e)
            null
        }
    }

    // ==========================================
    // 3. Products
    // ==========================================
    fun saveProducts(products: List<Product>) {
        try {
            val array = JSONArray()
            products.forEach { p ->
                array.put(JSONObject().apply {
                    put("id", p.id)
                    put("shopId", p.shopId)
                    put("shopName", p.shopName)
                    put("name", p.name)
                    put("category", p.category)
                    put("subcategory", p.subcategory)
                    put("price", p.price)
                    put("originalPrice", p.originalPrice)
                    put("description", p.description)
                    put("imageUrl", p.imageUrl)
                    put("unit", p.unit)
                    put("inStock", p.inStock)
                    put("rating", p.rating)
                    put("ratingCount", p.ratingCount)
                })
            }
            writeJsonFile(FILE_PRODUCTS, array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save products: ${e.message}", e)
        }
    }

    fun loadProducts(): List<Product>? {
        val raw = readJsonFile(FILE_PRODUCTS) ?: return null
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<Product>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    Product(
                        id = o.optString("id"),
                        shopId = o.optString("shopId"),
                        shopName = o.optString("shopName"),
                        name = o.optString("name"),
                        category = o.optString("category", "Grocery"),
                        subcategory = o.optString("subcategory", "All"),
                        price = o.optDouble("price", 0.0),
                        originalPrice = o.optDouble("originalPrice", 0.0),
                        description = o.optString("description"),
                        imageUrl = o.optString("imageUrl"),
                        unit = o.optString("unit", "1 pc"),
                        inStock = o.optBoolean("inStock", true),
                        rating = o.optDouble("rating", 4.5),
                        ratingCount = o.optInt("ratingCount", 50)
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse products: ${e.message}", e)
            null
        }
    }

    // ==========================================
    // 4. Orders
    // ==========================================
    fun saveOrders(orders: List<Order>) {
        try {
            val array = JSONArray()
            orders.forEach { o ->
                array.put(JSONObject().apply {
                    put("id", o.id)
                    put("orderNumber", o.orderNumber)
                    put("userId", o.userId)
                    put("customerName", o.customerName)
                    put("customerPhone", o.customerPhone)
                    put("deliveryAddress", o.deliveryAddress)
                    put("locality", o.locality)
                    put("pinCode", o.pinCode)
                    put("shopId", o.shopId)
                    put("shopName", o.shopName)
                    put("itemsCount", o.itemsCount)
                    put("subtotal", o.subtotal)
                    put("deliveryFee", o.deliveryFee)
                    put("platformFee", o.platformFee)
                    put("discount", o.discount)
                    put("totalAmount", o.totalAmount)
                    put("deliveryMethod", o.deliveryMethod)
                    put("paymentMethod", o.paymentMethod)
                    put("paymentStatus", o.paymentStatus)
                    put("transactionId", o.transactionId)
                    put("paymentReference", o.paymentReference)
                    put("expectedDeliverySlot", o.expectedDeliverySlot)
                    put("isNearClosingTime", o.isNearClosingTime)
                    put("lateNightDecision", o.lateNightDecision)
                    put("confirmedDeliveryTime", o.confirmedDeliveryTime)
                    put("scheduledForTomorrow", o.scheduledForTomorrow)
                    put("status", o.status)
                    put("statusNote", o.statusNote)

                    val itemsArr = JSONArray()
                    o.items.forEach { item ->
                        itemsArr.put(JSONObject().apply {
                            put("productId", item.productId)
                            put("productName", item.productName)
                            put("price", item.price)
                            put("quantity", item.quantity)
                            put("unit", item.unit)
                            put("isPrintingOrder", item.isPrintingOrder)
                            put("printingDetails", item.printingDetails)
                            put("printInstructions", item.printInstructions)
                            put("uploadedFileCount", item.uploadedFileCount)
                            put("fileNames", JSONArray(item.fileNames))
                            put("fileUrls", JSONArray(item.fileUrls))
                        })
                    }
                    put("items", itemsArr)

                    if (o.billingSnapshot != null) {
                        val b = o.billingSnapshot
                        put("billingSnapshot", JSONObject().apply {
                            put("appliedPolicy", b.appliedPolicy)
                            put("policyDisplayName", b.policyDisplayName)
                            put("thresholdAmount", b.thresholdAmount)
                            put("isFreeDeliveryEligible", b.isFreeDeliveryEligible)
                            put("freeDeliveryEligibilityReason", b.freeDeliveryEligibilityReason)
                            put("merchandiseSubtotal", b.merchandiseSubtotal)
                            put("productDiscount", b.productDiscount)
                            put("platformFee", b.platformFee)
                            put("totalDeliveryBeforeDiscount", b.totalDeliveryBeforeDiscount)
                            put("totalDeliveryDiscount", b.totalDeliveryDiscount)
                            put("netDeliveryFee", b.netDeliveryFee)
                            put("finalPayableAmount", b.finalPayableAmount)
                            put("calculationTimestamp", b.calculationTimestamp)
                        })
                    }
                })
            }
            writeJsonFile(FILE_ORDERS, array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save orders: ${e.message}", e)
        }
    }

    fun loadOrders(): List<Order>? {
        val raw = readJsonFile(FILE_ORDERS) ?: return null
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<Order>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val itemsList = mutableListOf<OrderItemRecord>()
                val itemsArr = o.optJSONArray("items")
                if (itemsArr != null) {
                    for (j in 0 until itemsArr.length()) {
                        val itemJson = itemsArr.getJSONObject(j)
                        val fnList = mutableListOf<String>()
                        val fnArr = itemJson.optJSONArray("fileNames")
                        if (fnArr != null) {
                            for (k in 0 until fnArr.length()) fnList.add(fnArr.getString(k))
                        }
                        val fuList = mutableListOf<String>()
                        val fuArr = itemJson.optJSONArray("fileUrls")
                        if (fuArr != null) {
                            for (k in 0 until fuArr.length()) fuList.add(fuArr.getString(k))
                        }

                        itemsList.add(
                            OrderItemRecord(
                                productId = itemJson.optString("productId"),
                                productName = itemJson.optString("productName"),
                                price = itemJson.optDouble("price", 0.0),
                                quantity = itemJson.optInt("quantity", 1),
                                unit = itemJson.optString("unit"),
                                isPrintingOrder = itemJson.optBoolean("isPrintingOrder", false),
                                printingDetails = itemJson.optString("printingDetails"),
                                fileNames = fnList,
                                fileUrls = fuList,
                                printInstructions = itemJson.optString("printInstructions"),
                                uploadedFileCount = itemJson.optInt("uploadedFileCount", 0)
                            )
                        )
                    }
                }

                val snapJson = o.optJSONObject("billingSnapshot")
                val snapshot = if (snapJson != null) {
                    OrderBillingSnapshot(
                        appliedPolicy = snapJson.optString("appliedPolicy", "MINIMUM_ORDER"),
                        policyDisplayName = snapJson.optString("policyDisplayName"),
                        thresholdAmount = snapJson.optDouble("thresholdAmount", 199.0),
                        isFreeDeliveryEligible = snapJson.optBoolean("isFreeDeliveryEligible", false),
                        freeDeliveryEligibilityReason = snapJson.optString("freeDeliveryEligibilityReason"),
                        merchandiseSubtotal = snapJson.optDouble("merchandiseSubtotal"),
                        productDiscount = snapJson.optDouble("productDiscount"),
                        platformFee = snapJson.optDouble("platformFee"),
                        totalDeliveryBeforeDiscount = snapJson.optDouble("totalDeliveryBeforeDiscount"),
                        totalDeliveryDiscount = snapJson.optDouble("totalDeliveryDiscount"),
                        netDeliveryFee = snapJson.optDouble("netDeliveryFee"),
                        finalPayableAmount = snapJson.optDouble("finalPayableAmount"),
                        calculationTimestamp = snapJson.optString("calculationTimestamp")
                    )
                } else null

                list.add(
                    Order(
                        id = o.optString("id"),
                        orderNumber = o.optString("orderNumber"),
                        userId = o.optString("userId"),
                        customerName = o.optString("customerName"),
                        customerPhone = o.optString("customerPhone"),
                        deliveryAddress = o.optString("deliveryAddress"),
                        locality = o.optString("locality"),
                        pinCode = o.optString("pinCode", "210427"),
                        shopId = o.optString("shopId"),
                        shopName = o.optString("shopName"),
                        items = itemsList,
                        itemsCount = o.optInt("itemsCount", itemsList.sumOf { it.quantity }),
                        subtotal = o.optDouble("subtotal", 0.0),
                        deliveryFee = o.optDouble("deliveryFee", 0.0),
                        platformFee = o.optDouble("platformFee", 0.0),
                        discount = o.optDouble("discount", 0.0),
                        totalAmount = o.optDouble("totalAmount", 0.0),
                        deliveryMethod = o.optString("deliveryMethod", "shop_delivery"),
                        paymentMethod = o.optString("paymentMethod", "cash_on_delivery"),
                        paymentStatus = o.optString("paymentStatus", "pending"),
                        transactionId = o.optString("transactionId"),
                        paymentReference = o.optString("paymentReference"),
                        expectedDeliverySlot = o.optString("expectedDeliverySlot", com.example.data.model.ShopScheduleUtils.calculateExact30MinForward()),
                        isNearClosingTime = o.optBoolean("isNearClosingTime", false),
                        lateNightDecision = o.optString("lateNightDecision", ""),
                        confirmedDeliveryTime = o.optString("confirmedDeliveryTime", ""),
                        scheduledForTomorrow = o.optBoolean("scheduledForTomorrow", false),
                        status = o.optString("status", "pending"),
                        statusNote = o.optString("statusNote"),
                        billingSnapshot = snapshot
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse orders: ${e.message}", e)
            null
        }
    }

    // ==========================================
    // 5. Cart Items
    // ==========================================
    fun saveCart(cartItems: List<CartItem>) {
        try {
            val array = JSONArray()
            cartItems.forEach { c ->
                array.put(JSONObject().apply {
                    put("quantity", c.quantity)
                    put("product", JSONObject().apply {
                        put("id", c.product.id)
                        put("shopId", c.product.shopId)
                        put("shopName", c.product.shopName)
                        put("name", c.product.name)
                        put("category", c.product.category)
                        put("subcategory", c.product.subcategory)
                        put("price", c.product.price)
                        put("originalPrice", c.product.originalPrice)
                        put("description", c.product.description)
                        put("imageUrl", c.product.imageUrl)
                        put("unit", c.product.unit)
                        put("inStock", c.product.inStock)
                    })
                })
            }
            writeJsonFile(FILE_CART, array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cart: ${e.message}", e)
        }
    }

    fun loadCart(): List<CartItem>? {
        val raw = readJsonFile(FILE_CART) ?: return null
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<CartItem>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val pJson = o.getJSONObject("product")
                val prod = Product(
                    id = pJson.optString("id"),
                    shopId = pJson.optString("shopId"),
                    shopName = pJson.optString("shopName"),
                    name = pJson.optString("name"),
                    category = pJson.optString("category", "Grocery"),
                    subcategory = pJson.optString("subcategory", "All"),
                    price = pJson.optDouble("price", 0.0),
                    originalPrice = pJson.optDouble("originalPrice", 0.0),
                    description = pJson.optString("description"),
                    imageUrl = pJson.optString("imageUrl"),
                    unit = pJson.optString("unit", "1 pc"),
                    inStock = pJson.optBoolean("inStock", true)
                )
                list.add(CartItem(product = prod, quantity = o.optInt("quantity", 1)))
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse cart: ${e.message}", e)
            null
        }
    }

    // ==========================================
    // 6. Delivery Configuration
    // ==========================================
    fun saveDeliveryConfig(config: DeliveryConfig) {
        try {
            val json = JSONObject().apply {
                put("id", config.id)
                put("maxDeliveryDistanceKm", config.maxDeliveryDistanceKm)
                put("activePolicy", config.activePolicy)
                put("minOrderThreshold", config.minOrderThreshold)
                put("isManualFreeDeliveryOn", config.isManualFreeDeliveryOn)
                put("defaultDeliveryFee", config.defaultDeliveryFee)
                put("freeDeliveryThreshold", config.freeDeliveryThreshold)
                put("minOrderAmount", config.minOrderAmount)
                put("version", config.version)
                put("lastUpdatedByAdminId", config.lastUpdatedByAdminId)
                put("lastUpdatedByAdminName", config.lastUpdatedByAdminName)
                put("lastUpdatedAtFormatted", config.lastUpdatedAtFormatted)
                put("supportedAreas", JSONArray(config.supportedAreas))

                val tierArr = JSONArray()
                config.distanceTiers.forEach { t ->
                    tierArr.put(JSONObject().apply {
                        put("minDistanceKm", t.minDistanceKm)
                        put("maxDistanceKm", t.maxDistanceKm)
                        put("charge", t.charge)
                        put("label", t.label)
                    })
                }
                put("distanceTiers", tierArr)
            }
            writeJsonFile(FILE_DELIVERY_CONFIG, json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save delivery config: ${e.message}", e)
        }
    }

    fun loadDeliveryConfig(): DeliveryConfig? {
        val raw = readJsonFile(FILE_DELIVERY_CONFIG) ?: return null
        return try {
            val json = JSONObject(raw)
            val areasList = mutableListOf<String>()
            val areasArr = json.optJSONArray("supportedAreas")
            if (areasArr != null) {
                for (i in 0 until areasArr.length()) areasList.add(areasArr.getString(i))
            }

            val tierList = mutableListOf<DistanceTier>()
            val tierArr = json.optJSONArray("distanceTiers")
            if (tierArr != null) {
                for (i in 0 until tierArr.length()) {
                    val t = tierArr.getJSONObject(i)
                    tierList.add(
                        DistanceTier(
                            minDistanceKm = t.optDouble("minDistanceKm", 0.0),
                            maxDistanceKm = t.optDouble("maxDistanceKm", 2.0),
                            charge = t.optDouble("charge", 9.0),
                            label = t.optString("label", "0 – 2 km")
                        )
                    )
                }
            }

            DeliveryConfig(
                id = json.optString("id", "default"),
                distanceTiers = if (tierList.isNotEmpty()) tierList else DeliveryConfig().distanceTiers,
                maxDeliveryDistanceKm = json.optDouble("maxDeliveryDistanceKm", 15.0),
                activePolicy = json.optString("activePolicy", "MINIMUM_ORDER"),
                minOrderThreshold = json.optDouble("minOrderThreshold", 199.0),
                isManualFreeDeliveryOn = json.optBoolean("isManualFreeDeliveryOn", false),
                defaultDeliveryFee = json.optDouble("defaultDeliveryFee", 20.0),
                freeDeliveryThreshold = json.optDouble("freeDeliveryThreshold", 199.0),
                minOrderAmount = json.optDouble("minOrderAmount", 50.0),
                version = json.optLong("version", 1L),
                lastUpdatedByAdminId = json.optString("lastUpdatedByAdminId", "PR12"),
                lastUpdatedByAdminName = json.optString("lastUpdatedByAdminName", "Pranjal Rajpoot"),
                lastUpdatedAtFormatted = json.optString("lastUpdatedAtFormatted", "System Initial Setup"),
                supportedAreas = if (areasList.isNotEmpty()) areasList else DeliveryConfig().supportedAreas
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse delivery config: ${e.message}", e)
            null
        }
    }

    // ==========================================
    // 7. Shop Registration Config (Admin 3-Step Setting)
    // ==========================================
    fun saveShopRegistrationConfig(config: ShopRegistrationConfig) {
        try {
            val json = JSONObject().apply {
                put("id", config.id)
                put("threeStepEnabled", config.threeStepEnabled)
                put("lastUpdatedByAdminId", config.lastUpdatedByAdminId)
                put("lastUpdatedByAdminName", config.lastUpdatedByAdminName)
                put("lastUpdatedAtFormatted", config.lastUpdatedAtFormatted)
                put("version", config.version)
            }
            writeJsonFile(FILE_REGISTRATION_CONFIG, json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save registration config: ${e.message}", e)
        }
    }

    fun loadShopRegistrationConfig(): ShopRegistrationConfig? {
        val raw = readJsonFile(FILE_REGISTRATION_CONFIG) ?: return null
        return try {
            val json = JSONObject(raw)
            ShopRegistrationConfig(
                id = json.optString("id", "shopRegistration"),
                threeStepEnabled = json.optBoolean("threeStepEnabled", false),
                lastUpdatedByAdminId = json.optString("lastUpdatedByAdminId", "PR12"),
                lastUpdatedByAdminName = json.optString("lastUpdatedByAdminName", "Main Admin"),
                lastUpdatedAtFormatted = json.optString("lastUpdatedAtFormatted", "29 Oct 2026, 10:00 AM"),
                version = json.optLong("version", 1L)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse registration config: ${e.message}", e)
            null
        }
    }

    // ==========================================
    // 8. Shop Verification Requests
    // ==========================================
    fun saveShopVerificationRequests(requests: List<ShopVerificationRequest>) {
        try {
            val array = JSONArray()
            requests.forEach { r ->
                array.put(JSONObject().apply {
                    put("id", r.id)
                    put("shopId", r.shopId)
                    put("shopName", r.shopName)
                    put("ownerName", r.ownerName)
                    put("phone", r.phone)
                    put("category", r.category)
                    put("address", r.address)
                    put("description", r.description)
                    put("gstNumber", r.gstNumber)
                    put("passwordHash", r.passwordHash)
                    put("aadhaarNumberMasked", r.aadhaarNumberMasked)
                    put("aadhaarDocUri", r.aadhaarDocUri)
                    put("panNumber", r.panNumber)
                    put("panDocUri", r.panDocUri)
                    put("gstCertificateDocUri", r.gstCertificateDocUri)
                    put("gstCertificateGstin", r.gstCertificateGstin)
                    put("status", r.status)
                    put("rejectionReason", r.rejectionReason)
                    put("documentsRequiredNotes", r.documentsRequiredNotes)
                    put("reviewedByAdminId", r.reviewedByAdminId)
                    put("reviewedByAdminName", r.reviewedByAdminName)
                    put("reviewedAtFormatted", r.reviewedAtFormatted)
                    put("submittedAtFormatted", r.submittedAtFormatted)
                    put("stepCompleted", r.stepCompleted)
                    put("isThreeStepRegistration", r.isThreeStepRegistration)
                })
            }
            writeJsonFile(FILE_VERIFICATION_REQUESTS, array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save verification requests: ${e.message}", e)
        }
    }

    fun loadShopVerificationRequests(): List<ShopVerificationRequest>? {
        val raw = readJsonFile(FILE_VERIFICATION_REQUESTS) ?: return null
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<ShopVerificationRequest>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    ShopVerificationRequest(
                        id = o.optString("id"),
                        shopId = o.optString("shopId"),
                        shopName = o.optString("shopName"),
                        ownerName = o.optString("ownerName"),
                        phone = o.optString("phone"),
                        category = o.optString("category", "Grocery"),
                        address = o.optString("address"),
                        description = o.optString("description"),
                        gstNumber = o.optString("gstNumber"),
                        passwordHash = o.optString("passwordHash"),
                        aadhaarNumberMasked = o.optString("aadhaarNumberMasked"),
                        aadhaarDocUri = o.optString("aadhaarDocUri"),
                        panNumber = o.optString("panNumber"),
                        panDocUri = o.optString("panDocUri"),
                        gstCertificateDocUri = o.optString("gstCertificateDocUri"),
                        gstCertificateGstin = o.optString("gstCertificateGstin"),
                        status = o.optString("status", ShopVerificationRequest.STATUS_PENDING_REVIEW),
                        rejectionReason = o.optString("rejectionReason"),
                        documentsRequiredNotes = o.optString("documentsRequiredNotes"),
                        reviewedByAdminId = o.optString("reviewedByAdminId"),
                        reviewedByAdminName = o.optString("reviewedByAdminName"),
                        reviewedAtFormatted = o.optString("reviewedAtFormatted"),
                        submittedAtFormatted = o.optString("submittedAtFormatted"),
                        stepCompleted = o.optInt("stepCompleted", 1),
                        isThreeStepRegistration = o.optBoolean("isThreeStepRegistration", true)
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse verification requests: ${e.message}", e)
            null
        }
    }

    // ==========================================
    // 9. Shopkeepers
    // ==========================================
    fun saveShopkeepers(shopkeepers: List<ShopkeeperAccount>) {
        try {
            val array = JSONArray()
            shopkeepers.forEach { sk ->
                array.put(JSONObject().apply {
                    put("id", sk.id)
                    put("loginId", sk.loginId)
                    put("shopkeeperName", sk.shopkeeperName)
                    put("shopId", sk.shopId)
                    put("shopName", sk.shopName)
                    put("phone", sk.phone)
                    put("email", sk.email)
                    put("gstNumber", sk.gstNumber)
                    put("password", sk.password)
                    put("passwordHash", sk.passwordHash)
                    put("isActive", sk.isActive)
                    put("status", sk.status)
                    put("rejectionReason", sk.rejectionReason)
                    put("suspensionReason", sk.suspensionReason)
                    put("assignedBy", sk.assignedBy)
                    put("createdAt", sk.createdAt)
                })
            }
            writeJsonFile(FILE_SHOPKEEPERS, array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save shopkeepers: ${e.message}", e)
        }
    }

    fun loadShopkeepers(): List<ShopkeeperAccount>? {
        val raw = readJsonFile(FILE_SHOPKEEPERS) ?: return null
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<ShopkeeperAccount>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    ShopkeeperAccount(
                        id = o.optString("id"),
                        loginId = o.optString("loginId"),
                        shopkeeperName = o.optString("shopkeeperName"),
                        shopId = o.optString("shopId"),
                        shopName = o.optString("shopName"),
                        phone = o.optString("phone"),
                        email = o.optString("email"),
                        gstNumber = o.optString("gstNumber"),
                        password = o.optString("password", "password123"),
                        passwordHash = o.optString("passwordHash"),
                        isActive = o.optBoolean("isActive", true),
                        status = o.optString("status", "APPROVED"),
                        rejectionReason = o.optString("rejectionReason"),
                        suspensionReason = o.optString("suspensionReason"),
                        assignedBy = o.optString("assignedBy", "PR12"),
                        createdAt = o.optString("createdAt", "2026-10-19")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse shopkeepers: ${e.message}", e)
            null
        }
    }

    // ==========================================
    // 10. Registered User Accounts
    // ==========================================
    fun saveRegisteredUsers(users: List<RegisteredAccount>) {
        try {
            val array = JSONArray()
            users.forEach { u ->
                array.put(JSONObject().apply {
                    put("id", u.id)
                    put("fullName", u.fullName)
                    put("normalizedName", u.normalizedName)
                    put("passwordHash", u.passwordHash)
                    put("role", u.role)
                    put("phone", u.phone)
                    put("email", u.email)
                    put("createdAt", u.createdAt)
                })
            }
            writeJsonFile(FILE_REGISTERED_USERS, array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save registered users: ${e.message}", e)
        }
    }

    fun loadRegisteredUsers(): List<RegisteredAccount>? {
        val raw = readJsonFile(FILE_REGISTERED_USERS) ?: return null
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<RegisteredAccount>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    RegisteredAccount(
                        id = o.optString("id"),
                        fullName = o.optString("fullName"),
                        normalizedName = o.optString("normalizedName"),
                        passwordHash = o.optString("passwordHash"),
                        role = o.optString("role", "customer"),
                        phone = o.optString("phone", "+91 98765 12345"),
                        email = o.optString("email", "customer@flashdrop.local"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse registered users: ${e.message}", e)
            null
        }
    }

    // ==========================================
    // 11. Transactions
    // ==========================================
    fun saveTransactions(txns: List<Transaction>) {
        try {
            val array = JSONArray()
            txns.forEach { t ->
                array.put(JSONObject().apply {
                    put("id", t.id)
                    put("orderId", t.orderId)
                    put("orderNumber", t.orderNumber)
                    put("shopId", t.shopId)
                    put("shopName", t.shopName)
                    put("customerName", t.customerName)
                    put("customerPhone", t.customerPhone)
                    put("amount", t.amount)
                    put("paymentMethod", t.paymentMethod)
                    put("status", t.status)
                    put("timestamp", t.timestamp)
                    put("timeOnly", t.timeOnly)
                    put("providerTxnId", t.providerTxnId)
                    put("referenceId", t.referenceId)
                    put("verificationProof", t.verificationProof)
                    put("failureReason", t.failureReason)
                    put("daysAgo", t.daysAgo)
                    put("itemsSummary", t.itemsSummary)
                    put("deliveryFee", t.deliveryFee)
                    put("platformCommission", t.platformCommission)
                    put("discountSubsidy", t.discountSubsidy)
                    put("shopkeeperPayoutAmount", t.shopkeeperPayoutAmount)
                    put("shopkeeperSettlementStatus", t.shopkeeperSettlementStatus)
                    put("onlineProblemCount", t.onlineProblemCount)
                    put("onlineProblemType", t.onlineProblemType)
                })
            }
            writeJsonFile(FILE_TRANSACTIONS, array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save transactions: ${e.message}", e)
        }
    }

    fun loadTransactions(): List<Transaction>? {
        val raw = readJsonFile(FILE_TRANSACTIONS) ?: return null
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<Transaction>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    Transaction(
                        id = o.optString("id"),
                        orderId = o.optString("orderId"),
                        orderNumber = o.optString("orderNumber"),
                        shopId = o.optString("shopId"),
                        shopName = o.optString("shopName"),
                        customerName = o.optString("customerName"),
                        customerPhone = o.optString("customerPhone"),
                        amount = o.optDouble("amount", 0.0),
                        paymentMethod = o.optString("paymentMethod", "UPI"),
                        status = o.optString("status", "Paid"),
                        timestamp = o.optString("timestamp"),
                        timeOnly = o.optString("timeOnly"),
                        providerTxnId = o.optString("providerTxnId"),
                        referenceId = o.optString("referenceId"),
                        verificationProof = o.optString("verificationProof", "Verified by FlashDrop Gateway"),
                        failureReason = o.optString("failureReason"),
                        daysAgo = o.optInt("daysAgo", 0),
                        itemsSummary = o.optString("itemsSummary"),
                        deliveryFee = o.optDouble("deliveryFee", 19.0),
                        platformCommission = o.optDouble("platformCommission", 0.0),
                        discountSubsidy = o.optDouble("discountSubsidy", 0.0),
                        shopkeeperPayoutAmount = o.optDouble("shopkeeperPayoutAmount", 0.0),
                        shopkeeperSettlementStatus = o.optString("shopkeeperSettlementStatus", "Settled"),
                        onlineProblemCount = o.optInt("onlineProblemCount", 0),
                        onlineProblemType = o.optString("onlineProblemType")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse transactions: ${e.message}", e)
            null
        }
    }

    // ==========================================
    // 13. Shop Weekly Schedules (IST)
    // ==========================================
    fun saveShopSchedules(schedules: Map<String, WeeklySchedule>) {
        try {
            val root = JSONObject()
            schedules.forEach { (shopId, sched) ->
                val sObj = JSONObject()
                sObj.put("shopId", sched.shopId)
                sObj.put("defaultOpeningTime", sched.defaultOpeningTime)
                sObj.put("defaultClosingTime", sched.defaultClosingTime)
                sObj.put("autoScheduleEnabled", sched.autoScheduleEnabled)
                sObj.put("autoOpeningEnabled", sched.autoOpeningEnabled)
                sObj.put("autoClosingEnabled", sched.autoClosingEnabled)
                sObj.put("isTemporarilyClosed", sched.isTemporarilyClosed)
                if (sched.manualOverride != null) {
                    sObj.put("manualOverride", sched.manualOverride)
                }
                sObj.put("lastManualOverrideTimestamp", sched.lastManualOverrideTimestamp)
                sObj.put("lastScheduleUpdate", sched.lastScheduleUpdate)
                sObj.put("updatedBy", sched.updatedBy)
                sObj.put("lateNightConfirmationEnabled", sched.lateNightConfirmationEnabled)
                val daysArr = JSONArray()
                sched.days.forEach { d ->
                    daysArr.put(JSONObject().apply {
                        put("dayOfWeek", d.dayOfWeek)
                        put("openingTime", d.openingTime)
                        put("closingTime", d.closingTime)
                        put("isOpen", d.isOpen)
                    })
                }
                sObj.put("days", daysArr)
                root.put(shopId, sObj)
            }
            writeJsonFile(FILE_SHOP_SCHEDULES, root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save shop schedules: ${e.message}", e)
        }
    }

    fun loadShopSchedules(): Map<String, WeeklySchedule>? {
        val raw = readJsonFile(FILE_SHOP_SCHEDULES) ?: return null
        return try {
            val root = JSONObject(raw)
            val map = mutableMapOf<String, WeeklySchedule>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val shopId = keys.next()
                val sObj = root.getJSONObject(shopId)
                val defaultOpen = sObj.optString("defaultOpeningTime", "10:00 AM")
                val defaultClose = sObj.optString("defaultClosingTime", "10:00 PM")
                val daysArr = sObj.optJSONArray("days")
                val daysList = mutableListOf<DaySchedule>()
                if (daysArr != null) {
                    for (i in 0 until daysArr.length()) {
                        val dObj = daysArr.getJSONObject(i)
                        daysList.add(
                            DaySchedule(
                                dayOfWeek = dObj.optString("dayOfWeek", "Monday"),
                                openingTime = dObj.optString("openingTime", defaultOpen),
                                closingTime = dObj.optString("closingTime", defaultClose),
                                isOpen = dObj.optBoolean("isOpen", true)
                            )
                        )
                    }
                }
                val manualOverride = if (sObj.has("manualOverride")) sObj.getBoolean("manualOverride") else null
                map[shopId] = WeeklySchedule(
                    shopId = shopId,
                    defaultOpeningTime = defaultOpen,
                    defaultClosingTime = defaultClose,
                    days = if (daysList.isNotEmpty()) daysList else WeeklySchedule.defaultDays(defaultOpen, defaultClose),
                    autoScheduleEnabled = sObj.optBoolean("autoScheduleEnabled", true),
                    autoOpeningEnabled = sObj.optBoolean("autoOpeningEnabled", true),
                    autoClosingEnabled = sObj.optBoolean("autoClosingEnabled", true),
                    manualOverride = manualOverride,
                    isTemporarilyClosed = sObj.optBoolean("isTemporarilyClosed", false),
                    lastManualOverrideTimestamp = sObj.optLong("lastManualOverrideTimestamp", 0L),
                    lastScheduleUpdate = sObj.optLong("lastScheduleUpdate", System.currentTimeMillis()),
                    updatedBy = sObj.optString("updatedBy", "shopkeeper"),
                    lateNightConfirmationEnabled = sObj.optBoolean("lateNightConfirmationEnabled", true)
                )
            }
            map
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load shop schedules: ${e.message}", e)
            null
        }
    }

    // ==========================================
    // 14. Customer Records (Admin Directory & Data)
    // ==========================================
    fun saveCustomers(customers: List<CustomerRecord>) {
        try {
            val array = JSONArray()
            customers.forEach { c ->
                array.put(JSONObject().apply {
                    put("id", c.id)
                    put("fullName", c.fullName)
                    put("email", c.email)
                    put("phone", c.phone)
                    put("photoUrl", c.photoUrl)
                    put("primaryAddress", c.primaryAddress)
                    put("locality", c.locality)
                    put("city", c.city)
                    put("state", c.state)
                    put("pinCode", c.pinCode)
                    put("authMethod", c.authMethod)
                    put("accountStatus", c.accountStatus)
                    put("isVerified", c.isVerified)
                    put("joinedDate", c.joinedDate)
                    put("lastActive", c.lastActive)
                    put("totalOrdersCount", c.totalOrdersCount)
                    put("completedOrdersCount", c.completedOrdersCount)
                    put("cancelledOrdersCount", c.cancelledOrdersCount)
                    put("totalSpentAmount", c.totalSpentAmount)
                    put("upiSpentAmount", c.upiSpentAmount)
                    put("codSpentAmount", c.codSpentAmount)
                    put("preferredPaymentMethod", c.preferredPaymentMethod)
                    put("favoriteShopName", c.favoriteShopName)
                    put("adminNotes", c.adminNotes)

                    val addrArr = JSONArray()
                    c.savedAddresses.forEach { a ->
                        addrArr.put(JSONObject().apply {
                            put("id", a.id)
                            put("name", a.name)
                            put("phone", a.phone)
                            put("countryCode", a.countryCode)
                            put("label", a.label)
                            put("houseNumber", a.houseNumber)
                            put("buildingBlock", a.buildingBlock)
                            put("street", a.street)
                            put("area", a.area)
                            put("city", a.city)
                            put("state", a.state)
                            put("pinCode", a.pinCode)
                            put("landmark", a.landmark)
                            put("isDefault", a.isDefault)
                        })
                    }
                    put("savedAddresses", addrArr)

                    val ordArr = JSONArray()
                    c.recentOrders.forEach { o ->
                        ordArr.put(JSONObject().apply {
                            put("orderId", o.orderId)
                            put("orderNumber", o.orderNumber)
                            put("shopId", o.shopId)
                            put("shopName", o.shopName)
                            put("itemsSummary", o.itemsSummary)
                            put("itemsCount", o.itemsCount)
                            put("totalAmount", o.totalAmount)
                            put("paymentMethod", o.paymentMethod)
                            put("paymentStatus", o.paymentStatus)
                            put("orderStatus", o.orderStatus)
                            put("orderDate", o.orderDate)
                            put("deliveryAddress", o.deliveryAddress)
                        })
                    }
                    put("recentOrders", ordArr)
                })
            }
            writeJsonFile(FILE_CUSTOMERS, array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save customers: ${e.message}", e)
        }
    }

    fun loadCustomers(): List<CustomerRecord>? {
        val raw = readJsonFile(FILE_CUSTOMERS) ?: return null
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<CustomerRecord>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)

                val addrList = mutableListOf<DeliveryAddress>()
                val addrArr = o.optJSONArray("savedAddresses")
                if (addrArr != null) {
                    for (j in 0 until addrArr.length()) {
                        val a = addrArr.getJSONObject(j)
                        addrList.add(
                            DeliveryAddress(
                                id = a.optString("id"),
                                name = a.optString("name"),
                                phone = a.optString("phone"),
                                countryCode = a.optString("countryCode", "+91"),
                                label = a.optString("label", "Home"),
                                houseNumber = a.optString("houseNumber"),
                                buildingBlock = a.optString("buildingBlock"),
                                street = a.optString("street"),
                                area = a.optString("area", "Subhash Chowk"),
                                city = a.optString("city", "Mahoba"),
                                state = a.optString("state", "Uttar Pradesh"),
                                pinCode = a.optString("pinCode", "210427"),
                                landmark = a.optString("landmark"),
                                isDefault = a.optBoolean("isDefault", false)
                            )
                        )
                    }
                }

                val ordList = mutableListOf<CustomerOrderSummary>()
                val ordArr = o.optJSONArray("recentOrders")
                if (ordArr != null) {
                    for (j in 0 until ordArr.length()) {
                        val ord = ordArr.getJSONObject(j)
                        ordList.add(
                            CustomerOrderSummary(
                                orderId = ord.optString("orderId"),
                                orderNumber = ord.optString("orderNumber"),
                                shopId = ord.optString("shopId"),
                                shopName = ord.optString("shopName"),
                                itemsSummary = ord.optString("itemsSummary"),
                                itemsCount = ord.optInt("itemsCount", 1),
                                totalAmount = ord.optDouble("totalAmount", 0.0),
                                paymentMethod = ord.optString("paymentMethod", "UPI"),
                                paymentStatus = ord.optString("paymentStatus", "Paid"),
                                orderStatus = ord.optString("orderStatus", "delivered"),
                                orderDate = ord.optString("orderDate"),
                                deliveryAddress = ord.optString("deliveryAddress")
                            )
                        )
                    }
                }

                list.add(
                    CustomerRecord(
                        id = o.optString("id"),
                        fullName = o.optString("fullName"),
                        email = o.optString("email"),
                        phone = o.optString("phone"),
                        photoUrl = o.optString("photoUrl"),
                        primaryAddress = o.optString("primaryAddress", "Subhash Chowk, Chhatarpur Road"),
                        locality = o.optString("locality", "Alha Chowk"),
                        city = o.optString("city", "Mahoba"),
                        state = o.optString("state", "Uttar Pradesh"),
                        pinCode = o.optString("pinCode", "210427"),
                        savedAddresses = addrList,
                        authMethod = o.optString("authMethod", "Registered Account"),
                        accountStatus = o.optString("accountStatus", "Active"),
                        isVerified = o.optBoolean("isVerified", true),
                        joinedDate = o.optString("joinedDate", "Oct 2026"),
                        lastActive = o.optString("lastActive", "Today"),
                        totalOrdersCount = o.optInt("totalOrdersCount", 0),
                        completedOrdersCount = o.optInt("completedOrdersCount", 0),
                        cancelledOrdersCount = o.optInt("cancelledOrdersCount", 0),
                        totalSpentAmount = o.optDouble("totalSpentAmount", 0.0),
                        upiSpentAmount = o.optDouble("upiSpentAmount", 0.0),
                        codSpentAmount = o.optDouble("codSpentAmount", 0.0),
                        preferredPaymentMethod = o.optString("preferredPaymentMethod", "UPI"),
                        favoriteShopName = o.optString("favoriteShopName", "Sharma General Store"),
                        recentOrders = ordList,
                        adminNotes = o.optString("adminNotes", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse customers: ${e.message}", e)
            null
        }
    }
}
