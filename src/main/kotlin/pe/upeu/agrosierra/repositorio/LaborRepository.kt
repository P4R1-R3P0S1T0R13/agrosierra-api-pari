package pe.upeu.agrosierra.repositorio

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import pe.upeu.agrosierra.dominio.Labor
import pe.upeu.agrosierra.dominio.TipoLabor

interface LaborRepository : JpaRepository<Labor, Long> {
    fun findByLoteId(loteId: Long, pageable: Pageable): Page<Labor>

    fun existsByLoteIdAndTipo(loteId: Long, tipo: TipoLabor): Boolean
}
