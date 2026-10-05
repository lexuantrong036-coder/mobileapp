package com.screensolver.ai.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.screensolver.ai.data.model.AiConfig
import com.screensolver.ai.data.model.SolverResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class AiSolverRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {

    companion object {
        private const val TAG = "AiSolverRepository"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private const val SYSTEM_PROMPT = """Bạn là trợ lý AI chuyên giải câu hỏi trắc nghiệm siêu tốc từ ảnh chụp màn hình điện thoại.
Hãy quan sát ảnh, tìm câu hỏi và các phương án trả lời (A, B, C, D hoặc Đúng/Sai).
Hãy chọn phương án đúng nhất và trả về DUY NHẤT một chuỗi JSON hợp lệ không chứa markdown code block với cấu trúc:
{
  "choice": "chữ cái đáp án đúng (ví dụ: A, B, C, D hoặc Đúng/Sai)",
  "text": "tóm tắt nội dung của phương án đó (tối đa 8 từ)",
  "explanation": "lý do chọn đáp án này trong đúng 1 câu ngắn gọn"
}"""
    }

    /**
     * Gửi ảnh chụp màn hình dạng Base64 lên 9router API để giải câu hỏi
     */
    suspend fun solveScreen(
        config: AiConfig,
        base64Jpeg: String
    ): Result<SolverResult> = withContext(Dispatchers.IO) {
        try {
            if (config.apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Chưa cấu hình API Key 9router. Vui lòng vào Cài đặt để nhập."))
            }

            val endpoint = resolveEndpoint(config.baseUrl)
            Log.d(TAG, "Gửi ảnh tới $endpoint với model ${config.model}")

            // Xây dựng JSON payload chuẩn OpenAI
            val requestJson = JsonObject().apply {
                addProperty("model", config.model)
                addProperty("temperature", 0.1)
                addProperty("max_tokens", 180)

                val messagesArray = com.google.gson.JsonArray()

                // System message
                messagesArray.add(JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", SYSTEM_PROMPT)
                })

                // User message có Vision Image
                val userContentArray = com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("type", "text")
                        addProperty("text", "Hãy tìm câu hỏi trên ảnh và trả về đáp án đúng nhất.")
                    })
                    add(JsonObject().apply {
                        addProperty("type", "image_url")
                        add("image_url", JsonObject().apply {
                            addProperty("url", "data:image/jpeg;base64,$base64Jpeg")
                        })
                    })
                }

                messagesArray.add(JsonObject().apply {
                    addProperty("role", "user")
                    add("content", userContentArray)
                })

                add("messages", messagesArray)
            }

            val requestBody = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val errorMsg = when (response.code) {
                        401 -> "API Key không hợp lệ hoặc hết hạn (Mã 401)"
                        404 -> "Model '${config.model}' không tồn tại hoặc sai URL (Mã 404)"
                        429 -> "Hết hạn mức hoặc bị giới hạn tốc độ yêu cầu (Mã 429)"
                        else -> "Lỗi từ máy chủ 9router: ${response.code} - ${response.message}"
                    }
                    Log.e(TAG, "Lỗi API: $errorMsg | Body: $bodyString")
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val parsedResult = parseChatCompletionResponse(bodyString)
                Result.success(parsedResult)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ngoại lệ khi gọi 9router API", e)
            Result.failure(e)
        }
    }

    /**
     * Kiểm tra nhanh API Key và Model mà không cần tải ảnh lên
     */
    suspend fun testConnection(config: AiConfig): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (config.apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Vui lòng nhập API Key"))
            }

            val endpoint = resolveEndpoint(config.baseUrl)
            val requestJson = JsonObject().apply {
                addProperty("model", config.model)
                addProperty("max_tokens", 10)
                val messagesArray = com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("role", "user")
                        addProperty("content", "Xin chào, hãy trả lời 'OK'")
                    })
                }
                add("messages", messagesArray)
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success("Kết nối thành công tới ${config.model}!")
                } else {
                    Result.failure(Exception("Lỗi kết nối [${response.code}]: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Bóc tách JSON từ phản hồi Chat Completion
     */
    private fun parseChatCompletionResponse(responseBody: String): SolverResult {
        try {
            val root = JsonParser.parseString(responseBody).asJsonObject
            val choices = root.getAsJsonArray("choices")
            if (choices == null || choices.size() == 0) {
                return SolverResult(choice = "?", text = "Không có câu trả lời", explanation = responseBody)
            }

            val firstChoice = choices[0].asJsonObject
            val message = firstChoice.getAsJsonObject("message")
            val content = message?.get("content")?.asString.orEmpty().trim()

            // Làm sạch nội dung JSON nếu model bọc trong ```json ... ```
            val cleanedJson = cleanJsonString(content)

            return try {
                val jsonObject = JsonParser.parseString(cleanedJson).asJsonObject
                val choice = jsonObject.get("choice")?.asString ?: extractChoiceFromText(content)
                val text = jsonObject.get("text")?.asString ?: "Đáp án đã chọn"
                val explanation = jsonObject.get("explanation")?.asString ?: ""

                SolverResult(
                    choice = choice.uppercase().trim(),
                    text = text.trim(),
                    explanation = explanation.trim(),
                    rawContent = content
                )
            } catch (e: Exception) {
                // Fallback nếu model trả về text thường thay vì JSON
                Log.w(TAG, "Không thể parse JSON, sử dụng heuristic fallback: $content")
                val choice = extractChoiceFromText(content)
                SolverResult(
                    choice = choice,
                    text = content.take(30),
                    explanation = content,
                    rawContent = content
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi bóc tách response: $responseBody", e)
            return SolverResult(choice = "!", text = "Lỗi định dạng", explanation = e.localizedMessage ?: "Unknown error")
        }
    }

    private fun cleanJsonString(input: String): String {
        var str = input.trim()
        if (str.startsWith("```json")) {
            str = str.removePrefix("```json")
        } else if (str.startsWith("```")) {
            str = str.removePrefix("```")
        }
        if (str.endsWith("```")) {
            str = str.removeSuffix("```")
        }
        return str.trim()
    }

    private fun extractChoiceFromText(text: String): String {
        val pattern = Pattern.compile("(?i)(?:đáp án|chọn|câu|phương án)\\s*[:\\-]?\\s*([A-D])\\b")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.uppercase() ?: "?"
        }

        val directChoice = Pattern.compile("\\b([A-D])\\b")
        val directMatcher = directChoice.matcher(text)
        if (directMatcher.find()) {
            return directMatcher.group(1)?.uppercase() ?: "?"
        }

        return "OK"
    }

    private fun resolveEndpoint(baseUrl: String): String {
        val clean = baseUrl.trim().trimEnd('/')
        return if (clean.endsWith("/chat/completions")) {
            clean
        } else {
            "$clean/chat/completions"
        }
    }
}
