package com.github.kittinunf.fuel.core.requests

import com.github.kittinunf.fuel.core.Method
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL

class RepeatableBodyTest {

    @Test
    fun `repeatable Body Is Never Consumed`() {
        val body = DefaultBody.from({ ByteArrayInputStream("body".toByteArray()) }, { 4 }).asRepeatable()
        assertEquals(body.isConsumed(), false)
        body.writeTo(ByteArrayOutputStream())
        assertEquals(body.isConsumed(), false)
    }

    @Test
    fun `Repeatable Byte Array Body`() {
        val value = ByteArray(32).apply {
            for (i in 0 until this.size) {
                this[i] = ('A'..'z').random().toByte()
            }
        }

        DefaultRequest(Method.POST, URL("https://test.fuel.com/"))
            .body(value)
            .apply {
                //println(body.asString(null))
                val output = ByteArrayOutputStream(value.size)
                assertEquals(body.length?.toInt(), value.size)
                assertEquals(body.toByteArray(), value)

                body.writeTo(output)
                assertEquals(output.toString(), String(value))
                assertEquals(body.isConsumed(), false)
            }
    }

    @Test
    fun `Repeatable String Body`() {
        val value = "body"
        DefaultRequest(Method.POST, URL("https://test.fuel.com/"))
            .body(value)
            .apply {
                //println(body.asString(null))
                val output = ByteArrayOutputStream(value.length)
                assertEquals(body.length?.toInt(), value.length)
                assertEquals(body.toByteArray(), value.toByteArray())

                body.writeTo(output)
                assertEquals(output.toString(), value)
                assertEquals(body.isConsumed(), false)
            }
    }

    @Test
    fun `request With RepeatableBody Is Printable After Consumption`() {
        val value = "String Body ${Math.random()}"

        DefaultRequest(Method.POST, URL("https://test.fuel.com/"))
            .body(value)
            .apply {
                val output = ByteArrayOutputStream()
                body.writeTo(output)

                assertThat(this.toString(), CoreMatchers.containsString(value))
            }
    }
}