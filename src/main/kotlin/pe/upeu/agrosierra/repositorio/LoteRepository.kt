package pe.upeu.agrosierra.repositorio

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import pe.upeu.agrosierra.dominio.Lote

interface LoteRepository : JpaRepository<Lote, Long>, JpaSpecificationExecutor<Lote> {
    fun findByCodigo(codigo: String): Lote?

    companion object {
        fun delValle(valle: String?) = Specification<Lote> { raiz, _, cb ->
            if (valle.isNullOrBlank()) null else cb.equal(cb.lower(raiz.get("valle")), valle.lowercase())
        }

        fun conCampaniaCerrada(cerrada: Boolean?) = Specification<Lote> { raiz, _, cb ->
            cerrada?.let { cb.equal(raiz.get<Boolean>("campaniaCerrada"), it) }
        }
    }
}
