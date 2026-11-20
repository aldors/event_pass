# 🎟️ EVENT PASS

> API REST desarrollada con Spring Boot para la creación, publicación, reserva, venta y validación de boletos para eventos.

Event Pass cubre el ciclo completo del boleto: un `ADMIN` crea el evento en `BORRADOR`, define tipos de boleto y lo `PUBLICA`. Al llegar `fechaInicio` el evento pasa a `INICIADO` y al llegar `fechaFin` a `FINALIZADO`. Un `USER` reserva (15 minutos, ampliados a 30 al abrir el checkout de Stripe), paga en modo test y descarga su boleto en PDF con código QR mientras la compra esté pagada y el evento vigente. Un `ADMIN` o `STAFF` valida el acceso en puerta con el endpoint de `usar`, el panel “Validar boletos” o la página `verificar.html`. Las reglas se basan en el **estado efectivo** del evento (calculado en tiempo real a partir de las fechas), no solo en el valor guardado en la BD.

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
- Roles: **`USER`**, **`STAFF`** y **`ADMIN`**.

### 📅 Eventos (ADMIN + público)
- Crear evento en estado `BORRADOR`.
- Agregar tipos de boleto (precio, stock, máximo por usuario) solo en `BORRADOR`; el nombre del tipo es único por evento (sin distinguir mayúsculas).
- Publicar (`BORRADOR` → `PUBLICADO`) requiere al menos un tipo de boleto y `fechaInicio` futura. Cancelar (`→ CANCELADO`) solo es posible mientras el estado efectivo siga siendo `PUBLICADO`.
- Transición automática `PUBLICADO` → `INICIADO` → `FINALIZADO` según `fechaInicio` / `fechaFin` (estado efectivo + scheduler).
- Listar y ver detalle (versión admin: todos los estados; versión pública: `PUBLICADO` e `INICIADO` vigentes).
- Eliminar eventos en borrador.
- Validaciones: fecha fin posterior al inicio, fecha inicio futura al publicar y estado correcto para publicar, cancelar y reservar.

### 🛒 Reservas, pagos y compras (USER + ADMIN)
- Reservar boletos solo si el estado efectivo del evento es `PUBLICADO` (en `INICIADO` se rechaza con `EventoIniciadoException`) → compra en `RESERVADA`.
- **La reserva expira a los 15 minutos** si no se paga; al crear la sesión de checkout se extiende a **30 minutos** y se reutiliza la sesión abierta de Stripe si sigue vigente (ver Schedulers).
- Pago con **Stripe (test)** → compra `PAGADA` + registro de `Pago`. El webhook valida firma, importe, moneda (`app.stripe.currency`) y el `compraId` de la metadata; hay reconciliación de respaldo vía `/verificar-pago` y scheduler.
- Consultar `mis-compras` y detalle de compra (el detalle indica qué boletos son descargables).

### 🎫 Boletos digitales y validación en puerta
- Generación de boleto digital en **PDF con código QR** (OpenPDF + ZXing). Requiere compra `PAGADA` y evento vigente; solo puede descargarlo el dueño del boleto.
- Descarga del PDF: `GET /boletos/{id}/pdf` (endpoint para `USER`/`ADMIN`, con comprobación de propiedad en el servicio).
- Verificación pública: `GET /boletos/verificar/{codigoQr}` → devuelve `valido` solo si el boleto está `ACTIVO`, la compra `PAGADA` y el evento efectivo es `PUBLICADO` o `INICIADO`.
- Uso en puerta (`ADMIN` o `STAFF`): `POST /boletos/{codigoQr}/usar` → `ACTIVO` → `UTILIZADO`, respetando también el estado efectivo del evento.
- Panel “Validar boletos” para `ADMIN` y `STAFF`, con escáner QR y confirmación antes de marcar el uso.
- Página estática `verificar.html` + escáner QR para consultar boletos desde el navegador.
- Frontend con permisos centralizados por rol (`obtenerPermisos` en `auth.js`), panel de administración cerrable y badges de estado alineados (`BORRADOR`, `PUBLICADO`, `INICIADO`, `FINALIZADO`, `CANCELADO`).

