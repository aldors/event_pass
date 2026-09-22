## EVENT PASS
> API REST desarrollada con Spring Boot para la venta de boletos de diversos eventos.

---

## 🚀 Funcionalidades principales  

- Registro de usuarios
- Inicio de sesión con JWT
- Access Tokens y Refresh Tokens
- Roles: **USER** y **ADMIN**
- Validaciones de negocio
- Manejo de excepciones personalizadas
- Manejo global de excepciones
- Creación y publicación de eventos
- Creación de tipos de boleto
- Pagos (*simulados por el momento*)
- Reservación de boletos (*a partir de la fecha de la reserva cuenta con 15 minutos antes de que expire*)
- Generación de boleto digital y código QR en pdf

---

## 🛠️ Tecnologías y Herramientas utilizadas  

| Tecnología/Herramienta | Uso principal |
|------------|---------------|
| **Java 21** | Lenguaje base |
| **Spring Boot 4.1.1** | Framework principal |
| **Spring Security** | Autenticación y autorización |
| **Spring Data JPA** | Persistencia de datos |
| **MySQL** | Base de datos |
| **JWT (JJWT)** | Tokens de sesión |
| **Hibernate** | ORM |
| **Maven** | Gestión de dependencias |
| **Lombok** | Simplificación de código |
| **Postman** | Gestión de APIs |
| **ZXing** | Generación de códigos QR |
| **Openpdf** | Generación de PDFs |
| **Git** | Contorl de versiones |

---

## 🏗️ Arquitectura  

El proyecto sigue una **arquitectura en capas**:  

```plaintext
Controller
   ↓
Service
   ↓
Repository
   ↓
Base de Datos
```

Además utiliza:

- **DTOs** para entrada y salida de datos
- **Mappers** para conversiones
- **Exception Handlers** para errores centralizados

---

## Importante

Para poder escanear el código QR del boleto digital desde un dispositivo externo se debe agregar en el `application.properties` -> `app.base-url=http://TU_IP_LOCAL:TU_PUERTO` y adicionalmente deberás desactivar el Firewall para que funcione correctamente
