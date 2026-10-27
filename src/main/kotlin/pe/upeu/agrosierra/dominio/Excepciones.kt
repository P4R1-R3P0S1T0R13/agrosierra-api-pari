package pe.upeu.agrosierra.dominio

/** 404: el recurso solicitado no existe. */
class RecursoNoEncontradoException(mensaje: String) : RuntimeException(mensaje)

/** 409: se incumple una regla de negocio (RN-01, RN-03). */
class ReglaNegocioException(mensaje: String) : RuntimeException(mensaje)

/** 403: el usuario tiene el privilegio pero la regla le impide operar sobre este recurso (RN-02). */
class OperacionNoPermitidaException(mensaje: String) : RuntimeException(mensaje)

/** 400: validación de negocio con detalle por campo (RN-04). */
class ValidacionException(val campos: Map<String, String>, mensaje: String = "Datos invalidos") :
    RuntimeException(mensaje)