### 🧩 Transversales
- DTOs de entrada/salida + Mappers (Evento, TipoBoleto, Login, Registro).
- Validación con Bean Validation (`@Valid`).
- ~49 excepciones de negocio + `GlobalExceptionHandler` con respuesta de errores centralizada (`ApiError`).
- Documentación OpenAPI/Swagger con seguridad Bearer JWT.
- CORS configurado para desarrollo front (`localhost:5500`, `127.0.0.1:5500`, `null`).

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
| **Stripe Java SDK** | 34.0.0 | Checkout, webhooks y reconciliación |
| **html5-qrcode** | 2.3.8 (CDN) | Escáner QR del frontend (`index.html`, `verificar.html`) |
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
controller/    → Auth, EventoAdmin, EventoPublic, Compra, Boleto, StripeWebhook
service/       → interfaces/ + implementaciones/ (Auth, EventoAdmin, EventoPublic, Compra, Boleto, Qr, Pdf, StripeWebhook)
repository/    → JpaRepository por entidad (8 repositorios)
entity/        → Usuario, Evento, TipoBoleto, Compra, DetalleCompra, Pago, Boleto, RefreshToken
dto/           → auth, evento, tipo_boleto, reservacion, pago, boleto
mapper/        → EventoMapper, TipoBoletoMapper, LoginMapper, RegistroMapper
security/      → SecurityConfig, JwtService, JwtAuthenticationFilter, handlers, CurrentUserService
support/       → EventoEstadoResolver (estado efectivo del evento)
scheduler/     → CompraScheduler, StripeReconciliacionScheduler, EventoScheduler
config/        → OpenApiConfig, StripeConfig
enums/         → Role, EstadoEvento, EstadoCompra, EstadoPago, EstadoBoleto
exception/     → excepciones de negocio + GlobalExceptionHandler + ApiError
```

El frontend es estático (sin framework ni bundler) y vive en `src/main/resources/static`: `index.html` (SPA), `verificar.html`, los módulos `js/` y los estilos `css/`. Lo sirve el propio Spring Boot en `http://localhost:8080/`; `config.js` define la URL base de la API.

---

## 🧾 Modelo de datos y reglas de negocio

```plaintext
Usuario (USER, ADMIN, STAFF)
  │ 1──N Compra (RESERVADA → PAGADA / CANCELADA / EXPIRADA)
  │        │ 1──N DetalleCompra ──N──1 TipoBoleto ──N──1 Evento (BORRADOR → PUBLICADO → INICIADO → FINALIZADO / CANCELADO)
  │        │ 1──N Pago (PENDIENTE → APROBADO / RECHAZADO / EXPIRADO, Stripe test)
  │        └──1──N Boleto (ACTIVO → UTILIZADO / INVALIDADO, con codigoQr único)
  └──1──N RefreshToken (revocable en logout)
```

> El estado del evento puede estar desactualizado en la BD entre ejecuciones del scheduler. Por eso `EventoEstadoResolver.estadoEfectivo()` deriva `PUBLICADO` → `INICIADO` al llegar `fechaInicio` y → `FINALIZADO` al llegar `fechaFin` (en `INICIADO` también); `BORRADOR`, `CANCELADO` y `FINALIZADO` se respetan tal cual. Catálogo, reserva, PDF, verificación y uso consultan ese estado efectivo; el scheduler solo lo persiste.

Reglas clave:

