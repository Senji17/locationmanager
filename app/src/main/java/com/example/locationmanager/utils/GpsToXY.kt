package com.example.locationmanager.utils

import kotlin.math.*

object GpsToXY {
    private const val EARTH_RADIUS = 6371000.0 // Earth's radius in meters

    fun latLonToXY(lat: Double, lon: Double, latRef: Double, lonRef: Double): Pair<Double, Double> {
        // Convert latitude & longitude differences to radians
        val dLat = Math.toRadians(lat - latRef)
        val dLon = Math.toRadians(lon - lonRef)

        // Improved formula for better accuracy
        val x = EARTH_RADIUS * dLon * cos(Math.toRadians(latRef))
        val y = EARTH_RADIUS * dLat

        return Pair(x, y) // Return coordinates in meters

    }
}
