package com.xiaoai.assistant.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 小管家 API 客户端
 *
 * 通过 HTTP 与服务端通信：
 * - GET  /api/status  -> 服务状态
 * - POST /api/chat   -> 发送文字，获取回复
 */
class ApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /** 获取服务端状态 */
    suspend fun getStatus(serverUrl: String): ServerStatus = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$serverUrl/api/status")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            val json = JSONObject(body)

            ServerStatus(
                running = json.optString("status", "") == "running",
                speaker = json.optString("speaker", "未知"),
                llmModel = json.optString("llm_model", "未知")
            )
        } catch (e: Exception) {
            ServerStatus(running = false, speaker = "连接失败", llmModel = "")
        }
    }

    /** 发送文字消息，获取 AI 回复 */
    suspend fun chat(serverUrl: String, text: String): ChatResult = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("text", text)
            }

            val request = Request.Builder()
                .url("$serverUrl/api/chat")
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            val json = JSONObject(body)

            ChatResult(
                reply = json.optString("reply", ""),
                pushedToSpeaker = json.optBoolean("pushed_to_speaker", false),
                error = json.optString("error", null)
            )
        } catch (e: Exception) {
            ChatResult(
                reply = "",
                pushedToSpeaker = false,
                error = "网络错误: ${e.localizedMessage ?: "未知错误"}"
            )
        }
    }
}

data class ServerStatus(
    val running: Boolean,
    val speaker: String,
    val llmModel: String
)

data class ChatResult(
    val reply: String,
    val pushedToSpeaker: Boolean,
    val error: String? = null
)