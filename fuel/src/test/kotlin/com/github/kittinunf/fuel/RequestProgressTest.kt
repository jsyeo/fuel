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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RequestProgressTest : MockHttpTestCase() {
    private val currentDir: File by lazy {
        val dir = System.getProperty("user.dir")
        File(dir, "src/test/assets")
    }

    private val threadSafeFuel = FuelManager()

    @Test
    fun reportsRequestProgressWithUpload() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/upload"),
            response = mock.response().withStatusCode(HttpURLConnection.HTTP_ACCEPTED)
        )

        val file = File(currentDir, "lorem_ipsum_long.tmp")
        val length = file.length()
        var progressCalls = 0
        var expectedLength = 0L

        val (response, result) = withContext(Dispatchers.IO) {
            threadSafeFuel.upload(mock.path("upload"))
                    .add(FileDataPart(file))
                    .progress { _, _ -> progressCalls += 1 }
                    .also { expectedLength = it.body.length!! }
                    .also { println("Request body is $expectedLength bytes ($length bytes of file data)") }
                    .awaitStringResponseResult()
        }

        val (data, error) = result
        assertNotNull(response)
        assertNotNull(data, "Expected data, actual error $error")

        // Probably 3: 2 flushes to write, 1 after it completes
        assertEquals(progressCalls > expectedLength / threadSafeFuel.progressBufferSize,
            true, "Expected progress to be called at least (total size/buffer size), actual $progressCalls calls"
        )
    }

    @Test
    fun reportsRequestProgressWithGenericPost() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/upload"),
            response = mock.response().withStatusCode(HttpURLConnection.HTTP_ACCEPTED)
        )

        val file = File(currentDir, "lorem_ipsum_long.tmp")
        val length = file.length()
        var progressCalls = 0

        val (response, result) = withContext(Dispatchers.IO) {
            threadSafeFuel.request(Method.POST, mock.path("upload"))
                    .body(file.also { println("Uploading $length bytes") })
                    .requestProgress { _, _ -> progressCalls += 1 }
                    .awaitStringResponseResult()
        }

        val (data, error) = result
        assertNotNull(response)
        assertNotNull(data, "Expected data, actual error $error")

        // Probably 2, as the body is written as a whole (per buffer size)
        assertEquals(progressCalls > length / threadSafeFuel.progressBufferSize,
                true, "Expected progress to be called at least (total size/buffer size), actual $progressCalls calls"
        )
    }
}
