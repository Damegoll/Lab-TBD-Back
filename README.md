# Lab-TBD-Back

API REST con **Spring Boot 3.5**, **Maven**, **Java 21** y **PostgreSQL**.

- **Sin ORM**: acceso a datos con `JdbcTemplate` y SQL escrito a mano (no hay JPA ni Hibernate).
- **Autenticación**: middleware (`JwtAuthFilter`) que valida un JWT en el header `Authorization: Bearer ...`.
- **Autorización por rol (RBAC)**: roles `ADMIN` y `USER`, definidos en `SecurityConfig`.

## Requisitos

- Java 21
- Maven 3.9+
- Docker (opcional, para levantar PostgreSQL)

## Puesta en marcha

```bash
docker compose up -d      # PostgreSQL en localhost:5433
mvn spring-boot:run       # API en http://localhost:8080
```

Al arrancar se ejecuta `src/main/resources/schema.sql` (crea las tablas si no existen) y se crea un
usuario `ADMIN` inicial.

> **Usuario inicial (solo desarrollo):** `admin` / `admin12345`.
> Cambia `ADMIN_PASSWORD` y `JWT_SECRET` en cualquier entorno que no sea local.

## Base de datos

El `docker-compose.yml` publica PostgreSQL en el puerto **5433** (no en el 5432) para no chocar con un
PostgreSQL instalado en tu equipo. Credenciales: base `lab_tbd`, usuario `labtbd`, contraseña `labtbd`.

### Usar tu propio PostgreSQL (sin Docker)

Crea el usuario y la base, por ejemplo desde `psql` como `postgres`:

```sql
CREATE USER labtbd WITH PASSWORD 'labtbd';
CREATE DATABASE lab_tbd OWNER labtbd;
```

y arranca la app apuntando a tu servidor (en IntelliJ: *Run > Edit Configurations > Environment variables*):

```
DB_URL=jdbc:postgresql://localhost:5432/lab_tbd;DB_USER=labtbd;DB_PASSWORD=labtbd
```

### Error "la autenticación password falló para el usuario labtbd"

La app llegó a un PostgreSQL, pero ese servidor no tiene el usuario `labtbd` con esa contraseña.
Casi siempre es porque se conectó a un PostgreSQL local en lugar del contenedor. Revisa:

1. `docker compose ps`: el contenedor `lab-tbd-postgres` debe estar `running`.
2. Que `DB_URL` (si lo definiste) apunte al puerto correcto: 5433 con Docker, o el de tu instalación local.
3. Si el volumen de Docker se creó antes con otras credenciales, recrea la base:
   `docker compose down -v && docker compose up -d` (borra los datos).

## Variables de entorno

| Variable                 | Valor por defecto                           |
|--------------------------|---------------------------------------------|
| `DB_URL`                 | `jdbc:postgresql://localhost:5433/lab_tbd`  |
| `DB_USER` / `DB_PASSWORD`| `labtbd` / `labtbd`                         |
| `PORT`                   | `8080`                                      |
| `CORS_ORIGINS`           | `http://localhost:5173` (el front con Vite) |
| `JWT_SECRET`             | valor de desarrollo (mín. 32 caracteres)    |
| `JWT_EXPIRATION_MINUTES` | `60`                                        |
| `ADMIN_USERNAME`         | `admin`                                     |
| `ADMIN_PASSWORD`         | `admin12345`                                |

## Endpoints y permisos

| Método | Ruta                | Acceso            | Descripción                    |
|--------|---------------------|-------------------|--------------------------------|
| POST   | `/api/auth/register`| Público           | Crea un usuario con rol `USER` |
| POST   | `/api/auth/login`   | Público           | Devuelve `{token, username, rol}` |
| GET    | `/api/tareas`       | `USER`, `ADMIN`   | Listar tareas                  |
| GET    | `/api/tareas/{id}`  | `USER`, `ADMIN`   | Obtener una tarea              |
| POST   | `/api/tareas`       | `USER`, `ADMIN`   | Crear tarea                    |
| PUT    | `/api/tareas/{id}`  | `USER`, `ADMIN`   | Actualizar tarea               |
| DELETE | `/api/tareas/{id}`  | solo `ADMIN`      | Eliminar tarea                 |
| GET    | `/api/usuarios`     | solo `ADMIN`      | Listar usuarios                |
| GET    | `/actuator/health`  | Público           | Estado del servicio            |

Sin token válido la API responde `401`; con token pero sin el rol requerido, `403`.

Ejemplo:

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin12345"}' | jq -r .token)

curl -X POST localhost:8080/api/tareas \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"titulo":"Primera tarea","descripcion":"Probar la API"}'
```

## Estructura (por capas)

```
src/main/java/com/labtbd
├── LabTbdApplication.java
├── controller/   # AuthController, TareaController, UsuarioController (HTTP)
├── service/      # AuthService, TareaService, UsuarioService (lógica de negocio)
├── repository/   # TareaRepository, UsuarioRepository (SQL con JdbcTemplate)
├── entity/       # Tarea, Usuario (filas de la base de datos)
├── dto/          # objetos de entrada/salida de la API (requests y responses)
├── security/     # JwtService y JwtAuthFilter (middleware de autenticación)
└── config/       # SecurityConfig (RBAC, CORS, BCrypt) y AdminInitializer
src/main/resources
├── application.yml
└── schema.sql
```

Flujo de una petición: `controller` -> `service` -> `repository` -> PostgreSQL. El controller solo
conoce DTOs, el repository solo conoce entities.

`Tarea` es solo un CRUD de ejemplo: reemplázalo por tu dominio real.
