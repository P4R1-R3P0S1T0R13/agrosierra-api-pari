package pe.upeu.agrosierra.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pe.upeu.agrosierra.seguridad.Usuario
import pe.upeu.agrosierra.seguridad.UsuarioAutenticado
import java.time.Duration
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

data class TokenEmitido(val token: String, val expiraEn: Instant)

/** Firma (HS256) y verifica los tokens. El token transporta id, usuario, rol y privilegios. */
@Service
class JwtService(
    @Value("\${agro.jwt.secreto}") secreto: String,
    @Value("\${agro.jwt.vigencia-minutos}") vigenciaMinutos: Long,
) {
    private val clave: SecretKey = Keys.hmacShaKeyFor(secreto.toByteArray())
    private val vigencia: Duration = Duration.ofMinutes(vigenciaMinutos)

    fun generar(usuario: Usuario): TokenEmitido {
        val ahora = Instant.now()
        val expira = ahora.plus(vigencia)
        val token = Jwts.builder()
            .subject(usuario.usuario)
            .claim("uid", usuario.id)
            .claim("nombre", usuario.nombre)
            .claim("rol", usuario.rol.nombre)
            .claim("privilegios", usuario.nombresDePrivilegios().toList())
            .issuedAt(Date.from(ahora))
            .expiration(Date.from(expira))
            .signWith(clave)
            .compact()
        return TokenEmitido(token, expira)
    }

    /** Verifica firma y vigencia; lanza [JwtException] si el token fue alterado o venció. */
    @Throws(JwtException::class)
    fun verificar(token: String): UsuarioAutenticado {
        val claims: Claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).payload
        @Suppress("UNCHECKED_CAST")
        val privilegios = (claims["privilegios"] as? List<String>).orEmpty().toSet()
        return UsuarioAutenticado(
            id = (claims["uid"] as Number).toLong(),
            usuario = claims.subject,
            nombre = claims["nombre"] as String,
            rol = claims["rol"] as String,
            privilegios = privilegios,
        )
    }
}
