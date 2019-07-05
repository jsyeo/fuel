package com.github.kittinunf.fuel.core.requests

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ByteArrayTest : MockHttpTestCase() {

    @Test
    fun `checking response for not null with random bytes`() = runBlocking {
        val bytes = ByteArray(255).also { Random().nextBytes(it) }
        mock.chain(
            request = mock.request().withPath("/bytes"),
            response = mock.response().withBody(bytes)
        )

        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.request(Method.GET, mock.path("bytes")).awaitByteArrayResponseResult()
        }
        val (data, error) = result
        assertNotNull(response, "Expected response to be not null")
        assertNotNull(data, "Expected data, actual error $error")

        assertEquals(data, bytes)
    }

    @Test
    fun `checking failed responses`() = runBlocking {
        mock.chain(
            request = mock.request().withPath("/invalid/url"),
            response = mock.response().withStatusCode(HttpURLConnection.HTTP_NOT_FOUND)
        )

        val (_, result) = withContext(Dispatchers.IO) {
            Fuel.request(Method.GET, mock.path("invalid/url")).awaitByteArrayResponseResult()
        }
        val (data, error) = result
        assertNotNull(error)
        assertEquals(data, ByteArray(0))
    }
}
