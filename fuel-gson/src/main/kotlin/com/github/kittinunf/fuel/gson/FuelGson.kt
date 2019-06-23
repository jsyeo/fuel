package com.github.kittinunf.fuel.gson

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Reader

/**
 * Await gets a response object of [T]
 */
suspend inline fun <reified T : Any> Request.awaitResponseResultObject() =
    awaitResponseResult(gsonDeserializer<T>())

/**
 * Generate a [ResponseDeserializable<T>] that can deserialize json of [T]
 *
 * @return [ResponseDeserializable<T>] the deserializer
 */
inline fun <reified T : Any> gsonDeserializerOf(clazz: Class<T>) = object : ResponseDeserializable<T> {
    override fun deserialize(reader: Reader): T? = Gson().fromJson(reader, clazz)
}

inline fun <reified T : Any> gsonDeserializer() = object : ResponseDeserializable<T> {
    override fun deserialize(reader: Reader): T? = Gson().fromJson<T>(reader, object : TypeToken<T>() {}.type)
}

/**
 * Serializes [src] to json and sets the body as application/json
 *
 * @param src [Any] the source to serialize
 * @return [Request] the modified request
 */
inline fun <reified T : Any> Request.jsonBody(src: T) =
    this.jsonBody(Gson().toJson(src, object : TypeToken<T>() {}.type)
        .also { Fuel.trace { "serialized $it" } } as String
    )