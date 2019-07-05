package com.github.kittinunf.fuel.core.interceptors

import com.github.kittinunf.fuel.core.BlobDataPart
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.requests.DefaultRequest
import com.github.kittinunf.fuel.core.requests.upload
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.StringContains.containsString
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.URL

class ParameterEncoderTest {
    @Test
    fun `Encode Request Parameters In URL When Body Disallowed`() {
        val methods = listOf(Method.GET, Method.DELETE, Method.HEAD, Method.OPTIONS, Method.TRACE)
        methods.forEach { method ->
            val testRequest = DefaultRequest(
                method,
                URL("https://test.fuel.com"),
                parameters = listOf("foo" to "bar")
            )

            var executed = false
            ParameterEncoder { request ->
                assertThat(request.url.toExternalForm(), containsString("?"))
                assertThat(request.url.query, containsString("foo=bar"))

                assertThat("Expected parameters to be cleared", request.parameters.isEmpty(), equalTo(true))

                executed = true
                request
            }(testRequest)

            assertThat("Expected encoder \"next\" to be called", executed, equalTo(true))
        }
    }

    @Test
    fun `encode Appends Request Parameters In URL`() {
        val methods = listOf(Method.GET, Method.DELETE, Method.HEAD, Method.OPTIONS, Method.TRACE)
        methods.forEach { method ->
            val testRequest = DefaultRequest(
                method,
                URL("https://test.fuel.com?a=b"),
                parameters = listOf("foo" to "bar")
            )

            var executed = false
            ParameterEncoder { request ->
                assertThat(request.url.toExternalForm(), containsString("?"))
                assertThat(request.url.query, containsString("&"))
                assertThat(request.url.query, containsString("a=b"))
                assertThat(request.url.query, containsString("foo=bar"))

                assertThat("Expected parameters to be cleared", request.parameters.isEmpty(), equalTo(true))

                executed = true
                request
            }(testRequest)

            assertThat("Expected encoder \"next\" to be called", executed, equalTo(true))
        }
    }

    @Test
    fun `encode Request Parameters In Body When Body Allowed`() {
        val methods = listOf(Method.POST, Method.PATCH, Method.PUT)
        methods.forEach { method ->
            val testRequest = DefaultRequest(
                method,
                URL("https://test.fuel.com"),
                parameters = listOf("foo" to "bar")
            )

            var executed = false
            ParameterEncoder { request ->
                assertThat(request.url.toExternalForm(), not(containsString("?")))
                assertThat(request.url.query, not(containsString("foo=bar")))

                val contentType = request[Headers.CONTENT_TYPE].lastOrNull()?.split(';')?.first()
                assertThat(contentType, equalTo("application/x-www-form-urlencoded"))
                assertThat(request.body.asString(contentType), equalTo("foo=bar"))
                assertThat("Expected parameters to be cleared", request.parameters.isEmpty(), equalTo(true))

                executed = true
                request
            }(testRequest)

            assertThat("Expected encoder \"next\" to be called", executed, equalTo(true))
        }
    }

    @Test
    fun `encode Request Parameters In URL When Body Exists`() {
        val methods = listOf(Method.POST, Method.PATCH, Method.PUT)
        methods.forEach { method ->
            val testRequest = DefaultRequest(
                method,
                URL("https://test.fuel.com"),
                parameters = listOf("foo" to "bar")
            ).body("my current body")

            var executed = false
            ParameterEncoder { request ->
                assertThat(request.url.toExternalForm(), containsString("?"))
                assertThat(request.url.query, containsString("foo=bar"))

                val contentType = request[Headers.CONTENT_TYPE].lastOrNull()?.split(';')?.first()
                assertThat(contentType, equalTo("text/plain"))
                assertThat(request.body.asString(contentType), equalTo("my current body"))
                assertThat("Expected parameters to be cleared", request.parameters.isEmpty(), equalTo(true))

                executed = true
                request
            }(testRequest)

            assertThat("Expected encoder \"next\" to be called", executed, equalTo(true))
        }
    }

