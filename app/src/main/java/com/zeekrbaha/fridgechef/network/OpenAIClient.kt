package com.zeekrbaha.fridgechef.network

import com.zeekrbaha.fridgechef.data.DailyPicks
import com.zeekrbaha.fridgechef.data.MealType
import com.zeekrbaha.fridgechef.data.Recipe
import com.zeekrbaha.fridgechef.data.RecipeStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

interface OpenAIClient {
    suspend fun suggestRecipes(ingredients: List<String>): List<Recipe>
    suspend fun suggestRecipes(imageJpeg: ByteArray): List<Recipe>
    suspend fun suggestRecipes(dishName: String): List<Recipe>
    suspend fun suggestRecipes(meal: MealType, style: RecipeStyle? = null): List<Recipe>
    suspend fun dailyPicks(): DailyPicks
}

class OpenAIChatClient(private val keyProvider: APIKeyProvider) : OpenAIClient {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun suggestRecipes(ingredients: List<String>): List<Recipe> {
        return recipeRequest("gpt-4o", Prompts.ingredients(), JsonPrimitive(ingredients.joinToString(", ")))
    }

    override suspend fun suggestRecipes(imageJpeg: ByteArray): List<Recipe> {
        val base64 = Base64.getEncoder().encodeToString(imageJpeg)
        val userContent = buildJsonArray {
            add(buildJsonObject { put("type", "text"); put("text", "") })
            add(
                buildJsonObject {
                    put("type", "image_url")
                    putJsonObject("image_url") { put("url", "data:image/jpeg;base64,$base64") }
                },
            )
        }
        return recipeRequest("gpt-4o", Prompts.image(), userContent)
    }

    override suspend fun suggestRecipes(dishName: String): List<Recipe> {
        return recipeRequest("gpt-4o", Prompts.dishName(), JsonPrimitive(dishName))
    }

    override suspend fun suggestRecipes(meal: MealType, style: RecipeStyle?): List<Recipe> {
        return recipeRequest("gpt-4o", Prompts.meal(meal, style), JsonPrimitive("Please suggest today's recipes."))
    }

    override suspend fun dailyPicks(): DailyPicks {
        val request = requestBody(
            model = "gpt-4o-mini",
            systemPrompt = Prompts.dailyPicks(),
            userContent = JsonPrimitive("Today"),
            schemaName = "daily_picks",
            schema = dailyPicksSchema(),
        )
        val text = post(request)
        return runCatching {
            val decoded = json.decodeFromString<DailyPicksResponse>(text)
            DailyPicks(decoded.breakfast, decoded.lunch, decoded.dinner)
        }.getOrElse { throw OpenAIError.Decoding }
    }

    private suspend fun recipeRequest(model: String, systemPrompt: String, userContent: kotlinx.serialization.json.JsonElement): List<Recipe> {
        val request = requestBody(model, systemPrompt, userContent, "recipes", recipeSchema())
        val text = post(request)
        return runCatching { json.decodeFromString<RecipeResponse>(text).recipes }.getOrElse {
            throw OpenAIError.Decoding
        }
    }

    private suspend fun post(body: JsonObject): String = withContext(Dispatchers.IO) {
        val apiKey = keyProvider.apiKey()
        if (apiKey.isBlank()) throw OpenAIError.Unauthorized

        runCatching {
            val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 30_000
                readTimeout = 60_000
                doOutput = true
            }
            connection.outputStream.use { output ->
                output.write(json.encodeToString(JsonObject.serializer(), body).toByteArray())
            }
            val status = connection.responseCode
            val response = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = response?.bufferedReader()?.use { it.readText() }.orEmpty()
            when {
                status == 401 -> throw OpenAIError.Unauthorized
                status in 400..499 -> throw OpenAIError.Client(responseText.ifBlank { "OpenAI rejected the request." })
                status >= 500 -> throw OpenAIError.Server
            }
            val envelope = json.decodeFromString<ChatResponse>(responseText)
            envelope.choices.firstOrNull()?.message?.content ?: throw OpenAIError.Decoding
        }.getOrElse { error ->
            when (error) {
                is OpenAIError -> throw error
                else -> throw OpenAIError.Transport(error)
            }
        }
    }

    private fun requestBody(
        model: String,
        systemPrompt: String,
        userContent: kotlinx.serialization.json.JsonElement,
        schemaName: String,
        schema: JsonObject,
    ) = buildJsonObject {
        put("model", model)
        putJsonArray("messages") {
            add(message("system", JsonPrimitive(systemPrompt)))
            add(message("user", userContent))
        }
        putJsonObject("response_format") {
            put("type", "json_schema")
            putJsonObject("json_schema") {
                put("name", schemaName)
                put("strict", true)
                put("schema", schema)
            }
        }
    }

    private fun message(role: String, content: kotlinx.serialization.json.JsonElement) = buildJsonObject {
        put("role", role)
        put("content", content)
    }

    private fun recipeSchema() = buildJsonObject {
        put("type", "object")
        put("additionalProperties", false)
        put("required", JsonArray(listOf(JsonPrimitive("recipes"))))
        putJsonObject("properties") {
            putJsonObject("recipes") {
                put("type", "array")
                put("minItems", 3)
                put("maxItems", 3)
                put("items", recipeItemSchema())
            }
        }
    }

    private fun recipeItemSchema() = buildJsonObject {
        put("type", "object")
        put("additionalProperties", false)
        put("required", JsonArray(listOf("title", "description", "ingredients", "steps", "estimatedTime").map(::JsonPrimitive)))
        putJsonObject("properties") {
            listOf("title", "description", "estimatedTime").forEach { key -> putJsonObject(key) { put("type", "string") } }
            listOf("ingredients", "steps").forEach { key ->
                putJsonObject(key) {
                    put("type", "array")
                    putJsonObject("items") { put("type", "string") }
                }
            }
        }
    }

    private fun dailyPicksSchema() = buildJsonObject {
        put("type", "object")
        put("additionalProperties", false)
        put("required", JsonArray(listOf("breakfast", "lunch", "dinner").map(::JsonPrimitive)))
        putJsonObject("properties") {
            listOf("breakfast", "lunch", "dinner").forEach { key -> putJsonObject(key) { put("type", "string") } }
        }
    }

    private companion object {
        const val ENDPOINT = "https://api.openai.com/v1/chat/completions"
    }
}

@Serializable
private data class ChatResponse(val choices: List<Choice>)

@Serializable
private data class Choice(val message: Message)

@Serializable
private data class Message(val content: String)

@Serializable
private data class RecipeResponse(val recipes: List<Recipe>)

@Serializable
private data class DailyPicksResponse(
    val breakfast: String,
    val lunch: String,
    val dinner: String,
)
