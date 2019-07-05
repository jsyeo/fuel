package com.github.kittinunf.fuel.gson

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.test.MockHttpTestCase
import com.github.kittinunf.fuel.test.MockReflected
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.lang.reflect.Type
import java.net.HttpURLConnection

private typealias IssuesList = List<IssueInfo>

private data class IssueInfo(
    val id: Int,
    val title: String,
    val number: Int?
) {
    fun specialMethod() = "$id: $title"
}

private sealed class IssueType {
    object Bug : IssueType()
    object Feature : IssueType()

    companion object : JsonSerializer<IssueType>, JsonDeserializer<IssueType> {
        override fun serialize(src: IssueType, typeOfSrc: Type, context: JsonSerializationContext): JsonElement =
            when (src) {
                is Bug -> JsonPrimitive("bug")
                is Feature -> JsonPrimitive("feature")
            }

        override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): IssueType =
            if (json.isJsonPrimitive && (json as JsonPrimitive).isString) {
                when (json.asString) {
                    "bug" -> Bug
                    "feature" -> Feature
                    else -> throw Error("Not a bug or a feature, what is it?")
                }
            } else {
                throw Error("String expected")
            }
    }
}

private typealias IssueTypeList = List<IssueType>

class FuelGsonTest : MockHttpTestCase() {
    data class HttpBinUserAgentModel(var userAgent: String = "")

    /*@Test
    fun `expect data without errors`() = runBlocking {
        val (_, result) = withContext(Dispatchers.IO) {
            reflectedRequest(Method.GET, "user-agent")
        }
        assertNotNull("Expected data, actual error ${result.component2()}", result.component1())
        assertNotNull("Expected data to have a user agent", result.component1()!!.userAgent)
    }*/

    @Test
    fun `expect HTTP Not Found to throw error`() = runBlocking {
        mock.chain(
            request = mock.request().withPath("/user-agent"),
            response = mock.response().withStatusCode(HttpURLConnection.HTTP_NOT_FOUND)
        )

        val (_, result) = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("user-agent")).awaitResponseResultObject<HttpBinUserAgentModel>()
        }
        val (data, error) = result
        assertNotNull("Expected error, actual data $data", error)
    }

    @Test
    fun `processing generic list`() = runBlocking {
       mock.chain(
            request = mock.request().withPath("/issues"),
            response = mock.response().withBody("[ " +
                "{ \"id\": 1, \"title\": \"issue 1\", \"number\": null }, " +
                "{ \"id\": 2, \"title\": \"issue 2\", \"number\": 32 }, " +
                " ]").withStatusCode(HttpURLConnection.HTTP_OK)
        )

        val (_, result) = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("issues")).awaitResponseResultObject<IssuesList>()
        }
        val (issues, error) = result
        assertNotNull("Expected issues, actual error $error", issues)
        assertEquals(issues!!.size, 3)
        assertEquals(issues.first().specialMethod(), "1: issue 1")
    }

    @Test
    fun `setting up json Body`() = runBlocking {
        val data = listOf(
            IssueInfo(id = 1, title = "issue 1", number = null),
            IssueInfo(id = 2, title = "issue 2", number = 32)
        )
        val (_, result) = withContext(Dispatchers.IO) {
            reflectedRequest(Method.POST, "json-body").jsonBody(data).awaitResponseResultObject<MockReflected>()
        }
        val (reflected, error) = result
        val issues: IssuesList = Gson().fromJson(reflected!!.body!!.string!!, object : TypeToken<IssuesList>() {}.type)
        assertNotNull("Expected issues, actual error $error", issues)
        assertEquals(issues.size, data.size)
        assertEquals(issues.first().specialMethod(), "1: issue 1")
    }

    @Test
    fun `create Custom Gson instance`() = runBlocking {
        val gson = GsonBuilder()
            .registerTypeAdapter(IssueType::class.java, IssueType.Companion)
            .create()

        val data = listOf(
            IssueType.Bug,
            IssueType.Feature
        )

        val (_, result) = withContext(Dispatchers.IO) {
            reflectedRequest(Method.POST, "json-body").jsonBody(data, gson).awaitResponseResultObject<MockReflected>()
        }
        val (reflected, error) = result
        val body = reflected!!.body!!.string!!
        val types: IssueTypeList = gson.fromJson(body, object : TypeToken<IssueTypeList>() {}.type)
        assertNotNull("Expected types, actual error $error", types)
        assertEquals(body, "[\"bug\",\"feature\"]")
        assertEquals(types.size, data.size)
        assertEquals(types.first(), IssueType.Bug)
    }
}