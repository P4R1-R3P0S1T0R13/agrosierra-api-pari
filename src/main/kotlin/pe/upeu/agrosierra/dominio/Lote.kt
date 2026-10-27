package pe.upeu.agrosierra.dominio

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "lote")
class Lote(
    @Column(nullable = false, unique = true, length = 20)
    var codigo: String,

    @Column(nullable = false)
    var nombre: String,

    @Column(nullable = false)
    var hectareas: Double,

    @Column(nullable = false)
    var cultivo: String,

    @Column(nullable = false)
    var valle: String,

    @Column(name = "campania_cerrada", nullable = false)
    var campaniaCerrada: Boolean = false,

    /** Dosis máxima de fumigación por hectárea (RN-04). */
    @Column(name = "dosis_maxima_por_hectarea", nullable = false)
    var dosisMaximaPorHectarea: Double,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) {
    /** Cantidad máxima de producto que admite una fumigación sobre todo el lote. */
    fun dosisMaximaTotal(): Double = dosisMaximaPorHectarea * hectareas
}
