package com.github.kittinunf.fuel.core

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSocketFactory

typealias RequestTransformer = (Request) -> Request
typealias ResponseTransformer = (Request, Response) -> Response

typealias ResponseValidator = (Response) -> Boolean

data class RequestExecutionOptions(
    val client: Client,
    val socketFactory: SSLSocketFactory? = null,
    val hostnameVerifier: HostnameVerifier? = null,
    val requestTransformer: RequestTransformer,
    var responseTransformer: ResponseTransformer
) {
    val requestProgress: Progress = Progress()
    val responseProgress: Progress = Progress()
    var timeoutInMillisecond: Int = 15_000
    var timeoutReadInMillisecond: Int = 15_000
    var decodeContent: Boolean? = null
    var allowRedirects: Boolean? = null
    var useHttpCache: Boolean? = null
    var responseValidator: ResponseValidator = { response ->
        !(response.isServerError || response.isClientError)
    }

    /**
     * Append a response transformer
     */
    operator fun plusAssign(next: ResponseTransformer) {
        val previous = responseTransformer
        responseTransformer = { request, response -> next(request, previous(request, response)) }
    }
}
