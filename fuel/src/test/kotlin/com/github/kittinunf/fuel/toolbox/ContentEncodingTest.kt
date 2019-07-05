package com.github.kittinunf.fuel.toolbox

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertArrayEquals
import org.mockserver.model.Header
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ContentEncodingTest : MockHttpTestCase() {
    @Test
    fun gzipContentEncodingTest() = runBlocking {
        val value = ByteArray(32).apply {
            for (i in 0 until this.size) {
                this[i] = ('A'..'z').random().toByte()
            }
        }

        val inner = ByteArrayOutputStream(value.size)
        val output = GZIPOutputStream(inner)
        output.write(value)
        output.finish()

        // It's written to here
        val gzipped = inner.toByteArray()

        mock.chain(
            request = mock.request()
                .withMethod(Method.POST.value)
                .withPath("/gzip")
                .withHeader(Header.header(Headers.ACCEPT_ENCODING, "gzip")),
            response = mock.response()
                .withHeaders(
                    Header.header(Headers.CONTENT_ENCODING, "gzip"),
                    Header.header(Headers.CONTENT_LENGTH, gzipped.size.toString())
                )
                .withBody(gzipped)
        )

        val (result, response) = withContext(Dispatchers.IO) {
            Fuel.request(Method.POST, mock.path("gzip"))
                    .header(Headers.ACCEPT_ENCODING, "gzip")
                    .body(value)
                    .awaitByteArrayResponseResult()
        }
        val (data, error) = response

        assertNotNull(data, "Expected data to be present, actual error $error")
        assertArrayEquals(value, data)

        assertNull(result[Headers.CONTENT_ENCODING].lastOrNull())
        assertNull(result[Headers.CONTENT_LENGTH].lastOrNull())
    }
}