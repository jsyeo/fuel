package com.github.kittinunf.fuel

import com.github.kittinunf.fuel.core.Client
import com.github.kittinunf.fuel.core.requests.DefaultBody
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.awaitByteArrayResponseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.File
import java.nio.charset.Charset
import kotlin.test.assertNotNull

class CustomClientTest {
    private val manager: FuelManager by lazy {
        val dir = System.getProperty("user.dir")
        val currentDir = File(dir, "src/test/assets")
        val mockJson = File(currentDir, "mock.json")

        FuelManager().apply {
            client = object : Client {
                override suspend fun executeRequest(request: Request): Response = Response(
                    url = request.url,
                    body = DefaultBody.from({ mockJson.inputStream() }, null),
                    statusCode = 200
                )
            }
        }
    }

    @Test
    fun httpRequestWithMockedResponse() = runBlocking {
        val (response, data) = withContext(Dispatchers.IO) {
            manager.request(Method.GET, "http://foo.bar").awaitByteArrayResponseResult()
        }
        assertNotNull(response)
        assertNotNull(data.get())
        assertThat(data.get().toString(Charset.defaultCharset()), containsString("key"))
        assertThat(data.get().toString(Charset.defaultCharset()), containsString("value"))
    }
}