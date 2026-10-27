package pe.upeu.agrosierra.dominio

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "labor")
class Labor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id")
    var lote: Lote,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var tipo: TipoLabor,

    @Column(nullable = false)
    var fecha: LocalDate,

    @Column(nullable = false)
    var responsable: String,

    @Column(nullable = false)
    var cantidad: Double,

    @Column(nullable = false, length = 500)
    var observacion: String = "",

    /** Usuario (login) que registró la labor; base de la regla RN-02. */
    @Column(name = "registrado_por", nullable = false, length = 40)
    var registradoPor: String,

    @Column(name = "registrado_en", nullable = false)
    var registradoEn: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
