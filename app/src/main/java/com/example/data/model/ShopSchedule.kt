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
 * Complete weekly schedule and auto opening/closing configuration for a shop in Mahoba.
 * Default is 10:00 AM - 10:00 PM.
 */
@IgnoreExtraProperties
data class WeeklySchedule(
    val shopId: String = "",
    val defaultOpeningTime: String = "10:00 AM",
    val defaultClosingTime: String = "10:00 PM",
    val days: List<DaySchedule> = defaultDays(),
    val autoScheduleEnabled: Boolean = true,
    val autoOpeningEnabled: Boolean = true,
    val autoClosingEnabled: Boolean = true,
    val manualOverride: Boolean? = null, // null = follow automatic rules; true = manual OPEN; false = manual CLOSED
    val isTemporarilyClosed: Boolean = false, // Emergency / Admin temporary disable without losing schedule
    val lastManualOverrideTimestamp: Long = 0L,
    val lastScheduleUpdate: Long = System.currentTimeMillis(),
    val updatedBy: String = "shopkeeper",
    val lateNightConfirmationEnabled: Boolean = true
) {
    companion object {
        val DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        fun defaultDays(
            defaultOpen: String = "10:00 AM",
            defaultClose: String = "10:00 PM"
        ): List<DaySchedule> {
            return DAY_NAMES.map { day ->
                DaySchedule(
                    dayOfWeek = day,
                    openingTime = defaultOpen,
                    closingTime = defaultClose,
                    isOpen = true
                )
            }
        }
    }
}

/**
 * Result of accurate expected delivery calculation in Indian Standard Time (Asia/Kolkata).
 */
data class DeliveryEstimateResult(
    val formattedEstimate: String, // e.g., "9:58 PM", "12:15 AM (Tomorrow)", "10:30 AM Tomorrow"
    val isTomorrowOrLater: Boolean = false,
    val isWithinWorkingHours: Boolean = true,
    val nextOpeningTimeDisplay: String = "10:00 AM Tomorrow",
    val targetDayName: String = "Today"
)

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
    val minutesUntilClosing: Int = 999,
    val nextOpeningTime: String = "Tomorrow, 10:00 AM",
    val nextClosingTime: String = "Today, 10:00 PM",
    val expectedDeliveryEstimate: String = "30 mins",
    val isDeliveryScheduledTomorrow: Boolean = false,
    val currentStatusMode: String = "Automatic" // "Automatic", "Manual Open", "Manual Closed", "Temporarily Disabled"
)

/**
 * Utility functions for parsing 12-hour AM/PM times, evaluating live shop availability,
 * and computing accurate 30-minute expected delivery times in Indian Standard Time (Asia/Kolkata).
 */
object ShopScheduleUtils {

    val IST_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    const val DEFAULT_DELIVERY_OFFSET_MINUTES = 30
    const val LATE_NIGHT_WINDOW_MINUTES = 30

    fun createDefaultWeeklySchedule(shopId: String = ""): WeeklySchedule = WeeklySchedule(shopId = shopId)

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
            val minute = if (parts.size > 1) parts[1].trim().filter { it.isDigit() }.toInt() else 0

            if (isPM && hour < 12) hour += 12
            if (isAM && hour == 12) hour = 0

