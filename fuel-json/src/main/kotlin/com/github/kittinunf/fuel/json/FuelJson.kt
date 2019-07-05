package com.github.kittinunf.fuel.json

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.awaitResponseResult
import org.json.JSONArray
import org.json.JSONObject

class FuelJson(private val content: String) {
    fun obj(): JSONObject = JSONObject(content)
    fun array(): JSONArray = JSONArray(content)
}

suspend fun Request.awaitResponseResultJson() = awaitResponseResult(jsonDeserializer())

fun jsonDeserializer() = object : ResponseDeserializable<FuelJson> {
    override fun deserialize(response: Response): FuelJson = FuelJson(String(response.data))
}
