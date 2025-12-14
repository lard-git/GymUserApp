package com.example.gymcustomer

data class GymStats(
    val currentOccupancy: Int = 0,
    val peakHour: String = "--:--",
    val totalTodayVisitors: Int = 0,
    val lastUpdated: String = ""
)