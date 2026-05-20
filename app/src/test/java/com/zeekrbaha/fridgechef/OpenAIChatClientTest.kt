package com.zeekrbaha.fridgechef

import com.zeekrbaha.fridgechef.network.APIKeyProvider
import com.zeekrbaha.fridgechef.network.OpenAIChatClient
import com.zeekrbaha.fridgechef.network.OpenAIError
import com.zeekrbaha.fridgechef.data.MealType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OpenAIChatClientTest {
    private val server = MockWebServer()
    private lateinit var client: OpenAIChatClient

    @Before fun setUp() {
        server.start()
        client = OpenAIChatClient(KeyProvider("test-key"), server.url("/v1/chat/completions").toString())
    }

    @After fun tearDown() {
        server.shutdown()
    }

    private class KeyProvider(private val key: String) : APIKeyProvider {
        override fun apiKey() = key
    }

    private fun chatResponse(content: String): String {
        val escapedContent = Json.encodeToString(content)
        return """{"choices":[{"message":{"content":$escapedContent}}]}"""
    }

    private fun recipesJson(title: String = "Test Recipe") =
        """{"recipes":[{"title":"$title","description":"d","ingredients":["a"],"steps":["s"],"estimatedTime":"5m"}]}"""

    private fun dailyPicksJson() =
        """{"breakfast":"oats","lunch":"salad","dinner":"pasta"}"""

    private fun enqueueRecipes(title: String = "Test Recipe") {
        server.enqueue(MockResponse().setResponseCode(200).setBody(chatResponse(recipesJson(title))))
    }

    private fun requestBody(): String = server.takeRequest().body.readUtf8()

    // --- request shape ---

    @Test fun ingredientsRequest_usesGpt4oModel() = runBlocking {
        enqueueRecipes()
        client.suggestRecipes(listOf("eggs", "cheese"))
        val body = Json.parseToJsonElement(requestBody()).jsonObject
        assertEquals("gpt-4o", body["model"]!!.jsonPrimitive.content)
    }

    @Test fun ingredientsRequest_hasAuthorizationHeader() = runBlocking {
        enqueueRecipes()
        client.suggestRecipes(listOf("eggs"))
        val req = server.takeRequest()
        assertEquals("Bearer test-key", req.getHeader("Authorization"))
    }

    @Test fun ingredientsRequest_usesJsonSchemaResponseFormat() = runBlocking {
        enqueueRecipes()
        client.suggestRecipes(listOf("eggs"))
        val body = Json.parseToJsonElement(requestBody()).jsonObject
        val responseFormat = body["response_format"]!!.jsonObject
        assertEquals("json_schema", responseFormat["type"]!!.jsonPrimitive.content)
        val schema = responseFormat["json_schema"]!!.jsonObject
        assertEquals("recipes", schema["name"]!!.jsonPrimitive.content)
    }

    @Test fun ingredientsRequest_decodesRecipesFromResponse() = runBlocking {
        enqueueRecipes("Pasta Delight")
        val recipes = client.suggestRecipes(listOf("pasta", "tomato"))
        assertEquals(1, recipes.size)
        assertEquals("Pasta Delight", recipes.first().title)
    }

    @Test fun imageRequest_encodesBase64InUserMessage() = runBlocking {
        enqueueRecipes()
        client.suggestRecipes(byteArrayOf(1, 2, 3))
        val body = Json.parseToJsonElement(requestBody()).jsonObject
        val messages = body["messages"]!!.jsonArray
        val userMsg = messages[1].jsonObject
        val contentArray = userMsg["content"]!!.jsonArray
        val imageEntry = contentArray.first { it.jsonObject["type"]!!.jsonPrimitive.content == "image_url" }
        val url = imageEntry.jsonObject["image_url"]!!.jsonObject["url"]!!.jsonPrimitive.content
        assertTrue(url.startsWith("data:image/jpeg;base64,"))
    }

    @Test fun dishNameRequest_sendsNameAsUserContent() = runBlocking {
        enqueueRecipes()
        client.suggestRecipes("Spaghetti")
        val body = Json.parseToJsonElement(requestBody()).jsonObject
        val messages = body["messages"]!!.jsonArray
        val userContent = messages[1].jsonObject["content"]!!.jsonPrimitive.content
        assertEquals("Spaghetti", userContent)
    }

    @Test fun mealRequest_systemPromptContainsMealName() = runBlocking {
        enqueueRecipes()
        client.suggestRecipes(MealType.Breakfast)
        val body = Json.parseToJsonElement(requestBody()).jsonObject
        val messages = body["messages"]!!.jsonArray
        val systemContent = messages[0].jsonObject["content"]!!.jsonPrimitive.content
        assertTrue(systemContent.contains("breakfast"))
    }

    @Test fun dailyPicksRequest_usesGpt4oMini() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(chatResponse(dailyPicksJson())))
        client.dailyPicks()
        val body = Json.parseToJsonElement(requestBody()).jsonObject
        assertEquals("gpt-4o-mini", body["model"]!!.jsonPrimitive.content)
    }

    @Test fun dailyPicksRequest_decodesBreakfastLunchDinner() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(chatResponse(dailyPicksJson())))
        val picks = client.dailyPicks()
        assertEquals("oats", picks.breakfast)
        assertEquals("salad", picks.lunch)
        assertEquals("pasta", picks.dinner)
    }

    // --- error mapping ---

    @Test fun blankApiKey_throwsUnauthorized_withoutRequest() = runBlocking<Unit> {
        val blankClient = OpenAIChatClient(KeyProvider(""), server.url("/v1/chat/completions").toString())
        var caught: Exception? = null
        try {
            blankClient.suggestRecipes(listOf("eggs"))
        } catch (e: OpenAIError.Unauthorized) {
            caught = e
        }
        assertNotNull(caught)
        assertEquals(0, server.requestCount)
    }

    @Test fun http401_throwsUnauthorized() = runBlocking<Unit> {
        server.enqueue(MockResponse().setResponseCode(401))
        var caught: Exception? = null
        try {
            client.suggestRecipes(listOf("eggs"))
        } catch (e: OpenAIError.Unauthorized) {
            caught = e
        }
        assertNotNull(caught)
    }

    @Test fun http400_throwsClientError() = runBlocking<Unit> {
        server.enqueue(MockResponse().setResponseCode(400).setBody("bad request"))
        var caught: Exception? = null
        try {
            client.suggestRecipes(listOf("eggs"))
        } catch (e: OpenAIError.Client) {
            caught = e
        }
        assertNotNull(caught)
    }

    @Test fun http500_throwsServer() = runBlocking<Unit> {
        server.enqueue(MockResponse().setResponseCode(500))
        var caught: Exception? = null
        try {
            client.suggestRecipes(listOf("eggs"))
        } catch (e: OpenAIError.Server) {
            caught = e
        }
        assertNotNull(caught)
    }

    @Test fun invalidJsonInContent_throwsDecoding() = runBlocking<Unit> {
        server.enqueue(MockResponse().setResponseCode(200).setBody(chatResponse("not valid json")))
        var caught: Exception? = null
        try {
            client.suggestRecipes(listOf("eggs"))
        } catch (e: OpenAIError.Decoding) {
            caught = e
        }
        assertNotNull(caught)
    }
}
