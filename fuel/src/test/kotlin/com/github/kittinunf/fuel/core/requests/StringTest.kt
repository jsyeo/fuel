package com.github.kittinunf.fuel.core.requests

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.awaitStringResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StringTest : MockHttpTestCase() {

    private fun randomString() = UUID.randomUUID().toString()
    private fun getString(string: String, path: String = "string"): Request {
        mock.chain(
            request = mock.request().withPath("/$path"),
            response = mock.response().withBody(string)
        )

        return Fuel.request(Method.GET, mock.path(path))
    }

    private fun mocked404(method: Method = Method.GET, path: String = "invalid/url"): Request {
        mock.chain(
            request = mock.request().withPath("/$path"),
            response = mock.response().withStatusCode(HttpURLConnection.HTTP_NOT_FOUND)
        )
        return Fuel.request(method, mock.path(path))
    }

    @Test
    fun `checking random string with not null`() = runBlocking {
        val string = randomString()
        val (_, result) = withContext(Dispatchers.IO) {
            getString(string).awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull("Expected data, actual error $error", data)
        assertEquals(data, string)
    }

    @Test
    fun `checking 404 on String`() = runBlocking {
        val (_, result) = withContext(Dispatchers.IO) {
            mocked404().awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull("Error Message", error?.localizedMessage)
        assertNull(data)
    }
}
