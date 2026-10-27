package pe.upeu.agrosierra.web

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pe.upeu.agrosierra.dominio.RecursoNoEncontradoException
import pe.upeu.agrosierra.seguridad.UsuarioAutenticado
import pe.upeu.agrosierra.seguridad.UsuarioRepository
import pe.upeu.agrosierra.web.dto.PaginaDto
import pe.upeu.agrosierra.web.dto.UsuarioDto

@RestController
@RequestMapping("/api/v1/usuarios")
class UsuarioController(private val usuarios: UsuarioRepository) {

    /** RF-B07: datos y privilegios del usuario del token. */
    @GetMapping("/me")
    fun actual(@AuthenticationPrincipal yo: UsuarioAutenticado): UsuarioDto =
        usuarios.findByUsuario(yo.usuario)?.let(UsuarioDto::de)
            ?: throw RecursoNoEncontradoException("Usuario no encontrado")

    /** Padrón de usuarios para el administrador (privilegio USUARIO_GESTIONAR en SecurityConfig). */
    @GetMapping
    fun listar(
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "20") tamanio: Int,
    ): PaginaDto<UsuarioDto> =
        PaginaDto.de(usuarios.findAll(PageRequest.of(pagina, tamanio, Sort.by("usuario"))), UsuarioDto::de)
}
