package com.github.kittinunf.fuel.serialization

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.awaitResponseResult
import kotlinx.io.InputStream
import kotlinx.io.Reader
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.serializer

suspend inline fun <reified T : Any> Request.awaitResponseResultObject(
    loader: DeserializationStrategy<T>,
    json: Json = Json(JsonConfiguration.Stable)
) = awaitResponseResult(kotlinxDeserializerOf(loader, json))

@ImplicitReflectionSerializer
suspend inline fun <reified T : Any> Request.awaitResponseResultObject(
    json: Json = Json(JsonConfiguration.Stable)
) = awaitResponseResultObject(T::class.serializer(), json)

inline fun <reified T : Any> kotlinxDeserializerOf(
    loader: DeserializationStrategy<T>,
    json: Json = Json(JsonConfiguration.Stable)
) = object : ResponseDeserializable<T> {
    override fun deserialize(content: String): T? = json.parse(loader, content)
    override fun deserialize(reader: Reader): T? = deserialize(reader.readText())
    override fun deserialize(bytes: ByteArray): T? = deserialize(String(bytes))

    override fun deserialize(inputStream: InputStream): T? =
        inputStream.bufferedReader().use {
            return deserialize(it)
        }
}

@ImplicitReflectionSerializer
inline fun <reified T : Any> kotlinxDeserializerOf(
    json: Json = Json(JsonConfiguration.Stable)
) = kotlinxDeserializerOf(T::class.serializer(), json)