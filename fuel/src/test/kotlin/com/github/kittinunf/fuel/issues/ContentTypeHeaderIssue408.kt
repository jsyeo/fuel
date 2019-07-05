package com.github.kittinunf.fuel.issues

import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import com.github.kittinunf.fuel.test.MockReflected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ContentTypeHeaderIssue408 : MockHttpTestCase() {

    @Test
    fun headerContentTypePreserved() = runBlocking {
        val tokenId = UUID.randomUUID()
        val request = reflectedRequest(Method.GET, "json/sessions",
            parameters = listOf("_action" to "getSessionInfo", "tokenId" to tokenId))
            .header("Accept-API-Version" to "resource=2.0")
            .header("Content-Type" to "application/json")

        val (_, result) = withContext(Dispatchers.IO) {
            request.awaitResponseResult(MockReflected.Deserializer())
        }
        val (reflected, error) = result

        assertNull(error)
        assertNotNull(reflected)

        val contentType = reflected[Headers.CONTENT_TYPE]
        assertEquals(contentType.lastOrNull(), "application/json")
        assertEquals(contentType.size, 1)
    }
}
