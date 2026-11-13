package com.example.araspoint_map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityValidationTest {

    @Test
    fun testEmailDomainValidation() {
        val validStudentEmail = "student.name@g.batstate-u.edu.ph"
        val validGmail = "admin@gmail.com"
        val invalidDomainEmail = "user@maliciousdomain.com"
        val sqlInjectionEmail = "' OR 1=1; --@gmail.com"

        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@(g\\.batstate-u\\.edu\\.ph|gmail\\.com)$")

        assertTrue(emailRegex.matches(validStudentEmail))
        assertTrue(emailRegex.matches(validGmail))
        assertFalse(emailRegex.matches(invalidDomainEmail))
        assertFalse(emailRegex.matches(sqlInjectionEmail))
    }

    @Test
    fun testPasswordLengthSecurity() {
        val weakPassword = "123"
        val securePassword = "RootAdminNasugbu99!"

        assertTrue(weakPassword.length < 6)
        assertTrue(securePassword.length >= 6)
        assertTrue(securePassword.any { it.isDigit() } && securePassword.any { !it.isLetterOrDigit() })
    }
}