| Regla | Detalle |
|---|---|
| Estado efectivo | Se calcula con las fechas (`EventoEstadoResolver`) y manda sobre el valor persistido hasta que el scheduler lo actualiza |
| Reserva con TTL | `RESERVADA` expira a los **15 min** si no hay pago Stripe pendiente; al crear el checkout pasa a **30 min**. Sin pago → `EXPIRADA` y se libera stock |
| Solo publicados | Solo se reserva si el estado efectivo es `PUBLICADO`; en `INICIADO` se lanza `EventoIniciadoException`. El catálogo público incluye `PUBLICADO` e `INICIADO` vigentes |
| Stock y tope | Se valida disponibilidad por tipo de boleto y el máximo de boletos del evento por usuario (`maxBoletosPorUsuario`), con bloqueo pesimista al reservar |
| Inicio de evento | Al llegar `fechaInicio`, `PUBLICADO` → `INICIADO`; se pueden consultar y validar boletos, pero ya no reservar |
| Cierre de evento | Al pasar `fechaFin`, `PUBLICADO`/`INICIADO` → `FINALIZADO`; boletos `ACTIVO` de compras pagadas → `INVALIDADO`, reservas colgadas → `EXPIRADA`; los `UTILIZADO` no cambian |
| Cancelación | Solo con estado efectivo `PUBLICADO`: invalida boletos `ACTIVO` (de compras pagadas y reservadas) y cancela reservas; mantiene boletos `UTILIZADO` y el historial de compras pagadas |
| QR de un solo uso | `ACTIVO` → `UTILIZADO` por `ADMIN` o `STAFF` solo si la compra está `PAGADA` y el evento efectivo es `PUBLICADO`/`INICIADO`; verificar es público y no marca el uso |
| PDF propio | Solo el dueño del boleto y con compra `PAGADA` puede descargarlo; el evento no debe estar `BORRADOR`, `CANCELADO` ni `FINALIZADO`. Un `ADMIN` **no** puede descargar PDFs ajenos aunque su rol pase el filtro del endpoint |
| Pago Stripe | Se validan firma del webhook, sesión consultada a Stripe, importe en centavos, moneda configurada y `compraId`; eventos duplicados no vuelven a aprobar el pago. Los reembolsos (`charge.refunded`) se ignoran |

Estados:

- `Role`: `USER`, `ADMIN`, `STAFF`
- `EstadoEvento`: `BORRADOR`, `PUBLICADO`, `INICIADO`, `FINALIZADO`, `CANCELADO`
- `EstadoCompra`: `RESERVADA`, `PAGADA`, `CANCELADA`, `EXPIRADA`
- `EstadoPago`: `PENDIENTE`, `APROBADO`, `RECHAZADO`, `EXPIRADO`
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
| POST | `/auth/logout` | `USER`, `ADMIN`, `STAFF` | Revocar refresh token |
| GET | `/auth/me` | `USER`, `ADMIN`, `STAFF` | Perfil del usuario autenticado |

### Eventos públicos — `/eventos`

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| GET | `/eventos/obtener` | Público | Listar eventos vigentes en `PUBLICADO` o `INICIADO` |
| GET | `/eventos/{eventoId}/obtener` | Público | Detalle público + tipos disponibles (también en `INICIADO`) |

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
| POST | `/compras/reservar-boletos` | `USER`, `ADMIN` | Crear reserva (TTL 15 min, solo en eventos `PUBLICADO`) |
| POST | `/compras/{compraId}/pagar` | `USER`, `ADMIN` | Crear/reutilizar sesión de checkout en Stripe (expira en 30 min y extiende la reserva a 30 min) |
| POST | `/compras/{compraId}/verificar-pago` | `USER`, `ADMIN` | Reconciliar pago pendiente contra Stripe (respaldo si el webhook no llegó) |
| GET | `/compras/mis-compras` | `USER`, `ADMIN` | Historial propio |
| GET | `/compras/{compraId}` | `USER`, `ADMIN` | Detalle de compra propia |

### Boletos — `/boletos`

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| GET | `/boletos/verificar/{codigoQr}` | Público | Ver datos y flag `valido` del boleto sin marcarlo |
| POST | `/boletos/{codigoQr}/usar` | `ADMIN`, `STAFF` | Marcar boleto como `UTILIZADO` (compra pagada y evento vigente) |
| GET | `/boletos/{id}/pdf` | `USER`, `ADMIN`* | Descargar boleto en PDF con QR (compra pagada y evento vigente) |

\* El rol `ADMIN` solo autoriza el endpoint a nivel de seguridad; el servicio exige que el boleto pertenezca al usuario autenticado. Lo mismo aplica a `GET /compras/{compraId}`: aunque la tabla indica `USER`/`ADMIN`, el servicio solo devuelve compras del usuario autenticado.

