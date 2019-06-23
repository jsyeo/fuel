package com.github.kittinunf.fuel.core

import com.github.kittinunf.fuel.core.deserializers.ByteArrayDeserializer
import com.github.kittinunf.fuel.core.deserializers.StringDeserializer
import com.github.kittinunf.fuel.core.requests.DefaultBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrElse
import com.github.kittinunf.result.map
import com.github.kittinunf.result.mapError
import java.io.InputStream
import java.io.Reader
import java.nio.charset.Charset

/**
 * Generic interface for [Response] deserialization.
 *
 * @note you are responsible of using the [Response] [Body] [InputStream] and closing it when you're done. Failing to do
 *   so can result in hanging connections if used in conjunction with [com.github.kittinunf.fuel.toolbox.HttpClient].
 *
 * @see ResponseDeserializable
 */
interface Deserializable<out T : Any> {

    /**
     * Deserialize [response] into [T]
     *
     * @param response [Response] the incoming response
     * @return [T] the instance of [T]
     */
    fun deserialize(response: Response): T
}

interface ResponseDeserializable<out T : Any> : Deserializable<T> {
    override fun deserialize(response: Response): T {
        response.body.toStream().use { stream ->
            return deserialize(stream)
                ?: deserialize(stream.reader())
                ?: reserialize(response, stream).let {
                    deserialize(response.data)
                        ?: deserialize(String(response.data))
                        ?: throw FuelError.wrap(IllegalStateException(
                            "One of deserialize(ByteArray) or deserialize(InputStream) or deserialize(Reader) or " +
                                "deserialize(String) must be implemented"
                        ))
                }
        }
    }

    private fun reserialize(response: Response, stream: InputStream): Response {
        val length = response.body.length
        response.body = DefaultBody.from({ stream }, length?.let { l -> { l } })
        return response
    }

    /**
     * Deserialize into [T] from an [InputStream]
     *
     * @param inputStream [InputStream] source bytes
     * @return [T] deserialized instance of [T] or null when not applied
     */
    fun deserialize(inputStream: InputStream): T? = null

    /**
     * Deserialize into [T] from a [Reader]
     *
     * @param reader [Reader] source bytes
     * @return [T] deserialized instance of [T] or null when not applied
     */
    fun deserialize(reader: Reader): T? = null

    /**
     * Deserialize into [T] from a [ByteArray]
     *
     * @note it is more efficient to implement the [InputStream] variant.
     *
     * @param bytes [ByteArray] source bytes
     * @return [T] deserialized instance of [T] or null when not applied
     */
    fun deserialize(bytes: ByteArray): T? = null

    /**
     * Deserialize into [T] from a [String]
     *
     * @note it is more efficient to implement the [Reader] variant.
     *
     * @param content [String] source bytes
     * @return [T] deserialized instance of [T] or null when not applied
     */
    fun deserialize(content: String): T? = null
}

/**
 * Await [T] or [FuelError]
 * @return [ResponseResultOf<T>] the [ResponseResultOf] of [T]
 */
suspend fun <T : Any, U : Deserializable<T>> Request.awaitResponseResult(deserializable: U): ResponseResultOf<T> {
    val initialResult = suspendable().awaitResult()
    return serializeFor(initialResult, deserializable).let {
            Pair(
                it.fold({ (response, _) -> response }, { error -> error.response }),
                it.map { (_, t) -> t }
            )
        }
}

private fun <T : Any, U : Deserializable<T>> serializeFor(result: Result<Response, FuelError>, deserializable: U) =
    result.map { (it to deserializable.deserialize(it)) }
        .mapError <Pair<Response, T>, Exception, FuelError> { FuelError.wrap(it, result.getOrElse(Response.error())) }

/***
 * Awaits the response as a [ByteArray], with metadata
 *
 * @return [ResponseResultOf] [ByteArray]
 */
suspend inline fun Request.awaitByteArrayResponseResult(): ResponseResultOf<ByteArray> =
    awaitResponseResult(ByteArrayDeserializer())

/***
 * Awaits the response as a [String], with metadata
 *
 * @param charset [Charset] the charset to use for the [String], defaulting to [Charsets.UTF_8]
 *
 * @return [ResponseResultOf] [String]
 */
suspend inline fun Request.awaitStringResponseResult(charset: Charset = Charsets.UTF_8): ResponseResultOf<String> =
    awaitResponseResult(StringDeserializer(charset))