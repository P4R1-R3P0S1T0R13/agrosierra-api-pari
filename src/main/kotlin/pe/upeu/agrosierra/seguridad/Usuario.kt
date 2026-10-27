package pe.upeu.agrosierra.seguridad

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "usuario")
class Usuario(
    @Column(nullable = false, unique = true, length = 40)
    var usuario: String,

    @Column(nullable = false)
    var nombre: String,

    /** Contraseña cifrada con BCrypt; nunca se guarda en claro. */
    @Column(name = "contrasena_hash", nullable = false)
    var contrasenaHash: String,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id")
    var rol: Rol,

    @Column(nullable = false)
    var activo: Boolean = true,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) {
    fun nombresDePrivilegios(): Set<String> = rol.privilegios.map { it.nombre }.toSortedSet()
}
