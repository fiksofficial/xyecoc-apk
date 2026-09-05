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
import com.xyecoc.mail.util.RemoteConfigManager

class ApiService(
    val baseUrl: String = "https://api.xyecoc.com",
    val cdnUrl: String = "https://cdn.xyecoc.com"
) {
    private val gson = GsonBuilder().serializeNulls().create()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getEffectiveUrl(): String {
        val rcUrl = RemoteConfigManager.apiBaseUrl
        return if (rcUrl.isNotBlank()) rcUrl else "$baseUrl/request"
    }

    val client: OkHttpClient
        get() {
            val timeout = RemoteConfigManager.apiTimeoutSeconds.coerceIn(5L, 120L)
            return OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor { message ->
                    Log.d("XyecocApi", message)
                }.apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .build()
        }

    suspend fun <T> request(payload: RequestPayload, typeToken: TypeToken<ApiResponse<T>>): ApiResponse<T> =
        withContext(Dispatchers.IO) {
            val maxRetries = RemoteConfigManager.apiRetryCount.toInt().coerceIn(1, 5)
            var attempt = 0
            var lastError = "Network error"

            while (attempt < maxRetries) {
                attempt++
                try {
                    val jsonBody = gson.toJson(payload)
                    val request = Request.Builder()
                        .url(getEffectiveUrl())
                        .header("Cache-Control", "no-transform")
                        .post(jsonBody.toRequestBody(jsonMediaType))
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful && responseBody.isNotBlank()) {
                        return@withContext gson.fromJson<ApiResponse<T>>(responseBody, typeToken.type)
                    } else {
                        lastError = "HTTP error: ${response.code}"
                    }
                } catch (e: Exception) {
                    Log.e("XyecocApi", "Attempt $attempt/$maxRetries failed: ${e.message}", e)
                    lastError = e.localizedMessage ?: "Network error"
                }
            }
            ApiResponse(status = 0, message = lastError)
        }
}
