package com.github.kittinunf.fuel

import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.awaitStringResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RequestStringExtensionTest : MockHttpTestCase() {
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
    fun httpGet() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/http-get"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            mock.path("http-get").httpGet().awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpPost() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/http-post"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            mock.path("http-post").httpPost().awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpPut() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.PUT.value).withPath("/http-put"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            mock.path("http-put").httpPut().awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpPatch() = runBlocking {
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
            mock.path("http-patch").httpPatch().awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpDelete() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.DELETE.value).withPath("/http-delete"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            mock.path("http-delete").httpDelete().awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun httpDownload() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/http-download"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            mock.path("http-download")
                    .httpDownload()
                    .fileDestination { _, _ -> File.createTempFile("http-download.dl", null) }
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
    fun httpUploadWithPut() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.PUT.value).withPath("/http-upload"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            mock.path("http-upload")
                    .httpUpload(method = Method.PUT)
                    .add {
                        val dir = System.getProperty("user.dir")
                        val currentDir = File(dir, "src/test/assets")
                        FileDataPart(File(currentDir, "lorem_ipsum_long.tmp"))
                    }
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
    fun httpUploadWithPost() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/http-upload"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            mock.path("http-upload")
                    .httpUpload()
                    .add {
                        val dir = System.getProperty("user.dir")
                        val currentDir = File(dir, "src/test/assets")
                        FileDataPart(File(currentDir, "lorem_ipsum_long.tmp"))
                    }
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
    fun httpHead() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.HEAD.value).withPath("/http-head"),
            response = mock.response().withStatusCode(HttpURLConnection.HTTP_OK)
        )

        val (response, result) = withContext(Dispatchers.IO) {
            mock.path("http-head").httpHead().awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)
    }
}