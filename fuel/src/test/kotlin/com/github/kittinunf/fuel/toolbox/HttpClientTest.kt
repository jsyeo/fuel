package com.github.kittinunf.fuel.toolbox

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Client
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.core.awaitStringResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import com.github.kittinunf.fuel.test.MockReflected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.mockserver.matchers.Times
import org.mockserver.model.Header.header
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HttpClientTest : MockHttpTestCase() {

    class TestHook : Client.Hook {
        override fun preConnect(connection: HttpURLConnection, request: Request) {
            // no-op
        }

        override fun interpretResponseStream(request: Request, inputStream: InputStream): InputStream = inputStream

        override fun postConnect(request: Request) {
            // no-op
        }

        override fun httpExchangeFailed(request: Request, exception: IOException) {
            // no-op
        }
    }

    @Test
    fun httpClientIsTheDefaultClient() {
        val request = Fuel.request(Method.GET, mock.path("default-client"))
        assertThat(request.executionOptions.client, instanceOf(HttpClient::class.java))
    }

    @Test
    fun usesOverrideMethodForPatch() = runBlocking {
        val request = Fuel.patch(mock.path("patch-with-override"))

        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/patch-with-override"),
            response = mock.reflect()
        )

        val (_, result) = withContext(Dispatchers.IO) {
            request.awaitResponseResult(MockReflected.Deserializer())
        }
        val (data, error) = result
        assertNotNull(data, "Expected data, actual error $error")
        assertEquals(data.method, Method.POST.value)
        assertEquals(data["X-HTTP-Method-Override"].firstOrNull(), Method.PATCH.value)
    }

    @Test
    fun injectsAcceptTransferEncoding() = runBlocking {
        val request = reflectedRequest(Method.GET, "accept-transfer-encoding")
        val (_, result) = withContext(Dispatchers.IO) {
            request.awaitResponseResult(MockReflected.Deserializer())
        }
        val (data, error) = result
        assertNotNull(data, "Expected data, actual error $error")
        assertNotEquals(data[Headers.ACCEPT_TRANSFER_ENCODING].size, 0)
    }

    @Test
    fun setsContentLengthIfKnown() = runBlocking {
        val request = reflectedRequest(Method.POST, "content-length-test")
            .body("my-body")

        val (_, result) = withContext(Dispatchers.IO) {
            request.awaitResponseResult(MockReflected.Deserializer())
        }
        val (data, error) = result
        assertNotNull(data, "Expected data, actual error $error")
        assertEquals(data[Headers.CONTENT_LENGTH].firstOrNull(), "my-body".toByteArray().size.toString())
    }

    @Test
    fun dropBodyForGetRequest() = runBlocking {
        val request = reflectedRequest(Method.GET, "get-body-output")
            .body("my-body")
        val (_, result) = withContext(Dispatchers.IO) {
            request.awaitResponseResult(MockReflected.Deserializer())
        }
        val (data, error) = result
        assertNotNull(data, "Expected data, actual error $error")
        assertNull(data.body)
    }

    @Test
    fun allowPostWithBody() = runBlocking {
        val request = reflectedRequest(Method.POST, "post-body-output")
            .body("my-body")
        val (_, result) = withContext(Dispatchers.IO) {
            request.awaitResponseResult(MockReflected.Deserializer())
        }
        val (data, error) = result
        assertNotNull(data, "Expected data, actual error $error")
        assertEquals(data.body!!.string, "my-body")
    }

    @Test
    fun allowPatchWithBody() = runBlocking {
        val request = Fuel.patch(mock.path("patch-body-output"))
            .body("my-body")

        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/patch-body-output"),
            response = mock.reflect()
        )

        val (_, result) = withContext(Dispatchers.IO) {
            request.awaitResponseResult(MockReflected.Deserializer())
        }
        val (data, error) = result
        assertNotNull(data, "Expected data, actual error $error")
        assertEquals(data.body!!.string, "my-body")
    }

    @Test
    fun allowDeleteWithBody() = runBlocking {
        val request = reflectedRequest(Method.DELETE, "delete-body-output")
            .body("my-body")

        val (_, result) = withContext(Dispatchers.IO) {
            request.awaitResponseResult(MockReflected.Deserializer())
        }
        val (data, error) = result
        assertNotNull(data, "Expected data, actual error $error")
        assertEquals(data.body!!.string, "my-body")
    }

    @Test
    fun canDisableClientCache() = runBlocking {
        mock.chain(
            request = mock.request()
                .withMethod(Method.GET.value)
                .withPath("/cached"),
            response = mock.response()
                .withHeader(Headers.CACHE_CONTROL, "max-age=600")
                .withBody("cached"),
            times = Times.once()
        )

        mock.chain(
            request = mock.request()
                .withMethod(Method.GET.value)
                .withPath("/cached")
                .withHeader(header(Headers.CACHE_CONTROL, "no-cache")),
            response = mock.response()
                .withHeader(Headers.CACHE_CONTROL, "max-age=600")
                .withBody("fresh"),
            times = Times.once()
        )

        val request = Fuel.get(mock.path("cached"))
        val (_, result) = withContext(Dispatchers.IO) {
            request.awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(data, "Expected data, actual error $error")
        assertEquals(data, "cached")

        val (_, result2) = request.apply { executionOptions.useHttpCache = false }.awaitStringResponseResult()
        val (data2, error2) = result2
        assertNotNull(data2, "Expected data2, actual error $error2")
        assertEquals(data2, "fresh")
    }

    @Test
    fun changeClientHook() {

        val request = Fuel.request(Method.GET, mock.path("change-hook")).apply {
            val httpClient = executionOptions.client as HttpClient
            httpClient.hook = TestHook()
        }

        val client = request.executionOptions.client as HttpClient
        assertThat(client.hook, instanceOf(TestHook::class.java))
    }
}
