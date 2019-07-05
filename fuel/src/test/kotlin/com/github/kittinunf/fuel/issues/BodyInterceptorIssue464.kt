package com.github.kittinunf.fuel.issues

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import com.github.kittinunf.fuel.test.MockReflected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BodyInterceptorIssue464 : MockHttpTestCase() {

    private val threadSafeManager = FuelManager()
    private val bodyInterceptor = { next: (Request) -> Request ->
        { request: Request ->
            val body = request.body.toByteArray()
            // make transformations based on the original request/body
            val transformed = request.body(body.reversedArray())
                    .header("Body-Interceptor", "Intercepted")
            next(transformed)
        }
    }

    @BeforeTest
    fun addBodyInterceptor() {
        threadSafeManager.addRequestInterceptor(bodyInterceptor)
    }

    @AfterTest
    fun removeBodyInterceptor() {
        threadSafeManager.removeRequestInterceptor(bodyInterceptor)
    }

    @Test
    fun getBodyInInterceptor() = runBlocking {
        val value = "foobarbaz"
        val request = reflectedRequest(Method.POST, "intercepted-body", manager = threadSafeManager)
            .body(value)

        val (_, result) = withContext(Dispatchers.IO) {
            request.awaitResponseResult(MockReflected.Deserializer())
        }
        val (reflected, error) = result
        assertNull(error)
        assertNotNull(reflected)
        assertEquals(reflected["Body-Interceptor"].firstOrNull(), "Intercepted")
        assertEquals(reflected.body?.string, value.reversed())
    }
}
