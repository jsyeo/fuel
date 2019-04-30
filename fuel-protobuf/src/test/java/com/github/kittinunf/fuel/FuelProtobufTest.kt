package com.github.kittinunf.fuel

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.protobuf.request
import com.github.kittinunf.fuel.protobuf.responseObject
import grpcbin.Grpcbin.DummyMessage
import org.junit.Test

class FuelProtobufTest {

    private val manager = FuelManager().apply { basePath = "http://grpcb.in:9000" }

    init {
    }

    @Test
    fun grpcbinDummy() {
        val builder = DummyMessage.newBuilder().setFFloat(3.2f)
                .setFString("hello-world").setFInt32(42)

        val (req, res, result) =
                manager.request("dummyUnary", builder)
                        .responseObject(DummyMessage.parser())
        println(req)
        println(res)
        println(result.get().fFloat)
    }
}
