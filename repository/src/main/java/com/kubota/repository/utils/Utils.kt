package com.kubota.repository.utils

import com.kubota.network.Constants.Companion.BASE_URL

class Utils {
    companion object {
        private const val TERMS_OF_USE_PATH = "TermsOfUse"
        private const val PRIVACY_POLICY_PATH = "PrivacyPolicy"

        fun getTermsOfUseUrl() = "$BASE_URL$TERMS_OF_USE_PATH"

        fun getPrivacyPolicyUrl() = "$BASE_URL$PRIVACY_POLICY_PATH"
    }
}