    @Test
    fun `ignore Parameter Encoding For Multipart Form Data Requests`() {
        val methods = listOf(Method.POST, Method.PATCH, Method.PUT)
        methods.forEach { method ->
            val testRequest = DefaultRequest(
                method,
                URL("https://test.fuel.com"),
                parameters = listOf("foo" to "bar")
            ).header(Headers.CONTENT_TYPE, "multipart/form-data")
            .upload()
            .add { BlobDataPart(ByteArrayInputStream("12345678".toByteArray()), name = "test", contentLength = 8L) }

            var executed = false
            ParameterEncoder { request ->
                assertThat(request.url.toExternalForm(), not(containsString("?")))
                assertThat(request.url.query, not(containsString("foo=bar")))

                val contentType = request[Headers.CONTENT_TYPE].lastOrNull()?.split(';')?.first()
                assertThat(contentType, equalTo("multipart/form-data"))
                val body = String(request.body.toByteArray())
                assertThat(body, containsString("foo"))
                assertThat(body, containsString("bar"))
                assertThat(body, containsString("test"))
                assertThat(body, containsString("12345678"))
                assertThat("Expected parameters not to be cleared", request.parameters.isEmpty(), equalTo(false))

                executed = true
                request
            }(testRequest)

            assertThat("Expected encoder \"next\" to be called", executed, equalTo(true))
        }
    }

    @Test
    fun `encode Multiple Parameters`() {
        val methods = listOf(Method.GET, Method.DELETE, Method.HEAD, Method.OPTIONS, Method.TRACE)
        methods.forEach { method ->
            val testRequest = DefaultRequest(
                method,
                URL("https://test.fuel.com"),
                parameters = listOf("foo" to "bar", "baz" to "q")
            )

            var executed = false
            ParameterEncoder { request ->
                assertThat(request.url.toExternalForm(), containsString("?"))
                assertThat(request.url.query, containsString("foo=bar"))
                assertThat(request.url.query, containsString("baz=q"))

                assertThat("Expected parameters to be cleared", request.parameters.isEmpty(), equalTo(true))

                executed = true
                request
            }(testRequest)

            assertThat("Expected encoder \"next\" to be called", executed, equalTo(true))
        }
    }

    @Test
    fun `encode Parameter Without Value`() {
        val methods = listOf(Method.GET, Method.DELETE, Method.HEAD, Method.OPTIONS, Method.TRACE)
        methods.forEach { method ->
            val testRequest = DefaultRequest(
                method,
                URL("https://test.fuel.com"),
                parameters = listOf("foo" to "bar", "baz" to null, "q" to "")
            )

            var executed = false
            ParameterEncoder { request ->
                assertThat(request.url.toExternalForm(), containsString("?"))
                assertThat(request.url.query, containsString("foo=bar"))
                assertThat(request.url.query, not(containsString("baz")))
                assertThat(request.url.query, containsString("q"))

                assertThat("Expected parameters to be cleared", request.parameters.isEmpty(), equalTo(true))

                executed = true
                request
            }(testRequest)

            assertThat("Expected encoder \"next\" to be called", executed, equalTo(true))
        }
    }

    @Test
    fun `encode Parameters With List Values`() {
        val methods = listOf(Method.GET, Method.DELETE, Method.HEAD, Method.OPTIONS, Method.TRACE)
        methods.forEach { method ->
            val testRequest = DefaultRequest(
                method,
                URL("https://test.fuel.com"),
                parameters = listOf("foo" to "bar", "baz" to listOf("x", "y", "z"))
            )

            var executed = false
            ParameterEncoder { request ->
                assertThat(request.url.toExternalForm(), containsString("?"))
                assertThat(request.url.query, containsString("foo=bar"))
                assertThat(request.url.query, containsString("baz[]=x"))
                assertThat(request.url.query, containsString("baz[]=y"))
                assertThat(request.url.query, containsString("baz[]=z"))

                assertThat("Expected parameters to be cleared", request.parameters.isEmpty(), equalTo(true))

                executed = true
                request
            }(testRequest)

            assertThat("Expected encoder \"next\" to be called", executed, equalTo(true))
        }
    }

    @Test
    fun `encode Parameters With Array Values`() {
        val methods = listOf(Method.GET, Method.DELETE, Method.HEAD, Method.OPTIONS, Method.TRACE)
        methods.forEach { method ->
            val testRequest = DefaultRequest(
                method,
                URL("https://test.fuel.com"),
                parameters = listOf("foo" to "bar", "baz" to arrayOf("x", "y", "z"))
            )

            var executed = false
            ParameterEncoder { request ->
                assertThat(request.url.toExternalForm(), containsString("?"))
                assertThat(request.url.query, containsString("foo=bar"))
                assertThat(request.url.query, containsString("baz[]=x"))
                assertThat(request.url.query, containsString("baz[]=y"))
                assertThat(request.url.query, containsString("baz[]=z"))

                assertThat("Expected parameters to be cleared", request.parameters.isEmpty(), equalTo(true))

                executed = true
                request
            }(testRequest)

            assertThat("Expected encoder \"next\" to be called", executed, equalTo(true))
        }
    }
}