`POST /webhooks/stripe` es público (no aparece en Swagger por estar oculto) pero exige la cabecera `Stripe-Signature` y un `stripe.webhook-secret` configurado; sin firma responde `400`.

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

# Delays de los schedulers (milisegundos; valores por defecto)
app.schedulers.compra-expiracion-delay-ms=600000
app.schedulers.evento-finalizacion-delay-ms=1800000
app.schedulers.stripe-reconciliation-delay-ms=300000
```

> Genera un secreto largo y aleatorio para `jwt.secret` (mínimo 32 caracteres/256 bits). No lo subas al repo.

### 2b. Configurar Stripe (modo test)

```properties
stripe.secret-key=sk_test_TU_CLAVE
stripe.webhook-secret=whsec_TU_SECRETO

# URL base a la que Stripe redirige tras el checkout (se le agrega /index.html?payment=...)
app.frontend-url=http://127.0.0.1:5500/event_pass/src/main/resources/static

# Código ISO de la moneda de los cargos (minúsculas)
app.stripe.currency=mxn
```

En local, reenvía los webhooks con Stripe CLI:

```bash
stripe listen --forward-to localhost:8080/webhooks/stripe
```

> Limitación actual: se procesan `checkout.session.completed/expired/async_payment_succeeded/async_payment_failed`. Los reembolsos (`charge.refunded`) se ignoran porque no se guarda el `payment_intent`.

### 3. Ejecutar

```bash
./mvnw spring-boot:run
# Windows:
mvnw.cmd spring-boot:run
```

La API queda en `http://localhost:8080` y el frontend integrado en `http://localhost:8080/` (y `http://localhost:8080/verificar.html`). Si se usa ese frontend en lugar de Live Server, ajusta `app.frontend-url=http://localhost:8080` (o la IP local correspondiente) para que Stripe redirija correctamente.

### 4. Crear un ADMIN o STAFF

El registro crea usuarios `USER`. Para operar eventos, promueve uno a `ADMIN`; para validar boletos en puerta, promueve uno a `STAFF`:

```sql
UPDATE usuarios SET rol = 'ADMIN' WHERE email = 'tu@email.com';
UPDATE usuarios SET rol = 'STAFF' WHERE email = 'staff@email.com';
```

Matriz rápida de acceso:

| Rol | Administrar eventos | Reservar/comprar y ver compras | Validar/usar boletos |
|---|---|---|---|
| `USER` | No | Sí | No |
| `ADMIN` | Sí | Sí | Sí |
| `STAFF` | No | No | Sí |

---

## 📖 Uso y documentación Swagger

Con la app corriendo:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Frontend / SPA: `http://localhost:8080/`
- Verificador QR (frontend estático): `http://localhost:8080/verificar.html`

El controlador del webhook de Stripe está marcado como oculto, por lo que `/webhooks/stripe` no figura en Swagger.

En Swagger, pulsa **Authorize** e ingresa `Bearer TU_ACCESS_TOKEN`.

Flujo típico end-to-end:

1. `POST /auth/registro` → `POST /auth/login` (USER).
2. Como ADMIN: `POST /admin/eventos/crear` → `agregar-tipo-boleto` → `publicar`.
3. Como USER: `GET /eventos/obtener` → `GET /eventos/{id}/obtener` → `POST /compras/reservar-boletos` (evento efectivamente `PUBLICADO`) → `POST /compras/{id}/pagar` (antes de 15 min; la sesión dura 30 min).
4. Tras el retorno de Stripe (`?payment=success|cancelled`), el frontend procesa el resultado y, si hace falta, `POST /compras/{id}/verificar-pago` reconcilia el pago.
5. `GET /compras/{id}` para obtener el `boletoId`, luego `GET /boletos/{id}/pdf` para descargar el boleto (compra pagada y evento vigente).
6. En puerta (`ADMIN` o `STAFF`): `GET /boletos/verificar/{codigoQr}` y `POST /boletos/{codigoQr}/usar`, o usa el panel “Validar boletos” / `verificar.html` con la cámara. Esto sigue funcionando mientras el evento esté `INICIADO` (hasta `fechaFin`).

---

## 🔒 Seguridad

