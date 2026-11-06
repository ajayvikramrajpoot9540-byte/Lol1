# FlashDrop – Advanced Smart Delivery & Shop Schedule Management System

Comprehensive plan to implement the advanced delivery time calculation, shopkeeper-controlled opening/closing schedule, 7-day custom weekly timetable, late-night order flow, and customer availability gating across the FlashDrop Android application, strictly adhering to the 6-page specification PDF.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Choices from Phase 1 Clarification**:
> - **Late-Night Trigger Window**: Orders placed within **30 minutes before shop closing** (e.g., 9:30 PM for a 10:00 PM closing) will trigger the late-night delivery prompt (*"Can you deliver this order today?"*) in the Shopkeeper Dashboard.
> - **Navigation Placement**: The new **"Shop Schedule & Auto Opening/Closing"** page will be accessible through both a **prominent quick-access dashboard card** on the Shopkeeper Dashboard and a **direct navigation menu item**.

- **Accurate 30-Minute Forward Calculation**: The default expected delivery time is calculated strictly as **current time + 30 minutes in Indian Standard Time (`Asia/Kolkata`)**, handling midnight transitions (e.g., 11:45 PM -> 12:15 AM Tomorrow) and shop working hours.
- **Operating Schedule Respect**: Deliveries will never promise a time outside of the shop's operating schedule. If an order arrives after closing or is deferred to tomorrow, expected delivery will be calculated as **next opening time + 30 minutes** (e.g., 10:00 AM next working day -> 10:30 AM Tomorrow).
- **Dual Schedule Management**: Includes both the **Master Settings Table** (Shop Opening/Closing time, Auto-Opening ON/OFF, Auto-Closing ON/OFF, Manual Override) and the **Custom Weekly Timetable** (Monday–Sunday independent times and ON/OFF day toggles).

---

### 1. Overview & Core Concept

- **What It Does**: Empowers shopkeepers in Mahoba to control their operating schedules, automated opening/closing behaviors, and late-night order handling, while providing customers with honest, real-time availability and exact 30-minute delivery estimates in IST.
- **Target Audience / Persona**:
  - **Shopkeepers**: Easily set working hours, toggle auto-opening/closing, manage weekly holidays, and handle late-night orders with 1-tap options ("Deliver Today" vs "Deliver Tomorrow").
  - **Customers**: Clear visibility into whether a shop is open or closed, when it opens next, accurate expected delivery times, and prevention of dead-end orders when a shop is closed.
  - **Admins (Pranjal Rajpoot & Chanchal Didi)**: Centralized visibility into all shop schedules and availability with override capabilities.
- **Key Value**: Eliminates unrealistic delivery promises, prevents customer frustration from closed shops, and streamlines shopkeeper operations.

---

### 2. User Experience & Visual Design

#### A. Shopkeeper Dashboard: "Shop Schedule & Auto Opening/Closing" Screen
1. **Header & Live IST Status Banner**:
   - Status badge: Bright Green `OPEN – Accepting Orders` or Red `CLOSED – Opens Today/Tomorrow at HH:MM AM/PM`.
   - Subtitle: Current Mahoba time in IST (`Asia/Kolkata`), updating automatically.
2. **Schedule Management Table (Master Settings)**:
   - Formatted as a structured, responsive table card:
     | Setting | Current Value | Interactive Control |
     | :--- | :--- | :--- |
     | **Shop Opening Time** | 10:00 AM (default) | [Edit] time picker |
     | **Shop Closing Time** | 10:00 PM (default) | [Edit] time picker |
     | **Automatic Opening** | ON / OFF | Accessible M3 Switch |
     | **Automatic Closing** | ON / OFF | Accessible M3 Switch |
     | **Manual Shop Status**| Open / Closed | [Change] Toggle Button |
     | **Current Shop Status**| Live Calculated | View-only pill badge |
     | **Next Opening Time** | Auto Computed | View-only timestamp |
     | **Next Closing Time** | Auto Computed | View-only timestamp |
