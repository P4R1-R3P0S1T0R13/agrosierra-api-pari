package pe.upeu.agrosierra.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pe.upeu.agrosierra.servicio.LaborService
import pe.upeu.agrosierra.servicio.LoteService
import pe.upeu.agrosierra.web.dto.LaborDto
import pe.upeu.agrosierra.web.dto.LoteDto
import pe.upeu.agrosierra.web.dto.PaginaDto

@RestController
@RequestMapping("/api/v1/lotes")
class LoteController(
    private val loteService: LoteService,
    private val laborService: LaborService,
) {

    @GetMapping
    fun listar(
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "20") tamanio: Int,
        @RequestParam(required = false) valle: String?,
        @RequestParam(required = false) estado: String?,
    ): PaginaDto<LoteDto> = PaginaDto.de(loteService.listar(pagina, tamanio, valle, estado), LoteDto::de)

    @GetMapping("/{id}")
    fun detalle(@PathVariable id: Long): LoteDto = LoteDto.de(loteService.obtener(id))

    /** RF-B06: solo con CAMPANIA_CERRAR (SecurityConfig); RN-03 se valida en LoteService. */
    @PostMapping("/{id}/cerrar-campania")
    fun cerrarCampania(@PathVariable id: Long): LoteDto = LoteDto.de(loteService.cerrarCampania(id))

    @GetMapping("/{id}/labores")
    fun labores(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "20") tamanio: Int,
    ): PaginaDto<LaborDto> = PaginaDto.de(laborService.listarPorLote(id, pagina, tamanio), LaborDto::de)
}
