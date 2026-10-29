# Lab-TBD-Back

API REST en Java 21 / Spring Boot 3.5, PostgreSQL y `JdbcTemplate`. El proyecto no usa JPA, Hibernate ni otro ORM: los repositorios ejecutan SQL explícito.

## Despliegue local con Docker Compose

Compose levanta PostgreSQL y la API. Requiere Docker con Docker Compose v2:

```powershell
docker compose up --build -d
docker compose ps
docker compose logs -f api
```

La API queda en `http://localhost:8080`; PostgreSQL en `localhost:5433`. Para detener los contenedores sin borrar la base:

```powershell
docker compose down
```

Para borrar también los datos persistidos (acción destructiva):

```powershell
docker compose down -v
```

La primera ejecución construye la imagen de la aplicación y puede tardar mientras Maven descarga dependencias. El servicio `api` espera a que PostgreSQL esté saludable antes de arrancar.

### Ejecutar la API desde Maven

Con Maven 3.9+ y PostgreSQL ya ejecutándose:

```powershell
mvn spring-boot:run
```

## Estado del despliegue

Compose es un despliegue local de desarrollo, no una configuración para nube/producción. No hay todavía pipeline CI/CD, TLS, proxy inverso ni infraestructura cloud. En cada `docker compose up`, el servicio `db-init` aplica idempotentemente el esquema y los triggers/procedimientos antes de iniciar la API.

El esquema reside en `src/main/resources/SQLResources/schema.sql`; los triggers y procedimientos en `src/main/resources/SQLResources/triggers_y_procedures.sql`. La inicialización crea las tablas de usuarios, billeteras, estaciones, vehículos, viajes, tarifas, traslados y auditoría, además de conservar `tareas` como ejemplo. En ejecuciones locales de Maven sin Compose, aplica ambos scripts con `psql` antes de probar los endpoints del dominio.

## Autenticación y permisos

- Las rutas `/api/v1/auth/register`, `/api/v1/auth/login` y `/actuator/health` son públicas.
- El registro público siempre asigna el rol `USUARIO`; no acepta el rol desde el cliente.
- El resto de rutas requiere `Authorization: Bearer <JWT>`.
- Roles disponibles: `USUARIO`, `OPERADOR` y `ADMIN`. Los endpoints también pueden aplicar autorización por método con `@PreAuthorize`.
- La contraseña se almacena con BCrypt. Los tokens son stateless y duran 60 minutos por defecto.
- Usuario administrador inicial de desarrollo: `admin` / `admin12345`. Se crea solo si no existe.

Ejemplo de inicio de sesión:

```http
POST /api/v1/auth/login
Content-Type: application/json

{"username":"admin","password":"admin12345"}
```

La respuesta incluye `token`, `tokenType`, `username` y `rol`. Usa el token en las siguientes peticiones:

```http
Authorization: Bearer <token>
```

`401 Unauthorized` significa que falta o no es válido el token; `403 Forbidden` significa que el usuario autenticado no tiene el rol requerido.

## Configuración

| Variable | Valor local por defecto | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/lab_tbd` | URL JDBC. En Compose se reemplaza para usar el host `postgres`. |
| `DB_USER` | `labtbd` | Usuario PostgreSQL. |
| `DB_PASSWORD` | `labtbd` | Contraseña local PostgreSQL. |
| `PORT` | `8080` | Puerto HTTP de la API. |
| `CORS_ORIGINS` | `http://localhost:5173` | Orígenes permitidos, separados por coma. |
| `JWT_SECRET` | Secreto de desarrollo incluido en configuración | Clave HMAC de al menos 32 bytes. Debe sustituirse fuera de local. |
| `JWT_EXPIRATION_MINUTES` | `60` | Vigencia del JWT. |
| `ADMIN_USERNAME` | `admin` | Usuario administrador inicial. |
| `ADMIN_PASSWORD` | `admin12345` | Contraseña inicial; cambiar fuera de local. |
| `ADMIN_EMAIL` | `admin@localhost` | Email del administrador inicial. |

Para probar otro secreto local, define `JWT_SECRET` en el entorno antes de levantar la aplicación. En producción no uses las credenciales ni el secreto por defecto; usa secretos robustos administrados fuera del repositorio.

## Rutas implementadas

| Método | Ruta | Acceso |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Público; crea rol `USUARIO` |
| `POST` | `/api/v1/auth/login` | Público |
| `GET` | `/api/v1/usuarios/me` | JWT válido |
| `GET` | `/api/v1/usuarios` y `/api/v1/usuarios/{id}` | `ADMIN` |
| `POST` | `/api/v1/usuarios` | `ADMIN` |
| `PUT`, `DELETE` | `/api/v1/usuarios/{id}` | `ADMIN` |
| `GET`, `POST`, `PUT`, `PATCH` | `/api/v1/vehiculos...` | `OPERADOR` o `ADMIN` |
| `GET`, `POST`, `DELETE` | `/api/v1/tarifas...` | `OPERADOR` o `ADMIN` |
| `GET`, `POST` | `/api/v1/ordenes-traslado...` | `OPERADOR` o `ADMIN` |
| `GET` | `/api/v1/vehiculos/{id}/auditoria` | `OPERADOR` o `ADMIN` |
| `GET` | `/api/v1/estaciones/disponibles` | Usuario autenticado |
| `GET` | `/api/v1/usuarios/me/resumen-mensual` | Usuario autenticado; propio resumen |
| `GET` | `/api/v1/usuarios/{id}/resumen-mensual` | `ADMIN` |
| `GET` | `/actuator/health` | Público |
