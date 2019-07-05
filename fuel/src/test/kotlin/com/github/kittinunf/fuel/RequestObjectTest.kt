package com.github.kittinunf.fuel

import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.isA
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.Reader
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RequestObjectTest : MockHttpTestCase() {

    data class ReflectMockModel(var userAgent: String = "") {
        class Deserializer : ResponseDeserializable<ReflectMockModel> {
            override fun deserialize(content: String): ReflectMockModel = ReflectMockModel(content)
        }

        class MalformedDeserializer : ResponseDeserializable<ReflectMockModel> {
            override fun deserialize(reader: Reader): ReflectMockModel = throw IllegalStateException("Malformed data")
        }
    }

    @Test
    fun httpRequestObjectUserAgentValidTest() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/user-agent"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("user-agent")).awaitResponseResult(ReflectMockModel.Deserializer())
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        assertThat(data, isA(ReflectMockModel::class.java))
        assertNotEquals(data.userAgent, "")
    }

    @Test
    fun httpRequestObjectUserAgentInvalidTest() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/user-agent"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("user-agent")).awaitResponseResult(ReflectMockModel.MalformedDeserializer())
        }
        val (data, error) = result
        assertNotNull(response)
        assertNotNull(error)
        assertNull(data)

        assertThat(response, notNullValue())
        assertThat(error, notNullValue())
        assertThat(data, nullValue())

        assertThat(error.exception as IllegalStateException, isA(IllegalStateException::class.java))
        assertEquals(error.exception.message, "Malformed data")
    }
}
