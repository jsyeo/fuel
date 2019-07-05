package com.github.kittinunf.fuel

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.test.MockHttpTestCase
import com.github.kittinunf.fuel.util.encodeBase64ToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RequestAuthenticationTest : MockHttpTestCase() {
    private val user: String = "username"
    private val password: String = "password"

    @Test
    fun httpBasicAuthenticationWithInvalidCase() = runBlocking {
        val manager = FuelManager()
        val auth = "$user:$password"
        val encodedAuth = auth.encodeBase64ToString()

        val correctRequest = mock.request()
            .withMethod(Method.GET.value)
            .withHeader("Authorization", "Basic $encodedAuth")
            .withPath("/authenticate")

        val incorrectRequest = mock.request()
            .withMethod(Method.GET.value)
            .withHeader("Authorization")
            .withPath("/authenticate")

        val correctResponse = mock.reflect()
        val incorrectResponse = mock.response().withStatusCode(HttpURLConnection.HTTP_UNAUTHORIZED)

        mock.chain(request = correctRequest, response = correctResponse)
        mock.chain(request = incorrectRequest, response = incorrectResponse)

        val (response, result) = withContext(Dispatchers.IO) {
            manager.get(mock.path("authenticate"))
                    .authentication()
                    .basic("invalid", "authentication")
                    .awaitByteArrayResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNotNull(error)
        assertNull(data)

        val statusCode = HttpURLConnection.HTTP_UNAUTHORIZED
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpBasicAuthenticationWithValidCase() = runBlocking {
        val manager = FuelManager()
        val auth = "$user:$password"
        val encodedAuth = auth.encodeBase64ToString()

        val correctRequest = mock.request()
            .withMethod(Method.GET.value)
            .withHeader("Authorization", "Basic $encodedAuth")
            .withPath("/authenticate")

        val incorrectRequest = mock.request()
            .withMethod(Method.GET.value)
            .withHeader("Authorization")
            .withPath("/authenticate")

        val correctResponse = mock.reflect()
        val incorrectResponse = mock.response().withStatusCode(HttpURLConnection.HTTP_UNAUTHORIZED)

        mock.chain(request = correctRequest, response = correctResponse)
        mock.chain(request = incorrectRequest, response = incorrectResponse)

        val (response, result) = withContext(Dispatchers.IO) {
            manager.get(mock.path("authenticate"))
                    .authentication()
                    .basic(user, password)
                    .awaitByteArrayResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }
}
