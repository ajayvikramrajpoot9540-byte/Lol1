# FlashDrop: Admin-Controlled Multi-Shop Delivery Charge & Free Delivery System

Comprehensive implementation plan for the delivery charge management system in the FlashDrop Android application, controlled exclusively and equally by Main Admins **Pranjal Rajpoot** and **Chanchal Didi**.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> **Equal Main Admin Authorization**: Access to and modification of the Delivery Charge Settings section is strictly restricted to verified Main Admins (Pranjal Rajpoot and Chanchal Didi). Backend verification in `ApnaBazRepository` and `firestore.rules` ensures neither Customers nor Shopkeepers can alter delivery rules or fee structures.

- **Decision 1 (Default Distance Tiers & Max Limit)**:
  - 0–2 km: **₹10**
  - >2 km to 5 km: **₹20**
  - >5 km to 10 km: **₹40**
  - >10 km: **₹70**
  - Maximum permitted delivery distance: **15 km** (configurable by Main Admins; orders with any shop exceeding this distance are blocked at checkout with an informative explanation).
- **Decision 2 (Three Configurable Free-Delivery Policies)**:
  1. `MIN_ORDER_THRESHOLD`: Free delivery when order product subtotal reaches an editable threshold (**₹199** by default).
  2. `NO_FREE_DELIVERY`: Distance-based delivery rates apply on all orders regardless of cart value.
  3. `MANUAL_TOGGLE`: Main Admins can toggle free delivery ON or OFF instantly via a master switch.
- **Decision 3 (Multi-Shop Routing & Charges)**:
  - When cart items originate from multiple shops, route distances are determined per shop (based on shop coordinates to customer address coordinates).
  - Delivery charges are computed separately for each shop according to the active distance tiers.
  - If the active free-delivery policy applies, the full combined delivery amount is discounted, showing a transparent line-item discount to prevent confusion or duplicate charges.
- **Decision 4 (Server-Validated Immutable Order Snapshot)**:
  - All totals, shop-level distances, delivery charges, discounts, and payable amounts are validated and calculated by trusted repository logic on checkout—never trusting raw client amounts.
  - A complete `DeliverySnapshot` is embedded in the Firestore `orders` document, guaranteeing that future changes to delivery settings never alter historical order records.

---

### 1. Architecture & Component Workflow

```
┌─────────────────────────────────────────────────────────────┐
│                   Admin Dashboard (Tab 3)                   │
│   • Authenticated as Pranjal Rajpoot or Chanchal Didi       │
│   • Distance Tiers: 0-2km (₹10), 2-5km (₹20), 5-10km (₹40), │
│     >10km (₹70), Max Limit (15km)                           │
│   • Policy Switcher: Min Threshold (₹199), None, Manual Sw  │
└──────────────────────────────┬──────────────────────────────┘
                               │ Writes (Admin verified)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│           Firestore: /app_settings/delivery_config          │
│   • Security Rules: Read by authenticated users             │
│   • Write: Verified Main Admins only                        │
└──────────────────────────────┬──────────────────────────────┘
                               │ Real-time flow
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    ApnaBazRepository                        │
│   • calculateMultiShopDelivery(cartItems, customerLocation) │
│   • Calculates per-shop distance (km) & fee (₹)             │
│   • Checks max delivery distance eligibility                │
│   • Evaluates active Free Delivery Policy                   │
│   • Produces immutable DeliverySnapshot                     │
└──────────────┬──────────────────────────────┬───────────────┘
               │                              │
               ▼                              ▼
┌──────────────────────────────┐┌──────────────────────────────┐
│        CheckoutScreen        ││       Order Creation         │
│ • Itemized Per-Shop Charges  ││ • Immutable DeliverySnapshot │
│ • Free Delivery Discount Tag ││ • Stored in Firestore        │
│ • Out-of-Range Warning Alert ││ • Unchangeable by future     │
│ • Block checkout if > max km ││   setting updates            │
└──────────────────────────────┘└──────────────────────────────┘
```

---

### 2. Implementation Steps

#### Step 1: Update Data Models (`DeliveryConfig.kt` & `Order.kt`)
- Refactor `DeliveryConfig`:
  - `tiers`: List of `DeliveryTier(id, minDistanceKm, maxDistanceKm, chargeRupees, label)` with default tiers (0–2 km: ₹10, 2–5 km: ₹20, 5–10 km: ₹40, 10–999 km: ₹70).
  - `maxDeliveryDistanceKm`: Double = 15.0.
  - `activePolicy`: `FreeDeliveryPolicy` enum (`MIN_ORDER_THRESHOLD`, `NO_FREE_DELIVERY`, `MANUAL_TOGGLE`).
  - `minOrderThreshold`: Double = 199.0.
  - `manualFreeDeliveryEnabled`: Boolean = false.
  - `updatedAt`: Long, `updatedByAdmin`: String.
- Create `ShopDeliveryBreakdown`:
  - `shopId`: String, `shopName`: String, `distanceKm`: Double, `deliveryFee`: Double, `isExceedingMaxDistance`: Boolean.
