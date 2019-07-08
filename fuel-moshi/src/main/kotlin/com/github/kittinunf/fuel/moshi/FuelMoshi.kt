package com.github.kittinunf.fuel.moshi

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.Okio
import java.io.InputStream
import java.lang.reflect.Type

val defaultMoshi = Moshi.Builder()

suspend inline fun <reified T : Any> Request.awaitResponseResultObject() =
    awaitResponseResult(moshiDeserializerOf(T::class.java))

suspend inline fun <reified T : Any> Request.awaitResponseResultObjects() =
        awaitResponseResult(moshiDeserializerOf<List<T>>(
                Types.newParameterizedType(List::class.java, T::class.java))
        )

fun <T : Any> moshiDeserializerOf(clazz: Class<T>) = object : ResponseDeserializable<T> {
    override fun deserialize(content: String): T? = defaultMoshi
            .build()
            .adapter(clazz)
            .fromJson(content)
}

fun <T: Any> moshiDeserializerOf(type: Type) = object : ResponseDeserializable<T> {
    override fun deserialize(content: String): T? = defaultMoshi
            .build()
            .adapter<T>(type)
            .fromJson(content)
}

inline fun <reified T : Any> moshiDeserializerOf(adapter: JsonAdapter<T>) = object : ResponseDeserializable<T> {
    override fun deserialize(content: String): T? = adapter.fromJson(content)

    override fun deserialize(inputStream: InputStream): T? = adapter.fromJson(Okio.buffer(Okio.source(inputStream)))
}
