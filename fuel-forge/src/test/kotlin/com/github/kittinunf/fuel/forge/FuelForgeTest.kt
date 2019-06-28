package com.github.kittinunf.fuel.forge

import com.github.kittinunf.forge.core.JSON
import com.github.kittinunf.forge.core.apply
import com.github.kittinunf.forge.core.at
import com.github.kittinunf.forge.core.map
import com.github.kittinunf.forge.core.maybeAt
import com.github.kittinunf.forge.util.create
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.test.MockHttpTestCase
import com.github.kittinunf.result.Result

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

import java.net.HttpURLConnection

class FuelForgeTest : MockHttpTestCase() {
    data class HttpBinUserAgentModel(var userAgent: String = "", var status: String = "")
    data class IssueInfo(val id: Int, val title: String, val number: Int?)

    private val httpBinUserDeserializer = { json: JSON ->
        ::HttpBinUserAgentModel.create
            .map(json at "userAgent")
            .apply(json at "status")
    }

    private val issueInfoDeserializer = { json: JSON ->
        ::IssueInfo.create
            .map(json at "id")
            .apply(json at "title")
            .apply(json maybeAt "number")
    }

    @Test
    fun `Testing to see httpBinUserDeserializer is not null`() = runBlocking {
        mock.chain(
            request = mock.request().withPath("/user-agent"),
            response = mock.reflect()
        )

        val fuel = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("user-agent")).awaitResponseResultObject(httpBinUserDeserializer)
        }

        val (_, result) = fuel
        println(result)
        assertNotNull(result.component1())
        assertNotNull(result.component2())
    }

    @Test
    fun `testing to see httpBinUserDeserializer throws Error`() = runBlocking {
        mock.chain(
            request = mock.request().withPath("/user-agent"),
            response = mock.response().withStatusCode(HttpURLConnection.HTTP_BAD_REQUEST)
        )

        val fuel = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("user-agent")).awaitResponseResultObject(httpBinUserDeserializer)
        }
        val (_, result) = fuel
        println(result)
        assertNotNull(result.component1())
    }
}