- Create `DeliveryCalculationResult`:
  - `shopBreakdowns`: List<ShopDeliveryBreakdown>, `grossDeliveryTotal`: Double, `discountAmount`: Double, `netDeliveryTotal`: Double, `isFreeDeliveryApplied`: Boolean, `freeDeliveryReason`: String?, `isCheckoutAllowed`: Boolean, `unreachableShops`: List<String>.
- Add `DeliverySnapshot` to `Order` model to record exact historical calculations and policies permanently.

#### Step 2: Repository & Trusted Calculation Engine (`ApnaBazRepository.kt`)
- Secure backend checks:
  - `isMainAdmin()`: verify current profile is Pranjal Rajpoot (`PR11`) or Chanchal Didi (`CD12`) with verified admin credentials.
  - Reject `updateDeliveryConfig()` calls if not authorized.
- Delivery Engine:
  - `calculateDeliveryForCart(cartItems: List<CartItem>, address: DeliveryAddress?): DeliveryCalculationResult`:
    - Group cart items by `shopId`.
    - Retrieve each shop's geo-coordinates (fallback to shop district center or distance estimator if coordinate missing).
    - Compute Haversine distance between shop and customer delivery address.
    - Match distance against sorted `DeliveryTier`s.
    - Check if any shop distance exceeds `maxDeliveryDistanceKm`; flag `isExceedingMaxDistance = true` and set `isCheckoutAllowed = false`.
    - Apply active free-delivery policy (`MIN_ORDER_THRESHOLD`, `NO_FREE_DELIVERY`, `MANUAL_TOGGLE`).
    - Compute `grossDeliveryTotal`, `discountAmount`, and `netDeliveryTotal`.
- Trusted Order Placement:
  - When customer calls `placeOrder()`, repo independently recalculates the entire bill and delivery snapshot. Client amounts are discarded in favor of calculated amounts.

#### Step 3: Admin Dashboard Delivery Charge Settings UI (`AdminDashboardScreen.kt`)
- Verify Main Admin banner indicating equal privileges for Pranjal Rajpoot and Chanchal Didi.
- Policy Selector: Segmented buttons or interactive cards for the 3 policies:
  1. *Free Delivery on Minimum Order* (with editable threshold input, default ₹199).
  2. *No Free Delivery* (always charge configured distance rates).
  3. *Manual ON/OFF Toggle* (with quick toggle switch and status badge).
- Distance Tier Editor:
  - Editable rate fields for 0–2 km (₹10), 2–5 km (₹20), 5–10 km (₹40), >10 km (₹70).
  - Editable Maximum Permitted Delivery Distance (default 15 km) with helper description.
- Action Buttons:
  - "Reset to Defaults" button.
  - "Save Delivery Settings" button with loading state, validation, and success snackbar.

#### Step 4: Customer Checkout Bill UI (`CheckoutScreen.kt`)
- Enhanced Bill Breakdown:
  - Items subtotal.
  - Multi-Shop Delivery Breakdown section:
    - Lists each shop by name, calculated delivery distance (e.g. *2.4 km*), and individual delivery fee.
  - Free Delivery Status & Discount:
    - Green banner/tag: *"Free Delivery Applied (Orders over ₹199)"* or *"FlashDrop Free Delivery Promo"*, displaying `-₹XX` discount.
    - Progress indicator if cart is below threshold: *"Add ₹X more to get Free Delivery"*.
  - Out-of-Range Warning:
    - Red alert card if any shop exceeds max distance (e.g., *"Shop 'XYZ' is 16.2 km away. Maximum delivery limit is 15.0 km. Please remove items from this shop to proceed."*).
    - Disables "Proceed to Payment / Place Order" button when unreachable shops exist.
  - Final Payable Amount highlighted.

#### Step 5: Security Rules (`firestore.rules`)
- `match /app_settings/delivery_config`:
  - `allow read: if request.auth != null;`
  - `allow write: if isMainAdmin();` (checking admin UID/token/email matching Pranjal Rajpoot or Chanchal Didi).

#### Step 6: Testing & Verification
- Add unit tests in `ApnaBazRepositoryRuleTest.kt`:
  - Test distance tier pricing matching prompt specs.
  - Test all 3 free delivery policies (threshold, no free delivery, manual toggle).
  - Test multi-shop distance calculations and combined delivery fees.
  - Test blocking checkout when distance exceeds max limit.
  - Test Main Admin authorization restriction.
- Verify build with `compile_applet` and run tests with `gradle :app:testDebugUnitTest`.

---

### 3. Verification & Acceptance Criteria
- [ ] Pranjal Rajpoot and Chanchal Didi can view and update Delivery Charge Settings. Non-admins cannot access or update settings.
- [ ] Default tiers configured: 0-2km (₹10), 2-5km (₹20), 5-10km (₹40), >10km (₹70), max distance 15km.
- [ ] All three policies function accurately: Min Order (₹199), No Free Delivery, Manual Toggle.
- [ ] Multi-shop orders compute separate delivery fees per shop based on route distance.
- [ ] Checkout displays itemized shop delivery breakdown, distance, free delivery discount, and final amount.
- [ ] Out-of-range shops block checkout with a clear explanation.
- [ ] Orders store an immutable snapshot of delivery charges.
