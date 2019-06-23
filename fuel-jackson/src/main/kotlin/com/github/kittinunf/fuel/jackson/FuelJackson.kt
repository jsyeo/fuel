package com.github.kittinunf.fuel.jackson

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.awaitResponseResult
import java.io.InputStream
import java.io.Reader

val defaultMapper: ObjectMapper? = ObjectMapper().registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

suspend inline fun <reified T : Any> Request.awaitResponseResultObject() =
    awaitResponseResult(jacksonDeserializerOf<T>())

suspend inline fun <reified T : Any> Request.awaitResponseResultObject(mapper: ObjectMapper) =
    awaitResponseResult(jacksonDeserializerOf<T>(mapper))

inline fun <reified T : Any> jacksonDeserializerOf(mapper: ObjectMapper? = defaultMapper) = object : ResponseDeserializable<T> {
    override fun deserialize(reader: Reader): T? = mapper?.readValue(reader)
    override fun deserialize(content: String): T? = mapper?.readValue(content)
    override fun deserialize(bytes: ByteArray): T? = mapper?.readValue(bytes)
    override fun deserialize(inputStream: InputStream): T? = mapper?.readValue(inputStream)
}