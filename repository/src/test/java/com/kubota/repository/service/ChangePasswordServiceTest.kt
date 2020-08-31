package com.kubota.repository.service

import com.kubota.network.service.NetworkResponse
import org.junit.Assert.*
import org.junit.Test

class ChangePasswordServiceTest {
    private val service = ChangePasswordService()

    @Test
    fun testParseResponse() {
        assertTrue(service.parseResponse(NetworkResponse.Success(Unit)) is Result.Success)
        assertFalse(service.parseResponse(NetworkResponse.ServerError(500, "")) is Result.Success)
        assertFalse(service.parseResponse(NetworkResponse.IOException("")) is Result.Success)

        assertTrue(service.parseResponse(NetworkResponse.ServerError(500, MISSING_CODE)) is Result.Error)
        assertFalse(service.parseResponse(NetworkResponse.ServerError(400, MISSING_CODE)) is Result.Error)
        assertFalse(service.parseResponse(NetworkResponse.ServerError(400, "")) is Result.Error)
    }

    @Test
    fun testParseResponse_InvalidToken(){
        assertTrue(service.parseResponse(NetworkResponse.ServerError(400, INVALID_TOKEN)) is Result.InvalidToken)
        assertTrue(service.parseResponse(NetworkResponse.ServerError(400, MISSING_TOKEN)) is Result.InvalidToken)

        assertTrue(service.parseResponse(NetworkResponse.ServerError(400, INVALID_TOKEN.toLowerCase())) is Result.InvalidToken)
        assertTrue(service.parseResponse(NetworkResponse.ServerError(400, INVALID_TOKEN.toUpperCase())) is Result.InvalidToken)

        assertTrue(service.parseResponse(NetworkResponse.ServerError(400, MISSING_TOKEN.toLowerCase())) is Result.InvalidToken)
        assertTrue(service.parseResponse(NetworkResponse.ServerError(400, MISSING_TOKEN.toUpperCase())) is Result.InvalidToken)

        assertTrue(service.parseResponse(NetworkResponse.ServerError(400," $INVALID_CODE $MISSING_TOKEN ")) is Result.InvalidToken)
        assertTrue(service.parseResponse(NetworkResponse.ServerError(400," $INVALID_CODE $MISSING_TOKEN ")) is Result.InvalidToken)
        assertTrue(service.parseResponse(NetworkResponse.ServerError(400," $INVALID_CODE $INVALID_TOKEN ")) is Result.InvalidToken)
        assertTrue(service.parseResponse(NetworkResponse.ServerError(400," $INVALID_CODE $INVALID_TOKEN ")) is Result.InvalidToken)

        assertFalse(service.parseResponse(NetworkResponse.ServerError(401, INVALID_TOKEN)) is Result.InvalidToken)
        assertFalse(service.parseResponse(NetworkResponse.ServerError(404, MISSING_TOKEN)) is Result.InvalidToken)
        assertFalse(service.parseResponse(NetworkResponse.ServerError(500, INVALID_TOKEN)) is Result.InvalidToken)
        assertFalse(service.parseResponse(NetworkResponse.Success(Unit)) is Result.InvalidToken)
        assertFalse(service.parseResponse(NetworkResponse.IOException(MISSING_CODE)) is Result.InvalidToken)
    }

    @Test
    fun testParseResponse_InvalidCode(){
        assertTrue(service.parseResponse(NetworkResponse.ServerError(400, INVALID_CODE)) is Result.InvalidCode)
        assertTrue(service.parseResponse(NetworkResponse.ServerError(400, MISSING_CODE)) is Result.InvalidCode)

        assertTrue(service.parseResponse(NetworkResponse.ServerError(400, INVALID_CODE.toLowerCase())) is Result.InvalidCode)
        assertTrue(service.parseResponse(NetworkResponse.ServerError(400, INVALID_CODE.toUpperCase())) is Result.InvalidCode)
        assertTrue(service.parseResponse(NetworkResponse.ServerError(400, MISSING_CODE.toLowerCase())) is Result.InvalidCode)
        assertTrue(service.parseResponse(NetworkResponse.ServerError(400, MISSING_CODE.toUpperCase())) is Result.InvalidCode)

        assertFalse(service.parseResponse(NetworkResponse.ServerError(400," $MISSING_CODE $MISSING_TOKEN ")) is Result.InvalidCode)
        assertFalse(service.parseResponse(NetworkResponse.ServerError(400," $MISSING_CODE $MISSING_TOKEN ")) is Result.InvalidCode)
        assertFalse(service.parseResponse(NetworkResponse.ServerError(400," $INVALID_CODE $INVALID_TOKEN ")) is Result.InvalidCode)
        assertFalse(service.parseResponse(NetworkResponse.ServerError(400," $INVALID_CODE $INVALID_TOKEN ")) is Result.InvalidCode)

        assertFalse(service.parseResponse(NetworkResponse.ServerError(401, MISSING_CODE)) is Result.InvalidCode)
        assertFalse(service.parseResponse(NetworkResponse.ServerError(404, MISSING_CODE)) is Result.InvalidCode)
        assertFalse(service.parseResponse(NetworkResponse.ServerError(500, MISSING_CODE)) is Result.InvalidCode)
        assertFalse(service.parseResponse(NetworkResponse.Success(Unit)) is Result.InvalidCode)
        assertFalse(service.parseResponse(NetworkResponse.IOException(MISSING_CODE)) is Result.InvalidCode)
    }

    @Test
    fun testParseResponse_InvalidPassword(){
        assertTrue(service.parseResponse(NetworkResponse.ServerError(400, "")) is Result.InvalidPassword)
        assertTrue(service.parseResponse(NetworkResponse.ServerError(400, "missing symbol")) is Result.InvalidPassword)
        assertTrue(service.parseResponse(NetworkResponse.ServerError(400, "at least 8 characters")) is Result.InvalidPassword)

        assertFalse(service.parseResponse(NetworkResponse.ServerError(401, "")) is Result.InvalidPassword)
        assertFalse(service.parseResponse(NetworkResponse.ServerError(404, "missing symbol")) is Result.InvalidPassword)
        assertFalse(service.parseResponse(NetworkResponse.ServerError(500, "at least 8 characters")) is Result.InvalidPassword)
    }

    @Test
    fun testParseResponse_NetworkErrors(){
        assertTrue(service.parseResponse(NetworkResponse.IOException("")) is Result.NetworkError)

        assertFalse(service.parseResponse(NetworkResponse.ServerError(400, MISSING_CODE)) is Result.NetworkError)
        assertFalse(service.parseResponse(NetworkResponse.ServerError(400, MISSING_TOKEN)) is Result.NetworkError)
        assertFalse(service.parseResponse(NetworkResponse.ServerError(500, "")) is Result.NetworkError)
        assertFalse(service.parseResponse(NetworkResponse.ServerError(500, "")) is Result.NetworkError)
    }
}