- Stateless, sin sesiones ni CSRF para la API (`SessionCreationPolicy.STATELESS`).
- Contraseñas con `BCryptPasswordEncoder`.
- Filtro `JwtAuthenticationFilter` + `entry point` y `access denied handler` personalizados con respuestas JSON.
- Rutas públicas: registro, login, refresh, eventos públicos, verificación de QR, Swagger, estáticos y webhook de Stripe (este último valida la firma). Resto por rol:
  - `/admin/eventos/**` → solo `ADMIN`.
  - `/compras/**` → `USER` y `ADMIN`.
  - `/boletos/*/usar` → `ADMIN` y `STAFF`; `/boletos/*/pdf` → `USER` y `ADMIN` (más comprobación de propiedad).
- CORS permitido para `http://localhost:5500`, `http://127.0.0.1:5500` y `null` (origen `file://`), con credenciales y exponiendo `Authorization`. Si el front se sirve desde otro origen, agrégalo en `SecurityConfig.corsConfigurationSource()`.

---

## ⏱️ Tareas programadas (Schedulers)

| Scheduler | Frecuencia (defecto) | Acción |
|---|---|---|
| `CompraScheduler` | cada 10 min (`600000` ms) | Expira reservas `RESERVADA` con TTL vencido y sin pago Stripe `PENDIENTE` → `EXPIRADA` |
| `StripeReconciliacionScheduler` | cada 5 min (`300000` ms) | Para pagos `PENDIENTE` con reserva vencida, consulta la sesión en Stripe: si sigue `open` la cierra y luego aprueba o expira según su estado |
| `EventoScheduler` | cada 30 min (`1800000` ms) | Pasa a `INICIADO` los eventos `PUBLICADO` cuya `fechaInicio` ya llegó (y `fechaFin` no); finaliza los `PUBLICADO`/`INICIADO` con `fechaFin` pasada → `FINALIZADO`, invalida boletos `ACTIVO` pagados y expira reservas colgadas |

Configurables con `app.schedulers.compra-expiracion-delay-ms`, `app.schedulers.stripe-reconciliation-delay-ms` y `app.schedulers.evento-finalizacion-delay-ms` (este último controla también el paso a `INICIADO`). Las transiciones de estado se reflejan antes en el estado efectivo calculado por `EventoEstadoResolver`, incluso entre ejecuciones del scheduler.

---

## ⚠️ Importante — QR en red local

El QR del PDF apunta a `app.base-url`. Para escanearlo desde un celular en la misma red:

1. En `application.properties`: `app.base-url=http://TU_IP_LOCAL:8080` (ej. `http://192.168.1.10:8080`, no `localhost`).
2. Permite el puerto en el Firewall de Windows (o desactívalo temporalmente para pruebas).
3. En `config.js`: `API_BASE_URL = "http://TU_IP_LOCAL:8080"` (viene con `http://localhost:8080` por defecto; desde el celular, `localhost` apunta al propio teléfono).
4. Si el front se sirve desde otro origen (p. ej. Live Server en `:5500`), agrégalo en `SecurityConfig.corsConfigurationSource()`. Si se sirve desde `http://TU_IP_LOCAL:8080`, es mismo origen y no requiere CORS. Ajusta también `app.frontend-url` para los redirects de Stripe.
5. Regenera/descarga el PDF después del cambio (el QR queda grabado con la URL antigua).

---

## 🗺️ Estado y siguientes pasos

- [x] Auth JWT con refresh y roles (`USER`, `ADMIN`, `STAFF`)
- [x] CRUD y publicación de eventos, con estados `PUBLICADO` → `INICIADO` → `FINALIZADO` y estado efectivo calculado en tiempo real
- [x] Permisos por rol centralizados en el frontend y panel “Validar boletos” con escáner para `ADMIN`/`STAFF`
- [x] Reserva con expiración + schedulers (incluida reconciliación de Stripe)
- [x] Boleto PDF + QR + verificación/uso con comprobación de estado efectivo
- [x] Pago con Stripe (modo test) + webhook con firma y validaciones + reconciliación
- [ ] Paginación y filtros de eventos
- [ ] Tests de integración

---

👤 **Autor:** Aldo — `aldorgithub@gmail.com`