3. **Custom Weekly Shop Schedule (7 Days)**:
   - Monday to Sunday list/table where each day has:
     - Day name & calendar icon.
     - Individual Opening time & Closing time buttons.
     - Day ON/OFF switch (e.g., Sunday toggle OFF for weekly holiday).
   - "Save Weekly Schedule" primary button (`GreenPrimary`) with loading and success feedback.
4. **Intelligent Late-Night Order Modal/Banner**:
   - For orders placed within 30 minutes of closing (e.g., 9:30 PM for a 10:00 PM close):
     - Alert: *"Late-Night Order Received! Can you deliver this order today?"*
     - Action 1: **DELIVER TODAY** -> Shopkeeper sets/confirms delivery time within working hours (e.g., 9:55 PM).
     - Action 2: **DELIVER TOMORROW** -> Automatically calculates next working day opening time + 30 mins (e.g., 10:30 AM Tomorrow).

#### B. Customer Interface Updates
1. **Home & Shop Listing Cards**:
   - Prominent Green `OPEN` or Red `CLOSED` pill badge.
   - For closed shops: *"Opens Tomorrow at 10:00 AM"* or *"Opens Monday at 10:00 AM"*.
2. **Shop Detail & Product Detail**:
   - Expected Delivery timestamp: e.g., *"Expected Delivery: 9:58 PM"* (when ordered at 9:28 PM).
   - When delivery is deferred to tomorrow: *"Scheduled for Tomorrow: 10:30 AM"*.
   - When closed: "Add to Cart" and "Order Now" buttons disabled with descriptive text *"Shop is currently closed. Opens tomorrow at 10:00 AM"*.
3. **Checkout Screen**:
   - Active validation preventing checkout if any shop in the cart is currently closed or outside delivery bounds.

#### C. Admin Portals
- Admin Dashboard tab displays every registered shop's live status, opening/closing schedule, auto toggles, and emergency override switch.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: IST Timezone Anchoring (`Asia/Kolkata`)**:
  - *Chosen Approach*: All time calculations use `java.util.TimeZone.getTimeZone("Asia/Kolkata")` and `Calendar` / `java.time` with explicit IST offsets.
  - *Why*: Ensures that users in different time zones or devices with incorrect clocks always compute shop availability relative to the merchant's physical location in Mahoba, UP.
- **Decision 2: 30-Minute Forward Engine with Schedule Awareness**:
  - *Chosen Approach*: `calculateExpectedDelivery(currentTime, shopSchedule)` computes `currentTime + 30 mins`. If this crosses the shop's closing time, or if the shop is currently closed, it advances to the shop's next open day's opening time + 30 minutes.
  - *Why*: Directly satisfies Section 1 & Section 4 of the prompt while eliminating false delivery promises.
- **Decision 3: Dual Persistence (Firestore + AppPersistenceManager)**:
  - *Chosen Approach*: Schedule data is saved to Firestore under `/shops/{shopId}` and cached in `AppPersistenceManager`.
  - *Why*: Guarantees instant local responsiveness even on poor network connectivity while syncing across devices and surviving app restarts/logouts.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Customer Interface                              │
│   • Displays GREEN "OPEN" / RED "CLOSED" badges                        │
│   • Expected Delivery Time: Exactly +30 mins in IST (Asia/Kolkata)     │
│   • Midnight & tomorrow schedule detection ("12:15 AM Tomorrow")       │
│   • Disables Add to Cart / Checkout when shop is unavailable           │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Reads live status
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       ShopScheduleEngine (IST)                         │
│   • parseTimeToMinutes() / formatMinutesToTime()                       │
│   • evaluateLiveStatus(schedule, currentTimeIST)                       │
│   • computeAccurateExpectedDelivery(orderTimeIST, shopSchedule)        │
│   • getNextOpeningSlot(schedule, fromDateIST)                          │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │
       ┌────────────────────────────┴───────────────────────────┐
       │ Updates schedule                                       │
