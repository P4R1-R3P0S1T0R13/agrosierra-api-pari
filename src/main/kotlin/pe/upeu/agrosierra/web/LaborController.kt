package pe.upeu.agrosierra.web

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pe.upeu.agrosierra.seguridad.UsuarioAutenticado
import pe.upeu.agrosierra.servicio.LaborService
import pe.upeu.agrosierra.web.dto.LaborDto
import pe.upeu.agrosierra.web.dto.LaborRequest

/** El privilegio de cada operación lo exige SecurityConfig; las reglas RN viven en LaborService. */
@RestController
@RequestMapping("/api/v1/labores")
class LaborController(private val laborService: LaborService) {

    @PostMapping
    fun registrar(
        @Valid @RequestBody req: LaborRequest,
        @AuthenticationPrincipal yo: UsuarioAutenticado,
    ): ResponseEntity<LaborDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(LaborDto.de(laborService.registrar(req, yo)))

    @PutMapping("/{id}")
    fun corregir(
        @PathVariable id: Long,
        @Valid @RequestBody req: LaborRequest,
        @AuthenticationPrincipal yo: UsuarioAutenticado,
    ): LaborDto = LaborDto.de(laborService.corregir(id, req, yo))

    @DeleteMapping("/{id}")
    fun eliminar(@PathVariable id: Long): ResponseEntity<Void> {
        laborService.eliminar(id)
        return ResponseEntity.noContent().build()
    }
}
