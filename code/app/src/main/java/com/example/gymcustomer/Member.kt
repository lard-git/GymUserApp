package com.example.gymcustomer

import java.io.Serializable

data class Member(
    val personal_info: Map<String, Any>? = null,
    val membership: Map<String, Any>? = null,
    val gym_data: Map<String, Any>? = null,
    val attendance_history: Map<String, Any>? = null // Add this to prevent warning
) : Serializable {

    // Safe getter methods that work with raw maps
    fun safePersonalInfo(): PersonalInfo = PersonalInfo(personal_info)
    fun safeMembership(): Membership = Membership(membership)
    fun safeGymData(): GymData = GymData(gym_data)

    class PersonalInfo(private val map: Map<String, Any>?) {
        fun safeFirstName(): String = getString("firstname")
        fun safeLastName(): String = getString("lastname")
        fun safePhone(): String = getString("phone")

        private fun getString(key: String): String {
            return when (val value = map?.get(key)) {
                is String -> value
                is Long -> value.toString()
                is Int -> value.toString()
                else -> ""
            }
        }
    }

    class Membership(private val map: Map<String, Any>?) {
        fun safeStatus(): String = getString("status")
        fun safeStartDate(): String = getString("start_date")
        fun safeEndDate(): String = getString("end_date")
        fun safePaymentAmount(): Int = getInt("payment_amount")
        fun safeMonthsPaid(): Int = getInt("months_paid")
        fun safeRemainingDays(): Int = getInt("remaining_days")

        private fun getString(key: String): String {
            return when (val value = map?.get(key)) {
                is String -> value
                is Long -> value.toString()
                is Int -> value.toString()
                else -> ""
            }
        }

        private fun getInt(key: String): Int {
            return when (val value = map?.get(key)) {
                is Int -> value
                is Long -> value.toInt()
                is String -> value.toIntOrNull() ?: 0
                else -> 0
            }
        }
    }

    class GymData(private val map: Map<String, Any>?) {
        fun safeIsCheckedIn(): Boolean = map?.get("is_checked_in") as? Boolean ?: false
        fun safeTotalVisits(): Int = getInt("total_visits")
        fun safeTotalTimeSpent(): Int = getInt("total_time_spent")
        fun safeUid(): String = getString("uid")

        private fun getString(key: String): String {
            return when (val value = map?.get(key)) {
                is String -> value
                is Long -> value.toString()
                is Int -> value.toString()
                else -> ""
            }
        }

        private fun getInt(key: String): Int {
            return when (val value = map?.get(key)) {
                is Int -> value
                is Long -> value.toInt()
                is String -> value.toIntOrNull() ?: 0
                else -> 0
            }
        }
    }
}