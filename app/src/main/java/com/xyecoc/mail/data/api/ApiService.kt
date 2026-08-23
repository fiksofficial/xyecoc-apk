package com.xyecoc.mail.data.api

import com.google.gson.reflect.TypeToken
import com.xyecoc.mail.data.model.ApiResponse
import com.xyecoc.mail.data.model.RequestPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ApiService(
    val baseUrl: String = "https://api.xyecoc.com",
    val cdnUrl: String = "https://cdn.xyecoc.com"
) {
    // Shared, process-wide client & serializer (see Network).
    val client: OkHttpClient get() = Network.client
    private val gson get() = Network.gson
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun <T> request(payload: RequestPayload, typeToken: TypeToken<ApiResponse<T>>): ApiResponse<T> {
        // Network I/O on Dispatchers.IO ...
        val responseBody = withContext(Dispatchers.IO) {
            try {
                val jsonBody = gson.toJson(payload)
                val request = Request.Builder()
                    .url("$baseUrl/request")
                    .post(jsonBody.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body?.string().orEmpty() else ""
                }
            } catch (e: Exception) {
                ""
            }
        }

        if (responseBody.isBlank()) {
            return ApiResponse(status = 0, message = "Network error")
        }

        // ... CPU-bound JSON parsing on Dispatchers.Default so a large ApiResponse
        // union never hogs an IO thread another request needs.
        return withContext(Dispatchers.Default) {
            try {
                @Suppress("UNCHECKED_CAST")
                gson.fromJson<ApiResponse<T>>(responseBody, typeToken.type)
            } catch (e: Exception) {
                ApiResponse(status = 0, message = e.localizedMessage ?: "Parse error")
            }
        }
    }
}