┌──────┴─────────────────────────────────┐   ┌──────────────────┴────────┐
│ Shop Schedule & Auto Opening Screen    │   │ Late-Night Order Manager  │
│ • Master Settings Table                │   │ • Within 30 mins of close │
│   - Open/Close Times (10 AM / 10 PM)   │   │ • DELIVER TODAY option    │
│   - Auto Opening ON/OFF                │   │ • DELIVER TOMORROW option │
│   - Auto Closing ON/OFF                │   │   (Next Open + 30 mins)   │
│   - Manual Open/Closed toggle          │   └───────────────────────────┘
│ • Custom Weekly 7-Day Timetable        │
│   - Mon-Sun independent hours & ON/OFF │
└──────────────────────┬─────────────────┘
                       │ Writes
                       ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     Firestore & Persistence                            │
│   • Collection: /shops/{shopId} & /shops/{shopId}/schedule             │
│   • Security Rules: Shopkeeper owns their shop's schedule              │
│   • Admin read & emergency disable permissions                         │
└────────────────────────────────────────────────────────────────────────┘
```

#### Core Data Models (`ShopSchedule.kt`)
- `DaySchedule`: `dayOfWeek`, `openingTime` ("10:00 AM"), `closingTime` ("10:00 PM"), `isOpen` (Boolean).
- `WeeklySchedule`:
  - `shopId`: String
  - `days`: List<DaySchedule> (Monday through Sunday)
  - `autoOpenEnabled`: Boolean = true
  - `autoCloseEnabled`: Boolean = true
  - `manualStatus`: String? = null ("OPEN", "CLOSED", null = Auto)
  - `isTemporaryClosed`: Boolean = false
  - `lastScheduleUpdate`: Long
- `DeliveryEstimate`:
  - `formattedEstimate`: String (e.g., "9:58 PM", "12:15 AM (Tomorrow)", "10:30 AM Tomorrow")
  - `isNextDay`: Boolean
  - `targetDeliveryTimestamp`: Long
  - `isShopOpen`: Boolean

---

### 5. Implementation Steps & Verification Plan

1. **Step 1: Enhance `ShopSchedule.kt` and `ShopScheduleUtils.kt`**:
   - Implement `calculateAccurateExpectedDelivery(currentTime: Long, schedule: WeeklySchedule): DeliveryEstimate`.
   - Add next working day scanner that skips disabled days (holidays).
   - Add support for auto-opening and auto-closing toggle logic.
2. **Step 2: Update `ShopkeeperDashboardScreen.kt` & Navigation**:
   - Add prominent "Shop Schedule & Auto Opening/Closing" card on Dashboard.
   - Add late-night order alert banner with "DELIVER TODAY" and "DELIVER TOMORROW" handlers.
3. **Step 3: Redesign `ShopTimingScheduleScreen.kt`**:
   - Match the exact Master Settings Table from Section 2 of the PDF.
   - Include editable 7-day timetable with ON/OFF switches per day.
   - Connect all toggles to Firestore and repository.
4. **Step 4: Update Customer-Facing Screens (`HomeScreen`, `ShopDetailScreen`, `ProductDetailScreen`, `CheckoutScreen`)**:
   - Display real-time 30-min expected delivery in IST.
   - Gating: prevent adding items or checking out when shop is closed.
5. **Step 5: Admin Integration (`AdminDashboardScreen`)**:
   - Add shop schedule inspection and temporary disable actions.
6. **Step 6: Security Rules & Unit Tests**:
   - Add unit tests verifying the 15 test scenarios listed in the PDF.
   - Compile and verify with `compile_applet`.
7. **Step 7: Multi-Shop Price Comparison Card on Product Details (Completed)**:
   - Built interactive `MultiShopPriceComparisonCard.kt` in `com.example.ui.components`.
   - Matching engine matches comparable products across local Mahoba shops by keyword tokens and category.
   - Computes side-by-side shop ratings (⭐ 4.8), distance, live open/closed status, item prices, delivery fees, and net total cost.
   - Highlights the #1 lowest price in Mahoba with a glowing `★ Lowest Price` badge.
   - Supports 1-tap store switching and direct "+ Add to Cart" action buttons.
   - Mirrored across both the Android app (`ProductDetailScreen.kt`) and FlashDrop Website modal (`flashdrop-website/app.js` and `styles.css`).
   - Verified with unit tests in `MultiShopPriceComparisonTest.kt` and `compile_applet`.

