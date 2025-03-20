package com.example.locationmanager.model

data class GpsData(
    val latitude: Double,         // ✅ Latitude (GPS)
    val longitude: Double,        // ✅ Longitude (GPS)
    val altitude: Double,         // ✅ Altitude (if available)
    val x: Double,                // ✅ Converted X coordinate
    val y: Double,                // ✅ Converted Y coordinate
    val speed_gps: Double,        // ✅ Raw GPS speed
    val speed_fused: Double,      // ✅ Fused GPS + IMU speed
    val acceleration: Double,     // ✅ Acceleration magnitude
    val gyroscopeX: Double,       // ✅ Gyroscope X-axis
    val gyroscopeY: Double,       // ✅ Gyroscope Y-axis
    val gyroscopeZ: Double,       // ✅ Gyroscope Z-axis
    val magnetometerX: Double,    // ✅ Magnetometer X-axis
    val magnetometerY: Double,    // ✅ Magnetometer Y-axis
    val magnetometerZ: Double,    // ✅ Magnetometer Z-axis
    val timestamp: Long           // ✅ Timestamp for data logging
)
