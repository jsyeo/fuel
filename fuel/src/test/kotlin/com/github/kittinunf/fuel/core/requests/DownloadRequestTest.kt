package com.github.kittinunf.fuel.core.requests

import com.github.kittinunf.fuel.core.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.core.awaitStringResponseResult
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.test.MockHttpTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import com.google.common.net.MediaType
import org.hamcrest.core.Is.isA
import org.junit.Assert.assertThat
import org.mockserver.model.BinaryBody
import org.mockserver.model.Delay
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.util.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.Test

class DownloadRequestTest : MockHttpTestCase() {

    private fun <T : Any> assertDownloadedBytesToFile(result: ResponseResultOf<T>, file: File, numberOfBytes: Int): ResponseResultOf<T> {
        val (response, wrapped) = result
        val (data, error) = wrapped
        assertNotNull(response, "Expected response to not be null")
        assertNotNull(data, "Expected data, actual $error")
        assertEquals(file.length(), numberOfBytes.toLong(), "Expected file length ${file.length()} to match $numberOfBytes")
        assertEquals(response.statusCode, HttpURLConnection.HTTP_OK)
        return result
    }

    @Test
    fun `Download To File`() = runBlocking {
        val manager = FuelManager()

        val numberOfBytes = 32768
        val file = File.createTempFile(numberOfBytes.toString(), null)
        val bytes = ByteArray(numberOfBytes).also { Random().nextBytes(it) }

        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/bytes"),
            response = mock.response().withBody(BinaryBody(bytes, MediaType.OCTET_STREAM))
        )

        val result = withContext(Dispatchers.IO) {
            manager.download(mock.path("bytes"))
                .fileDestination { _, _ -> file }
                .awaitByteArrayResponseResult()
        }

        assertDownloadedBytesToFile(result, file, numberOfBytes)
        Unit
    }

    @Test
    fun `Download To Stream`() = runBlocking {
        val manager = FuelManager()

        val numberOfBytes = 32768
        val stream = ByteArrayOutputStream(numberOfBytes)
        val bytes = ByteArray(numberOfBytes).also { Random().nextBytes(it) }

        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/bytes"),
            response = mock.response().withBody(BinaryBody(bytes, MediaType.OCTET_STREAM))
        )

        val (response, wrapped) = withContext(Dispatchers.IO) {
            manager.download(mock.path("bytes"))
                .streamDestination { _, _ -> Pair(stream, { ByteArrayInputStream(stream.toByteArray()) }) }
                .awaitByteArrayResponseResult()
        }
        val (data, error) = wrapped
        assertNotNull(response, "Expected response to not be null")
        assertNotNull(data, "Expected data, actual $error")
        assertEquals(stream.size(), numberOfBytes, "Expected stream output length ${stream.size()} to match $numberOfBytes")
        assertEquals(response.statusCode, HttpURLConnection.HTTP_OK)
    }

    @Test
    fun `Download Bytes With Progress`() = runBlocking {
        val manager = FuelManager()

        val numberOfBytes = 1186
        val file = File.createTempFile(numberOfBytes.toString(), null)
        val bytes = ByteArray(numberOfBytes).also { Random().nextBytes(it) }

        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/bytes"),
            response = mock.response().withBody(BinaryBody(bytes, MediaType.OCTET_STREAM))
        )

        var read = -1L
        var total = -1L

        val triple = withContext(Dispatchers.IO) {
            manager.download(mock.path("bytes"))
                .fileDestination { _, _ -> file }
                .progress { readBytes, totalBytes -> read = readBytes; total = totalBytes }
                .awaitByteArrayResponseResult()
        }

        val (_, result) = assertDownloadedBytesToFile(triple, file, numberOfBytes)
        val (data, _) = result

        assertThat(data, isA(ByteArray::class.java))
        assertEquals(data!!.size.toLong(), read)
        assertEquals(read == total && read != -1L && total != -1L, true, "Progress read bytes and total bytes should be equal")
    }

    @Test
    fun `Download String With Progress`() = runBlocking {
        val manager = FuelManager()

        val numberOfBytes = DEFAULT_BUFFER_SIZE * 5
        val file = File.createTempFile(numberOfBytes.toString(), null)
        val bytes = ByteArray(numberOfBytes).also { Random().nextBytes(it) }

        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/bytes"),
            response = mock.response().withBody(BinaryBody(bytes, MediaType.OCTET_STREAM))
        )

        var read = -1L
        var total = -1L

        val triple = withContext(Dispatchers.IO) {
            manager.download(mock.path("bytes"))
                .fileDestination { _, _ -> file }
                .progress { readBytes, totalBytes -> read = readBytes; total = totalBytes }
                .awaitStringResponseResult()
        }
        val (_, result) = assertDownloadedBytesToFile(triple, file, numberOfBytes)
        val (data, _) = result

        assertThat(data, isA(String::class.java))
        assertEquals(data, file.readText())
        assertEquals(read == total && read != -1L && total != -1L, true, "Progress read bytes and total bytes should be equal")
    }

    @Test
    fun `Download File From Http Not Found`() = runBlocking {
        val manager = FuelManager()

        val numberOfBytes = 131072
        val file = File.createTempFile(numberOfBytes.toString(), null)

        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/bytes"),
            response = mock.response().withStatusCode(HttpURLConnection.HTTP_NOT_FOUND)
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.download(mock.path("bytes"))
                .fileDestination { _, _ -> file }
                .progress { _, _ -> }
                .awaitStringResponseResult()
        }
        val (data, error) = result

        assertNotNull(response, "Expected response to not be null")
        assertNotNull(error, "Expected error, actual $data")
        assertEquals(file.length(), 0L, "Expected nothing to be written to file")

        val statusCode = HttpURLConnection.HTTP_NOT_FOUND
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun `Download To Invalid File Destination`() = runBlocking {
        val manager = FuelManager()

        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/bytes"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.download(mock.path("bytes"))
                .fileDestination { _, _ ->
                    val dir = System.getProperty("user.dir")
                    File.createTempFile("not_found_file", null, File(dir, "not-a-folder"))
                }
               .awaitStringResponseResult()
        }
        val (data, error) = result

        assertNotNull(response, "Expected response to not be null")
        assertNotNull(error, "Expected error, actual $data")

        val statusCode = 200
        assertThat(error.exception as IOException, isA(IOException::class.java))
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun `Download Big File`() = runBlocking {
        val manager = FuelManager()

        val numberOfBytes = 1024 * 1024 * 10 // 10 MB
        val file = File.createTempFile(numberOfBytes.toString(), null)
        val bytes = ByteArray(numberOfBytes).apply { Random().nextBytes(this) }

        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/bytes"),
            response = mock.response().withDelay(Delay.seconds(1)).withBody(BinaryBody(bytes, MediaType.OCTET_STREAM))
        )

        var read = -1L
        var total = -1L

        val triple = withContext(Dispatchers.IO) {
            manager.download(mock.path("bytes"))
                .fileDestination { _, _ -> file }
                .progress { readBytes, totalBytes -> read = readBytes; total = totalBytes }
                .awaitByteArrayResponseResult()
        }

        assertDownloadedBytesToFile(triple, file, numberOfBytes)
        assertEquals(read == total && read != -1L && total != -1L, true, "Progress read bytes and total bytes should be equal")
    }
}
