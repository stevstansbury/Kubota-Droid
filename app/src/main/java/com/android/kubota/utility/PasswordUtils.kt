package com.android.kubota.utility

object PasswordUtils {
    fun hasAtLeast8Characters(password: String) = password.length >= 8

    fun containsAlphaCharacter(password: String) = password.matches(".*[A-Za-z].*".toRegex())

    fun containsNumericCharacter(password: String) = password.matches(".*[0-9].*".toRegex())

    fun containsASymbol(password: String) = password.matches(".*[-+_!@#\\\\$%^&*.,?].*".toRegex())

    fun isValidPassword(password: String): Boolean {
        return hasAtLeast8Characters(password) && containsAlphaCharacter(password) &&
                containsNumericCharacter(password) && containsASymbol(password)
    }
}