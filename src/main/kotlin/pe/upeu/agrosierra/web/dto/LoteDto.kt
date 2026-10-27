package pe.upeu.agrosierra.web.dto

import pe.upeu.agrosierra.dominio.Lote

data class LoteDto(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val hectareas: Double,
    val cultivo: String,
    val valle: String,
    val campaniaCerrada: Boolean,
    val estadoCampania: String,
    val dosisMaximaPorHectarea: Double,
) {
    companion object {
        fun de(l: Lote) = LoteDto(
            id = l.id!!,
            codigo = l.codigo,
            nombre = l.nombre,
            hectareas = l.hectareas,
            cultivo = l.cultivo,
            valle = l.valle,
            campaniaCerrada = l.campaniaCerrada,
            estadoCampania = if (l.campaniaCerrada) "CERRADA" else "ABIERTA",
            dosisMaximaPorHectarea = l.dosisMaximaPorHectarea,
        )
    }
}
