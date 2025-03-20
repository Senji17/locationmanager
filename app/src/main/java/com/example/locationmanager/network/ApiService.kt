package com.example.locationmanager.network

import com.example.locationmanager.model.GpsData
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("upload_cloud")  // ✅ Matches FastAPI endpoint
    fun sendGpsData(@Body gpsData: GpsData): Call<Void>
}
