package com.xyecoc.mail.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.xyecoc.mail.data.model.ApiResponse
import com.xyecoc.mail.data.model.RequestPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class ApiService(
    val baseUrl: String = "https://api.xyecoc.com",
    val cdnUrl: String = "https://cdn.xyecoc.com"
) {
    private val gson = GsonBuilder().serializeNulls().create()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor { message ->
            Log.d("XyecocApi", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    suspend fun <T> request(payload: RequestPayload, typeToken: TypeToken<ApiResponse<T>>): ApiResponse<T> =
        withContext(Dispatchers.IO) {
            try {
                val jsonBody = gson.toJson(payload)
                val request = Request.Builder()
                    .url("$baseUrl/request")
                    .post(jsonBody.toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    gson.fromJson(responseBody, typeToken.type)
                } else {
                    ApiResponse(status = 0, message = "HTTP error: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("XyecocApi", "Request failed: ${e.message}", e)
                ApiResponse(status = 0, message = e.localizedMessage ?: "Network error")
            }
        }
}
