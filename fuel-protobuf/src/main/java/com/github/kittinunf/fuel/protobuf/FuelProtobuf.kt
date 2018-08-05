package com.github.kittinunf.fuel.protobuf

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.response
import com.github.kittinunf.fuel.util.FuelRouting
import com.google.protobuf.MessageLite
import com.google.protobuf.Parser
import java.io.InputStream

// Request
fun FuelManager.request(path: String, builder: MessageLite.Builder) =
        request(Method.POST, path, null).apply {
            // add necessary header
            header(mapOf("Content-Type" to "application/protobuf", "Accept" to "application/protobuf"))
            // add body
            val body = builder.build().toByteArray()
            body(body)
        }

fun FuelManager.request(pathStringConvertible: Fuel.PathStringConvertible, builder: MessageLite.Builder) =
        request(pathStringConvertible.path, builder)

// FuelRouting
interface FuelProtobufRouting : FuelRouting {

    override val method: Method
        get() = Method.POST

    override val request: Request
        get() = super.request
                .header("Content-Type" to "application/protobuf", "Accept" to "application/protobuf")

    override val params: List<Pair<String, Any?>>?
        get() = null
}

// Response
inline fun <reified T : MessageLite> protobufDeserializerOf(parser: Parser<T>): ResponseDeserializable<T> =
        object : ResponseDeserializable<T> {
            override fun deserialize(inputStream: InputStream) = parser.parseFrom(inputStream) as T
        }

inline fun <reified T : MessageLite> Request.responseObject(parser: Parser<T>) = response(protobufDeserializerOf(parser))
