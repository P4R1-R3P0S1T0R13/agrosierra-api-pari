package pe.upeu.agrosierra.config

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pe.upeu.agrosierra.seguridad.Privilegio
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.CAMPANIA_CERRAR
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.LABOR_CREAR
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.LABOR_EDITAR
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.LABOR_EDITAR_TODAS
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.LABOR_ELIMINAR
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.LABOR_LEER
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.LOTE_LEER
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.USUARIO_GESTIONAR
import pe.upeu.agrosierra.seguridad.PrivilegioRepository
import pe.upeu.agrosierra.seguridad.Rol
import pe.upeu.agrosierra.seguridad.RolRepository
import pe.upeu.agrosierra.seguridad.Usuario
import pe.upeu.agrosierra.seguridad.UsuarioRepository

/**
 * Carga privilegios, roles (matriz 3.3) y usuarios de prueba (anexo 10) al iniciar.
 * Es idempotente: solo inserta lo que falta.
 */
@Component
@Order(1)
class SemillaSeguridad(
    private val privilegios: PrivilegioRepository,
    private val roles: RolRepository,
    private val usuarios: UsuarioRepository,
    private val codificador: PasswordEncoder,
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    private val descripciones = mapOf(
        LOTE_LEER to "Consultar el padron y el detalle de lotes",
        LABOR_LEER to "Consultar las labores de un lote",
        LABOR_CREAR to "Registrar una labor nueva",
        LABOR_EDITAR to "Corregir una labor existente",
        LABOR_EDITAR_TODAS to "Corregir labores de cualquier tecnico sin limite de tiempo",
        LABOR_ELIMINAR to "Eliminar una labor",
        CAMPANIA_CERRAR to "Cerrar la campania de un lote",
        USUARIO_GESTIONAR to "Crear usuarios y asignar roles",
    )

    /** Matriz de privilegios de la sección 3.3. */
    private val matriz = mapOf(
        "TECNICO" to setOf(LOTE_LEER, LABOR_LEER, LABOR_CREAR, LABOR_EDITAR),
        "JEFE_CAMPO" to setOf(
            LOTE_LEER, LABOR_LEER, LABOR_CREAR, LABOR_EDITAR, LABOR_EDITAR_TODAS, LABOR_ELIMINAR, CAMPANIA_CERRAR,
        ),
        "ADMINISTRADOR" to setOf(LOTE_LEER, LABOR_LEER, USUARIO_GESTIONAR),
    )

    private data class UsuarioSemilla(val usuario: String, val nombre: String, val rol: String)

    private val usuariosDePrueba = listOf(
        UsuarioSemilla("tecnico1", "Juan Quispe Mamani", "TECNICO"),
        UsuarioSemilla("tecnico2", "Rosa Condori Huaman", "TECNICO"),
        UsuarioSemilla("jefe1", "Carlos Pari Ticona", "JEFE_CAMPO"),
        UsuarioSemilla("admin", "Administrador AgroSierra", "ADMINISTRADOR"),
    )

    @Transactional
    override fun run(vararg args: String) {
        val porNombre = descripciones.map { (nombre, desc) ->
            nombre to (privilegios.findByNombre(nombre) ?: privilegios.save(Privilegio(nombre, desc)))
        }.toMap()

        matriz.forEach { (nombreRol, permitidos) ->
            val rol = roles.findByNombre(nombreRol) ?: Rol(nombreRol)
            rol.privilegios = permitidos.map { porNombre.getValue(it) }.toMutableSet()
            roles.save(rol)
        }

        usuariosDePrueba.filter { usuarios.findByUsuario(it.usuario) == null }.forEach {
            usuarios.save(
                Usuario(
                    usuario = it.usuario,
                    nombre = it.nombre,
                    contrasenaHash = codificador.encode(CONTRASENA_DEMO),
                    rol = roles.findByNombre(it.rol)!!,
                ),
            )
            log.info("Usuario de prueba creado: {} ({})", it.usuario, it.rol)
        }
    }

    companion object {
        /** Contraseña del anexo; en la base solo se guarda su hash BCrypt. */
        private const val CONTRASENA_DEMO = "Agro2026*"
    }
}
