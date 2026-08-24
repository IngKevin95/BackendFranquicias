# Dise├▒o: Usuarios, Roles y Autenticacion Real con JWT

**Fecha:** 2026-08-24  
**Estado:** Aprobado

---

## Contexto

La implementacion actual de autenticacion JWT usa credenciales hardcoded en `AuthController.java`. No existe tabla de usuarios, no hay BCrypt, y todos los tokens tienen el mismo "rol" implicito de admin. Este spec define la implementacion real con tabla de usuarios en PostgreSQL, roles granulares en el JWT, y control de acceso por endpoint.

---

## Modelo de datos

### Tabla `usuario`

```sql
CREATE TYPE rol_usuario AS ENUM ('ADMIN', 'WRITE', 'READ');

CREATE TABLE usuario (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    role          rol_usuario NOT NULL,
    activo        BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

- `password_hash`: hash BCrypt del password en texto plano. Factor de costo 10.
- `role`: enum nativo PostgreSQL. Un usuario tiene exactamente un rol.
- `activo`: permite deshabilitar un usuario sin borrarlo. Usuarios inactivos reciben 401 en login.
- Seed inicial: usuario `admin` con password `<password-provista-por-separado>` y rol `ADMIN`, insertado via Flyway `V4`.

---

## Roles y permisos

| Rol | Descripcion |
|-----|-------------|
| `READ` | Solo puede consultar: `GET /api/v1/**/max-stock` |
| `WRITE` | Puede leer y escribir sobre franquicias, sucursales y productos (crear, renombrar, eliminar, modificar stock) |
| `ADMIN` | Todo lo de WRITE mas crear usuarios via `POST /api/v1/usuarios` |

### Matriz de acceso por endpoint

| Metodo HTTP | Path pattern | READ | WRITE | ADMIN |
|-------------|-------------|------|-------|-------|
| GET | `/api/v1/**/max-stock` | si | si | si |
| POST | `/api/v1/franquicias` | no | si | si |
| PATCH | `/api/v1/franquicias/**` | no | si | si |
| POST | `/api/v1/franquicias/**/sucursales` | no | si | si |
| PATCH | `/api/v1/franquicias/**/sucursales/**` | no | si | si |
| POST | `/api/v1/franquicias/**/productos` | no | si | si |
| PATCH | `/api/v1/franquicias/**/productos/**` | no | si | si |
| DELETE | `/api/v1/franquicias/**/productos/**` | no | si | si |
| POST | `/api/v1/usuarios` | no | no | si |
| POST | `/api/v1/auth/login` | publico | publico | publico |
| GET | `/actuator/**` | publico | publico | publico |
| GET | `/swagger-ui/**` | publico | publico | publico |
| GET | `/v3/api-docs/**` | publico | publico | publico |

---

## JWT con claim de rol

El token pasa de tener solo `sub` a incluir el claim `role`:

```json
{
  "sub": "juan",
  "role": "WRITE",
  "iat": 1234567890,
  "exp": 1234603890
}
```

- `JwtUtil.generateToken(String username, String role)`: firma el token con ambos claims.
- `JwtUtil.extractRole(String token)`: extrae el claim `role`.
- El `WebFilter` en `SecurityConfig` extrae username + role, construye `UsernamePasswordAuthenticationToken` con `SimpleGrantedAuthority("ROLE_" + role)` y lo registra en `ReactiveSecurityContextHolder`.

---

## Arquitectura hexagonal ÔÇö archivos nuevos y modificados

### Dominio
- `domain/model/RolUsuario.java` ÔÇö enum: `ADMIN`, `WRITE`, `READ`
- `domain/model/Usuario.java` ÔÇö record: `id`, `username`, `passwordHash`, `email`, `role`, `activo`, `createdAt`
- `domain/port/UsuarioRepositoryPort.java` ÔÇö `findByUsername(String): Mono<Usuario>`, `save(Usuario): Mono<Usuario>`

### Infraestructura ÔÇö persistencia
- `infrastructure/persistence/entity/UsuarioEntity.java` ÔÇö entidad R2DBC mapeada a tabla `usuario`
- `infrastructure/persistence/mapper/UsuarioMapper.java` ÔÇö `toEntity(Usuario)`, `toDomain(UsuarioEntity)`
- `infrastructure/persistence/repository/UsuarioR2dbcRepository.java` ÔÇö `ReactiveCrudRepository<UsuarioEntity, UUID>` con `findByUsername`
- `infrastructure/persistence/adapter/UsuarioRepositoryAdapter.java` ÔÇö implementa `UsuarioRepositoryPort`

### Infraestructura ÔÇö seguridad
- `infrastructure/config/security/JwtUtil.java` ÔÇö MODIFICADO: `generateToken(username, role)`, `extractRole(token)`
- `infrastructure/config/security/SecurityConfig.java` ÔÇö MODIFICADO: `WebFilter` extrae role, reglas `hasRole` por path+metodo
- `infrastructure/config/security/PasswordEncoderConfig.java` ÔÇö NUEVO: bean `BCryptPasswordEncoder`

### Infraestructura ÔÇö web
- `infrastructure/web/controller/AuthController.java` ÔÇö MODIFICADO: consulta BD, verifica BCrypt, genera token con role
- `infrastructure/web/controller/UsuarioController.java` ÔÇö NUEVO: `POST /api/v1/usuarios` (solo ADMIN)
- `infrastructure/web/dto/auth/RegisterRequest.java` ÔÇö NUEVO: record `username`, `password`, `email`, `role`
- `infrastructure/web/dto/UsuarioResponse.java` ÔÇö NUEVO: record `id`, `username`, `email`, `role`, `activo`, `createdAt`

### Migracion
- `resources/db/migration/V4__usuarios.sql` ÔÇö tabla `usuario`, tipo enum, seed admin

---

## Manejo de errores

| Situacion | HTTP | Mensaje |
|-----------|------|---------|
| Credenciales incorrectas | 401 | "Credenciales invalidas" |
| Usuario inactivo | 401 | "Usuario inactivo" |
| Token ausente o invalido | 401 | (sin body, Spring Security) |
| Rol insuficiente | 403 | (sin body, Spring Security) |
| Username ya existe | 409 | "El username ya existe" |
| Email ya existe | 409 | "El email ya existe" |

---

## Tests

- `UsuarioRepositoryAdapterTest`: test de integracion con Testcontainers ÔÇö `save` y `findByUsername`
- `AuthControllerTest`: login exitoso retorna token con claim `role`; login con password incorrecto retorna 401; usuario inactivo retorna 401
- `UsuarioControllerTest`: ADMIN puede crear usuario; WRITE recibe 403; READ recibe 403
- Todos los `*ControllerTest` existentes: actualizar tokens para incluir role en claim
- `SecurityConfigTest`: verificar que READ no puede hacer POST a `/api/v1/franquicias`
