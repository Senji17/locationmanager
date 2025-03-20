package com.example.locationmanager

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.locationmanager.utils.GpsToXY

class MainActivity : AppCompatActivity() {

    private lateinit var madLocationManager: MadLocationManager
    private lateinit var webView: WebView
    private lateinit var gpsTextView: TextView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        madLocationManager = MadLocationManager(this)
        madLocationManager.setMainActivity(this)  // ✅ Set reference to MainActivity

        gpsTextView = findViewById(R.id.gpsTextView)
        webView = findViewById(R.id.osmWebView)

        checkLocationPermission()
        setupWebView()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            madLocationManager.startTracking()
        }
    }


    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.INTERNET
                ),
                1
            )
        } else {
            madLocationManager.startTracking()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT

        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/osm_map.html")

    }

    fun updateGpsData(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        speedGps: Double,
        speedFused: Double,
        acceleration: Double,
        accelerationMagnitude: Double,
        gyroX: Double,
        gyroY: Double,
        gyroZ: Double,
        magX: Double,
        magY: Double,
        magZ: Double // ✅ ต้องมี magZ ตรงนี้ด้วย
    ) {
        val (x, y) = GpsToXY.latLonToXY(latitude, longitude, 13.736717, 100.523186)

        runOnUiThread {
            gpsTextView.text = """
            📍 GPS Data:
            Lat: $latitude
            Lon: $longitude
            Altitude: ${"%.2f".format(altitude)} m
            Speed (GPS): ${"%.2f".format(speedGps)} m/s
            Speed (Fused): ${"%.2f".format(speedFused)} m/s
            Acceleration: ${"%.2f".format(acceleration)} m/s²
            X: ${"%.2f".format(x)}
            Y: ${"%.2f".format(y)}

            🌀 Gyroscope:
            X: ${"%.2f".format(gyroX)}
            Y: ${"%.2f".format(gyroY)}
            Z: ${"%.2f".format(gyroZ)}

            🧭 Magnetometer:
            X: ${"%.2f".format(magX)}
            Y: ${"%.2f".format(magY)}
            Z: ${"%.2f".format(magZ)}
        """.trimIndent()

            if (::webView.isInitialized) {
                val jsCommand = """
                receiveGpsFromAndroid($latitude, $longitude, $x, $y, $speedGps, $speedFused, 
                                      $acceleration, $gyroX, $gyroY, $gyroZ, $magX, $magY, $magZ)
            """.trimIndent()
                webView.evaluateJavascript(jsCommand, null)
            }
        }
    }

}

