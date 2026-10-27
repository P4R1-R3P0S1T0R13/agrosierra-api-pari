package pe.upeu.agrosierra.config

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pe.upeu.agrosierra.dominio.Labor
import pe.upeu.agrosierra.dominio.Lote
import pe.upeu.agrosierra.dominio.TipoLabor
import pe.upeu.agrosierra.dominio.TipoLabor.COSECHA
import pe.upeu.agrosierra.dominio.TipoLabor.FUMIGACION
import pe.upeu.agrosierra.dominio.TipoLabor.RIEGO
import pe.upeu.agrosierra.repositorio.LaborRepository
import pe.upeu.agrosierra.repositorio.LoteRepository
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Datos mínimos del anexo 10: 8 lotes en 3 valles (2 con campaña cerrada) y 20 labores.
 * Cada lote abierto tiene 2 o más labores; LT-01 tiene COSECHA y LT-02 no (para demostrar RN-03).
 * Solo se cargan si la tabla de lotes está vacía.
 */
@Component
@Order(2)
class SemillaCampo(
    private val lotes: LoteRepository,
    private val labores: LaborRepository,
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(vararg args: String) {
        if (lotes.count() > 0) return

        val lt = listOf(
            Lote("LT-01", "La Esperanza", 2.5, "Papa", "Mantaro", false, 4.0),
            Lote("LT-02", "Santa Rosa", 1.8, "Maiz", "Mantaro", false, 3.5),
            Lote("LT-03", "El Mirador", 3.2, "Quinua", "Mantaro", true, 2.0),
            Lote("LT-04", "Los Andes", 4.0, "Papa", "Colca", false, 4.0),
            Lote("LT-05", "San Pedro", 2.0, "Haba", "Colca", false, 3.0),
            Lote("LT-06", "Pampa Verde", 5.5, "Alfalfa", "Colca", true, 2.5),
            Lote("LT-07", "Villa Rica", 1.2, "Cebada", "Urubamba", false, 3.0),
            Lote("LT-08", "Chacra Alta", 2.8, "Maiz", "Urubamba", false, 3.5),
        ).map(lotes::save).associateBy { it.codigo }

        val ahora = Instant.now()
        val hoy = LocalDate.now()
        val antiguo = ahora.minus(10, ChronoUnit.DAYS)

        fun labor(
            codigo: String, tipo: TipoLabor, diasAtras: Long, responsable: String, cantidad: Double,
            observacion: String, autor: String, registradoEn: Instant = antiguo,
        ) = Labor(
            lote = lt.getValue(codigo), tipo = tipo, fecha = hoy.minusDays(diasAtras), responsable = responsable,
            cantidad = cantidad, observacion = observacion, registradoPor = autor, registradoEn = registradoEn,
        )

        val tecnico1 = "Juan Quispe Mamani"
        val tecnico2 = "Rosa Condori Huaman"
        val jefe = "Carlos Pari Ticona"
        listOf(
            labor("LT-01", RIEGO, 20, tecnico1, 120.0, "Riego por gravedad", "tecnico1"),
            labor("LT-01", FUMIGACION, 12, tecnico1, 8.5, "Control de rancha", "tecnico1"),
            labor("LT-01", COSECHA, 2, jefe, 3500.0, "Cosecha de papa amarilla", "jefe1"),
            labor("LT-02", RIEGO, 9, tecnico2, 90.0, "Riego por surcos", "tecnico2"),
            // Registrada hoy por tecnico1: puede corregirla dentro de las 24 h (RN-02)
            labor("LT-02", FUMIGACION, 0, tecnico1, 5.0, "Control de cogollero", "tecnico1", ahora),
            labor("LT-03", RIEGO, 60, tecnico1, 100.0, "Riego de inicio de campania", "tecnico1"),
            labor("LT-03", COSECHA, 30, jefe, 1800.0, "Cosecha de quinua", "jefe1"),
            labor("LT-04", RIEGO, 15, tecnico2, 150.0, "Riego por aspersion", "tecnico2"),
            labor("LT-04", FUMIGACION, 8, tecnico2, 12.0, "Control de gorgojo", "tecnico2"),
            labor("LT-04", RIEGO, 1, tecnico1, 140.0, "Riego de mantenimiento", "tecnico1", ahora),
            labor("LT-05", RIEGO, 11, tecnico1, 80.0, "Riego por gravedad", "tecnico1"),
            labor("LT-05", FUMIGACION, 4, tecnico2, 4.5, "Control de pulgon", "tecnico2"),
            labor("LT-06", RIEGO, 45, tecnico2, 200.0, "Riego por inundacion", "tecnico2"),
            labor("LT-06", COSECHA, 25, jefe, 6000.0, "Corte de alfalfa", "jefe1"),
            labor("LT-07", RIEGO, 7, tecnico1, 60.0, "Riego por goteo", "tecnico1"),
            labor("LT-07", FUMIGACION, 3, tecnico1, 2.5, "Control de roya", "tecnico1"),
            labor("LT-07", COSECHA, 1, jefe, 900.0, "Cosecha de cebada", "jefe1"),
            labor("LT-08", RIEGO, 14, tecnico2, 110.0, "Riego por surcos", "tecnico2"),
            labor("LT-08", FUMIGACION, 6, tecnico2, 7.0, "Control de gusano cogollero", "tecnico2"),
            labor("LT-08", RIEGO, 0, tecnico2, 105.0, "Riego de mantenimiento", "tecnico2", ahora),
        ).forEach(labores::save)

        log.info("Datos semilla cargados: {} lotes y {} labores", lotes.count(), labores.count())
    }
}
