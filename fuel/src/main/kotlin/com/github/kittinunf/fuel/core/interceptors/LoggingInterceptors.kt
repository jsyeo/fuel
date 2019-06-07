package com.github.kittinunf.fuel.core.interceptors

import com.github.kittinunf.fuel.core.FoldableRequestInterceptor
import com.github.kittinunf.fuel.core.FoldableResponseInterceptor
import com.github.kittinunf.fuel.core.RequestTransformer
import com.github.kittinunf.fuel.core.ResponseTransformer
import com.github.kittinunf.fuel.core.extensions.cUrlString

object LogRequestInterceptor : FoldableRequestInterceptor {
    override fun invoke(next: RequestTransformer): RequestTransformer {
        return { request ->
            println(request)
            next(request)
        }
    }
}

object LogRequestAsCurlInterceptor : FoldableRequestInterceptor {
    override fun invoke(next: RequestTransformer): RequestTransformer {
        return { request ->
            println(request.cUrlString())
            next(request)
        }
    }
}

object LogResponseInterceptor : FoldableResponseInterceptor {
    override fun invoke(next: ResponseTransformer): ResponseTransformer {
        return { request, response ->
            println(response.toString())
            next(request, response)
        }
    }
}
