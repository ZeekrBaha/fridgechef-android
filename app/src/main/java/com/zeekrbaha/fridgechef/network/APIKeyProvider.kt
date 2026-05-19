package com.zeekrbaha.fridgechef.network

import com.zeekrbaha.fridgechef.BuildConfig

interface APIKeyProvider {
    fun apiKey(): String
}

class BuildConfigAPIKeyProvider : APIKeyProvider {
    override fun apiKey(): String = BuildConfig.OPENAI_API_KEY
}
