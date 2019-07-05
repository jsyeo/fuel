package com.github.kittinunf.fuel.jackson

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.test.MockHttpTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.core.Is.isA
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Test
import java.net.HttpURLConnection

class FuelJacksonTest : MockHttpTestCase() {
    @Test
    fun `checking one issues with not null`() = runBlocking {
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
        assertThat(result.get(), isA(IssueInfo::class.java))
    }

    @Test
    fun `checking one issues with not null and custom mapper`() = runBlocking {
        mock.chain(
            request = mock.request().withPath("/issues/1"),
            response = mock.response().withBody(
                "{ \"id\": 1, \"title\": \"issue 1\", \"number\": null, \"snake_property\": 10 }"
            ).withStatusCode(HttpURLConnection.HTTP_OK)
        )

        val (res, result) = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("issues/1")).awaitResponseResultObject<IssueInfo>(createCustomMapper())
        }
        assertNotNull(res)
        assertNotNull(result)
        assertThat(result.get(), isA(IssueInfo::class.java))
        assertEquals(result.get().snakeProperty, 10)
    }

    @Test
    fun `one issues throws Not Found Errors`() = runBlocking {
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

    data class IssueInfo(val id: Int, val title: String, val number: Int, val snakeProperty: Int)

    private fun createCustomMapper(): ObjectMapper {
        val mapper = ObjectMapper().registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
        return mapper
    }
}