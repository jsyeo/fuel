package com.github.kittinunf.fuel.json

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.test.MockHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.core.Is.isA
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection

class FuelJsonTest {
    private lateinit var mock: MockHelper

    @Before
    fun setupFuelManager() {
        FuelManager.instance.apply {
            baseHeaders = mapOf("foo" to "bar")
            baseParams = listOf("key" to "value")
        }
        this.mock = MockHelper().apply { setup() }
    }

    @After
    fun resetFuelManager() {
        FuelManager.instance.reset()
        this.mock.tearDown()
    }

    @Test
    fun `hello word parameters with json results`() = runBlocking {
        mock.chain(
            request = mock.request().withPath("/get"),
            response = mock.reflect()
        )
        val (response, result) = withContext(Dispatchers.IO) {
            mock.path("get").httpGet(listOf("hello" to "world")).awaitResponseResultJson()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)
        assertThat(data as FuelJson, isA(FuelJson::class.java))
        assertThat(data.obj(), isA(JSONObject::class.java))
        assertEquals(response.statusCode, HttpURLConnection.HTTP_OK)
    }
}