package com.github.kittinunf.fuel.moshi

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.test.MockHttpTestCase
import com.github.kittinunf.result.Result.Failure
import com.github.kittinunf.result.Result.Success
import com.google.common.reflect.TypeToken
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.isA
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Test
import java.net.HttpURLConnection

class FuelMoshiTest : MockHttpTestCase() {
    data class HttpBinUserAgentModel(var userAgent: String = "")

    @Test
    fun `check instance of HttpBinUserAgentModel`() = runBlocking {
        mock.chain(
            request = mock.request().withPath("/user-agent"),
            response = mock.reflect()
        )

        val pair = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("user-agent")).awaitResponseResultObject<HttpBinUserAgentModel>()
        }
        assertNotNull(pair.second.component1())
        assertThat(pair.second.component1(), isA(HttpBinUserAgentModel::class.java))
    }

    @Test
    fun `expect HTTP Not Found to throw error`() = runBlocking {
        mock.chain(
            request = mock.request().withPath("/user-agent"),
            response = mock.response().withStatusCode(HttpURLConnection.HTTP_NOT_FOUND)
        )

        val pair = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("user-agent")).awaitResponseResultObject<HttpBinUserAgentModel>()
        }
        assertThat(pair.second.component2(), isA(FuelError::class.java))
    }

    data class IssueInfo(val id: Int, val title: String, val number: Int)

    @Test
    fun `processing generic list`() = runBlocking {
        mock.chain(
                request = mock.request().withPath("/issues"),
                response = mock.response().withBody("[ " +
                        "{ \"id\": 1, \"title\": \"issue 1\", \"number\": null }, " +
                        "{ \"id\": 2, \"title\": \"issue 2\", \"number\": 32 } " +
                        " ]").withStatusCode(HttpURLConnection.HTTP_OK)
        )
        val (_, result) = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("issues")).awaitResponseResultObject<List<IssueInfo>>()
        }
        val issues = result.get()
        assertEquals(issues.size, 2)
        //TODO: some how Instance Of doesn't work here
    }

    enum class Stage {
        @Json(name = "na")
        UNKNOWN,
        IN_PROGRESS,
        FINISHED
    }

    data class StageDTO(val stage: Stage)

    class StageMoshiAdapter : JsonAdapter<Stage>() {
        override fun fromJson(reader: JsonReader): Stage? {
            val value = reader.nextString()

            return when (value) {
                "na" -> Stage.UNKNOWN
                "in_progress" -> Stage.IN_PROGRESS
                "finished" -> Stage.FINISHED
                else -> error("No supported value")
            }
        }

        override fun toJson(writer: JsonWriter, value: Stage?) {
        }
    }

    @Test
    fun `custom adapter sucesss`() = runBlocking {
        defaultMoshi.add(TypeToken.of(Stage::class.java).type, StageMoshiAdapter())

        mock.apply {
            chain(
                request = mock.request().withPath("/stage1"),
                response = mock.response().withBody(""" { "stage" : "na" } """.trimIndent())
            )
            chain(
                request = mock.request().withPath("/stage2"),
                response = mock.response().withBody(""" { "stage" : "in_progress" } """.trimIndent())
            )
            chain(
                request = mock.request().withPath("/stage3"),
                response = mock.response().withBody(""" { "stage" : "finished" } """.trimIndent())
            )
        }

        val (res, res1) = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("stage1")).awaitResponseResultObject<StageDTO>()
        }
        assertNotNull(res)
        assertThat(res1 as Success, isA(Success::class.java))
        assertEquals(res1.value.stage, Stage.UNKNOWN)

        val (_, res2) = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("stage2")).awaitResponseResultObject<StageDTO>()
        }
        assertThat(res2 as Success, isA(Success::class.java))
        assertEquals(res2.value.stage, Stage.IN_PROGRESS)

        val (_, res3) = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("stage3")).awaitResponseResultObject<StageDTO>()
        }
        assertThat(res3 as Success, isA(Success::class.java))
        assertEquals(res3.value.stage, Stage.FINISHED)
    }

    @Test
    fun `test custom adapter failure`() = runBlocking {
        defaultMoshi.add(TypeToken.of(Stage::class.java).type, StageMoshiAdapter())

        mock.apply {
            chain(
                request = mock.request().withPath("/stage-error"),
                response = mock.response().withBody(""" { "stage" : "abcdef" } """.trimIndent())
            )
        }

        val (res, res1) = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("stage-error")).awaitResponseResultObject<StageDTO>()
        }
        assertNotNull(res)
        assertThat(res1 as Failure, isA(Failure::class.java))
        assertThat(res1.error.exception as IllegalStateException, isA(IllegalStateException::class.java))
    }
}