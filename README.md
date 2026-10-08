# 🎟️ EVENT PASS

> API REST desarrollada con Spring Boot para la creación, publicación, reserva, venta y validación de boletos para eventos.

Event Pass cubre el ciclo completo del boleto: un `ADMIN` crea el evento en `BORRADOR`, define tipos de boleto y lo `PUBLICA`. Un `USER` reserva (con expiración de 15 minutos), paga (simulado), descarga su boleto en PDF con código QR y el `ADMIN` lo valida en puerta con el endpoint de `usar` o la página `verificar.html`.

**Repo:** `git@github.com:aldors/event_pass.git`

---

## 📑 Tabla de contenidos

- [Funcionalidades](#-funcionalidades-principales)
- [Tecnologías](#️-tecnologías-y-herramientas)
- [Arquitectura](#️-arquitectura)
- [Modelo de datos y reglas de negocio](#-modelo-de-datos-y-reglas-de-negocio)
- [Endpoints](#-endpoints-de-la-api)
- [Instalación y configuración](#-instalación-y-configuración)
- [Uso y documentación Swagger](#-uso-y-documentación-swagger)
- [Seguridad](#-seguridad)
- [Tareas programadas](#-tareas-programadas-schedulers)
- [Nota sobre el QR externo](#-importante--qr-en-red-local)

---

## 🚀 Funcionalidades principales

### 🔐 Autenticación y usuarios
- Registro de usuarios (`USER` por defecto) con BCrypt.
- Login con JWT: **Access Token (30 min)** + **Refresh Token (60 min)**.
- Refresh, logout (revocación del refresh token) y perfil `/auth/me`.
- Roles: **`USER`** y **`ADMIN`**.

### 📅 Eventos (ADMIN + público)
- Crear evento en estado `BORRADOR`.
- Agregar tipos de boleto (precio, stock, máximo por usuario).
- Publicar (`BORRADOR` → `PUBLICADO`) y cancelar (`→ CANCELADO`).
- Listar y ver detalle (versión admin y versión pública).
- Eliminar eventos en borrador.
- Validaciones: fecha inicio futura, fecha fin posterior a inicio, solo se puede publicar/reservar en estado correcto.

### 🛒 Reservas, pagos y compras (USER)
- Reservar boletos solo en eventos `PUBLICADOS` → compra en `RESERVADA`.
- **La reserva expira a los 15 minutos** si no se paga (ver Schedulers).
- Pago **simulado** → compra `PAGADA` + boletos `ACTIVO` + registro de `Pago`.
- Consultar `mis-compras` y detalle de compra.

### 🎫 Boletos digitales
- Generación de boleto digital en **PDF con código QR** (OpenPDF + ZXing).
- Descarga del PDF: `GET /boletos/{id}/pdf` (dueño o ADMIN).
- Verificación pública: `GET /boletos/verificar/{codigoQr}`.
- Uso en puerta (solo `ADMIN`): `POST /boletos/{codigoQr}/usar` → `ACTIVO` → `UTILIZADO`.
- Página estática `verificar.html` + escáner QR para validar/usar boletos desde el navegador.

### 🧩 Transversales
- DTOs de entrada/salida + Mappers (Evento, TipoBoleto, Login, Registro).
- Validación con Bean Validation (`@Valid`).
- ~20 excepciones de negocio + `GlobalExceptionHandler` con respuesta de errores centralizada.
- Documentación OpenAPI/Swagger con seguridad Bearer JWT.
- CORS configurado para desarrollo front (`localhost:5500`, `127.0.0.1:5500`).

---

## 🛠️ Tecnologías y herramientas

| Tecnología / Herramienta | Versión | Uso principal |
|---|---|---|
| **Java** | 21 | Lenguaje base |
| **Spring Boot** | 4.1.1 | Framework principal |
| **Spring Web MVC** | — | Controllers REST |
| **Spring Security** | — | Autenticación y autorización JWT |
| **Spring Data JPA + Hibernate** | — | Persistencia / ORM |
| **Bean Validation** | — | Validación de DTOs |
| **MySQL** | — | Base de datos (`ddl-auto=update`) |
| **JWT (JJWT)** | 0.12.6 | Access + Refresh Tokens |
| **springdoc-openapi** | 3.0.3 | Swagger UI / OpenAPI |
| **ZXing** | 3.5.4 | Generación y lectura de QR |
| **OpenPDF (LibrePDF)** | 3.0.5 | Generación del boleto en PDF |
| **Lombok** | — | Reducción de boilerplate |
| **Maven** | — | Gestión de dependencias |
| **Postman / Swagger UI** | — | Prueba de la API |
| **Git** | — | Control de versiones |

---

## 🏗️ Arquitectura

Arquitectura **en capas**:

```plaintext
Controller
    ↓ (DTOs + @Valid)
Service (interfaces + implementaciones)
    ↓
Repository (Spring Data JPA)
    ↓
MySQL
```

Capas y paquetes (`com.aldo.event_pass`):

```plaintext
controller/    → Auth, EventoAdmin, EventoPublic, Compra, Boleto
service/       → interfaces/ + implementaciones/ (Auth, Evento, Compra, Boleto, Qr, Pdf)
repository/    → JpaRepository por entidad
entity/        → Usuario, Evento, TipoBoleto, Compra, DetalleCompra, Pago, Boleto, RefreshToken
dto/           → auth, evento, tipo_boleto, reservacion, pago, boleto
mapper/        → EventoMapper, TipoBoletoMapper, LoginMapper, RegistroMapper
security/      → SecurityConfig, JwtService, JwtAuthenticationFilter, handlers, CurrentUserService
exception/     → excepciones de negocio + GlobalExceptionHandler
scheduler/     → CompraScheduler, EventoScheduler
config/        → OpenApiConfig
enums/         → Role, EstadoEvento, EstadoCompra, EstadoPago, EstadoBoleto
```

---

## 🧾 Modelo de datos y reglas de negocio

```plaintext
Usuario (USER, ADMIN)
  │ 1──N Compra (RESERVADA → PAGADA / CANCELADA / EXPIRADA)
  │        │ 1──N DetalleCompra ──N──1 TipoBoleto ──N──1 Evento (BORRADOR → PUBLICADO → FINALIZADO / CANCELADO)
  │        │ 1──1 Pago (PENDIENTE → APROBADO / RECHAZADO, simulado)
  │        └──1──N Boleto (ACTIVO → UTILIZADO / INVALIDADO, con codigoQr único)
  └──1──N RefreshToken (revocable en logout)
```

Reglas clave:

| Regla | Detalle |
|---|---|
| Reserva con TTL | `RESERVADA` expira a los **15 min** sin pago → `EXPIRADA` y libera stock |
| Solo publicados | Solo se puede reservar en eventos `PUBLICADO` |
| Stock y tope | Se valida disponibilidad y máximo de boletos por usuario y tipo |
| Cierre de evento | Al pasar `fechaFin`, el evento pasa a `FINALIZADO`; boletos `ACTIVO` pagados → `INVALIDADO`, reservas colgadas → `EXPIRADA` |
| QR de un solo uso | `ACTIVO` → `UTILIZADO` solo por `ADMIN`; verificar es público |
| PDF propio | Solo el dueño de la compra puede descargar el PDF |

Estados:

- `EstadoEvento`: `BORRADOR`, `PUBLICADO`, `FINALIZADO`, `CANCELADO`
- `EstadoCompra`: `RESERVADA`, `PAGADA`, `CANCELADA`, `EXPIRADA`
- `EstadoPago`: `PENDIENTE`, `APROBADO`, `RECHAZADO`
- `EstadoBoleto`: `ACTIVO`, `UTILIZADO`, `INVALIDADO`

---

## 🔌 Endpoints de la API

Base URL local: `http://localhost:8080`

### Auth — `/auth`

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| POST | `/auth/registro` | Público | Registrar usuario |
| POST | `/auth/login` | Público | Login, devuelve access + refresh token |
| POST | `/auth/refresh-token` | Público (con refresh válido) | Renovar access token |
| POST | `/auth/logout` | `USER`, `ADMIN` | Revocar refresh token |
| GET | `/auth/me` | `USER`, `ADMIN` | Perfil del usuario autenticado |

### Eventos públicos — `/eventos`

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| GET | `/eventos/obtener` | Público | Listar eventos publicados |
| GET | `/eventos/{eventoId}/obtener` | Público | Detalle público + tipos disponibles |

### Eventos admin — `/admin/eventos`

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| POST | `/admin/eventos/crear` | `ADMIN` | Crear evento en `BORRADOR` |
| POST | `/admin/eventos/{eventoId}/agregar-tipo-boleto` | `ADMIN` | Agregar tipo de boleto |
| POST | `/admin/eventos/{eventoId}/publicar` | `ADMIN` | Publicar evento |
| POST | `/admin/eventos/{eventoId}/cancelar` | `ADMIN` | Cancelar evento |
| DELETE | `/admin/eventos/{eventoId}` | `ADMIN` | Eliminar evento |
| GET | `/admin/eventos/obtener` | `ADMIN` | Listar eventos (todos los estados) |
| GET | `/admin/eventos/{eventoId}/obtener` | `ADMIN` | Detalle admin |

### Compras — `/compras`

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| POST | `/compras/reservar-boletos` | `USER`, `ADMIN` | Crear reserva (TTL 15 min) |
| POST | `/compras/{compraId}/pagar` | `USER`, `ADMIN` | Pagar reserva (simulado) |
| GET | `/compras/mis-compras` | `USER`, `ADMIN` | Historial propio |
| GET | `/compras/{compraId}` | `USER`, `ADMIN` | Detalle de compra propia |

### Boletos — `/boletos`

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| GET | `/boletos/verificar/{codigoQr}` | Público | Ver datos del boleto sin marcarlo |
| POST | `/boletos/{codigoQr}/usar` | `ADMIN` | Marcar boleto como `UTILIZADO` |
| GET | `/boletos/{id}/pdf` | `USER`, `ADMIN` | Descargar boleto en PDF con QR |

Ejemplo de login:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@mail.com","password":"123456"}'
```

Luego usa el token:

```bash
curl http://localhost:8080/compras/mis-compras \
  -H "Authorization: Bearer TU_ACCESS_TOKEN"
```

---

## ⚙️ Instalación y configuración

### Prerrequisitos

- **Java 21+**
- **Maven 3.9+** (o usa `./mvnw` / `mvnw.cmd` incluido)
- **MySQL 8+** corriendo en local

### 1. Clonar y crear la base de datos

```bash
git clone git@github.com:aldors/event_pass.git
cd event_pass
```

```sql
CREATE DATABASE event_pass_db CHARACTER SET utf8mb4;
```

### 2. Configurar propiedades

Copia la plantilla y edítala:

```bash
cp src/main/resources/application-example.properties src/main/resources/application.properties
```

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/event_pass_db
spring.datasource.username=TU_USER
spring.datasource.password=TU_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

jwt.secret=TU_JWT_SECRET_DE_AL_MENOS_256_BITS
jwt.expirationAccessToken=1800000
jwt.expirationRefreshToken=3600000

# Necesario para que el QR apunte a una URL accesible en red local
app.base-url=http://TU_IP_LOCAL:8080
```

> Genera un secreto largo y aleatorio para `jwt.secret` (mínimo 32 caracteres/256 bits). No lo subas al repo.

### 3. Ejecutar

```bash
./mvnw spring-boot:run
# Windows:
mvnw.cmd spring-boot:run
```

La API queda en `http://localhost:8080`.

### 4. Crear un ADMIN

El registro crea usuarios `USER`. Para operar eventos, promueve uno a `ADMIN` directamente en MySQL:

```sql
UPDATE usuario SET rol = 'ADMIN' WHERE email = 'tu@email.com';
```

---

## 📖 Uso y documentación Swagger

Con la app corriendo:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Verificador QR (frontend estático): `http://localhost:8080/verificar.html`

En Swagger, pulsa **Authorize** e ingresa `Bearer TU_ACCESS_TOKEN`.

Flujo típico end-to-end:

1. `POST /auth/registro` → `POST /auth/login` (USER).
2. Como ADMIN: `POST /admin/eventos/crear` → `agregar-tipo-boleto` → `publicar`.
3. Como USER: `GET /eventos/obtener` → `POST /compras/reservar-boletos` → `POST /compras/{id}/pagar` (antes de 15 min).
4. `GET /boletos/{id}/pdf` para descargar el boleto.
5. En puerta (ADMIN): `GET /boletos/verificar/{codigoQr}` y `POST /boletos/{codigoQr}/usar`, o usa `verificar.html` con la cámara.

---

## 🔒 Seguridad

- Stateless, sin sesiones ni CSRF para la API (`SessionCreationPolicy.STATELESS`).
- Contraseñas con `BCryptPasswordEncoder`.
- Filtro `JwtAuthenticationFilter` + `entry point` y `access denied handler` personalizados con respuestas JSON.
- Rutas públicas: registro, login, refresh, eventos públicos, verificación de QR, Swagger y estáticos. Resto por rol.
- CORS limitado a `http://localhost:5500`, `http://127.0.0.1:5500` (ajusta en `SecurityConfig` para tu front).

---

## ⏱️ Tareas programadas (Schedulers)

| Scheduler | Frecuencia (defecto) | Acción |
|---|---|---|
| `CompraScheduler` | cada 10 min (`600000` ms) | Expira reservas `RESERVADA` con más de 15 min → `EXPIRADA` |
| `EventoScheduler` | cada 30 min (`1800000` ms) | Finaliza eventos `PUBLICADO` con `fechaFin` pasada → `FINALIZADO`, invalida boletos no usados y expira sus reservas |

Configurables con `app.schedulers.compra-expiracion-delay-ms` y `app.schedulers.evento-finalizacion-delay-ms`.

---

## ⚠️ Importante — QR en red local

El QR del PDF apunta a `app.base-url`. Para escanearlo desde un celular en la misma red:

1. En `application.properties`: `app.base-url=http://TU_IP_LOCAL:8080` (ej. `http://192.168.1.10:8080`, no `localhost`).
2. Permite el puerto en el Firewall de Windows (o desactívalo temporalmente para pruebas).
3. En `config.js`: `API_BASE_URL = "http://TU_IP_LOCAL:8080"`.
4. En `SecurityConfig` -> bean: `corsConfigurationSource()` -> `configuration.setAllowedOrigins(List.of())`: `http://TU_IP_LOCAL:8080`
5. Regenera/descarga el PDF después del cambio.

---

## 🗺️ Estado y siguientes pasos

- [x] Auth JWT con refresh y roles
- [x] CRUD y publicación de eventos
- [x] Reserva con expiración + schedulers
- [x] Boleto PDF + QR + verificación/uso
- [ ] Pago real (actualmente simulado)
- [ ] Paginación y filtros de eventos
- [ ] Tests de integración
- [ ] Despliegue con Docker

---

👤 **Autor:** Aldo — `aldorgithub@gmail.com`
