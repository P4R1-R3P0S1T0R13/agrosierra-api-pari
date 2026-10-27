package pe.upeu.agrosierra.web.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Formato de error único de todo el servicio (contrato 4.2). */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorDto(
    val timestamp: String,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val validationErrors: Map<String, String>? = null,
) {
    companion object {
        fun de(status: Int, error: String, message: String, path: String, campos: Map<String, String>? = null) =
            ErrorDto(
                timestamp = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                status = status,
                error = error,
                message = message,
                path = path,
                validationErrors = campos,
            )
    }
}
