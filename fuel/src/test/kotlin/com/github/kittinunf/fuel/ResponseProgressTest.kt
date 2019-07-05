package com.github.kittinunf.fuel

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.awaitStringResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import com.google.common.net.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.mockserver.model.BinaryBody
import org.mockserver.model.Delay
import java.io.File
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ResponseProgressTest : MockHttpTestCase() {

    private val threadSafeFuel = FuelManager()

    @Test
    fun reportsResponseProgressWithDownload() = runBlocking {
        val length = threadSafeFuel.progressBufferSize * 8
        val file = File.createTempFile(length.toString(), null)
        val downloadData = ByteArray(length).apply { Random().nextBytes(this) }

        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/download"),
            response = mock.response().withDelay(Delay.seconds(1)).withBody(BinaryBody(downloadData, MediaType.OCTET_STREAM))
        )

        var progressCalls = 0

        val (response, result) = withContext(Dispatchers.IO) {
            threadSafeFuel.download(mock.path("download"))
                    .fileDestination { _, _ -> file.also { println("Downloading $length bytes to file") } }
                    .progress { _, _ -> progressCalls += 1 }
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNotNull(data, "Expected data, actual error $error")

        // Probably around 9 (8 times a buffer write, and the final closing -1 write)
        assertEquals(
                progressCalls > length / threadSafeFuel.progressBufferSize,
                true,
                "Expected progress to be called at least (total size/buffer size), actual $progressCalls calls"
        )
    }

    @Test
    fun reportsResponseProgressWithGenericGet() = runBlocking {
        val length = threadSafeFuel.progressBufferSize * 8 - 200
        val downloadData = ByteArray(length).apply { Random().nextBytes(this) }

        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/download"),
            response = mock.response().withDelay(Delay.seconds(1)).withBody(BinaryBody(downloadData, MediaType.OCTET_STREAM))
        )

        var progressCalls = 0

        val (response, result) = withContext(Dispatchers.IO) {
            threadSafeFuel.request(Method.GET, mock.path("download"))
                    .responseProgress { _, _ -> progressCalls += 1 }
                    .also { println("Downloading $length bytes to memory") }
                    .awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNotNull(data, "Expected data, actual error $error")

        // Probably around 9 (8 times a buffer write, and the final closing -1 write)
        assertEquals(
                progressCalls > length / threadSafeFuel.progressBufferSize,
                true,
                "Expected progress to be called at least (total size/buffer size), actual $progressCalls calls"
        )
    }
}
