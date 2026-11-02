package com.example.fin_ai.data

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException

/**
 * Turns a failed API call into something worth showing a user: the API's own
 * `{ "error": "..." }` message when there is one, instead of a bare "HTTP 422".
 */
fun Throwable.toUserMessage(fallback: String): String = when (this) {
    is HttpException -> runCatching {
        val body = response()?.errorBody()?.string().orEmpty()
        JsonParser.parseString(body).asJsonObject.get("error").asString
    }.getOrNull() ?: fallback
    is IOException -> "Can't reach the server. Check your connection and try again."
    else -> message ?: fallback
}
