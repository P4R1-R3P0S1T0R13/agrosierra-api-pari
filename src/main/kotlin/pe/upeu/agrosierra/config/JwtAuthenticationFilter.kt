package pe.upeu.agrosierra.config

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Lee la cabecera Authorization, verifica la firma del token y deja en el contexto
 * un usuario cuyas autoridades son sus privilegios. Si el token falta o es inválido
 * no autentica: el punto de entrada de SecurityConfig responde 401.
 */
@Component
class JwtAuthenticationFilter(private val jwt: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, cadena: FilterChain) {
        val cabecera = req.getHeader(HttpHeaders.AUTHORIZATION)
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            try {
                val usuario = jwt.verificar(cabecera.removePrefix(PREFIJO).trim())
                val autoridades = usuario.privilegios.map { SimpleGrantedAuthority(it) }
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(usuario, null, autoridades)
            } catch (e: ExpiredJwtException) {
                req.setAttribute(ATRIBUTO_ERROR, "El token vencio")
            } catch (e: JwtException) {
                req.setAttribute(ATRIBUTO_ERROR, "Token invalido")
            } catch (e: IllegalArgumentException) {
                req.setAttribute(ATRIBUTO_ERROR, "Token invalido")
            }
        }
        cadena.doFilter(req, res)
    }

    companion object {
        const val PREFIJO = "Bearer "
        const val ATRIBUTO_ERROR = "agro.jwt.error"
    }
}
