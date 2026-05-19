package com.zeekrbaha.fridgechef.network

sealed class OpenAIError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data object Unauthorized : OpenAIError("Check your OpenAI API key.")
    data class Client(val detail: String) : OpenAIError(detail)
    data object Server : OpenAIError("OpenAI is temporarily unavailable.")
    data object Decoding : OpenAIError("Could not read the recipe response.")
    data class Transport(val underlying: Throwable) : OpenAIError("Network request failed.", underlying)
}
