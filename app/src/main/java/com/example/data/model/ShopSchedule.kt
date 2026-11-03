package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Single day operating schedule for a merchant in Mahoba.
 */
@IgnoreExtraProperties
data class DaySchedule(
    val dayOfWeek: String = "Monday", // "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    val openingTime: String = "10:00 AM",
    val closingTime: String = "10:00 PM",
    val isOpen: Boolean = true // Shop Status ON/OFF toggle for this specific day
)

/**
 * Complete weekly schedule for a shop in Mahoba.
 * Default is 10:00 AM - 10:00 PM, Monday through Sunday.
 */
@IgnoreExtraProperties
data class WeeklySchedule(
    val shopId: String = "",
    val days: List<DaySchedule> = defaultDays(),
    val autoScheduleEnabled: Boolean = true,
    val manualOverride: Boolean? = null, // null = auto schedule; true = manual temporary OPEN; false = manual temporary CLOSED
    val lastManualOverrideTimestamp: Long = 0L
) {
    companion object {
        val DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        fun defaultDays(): List<DaySchedule> {
            return DAY_NAMES.map { day ->
                DaySchedule(
                    dayOfWeek = day,
                    openingTime = "10:00 AM",
                    closingTime = "10:00 PM",
                    isOpen = true
                )
            }
        }
    }
}

/**
 * Real-time live status evaluation result calculated in Indian Standard Time (IST).
 */
data class ShopLiveStatus(
    val isOpen: Boolean,
    val statusLabel: String, // "OPEN – Accepting Orders", "CLOSED – Opens Today at 10:00 AM", "CLOSED – Opens Tomorrow at 10:00 AM"
    val currentDay: String,
    val openingTimeToday: String,
    val closingTimeToday: String,
    val isNearClosingTime: Boolean = false,
    val minutesUntilClosing: Int = 999
)

/**
 * Utility functions for parsing 12-hour AM/PM times and evaluating live status in Indian Standard Time (IST).
 */
object ShopScheduleUtils {

    val IST_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    /**
     * Converts a string like "10:00 AM", "9:30 PM", "12:00 PM" into total minutes from midnight (0..1439).
     */
    fun parseTimeToMinutes(timeStr: String): Int {
        val clean = timeStr.trim().uppercase(Locale.ENGLISH)
        return try {
            val isPM = clean.contains("PM")
            val isAM = clean.contains("AM")
            val timePart = clean.replace("AM", "").replace("PM", "").trim()
            val parts = timePart.split(":")
            var hour = parts[0].trim().toInt()
            val minute = if (parts.size > 1) parts[1].trim().toInt() else 0

            if (isPM && hour < 12) hour += 12
            if (isAM && hour == 12) hour = 0

            (hour * 60) + minute
        } catch (e: Exception) {
            // Safe fallback: 10:00 AM (600) or 10:00 PM (1320)
            if (clean.contains("PM")) 22 * 60 else 10 * 60
        }
    }

    /**
     * Converts minutes from midnight into standard "10:00 AM" / "10:00 PM" formatted string.
     */
    fun formatMinutesToTime(minutes: Int): String {
        val safeMinutes = (minutes % 1440 + 1440) % 1440
        val totalHours = safeMinutes / 60
        val mins = safeMinutes % 60
        val amPm = if (totalHours >= 12) "PM" else "AM"
        val displayHour = when (val h = totalHours % 12) {
            0 -> 12
            else -> h
        }
        return String.format(Locale.ENGLISH, "%02d:%02d %s", displayHour, mins, amPm)
    }

    /**
     * Returns current IST calendar.
     */
    fun getIstCalendar(): Calendar = Calendar.getInstance(IST_TIMEZONE)

