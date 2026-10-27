package pe.upeu.agrosierra.seguridad

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pe.upeu.agrosierra.config.JwtService

data class LoginRequest(
    @field:NotBlank(message = "El usuario es obligatorio")
    val usuario: String = "",
    @field:NotBlank(message = "La contrasena es obligatoria")
    val contrasena: String = "",
)

data class LoginResponse(
    val token: String,
    val tipo: String = "Bearer",
    val expiraEn: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String>,
)

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val usuarios: UsuarioRepository,
    private val codificador: PasswordEncoder,
    private val jwt: JwtService,
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): LoginResponse {
        val usuario = usuarios.findByUsuario(req.usuario.trim())
            ?.takeIf { it.activo && codificador.matches(req.contrasena, it.contrasenaHash) }
            ?: throw BadCredentialsException("Usuario o contrasena incorrectos")

        val emitido = jwt.generar(usuario)
        return LoginResponse(
            token = emitido.token,
            expiraEn = emitido.expiraEn.toEpochMilli(),
            usuario = usuario.usuario,
            nombre = usuario.nombre,
            rol = usuario.rol.nombre,
            privilegios = usuario.nombresDePrivilegios(),
        )
    }
}
