package com.github.kittinunf.fuel.core

import com.github.kittinunf.fuel.core.requests.DefaultBody
import com.github.kittinunf.fuel.test.MockHttpTestCase
import com.github.kittinunf.fuel.util.decodeBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockserver.model.BinaryBody
import java.io.ByteArrayInputStream
import java.net.URLConnection
import java.util.Random

class BodyRepresentationTest : MockHttpTestCase() {
    private val manager: FuelManager by lazy { FuelManager() }

    @Test
    fun `Empty Body Representation`() {
        assertEquals(
            DefaultBody.from({ ByteArrayInputStream(ByteArray(0)) }, { 0L }).asString("(unknown)"),
            "(empty)"
        )
    }

    @Test
    fun `Unknown Bytes Representation`() = runBlocking {
        val bytes = ByteArray(555 - 16)
            .also { Random().nextBytes(it) }
            .let { ByteArray(16).plus(it) }

        mock.chain(
            request = mock.request().withMethod(Method.GET.value).withPath("/bytes"),
            response = mock.response().withBody(BinaryBody(bytes, null)).withHeader("Content-Type", "")
        )

        val (response, _) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("bytes")).awaitStringResponseResult()
        }
        assertEquals(
            response.body().asString(response[Headers.CONTENT_TYPE].lastOrNull()),
            "(555 bytes of (unknown))"
        )
    }

    @Test
    fun `Guess Content Type`() {
        val decodedImage = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVQYV2NgYAAAAAMAAWgmWQ0AAAAASUVORK5CYII=".decodeBase64()!!

        assertEquals(
            DefaultBody
                .from({ ByteArrayInputStream(decodedImage) }, { decodedImage.size.toLong() })
                .asString(URLConnection.guessContentTypeFromStream(ByteArrayInputStream(decodedImage))),
            "(${decodedImage.size} bytes of image/png)"
        )
    }

    @Test
    fun `Bytes Representation Of OCTET Stream`() {
        val contentTypes = listOf("application/octet-stream")
        val content = ByteArray(555 - 16)
            .also { Random().nextBytes(it) }
            .let { ByteArray(16).plus(it) }

        contentTypes.forEach { contentType ->
            assertEquals(
                DefaultBody
                    .from({ ByteArrayInputStream(content) }, { content.size.toLong() })
                    .asString(contentType),
                "(555 bytes of $contentType)"
            )
        }
    }

    @Test
    fun `Bytes Representation Of Media`() {
        val contentTypes = listOf(
            "image/bmp", "image/gif", "image/jpeg", "image/png", "image/tiff", "image/webp", "image/x-icon",
            "audio/aac", "audio/midi", "audio/x-midi", "audio/ogg", "audio/wav", "audio/webm", "audio/3gpp", "audio/3gpp2",
            "video/mpeg", "video/ogg", "video/webm", "video/x-msvideo", "video/3gpp", "video/3gpp2",
            "font/otf", "font/ttf", "font/woff", "font/woff2"
        )
        val content = ByteArray(555 - 16)
            .also { Random().nextBytes(it) }
            .let { ByteArray(16).plus(it) }

        contentTypes.forEach { contentType ->
            assertEquals(
                DefaultBody
                    .from({ ByteArrayInputStream(content) }, { content.size.toLong() })
                    .asString(contentType),
                "(555 bytes of $contentType)"
            )
        }
    }

    @Test
    fun `Text Representation Of YAML`() {
        val contentTypes = listOf("application/x-yaml", "text/yaml")
        val content = "language: c\n"

        contentTypes.forEach { contentType ->
            assertEquals(
                DefaultBody
                    .from({ ByteArrayInputStream(content.toByteArray()) }, { content.length.toLong() })
                    .asString(contentType),
                content
            )
        }
    }

    @Test
    fun `Text Representation Of XML`() {
        val contentTypes = listOf("application/xml", "application/xhtml+xml", "application/vnd.fuel.test+xml", "image/svg+xml")
        val content = "<html xmlns=\"http://www.w3.org/1999/xhtml\"/>"

        contentTypes.forEach { contentType ->
            assertEquals(
                DefaultBody
                    .from({ ByteArrayInputStream(content.toByteArray()) }, { content.length.toLong() })
                    .asString(contentType),
                content
            )
        }
    }

    @Test
    fun `Text Representation Of JavaScript`() {
        val contentTypes = listOf("application/javascript", "application/typescript", "application/vnd.coffeescript")
        val content = "function test()"

        contentTypes.forEach { contentType ->
            assertEquals(
                DefaultBody
                    .from({ ByteArrayInputStream(content.toByteArray()) }, { content.length.toLong() })
                    .asString(contentType),
                content
            )
        }
    }

    @Test
    fun `Text Representation Of JSON`() {
        val contentTypes = listOf("application/json")
        val content = "{ \"foo\": 42 }"

        contentTypes.forEach { contentType ->
            assertEquals(
                DefaultBody
                    .from({ ByteArrayInputStream(content.toByteArray()) }, { content.length.toLong() })
                    .asString(contentType),
                content
            )
        }
    }

    @Test
    fun `Text Representation Of JSON With UTF8 Charset`() {
        val contentTypes = listOf("application/json; charset=utf-8")
        val content = "{ \"foo\": 42 }"

        contentTypes.forEach { contentType ->
            assertEquals(
                DefaultBody
                    .from({ ByteArrayInputStream(content.toByteArray()) }, { content.length.toLong() })
                    .asString(contentType),
                content
            )
        }
    }

    @Test
    fun `Text Representation Of CSV`() {
        val contentTypes = listOf("text/csv")
        val content = "function test()"

        contentTypes.forEach { contentType ->
            assertEquals(
                DefaultBody
                    .from({ ByteArrayInputStream(content.toByteArray()) }, { content.length.toLong() })
                    .asString(contentType),
                content
            )
        }
    }

    @Test
    fun `Text Representation Of CSV With UTF16be Charset`() {
        val contentTypes = listOf("application/csv; charset=utf-16be")
        val content = String("hello,world!".toByteArray(Charsets.UTF_16BE))

        contentTypes.forEach { contentType ->
            assertEquals(
                DefaultBody
                    .from({ ByteArrayInputStream(content.toByteArray()) }, { content.length.toLong() })
                    .asString(contentType),
                "hello,world!"
            )
        }
    }

    @Test
    fun `TextRepresentation Of Text Types`() {
        val contentTypes = listOf("text/csv", "text/html", "text/calendar", "text/plain", "text/css")
        val content = "maybe invalid but we don't care"

        contentTypes.forEach { contentType ->
            assertEquals(
                DefaultBody
                    .from({ ByteArrayInputStream(content.toByteArray()) }, { content.length.toLong() })
                    .asString(contentType),
                content
            )
        }
    }
}