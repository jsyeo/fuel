package com.github.kittinunf.fuel

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.core.awaitStringResponseResult
import com.github.kittinunf.fuel.core.interceptors.LogRequestAsCurlInterceptor
import com.github.kittinunf.fuel.core.interceptors.LogRequestInterceptor
import com.github.kittinunf.fuel.core.interceptors.LogResponseInterceptor
import com.github.kittinunf.fuel.test.MockHttpTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.HttpURLConnection
import kotlin.test.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InterceptorTest : MockHttpTestCase() {

    private val outContent = ByteArrayOutputStream()
    private val errContent = ByteArrayOutputStream()
    private val originalOut = System.out
    private val originalErr = System.err

    @BeforeTest
    fun prepareStream() {
        System.setOut(PrintStream(outContent))
        System.setErr(PrintStream(errContent))
    }

    @AfterTest
    fun tearDownStreams() {
        System.setOut(originalOut)
        System.setErr(originalErr)
    }

    @Test
    fun testWithNoInterceptor() = runBlocking {
        val httpRequest = mock.request()
            .withMethod(Method.GET.value)
            .withPath("/get")

        mock.chain(request = httpRequest, response = mock.reflect())

        val manager = FuelManager()
        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("get")).awaitByteArrayResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        assertThat("Expected response not to be logged", outContent.toString(), not(containsString(response.toString())))

        assertEquals(response.statusCode, HttpURLConnection.HTTP_OK)
    }

    @Test
    fun testWithLoggingRequestInterceptor() = runBlocking {
        val httpRequest = mock.request()
            .withMethod(Method.GET.value)
            .withPath("/get")

        mock.chain(request = httpRequest, response = mock.reflect())

        val manager = FuelManager()
        manager.addRequestInterceptor(LogRequestInterceptor)

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("get")).awaitByteArrayResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        assertThat("Expected response not to be logged", outContent.toString(), not(containsString(response.toString())))

        assertEquals(response.statusCode, HttpURLConnection.HTTP_OK)
        manager.removeRequestInterceptor(LogRequestInterceptor)
        Unit // some how, this is make Junit happy.
    }

    @Test
    fun testWithLoggingResponseInterceptor() = runBlocking {
        val httpRequest = mock.request()
            .withMethod(Method.GET.value)
            .withPath("/get")

        mock.chain(request = httpRequest, response = mock.reflect())

        val manager = FuelManager()
        manager.addResponseInterceptor(LogResponseInterceptor)

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("get")).awaitByteArrayResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        assertThat("Expected response to be logged", outContent.toString(), containsString(response.toString()))

        assertEquals(response.statusCode, HttpURLConnection.HTTP_OK)
        manager.removeResponseInterceptor(LogResponseInterceptor)
        Unit
    }

    @Test
    fun testWithResponseToString() = runBlocking {
        val httpRequest = mock.request()
            .withMethod(Method.GET.value)
            .withPath("/get")

        mock.chain(request = httpRequest, response = mock.reflect())

        val manager = FuelManager()
        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("get")).awaitByteArrayResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        assertEquals(response.statusCode, HttpURLConnection.HTTP_OK)

        assertThat(response.toString(), containsString("Response :"))
        assertThat(response.toString(), containsString("Length :"))
        assertThat(response.toString(), containsString("Body :"))
        assertThat(response.toString(), containsString("Headers :"))
    }

    @Test
    fun testWithMultipleInterceptors() = runBlocking {
        val httpRequest = mock.request()
            .withMethod(Method.GET.value)
            .withPath("/get")

        mock.chain(request = httpRequest, response = mock.reflect())

        val manager = FuelManager()

        var interceptorCalled = false

        fun <T> customLoggingInterceptor() = { next: (T) -> T ->
            { t: T ->
                println("1: $t")
                interceptorCalled = true
                next(t)
            }
        }

        manager.apply {
            addRequestInterceptor(LogRequestAsCurlInterceptor)
            addRequestInterceptor(customLoggingInterceptor())
        }

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("get")).header(mapOf("User-Agent" to "Fuel")).awaitByteArrayResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        assertEquals(response.statusCode, HttpURLConnection.HTTP_OK)
        assertEquals(interceptorCalled, true)
    }

    @Test
    fun testWithBreakingChainInterceptor() = runBlocking {
        val httpRequest = mock.request()
            .withMethod(Method.GET.value)
            .withPath("/get")

        mock.chain(request = httpRequest, response = mock.reflect())

        val manager = FuelManager()

        var interceptorCalled = false

        @Suppress("RedundantLambdaArrow")
        fun <T> customLoggingBreakingInterceptor() = { _: (T) -> T ->
            { t: T ->
                println("1: $t")
                interceptorCalled = true
                // if next is not called, next Interceptor will not be called as well
                t
            }
        }

        var interceptorNotCalled = true
        fun <T> customLoggingInterceptor() = { next: (T) -> T ->
            { t: T ->
                println("1: $t")
                interceptorNotCalled = false
                next(t)
            }
        }

        manager.apply {
            addRequestInterceptor(LogRequestAsCurlInterceptor)
            addRequestInterceptor(customLoggingBreakingInterceptor())
            addRequestInterceptor(customLoggingInterceptor())
        }

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("get")).header(mapOf("User-Agent" to "Fuel")).awaitByteArrayResponseResult()
        }
        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        assertEquals(response.statusCode, HttpURLConnection.HTTP_OK)
        assertEquals(interceptorCalled, true)
        assertEquals(interceptorNotCalled, true)
    }

    @Test
    fun testWithoutDefaultRedirectionInterceptor() = runBlocking {
        val firstRequest = mock.request()
            .withMethod(Method.GET.value)
            .withPath("/redirect")

        val firstResponse = mock.response()
            .withHeader("Location", mock.path("redirected"))
            .withStatusCode(HttpURLConnection.HTTP_MOVED_TEMP)

        mock.chain(request = firstRequest, response = firstResponse)

        val manager = FuelManager()
        manager.addRequestInterceptor(LogRequestAsCurlInterceptor)
        manager.removeAllResponseInterceptors()

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("redirect")).header(mapOf("User-Agent" to "Fuel")).awaitByteArrayResponseResult()
        }

        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        assertEquals(response.statusCode, HttpURLConnection.HTTP_MOVED_TEMP)
    }

    @Test
    fun testHttpExceptionWithRemoveValidator() = runBlocking {
        val firstRequest = mock.request()
            .withMethod(Method.GET.value)
            .withPath("/invalid")

        val firstResponse = mock.response()
            .withStatusCode(418) // I'm a teapot

        mock.chain(request = firstRequest, response = firstResponse)

        val manager = FuelManager()

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("invalid"))
                    .validate { true }
                    .awaitStringResponseResult()
        }

        val (data, error) = result
        assertNotNull(response)
        assertNull(error)
        assertNotNull(data)

        assertEquals(response.statusCode, 418)
    }

    @Test
    fun failsIfRequestedResourceReturns404() = runBlocking {
        val firstRequest = mock.request()
            .withMethod(Method.GET.value)
            .withPath("/not-found")

        val firstResponse = mock.response()
            .withStatusCode(HttpURLConnection.HTTP_NOT_FOUND)

        mock.chain(request = firstRequest, response = firstResponse)

        val manager = FuelManager()
        val (_, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("not-found")).awaitByteArrayResponseResult()
        }
        val (data, error) = result
        assertNotNull(error)
        assertNull(data)
    }

    @Test
    fun testGetNotModified() = runBlocking {
        val firstRequest = mock.request()
            .withMethod(Method.GET.value)
            .withPath("/not-modified")

        val firstResponse = mock.response()
            .withStatusCode(HttpURLConnection.HTTP_NOT_MODIFIED)

        mock.chain(request = firstRequest, response = firstResponse)
        val manager = FuelManager()
        val (_, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("not-modified")).awaitStringResponseResult()
        }
        val (data, error) = result
        assertNotNull(data)
        assertNull(error)
    }

    @Test
    fun testRemoveAllRequestInterceptors() = runBlocking {
        val firstRequest = mock.request()
            .withMethod(Method.GET.value)
            .withPath("/teapot")

        val firstResponse = mock.response()
            .withStatusCode(418)

        mock.chain(request = firstRequest, response = firstResponse)

        val manager = FuelManager()
        manager.removeAllRequestInterceptors()

        val (response, result) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, mock.path("teapot")).awaitStringResponseResult()
        }

        val (data, error) = result
        assertNotNull(response)
        assertNotNull(error)
        assertNull(data)

        assertEquals(response.statusCode, 418)
    }
}
