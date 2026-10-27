package pe.upeu.agrosierra.servicio

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.agrosierra.dominio.Lote
import pe.upeu.agrosierra.dominio.RecursoNoEncontradoException
import pe.upeu.agrosierra.dominio.ReglaNegocioException
import pe.upeu.agrosierra.dominio.TipoLabor
import pe.upeu.agrosierra.dominio.ValidacionException
import pe.upeu.agrosierra.repositorio.LaborRepository
import pe.upeu.agrosierra.repositorio.LoteRepository

@Service
@Transactional(readOnly = true)
class LoteService(
    private val lotes: LoteRepository,
    private val labores: LaborRepository,
) {

    fun listar(pagina: Int, tamanio: Int, valle: String?, estado: String?): Page<Lote> {
        val cerrada = when (estado?.trim()?.uppercase()) {
            null, "" -> null
            "ABIERTA" -> false
            "CERRADA" -> true
            else -> throw ValidacionException(mapOf("estado" to "El estado debe ser ABIERTA o CERRADA"))
        }
        val filtro = LoteRepository.delValle(valle).and(LoteRepository.conCampaniaCerrada(cerrada))
        return lotes.findAll(filtro, paginacion(pagina, tamanio, Sort.by("codigo")))
    }

    fun obtener(id: Long): Lote =
        lotes.findByIdOrNull(id) ?: throw RecursoNoEncontradoException("El lote $id no existe")

    /** RN-03: la campaña solo se cierra si el lote tiene al menos una labor de COSECHA (409). */
    @Transactional
    fun cerrarCampania(id: Long): Lote {
        val lote = obtener(id)
        if (lote.campaniaCerrada) {
            throw ReglaNegocioException("La campania del lote ${lote.codigo} ya esta cerrada")
        }
        if (!labores.existsByLoteIdAndTipo(id, TipoLabor.COSECHA)) {
            throw ReglaNegocioException(
                "No se puede cerrar la campania del lote ${lote.codigo}: no tiene ninguna labor de cosecha registrada",
            )
        }
        lote.campaniaCerrada = true
        return lote
    }
}

/** Normaliza página y tamaño para que un valor fuera de rango no rompa la consulta. */
internal fun paginacion(pagina: Int, tamanio: Int, orden: Sort): PageRequest =
    PageRequest.of(pagina.coerceAtLeast(0), tamanio.coerceIn(1, 100), orden)
