package com.github.kittinunf.fuel.issues

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import com.github.kittinunf.fuel.test.MockReflected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockserver.model.Header.header

class DeleteIssue306 : MockHttpTestCase() {

    companion object {
        const val PERSISTENT_MENU = "persistent_menu"
    }

    @Test
    fun itCorrectlySendsTheBody() = runBlocking {
        val version = 3
        val uri = "$version/me/messenger_profile"

        val root = "{ \"fields\": [\"$PERSISTENT_MENU\"] }"

        val request = Fuel.delete(mock.path(uri), parameters = listOf("access_token" to "730161810405329|J5eZMzywkpHjjeQKtpbgN-Eq0tQ"))
            .header(mapOf(Headers.CONTENT_TYPE to "application/json"))
            .body(root)

        mock.chain(
            request = mock.request()
                .withMethod(Method.DELETE.value)
                .withPath("/$uri")
                .withHeader(header(Headers.CONTENT_TYPE, "application/json")),
            response = mock.reflect()
        )

        val (_, result) = withContext(Dispatchers.IO) {
            request.awaitResponseResult(MockReflected.Deserializer())
        }
        val (reflected, error) = result
        assertNull(error)
        assertNotNull(reflected)
        assertEquals(reflected!!.body?.string ?: "(no body)", root)
    }
}
