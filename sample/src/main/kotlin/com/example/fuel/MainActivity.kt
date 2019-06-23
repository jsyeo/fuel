package com.example.fuel

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.core.awaitStringResponseResult
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.github.kittinunf.fuel.gson.awaitResponseResultObject
import com.github.kittinunf.fuel.httpDelete
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.fuel.stetho.StethoHook
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.mainAuxText
import kotlinx.android.synthetic.main.activity_main.mainClearButton
import kotlinx.android.synthetic.main.activity_main.mainGoButton
import kotlinx.android.synthetic.main.activity_main.mainResultText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.Reader

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private val job = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FuelManager.instance.apply {
            basePath = "http://httpbin.org"
            baseHeaders = mapOf("Device" to "Android")
            baseParams = listOf("key" to "value")
            hook = StethoHook("Fuel Sample App")
//            addResponseInterceptor { loggingResponseInterceptor() }
        }

        mainGoButton.setOnClickListener {
            execute()
        }

        mainClearButton.setOnClickListener {
            mainResultText.text = ""
            mainAuxText.text = ""
        }
    }

    override fun onPause() {
        job.cancel()
        super.onPause()
    }

    private fun execute() = ioScope.launch {
        httpGet()
        httpPut()
        httpPost()
        httpDelete()
        httpDownload()
        httpUpload()
        httpBasicAuthentication()
        httpListResponseObject()
        httpResponseObject()
        httpGsonResponseObject()
    }

    private suspend fun httpResponseObject() {
        val (_, result) = "https://api.github.com/repos/kittinunf/Fuel/issues/1"
            .httpGet()
            .also { Log.d(TAG, it.cUrlString()) }
            .awaitResponseResult(Issue.Deserializer())
        update(result)
    }


    private suspend fun httpListResponseObject() {
        val (_, result) = "https://api.github.com/repos/kittinunf/Fuel/issues"
            .httpGet()
            .also { Log.d(TAG, it.cUrlString()) }
            .awaitResponseResult(Issue.ListDeserializer())
        update(result)
    }

    private suspend fun httpGsonResponseObject() {
        val (_, result) = "https://api.github.com/repos/kittinunf/Fuel/issues/1"
            .httpGet()
            .also { Log.d(TAG, it.cUrlString()) }
            .awaitResponseResultObject<Issue>()
        update(result)
    }

    private suspend fun httpGet()  {
        val (_, result) = Fuel.get("/get", listOf("foo" to "foo", "bar" to "bar"))
            .also { Log.d(TAG, it.cUrlString()) }
            .awaitStringResponseResult()
        update(result)


        val (_, result2) = "/get"
            .httpGet()
            .also { Log.d(TAG, it.cUrlString()) }
            .awaitStringResponseResult()
        update(result2)
    }

    private suspend fun httpPut() {
        val (_, result) = Fuel.put("/put", listOf("foo" to "foo", "bar" to "bar"))
            .also { Log.d(TAG, it.cUrlString()) }
            .awaitStringResponseResult()
        update(result)

        val (_, result2) = "/put"
            .httpPut(listOf("foo" to "foo", "bar" to "bar"))
            .also { Log.d(TAG, it.cUrlString()) }
            .awaitStringResponseResult()
        update(result2)
    }

    private suspend fun httpPost(){
        val (_, result) = Fuel.post("/post", listOf("foo" to "foo", "bar" to "bar"))
            .also { Log.d(TAG, it.cUrlString()) }
            .awaitStringResponseResult()
        update(result)

        val (_, result2) = "/post"
            .httpPost(listOf("foo" to "foo", "bar" to "bar"))
            .also { Log.d(TAG, it.cUrlString()) }
            .awaitStringResponseResult()
        update(result2)
    }

    private suspend fun httpDelete() {
        val (_, result) = Fuel.delete("/delete", listOf("foo" to "foo", "bar" to "bar"))
            .also { Log.d(TAG, it.cUrlString()) }
            .awaitStringResponseResult()
        update(result)

        val (_, result2) = "/delete"
            .httpDelete(listOf("foo" to "foo", "bar" to "bar"))
            .also { Log.d(TAG, it.cUrlString()) }
            .awaitStringResponseResult()
        update(result2)
    }

    private suspend fun httpDownload() {
        val n = 100
        val (_, result) = Fuel.download("/bytes/${1024 * n}")
            .fileDestination { _, _ -> File(filesDir, "test.tmp") }
            .progress { readBytes, totalBytes ->
                val progress = "$readBytes / $totalBytes"
                runOnUiThread { mainAuxText.text = progress }
                Log.v(TAG, progress)
            }
            .also { Log.d(TAG, it.toString()) }
            .awaitStringResponseResult()
        update(result)
    }

    private suspend fun httpUpload() {
        val (_, result) = Fuel.upload("/post")
            .add {
                // create random file with some non-sense string
                val file = File(filesDir, "out.tmp")
                file.writer().use { writer ->
                    repeat(100) {
                        writer.appendln("abcdefghijklmnopqrstuvwxyz")
                    }
                }
                FileDataPart(file)
            }
            .progress { writtenBytes, totalBytes ->
                Log.v(TAG, "Upload: ${writtenBytes.toFloat() / totalBytes.toFloat()}")
            }
            .also { Log.d(TAG, it.toString()) }
            .awaitStringResponseResult()
        update(result)
    }

    private suspend fun httpBasicAuthentication() {
        val username = "U$3|2|\\|@me"
        val password = "P@$\$vv0|2|)"

        val (_, result) = Fuel.get("/basic-auth/$username/$password")
            .authentication()
            .basic(username, password)
            .also { Log.d(TAG, it.cUrlString()) }
            .awaitStringResponseResult()
        update(result)

        val (_, result2) = "/basic-auth/$username/$password".httpGet()
            .authentication()
            .basic(username, password)
            .also { Log.d(TAG, it.cUrlString()) }
            .awaitStringResponseResult()
        update(result2)
    }

    private suspend fun <T : Any> update(result: Result<T, FuelError>) = withContext(Dispatchers.Main) {
        result.fold(
            success = { mainResultText.append(it.toString()) },
            failure = { mainResultText.append(String(it.errorData)) }
        )
    }

    data class Issue(
        val id: Int = 0,
        val title: String = "",
        val url: String = ""
    ) {
        class Deserializer : ResponseDeserializable<Issue> {
            override fun deserialize(reader: Reader) = Gson().fromJson(reader, Issue::class.java)!!
        }

        class ListDeserializer : ResponseDeserializable<List<Issue>> {
            override fun deserialize(reader: Reader): List<Issue> {
                val type = object : TypeToken<List<Issue>>() {}.type
                return Gson().fromJson(reader, type)
            }
        }
    }
}
