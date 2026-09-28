# Sandbox SPEI

API REST con Spring Boot para simular pagos SPEI. Incluye validaciones de negocio, idempotencia, estados de operación, historial de transiciones y un catálogo ficticio de instituciones.

## ¿Qué hace este proyecto?

El sistema permite:

- Registrar operaciones de pago con datos del emisor y receptor.
- Validar reglas antes de aceptar una operación.
- Rechazar o gestionar solicitudes duplicadas con la misma clave de idempotencia.
- Consultar una operación por ID o listar todas paginadas.
- Cambiar el estado de una operación según su flujo válido.
- Mantener un historial de transiciones por operación.
- Exponer un catálogo básico de instituciones bancarias.

## Arranque 

Estas instrucciones son para Windows 10/11 con conexión a Internet. No necesitas instalar Gradle ni MySQL: el proyecto incluye Gradle Wrapper y usa H2 en memoria de forma predeterminada. La primera ejecución descarga Gradle y dependencias, por lo que el tiempo depende de la conexión.

### 1. Instala Java y Git

Abre PowerShell y ejecuta:

```powershell
winget install --id EclipseAdoptium.Temurin.17.JDK -e
winget install --id Git.Git -e
```

Cierra y vuelve a abrir PowerShell; verifica que ambos comandos estén disponibles:

```powershell
java -version
git --version
```

Se requiere Java 17 o superior.

### 2. Descarga el proyecto

```powershell
git clone https://github.com/TonaSanchez/praxthon.git
cd praxthon
```

### 3. Inicia el sandbox

```powershell
.\gradlew.bat bootRun
```

Espera el mensaje `Started SandboxSpeiApplication`. La API estará disponible en `http://localhost:8080`. Al iniciar se crean automáticamente cuatro operaciones de ejemplo.

En macOS o Linux, usa `./gradlew bootRun` en lugar de `gradlew.bat`.

### 4. Comprueba que responde

Abre otra ventana de PowerShell y consulta el catálogo:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/catalogos/instituciones
```

Debe responder con cinco instituciones ficticias (códigos `801` a `805`). También puedes consultar las operaciones precargadas:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/operaciones
```

Para detener la aplicación, vuelve a la terminal donde está ejecutándose y pulsa `Ctrl+C`.

## Configuración de datos

El perfil predeterminado está en `src/main/resources/application.properties`: usa una base H2 en memoria, crea el esquema al iniciar y carga datos de ejemplo. Los datos se reinician al detener la aplicación. No edites credenciales ni instales una base de datos para el arranque rápido.

Hay una configuración opcional para MySQL en `src/main/resources/application-mysql.properties`. Para usarla se necesita MySQL con la base `sandbox_spei` y el esquema creado; configura la contraseña mediante la variable `DB_PASSWORD` y activa el perfil `mysql` al iniciar. Este paso no es necesario para probar el sandbox.

## API

La especificación detallada está en [`src/main/resources/openapi.yaml`](src/main/resources/openapi.yaml). Endpoints principales:

| Método | Ruta | Descripción |

| `POST`  `/api/v1/operaciones`  Registrar un pago 
| `GET`  `/api/v1/operaciones`  Listar operaciones paginadas 
| `GET`  `/api/v1/operaciones/{id}`  Consultar operación e historial 
| `PATCH`  `/api/v1/operaciones/{id}/estado`  Solicitar cambio de estado 
| `GET`  `/api/v1/catalogos/instituciones`  Consultar instituciones ficticias 

La creación acepta los encabezados opcionales `Clave-Idempotencia` y `X-Escenario-Forzado`. Las instrucciones válidas responden `201`; las inválidas responden `422`. Reutilizar una clave de idempotencia con el mismo cuerpo devuelve la operación original; usarla con un cuerpo distinto responde `409`.

## Pruebas

Desde la raíz del repositorio:

```powershell
.\gradlew.bat test
```

## Tecnologías y estructura

- Java 17+, Spring Boot, Spring Web MVC, Spring Data JPA y Hibernate.
- Gradle Wrapper para compilar, probar y ejecutar sin instalar Gradle globalmente.
- `src/main/java/com/praxthon/sandbox_spei/controller`: endpoints REST.
- `src/main/java/com/praxthon/sandbox_spei/service`: reglas de negocio y flujo de estados.
- `src/main/java/com/praxthon/sandbox_spei/entity` y `repository`: persistencia.
- `src/main/resources`: configuración, contrato OpenAPI y scripts SQL.
