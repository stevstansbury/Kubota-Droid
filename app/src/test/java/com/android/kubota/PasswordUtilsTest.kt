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
    fun testContainsAlphaCharacter() {
        assertFalse(PasswordUtils.containsAlphaCharacter(""))
        assert(PasswordUtils.containsAlphaCharacter("faskfJdhkjsh32401498"))
        assert(PasswordUtils.containsAlphaCharacter("dsfAsjfhskfj"))
        assertFalse(PasswordUtils.containsAlphaCharacter("1234567"))

        assert(PasswordUtils.containsAlphaCharacter("fask#Jdhkjsh32401498"))
        assert(PasswordUtils.containsAlphaCharacter("dsfAsjfh^skfj"))
        assertFalse(PasswordUtils.containsAlphaCharacter("1234=+-_567"))

        assertFalse(PasswordUtils.containsAlphaCharacter(" "))
        assertFalse(PasswordUtils.containsAlphaCharacter("[]"))
        assertFalse(PasswordUtils.containsAlphaCharacter("""\"""))
    }

    @Test
    fun testContainsNumericCharacter() {
        assertFalse(PasswordUtils.containsNumericCharacter(""))
        assert(PasswordUtils.containsNumericCharacter("faskfJdhkjsh32401498"))
        assertFalse(PasswordUtils.containsNumericCharacter("dsfAsjfhskfj"))
        assert(PasswordUtils.containsNumericCharacter("1234567"))

        assert(PasswordUtils.containsNumericCharacter("fask#Jdhkjsh32401498"))
        assertFalse(PasswordUtils.containsNumericCharacter("dsfAsjfh^skfj"))
        assert(PasswordUtils.containsNumericCharacter("1234=+-_567"))

        assertFalse(PasswordUtils.containsNumericCharacter(" "))
        assertFalse(PasswordUtils.containsNumericCharacter("[]"))
        assertFalse(PasswordUtils.containsNumericCharacter("""\"""))
    }

    @Test
    fun testContainsASymbol() {
        assertFalse(PasswordUtils.containsASymbol(""))
        assertFalse(PasswordUtils.containsASymbol("faskfJdhkjsh32401498"))
        assertFalse(PasswordUtils.containsASymbol("dsfAsjfhskfj"))
        assertFalse(PasswordUtils.containsASymbol("1234567"))

        assert(PasswordUtils.containsASymbol("fask#Jdhkjsh32401498"))
        assert(PasswordUtils.containsASymbol("dsfAsjfh^skfj"))
        assert(PasswordUtils.containsASymbol("1234=+-_567"))

        assertFalse(PasswordUtils.containsASymbol(" "))
        assertFalse(PasswordUtils.containsASymbol("[]"))
        assert(PasswordUtils.containsASymbol("[^*.,]"))
        assert(PasswordUtils.containsASymbol("""\"""))
    }

    @Test
    fun testIsValidPassword() {
        assertFalse(PasswordUtils.isValidPassword(""))
        assertFalse(PasswordUtils.isValidPassword("faskfJdhkjsh32401498"))
        assert(PasswordUtils.isValidPassword("fakfJdhkjsh3240!498"))
        assertFalse(PasswordUtils.isValidPassword("dsfAsjfhskfj"))
        assertFalse(PasswordUtils.isValidPassword("1234567"))

        assert(PasswordUtils.isValidPassword("fask#Jdhkjsh32401498"))
        assert(PasswordUtils.isValidPassword("dsf1sjfh^skfj"))
        assertFalse(PasswordUtils.isValidPassword("1234=+-_567"))

        assertFalse(PasswordUtils.isValidPassword(" "))
        assertFalse(PasswordUtils.isValidPassword("[]"))
        assertFalse(PasswordUtils.isValidPassword("""\"""))
    }
}