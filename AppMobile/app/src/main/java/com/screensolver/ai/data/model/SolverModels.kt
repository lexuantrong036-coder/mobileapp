package com.screensolver.ai.data.model

import com.google.gson.annotations.SerializedName

/**
 * Kết quả giải câu hỏi trả về cho UI
 */
data class SolverResult(
    val choice: String,        // "A", "B", "C", "D"...
    val text: String,          // Tóm tắt nội dung đáp án (ngắn gọn)
    val explanation: String,   // Giải thích ngắn gọn 1 câu
    val rawContent: String = ""
)

/**
 * Cấu hình kết nối 9router AI
 */
data class AiConfig(
    val baseUrl: String = "https://api.9router.com/v1",
    val apiKey: String = "",
    val model: String = "gemini-1.5-flash",
    val isAutoLoopEnabled: Boolean = false,
    val autoIntervalSeconds: Float = 2.0f
)

// --- Các lớp hỗ trợ giao thức OpenAI-compatible Chat Completions ---

data class ChatCompletionRequest(
    @SerializedName("model") val model: String,
    @SerializedName("temperature") val temperature: Double = 0.1,
    @SerializedName("max_tokens") val maxTokens: Int = 200,
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("response_format") val responseFormat: ResponseFormat? = ResponseFormat("json_object")
)

data class ResponseFormat(
    @SerializedName("type") val type: String = "json_object"
)

data class ChatMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: Any // Có thể là String hoặc List<ContentPart>
)

data class ContentPart(
    @SerializedName("type") val type: String, // "text" hoặc "image_url"
    @SerializedName("text") val text: String? = null,
    @SerializedName("image_url") val imageUrl: ImageUrlWrapper? = null
)

data class ImageUrlWrapper(
    @SerializedName("url") val url: String // "data:image/jpeg;base64,..."
)

data class ChatCompletionResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("choices") val choices: List<ChatChoice>?
)

data class ChatChoice(
    @SerializedName("index") val index: Int,
    @SerializedName("message") val message: ChatResponseMessage?
)

data class ChatResponseMessage(
    @SerializedName("role") val role: String?,
    @SerializedName("content") val content: String?
)
