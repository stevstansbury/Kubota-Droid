package com.android.kubota.utility

import androidx.annotation.VisibleForTesting

object PasswordUtils {
    private const val SPECIAL_CHARACTERS = """~!@#$%^&*()_\[+\]`;'\\/.,{}|":<>?=*\-+"""

    fun hasAtLeast8Characters(password: String) = password.length >= 8


    fun hasUpperCaseLetter(password: String) = "(?=.*[A-Z])".toRegex().containsMatchIn(password)


    fun hasLowerCaseLetter(password: String) = "(?=.*[a-z])".toRegex().containsMatchIn(password)


    fun hasNumberOrSpecialCharacter(password: String): Boolean {
        return hasANumber(password) || hasASpecialCharacter(password)
    }

    fun isValidPassword(password: String): Boolean {
        return hasAtLeast8Characters(password) && hasLowerCaseLetter(password) &&
                hasUpperCaseLetter(password) && hasNumberOrSpecialCharacter(password) &&
                !containsInvalidCharacters(password)
    }

    fun containsInvalidCharacters(password: String): Boolean {
        return """(?=.*[^\w${SPECIAL_CHARACTERS}])""".toRegex().containsMatchIn(password)
    }

    @VisibleForTesting
    fun hasANumber(password: String) = "(?=.*[0-9])".toRegex().containsMatchIn(password)


    @VisibleForTesting
    fun hasASpecialCharacter(password: String): Boolean {
        return """(?=.*[${SPECIAL_CHARACTERS}])""".toRegex().containsMatchIn(password)
    }

}