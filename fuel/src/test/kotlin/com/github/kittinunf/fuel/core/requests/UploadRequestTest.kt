package com.github.kittinunf.fuel.core.requests

import com.github.kittinunf.fuel.core.BlobDataPart
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.InlineDataPart
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.core.awaitStringResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import com.github.kittinunf.fuel.test.MockReflected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.isA
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException
import java.net.HttpURLConnection

class UploadRequestTest : MockHttpTestCase() {
    private val currentDir = File(System.getProperty("user.dir"), "src/test/assets")

    private fun assertFileUploaded(file: File, result: ResponseResultOf<MockReflected>, name: String? = file.nameWithoutExtension, fileName: String? = file.name): ResponseResultOf<MockReflected> {
        val (response, wrapped) = result
        val (data, error) = wrapped

        assertNotNull("Expected response not to be null", response)
        assertNotNull("Expected data, actual error $error", data)

        val statusCode = HttpURLConnection.HTTP_OK
        assertEquals(response.statusCode, statusCode)

        val body = data!!.body!!.string!!

        val expectedContents = file.readText()
        assertThat(body, containsString(expectedContents))

        val contentDispositions = body.lines().filter { it.startsWith(Headers.CONTENT_DISPOSITION, true) }
        val contentDispositionParameters = contentDispositions
                .flatMap { it -> it.split(";")
                        .map { it.trim() }
                }
        if (name != null) {
            val foundNames = contentDispositionParameters.filter { it.startsWith("name=") }
                .map { it.substringAfter("name=") }
                .map { it.trim('"') }

            assertThat("Expected $name to be the name, actual $foundNames", foundNames.contains(name), equalTo(true))
        }

        if (fileName != null) {
            val foundFileNames = contentDispositionParameters.filter { it.startsWith("filename=") }
                .map { it.substringAfter("filename=") }
                .map { it.trim('"') }

            assertThat("Expected $fileName to be the filename, actual $foundFileNames", foundFileNames.contains(fileName), equalTo(true))
        }

        return result
    }

    @Test
    fun uploadFileAsDataPart() = runBlocking {
        val manager = FuelManager()

        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/upload"),
            response = mock.reflect()
        )

