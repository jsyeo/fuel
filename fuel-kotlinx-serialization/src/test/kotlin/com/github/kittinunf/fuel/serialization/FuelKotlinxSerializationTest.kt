package com.github.kittinunf.fuel.serialization

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.test.MockHttpTestCase
import com.github.kittinunf.result.Result
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.net.HttpURLConnection

@ImplicitReflectionSerializer
class FuelKotlinxSerializationTest : MockHttpTestCase() {
    @Test
    fun `one issues with non null and not parse null on field`() = runBlocking {
        mock.chain(
            request = mock.request().withPath("/issues/1"),
            response = mock.response().withBody(
                    "{ \"id\": 1, \"title\": \"issue 1\", \"number\": null }"
            ).withStatusCode(HttpURLConnection.HTTP_OK)
        )

        val (res, result) = withContext(Dispatchers.IO) {
           Fuel.get(mock.path("issues/1")).awaitResponseResultObject<IssueInfo>()
        }
        assertNotNull(res)
        assertNotNull(result)
        assertNotNull(result.component2())
        val success = when (result) {
            is Result.Success -> true
            is Result.Failure -> false
        }
        assertFalse("should not parse null into non-null field", success)
    }

    @Test
    fun `one issues with 404`() = runBlocking {
        mock.chain(
            request = mock.request().withPath("/issues/1"),
            response = mock.response().withStatusCode(HttpURLConnection.HTTP_NOT_FOUND)
        )
        val (res, result) = withContext(Dispatchers.IO) {
           Fuel.get(mock.path("issues/1")).awaitResponseResultObject<IssueInfo>()
        }
        assertNotNull(res)
        assertNotNull(result)
        val (value, error) = result
        assertNull(value)
        assertNotNull(error)
        assertEquals((error as FuelError).response.statusCode, HttpURLConnection.HTTP_NOT_FOUND)
    }

    @Serializable
    data class IssueInfo(val id: Int, val title: String, val number: Int)
}