package com.github.kittinunf.fuel.core

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection

interface Client {
    suspend fun executeRequest(request: Request): Response

    interface Hook {
        fun preConnect(connection: HttpURLConnection, request: Request)
        fun postConnect(request: Request)
        fun interpretResponseStream(request: Request, inputStream: InputStream): InputStream
        fun httpExchangeFailed(request: Request, exception: IOException)
    }
}
