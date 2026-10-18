# proyecto-backend

API REST con **Spring Boot 3.5**, **Maven**, **Java 21** y **PostgreSQL**.

## Requisitos

- Java 21
- Maven 3.9+
- Docker (opcional, para levantar PostgreSQL)

## Puesta en marcha

1. Levantar PostgreSQL:

   ```bash
   docker compose up -d
   ```

2. Ejecutar la aplicación:

   ```bash
   mvn spring-boot:run
   ```

La API queda en `http://localhost:8080`.

## Variables de entorno

| Variable        | Valor por defecto                          |
|-----------------|--------------------------------------------|
| `DB_URL`        | `jdbc:postgresql://localhost:5432/proyecto` |
| `DB_USER`       | `proyecto`                                 |
| `DB_PASSWORD`   | `proyecto`                                 |
| `PORT`          | `8080`                                     |
| `CORS_ORIGINS`  | `http://localhost:5173` (el front en Vite) |

## Endpoints de ejemplo

| Método | Ruta               | Descripción        |
|--------|--------------------|--------------------|
| GET    | `/api/tareas`      | Listar tareas      |
| GET    | `/api/tareas/{id}` | Obtener una tarea  |
| POST   | `/api/tareas`      | Crear tarea        |
| PUT    | `/api/tareas/{id}` | Actualizar tarea   |
| DELETE | `/api/tareas/{id}` | Eliminar tarea     |
| GET    | `/actuator/health` | Estado del servicio|

Ejemplo:

```bash
curl -X POST localhost:8080/api/tareas \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Primera tarea","descripcion":"Probar la API"}'
```

## Estructura

```
src/main/java/com/example/proyecto
├── ProyectoApplication.java
├── config/CorsConfig.java
└── tarea/            # entidad, repositorio y controlador de ejemplo
```

`tarea/` es solo un CRUD de ejemplo: reemplázalo por tu dominio real.
