package com.example.core.network

import com.example.core.model.AppError
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object NetworkErrorMapper {
    fun mapHttpStatus(code: Int, rawBody: String? = null): AppError {
        val userFriendlyMessage = when (code) {
            200, 201, 204 -> "Success"
            400 -> "Nila received an invalid request format."
            401 -> "Authentication expired or unauthorized. Please verify credentials."
            403 -> "Access forbidden. Provider quota or permissions insufficient."
            404 -> "The requested AI resource was not found on the server."
            408 -> "Request timed out while waiting for AI response."
            409 -> "Conflict occurred during resource synchronization."
            413 -> "Input payload or attachment exceeds permissible size limit."
            429 -> "Rate limit reached. Please wait a moment before sending another request."
            500 -> "Nila AI server encountered an internal issue. Retrying may help."
            502 -> "Bad gateway received from upstream AI inference service."
            503 -> "AI service is temporarily unavailable or undergoing maintenance."
            504 -> "Gateway timed out contacting remote AI engine."
            else -> "Unexpected network response code ($code)."
        }
        return AppError.ProviderError(userFriendlyMessage)
    }

    fun mapException(e: Throwable): AppError {
        return when (e) {
            is UnknownHostException -> AppError.NetworkError("Unable to reach Nila servers. Please check your internet connection.")
            is SocketTimeoutException -> AppError.NetworkError("Connection timed out waiting for server response.")
            is ConnectException -> AppError.NetworkError("Failed to connect to the server. Service may be offline.")
            is SSLException -> AppError.NetworkError("Secure SSL handshake failed. Network might be restricted.")
            is IOException -> AppError.NetworkError("A network I/O error occurred: ${e.localizedMessage ?: "Unknown"}")
            else -> AppError.UnknownError("Something went wrong while contacting Nila's engine.")
        }
    }

    /**
     * Inspects body before parsing to prevent exposing raw HTML ("Unexpected token <")
     * or plain text syntax errors to users.
     */
    fun sanitizeResponseBody(contentType: String?, bodyText: String): String {
        val trimmed = bodyText.trim()
        if (contentType != null && contentType.contains("text/html", ignoreCase = true) ||
            trimmed.startsWith("<!DOCTYPE", ignoreCase = true) ||
            trimmed.startsWith("<html", ignoreCase = true)
        ) {
            return "Server returned an unexpected HTML status page."
        }
        return bodyText
    }
}
