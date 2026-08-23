package com.xyecoc.mail.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.xyecoc.mail.BuildConfig
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Process-wide networking singletons.
 *
 * Previously every repository/ViewModel default-constructed its own [ApiService],
 * each building a fresh [OkHttpClient] with its own connection pool, dispatcher and
 * DNS cache — which defeats HTTP/2 multiplexing and keep-alive reuse. Everything now
 * shares one client and one [Gson] instance.
 */
object Network {

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .apply {
                // Verbose body logging only in debug; never materialize bodies in release.
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        }
                    )
                }
            }
            .build()
    }

    val gson: Gson by lazy {
        GsonBuilder().serializeNulls().create()
    }
}
