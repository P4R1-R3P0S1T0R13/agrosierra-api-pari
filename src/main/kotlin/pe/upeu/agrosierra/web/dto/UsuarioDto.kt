package pe.upeu.agrosierra.web.dto

import pe.upeu.agrosierra.seguridad.Usuario

data class UsuarioDto(
    val id: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String>,
) {
    companion object {
        fun de(u: Usuario) = UsuarioDto(u.id!!, u.usuario, u.nombre, u.rol.nombre, u.nombresDePrivilegios())
    }
}
