package pe.upeu.agrosierra

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.transaction.annotation.Transactional
import pe.upeu.agrosierra.repositorio.LaborRepository
import pe.upeu.agrosierra.repositorio.LoteRepository

/** Verifica el contrato 4.3: códigos de estado, privilegios y reglas RN-01 a RN-04. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContratoApiTest {

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper
    @Autowired lateinit var lotes: LoteRepository
    @Autowired lateinit var labores: LaborRepository

    private fun token(usuario: String): String {
        val cuerpo = mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuario":"$usuario","contrasena":"Agro2026*"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readTree(cuerpo)["token"].asText()
    }

    private fun idLote(codigo: String) = lotes.findByCodigo(codigo)!!.id!!

    private fun labor(loteId: Long, tipo: String = "RIEGO", cantidad: Double = 50.0) =
        """{"loteId":$loteId,"tipo":"$tipo","fecha":"2026-10-27","responsable":"Juan Quispe","cantidad":$cantidad,"observacion":"prueba"}"""

    private fun crear(usuario: String, cuerpo: String): ResultActionsDsl = mvc.post("/api/v1/labores") {
        header("Authorization", "Bearer ${token(usuario)}")
        contentType = MediaType.APPLICATION_JSON
        content = cuerpo
    }

    @Test
    fun `login devuelve token, nombre y privilegios`() {
        mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuario":"jefe1","contrasena":"Agro2026*"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.token") { isNotEmpty() }
            jsonPath("$.nombre") { value("Carlos Pari Ticona") }
            jsonPath("$.privilegios") { isArray() }
        }
    }

    @Test
    fun `credenciales incorrectas responden 401`() {
        mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuario":"jefe1","contrasena":"otra"}"""
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `sin token o con token alterado responde 401`() {
        mvc.get("/api/v1/lotes").andExpect { status { isUnauthorized() } }
        // Se cambia el contenido (payload) sin volver a firmar: la firma ya no coincide
        val (cabecera, contenido, firma) = token("tecnico1").split(".")
        val alterado = listOf(cabecera, contenido.dropLast(2) + (if (contenido.endsWith("AA")) "BB" else "AA"), firma)
            .joinToString(".")
        mvc.get("/api/v1/lotes") { header("Authorization", "Bearer $alterado") }
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `listado usa el envoltorio de paginacion y filtra por valle`() {
        mvc.get("/api/v1/lotes?pagina=0&tamanio=2&valle=Mantaro") { header("Authorization", "Bearer ${token("tecnico1")}") }
            .andExpect {
                status { isOk() }
                jsonPath("$.contenido.length()") { value(2) }
                jsonPath("$.totalElementos") { value(3) }
                jsonPath("$.totalPaginas") { value(2) }
                jsonPath("$.ultima") { value(false) }
            }
    }

    @Test
    fun `tecnico registra una labor y recibe 201`() {
        crear("tecnico1", labor(idLote("LT-02"))).andExpect {
            status { isCreated() }
            jsonPath("$.id") { isNumber() }
        }
    }

    @Test
    fun `tecnico con token valido no puede eliminar ni cerrar campania - 403`() {
        val t = token("tecnico1")
        mvc.delete("/api/v1/labores/${labores.findAll().first().id}") { header("Authorization", "Bearer $t") }
            .andExpect { status { isForbidden() } }
        mvc.post("/api/v1/lotes/${idLote("LT-01")}/cerrar-campania") { header("Authorization", "Bearer $t") }
            .andExpect { status { isForbidden() } }
    }

    @Test
    fun `administrador no registra labores - 403`() {
        crear("admin", labor(idLote("LT-02"))).andExpect { status { isForbidden() } }
    }

    @Test
    fun `formulario invalido responde 400 con detalle por campo`() {
        crear("tecnico1", """{"loteId":${idLote("LT-02")},"tipo":"PODA","fecha":"27/10","responsable":"","cantidad":-1}""")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.validationErrors.tipo") { exists() }
                jsonPath("$.validationErrors.fecha") { exists() }
                jsonPath("$.validationErrors.responsable") { exists() }
                jsonPath("$.validationErrors.cantidad") { exists() }
            }
    }

    @Test
    fun `RN-01 lote con campania cerrada responde 409`() {
        crear("tecnico1", labor(idLote("LT-03"))).andExpect { status { isConflict() } }
    }

    @Test
    fun `RN-02 tecnico no corrige labores de otro tecnico ni vencidas`() {
        val t = token("tecnico1")
        val deOtro = labores.findAll().first { it.registradoPor == "tecnico2" }
        mvc.put("/api/v1/labores/${deOtro.id}") {
            header("Authorization", "Bearer $t"); contentType = MediaType.APPLICATION_JSON
            content = labor(deOtro.lote.id!!)
        }.andExpect { status { isForbidden() } }

        val vencida = labores.findAll().first { it.registradoPor == "tecnico1" && it.lote.codigo == "LT-05" }
        mvc.put("/api/v1/labores/${vencida.id}") {
            header("Authorization", "Bearer $t"); contentType = MediaType.APPLICATION_JSON
            content = labor(vencida.lote.id!!)
        }.andExpect { status { isForbidden() } }

        val reciente = labores.findAll().first { it.registradoPor == "tecnico1" && it.lote.codigo == "LT-02" }
        mvc.put("/api/v1/labores/${reciente.id}") {
            header("Authorization", "Bearer $t"); contentType = MediaType.APPLICATION_JSON
            content = labor(reciente.lote.id!!, "FUMIGACION", 4.0)
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `RN-03 no se cierra la campania sin cosecha y si con ella`() {
        val t = token("jefe1")
        mvc.post("/api/v1/lotes/${idLote("LT-02")}/cerrar-campania") { header("Authorization", "Bearer $t") }
            .andExpect { status { isConflict() } }
        mvc.post("/api/v1/lotes/${idLote("LT-01")}/cerrar-campania") { header("Authorization", "Bearer $t") }
            .andExpect {
                status { isOk() }
                jsonPath("$.campaniaCerrada") { value(true) }
            }
    }

    @Test
    fun `RN-04 fumigacion sobre la dosis maxima responde 400 en cantidad`() {
        // LT-02: 1.8 ha x 3.5 = 6.3
        crear("tecnico1", labor(idLote("LT-02"), "FUMIGACION", 6.4)).andExpect {
            status { isBadRequest() }
            jsonPath("$.validationErrors.cantidad") { exists() }
        }
    }

    @Test
    fun `jefe elimina una labor y responde 204 sin cuerpo, luego 404`() {
        val id = labores.findAll().first().id
        val t = token("jefe1")
        mvc.delete("/api/v1/labores/$id") { header("Authorization", "Bearer $t") }
            .andExpect { status { isNoContent() }; content { string("") } }
        mvc.delete("/api/v1/labores/$id") { header("Authorization", "Bearer $t") }
            .andExpect { status { isNotFound() } }
    }
}
