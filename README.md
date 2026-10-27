# AgroAPI · AgroSierra (PARI)

Servicio REST de la cooperativa AgroSierra: padrón de lotes, labores de campo (riego, fumigación,
cosecha) y cierre de campaña. Autenticación JWT y autorización **por privilegio**.
Lo consume la aplicación Android `agrosierra-app-pari`.

- **Stack:** Kotlin 2.1 · Spring Boot 3.4 · Spring Security · Spring Data JPA · PostgreSQL 16 · JJWT 0.12
- **Puerto:** `8080` · **Prefijo:** `/api/v1`

## Ejecución

### Opción A: un solo comando (recomendada)

```bash
docker compose up --build
```

Levanta PostgreSQL y el servicio. Al iniciar se cargan los usuarios, roles, privilegios, 8 lotes y 20 labores.
Para reiniciar con datos limpios: `docker compose down -v && docker compose up --build`.

### Opción B: base en Docker y servicio desde el IDE

```bash
docker compose up -d db
./gradlew bootRun
```

### Pruebas

```bash
./gradlew test      # 13 pruebas de integración (MockMvc + H2 en memoria)
```

La colección `docs/AgroAPI-PARI.postman_collection.json` (Postman o Bruno) recorre todo el contrato,
incluidos los ejemplos de 401, 403, 400, 404 y 409. Ejecuta primero la carpeta **1. Autenticacion**.

## Usuarios de prueba

| Usuario    | Contraseña  | Rol           |
|------------|-------------|---------------|
| `tecnico1` | `Agro2026*` | TECNICO       |
| `tecnico2` | `Agro2026*` | TECNICO (sirve para demostrar RN-02) |
| `jefe1`    | `Agro2026*` | JEFE_CAMPO    |
| `admin`    | `Agro2026*` | ADMINISTRADOR |

En la base solo se guarda el hash BCrypt de la contraseña.

## Estructura de paquetes

```
src/main/kotlin/pe/upeu/agrosierra/
├── config/       SecurityConfig, JwtAuthenticationFilter, JwtService, ManejadorDeErrores,
│                 SemillaSeguridad, SemillaCampo
├── seguridad/    Usuario, Rol, Privilegio, UsuarioRepository, AuthController, UsuarioAutenticado
├── dominio/      Lote, Labor, TipoLabor, Excepciones
├── repositorio/  LoteRepository, LaborRepository
├── servicio/     LoteService (RN-03), LaborService (RN-01, RN-02, RN-04), transacciones
├── web/          LoteController, LaborController, UsuarioController
│   └── dto/      PaginaDto, ErrorDto, LoteDto, LaborDto/LaborRequest, UsuarioDto
└── Aplicacion.kt
```

## Contrato del API

| Verbo  | Ruta                                       | Privilegio       | Códigos                 |
|--------|--------------------------------------------|------------------|-------------------------|
| POST   | `/api/v1/auth/login`                       | Público          | 200 · 400 · 401         |
| GET    | `/api/v1/lotes?pagina&tamanio&valle&estado`| LOTE_LEER        | 200 · 401 · 403         |
| GET    | `/api/v1/lotes/{id}`                       | LOTE_LEER        | 200 · 401 · 404         |
| GET    | `/api/v1/lotes/{id}/labores?pagina&tamanio`| LABOR_LEER       | 200 · 401 · 404         |
| POST   | `/api/v1/labores`                          | LABOR_CREAR      | 201 · 400 · 401 · 403 · 409 |
| PUT    | `/api/v1/labores/{id}`                     | LABOR_EDITAR     | 200 · 400 · 401 · 403 · 404 · 409 |
| DELETE | `/api/v1/labores/{id}`                     | LABOR_ELIMINAR   | 204 · 401 · 403 · 404   |
| POST   | `/api/v1/lotes/{id}/cerrar-campania`       | CAMPANIA_CERRAR  | 200 · 401 · 403 · 404 · 409 |
| GET    | `/api/v1/usuarios/me`                      | Autenticado      | 200 · 401               |
| GET    | `/api/v1/usuarios`                         | USUARIO_GESTIONAR| 200 · 401 · 403         |