    /**
     * Maps java.util.Calendar.DAY_OF_WEEK (SUNDAY=1, MONDAY=2, ...) to DaySchedule name.
     */
    fun getDayNameFromCalendar(cal: Calendar): String {
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Monday"
        }
    }

    /**
     * Computes the live opening status for a shop based on its saved weekly schedule in Indian Standard Time (IST).
     */
    fun evaluateLiveStatus(
        schedule: WeeklySchedule,
        customIstCalendar: Calendar? = null
    ): ShopLiveStatus {
        val cal = customIstCalendar ?: getIstCalendar()
        val currentDay = getDayNameFromCalendar(cal)
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)
        val currentMinutes = currentHour * 60 + currentMinute

        // Find today's day schedule
        val todaySchedule = schedule.days.find { it.dayOfWeek.equals(currentDay, ignoreCase = true) }
            ?: DaySchedule(currentDay, "10:00 AM", "10:00 PM", true)

        // Find tomorrow's day schedule
        val tomorrowCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowDay = getDayNameFromCalendar(tomorrowCal)
        val tomorrowSchedule = schedule.days.find { it.dayOfWeek.equals(tomorrowDay, ignoreCase = true) }
            ?: DaySchedule(tomorrowDay, "10:00 AM", "10:00 PM", true)

        val openMinutes = parseTimeToMinutes(todaySchedule.openingTime)
        val closeMinutes = parseTimeToMinutes(todaySchedule.closingTime)

        // Check if near closing time (within 60 minutes before scheduled closing time, e.g. at 9:30 PM for 10:00 PM close)
        val minutesUntilClosing = closeMinutes - currentMinutes
        val isNearClosing = todaySchedule.isOpen &&
                currentMinutes >= (closeMinutes - 60) &&
                currentMinutes <= closeMinutes

        // If manual temporary override exists
        if (schedule.manualOverride != null) {
            val isManuallyOpen = schedule.manualOverride
            val label = if (isManuallyOpen) {
                "OPEN – Accepting Orders"
            } else {
                "CLOSED – Temporarily Closed by Shopkeeper"
            }
            return ShopLiveStatus(
                isOpen = isManuallyOpen,
                statusLabel = label,
                currentDay = currentDay,
                openingTimeToday = todaySchedule.openingTime,
                closingTimeToday = todaySchedule.closingTime,
                isNearClosingTime = isManuallyOpen && isNearClosing,
                minutesUntilClosing = minutesUntilClosing
            )
        }

        // Automatic schedule evaluation
        if (!todaySchedule.isOpen) {
            // Day toggle is OFF
            val label = if (tomorrowSchedule.isOpen) {
                "CLOSED – Opens Tomorrow at ${tomorrowSchedule.openingTime}"
            } else {
                // Look for next open day
                val nextOpen = schedule.days.firstOrNull { it.isOpen }
                if (nextOpen != null) "CLOSED – Opens ${nextOpen.dayOfWeek} at ${nextOpen.openingTime}" else "CLOSED"
            }
            return ShopLiveStatus(
                isOpen = false,
                statusLabel = label,
                currentDay = currentDay,
                openingTimeToday = todaySchedule.openingTime,
                closingTimeToday = todaySchedule.closingTime,
                isNearClosingTime = false,
                minutesUntilClosing = 999
            )
        }

        // Day is marked ON
        return when {
            currentMinutes < openMinutes -> {
                // Before opening time
                ShopLiveStatus(
                    isOpen = false,
                    statusLabel = "CLOSED – Opens Today at ${todaySchedule.openingTime}",
                    currentDay = currentDay,
                    openingTimeToday = todaySchedule.openingTime,
                    closingTimeToday = todaySchedule.closingTime,
                    isNearClosingTime = false,
                    minutesUntilClosing = closeMinutes - currentMinutes
                )
            }
            currentMinutes > closeMinutes -> {
                // After closing time
                val label = if (tomorrowSchedule.isOpen) {
                    "CLOSED – Opens Tomorrow at ${tomorrowSchedule.openingTime}"
                } else {
                    val nextOpen = schedule.days.firstOrNull { it.isOpen }
                    if (nextOpen != null) "CLOSED – Opens ${nextOpen.dayOfWeek} at ${nextOpen.openingTime}" else "CLOSED"
                }
                ShopLiveStatus(
                    isOpen = false,
                    statusLabel = label,
                    currentDay = currentDay,
                    openingTimeToday = todaySchedule.openingTime,
                    closingTimeToday = todaySchedule.closingTime,
                    isNearClosingTime = false,
                    minutesUntilClosing = 0
                )
            }
            else -> {
                // Within open hours
                ShopLiveStatus(
                    isOpen = true,
                    statusLabel = "OPEN – Accepting Orders",
                    currentDay = currentDay,
                    openingTimeToday = todaySchedule.openingTime,
                    closingTimeToday = todaySchedule.closingTime,
                    isNearClosingTime = isNearClosing,
                    minutesUntilClosing = minutesUntilClosing
                )
            }
        }
    }
}
