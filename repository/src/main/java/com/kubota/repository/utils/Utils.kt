package com.kubota.repository.utils

import com.kubota.network.Constants.BASE_URL

class Utils {
    companion object {
        private const val TERMS_OF_USE_PATH = "TermsOfUse"
        private const val PRIVACY_POLICY_PATH = "PrivacyPolicy"

        fun getTermsOfUseUrl() = "$BASE_URL/api/$TERMS_OF_USE_PATH"

        fun getPrivacyPolicyUrl() = "$BASE_URL/api/$PRIVACY_POLICY_PATH"
    }
}