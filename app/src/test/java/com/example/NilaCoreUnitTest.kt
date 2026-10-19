package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.command.CommandResult
import com.example.core.command.LocalCommandRouter
import com.example.core.model.AppError
import com.example.core.network.NetworkErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NilaCoreUnitTest {

    @Test
    fun `wake word nila responds with I am here`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val router = LocalCommandRouter(context)
        val result = router.evaluate("Nila")
        assertTrue(result is CommandResult.Handled)
        assertEquals("I am here.", (result as CommandResult.Handled).reply)
    }

    @Test
    fun `navigation command returns home target`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val router = LocalCommandRouter(context)
        val result = router.evaluate("go home")
        assertTrue(result is CommandResult.Handled)
        assertEquals("home", (result as CommandResult.Handled).navigationTarget)
    }

    @Test
    fun `blocked password and otp commands are denied by security policy`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val router = LocalCommandRouter(context)
        val result = router.evaluate("automate banking otp password")
        assertTrue(result is CommandResult.Blocked)
    }

    @Test
    fun `network error mapper returns friendly message for 429 and 500`() {
        val rateLimit = NetworkErrorMapper.mapHttpStatus(429)
        assertTrue(rateLimit.userFriendlyMessage.contains("Rate limit reached"))

        val serverError = NetworkErrorMapper.mapHttpStatus(500)
        assertTrue(serverError.userFriendlyMessage.contains("internal issue"))
    }

    @Test
    fun `html response sanitization prevents unexpected token syntax errors`() {
        val html = "<!DOCTYPE html><html><body>Error 502</body></html>"
        val sanitized = NetworkErrorMapper.sanitizeResponseBody("text/html", html)
        assertEquals("Server returned an unexpected HTML status page.", sanitized)
    }
}
