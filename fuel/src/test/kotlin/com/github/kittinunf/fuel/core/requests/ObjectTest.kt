package com.github.kittinunf.fuel.core.requests

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.test.MockHttpTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.UUID

private data class UUIDResponse(val uuid: String)

private object UUIDResponseDeserializer : ResponseDeserializable<UUIDResponse> {

    class NoValidFormat(m: String = "Not a UUID") : Exception(m)

    override fun deserialize(content: String): UUIDResponse {
        if (content.contains("=") || !content.contains("-")) {
            throw FuelError.wrap(NoValidFormat())
        }
        return UUIDResponse(content)
    }
}

class ObjectTest : MockHttpTestCase() {

    private fun randomUUID() = UUID.randomUUID()
    private fun getUUID(uuid: UUID, path: String = "uuid"): Request {
        mock.chain(
            request = mock.request().withPath("/$path"),
            response = mock.response().withBody(uuid.toString())
        )

        return Fuel.request(Method.GET, mock.path(path))
    }

    @Test
    fun `checking UUID result and response`() = runBlocking {
        val uuid = randomUUID()
        val (response, result) = withContext(Dispatchers.IO) {
            getUUID(uuid).awaitResponseResult(UUIDResponseDeserializer)
        }
        val (data, error) = result
        assertNotNull("Expected data, actual error $error", data)

        assertEquals(data!!.uuid, uuid.toString())
        assertNotNull("Expected response to be not null", response)
    }
}
