package com.android.kubota

import com.android.kubota.utility.PasswordUtils
import org.junit.Assert.assertFalse
import org.junit.Test

class PasswordUtilsTest {

    @Test
    fun testHasAtLeast8Characters() {
        assertFalse(PasswordUtils.hasAtLeast8Characters(""))
        assert(PasswordUtils.hasAtLeast8Characters("faskfJdhkjsh32401498"))
        assert(PasswordUtils.hasAtLeast8Characters("dsfAsjfhskfj"))
        assertFalse(PasswordUtils.hasAtLeast8Characters("1234567"))

        assert(PasswordUtils.hasAtLeast8Characters("fask#Jdhkjsh32401498"))
        assert(PasswordUtils.hasAtLeast8Characters("dsfAsjfh^skfj"))
        assert(PasswordUtils.hasAtLeast8Characters("1234=+-_567"))

        assertFalse(PasswordUtils.hasAtLeast8Characters(" "))
        assertFalse(PasswordUtils.hasAtLeast8Characters("[]"))
        assertFalse(PasswordUtils.hasAtLeast8Characters("""\"""))
    }

    @Test
    fun testHasUpperCaseLetter() {
        assertFalse(PasswordUtils.hasUpperCaseLetter(""))
        assert(PasswordUtils.hasUpperCaseLetter("faskfJdhkjsh32401498"))
        assert(PasswordUtils.hasUpperCaseLetter("dsfAsjfhskfj"))
        assertFalse(PasswordUtils.hasUpperCaseLetter("1234567"))

        assert(PasswordUtils.hasUpperCaseLetter("fask#Jdhkjsh32401498"))
        assert(PasswordUtils.hasUpperCaseLetter("dsfAsjfh^skfj"))
        assertFalse(PasswordUtils.hasUpperCaseLetter("1234=+-_567"))

        assertFalse(PasswordUtils.hasUpperCaseLetter(" "))
        assertFalse(PasswordUtils.hasUpperCaseLetter("[]"))
        assertFalse(PasswordUtils.hasUpperCaseLetter("""\"""))
    }

    @Test
    fun testHasLowerCaseLetter() {
        assertFalse(PasswordUtils.hasLowerCaseLetter(""))
        assert(PasswordUtils.hasLowerCaseLetter("faskfJdhkjsh32401498"))
        assert(PasswordUtils.hasLowerCaseLetter("dsfAsjfhskfj"))
        assertFalse(PasswordUtils.hasLowerCaseLetter("1234567"))

        assert(PasswordUtils.hasLowerCaseLetter("fask#Jdhkjsh32401498"))
        assert(PasswordUtils.hasLowerCaseLetter("dsfAsjfh^skfj"))
        assertFalse(PasswordUtils.hasLowerCaseLetter("1234=+-_567"))

        assertFalse(PasswordUtils.hasLowerCaseLetter(" "))
        assertFalse(PasswordUtils.hasLowerCaseLetter("[]"))
        assertFalse(PasswordUtils.hasLowerCaseLetter("""\"""))
    }

    @Test
    fun testHasNumber() {
        assertFalse(PasswordUtils.hasANumber(""))
        assert(PasswordUtils.hasANumber("faskfJdhkjsh32401498"))
        assertFalse(PasswordUtils.hasANumber("dsfAsjfhskfj"))
        assert(PasswordUtils.hasANumber("1234567"))

        assert(PasswordUtils.hasANumber("fask#Jdhkjsh32401498"))
        assertFalse(PasswordUtils.hasANumber("dsfAsjfh^skfj"))
        assert(PasswordUtils.hasANumber("1234=+-_567"))

        assertFalse(PasswordUtils.hasANumber(" "))
        assertFalse(PasswordUtils.hasANumber("[]"))
        assertFalse(PasswordUtils.hasANumber("""\"""))
    }

    @Test
    fun testHasASpecialCharacter() {
        assertFalse(PasswordUtils.hasASpecialCharacter(""))
        assertFalse(PasswordUtils.hasASpecialCharacter("faskfJdhkjsh32401498"))
        assertFalse(PasswordUtils.hasASpecialCharacter("dsfAsjfhskfj"))
        assertFalse(PasswordUtils.hasASpecialCharacter("1234567"))

        assert(PasswordUtils.hasASpecialCharacter("fask#Jdhkjsh32401498"))
        assert(PasswordUtils.hasASpecialCharacter("dsfAsjfh^skfj"))
        assert(PasswordUtils.hasASpecialCharacter("1234=+-_567"))

        assertFalse(PasswordUtils.hasASpecialCharacter(" "))
        assert(PasswordUtils.hasASpecialCharacter("[]"))
        assert(PasswordUtils.hasASpecialCharacter("""\"""))
    }

    @Test
    fun testContainsInvalidCharacters() {
        assertFalse(PasswordUtils.containsInvalidCharacters(""))
        assertFalse(PasswordUtils.containsInvalidCharacters("faskfJdhkjsh32401498"))
        assertFalse(PasswordUtils.containsInvalidCharacters("dsfAsjfhskfj"))
        assertFalse(PasswordUtils.containsInvalidCharacters("1234567"))

        assertFalse(PasswordUtils.containsInvalidCharacters("fask#Jdhkjsh32401498"))
        assertFalse(PasswordUtils.containsInvalidCharacters("dsfAsjfh^skfj"))
        assertFalse(PasswordUtils.containsInvalidCharacters("1234=+-_567"))

        assert(PasswordUtils.containsInvalidCharacters(" "))
        assertFalse(PasswordUtils.containsInvalidCharacters("[]"))
        assertFalse(PasswordUtils.containsInvalidCharacters("""\"""))
    }

    @Test
    fun testIsValidPassword() {
        assertFalse(PasswordUtils.isValidPassword(""))
        assert(PasswordUtils.isValidPassword("faskfJdhkjsh32401498"))
        assertFalse(PasswordUtils.isValidPassword("dsfAsjfhskfj"))
        assertFalse(PasswordUtils.isValidPassword("1234567"))

        assert(PasswordUtils.isValidPassword("fask#Jdhkjsh32401498"))
        assert(PasswordUtils.isValidPassword("dsfAsjfh^skfj"))
        assertFalse(PasswordUtils.isValidPassword("1234=+-_567"))

        assertFalse(PasswordUtils.isValidPassword(" "))
        assertFalse(PasswordUtils.isValidPassword("[]"))
        assertFalse(PasswordUtils.isValidPassword("""\"""))
    }
}