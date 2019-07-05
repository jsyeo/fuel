package com.github.kittinunf.fuel

import com.github.kittinunf.fuel.core.Encoding
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.RequestFactory
import com.github.kittinunf.fuel.core.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.core.awaitStringResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import java.io.File
import java.net.HttpURLConnection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BlockingRequestTest : MockHttpTestCase() {
    private val manager: FuelManager by lazy { FuelManager() }

    class PathStringConvertibleImpl(url: String) : RequestFactory.PathStringConvertible {
        override val path = url
    }

    class RequestConvertibleImpl(val method: Method, private val url: String) : RequestFactory.RequestConvertible {
        override val request = createRequest()

        private fun createRequest(): Request {
            val encoder = Encoding(
                httpMethod = method,
                urlString = url,
                parameters = listOf("foo" to "bar")
            )
            return encoder.request
        }
    }

    @Test
    fun httpGetRequestWithDataResponse() = runBlocking {
        val httpRequest = mock.request()
            .withMethod(Method.GET.value)
            .withPath("/get")

        mock.chain(request = httpRequest, response = mock.reflect())

        val (response, data) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("get")).awaitByteArrayResponseResult()
        }
        assertNotNull(response)
        assertNotNull(data.get())

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpGetRequestWithStringResponse() = runBlocking {
        val httpRequest = mock.request()
                .withMethod(Method.GET.value)
                .withPath("/get")

        mock.chain(request = httpRequest, response = mock.reflect())

        val (response, data) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("get")).awaitStringResponseResult()
        }
        assertNotNull(response)
        assertNotNull(data.get())

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpGetRequestWithParameters() = runBlocking {
        val paramKey = "foo"
        val paramValue = "bar"

        val httpRequest = mock.request()
            .withMethod(Method.GET.value)
            .withPath("/get")
                .withQueryStringParameter(paramKey, paramValue)

        mock.chain(request = httpRequest, response = mock.reflect())

        val (response, data) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("get"), listOf(paramKey to paramValue)).awaitStringResponseResult()
        }
        assertNotNull(response)
        assertNotNull(data.get())

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data.get(), containsString(paramKey))
        assertThat(data.get(), containsString(paramValue))
    }

    @Test
    fun httpPostRequestWithParameters() = runBlocking {
        val paramKey = "foo"
        val paramValue = "bar"

        val httpRequest = mock.request()
                .withMethod(Method.POST.value)
                .withPath("/post")
                .withBody("$paramKey=$paramValue")

        mock.chain(request = httpRequest, response = mock.reflect())

        val (response, data) = withContext(Dispatchers.IO) {
            manager.request(Method.POST, mock.path("post"), listOf(paramKey to paramValue)).awaitStringResponseResult()
        }
        assertNotNull(response)
        assertNotNull(data.get())

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data.get(), containsString(paramKey))
        assertThat(data.get(), containsString(paramValue))
    }

    @Test
    fun httpPostRequestWithBody() = runBlocking {
        val foo = "foo"
        val bar = "bar"
        val body = "{ $foo : $bar }"

        val httpRequest = mock.request()
                .withMethod(Method.POST.value)
                .withPath("/post")

        mock.chain(request = httpRequest, response = mock.reflect())

        val (response, data) = withContext(Dispatchers.IO) {
            manager.request(Method.POST, mock.path("post")).body(body).awaitStringResponseResult()
        }
        assertNotNull(response)
        assertNotNull(data.get())

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data.get(), containsString(foo))
        assertThat(data.get(), containsString(bar))
    }

    @Test
    fun httpPutRequestWithParameters() = runBlocking {
        val paramKey = "foo"
        val paramValue = "bar"

        val httpRequest = mock.request()
                .withMethod(Method.PUT.value)
                .withPath("/put")
                .withBody("$paramKey=$paramValue")

        mock.chain(request = httpRequest, response = mock.reflect())

        val (response, data) = withContext(Dispatchers.IO) {
            manager.request(Method.PUT, mock.path("put"), listOf(paramKey to paramValue)).awaitStringResponseResult()
        }
        assertNotNull(response)
        assertNotNull(data.get())

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data.get(), containsString(paramKey))
        assertThat(data.get(), containsString(paramValue))
    }

    @Test
    fun httpDeleteRequestWithParameters() = runBlocking {
        val paramKey = "foo"
        val paramValue = "bar"

        val httpRequest = mock.request()
                .withMethod(Method.DELETE.value)
                .withPath("/delete")
                .withQueryStringParameter(paramKey, paramValue)

        mock.chain(request = httpRequest, response = mock.reflect())

        val (response, data) = withContext(Dispatchers.IO) {
            manager.request(Method.DELETE, mock.path("delete"), listOf(paramKey to paramValue)).awaitStringResponseResult()
        }
        assertNotNull(response)
        assertNotNull(data.get())

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data.get(), containsString(paramKey))
        assertThat(data.get(), containsString(paramValue))
    }

    @Test
    fun httpGetRequestWithPathStringConvertible() = runBlocking {
        val httpRequest = mock.request()
                .withMethod(Method.GET.value)
                .withPath("/path-string")

        mock.chain(request = httpRequest, response = mock.reflect())

        val (response, data) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, PathStringConvertibleImpl(mock.path("path-string"))).awaitStringResponseResult()
        }
        assertNotNull(response)
        assertNotNull(data.get())

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data.get(), containsString("path-string"))
    }

    @Test
    fun httpGetRequestWithRequestConvertible() = runBlocking {
        val httpRequest = mock.request()
                .withMethod(Method.GET.value)
                .withPath("/get")

        mock.chain(request = httpRequest, response = mock.reflect())

        val (response, data) = withContext(Dispatchers.IO) {
            manager.request(RequestConvertibleImpl(Method.GET, mock.path("get"))).awaitStringResponseResult()
        }
        assertNotNull(response)
        assertNotNull(data.get())

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpGetRequestWithPathStringConvertibleAndOverriddenParameters() = runBlocking {
        val paramKey = "foo"
        val paramValue = "xxx"

        val httpRequest = mock.request()
                .withMethod(Method.GET.value)
                .withPath("/get")

        mock.chain(request = httpRequest, response = mock.reflect())

        val (response, data) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, PathStringConvertibleImpl(mock.path("get")), listOf(paramKey to paramValue)).awaitStringResponseResult()
        }
        assertNotNull(response)
        assertNotNull(data.get())

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data.get(), containsString(paramKey))
        assertThat(data.get(), containsString(paramValue))
    }

    @Test
    fun httpGetRequestWithNotOverriddenHeaders() = runBlocking {
        val headerKey = Headers.CONTENT_TYPE
        val headerValue = "application/json"
        manager.baseHeaders = mapOf(headerKey to headerValue)

        val httpRequest = mock.request()
                .withMethod(Method.GET.value)
                .withPath("/get")

        mock.chain(request = httpRequest, response = mock.reflect())

        val (response, data) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("get"), listOf("email" to "foo@bar.com")).awaitStringResponseResult()
        }
        assertNotNull(response)
        assertNotNull(data.get())

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpUploadRequestWithParameters() = runBlocking {
        val httpRequest = mock.request()
                .withMethod(Method.POST.value)
                .withPath("/upload")

        mock.chain(request = httpRequest, response = mock.reflect())

        val path = File(System.getProperty("user.dir"), "/src/test/assets").resolve("lorem_ipsum_long.tmp").path
        val (response, data) = withContext(Dispatchers.IO) {
            manager.upload(mock.path("upload"), parameters = listOf("foo" to "bar", "foo1" to "bar1"))
                    .add { FileDataPart.from(path, "lorem_ipsum_long.tmp") }
                    .awaitStringResponseResult()
        }
        assertNotNull(response)
        assertNotNull(data.get())

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }
}
