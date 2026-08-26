package com.example.blueprintai.model

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolInterceptor @Inject constructor(
    private val httpClient: HttpClient
) {
    suspend fun intercept(toolCallJson: String): String {
        return try {
            val element = Json.parseToJsonElement(toolCallJson)
            val name = element.jsonObject["name"]?.jsonPrimitive?.content ?: return "Error: Tool name missing"
            val args = element.jsonObject["args"]?.jsonObject ?: JsonObject(emptyMap())

            when (name) {
                "search_web" -> handleSearchWeb(args)
                "read_webpage" -> handleReadWebpage(args)
                else -> "Error: Unknown tool $name"
            }
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    private suspend fun handleSearchWeb(args: JsonObject): String {
        val query = args["query"]?.jsonPrimitive?.content ?: return "Error: Query missing"
        // Mocking search for now, or using a simple provider if URL is known.
        // In a real app, this would call SearXNG or similar.
        return "Search results for '$query': [Result 1, Result 2...]" 
    }

    private suspend fun handleReadWebpage(args: JsonObject): String {
        val url = args["url"]?.jsonPrimitive?.content ?: return "Error: URL missing"
        return try {
            val response: HttpResponse = httpClient.get(url)
            val body = response.bodyAsText()
            // Simple HTML tag removal and whitespace cleanup
            val plainText = body.replace(Regex("<script.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
                .replace(Regex("<style.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
                .replace(Regex("<[^>]*>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            plainText.take(2500) // Truncate for the model
        } catch (e: Exception) {
            "Error reading $url: ${e.localizedMessage}"
        }
    }
}
