package pe.upeu.agrosierra.web.dto

import org.springframework.data.domain.Page

/** Envoltorio de paginación común a todos los listados (contrato 4.2). */
data class PaginaDto<T>(
    val contenido: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val ultima: Boolean,
) {
    companion object {
        fun <E, T> de(pagina: Page<E>, convertir: (E) -> T) = PaginaDto(
            contenido = pagina.content.map(convertir),
            pagina = pagina.number,
            tamanio = pagina.size,
            totalElementos = pagina.totalElements,
            totalPaginas = pagina.totalPages,
            ultima = pagina.isLast,
        )
    }
}
