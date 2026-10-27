package pe.upeu.agrosierra.config

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.CAMPANIA_CERRAR
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.LABOR_CREAR
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.LABOR_EDITAR
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.LABOR_ELIMINAR
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.LABOR_LEER
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.LOTE_LEER
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.USUARIO_GESTIONAR
import pe.upeu.agrosierra.web.dto.ErrorDto

/**
 * Matriz de autorización del contrato 4.3: cada ruta exige un PRIVILEGIO, nunca un rol.
 * Qué privilegios tiene cada rol se decide en la base de datos (tabla rol_privilegio).
 */
@Configuration
class SecurityConfig(
    private val filtroJwt: JwtAuthenticationFilter,
    private val mapper: ObjectMapper,
) {

    @Bean
    fun cadenaDeSeguridad(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                it.requestMatchers("/error").permitAll()

                it.requestMatchers(HttpMethod.GET, "/api/v1/lotes/*/labores").hasAuthority(LABOR_LEER)
                it.requestMatchers(HttpMethod.GET, "/api/v1/lotes", "/api/v1/lotes/*").hasAuthority(LOTE_LEER)
                it.requestMatchers(HttpMethod.POST, "/api/v1/lotes/*/cerrar-campania").hasAuthority(CAMPANIA_CERRAR)

                it.requestMatchers(HttpMethod.POST, "/api/v1/labores").hasAuthority(LABOR_CREAR)
                it.requestMatchers(HttpMethod.PUT, "/api/v1/labores/*").hasAuthority(LABOR_EDITAR)
                it.requestMatchers(HttpMethod.DELETE, "/api/v1/labores/*").hasAuthority(LABOR_ELIMINAR)

                it.requestMatchers(HttpMethod.GET, "/api/v1/usuarios/me").authenticated()
                it.requestMatchers("/api/v1/usuarios", "/api/v1/usuarios/**").hasAuthority(USUARIO_GESTIONAR)

                it.anyRequest().authenticated()
            }
            .exceptionHandling {
                // Sin token, alterado o vencido -> 401
                it.authenticationEntryPoint { req, res, _ ->
                    val motivo = req.getAttribute(JwtAuthenticationFilter.ATRIBUTO_ERROR) as? String
                        ?: "Se requiere autenticacion"
                    escribirError(req, res, HttpStatus.UNAUTHORIZED, motivo)
                }
                // Autenticado pero sin el privilegio -> 403
                it.accessDeniedHandler { req, res, _ ->
                    escribirError(req, res, HttpStatus.FORBIDDEN, "No tiene el privilegio requerido para esta operacion")
                }
            }
            .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun codificadorDeContrasenas(): PasswordEncoder = BCryptPasswordEncoder()

    private fun escribirError(req: HttpServletRequest, res: HttpServletResponse, estado: HttpStatus, mensaje: String) {
        res.status = estado.value()
        res.contentType = MediaType.APPLICATION_JSON_VALUE
        res.characterEncoding = "UTF-8"
        mapper.writeValue(res.outputStream, ErrorDto.de(estado.value(), estado.reasonPhrase, mensaje, req.requestURI))
    }
}
