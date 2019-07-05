package com.github.kittinunf.fuel

import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.test.MockHttpTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ReadmeIntegrityTest : MockHttpTestCase() {

    // This silences the printing so it doesn't pollute the log
    private val outContent = ByteArrayOutputStream()
    private val originalOut = System.out

    @Before
    fun prepareStream() {
        System.setOut(PrintStream(outContent))
        Fuel.reset()
    }

    @After
    fun tearDownStream() {
        System.setOut(originalOut)
    }

    //TODO: it is not really Testing anything?
    /*@Test
    fun makingRequestsAboutPatchRequests() = runBlocking {
        mock.chain(
            request = mock.request().withMethod(Method.POST.value),
            response = mock.reflect()
        )

        withContext(Dispatchers.IO) {
            Fuel.patch(mock.path("/post"))
                    .also { println("[request] $it") }
                    .awaitByteArrayResponseResult()
        }
    }*/
}
