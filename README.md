# Prototipo PLN para Gestión de Incidencias Urbanas — Plataforma Web Administrativa

Aplicación web _full-stack_ (Angular + Spring Boot + MySQL) orientada a la administración, priorización y trazabilidad operativa de reportes ciudadanos sobre infraestructura urbana. Desarrollada como Prueba de Concepto (POC) para el Trabajo de Título **"Prototipo de sistema basado en PLN para la gestión de incidencias urbanas en ciudades inteligentes"** (Ingeniería en Informática, UTEM).

- **Repositorio hermano:** [Módulo de PLN y Microservicio de Inferencia](https://github.com/Acuario22/Proyecto_Trabajo_Titulo)

---

## Arquitectura y Stack Tecnológico

La plataforma se ejecuta de forma **estrictamente local** y se comunica vía HTTP/JSON con el microservicio de inferencia ML (`http://localhost:8000`):

- **Frontend (`dashboard-administrativo/`):** Angular `22.1.7` (_Standalone Components_, _Signals_, detección de cambios _zoneless_), TypeScript `~6.0`, RxJS `~7.8` y CSS estándar. Puerto `4200`.
- **Backend (`dashboard-administrativo-backend/`):** Java `21` (LTS), Spring Boot `3.4.0`, Spring Data JPA (Hibernate), Bean Validation, Lombok y Springdoc OpenAPI 3 (Swagger UI). Puerto `8080`.
- **Base de datos (`Base de datos - Scripts/`):** MySQL Server (`BD_REPORTES_URBANOS`), modelo relacional de 7 tablas.
- **Integración ML:** Cliente HTTP `RestClient` hacia `POST /api/v1/clasificar/{categoria,sentimiento,urgencia}`, con valores _fallback_ configurables en caso de indisponibilidad del microservicio.

---

## Alcance Funcional

El sistema implementa el **portal administrativo** y el ciclo de vida del reporte en **4 estados** (`NO INICIADO` → `INICIADO` → `EN REVISION` → `FINALIZADO`):

- **Panel administrativo (`/panel-administrativo`):** KPIs globales, filtros por categoría y estado, paginación, auto-recarga configurable y modo claro/oscuro.
- **Modal de auditoría ML:** Detalle del reporte y trazabilidad de la priorización (_Predicción ML → Diagnóstico de reglas → Urgencia final_).
- **Gestión de órdenes de trabajo (`/gestionar/:id`):** Asignación de cuadrilla, responsable, fecha/hora, materiales e instrucciones (transiciona a `INICIADO`).
- **Revisión técnica y cierre (`/revisar/:id`):** Verificación del trabajo en terreno y registro de cierre administrativo (transiciona a `FINALIZADO`).
- **Trazabilidad histórica (`/seguimiento/:id`):** Ficha técnica, línea de tiempo cronológica y ubicación en Google Maps (búsqueda por ID o código `RPT-AAAA-XXXX`).

> **Nota de alcance:** Las vistas del ciudadano y del responsable de cuadrilla corresponden únicamente a _wireframes_ conceptuales y están fuera del alcance implementado en este repositorio.

---

## Estructura del Repositorio

```text
Proyecto-Trabajo-Titulo-Web/
├── Base de datos - Scripts/
│   ├── data_base_script.sql          # Creación de BD_REPORTES_URBANOS, 7 tablas y catálogos base
│   ├── Poblar_base_script.sql        # Datos de prueba (reportes, orden de trabajo e historial)
│   └── Modelo de datos.png           # Diagrama entidad-relación (MER)
├── dashboard-administrativo/         # Frontend Angular 22 (puerto 4200)
│   └── src/app/
│       ├── core/services/            # Cliente HTTP, estado de filtros, notificaciones y tema
│       ├── pages/                    # Vistas: panel, gestionar, revisar y trazabilidad
│       └── shared/components/        # Header, modal de auditoría ML y notificaciones
└── dashboard-administrativo-backend/ # Backend Spring Boot 3.4 / Java 21 (puerto 8080)
    └── src/main/
        ├── java/com/gestionreportes/dashboard/
        │   ├── config/               # CORS, RestClient ML, Swagger y DataInitializer
        │   ├── controller/           # ReporteController (/api/reportes)
        │   ├── dto/                  # DTOs de entrada, salida e inferencia ML
        │   ├── model/                # 7 entidades JPA + enum NivelRiesgo
        │   ├── repository/           # Repositorios Spring Data JPA
        │   └── service/              # Lógica de negocio y cliente de inferencia ML
        └── resources/
            └── application.properties
```

---

## Requisitos Previos

- **Java JDK 21** (LTS) — Maven no es obligatorio si se usa el wrapper incluido (`mvnw` / `mvnw.cmd`).
- **Node.js** `22+` (probado con Node `24.16.0` y npm `12.0.2`).
- **MySQL Server** ejecutándose en el puerto `3306`.
- **Microservicio de inferencia ML** activo en `http://localhost:8000` (requerido para clasificación automática en `POST /api/reportes`).

---

## Instalación y Levantamiento Local

### 1. Crear y poblar la base de datos (MySQL)

Ejecuta los scripts ubicados en `Base de datos - Scripts/` en el siguiente orden (desde MySQL Workbench o CLI):

1. **`data_base_script.sql`**: Crea el esquema `BD_REPORTES_URBANOS`, las 7 tablas (`TBLCATEGORIA`, `TBLURGENCIA`, `TBLSENTIMIENTO`, `TBLESTADOREPORTE`, `TBLREPORTES`, `TBLORDENTRABAJO`, `TBLHISTORIAL`) y carga los catálogos iniciales con el flujo de 4 estados.
2. **`Poblar_base_script.sql`** _(opcional)_: Inserta reportes de prueba, una orden de trabajo y registros de historial para visualizar el panel con datos desde el inicio.

Luego configura tus credenciales locales en `dashboard-administrativo-backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://<TU_HOST>:<TU_PUERTO>/BD_REPORTES_URBANOS?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
spring.datasource.username=<TU_USUARIO_MYSQL>
spring.datasource.password=<TU_CONTRASENA_MYSQL>

# Microservicio de inferencia ML (FastAPI)
ml.service.base-url=http://localhost:8000

```

> **Diagrama del modelo de datos:** disponible en [`Base de datos - Scripts/Modelo de datos.png`](./Base%20de%20datos%20-%20Scripts/Modelo%20de%20datos.png).

### 2. Levantar el Backend (Puerto 8080)

Desde la carpeta `dashboard-administrativo-backend/`:

```powershell
# Windows (PowerShell / CMD)
.\mvnw.cmd spring-boot:run

```

```bash
# Linux / macOS
./mvnw spring-boot:run

```

- **API REST:** `http://localhost:8080/api/reportes`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`

### 3. Levantar el Frontend (Puerto 4200)

Desde la carpeta `dashboard-administrativo/`:

```bash
npm install
npm start

```

- **Panel Administrativo:** `http://localhost:4200/panel-administrativo`

---

## Endpoints Principales de la API REST

| Método | Ruta                                         | Descripción                                                          |
| ------ | -------------------------------------------- | -------------------------------------------------------------------- |
| `GET`  | `/api/reportes`                              | Lista reportes con filtros opcionales `categoria` y `estado`         |
| `GET`  | `/api/reportes/todos`                        | Retorna la lista completa de reportes sin filtros                    |
| `GET`  | `/api/reportes/estado/{idEstado}`            | Filtra reportes por ID de estado (`1` a `4`)                         |
| `GET`  | `/api/reportes/{id}`                         | Obtiene el detalle de un reporte por su ID                           |
| `GET`  | `/api/reportes/categorias`                   | Retorna categorías con conteo de reportes asociados                  |
| `GET`  | `/api/reportes/metricas`                     | Retorna KPIs globales del panel administrativo                       |
| `GET`  | `/api/reportes/{identificador}/trazabilidad` | Ficha técnica y línea de tiempo (acepta ID o código `RPT-AAAA-XXXX`) |
| `POST` | `/api/reportes`                              | Crea un reporte invocando al microservicio ML para clasificarlo      |
| `GET`  | `/api/reportes/{id}/orden-trabajo`           | Consulta la orden de trabajo asociada a un reporte                   |
| `POST` | `/api/reportes/{id}/orden-trabajo`           | Emite orden de trabajo y cambia el estado a `INICIADO`               |
| `POST` | `/api/reportes/{id}/finalizar`               | Registra cierre administrativo y pasa el reporte a `FINALIZADO`      |

---

## Autor

- **Jhon Santa Cruz** — Ingeniería en Informática, Universidad Tecnológica Metropolitana (UTEM).
