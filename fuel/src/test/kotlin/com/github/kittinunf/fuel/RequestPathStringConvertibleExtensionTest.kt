package com.github.kittinunf.fuel

import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.RequestFactory
import com.github.kittinunf.fuel.core.awaitStringResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RequestPathStringConvertibleExtensionTest : MockHttpTestCase() {
    class PathStringConvertibleImpl(url: String) : RequestFactory.PathStringConvertible {
        override val path = url
    }

    @Test
    fun httpGetRequestWithSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/http-get"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            PathStringConvertibleImpl(mock.path("http-get"))
                    .httpGet()
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
    fun httpPostRequestWithSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/http-post"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            PathStringConvertibleImpl(mock.path("http-post"))
                    .httpPost()
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data, containsString("http-post"))
    }

    @Test
    fun httpPutRequestWithSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.PUT.value).withPath("/http-put"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            PathStringConvertibleImpl(mock.path("http-put"))
                    .httpPut()
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data, containsString("http-put"))
    }

    @Test
    fun httpPatchRequestWithSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.PATCH.value).withPath("/http-patch"),
            response = mock.reflect()
        )

        mock.chain(
            request = mock.request()
                .withMethod(Method.POST.value)
                .withHeader("X-HTTP-Method-Override", Method.PATCH.value)
                .withPath("/http-patch"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            PathStringConvertibleImpl(mock.path("http-patch"))
                    .httpPatch()
                    .awaitStringResponseResult()
        }

        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data, containsString("http-patch"))
    }

    @Test
    fun httpDeleteRequestWithSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.DELETE.value).withPath("/http-delete"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            PathStringConvertibleImpl(mock.path("http-delete"))
                    .httpDelete()
                    .awaitStringResponseResult()
        }

        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        assertThat(data, containsString("http-delete"))
    }

    @Test
    fun httpUploadRequestWithSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/http-upload"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            PathStringConvertibleImpl(mock.path("http-upload"))
                    .httpUpload()
                    .add { FileDataPart.from(File(System.getProperty("user.dir"), "src/test/assets").absolutePath, "lorem_ipsum_long.tmp") }
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
    fun httpDownloadRequestWithSharedInstance() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/http-download"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            PathStringConvertibleImpl(mock.path("http-download"))
                    .httpDownload()
                    .fileDestination { _, _ -> File.createTempFile("123456", null) }
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }
}
