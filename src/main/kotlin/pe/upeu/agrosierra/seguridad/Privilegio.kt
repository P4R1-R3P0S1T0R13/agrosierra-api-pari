package pe.upeu.agrosierra.seguridad

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "privilegio")
class Privilegio(
    @Column(nullable = false, unique = true, length = 40)
    var nombre: String,

    @Column(nullable = false)
    var descripcion: String = "",

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) {
    /** Nombres de privilegio del caso (sección 3.3). La asignación a cada rol vive en la base de datos. */
    companion object {
        const val LOTE_LEER = "LOTE_LEER"
        const val LABOR_LEER = "LABOR_LEER"
        const val LABOR_CREAR = "LABOR_CREAR"
        const val LABOR_EDITAR = "LABOR_EDITAR"
        /** Alcance de LABOR_EDITAR: sin él, solo se corrigen las labores propias dentro de 24 h (RN-02). */
        const val LABOR_EDITAR_TODAS = "LABOR_EDITAR_TODAS"
        const val LABOR_ELIMINAR = "LABOR_ELIMINAR"
        const val CAMPANIA_CERRAR = "CAMPANIA_CERRAR"
        const val USUARIO_GESTIONAR = "USUARIO_GESTIONAR"
    }
}
