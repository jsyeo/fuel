package com.github.kittinunf.fuel.toolbox

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import com.github.kittinunf.fuel.util.encode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.mockserver.model.Header
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TransferEncodingTest : MockHttpTestCase() {

    @Test
    fun identityTransferEncodingTest() = runBlocking {
        val value = ByteArray(32).apply {
            for (i in 0 until this.size) {
                this[i] = ('A'..'z').random().toByte()
            }
        }

        val identity = ByteArrayOutputStream(value.size).apply {
            write(value)
        }.toByteArray()

        mock.chain(
            request = mock.request()
                .withMethod(Method.POST.value)
                .withPath("/identity")
                .withHeader(Header.header(Headers.ACCEPT_TRANSFER_ENCODING, "gzip, deflate; q=0.5")),
            response = mock.response()
                .withHeaders(
                    Header.header(Headers.TRANSFER_ENCODING, "identity"),
                    Header.header(Headers.CONTENT_LENGTH, value.size.toString())
                )
                .withBody(identity)
        )

        val (result, response) = withContext(Dispatchers.IO) {
            Fuel.request(Method.POST, mock.path("identity"))
                    .body(value)
                    .awaitByteArrayResponseResult()
        }

        assertArrayEquals(value, response.component1())
        assertNull(result[Headers.CONTENT_ENCODING].lastOrNull())
        assertEquals(result[Headers.CONTENT_LENGTH].lastOrNull(), value.size.toString())
    }

    @Test
    fun gzipTransferEncodingTest() = runBlocking {
        val value = ByteArray(32).apply {
            for (i in 0 until this.size) {
                this[i] = ('A'..'z').random().toByte()
            }
        }

        val inner = ByteArrayOutputStream(value.size)
        inner.encode("gzip").apply {
            write(value)
            close()
        }

        // It's written to here
        val gzipped = inner.toByteArray()

        mock.chain(
            request = mock.request()
                .withMethod(Method.POST.value)
                .withPath("/gzip")
                .withHeader(Header.header(Headers.ACCEPT_TRANSFER_ENCODING, "gzip, deflate; q=0.5")),
            response = mock.response()
                .withHeaders(
                    Header.header(Headers.TRANSFER_ENCODING, "gzip"),
                    Header.header(Headers.CONTENT_LENGTH, value.size.toString())
                )
                .withBody(gzipped)
        )

        val (result, response) = withContext(Dispatchers.IO) {
            Fuel.request(Method.POST, mock.path("gzip"))
                    .body(value)
                    .awaitByteArrayResponseResult()
        }
        val (data, error) = response
        assertNotNull(data, "Expected data, actual error $error")
        assertArrayEquals(value, data)
        assertNull(result[Headers.CONTENT_ENCODING].lastOrNull())
        assertNull(result[Headers.CONTENT_LENGTH].lastOrNull())
    }

    @Test
    fun deflateTransferEncodingTest() = runBlocking {
        val value = ByteArray(32).apply {
            for (i in 0 until this.size) {
                this[i] = ('A'..'z').random().toByte()
            }
        }

        val inner = ByteArrayOutputStream(value.size)
        inner.encode("deflate").apply {
            write(value)
            close()
        }

        // It's written to here
        val gzipped = inner.toByteArray()

        mock.chain(
            request = mock.request()
                .withMethod(Method.POST.value)
                .withPath("/deflate")
                .withHeader(Header.header(Headers.ACCEPT_TRANSFER_ENCODING, "gzip, deflate; q=0.5")),
            response = mock.response()
                .withHeaders(
                    Header.header(Headers.TRANSFER_ENCODING, "deflate"),
                    Header.header(Headers.CONTENT_LENGTH, value.size.toString())
                )
                .withBody(gzipped)
        )

        val (result, response) = withContext(Dispatchers.IO) {
            Fuel.request(Method.POST, mock.path("deflate"))
                    .body(value)
                    .awaitByteArrayResponseResult()
        }
        val (data, error) = response
        assertNotNull(data, "Expected data, actual error $error")
        assertArrayEquals(value, data)
        assertNull(result[Headers.CONTENT_ENCODING].lastOrNull())
        assertNull(result[Headers.CONTENT_LENGTH].lastOrNull())
    }

    @Test
    fun stackedTransferEncodingTest() = runBlocking {
        val value = ByteArray(32).apply {
            for (i in 0 until this.size) {
                this[i] = ('A'..'z').random().toByte()
            }
        }

        val innerData = ByteArrayOutputStream(value.size * 2)
        innerData.encode("gzip").apply {
            write(value)
            close()
        }

        val outputData = ByteArrayOutputStream(value.size * 2)
        outputData.encode("gzip").apply {
            write(innerData.toByteArray())
            close()
        }

        // It's written to here
        val gzipped = outputData.toByteArray()

        mock.chain(
            request = mock.request()
                .withMethod(Method.POST.value)
                .withPath("/stacked")
                .withHeader(Header.header(Headers.ACCEPT_TRANSFER_ENCODING, "gzip, deflate; q=0.5")),
            response = mock.response()
                .withHeaders(
                    Header.header(Headers.TRANSFER_ENCODING, "gzip, gzip"),
                    Header.header(Headers.CONTENT_LENGTH, value.size.toString())
                )
                .withBody(gzipped)
        )

        val (result, response) = withContext(Dispatchers.IO) {
            Fuel.request(Method.POST, mock.path("stacked"))
                    .body(value)
                    .awaitByteArrayResponseResult()
        }

        val (data, error) = response
        assertNotNull(data, "Expected data, actual error $error")
        assertArrayEquals(value, data)
        assertNull(result[Headers.CONTENT_ENCODING].lastOrNull())
        assertNull(result[Headers.CONTENT_LENGTH].lastOrNull())
    }
}