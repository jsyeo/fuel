package com.github.kittinunf.fuel.issues

import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.test.MockHttpTestCase
import com.github.kittinunf.fuel.test.MockReflected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ContentTypeHeaderIssue473 : MockHttpTestCase() {

    @Test
    fun jsonBodyContentTypeHeader() = runBlocking {
        val value = "{ \"foo\": \"bar\" }"
        val request = reflectedRequest(Method.POST, "json-body")
            .jsonBody(value)

        val (_, result) = withContext(Dispatchers.IO) {
            request.awaitResponseResult(MockReflected.Deserializer())
        }
        val (reflected, error) = result

        assertNull(error)
        assertNotNull(reflected)

        val contentType = reflected[Headers.CONTENT_TYPE]
        assertEquals(contentType.lastOrNull(), "application/json")
        assertEquals(contentType.size, 1)
        assertEquals(reflected.body?.string, value)
    }
}