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
 */
class ApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)  // 下载可能较慢
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

    /** 发送文字消息 */
    suspend fun chat(serverUrl: String, text: String): ChatResult = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply { put("text", text) }
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
                error = if (json.has("error") && json.getString("error").isNotBlank()) json.getString("error") else null
            )
        } catch (e: Exception) {
            ChatResult("", false, "网络错误: ${e.localizedMessage ?: "未知错误"}")
        }
    }

    /** 搜索音乐 */
    suspend fun searchMusic(serverUrl: String, keyword: String): List<MusicResult> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply { put("keyword", keyword) }
            val request = Request.Builder()
                .url("$serverUrl/api/music/search")
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            val json = JSONObject(body)

            if (json.has("error")) {
                return@withContext emptyList()
            }

            val results = json.optJSONArray("results")
            val list = mutableListOf<MusicResult>()
            if (results != null) {
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    list.add(MusicResult(
                        id = item.optString("id", ""),
                        name = item.optString("name", ""),
                        artist = item.optString("artist", ""),
                        album = item.optString("album", ""),
                        ext = item.optString("ext", ""),
                        size = item.optString("size", ""),
                        duration = item.optString("duration", "")
                    ))
                }
            }
            return@withContext list
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    /** 下载并播放到音箱 */
    suspend fun downloadMusic(serverUrl: String, id: String, name: String, artist: String): String? = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("id", id)
                put("name", name)
                put("artist", artist)
            }
            val request = Request.Builder()
                .url("$serverUrl/api/music/download")
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            val json = JSONObject(body)

            return@withContext if (json.optBoolean("success", false)) {
                json.optString("file", null)
            } else null
        } catch (e: Exception) {
            return@withContext null
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
) {
    fun hasError(): Boolean = !error.isNullOrBlank()
}

data class MusicResult(
    val id: String,
    val name: String,
    val artist: String,
    val album: String,
    val ext: String,
    val size: String,
    val duration: String
)
