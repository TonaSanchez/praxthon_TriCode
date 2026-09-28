# Sandbox SPEI

API REST desarrollada con Spring Boot para simular operaciones de pago SPEI, con validaciones de negocio, control de idempotencia, historial de transiciones y consulta de pagos.

## ¿Qué hace este proyecto?

El sistema permite:

- Registrar operaciones de pago con datos del emisor y receptor.
- Validar reglas antes de aceptar una operación.
- Rechazar o gestionar solicitudes duplicadas con la misma clave de idempotencia.
- Consultar una operación por ID o listar todas paginadas.
- Cambiar el estado de una operación según su flujo válido.
- Mantener un historial de transiciones por operación.
- Exponer un catálogo básico de instituciones bancarias.

## Stack tecnológico

- Java 17
- Spring Boot 4.1.1
- Gradle
- Spring Web MVC
- Spring Data JPA
- MySQL
- Hibernate / JPA

## Requisitos

- Java 17+
- Gradle
- MySQL local o disponible

## Configuración

1. Clona el repositorio.
2. Crea la base de datos `sandbox_spei`.
3. Ajusta las credenciales en `src/main/resources/application.properties`.
4. Asegúrate de que el puerto y la conexión a MySQL estén correctos.

## Ejecución

Desde la raíz del proyecto:

```bash
gradlew bootRun
```

La aplicación queda disponible en:

```text
http://localhost:8080
```

## Endpoints principales

```text
POST   /api/v1/operaciones
GET    /api/v1/operaciones
GET    /api/v1/operaciones/{id}
PATCH  /api/v1/operaciones/{id}/estado
GET    /api/v1/catalogos/instituciones
```

## Estructura del proyecto

- `src/main/java/com/praxthon/sandbox_spei/controller` – controladores REST
- `src/main/java/com/praxthon/sandbox_spei/service` – lógica de negocio y estados
- `src/main/java/com/praxthon/sandbox_spei/entity` – entidades JPA
- `src/main/java/com/praxthon/sandbox_spei/repository` – acceso a datos
- `src/main/resources` – configuración, SQL y propiedades
- `build.gradle` – configuración de Gradle

## Nota

Este proyecto simula un flujo SPEI con validación de negocio y trazabilidad de cambios, pensado para pruebas y demostración de un motor de pagos.
