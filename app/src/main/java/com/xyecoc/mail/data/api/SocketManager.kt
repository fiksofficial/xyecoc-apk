package com.xyecoc.mail.data.api

import android.util.Log
import com.google.gson.reflect.TypeToken
import com.xyecoc.mail.data.model.ApiResponse
import com.xyecoc.mail.data.model.RequestPayload
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

class SocketManager(
    private val socketUrl: String = "wss://api.xyecoc.com"
) {
    private var socket: Socket? = null
    private val gson get() = Network.gson

    private val _mailUpdatesFlow = MutableSharedFlow<ApiResponse<Any>>(extraBufferCapacity = 64)
    val mailUpdatesFlow: SharedFlow<ApiResponse<Any>> = _mailUpdatesFlow.asSharedFlow()

    fun connect(token: String?) {
        disconnect()
        try {
            val opts = IO.Options().apply {
                query = "token=${token ?: ""}"
                transports = arrayOf("websocket", "polling")
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                timeout = 20000
            }
            socket = IO.socket(socketUrl, opts).apply {
                on(Socket.EVENT_CONNECT) {
                    Log.d("SocketManager", "Connected to WebSocket")
                }
                on(Socket.EVENT_DISCONNECT) {
                    Log.d("SocketManager", "Disconnected from WebSocket")
                }
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    Log.w("SocketManager", "Socket connection error: ${args.getOrNull(0)}")
                }
                on("response") { args ->
                    val data = args.getOrNull(0)
                    if (data != null) {
                        try {
                            val jsonStr = data.toString()
                            val type = object : TypeToken<ApiResponse<Any>>() {}.type
                            val response: ApiResponse<Any> = gson.fromJson(jsonStr, type)
                            _mailUpdatesFlow.tryEmit(response)
                        } catch (e: Exception) {
                            Log.e("SocketManager", "Failed to parse socket response: $data", e)
                        }
                    }
                }
                connect()
            }
        } catch (e: Exception) {
            Log.e("SocketManager", "Socket init error", e)
        }
    }

    fun emitRequest(payload: RequestPayload) {
        try {
            val jsonStr = gson.toJson(payload)
            val jsonObject = JSONObject(jsonStr)
            socket?.emit("request", jsonObject)
        } catch (e: Exception) {
            Log.e("SocketManager", "Failed to emit request", e)
        }
    }

    fun isConnected(): Boolean = socket?.connected() == true

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }

    companion object {
        /**
         * Single process-wide socket. Both the foreground service and the inbox
         * observe this one connection instead of opening a socket per repository.
         */
        val shared: SocketManager by lazy { SocketManager() }
    }
}
