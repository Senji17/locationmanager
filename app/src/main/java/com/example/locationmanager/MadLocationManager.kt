package com.example.locationmanager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlin.math.sqrt

class MadLocationManager(private val context: Context) : LocationListener, SensorEventListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var mainActivity: MainActivity? = null
    private val client = OkHttpClient()

    private var accelerationX = 0.0
    private var accelerationY = 0.0
    private var accelerationZ = 0.0

    private var gyroscopeX = 0.0
    private var gyroscopeY = 0.0
    private var gyroscopeZ = 0.0

    private var magnetometerX = 0.0
    private var magnetometerY = 0.0
    private var magnetometerZ = 0.0

    private var velocityX = 0.0
    private var velocityY = 0.0
    private var velocityZ = 0.0

    private var lastTimestamp: Long = 0
    private val alpha = 0.8
    private var fusedSpeed = 0.0
    private var accelerationMagnitude = 0.0


    private var geomagnetic = FloatArray(3)
    private var rotationMatrix = FloatArray(9)
    private var orientationAngles = FloatArray(3)

    private var roll = 0.0
    private var pitch = 0.0
    private var yaw = 0.0
    
    fun setMainActivity(activity: MainActivity) {
        mainActivity = activity
    }

    fun startTracking() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 1f, this)
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI)
        } else {
            Log.e("MadLocationManager", "❌ Missing location permission")
        }
    }

    fun stopTracking() {
        locationManager.removeUpdates(this)
        sensorManager.unregisterListener(this)
    }

    override fun onLocationChanged(location: Location) {
        val speedGps = location.speed.toDouble()
        fusedSpeed = alpha * getImuSpeed() + (1 - alpha) * speedGps
        val altitude = if (location.hasAltitude()) location.altitude else Double.NaN

        // ✅ Log GPS & Sensor Data
        Log.d("MadLocationManager", """
            📍 GPS Data:
            Lat: ${location.latitude}, Lon: ${location.longitude}, Alt: $altitude
            🚀 Speed: GPS=$speedGps m/s, Fused=$fusedSpeed m/s
            🔄 IMU: Acceleration=$accelerationMagnitude m/s²
            🔁 Gyroscope: X=$gyroscopeX, Y=$gyroscopeY, Z=$gyroscopeZ
            🧭 Magnetometer: X=$magnetometerX, Y=$magnetometerY, Z=$magnetometerZ
            
        """.trimIndent())



        // ✅ Send Data to Server
        sendDataToServer(
            location.latitude,
            location.longitude,
            altitude,
            speedGps,
            fusedSpeed,
            accelerationMagnitude,
            gyroscopeX,
            gyroscopeY,
            gyroscopeZ,
            magnetometerX,
            magnetometerY,
            magnetometerZ,

        )
    }

    private var gravity = FloatArray(3) { 0f }
    private val linearAcceleration = FloatArray(3) { 0f } // ความเร่งจริงที่ต้องการ

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    gravity = it.values.clone() // ✅ ถูกต้อง  สำหรับคำนวณ Orientation
                    val deltaTime = (it.timestamp - lastTimestamp) / 1_000_000_000.0
                    lastTimestamp = it.timestamp

                    accelerationX = it.values[0].toDouble()
                    accelerationY = it.values[1].toDouble()
                    accelerationZ = it.values[2].toDouble()

                    velocityX += accelerationX * deltaTime
                    velocityY += accelerationY * deltaTime
                    velocityZ += accelerationZ * deltaTime

                    accelerationMagnitude = sqrt(accelerationX * accelerationX + accelerationY * accelerationY + accelerationZ * accelerationZ)
                }
                Sensor.TYPE_GYROSCOPE -> {
                    gyroscopeX = it.values[0].toDouble()
                    gyroscopeY = it.values[1].toDouble()
                    gyroscopeZ = it.values[2].toDouble()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    geomagnetic = it.values.clone()
                    magnetometerX = it.values[0].toDouble()
                    magnetometerY = it.values[1].toDouble()
                    magnetometerZ = it.values[2].toDouble()
                }
            }

            // ✅ คำนวณ RPY หลังจากรับทั้ง accelerometer และ magnetometer
            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                roll = Math.toDegrees(orientationAngles[2].toDouble())
                pitch = Math.toDegrees(orientationAngles[1].toDouble())
                yaw = Math.toDegrees(orientationAngles[0].toDouble())
            }
        }
    }


    private fun getImuSpeed(): Double {
        return sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun sendDataToServer(
        latitude: Double, longitude: Double, altitude: Double,
        speedGps: Double, speedFused: Double, acceleration: Double,
        gyroX: Double, gyroY: Double, gyroZ: Double,
        magX: Double, magY: Double, magZ: Double
    ) {
        val jsonData = JSONObject().apply {
            put("type", "sensor_data")
            put("data", JSONObject().apply {
                put("latitude", latitude)
                put("longitude", longitude)
                put("altitude", altitude)
                put("speed_gps", speedGps)
                put("speed_fused", speedFused)
                put("acceleration", acceleration)
                put("gyroscope_x", gyroX)
                put("gyroscope_y", gyroY)
                put("gyroscope_z", gyroZ)
                put("magnetometer_x", magX)
                put("magnetometer_y", magY)
                put("magnetometer_z", magZ)
                put("roll", roll)
                put("pitch", pitch)
                put("yaw", yaw)
            })
        }

        val requestBody = jsonData.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        Log.d("MadLocationManager", "📤 Sending Data: $jsonData")

        val request = Request.Builder()
            .url("http://188.166.222.52:11111/upload_cloud")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MadLocationManager", "❌ Failed to send sensor data: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("MadLocationManager", "✅ Sensor data sent successfully!")
                } else {
                    Log.e("MadLocationManager", "❌ Server Error: ${response.message}")
                }
            }
        })
    }

}