        val file = File(currentDir, "lorem_ipsum_short.tmp")
        val triple = withContext(Dispatchers.IO) {
            manager.upload(mock.path("upload"))
                    .add(FileDataPart(file))
                    .awaitResponseResult(MockReflected.Deserializer())
        }
        assertFileUploaded(file, triple)
        Unit
    }

    @Test
    fun uploadFileAndParameters() = runBlocking {
        val manager = FuelManager()

        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/upload"),
            response = mock.reflect()
        )

        val file = File(currentDir, "lorem_ipsum_short.tmp")
        val triple = withContext(Dispatchers.IO) {
            manager.upload(mock.path("upload"), parameters = listOf("foo" to "bar"))
                    .add(FileDataPart(file, name = "file"))
                    .awaitResponseResult(MockReflected.Deserializer())
        }

        val (_, result) = assertFileUploaded(file, triple, name = "file")
        val (data, _) = result
        assertThat(data!!.body!!.string, containsString("name=\"foo\""))
        assertThat(data.body!!.string, containsString("bar"))
    }

    @Test
    fun uploadFileUsingPut() = runBlocking {
        val manager = FuelManager()

        mock.chain(
            request = mock.request().withMethod(Method.PUT.value).withPath("/upload"),
            response = mock.reflect()
        )

        val file = File(currentDir, "lorem_ipsum_long.tmp")
        val triple = withContext(Dispatchers.IO) {
            manager.upload(mock.path("upload"), Method.PUT)
                    .add(FileDataPart(file))
                    .awaitResponseResult(MockReflected.Deserializer())
        }
        assertFileUploaded(file, triple)
        Unit
    }

    @Test
    fun uploadFileUsingProgress() = runBlocking {
        val manager = FuelManager()

        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/upload"),
            response = mock.reflect()
        )

        var read = -1L
        var total = -1L

        val file = File(currentDir, "lorem_ipsum_long.tmp")
        val triple = withContext(Dispatchers.IO) {
            manager.upload(mock.path("upload"))
                    .add(FileDataPart(file))
                    .progress { readBytes, totalBytes -> read = readBytes; total = totalBytes }
                    .awaitResponseResult(MockReflected.Deserializer())
        }
        assertFileUploaded(file, triple)
        assertEquals("Expected upload progress", read == total && read != -1L && total != -1L, true)
    }

    @Test
    fun uploadToInvalidEndpoint() = runBlocking {
        val manager = FuelManager()

        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/nope"),
            response = mock.response().withStatusCode(HttpURLConnection.HTTP_NOT_FOUND)
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.upload(mock.path("nope"))
                    .add(FileDataPart(File(currentDir, "lorem_ipsum_short.tmp")))
                    .awaitStringResponseResult()
        }

        val (data, error) = result
        assertNotNull("Expected response not to be null", response)
        assertNotNull("Expected error, actual data $data", error)

        val statusCode = HttpURLConnection.HTTP_NOT_FOUND
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun uploadNonExistingFile() = runBlocking {
        val manager = FuelManager()

        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/upload"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.upload(mock.path("upload"))
                    .add { FileDataPart(File(currentDir, "not_found_file.tmp")) }
                    .awaitStringResponseResult()
        }

        val (data, error) = result
        assertNotNull("Expected response not to be null", response)
        assertNotNull("Expected error, actual data $data", error)

        assertThat(error?.exception as FileNotFoundException, isA(FileNotFoundException::class.java))

        val statusCode = -1
        assertEquals(response.statusCode, statusCode)
    }

    @Test
    fun uploadMultipleFilesUnderSameField() = runBlocking {
        val manager = FuelManager()

        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/upload"),
            response = mock.reflect()
        )

        val shortFile = File(currentDir, "lorem_ipsum_short.tmp")
        val longFile = File(currentDir, "lorem_ipsum_long.tmp")
        val triple = withContext(Dispatchers.IO) {
            manager.upload(mock.path("upload"), parameters = listOf("foo" to "bar"))
            .add(
                FileDataPart(shortFile, name = "file"),
                FileDataPart(longFile, name = "file")
            )
            .awaitResponseResult(MockReflected.Deserializer())
        }

        assertFileUploaded(shortFile, triple, name = "file")
        assertFileUploaded(longFile, triple, name = "file")

        assertThat(triple.second.component1()!!.body!!.string, not(containsString("multipart/mixed")))
    }

    @Test
    fun uploadMultipleFilesUnderSameFieldArray() = runBlocking {
        val manager = FuelManager()

        mock.chain(
                request = mock.request().withMethod(Method.POST.value).withPath("/upload"),
                response = mock.reflect()
        )

        val shortFile = File(currentDir, "lorem_ipsum_short.tmp")
        val longFile = File(currentDir, "lorem_ipsum_long.tmp")
        val triple = withContext(Dispatchers.IO) {
            manager.upload(mock.path("upload"), parameters = listOf("foo" to "bar"))
                    .add(
                            FileDataPart(shortFile, name = "file[]"),
                            FileDataPart(longFile, name = "file[]")
                    )
                    .awaitResponseResult(MockReflected.Deserializer())
        }

        assertFileUploaded(shortFile, triple, name = "file[]")
        assertFileUploaded(longFile, triple, name = "file[]")

        assertThat(triple.second.component1()!!.body!!.string, not(containsString("multipart/mixed")))
    }

    @Test
    fun uploadMultipleFilesAsMultipleFields() = runBlocking {
        val manager = FuelManager()

        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/upload"),
            response = mock.reflect()
        )

        val shortFile = File(currentDir, "lorem_ipsum_short.tmp")
        val longFile = File(currentDir, "lorem_ipsum_long.tmp")

        val triple = withContext(Dispatchers.IO) {
            manager.upload(mock.path("upload"), parameters = listOf("foo" to "bar"))
                    .add(FileDataPart(shortFile, contentType = "image/jpeg"))
                    .add(FileDataPart(longFile, name = "second-file", contentType = "image/jpeg"))
                    .awaitResponseResult(MockReflected.Deserializer())
        }

        assertFileUploaded(shortFile, triple)
        assertFileUploaded(longFile, triple, name = "second-file")
        Unit
    }

    @Test
    fun uploadBlob() = runBlocking {
        val file = File(currentDir, "lorem_ipsum_short.tmp")
        val blob = BlobDataPart(file.inputStream(), contentLength = file.length(), filename = file.name, name = "coolblob")
        val manager = FuelManager()

        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/upload"),
            response = mock.reflect()
        )

        val triple = withContext(Dispatchers.IO) {
            manager.upload(mock.path("upload"), parameters = listOf("foo" to "bar"))
                    .add { blob }
                    .awaitResponseResult(MockReflected.Deserializer())
        }
        assertFileUploaded(file, triple, name = "coolblob", fileName = file.name)
        Unit
    }

    @Test
    fun uploadWithCustomBoundary() = runBlocking {
        val manager = FuelManager()

        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/upload"),
            response = mock.reflect()
        )

        val boundary = "160f77ec3eff"
        val file = File(currentDir, "lorem_ipsum_short.tmp")
        val triple = withContext(Dispatchers.IO) {
            manager.upload(mock.path("upload"), parameters = listOf("foo" to "bar"))
                    .add(FileDataPart(file))
                    .header(Headers.CONTENT_TYPE, "multipart/form-data; boundary=\"$boundary\"")
                    .awaitResponseResult(MockReflected.Deserializer())
        }

        val ( _, result) = assertFileUploaded(file, triple)
        val (data, _) = result

        val body = data!!.body!!.string
        assertThat(body, containsString("--$boundary--"))
    }

    @Test
    fun uploadWithInvalidBoundary() = runBlocking {
        val manager = FuelManager()

        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/upload"),
            response = mock.reflect()
        )

        val (response, result) = withContext(Dispatchers.IO) {
            manager.upload(mock.path("upload"), parameters = listOf("foo" to "bar"))
                    .add(FileDataPart(File(currentDir, "lorem_ipsum_short.tmp")))
                    .header(Headers.CONTENT_TYPE, "multipart/form-data")
                    .awaitResponseResult(MockReflected.Deserializer())
        }

        val (data, error) = result
        assertNotNull("Expected response not to be null", response)
        assertNotNull("Expected error, actual data $data", error)

        assertThat(error?.exception as? IllegalArgumentException, isA(IllegalArgumentException::class.java))
    }

    @Test
    fun uploadInlineDataPart() = runBlocking {
        val manager = FuelManager()
        mock.chain(
            request = mock.request().withMethod(Method.POST.value).withPath("/upload"),
            response = mock.reflect()
        )

        val shortFile = File(currentDir, "lorem_ipsum_short.tmp")
        val longFile = File(currentDir, "lorem_ipsum_long.tmp")
        val metadata = longFile.readText()
        val triple = withContext(Dispatchers.IO) {
            manager.upload(mock.path("upload"), parameters = listOf("foo" to "bar"))
                    .add(
                            FileDataPart(shortFile, name = "file"),
                            InlineDataPart(metadata, name = "metadata", contentType = "application/json", filename = "metadata.json")
                    )
                    .awaitResponseResult(MockReflected.Deserializer())
        }

        assertFileUploaded(shortFile, triple, name = "file")
        assertFileUploaded(longFile, triple, name = "metadata", fileName = "metadata.json")

        assertThat(triple.second.component1()!!.body!!.string, not(containsString("multipart/mixed")))
    }
}
