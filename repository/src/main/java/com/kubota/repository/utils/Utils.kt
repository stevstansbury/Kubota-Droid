package com.kubota.repository.utils

import com.google.gson.GsonBuilder
import com.kubota.network.Constants.Companion.BASE_URL
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Utils {
    companion object {
        private const val TERMS_OF_USE_PATH = "TermsOfUse"
        private const val PRIVACY_POLICY_PATH = "PrivacyPolicy"

        fun getTermsOfUseUrl() = "$BASE_URL$TERMS_OF_USE_PATH"

        fun getPrivacyPolicyUrl() = "$BASE_URL$PRIVACY_POLICY_PATH"

        private fun getHttpClient() = OkHttpClient.Builder().cache(CacheUtils.getCacheInstance()).build()

        private fun getGsonConverterFactory() =  GsonConverterFactory.create(GsonBuilder().setLenient().create())

        fun getRetrofit() = Retrofit.Builder().baseUrl(BASE_URL).client(getHttpClient()).addConverterFactory(getGsonConverterFactory()).build()
    }
}