`estado` admite `ABIERTA` o `CERRADA`. Las labores se devuelven de la más reciente a la más antigua.

**Paginación** (todos los listados):

```json
{ "contenido": [], "pagina": 0, "tamanio": 20, "totalElementos": 37, "totalPaginas": 2, "ultima": false }
```

**Error** (todo el servicio):

```json
{ "timestamp": "2026-10-27T09:14:22", "status": 400, "error": "Bad Request",
  "message": "Datos invalidos", "path": "/api/v1/labores",
  "validationErrors": { "cantidad": "La dosis supera el maximo del lote: 6.30 (3.50 por ha x 1.80 ha)" } }
```

**Login:**

```json
// POST /api/v1/auth/login
{ "usuario": "tecnico1", "contrasena": "Agro2026*" }
// 200
{ "token": "eyJ...", "tipo": "Bearer", "expiraEn": 1793133494123, "usuario": "tecnico1",
  "nombre": "Juan Quispe Mamani", "rol": "TECNICO",
  "privilegios": ["LABOR_CREAR", "LABOR_EDITAR", "LABOR_LEER", "LOTE_LEER"] }
```

**Labor (POST/PUT):**

```json
{ "loteId": 2, "tipo": "FUMIGACION", "fecha": "2026-10-27", "responsable": "Juan Quispe Mamani",
  "cantidad": 5.0, "observacion": "Control de cogollero" }
```

## Roles y privilegios

| Privilegio          | TECNICO | JEFE_CAMPO | ADMINISTRADOR |
|---------------------|:-------:|:----------:|:-------------:|
| LOTE_LEER           | ✔ | ✔ | ✔ |
| LABOR_LEER          | ✔ | ✔ | ✔ |
| LABOR_CREAR         | ✔ | ✔ |   |
| LABOR_EDITAR        | ✔ (solo propias, 24 h) | ✔ | |
| LABOR_EDITAR_TODAS  |   | ✔ |   |
| LABOR_ELIMINAR      |   | ✔ |   |
| CAMPANIA_CERRAR     |   | ✔ |   |
| USUARIO_GESTIONAR   |   |   | ✔ |

## Decisiones de arquitectura

1. **La seguridad vive en el servicio.** `JwtAuthenticationFilter` verifica la firma y la vigencia del token
   y carga los **privilegios** como autoridades. `SecurityConfig` asocia cada ruta a un privilegio
   (`hasAuthority`). Sin token, o con un token alterado o vencido, responde **401**. Con un token válido
   pero sin el privilegio, responde **403**. Ningún controlador compara nombres de rol.
2. **La matriz rol → privilegios está en la base de datos** (tabla `rol_privilegio`), cargada por
   `SemillaSeguridad`. Para agregar un rol nuevo basta registrarlo con sus privilegios, sin tocar
   filtros ni controladores.
3. **RN-02 se resuelve con un privilegio de alcance** (`LABOR_EDITAR_TODAS`), no con un `if` sobre el rol:
   quien no lo tiene solo corrige lo que él registró (`registradoPor`) y dentro de 24 h (`registradoEn`).
4. **Las reglas de negocio están en `servicio/`**, dentro de transacciones: RN-01 y RN-03 → `409`,
   RN-02 → `403`, RN-04 → `400` con el detalle en `validationErrors.cantidad`.
5. **Un único formato de error** (`ManejadorDeErrores` y los manejadores 401/403 de `SecurityConfig`)
   para que la aplicación traduzca cada respuesta a un error tipado.
6. **Token sin renovación**, vigencia de 1 hora, firmado con HMAC-SHA. Transporta `uid`, usuario, nombre,
   rol y privilegios. El secreto se inyecta con `JWT_SECRETO`.
