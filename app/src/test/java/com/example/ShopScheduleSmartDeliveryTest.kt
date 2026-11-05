package com.example

import com.example.data.model.Product
import com.example.data.model.ShopScheduleUtils
import com.example.ui.components.CameraVisualSearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ShopScheduleSmartDeliveryTest {

    private fun istCalendar(
        year: Int = 2026,
        monthZeroBased: Int = Calendar.NOVEMBER,
        dayOfMonth: Int = 4, // Wednesday, Nov 4, 2026
        hourOfDay: Int,
        minute: Int
    ): Calendar {
        return Calendar.getInstance(ShopScheduleUtils.IST_TIMEZONE).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun testExact30MinForwardDeliveryCalculation_ExamplesFromPdf() {
        // Example 1: 9:28 PM -> 9:58 PM
        val cal928pm = istCalendar(hourOfDay = 21, minute = 28)
        val est928pm = ShopScheduleUtils.calculateExact30MinForward(cal928pm)
        assertEquals("9:58 PM", est928pm)

        // Example 2: 11:45 PM -> 12:15 AM (Tomorrow)
        val cal1145pm = istCalendar(hourOfDay = 23, minute = 45)
        val est1145pm = ShopScheduleUtils.calculateExact30MinForward(cal1145pm)
        assertEquals("12:15 AM (Tomorrow)", est1145pm)

        // Example 3: 8:20 AM -> 8:50 AM
        val cal820am = istCalendar(hourOfDay = 8, minute = 20)
        val est820am = ShopScheduleUtils.calculateExact30MinForward(cal820am)
        assertEquals("8:50 AM", est820am)
    }

    @Test
    fun testDefaultOpening10AmAndClosing10PmWithAutoOpenAndCloseOn() {
        val schedule = ShopScheduleUtils.createDefaultWeeklySchedule("sharma_general")
        assertEquals("10:00 AM", schedule.defaultOpeningTime)
        assertEquals("10:00 PM", schedule.defaultClosingTime)
        assertTrue(schedule.autoOpeningEnabled)
        assertTrue(schedule.autoClosingEnabled)

        // Before 10:00 AM (e.g., 9:15 AM) -> CLOSED
        val status915am = ShopScheduleUtils.evaluateLiveStatus(schedule, istCalendar(hourOfDay = 9, minute = 15))
        assertFalse(status915am.isOpen)

        // At 10:00 AM -> OPEN automatically
        val status1000am = ShopScheduleUtils.evaluateLiveStatus(schedule, istCalendar(hourOfDay = 10, minute = 0))
        assertTrue(status1000am.isOpen)

        // At 2:00 PM -> OPEN, expected delivery 2:30 PM
        val status200pm = ShopScheduleUtils.evaluateLiveStatus(schedule, istCalendar(hourOfDay = 14, minute = 0))
        assertTrue(status200pm.isOpen)
        assertEquals("2:30 PM", status200pm.expectedDeliveryEstimate)

        // At 10:01 PM -> CLOSED automatically
        val status1001pm = ShopScheduleUtils.evaluateLiveStatus(schedule, istCalendar(hourOfDay = 22, minute = 1))
        assertFalse(status1001pm.isOpen)
    }

    @Test
    fun testCustomOpeningAndClosingTimes() {
        val base = ShopScheduleUtils.createDefaultWeeklySchedule("custom_shop")
        val customDays = base.days.map { it.copy(openingTime = "08:30 AM", closingTime = "11:00 PM") }
        val customSchedule = base.copy(
            defaultOpeningTime = "08:30 AM",
            defaultClosingTime = "11:00 PM",
            days = customDays
        )

        // At 8:45 AM -> OPEN
        val status845am = ShopScheduleUtils.evaluateLiveStatus(customSchedule, istCalendar(hourOfDay = 8, minute = 45))
        assertTrue(status845am.isOpen)

        // At 10:35 PM (within 30 mins of 11:00 PM closing) -> OPEN + Near Closing Time
        val status1035pm = ShopScheduleUtils.evaluateLiveStatus(customSchedule, istCalendar(hourOfDay = 22, minute = 35))
        assertTrue(status1035pm.isOpen)
        assertTrue(status1035pm.isNearClosingTime)
    }

    @Test
    fun testAutoOpeningOffAndAutoClosingOffAndManualClose() {
        val base = ShopScheduleUtils.createDefaultWeeklySchedule("sharma_general")

        // Automatic Opening OFF -> Remains closed during daytime unless manually opened
        val autoOpenOff = base.copy(autoOpeningEnabled = false, manualOverride = null)
        val statusMidday = ShopScheduleUtils.evaluateLiveStatus(autoOpenOff, istCalendar(hourOfDay = 13, minute = 0))
        assertFalse(statusMidday.isOpen)

        // Automatic Closing OFF -> Remains open past 10:00 PM closing time
        val autoCloseOff = base.copy(autoClosingEnabled = false, manualOverride = null)
        val statusLateNight = ShopScheduleUtils.evaluateLiveStatus(autoCloseOff, istCalendar(hourOfDay = 22, minute = 30))
        assertTrue(statusLateNight.isOpen)

        // Manual Shop Close -> Immediately closes even at 1:00 PM
        val manualClosed = base.copy(manualOverride = false)
        val statusManualClosed = ShopScheduleUtils.evaluateLiveStatus(manualClosed, istCalendar(hourOfDay = 13, minute = 0))
        assertFalse(statusManualClosed.isOpen)
    }

    @Test
    fun testLateNightOrder930PmAndDeliverTomorrowCalculation() {
        val schedule = ShopScheduleUtils.createDefaultWeeklySchedule("sharma_general")

        // Order placed at 9:30 PM (30 minutes before 10:00 PM closing)
        val cal930pm = istCalendar(hourOfDay = 21, minute = 30)
        val status930pm = ShopScheduleUtils.evaluateLiveStatus(schedule, cal930pm)
        assertTrue(status930pm.isOpen)
        assertTrue(status930pm.isNearClosingTime)

        // Deliver Today estimate at 9:30 PM -> 10:00 PM (within operating hours)
        val todayEst = ShopScheduleUtils.calculateAccurateDeliveryEstimate(schedule, cal930pm, scheduleForTomorrow = false)
        assertEquals("10:00 PM", todayEst.formattedEstimate)
        assertFalse(todayEst.isTomorrowOrLater)

        // Deliver Tomorrow estimate -> 10:30 AM Tomorrow (Opening 10:00 AM + 30 minutes)
        val tomorrowEst = ShopScheduleUtils.calculateNextWorkingDayDeliveryEstimate(schedule, cal930pm)
        assertEquals("10:30 AM Tomorrow", tomorrowEst.formattedEstimate)
        assertTrue(tomorrowEst.isTomorrowOrLater)
    }

    @Test
    fun testCustomWeeklyScheduleSkipsClosedHolidayDayAndAdminTempDisablePreservesSchedule() {
        val base = ShopScheduleUtils.createDefaultWeeklySchedule("sharma_general")
        // Suppose Thursday is closed for weekly holiday, Friday opens at 09:00 AM
        val customDays = base.days.map { day ->
            when (day.dayOfWeek) {
                "Thursday" -> day.copy(isOpen = false) // Thursday OFF
                "Friday" -> day.copy(openingTime = "09:00 AM", closingTime = "09:00 PM", isOpen = true)
                else -> day
            }
        }
        val weeklySchedule = base.copy(days = customDays)

        // On Wednesday night (Nov 4, 2026 is Wednesday), next working day should skip Thursday and pick Friday 9:00 AM + 30 mins = 9:30 AM Friday
        val wednesdayNight = istCalendar(year = 2026, monthZeroBased = Calendar.NOVEMBER, dayOfMonth = 4, hourOfDay = 21, minute = 45)
        val nextWorkingEst = ShopScheduleUtils.calculateNextWorkingDayDeliveryEstimate(weeklySchedule, wednesdayNight)
        assertEquals("9:30 AM Friday", nextWorkingEst.formattedEstimate)

        // Admin temporarily disables shop -> Shop is closed, but custom weekly schedule days remain intact
        val adminDisabled = weeklySchedule.copy(isTemporarilyClosed = true)
        val liveDisabled = ShopScheduleUtils.evaluateLiveStatus(adminDisabled, istCalendar(hourOfDay = 12, minute = 0))
        assertFalse(liveDisabled.isOpen)
        assertEquals("09:00 AM", adminDisabled.days.first { it.dayOfWeek == "Friday" }.openingTime)
    }

    @Test
    fun testCameraVisualSearchLowestPriceFirstRanking() {
        val sampleCatalog = listOf(
            Product(
                id = "milk_premium",
                name = "Organic Cow Milk 1L",
                category = "Dairy",
                price = 78.0,
                originalPrice = 85.0,
                description = "Farm fresh organic milk",
                unit = "1 L",
                inStock = true,
                shopId = "shop_1"
            ),
            Product(
                id = "milk_out_of_stock_cheap",
                name = "Toned Milk Pouch",
                category = "Dairy",
                price = 25.0,
                originalPrice = 28.0,
                description = "Out of stock milk pouch",
                unit = "500 ml",
                inStock = false,
                shopId = "shop_1"
            ),
            Product(
                id = "milk_lowest_available",
                name = "Amul Taaza Milk",
                category = "Dairy",
                price = 27.0,
                originalPrice = 30.0,
                description = "Fresh pasteurized milk",
                unit = "500 ml",
                inStock = true,
                shopId = "shop_2"
            ),
            Product(
                id = "milk_mid_available",
                name = "Mother Dairy Full Cream Milk",
                category = "Dairy",
                price = 34.0,
                originalPrice = 36.0,
                description = "Rich full cream milk",
                unit = "500 ml",
                inStock = true,
                shopId = "shop_1"
            ),
            Product(
                id = "bread_1",
                name = "Whole Wheat Bread",
                category = "Bakery",
                price = 45.0,
                originalPrice = 50.0,
                description = "Fresh brown bread",
                unit = "400 g",
                inStock = true,
                shopId = "shop_2"
            )
        )

        val rankedMatches = CameraVisualSearchEngine.filterAndRankByLowestPrice(
            allProducts = sampleCatalog,
            query = "milk"
        )

        // All 4 milk products match; available (inStock = true) ones must come first, ordered by lowest price ascending
        assertEquals(4, rankedMatches.size)
        assertEquals("milk_lowest_available", rankedMatches[0].id)
        assertEquals(27.0, rankedMatches[0].price, 0.001)
        assertTrue(rankedMatches[0].inStock)

        assertEquals("milk_mid_available", rankedMatches[1].id)
        assertEquals(34.0, rankedMatches[1].price, 0.001)

        assertEquals("milk_premium", rankedMatches[2].id)
        assertEquals(78.0, rankedMatches[2].price, 0.001)

        // Out-of-stock item is placed after all available items
        assertEquals("milk_out_of_stock_cheap", rankedMatches[3].id)
        assertFalse(rankedMatches[3].inStock)
    }
}
