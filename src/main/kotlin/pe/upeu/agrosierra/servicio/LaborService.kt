package pe.upeu.agrosierra.servicio

import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.agrosierra.dominio.Labor
import pe.upeu.agrosierra.dominio.Lote
import pe.upeu.agrosierra.dominio.RecursoNoEncontradoException
import pe.upeu.agrosierra.dominio.ReglaNegocioException
import pe.upeu.agrosierra.dominio.TipoLabor
import pe.upeu.agrosierra.dominio.ValidacionException
import pe.upeu.agrosierra.repositorio.LaborRepository
import pe.upeu.agrosierra.repositorio.LoteRepository
import pe.upeu.agrosierra.seguridad.UsuarioAutenticado
import pe.upeu.agrosierra.web.dto.LaborRequest
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Service
@Transactional
class LaborService(
    private val labores: LaborRepository,
    private val lotes: LoteRepository,
    private val loteService: LoteService,
) {

    @Transactional(readOnly = true)
    fun listarPorLote(loteId: Long, pagina: Int, tamanio: Int): Page<Labor> {
        loteService.obtener(loteId)
        val orden = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"))
        return labores.findByLoteId(loteId, paginacion(pagina, tamanio, orden))
    }

    fun registrar(req: LaborRequest, autor: UsuarioAutenticado): Labor {
        val datos = leer(req)
        exigirCampaniaAbierta(datos.lote)
        exigirDosisPermitida(datos)
        val labor = Labor(
            lote = datos.lote,
            tipo = datos.tipo,
            fecha = datos.fecha,
            responsable = datos.responsable,
            cantidad = datos.cantidad,
            observacion = datos.observacion,
            registradoPor = autor.usuario,
        )
        return labores.save(labor)
    }

    fun corregir(id: Long, req: LaborRequest, autor: UsuarioAutenticado): Labor {
        val labor = obtener(id)
        val datos = leer(req)
        exigirCampaniaAbierta(labor.lote)
        exigirCampaniaAbierta(datos.lote)
        exigirDosisPermitida(datos)
        labor.lote = datos.lote
        labor.tipo = datos.tipo
        labor.fecha = datos.fecha
        labor.responsable = datos.responsable
        labor.cantidad = datos.cantidad
        labor.observacion = datos.observacion
        return labor
    }

    fun eliminar(id: Long) {
        labores.delete(obtener(id))
    }

    /** RN-01: no se registra ni corrige ninguna labor sobre un lote con la campaña cerrada (409). */
    private fun exigirCampaniaAbierta(lote: Lote) {
        if (lote.campaniaCerrada) {
            throw ReglaNegocioException(
                "La campania del lote ${lote.codigo} esta cerrada: no se pueden registrar ni corregir labores",
            )
        }
    }

    /** RN-04: en una fumigación la cantidad no puede superar dosis máxima por ha x hectáreas (400 en cantidad). */
    private fun exigirDosisPermitida(datos: DatosLabor) {
        if (datos.tipo != TipoLabor.FUMIGACION) return
        val maximo = datos.lote.dosisMaximaTotal()
        if (datos.cantidad > maximo) {
            throw ValidacionException(
                mapOf(
                    "cantidad" to "La dosis supera el maximo del lote: %.2f (%.2f por ha x %.2f ha)"
                        .format(maximo, datos.lote.dosisMaximaPorHectarea, datos.lote.hectareas),
                ),
            )
        }
    }

    private fun obtener(id: Long): Labor =
        labores.findByIdOrNull(id) ?: throw RecursoNoEncontradoException("La labor $id no existe")

    private data class DatosLabor(
        val lote: Lote,
        val tipo: TipoLabor,
        val fecha: LocalDate,
        val responsable: String,
        val cantidad: Double,
        val observacion: String,
    )

    /** Convierte la petición ya validada por anotaciones en valores de dominio. */
    private fun leer(req: LaborRequest): DatosLabor {
        val fecha = try {
            LocalDate.parse(req.fecha!!)
        } catch (e: DateTimeParseException) {
            throw ValidacionException(mapOf("fecha" to "La fecha no es valida"))
        }
        val lote = lotes.findByIdOrNull(req.loteId!!)
            ?: throw ValidacionException(mapOf("loteId" to "El lote indicado no existe"))
        return DatosLabor(
            lote = lote,
            tipo = TipoLabor.valueOf(req.tipo!!),
            fecha = fecha,
            responsable = req.responsable!!.trim(),
            cantidad = req.cantidad!!,
            observacion = req.observacion?.trim().orEmpty(),
        )
    }
}
