package com.github.kittinunf.fuel

import com.github.kittinunf.fuel.core.Encoding
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.RequestFactory
import com.github.kittinunf.fuel.core.awaitStringResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import java.io.File
import java.net.HttpURLConnection
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RequestSharedInstanceTest : MockHttpTestCase() {

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

    @BeforeTest
    fun setupFuelManager() {
        FuelManager.instance.baseHeaders = mapOf("foo" to "bar")
        FuelManager.instance.baseParams = listOf("key" to "value")
    }

    @AfterTest
    fun resetFuelManager() {
        FuelManager.instance.reset()
    }

    @Test
    fun httpGetRequestWithSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/Fuel/get"),
            response = mock.reflect()
        )
        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("Fuel/get")).awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data.toLowerCase(), containsString("foo"))
        assertThat(data.toLowerCase(), containsString("bar"))
        assertThat(data.toLowerCase(), containsString("key"))
        assertThat(data.toLowerCase(), containsString("value"))

        assertThat(data, containsString("Fuel/get"))
    }

    @Test
    fun httpPostRequestWithSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/Fuel/post"),
            response = mock.reflect()
        )
        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.post(mock.path("Fuel/post")).awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data.toLowerCase(), containsString("foo"))
        assertThat(data.toLowerCase(), containsString("bar"))
        assertThat(data.toLowerCase(), containsString("key"))
        assertThat(data.toLowerCase(), containsString("value"))

        assertThat(data, containsString("Fuel/post"))
    }

    @Test
    fun httpPutRequestWithSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.PUT.value).withPath("/Fuel/put"),
            response = mock.reflect()
        )
        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.put(mock.path("Fuel/put")).awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data.toLowerCase(), containsString("foo"))
        assertThat(data.toLowerCase(), containsString("bar"))
        assertThat(data.toLowerCase(), containsString("key"))
        assertThat(data.toLowerCase(), containsString("value"))

        assertThat(data, containsString("Fuel/put"))
    }

    @Test
    fun httpDeleteRequestWithSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.DELETE.value).withPath("/Fuel/delete"),
            response = mock.reflect()
        )
        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.delete(mock.path("Fuel/delete")).awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data.toLowerCase(), containsString("foo"))
        assertThat(data.toLowerCase(), containsString("bar"))
        assertThat(data.toLowerCase(), containsString("key"))
        assertThat(data.toLowerCase(), containsString("value"))

        assertThat(data, containsString("Fuel/delete"))
    }

    @Test
    fun httpGetRequestWithPathStringConvertibleAndSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/Fuel/get"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.get(PathStringConvertibleImpl(mock.path("Fuel/get")))
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data, containsString("Fuel/get"))
    }

    @Test
    fun httpPostRequestWithPathStringConvertibleAndSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/Fuel/post"),
            response = mock.reflect()
        )
        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.post(PathStringConvertibleImpl(mock.path("Fuel/post")))
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data, containsString("Fuel/post"))
    }

    @Test
    fun httpPutRequestWithPathStringConvertibleAndSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.PUT.value).withPath("/Fuel/put"),
            response = mock.reflect()
        )
        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.put(PathStringConvertibleImpl(mock.path("Fuel/put")))
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data, containsString("Fuel/put"))
    }

    @Test
    fun httpDeleteRequestWithPathStringConvertibleAndSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.DELETE.value).withPath("/Fuel/delete"),
            response = mock.reflect()
        )
        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.delete(PathStringConvertibleImpl(mock.path("Fuel/delete")))
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data, containsString("Fuel/delete"))
    }

    @Test
    fun httpGetRequestWithRequestConvertibleAndSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/Fuel/get"),
            response = mock.reflect()
        )
        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.request(RequestConvertibleImpl(Method.GET, mock.path("Fuel/get"))).awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpPostRequestWithRequestConvertibleAndSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/Fuel/post"),
            response = mock.reflect()
        )
        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.request(RequestConvertibleImpl(Method.POST, mock.path("Fuel/post"))).awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpPutRequestWithRequestConvertibleAndSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.PUT.value).withPath("/Fuel/put"),
            response = mock.reflect()
        )
        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.request(RequestConvertibleImpl(Method.PUT, mock.path("Fuel/put"))).awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpDeleteRequestWithRequestConvertibleAndSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.DELETE.value).withPath("/Fuel/delete"),
            response = mock.reflect()
        )
        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.request(RequestConvertibleImpl(Method.DELETE, mock.path("Fuel/delete"))).awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpUploadWithProgressValidCase() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/Fuel/upload"),
            response = mock.reflect()
        )

        var read = -1L
        var total = -1L

        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.upload(mock.path("Fuel/upload"))
                    .add {
                        val dir = System.getProperty("user.dir")
                        FileDataPart(File(dir, "src/test/assets/lorem_ipsum_long.tmp"))
                    }
                    .progress { readBytes, totalBytes ->
                        read = readBytes
                        total = totalBytes
                        println("read: $read, total: $total")
                    }
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertEquals(read == total && read != -1L && total != -1L, true)
    }

    @Test
    fun httpDownloadWithProgressValidCase() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/Fuel/download"),
            response = mock.reflect()
        )

        var read = -1L
        var total = -1L

        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.download(mock.path("Fuel/download"))
                    .fileDestination { _, _ -> File.createTempFile("download.dl", null) }
                    .progress { readBytes, totalBytes ->
                        read = readBytes
                        total = totalBytes
                    }
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertEquals(read, total)
    }
}