            ((hour * 60) + minute).coerceIn(0, 1439)
        } catch (e: Exception) {
            if (clean.contains("PM")) 22 * 60 else 10 * 60
        }
    }

    /**
     * Converts minutes from midnight into standard "10:00 AM" / "09:58 PM" formatted string (with 2-digit hour for schedule tables).
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
     * Converts minutes from midnight into natural display time without leading zero on single-digit hours:
     * e.g., "9:58 PM", "12:15 AM", "8:50 AM", "10:30 AM".
     */
    fun formatMinutesToDisplayTime(minutes: Int): String {
        val safeMinutes = (minutes % 1440 + 1440) % 1440
        val totalHours = safeMinutes / 60
        val mins = safeMinutes % 60
        val amPm = if (totalHours >= 12) "PM" else "AM"
        val displayHour = when (val h = totalHours % 12) {
            0 -> 12
            else -> h
        }
        return String.format(Locale.ENGLISH, "%d:%02d %s", displayHour, mins, amPm)
    }

    /**
     * Returns current IST calendar (Asia/Kolkata).
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
     * Pure 30-minute forward calculation from the given IST calendar (or current IST time).
     * Examples:
     * - 9:28 PM -> "9:58 PM"
     * - 11:45 PM -> "12:15 AM (Tomorrow)"
     * - 8:20 AM -> "8:50 AM"
     */
    fun calculateExact30MinForward(
        customIstCalendar: Calendar? = null,
        offsetMinutes: Int = DEFAULT_DELIVERY_OFFSET_MINUTES
    ): String {
        val cal = customIstCalendar ?: getIstCalendar()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)
        val currentTotalMinutes = currentHour * 60 + currentMinute
        val targetTotalMinutes = currentTotalMinutes + offsetMinutes
        val crossesMidnight = targetTotalMinutes >= 1440
        val formattedTime = formatMinutesToDisplayTime(targetTotalMinutes)
        return if (crossesMidnight) {
            "$formattedTime (Tomorrow)"
        } else {
            formattedTime
        }
    }

    /**
     * Finds the next available working day starting from `startDayOffset` (1 = Tomorrow).
     * Automatically skips scheduled holidays / days where `isOpen == false`.
     */
    fun findNextWorkingDaySchedule(
        schedule: WeeklySchedule,
        baseCalendar: Calendar = getIstCalendar(),
        startDayOffset: Int = 1
    ): Pair<Int, DaySchedule> {
        for (offset in startDayOffset..(startDayOffset + 7)) {
            val testCal = (baseCalendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val dayName = getDayNameFromCalendar(testCal)
            val daySched = schedule.days.find { it.dayOfWeek.equals(dayName, ignoreCase = true) }
                ?: DaySchedule(
                    dayOfWeek = dayName,
                    openingTime = schedule.defaultOpeningTime,
                    closingTime = schedule.defaultClosingTime,
                    isOpen = true
                )
            if (daySched.isOpen) {
                return offset to daySched
            }
        }
        // Fallback if all 7 days are marked OFF: tomorrow with default opening time
        val fallbackCal = (baseCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val fallbackDay = getDayNameFromCalendar(fallbackCal)
        return 1 to DaySchedule(fallbackDay, schedule.defaultOpeningTime, schedule.defaultClosingTime, true)
    }

    /**
     * Calculates expected delivery for an order scheduled for the next working day
     * (30 minutes forward from the next working day's opening time).
     * Example:
     * - Next opening: 10:00 AM Tomorrow -> "10:30 AM Tomorrow"
     * - If Tomorrow (Sunday) is OFF and Monday opens at 10:00 AM -> "10:30 AM Monday"
     */
    fun calculateNextWorkingDayDeliveryEstimate(
        schedule: WeeklySchedule,
        customIstCalendar: Calendar? = null
    ): DeliveryEstimateResult {
        val cal = customIstCalendar ?: getIstCalendar()
        val (offset, nextDaySched) = findNextWorkingDaySchedule(schedule, cal, startDayOffset = 1)
        val openMins = parseTimeToMinutes(nextDaySched.openingTime)
        val deliveryMins = openMins + DEFAULT_DELIVERY_OFFSET_MINUTES
        val deliveryTimeStr = formatMinutesToDisplayTime(deliveryMins)
        val nextOpenTimeStr = formatMinutesToDisplayTime(openMins)

        val dayLabel = if (offset == 1) "Tomorrow" else nextDaySched.dayOfWeek
        return DeliveryEstimateResult(
            formattedEstimate = "$deliveryTimeStr $dayLabel",
            isTomorrowOrLater = true,
            isWithinWorkingHours = true,
            nextOpeningTimeDisplay = "$nextOpenTimeStr $dayLabel",
            targetDayName = dayLabel
        )
    }

    /**
     * Calculates the accurate expected delivery time for a shop respecting its schedule and IST timezone.
     * - If the shop is open and currentTime + 30 mins is within operating hours (or manual open / autoClose OFF),
     *   returns exact +30 mins (e.g. "9:58 PM" or "12:15 AM (Tomorrow)").
     * - If the shop is closed before today's opening (and today is open), returns today's opening + 30 mins
     *   (e.g. "10:30 AM Today").
     * - If the shop is closed for the day or `scheduleForTomorrow` is true, returns next working day's
     *   opening + 30 mins (e.g. "10:30 AM Tomorrow" or "10:30 AM Monday").
     */
    fun calculateAccurateDeliveryEstimate(
        schedule: WeeklySchedule,
        customIstCalendar: Calendar? = null,
        scheduleForTomorrow: Boolean = false
    ): DeliveryEstimateResult {
        val cal = customIstCalendar ?: getIstCalendar()
        if (scheduleForTomorrow) {
            return calculateNextWorkingDayDeliveryEstimate(schedule, cal)
        }

        val currentDay = getDayNameFromCalendar(cal)
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val todaySchedule = schedule.days.find { it.dayOfWeek.equals(currentDay, ignoreCase = true) }
            ?: DaySchedule(currentDay, schedule.defaultOpeningTime, schedule.defaultClosingTime, true)

        val openMinutes = parseTimeToMinutes(todaySchedule.openingTime)
        val closeMinutes = parseTimeToMinutes(todaySchedule.closingTime)
        val liveStatus = evaluateLiveStatus(schedule, cal)

        if (liveStatus.isOpen) {
            val targetMinutes = currentMinutes + DEFAULT_DELIVERY_OFFSET_MINUTES
            val crossesMidnight = targetMinutes >= 1440
            // If autoClosingEnabled is ON and manualOverride is null, ensure we don't promise past closing time
            // unless it's a late-night order that the shopkeeper confirms for today
            if (schedule.autoClosingEnabled && schedule.manualOverride == null && targetMinutes > closeMinutes && currentMinutes <= closeMinutes) {
                // Within the 30-min late-night window before closing:
                // If delivered today, must be by closing time; otherwise next working day
                val clampedMins = closeMinutes
                return DeliveryEstimateResult(
                    formattedEstimate = formatMinutesToDisplayTime(clampedMins),
                    isTomorrowOrLater = false,
                    isWithinWorkingHours = true,
                    nextOpeningTimeDisplay = liveStatus.nextOpeningTime,
                    targetDayName = "Today"
                )
            }

            val displayTime = formatMinutesToDisplayTime(targetMinutes)
            return DeliveryEstimateResult(
                formattedEstimate = if (crossesMidnight) "$displayTime (Tomorrow)" else displayTime,
                isTomorrowOrLater = crossesMidnight,
                isWithinWorkingHours = true,
                nextOpeningTimeDisplay = liveStatus.nextOpeningTime,
                targetDayName = if (crossesMidnight) "Tomorrow" else "Today"
            )
        } else {
            // Shop is currently closed
            // Check if it opens later today
            if (!schedule.isTemporarilyClosed &&
                schedule.manualOverride == null &&
                schedule.autoOpeningEnabled &&
                todaySchedule.isOpen &&
                currentMinutes < openMinutes
            ) {
                val deliveryAtOpen = openMinutes + DEFAULT_DELIVERY_OFFSET_MINUTES
                val openDisplay = formatMinutesToDisplayTime(openMinutes)
                val deliveryDisplay = formatMinutesToDisplayTime(deliveryAtOpen)
                return DeliveryEstimateResult(
                    formattedEstimate = "$deliveryDisplay Today",
                    isTomorrowOrLater = false,
                    isWithinWorkingHours = false,
                    nextOpeningTimeDisplay = "$openDisplay Today",
                    targetDayName = "Today"
                )
            }
            // Otherwise opens on next available working day
            return calculateNextWorkingDayDeliveryEstimate(schedule, cal).copy(isWithinWorkingHours = false)
        }
    }

    /**
     * Validates whether a proposed delivery time string (e.g. "9:55 PM") is within today's operating hours.
     */
    fun isTimeWithinTodayOperatingHours(
        timeStr: String,
        schedule: WeeklySchedule,
        customIstCalendar: Calendar? = null
    ): Boolean {
        val cal = customIstCalendar ?: getIstCalendar()
        val currentDay = getDayNameFromCalendar(cal)
        val todaySchedule = schedule.days.find { it.dayOfWeek.equals(currentDay, ignoreCase = true) }
            ?: DaySchedule(currentDay, schedule.defaultOpeningTime, schedule.defaultClosingTime, true)
        if (!todaySchedule.isOpen) return false
        val openMins = parseTimeToMinutes(todaySchedule.openingTime)
        val closeMins = parseTimeToMinutes(todaySchedule.closingTime)
        val candidateMins = parseTimeToMinutes(timeStr)
        return candidateMins in openMins..closeMins
    }

    /**
     * Computes the live opening status for a shop based on its saved weekly schedule,
     * Automatic Opening ON/OFF, Automatic Closing ON/OFF, Manual Status, and Temporary Closure in IST.
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
            ?: DaySchedule(currentDay, schedule.defaultOpeningTime, schedule.defaultClosingTime, true)

        // Find next available working day (skipping disabled days/holidays)
        val (nextDayOffset, nextWorkingDaySchedule) = findNextWorkingDaySchedule(schedule, cal, startDayOffset = 1)
        val nextDayPrefix = if (nextDayOffset == 1) "Tomorrow" else nextWorkingDaySchedule.dayOfWeek

        val openMinutes = parseTimeToMinutes(todaySchedule.openingTime)
        val closeMinutes = parseTimeToMinutes(todaySchedule.closingTime)

        // Late-night order detection: within 30 minutes before closing time (e.g. 9:30 PM for 10:00 PM closing)
        val minutesUntilClosing = closeMinutes - currentMinutes
        val isNearClosing = todaySchedule.isOpen &&
                currentMinutes >= (closeMinutes - LATE_NIGHT_WINDOW_MINUTES) &&
                currentMinutes <= closeMinutes

        val nextOpenIfClosedToday = "$nextDayPrefix at ${nextWorkingDaySchedule.openingTime}"
        val nextCloseDefault = if (todaySchedule.isOpen && currentMinutes < closeMinutes) {
            "Today at ${todaySchedule.closingTime}"
        } else {
            "$nextDayPrefix at ${nextWorkingDaySchedule.closingTime}"
        }

        // 1. Check temporary closure (Admin or emergency disable)
        if (schedule.isTemporarilyClosed) {
            val nextEst = calculateNextWorkingDayDeliveryEstimate(schedule, cal)
            return ShopLiveStatus(
                isOpen = false,
                statusLabel = "CLOSED – Temporarily Unavailable (Opens $nextOpenIfClosedToday)",
                currentDay = currentDay,
                openingTimeToday = todaySchedule.openingTime,
                closingTimeToday = todaySchedule.closingTime,
                isNearClosingTime = false,
                minutesUntilClosing = 0,
                nextOpeningTime = nextOpenIfClosedToday,
                nextClosingTime = nextCloseDefault,
                expectedDeliveryEstimate = nextEst.formattedEstimate,
                isDeliveryScheduledTomorrow = true,
                currentStatusMode = "Temporarily Disabled"
            )
        }

        val isWithinScheduledHours = todaySchedule.isOpen && currentMinutes in openMinutes..closeMinutes

        // 2. Evaluate Manual Override vs Automatic Opening & Automatic Closing rules
        if (schedule.manualOverride != null) {
            val manualOpen = schedule.manualOverride
            // If shopkeeper manually opened the shop, check if Automatic Closing should close it after closing time
            val autoClosedAfterHours = manualOpen &&
                    schedule.autoClosingEnabled &&
                    todaySchedule.isOpen &&
                    currentMinutes > closeMinutes

            // If shopkeeper manually closed the shop before opening, check if Automatic Opening should open it during open hours
            val EFFECTIVE_OPEN = when {
                autoClosedAfterHours -> false
                else -> manualOpen
            }

            if (EFFECTIVE_OPEN) {
                val targetMins = currentMinutes + DEFAULT_DELIVERY_OFFSET_MINUTES
                val crossesMidnight = targetMins >= 1440
                val disp = formatMinutesToDisplayTime(targetMins)
                val est = if (crossesMidnight) "$disp (Tomorrow)" else disp
                return ShopLiveStatus(
                    isOpen = true,
                    statusLabel = "OPEN – Accepting Orders",
                    currentDay = currentDay,
                    openingTimeToday = todaySchedule.openingTime,
                    closingTimeToday = todaySchedule.closingTime,
                    isNearClosingTime = isNearClosing,
                    minutesUntilClosing = if (minutesUntilClosing >= 0) minutesUntilClosing else 999,
                    nextOpeningTime = if (todaySchedule.isOpen && currentMinutes < openMinutes) "Today at ${todaySchedule.openingTime}" else nextOpenIfClosedToday,
                    nextClosingTime = if (schedule.autoClosingEnabled) nextCloseDefault else "Manual Closing (Auto-Close OFF)",
                    expectedDeliveryEstimate = est,
                    isDeliveryScheduledTomorrow = crossesMidnight,
                    currentStatusMode = "Manual Open"
                )
            } else {
                val nextOpenLabel = if (todaySchedule.isOpen && currentMinutes < openMinutes && schedule.autoOpeningEnabled) {
                    "Today at ${todaySchedule.openingTime}"
                } else {
                    nextOpenIfClosedToday
                }
                val nextEst = calculateNextWorkingDayDeliveryEstimate(schedule, cal)
                return ShopLiveStatus(
                    isOpen = false,
                    statusLabel = "CLOSED – Opens $nextOpenLabel",
                    currentDay = currentDay,
                    openingTimeToday = todaySchedule.openingTime,
                    closingTimeToday = todaySchedule.closingTime,
                    isNearClosingTime = false,
                    minutesUntilClosing = 0,
                    nextOpeningTime = nextOpenLabel,
                    nextClosingTime = nextCloseDefault,
                    expectedDeliveryEstimate = nextEst.formattedEstimate,
                    isDeliveryScheduledTomorrow = true,
                    currentStatusMode = if (autoClosedAfterHours) "Automatic" else "Manual Closed"
                )
            }
        }

        // 3. Pure Automatic Mode (manualOverride == null)
        if (!todaySchedule.isOpen) {
            // Today is a scheduled weekly holiday / OFF
            val nextEst = calculateNextWorkingDayDeliveryEstimate(schedule, cal)
            return ShopLiveStatus(
                isOpen = false,
                statusLabel = "CLOSED – Opens $nextOpenIfClosedToday",
                currentDay = currentDay,
                openingTimeToday = todaySchedule.openingTime,
                closingTimeToday = todaySchedule.closingTime,
                isNearClosingTime = false,
                minutesUntilClosing = 999,
                nextOpeningTime = nextOpenIfClosedToday,
                nextClosingTime = nextCloseDefault,
                expectedDeliveryEstimate = nextEst.formattedEstimate,
                isDeliveryScheduledTomorrow = true,
                currentStatusMode = "Automatic"
            )
        }

        return when {
            currentMinutes < openMinutes -> {
                // Before today's opening time
                val openDisplay = formatMinutesToDisplayTime(openMinutes + DEFAULT_DELIVERY_OFFSET_MINUTES)
                val label = if (schedule.autoOpeningEnabled) {
                    "CLOSED – Opens Today at ${todaySchedule.openingTime}"
                } else {
                    "CLOSED – Manual Opening Required (${todaySchedule.openingTime})"
                }
                ShopLiveStatus(
                    isOpen = false,
                    statusLabel = label,
                    currentDay = currentDay,
                    openingTimeToday = todaySchedule.openingTime,
                    closingTimeToday = todaySchedule.closingTime,
                    isNearClosingTime = false,
                    minutesUntilClosing = closeMinutes - currentMinutes,
                    nextOpeningTime = if (schedule.autoOpeningEnabled) "Today at ${todaySchedule.openingTime}" else "Manual (Scheduled ${todaySchedule.openingTime})",
                    nextClosingTime = "Today at ${todaySchedule.closingTime}",
                    expectedDeliveryEstimate = "$openDisplay Today",
                    isDeliveryScheduledTomorrow = false,
                    currentStatusMode = "Automatic"
                )
            }

            currentMinutes > closeMinutes -> {
                // After today's closing time
                if (!schedule.autoClosingEnabled && schedule.autoOpeningEnabled) {
                    // Automatic Closing is OFF: shop stays open past closing time until manually closed!
                    val targetMins = currentMinutes + DEFAULT_DELIVERY_OFFSET_MINUTES
                    val crossesMidnight = targetMins >= 1440
                    val disp = formatMinutesToDisplayTime(targetMins)
                    ShopLiveStatus(
                        isOpen = true,
                        statusLabel = "OPEN – Accepting Orders (Auto-Close OFF)",
                        currentDay = currentDay,
                        openingTimeToday = todaySchedule.openingTime,
                        closingTimeToday = todaySchedule.closingTime,
                        isNearClosingTime = false,
                        minutesUntilClosing = 999,
                        nextOpeningTime = nextOpenIfClosedToday,
                        nextClosingTime = "Manual Closing (Auto-Close OFF)",
                        expectedDeliveryEstimate = if (crossesMidnight) "$disp (Tomorrow)" else disp,
                        isDeliveryScheduledTomorrow = crossesMidnight,
                        currentStatusMode = "Automatic (Auto-Close OFF)"
                    )
                } else {
                    // Automatic Closing is ON: shop automatically closes at closing time
                    val nextEst = calculateNextWorkingDayDeliveryEstimate(schedule, cal)
                    ShopLiveStatus(
                        isOpen = false,
                        statusLabel = "CLOSED – Opens $nextOpenIfClosedToday",
                        currentDay = currentDay,
                        openingTimeToday = todaySchedule.openingTime,
                        closingTimeToday = todaySchedule.closingTime,
                        isNearClosingTime = false,
                        minutesUntilClosing = 0,
                        nextOpeningTime = nextOpenIfClosedToday,
                        nextClosingTime = nextCloseDefault,
                        expectedDeliveryEstimate = nextEst.formattedEstimate,
                        isDeliveryScheduledTomorrow = true,
                        currentStatusMode = "Automatic"
                    )
                }
            }

            else -> {
                // Within scheduled open hours (currentMinutes in openMinutes..closeMinutes)
                if (!schedule.autoOpeningEnabled) {
                    // Automatic Opening is OFF and shopkeeper hasn't manually opened it
                    val nextEst = calculateNextWorkingDayDeliveryEstimate(schedule, cal)
                    ShopLiveStatus(
                        isOpen = false,
                        statusLabel = "CLOSED – Awaiting Shopkeeper Manual Open",
                        currentDay = currentDay,
                        openingTimeToday = todaySchedule.openingTime,
                        closingTimeToday = todaySchedule.closingTime,
                        isNearClosingTime = false,
                        minutesUntilClosing = minutesUntilClosing,
                        nextOpeningTime = "Manual Open Required",
                        nextClosingTime = "Today at ${todaySchedule.closingTime}",
                        expectedDeliveryEstimate = nextEst.formattedEstimate,
                        isDeliveryScheduledTomorrow = true,
                        currentStatusMode = "Automatic (Auto-Open OFF)"
                    )
                } else {
                    // Automatic Opening is ON: Shop is OPEN!
                    val targetMins = currentMinutes + DEFAULT_DELIVERY_OFFSET_MINUTES
                    val crossesMidnight = targetMins >= 1440
                    val disp = formatMinutesToDisplayTime(targetMins)
                    val est = if (crossesMidnight) "$disp (Tomorrow)" else disp
                    ShopLiveStatus(
                        isOpen = true,
                        statusLabel = "OPEN – Accepting Orders",
                        currentDay = currentDay,
                        openingTimeToday = todaySchedule.openingTime,
                        closingTimeToday = todaySchedule.closingTime,
                        isNearClosingTime = isNearClosing,
                        minutesUntilClosing = minutesUntilClosing,
                        nextOpeningTime = nextOpenIfClosedToday,
                        nextClosingTime = if (schedule.autoClosingEnabled) "Today at ${todaySchedule.closingTime}" else "Manual Closing (Auto-Close OFF)",
                        expectedDeliveryEstimate = est,
                        isDeliveryScheduledTomorrow = crossesMidnight,
                        currentStatusMode = "Automatic"
                    )
                }
            }
        }
    }
}
