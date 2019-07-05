package com.github.kittinunf.fuel

import com.github.kittinunf.fuel.core.Encoding
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.RequestFactory
import com.github.kittinunf.fuel.core.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.core.awaitStringResponseResult
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.test.MockHttpTestCase
import com.github.kittinunf.fuel.util.decodeBase64
import com.google.common.net.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.mockserver.model.BinaryBody
import java.net.HttpURLConnection
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RequestTest : MockHttpTestCase() {

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
    fun testResponseURLShouldSameWithRequestURL() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/request"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("request")).awaitByteArrayResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpGetRequestWithDataResponse() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/request"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("request")).awaitByteArrayResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpGetRequestWithStringResponse() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/request"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("request")).awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpGetRequestWithImageResponse() = runBlocking {
        val decodedImage = "iVBORwKGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAClEQVQYV2NgYAAAAAMAAWgmWQAAAAASUVORK5CYII=".decodeBase64()

        val httpResponse = mock.response()
                .withHeader(Headers.CONTENT_TYPE, "image/png")
                .withBody(BinaryBody(decodedImage))

        mock.chain(
                request = mock.request().withMethod(Method.GET.value).withPath("/image"),
                response = httpResponse
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("image")).awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)
        assertThat(response.toString(), containsString("bytes of image/png"))

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpGetRequestWithBytesResponse() = runBlocking {
        val bytes = ByteArray(555)
        Random().nextBytes(bytes)

        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/bytes"),
            response = mock.response().withBody(BinaryBody(bytes, MediaType.OCTET_STREAM))
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("bytes")).awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        assertThat(response.toString(), containsString("Body : (555 bytes of application/octet-stream)"))

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpGetRequestWithParameters() = runBlocking {
        val paramKey = "foo"
        val paramValue = "bar"

        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/get"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("get"), listOf(paramKey to paramValue))
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data, containsString(paramKey))
        assertThat(data, containsString(paramValue))
    }

    @Test
    fun httpPostRequestWithParameters() = runBlocking {
        val paramKey = "foo"
        val paramValue = "bar"

        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/post"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.POST, mock.path("post"), listOf(paramKey to paramValue))
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data, containsString(paramKey))
        assertThat(data, containsString(paramValue))
    }

    @Test
    fun httpPostRequestWithBody() = runBlocking {
        val foo = "foo"
        val bar = "bar"
        val body = "{ $foo: $bar }"

        // Reflect encodes the body as a string, and gives back the body as a property of the body
        //  therefore the outer body here is the ey and the inner string is the actual body
        val correctBodyResponse = "\"body\":{\"type\":\"STRING\",\"string\":\"$body\",\"contentType\":\"text/plain; charset=utf-8\"}"

        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/post"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.POST, mock.path("post"))
                    .jsonBody(body)
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data, containsString(correctBodyResponse))
    }

    @Test
    fun httpPutRequestWithParameters() = runBlocking {
        val paramKey = "foo"
        val paramValue = "bar"

        mock.chain(
            request = mock.request().withMethod(Method.PUT.value).withPath("/put"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.PUT, mock.path("put"), listOf(paramKey to paramValue))
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data, containsString(paramKey))
        assertThat(data, containsString(paramValue))
    }

    @Test
    fun httpPatchRequestWithParameters() = runBlocking {
        val paramKey = "foo2"
        val paramValue = "bar2"

        mock.chain(
            request = mock.request().withMethod(Method.PATCH.value).withPath("/patch"),
            response = mock.reflect()
        )

        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withHeader("X-HTTP-Method-Override", Method.PATCH.value).withPath("/patch"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.PATCH, mock.path("patch"), listOf(paramKey to paramValue))
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data, containsString(paramKey))
        assertThat(data, containsString(paramValue))
    }

    @Test
    fun httpDeleteRequestWithParameters() = runBlocking {
        val paramKey = "foo"
        val paramValue = "bar"
        val foo = "foo"
        val bar = "bar"
        val body = "{ $foo : $bar }"
        val correctBodyResponse = "\"body\":{\"type\":\"STRING\",\"string\":\"$body\",\"contentType\":\"text/plain; charset=utf-8\"}"

        mock.chain(
            request = mock.request().withMethod(Method.DELETE.value).withPath("/delete"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.DELETE, mock.path("delete"), listOf(paramKey to paramValue))
                    .jsonBody(body)
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data, containsString(paramKey))
        assertThat(data, containsString(paramValue))
        assertThat(data, containsString(correctBodyResponse))
    }

    @Test
    fun httpHeadRequest() = runBlocking {
        val paramKey = "foo"
        val paramValue = "bar"

        mock.chain(
            request = mock.request().withMethod(Method.HEAD.value).withPath("/head"),
            response = mock.response().withStatusCode(HttpURLConnection.HTTP_OK)
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.HEAD,
                    mock.path("head"),
                    listOf(paramKey to paramValue)
            ).awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertEquals(data, "")
    }

    @Test
    fun httpOptionsRequest() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.OPTIONS.value).withPath("/options"),
            response = mock.response().withStatusCode(HttpURLConnection.HTTP_OK)
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.OPTIONS, mock.path("options")).awaitStringResponseResult()
        }
        val (data, error) = result

        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpTraceRequest() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.TRACE.value).withPath("/trace"),
            response = mock.response().withStatusCode(HttpURLConnection.HTTP_OK)
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.TRACE, mock.path("trace"))
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpGetRequestUserAgentWithPathStringConvertible() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/user-agent"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, PathStringConvertibleImpl(mock.path("user-agent")))
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data, containsString("user-agent"))
    }

    @Test
    fun httpGetRequestWithRequestConvertible() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/get"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(RequestConvertibleImpl(Method.GET, mock.path("get")))
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpPatchRequestWithRequestConvertible() = runBlocking {
        val paramKey = "foo"
        val paramValue = "bar"

        mock.chain(
            request = mock.request().withMethod(Method.PATCH.value).withPath("/patch"),
            response = mock.reflect()
        )

        // HttpUrlConnection doesn't support patch
        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withHeader("X-HTTP-Method-Override", Method.PATCH.value).withPath("/patch"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.PATCH,
                    PathStringConvertibleImpl(mock.path("patch")),
                    listOf(paramKey to paramValue)
            ).awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpPostRequestWithRequestConvertibleAndOverriddenParameters() = runBlocking {
        val paramKey = "foo"
        val paramValue = "xxx"

        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/post"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.POST, mock.path("post"), listOf(paramKey to paramValue))
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data, containsString(paramKey))
        assertThat(data, containsString(paramValue))
    }

    @Test
    fun httpGetParameterArrayWillFormCorrectURL() = runBlocking {
        val lionel = "Lionel Ritchie"
        val list = arrayOf("once", "Twice", "Three", "Times", "Lady")
        val params = listOf("foo" to list, "bar" to lionel)

        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/get"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            mock.path("get").httpGet(params).awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)

        val json = JSONObject(data)
        val query = json.getJSONObject("query")

        assertEquals(JSONArray("[\"$lionel\"]").toString(), query.getJSONArray("bar").toString())
        assertEquals(list.toList(), query.getJSONArray("foo[]").map { it.toString() })
    }
}
