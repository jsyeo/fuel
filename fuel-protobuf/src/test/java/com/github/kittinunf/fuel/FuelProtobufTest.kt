package com.github.kittinunf.fuel

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.protobuf.request
import com.github.kittinunf.fuel.protobuf.responseObject
import grpc.gateway.examples.examplepb.ABitOfEverythingOuterClass
import org.junit.Test
import java.util.UUID

class FuelProtobufTest {

    private val manager = FuelManager().apply { basePath = "https://grpcb.in" }

    init {
    }

    @Test
    fun testIndex() {
        val uuid = UUID.randomUUID()
        val builder = ABitOfEverythingOuterClass.ABitOfEverything.newBuilder().setUuid(uuid.toString())
        val (req, res, result) =
                manager.request("v1/example/a_bit_of_everythin", builder)
                        .responseObject(ABitOfEverythingOuterClass.ABitOfEverything.parser())
        println(req)
        println(res)
        println(result)
    }
}
