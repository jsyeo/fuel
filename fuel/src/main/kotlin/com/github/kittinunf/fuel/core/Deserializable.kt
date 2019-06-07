package com.github.kittinunf.fuel.core

import com.github.kittinunf.fuel.core.requests.DefaultBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrElse
import com.github.kittinunf.result.map
import com.github.kittinunf.result.mapError
import java.io.InputStream
import java.io.Reader

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
 * Await [T] or throws [FuelError]
 * @return [T] the [T]
 */
@Throws(FuelError::class)
suspend fun <T : Any, U : Deserializable<T>> Request.await(deserializable: U): T {
    val response = suspendable().await()
    return runCatching { deserializable.deserialize(response) }
        .onFailure { throw FuelError.wrap(it, response) }
        .getOrThrow()
}

/**
 * Await [T] or [FuelError]
 * @return [ResponseOf<T>] the [Result] of [T]
 */
@Throws(FuelError::class)
suspend fun <T : Any, U : Deserializable<T>> Request.awaitResponse(deserializable: U): ResponseOf<T> {
    val response = suspendable().await()
    return runCatching { Triple(this, response, deserializable.deserialize(response)) }
        .onFailure { throw FuelError.wrap(it, response) }
        .getOrThrow()
}

/**
 * Await [T] or [FuelError]
 * @return [Result<T>] the [Result] of [T]
 */
suspend fun <T : Any, U : Deserializable<T>> Request.awaitResult(deserializable: U): Result<T, FuelError> {
    val initialResult = suspendable().awaitResult()
    return serializeFor(initialResult, deserializable).map { (_, t) -> t }
}

/**
 * Await [T] or [FuelError]
 * @return [ResponseResultOf<T>] the [ResponseResultOf] of [T]
 */
suspend fun <T : Any, U : Deserializable<T>> Request.awaitResponseResult(deserializable: U): ResponseResultOf<T> {
    val initialResult = suspendable().awaitResult()
    return serializeFor(initialResult, deserializable).let {
            Triple(this,
                it.fold({ (response, _) -> response }, { error -> error.response }),
                it.map { (_, t) -> t }
            )
        }
}

private fun <T : Any, U : Deserializable<T>> serializeFor(result: Result<Response, FuelError>, deserializable: U) =
    result.map { (it to deserializable.deserialize(it)) }
        .mapError <Pair<Response, T>, Exception, FuelError> { FuelError.wrap(it, result.getOrElse(Response.error())) }