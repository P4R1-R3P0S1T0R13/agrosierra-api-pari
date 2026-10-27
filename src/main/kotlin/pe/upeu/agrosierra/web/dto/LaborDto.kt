package pe.upeu.agrosierra.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import pe.upeu.agrosierra.dominio.Labor

data class LaborDto(
    val id: Long,
    val loteId: Long,
    val tipo: String,
    val fecha: String,
    val responsable: String,
    val cantidad: Double,
    val observacion: String,
    val registradoPor: String,
) {
    companion object {
        fun de(l: Labor) = LaborDto(
            id = l.id!!,
            loteId = l.lote.id!!,
            tipo = l.tipo.name,
            fecha = l.fecha.toString(),
            responsable = l.responsable,
            cantidad = l.cantidad,
            observacion = l.observacion,
            registradoPor = l.registradoPor,
        )
    }
}

/** Cuerpo de POST y PUT de labores. Los errores se devuelven por campo (400). */
data class LaborRequest(
    @field:NotNull(message = "El lote es obligatorio")
    val loteId: Long? = null,

    @field:NotBlank(message = "El tipo es obligatorio")
    @field:Pattern(regexp = "RIEGO|FUMIGACION|COSECHA", message = "El tipo debe ser RIEGO, FUMIGACION o COSECHA")
    val tipo: String? = null,

    @field:NotBlank(message = "La fecha es obligatoria")
    @field:Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "La fecha debe tener el formato AAAA-MM-DD")
    val fecha: String? = null,

    @field:NotBlank(message = "El responsable es obligatorio")
    @field:Size(max = 100, message = "El responsable admite como maximo 100 caracteres")
    val responsable: String? = null,

    @field:NotNull(message = "La cantidad es obligatoria")
    @field:Positive(message = "La cantidad debe ser mayor que cero")
    val cantidad: Double? = null,

    @field:Size(max = 500, message = "La observacion admite como maximo 500 caracteres")
    val observacion: String? = "",
)
