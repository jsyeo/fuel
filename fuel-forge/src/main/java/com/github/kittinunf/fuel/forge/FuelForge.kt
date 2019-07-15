package com.github.kittinunf.fuel.forge

import com.github.kittinunf.forge.Forge
import com.github.kittinunf.forge.core.DeserializedResult
import com.github.kittinunf.forge.core.JSON
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.awaitResponseResult

suspend inline fun <reified T : Any> Request.awaitResponseResultObject(noinline deserializer: JSON.() -> DeserializedResult<T>) =
    awaitResponseResult(forgeDeserializerOf(deserializer))

suspend inline fun <reified T : Any> Request.awaitResponseResultObjects(noinline deserializer: JSON.() -> DeserializedResult<T>) =
    awaitResponseResult(forgesDeserializerOf(deserializer))

fun <T : Any> forgeDeserializerOf(deserializer: JSON.() -> DeserializedResult<T>) =
    object : ResponseDeserializable<T> {
        override fun deserialize(content: String): T? =
            when (val result = Forge.modelFromJson(content, deserializer)) {
                is DeserializedResult.Success -> { result.value }
                is DeserializedResult.Failure -> throw result.error
            }
    }

fun <T : Any> forgesDeserializerOf(deserializer: JSON.() -> DeserializedResult<T>) =
    object : ResponseDeserializable<List<T>> {
        override fun deserialize(content: String): List<T>? =
            Forge.modelsFromJson(content, deserializer).map {
                it.get<T>()
            }
}
