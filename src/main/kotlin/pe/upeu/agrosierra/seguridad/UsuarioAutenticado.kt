package pe.upeu.agrosierra.seguridad

/** Principal construido a partir del token ya verificado. */
data class UsuarioAutenticado(
    val id: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String>,
) {
    fun tiene(privilegio: String) = privilegio in privilegios
}
