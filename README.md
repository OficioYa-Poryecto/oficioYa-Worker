# oficioYa-Worker

Microservicio del dominio **Worker** de OficioYa: catálogo de oficios y categorías, oficios del trabajador
(principal y secundarios) con sus tarifas, zonas de cobertura, disponibilidad y galería de fotos.

Stack: Java 21, Spring Boot 4, PostgreSQL (H2 en pruebas), Spring Security + JWT, MapStruct, springdoc (Swagger).

## Variables de entorno

La aplicación no trae credenciales en el código. Si falta una variable obligatoria, no arranca.

| Variable | Obligatoria | Descripción | Ejemplo |
|---|---|---|---|
| `DB_URL` | Sí | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/oficioya_worker` |
| `DB_USER` | Sí | Usuario de la base de datos | `oficioya` |
| `DB_PASSWORD` | Sí | Contraseña de la base de datos | — |
| `JWT_SECRET` | Sí | Clave HS256 para validar los tokens. Debe ser **la misma que usa Identity** para firmarlos. Mínimo 32 caracteres | — |
| `JWT_EXPIRATION_MINUTES` | No (60) | Vigencia de los tokens que genere este servicio | `60` |
| `PUBLIC_QUERIES` | No (`false`) | `true` permite los GET de consulta (catálogo, trabajadores, disponibilidad, fotos) sin token | `false` |

Para desarrollo local, copia `.env.example` a `.env` (ignorado por git) y llénalo. Para generar una clave:

```bash
openssl rand -base64 48
```

En Azure, configúralas como *Application settings* del App Service (o secretos de Key Vault), nunca en el repositorio.

Las pruebas usan el perfil `test` (`src/test/resources/application-test.properties`) con H2 y una clave de prueba,
así que `./mvnw test` no necesita variables de entorno.

## Ejecutar

```bash
# Linux/macOS (con las variables ya exportadas)
./mvnw spring-boot:run

# Windows PowerShell
$env:DB_URL="jdbc:postgresql://localhost:5432/oficioya_worker"; $env:DB_USER="..."; $env:DB_PASSWORD="..."; $env:JWT_SECRET="..."
./mvnw spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui.html` (público).

## Seguridad

- Autenticación con `Authorization: Bearer <token>`. Este servicio **no tiene login**: los tokens los emite Identity.
- Claims usados: `sub` = id del usuario y `role` = `TRABAJADOR`, `CONTRATANTE` o `ADMIN`
  (supuesto pendiente de confirmar con Identity; se leen en un único método, `JwtService.toClaims`).
- Reglas:
  - Solo `ADMIN` gestiona el catálogo de oficios y categorías.
  - Cada `TRABAJADOR` solo modifica su propio perfil (oficios, tarifas, zonas, disponibilidad y fotos).
  - Las consultas requieren estar autenticado (configurable con `PUBLIC_QUERIES`).
- El perfil público del trabajador no expone correo ni teléfono.

Todos los errores responden con el mismo formato:

```json
{ "timestamp": "2026-10-30T10:15:00", "status": 401, "error": "Unauthorized",
  "message": "Token has expired", "path": "/api/v1/trades" }
```

| Código | Cuándo |
|---|---|
| 400 | Datos inválidos o JSON mal formado |
| 401 | Sin token, o token inválido, expirado o con firma incorrecta |
| 403 | Rol insuficiente o perfil ajeno |
| 404 | Recurso no encontrado |
| 409 | Regla de negocio incumplida (duplicados, trabajador pausado, franjas que se cruzan…) |

## Pruebas

```bash
./mvnw clean test
```

> Si el proyecto está dentro de OneDrive, la sincronización puede corromper los `.class` de `target/` y producir
> fallos intermitentes (`NoClassDefFoundError`, beans de mappers no encontrados). Excluye `target/` de la
> sincronización o trabaja el proyecto fuera de OneDrive.
