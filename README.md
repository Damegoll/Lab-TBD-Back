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
docker compose up -d      # PostgreSQL en localhost:5432
mvn spring-boot:run       # API en http://localhost:8080
```

Al arrancar se ejecuta `src/main/resources/schema.sql` (crea las tablas si no existen) y se crea un
usuario `ADMIN` inicial.

> **Usuario inicial (solo desarrollo):** `admin` / `admin12345`.
> Cambia `ADMIN_PASSWORD` y `JWT_SECRET` en cualquier entorno que no sea local.

## Variables de entorno

| Variable                 | Valor por defecto                           |
|--------------------------|---------------------------------------------|
| `DB_URL`                 | `jdbc:postgresql://localhost:5432/lab_tbd`  |
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

## Estructura

```
src/main/java/com/labtbd
├── LabTbdApplication.java
├── security/   # JwtService, JwtAuthFilter (middleware), SecurityConfig (RBAC), AuthController, AdminInitializer
├── usuario/    # usuarios: repositorio JDBC y listado (ADMIN)
└── tarea/      # CRUD de ejemplo (repositorio JDBC + controlador)
src/main/resources
├── application.yml
└── schema.sql
```

`tarea/` es solo un CRUD de ejemplo: reemplázalo por tu dominio real.
