package pe.upeu.agrosierra.config

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import pe.upeu.agrosierra.dominio.OperacionNoPermitidaException
import pe.upeu.agrosierra.dominio.RecursoNoEncontradoException
import pe.upeu.agrosierra.dominio.ReglaNegocioException
import pe.upeu.agrosierra.dominio.ValidacionException
import pe.upeu.agrosierra.web.dto.ErrorDto

/** Traduce cada excepción al formato de error común y al código de estado del contrato. */
@RestControllerAdvice
class ManejadorDeErrores {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacion(ex: MethodArgumentNotValidException, req: HttpServletRequest): ResponseEntity<ErrorDto> {
        val campos = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Valor invalido") }
        return responder(HttpStatus.BAD_REQUEST, "Datos invalidos", req, campos)
    }

    @ExceptionHandler(ValidacionException::class)
    fun validacionNegocio(ex: ValidacionException, req: HttpServletRequest) =
        responder(HttpStatus.BAD_REQUEST, ex.message ?: "Datos invalidos", req, ex.campos)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun cuerpoIlegible(ex: HttpMessageNotReadableException, req: HttpServletRequest) =
        responder(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es valido", req, emptyMap())

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun parametroInvalido(ex: MethodArgumentTypeMismatchException, req: HttpServletRequest) =
        responder(HttpStatus.BAD_REQUEST, "Parametro invalido", req, mapOf(ex.name to "Valor no admitido"))

    @ExceptionHandler(RecursoNoEncontradoException::class, NoResourceFoundException::class)
    fun noEncontrado(ex: Exception, req: HttpServletRequest) =
        responder(HttpStatus.NOT_FOUND, ex.message ?: "Recurso no encontrado", req)

    @ExceptionHandler(ReglaNegocioException::class)
    fun conflicto(ex: ReglaNegocioException, req: HttpServletRequest) =
        responder(HttpStatus.CONFLICT, ex.message ?: "Conflicto", req)

    @ExceptionHandler(OperacionNoPermitidaException::class, AccessDeniedException::class)
    fun prohibido(ex: Exception, req: HttpServletRequest) =
        responder(HttpStatus.FORBIDDEN, ex.message ?: "No tiene el privilegio requerido", req)

    @ExceptionHandler(AuthenticationException::class)
    fun noAutenticado(ex: AuthenticationException, req: HttpServletRequest) =
        responder(HttpStatus.UNAUTHORIZED, ex.message ?: "Credenciales invalidas", req)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun metodo(ex: HttpRequestMethodNotSupportedException, req: HttpServletRequest) =
        responder(HttpStatus.METHOD_NOT_ALLOWED, "Metodo no admitido", req)

    @ExceptionHandler(Exception::class)
    fun inesperado(ex: Exception, req: HttpServletRequest): ResponseEntity<ErrorDto> {
        log.error("Error no controlado en {}", req.requestURI, ex)
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req)
    }

    private fun responder(
        estado: HttpStatus,
        mensaje: String,
        req: HttpServletRequest,
        campos: Map<String, String>? = null,
    ) = ResponseEntity.status(estado)
        .body(ErrorDto.de(estado.value(), estado.reasonPhrase, mensaje, req.requestURI, campos))
}
