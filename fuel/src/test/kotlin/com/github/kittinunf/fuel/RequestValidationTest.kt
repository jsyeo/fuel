package com.github.kittinunf.fuel

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.core.awaitStringResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.isA
import org.junit.Assert.assertThat
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RequestValidationTest : MockHttpTestCase() {
    @Test
    fun httpValidationWithDefaultCase() = runBlocking {
        // Register all valid
        for (status in (200..399)) {
            mock.chain(
                request = mock.request().withMethod(Method.GET.value).withPath("/$status"),
                response = mock.response().withStatusCode(status)
            )
        }

        // Register teapot
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/418"),
            response = mock.response().withStatusCode(418)
        )

        // Register 501
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/501"),
            response = mock.response().withStatusCode(501)
        )

        // Test defaults
        for (status in (200..399)) {
            val (response, result) = withContext(Dispatchers.IO) {
                FuelManager().request(Method.GET, mock.path("$status")).awaitByteArrayResponseResult()
            }
            val (_, error) = result
            assertNotNull(response)
            assertNull(error)

            assertEquals(response.statusCode, status)
        }

        // Test invalid 4xx
        val (response, result) = withContext(Dispatchers.IO) {
            FuelManager().request(Method.GET, mock.path("418")).awaitByteArrayResponseResult()
        }
        val (_, error) = result
        assertNotNull(response)
        assertNotNull(error)
        assertNotNull(error.errorData)

        assertEquals(response.statusCode, 418)

        // Test invalid 5xx
        val (anotherResponse, anotherResult) = withContext(Dispatchers.IO) {
            FuelManager().request(Method.GET, mock.path("501")).awaitByteArrayResponseResult()
        }
        val (_, anotherError) = anotherResult
        assertNotNull(anotherResponse)
        assertNotNull(anotherError)
        assertNotNull(anotherError.errorData)

        assertEquals(anotherResponse.statusCode,501)
    }

    @Test
    fun httpValidationWithCustomValidCase() = runBlocking {
        val preDefinedStatusCode = 203

        val manager = FuelManager()

        // Response to ANY GET request, with a 203 which should have been okay, but it's not with
        // the custom valid case
        mock.chain(
            request = mock.request().withMethod(Method.GET.value),
            response = mock.response().withStatusCode(203)
        )

        // this validate (200..202) which should fail with 203
        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("any"))
                    .validate { it.statusCode in (200..202) }
                    .awaitStringResponseResult()
        }

        val (data, error) = result
        assertNotNull(response)
        assertNotNull(error)
        assertNull(data)

        assertEquals(response.statusCode, preDefinedStatusCode)
    }

    @Test
    fun httpValidationWithCustomInvalidCase() = runBlocking {
        val preDefinedStatusCode = 418
        val manager = FuelManager()

        mock.chain(
            request = mock.request().withMethod(Method.GET.value),
            response = mock.response().withStatusCode(preDefinedStatusCode)
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("status/$preDefinedStatusCode"))
                    .validate { it.statusCode in (400..419) }
                    .awaitByteArrayResponseResult()
        }

        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        assertEquals(response.statusCode, preDefinedStatusCode)
    }

    @Test
    fun httpAnyFailureWithCustomValidator() = runBlocking {
        val manager = FuelManager()

        mock.chain(
                request = mock.request().withMethod(Method.GET.value),
                response = mock.response().withStatusCode(200)
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("any"))
                    .validate { false } // always fail
                    .awaitStringResponseResult()
        }
        val (_, error) = result
        assertNotNull(response)
        assertNotNull(error)
        assertThat(error.exception as HttpException, isA(HttpException::class.java))
        assertThat(error.exception.message, containsString("HTTP Exception 200"))
    }
}
