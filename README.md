# UCA-CFC Connect

API REST para administrar los procesos académicos y administrativos del Centro de Formación Continua de la UCA. El primer módulo funcional implementado es la gestión de cursos.

## Tecnologías y requisitos

- Java 21
- Spring Boot 4.1
- Spring MVC, Spring Data JPA y Hibernate
- MySQL 8
- Maven Wrapper incluido
- Springdoc OpenAPI/Swagger
- JUnit 5 y Mockito

## Configuración de la base de datos

### Actualización de clientes y carnet

Antes de ejecutar esta versión sobre una base existente, respalda la base y ejecuta una sola vez
`database/normalizar_clientes_carnet.sql`, seguido de `database/participantes_automaticos.sql`.
Si ya aplicaste la normalización, ejecuta únicamente `participantes_automaticos.sql`:
agrega los participantes que falten sin duplicar ni modificar los existentes.
La aplicación conserva `ddl-auto=validate`.
La migración conserva los nombres anteriores en `nombre_original`: recepción debe confirmar
primer nombre, segundo nombre (opcional), primer apellido y segundo apellido en **Clientes → Completar datos**.
No se deducen apellidos de nombres incompletos ni se cambian identificadores o inscripciones existentes.

Al guardar una ficha completa se genera automáticamente el carnet: iniciales de ambos apellidos,
año de registro en El Salvador y doce caracteres hexadecimales aleatorios (por ejemplo,
`PD2026A7C93F82D104`). Las iniciales se normalizan sin tildes. Un índice único impide repetirlo
y un trigger impide cambiar o borrar un carnet asignado, incluso mediante SQL.
Corregir los nombres no cambia el carnet. Las cuentas que todavía no tienen sus apellidos
confirmados muestran el carnet pendiente hasta que recepción complete la ficha.

El alta de cliente y participante es una sola operación: tanto el autorregistro en **Crear cuenta**
como el registro desde recepción crean automáticamente un participante y un carnet.
Los dos formularios solicitan primer nombre, segundo nombre (opcional), primer apellido y segundo apellido.
El formulario de recepción no incluye un campo de carnet: el sistema lo asigna al guardar.
Aceptar una invitación y actualizar una ficha reutilizan el participante existente; nunca lo duplican.
La restricción única por cliente y el bloqueo de la ficha protegen también las solicitudes concurrentes.
**Participantes** permite consultar los registros e **Inscripciones** busca por carnet antes de guardar.
No se exige una cuenta de acceso para inscribir desde recepción; una cuenta vinculada inactiva debe reactivarse.
El carnet aparece en la cabecera del portal y en Configuración de cuenta, siempre de solo lectura.
Las fichas históricas sin apellidos conservan sus datos y su carnet pendiente hasta confirmar su identidad.
La edición de nombre de Configuración usa también los cuatro campos y sincroniza cliente,
usuario y participante sin regenerar el carnet. `PATCH /api/v1/auth/me/nombre` recibe
`primerNombre`, `segundoNombre` (opcional), `primerApellido` y `segundoApellido`.

Si el servidor local es MariaDB de XAMPP, agrega en el `.env` local
`spring.jpa.database-platform=org.hibernate.dialect.MariaDBDialect` para que los bloqueos
de cupos e inscripciones usen SQL compatible. Conserva el dialecto predeterminado al usar MySQL 8.

Rutas de recepción (requieren rol RECEPCIONISTA):

- `POST /api/v1/recepcion/clientes`: los cuatro campos de nombre, con segundo nombre opcional.
- `PUT /api/v1/recepcion/clientes/{id}`: completar o corregir la ficha sin cambiar el carnet.
- `GET /api/v1/recepcion/clientes/carnet/{carnet}`: cliente y estado de participante.
- `POST /api/v1/recepcion/participantes`: `{ "carnet": "..." }` (compatibilidad; reutiliza el registro si ya existe).
- `POST /api/v1/recepcion/inscripciones`: `{ "carnet": "...", "programaTipo": "CURSO", "programaId": 1 }`
  (también admite `DIPLOMADO`).

La aplicación usa por defecto la base local `uca_cfc_connect`, el usuario `root` y una contraseña vacía. Se puede cambiar la configuración sin modificar el código mediante estas variables de entorno:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/uca_cfc_connect'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='tu_contrasena'
```

Las tablas `categorias` y `modalidades` deben contener registros antes de crear un curso, porque el API valida que ambas llaves foráneas existan. El archivo `database/datos_catalogo_cursos.sql` incluye datos iniciales idempotentes para ambos catálogos; puede ejecutarse desde MySQL Workbench. Los identificadores generados se consultan luego mediante los endpoints de catálogo.

### Cobros de recepción y búsqueda de carnet

Al escribir al menos dos caracteres del carnet en Participantes o Inscripciones se sugieren
coincidencias con el nombre del cliente. Al seleccionar un carnet completo se consulta automáticamente
su estado; el botón Buscar sigue disponible para carnets que no estén en las sugerencias.

En Pagos, las opciones de inscripción se limitan al cliente elegido y a inscripciones pendientes
sin pagos pendientes ni confirmados. El monto se completa con el total acordado de la inscripción
(curso o diplomado), incluso si el precio del programa cambió después, y se muestra de solo lectura.
El servidor valida el importe, el propietario y el estado bajo bloqueo de la inscripción para evitar
duplicar cobros, incluidos pagos registrados desde el portal del estudiante.
La lista de recepción muestra pagos pendientes de confirmar. Confirmarlos los retira de esa lista
y conserva su historial en la base de datos y en las consultas administrativas.
En la misma sección, **Historial de pagos** muestra todos los estados con filtros por fecha desde/hasta
(ambos días incluidos), estado, método, cliente y búsqueda por nombre, carnet o referencia.
El historial es de solo consulta, tiene paginación de 10, 25 o 50 registros y se actualiza después
de registrar o confirmar un pago sin perder los filtros aplicados.
La ruta `GET /api/v1/recepcion/pagos/historial` admite `desde`, `hasta`, `estado`, `metodo`,
`clienteId`, `q`, `page` y `size` (máximo 100), y requiere rol de recepción.

### Inicio de sesión con Google

1. En Google Cloud crea un cliente OAuth 2.0 de tipo **Aplicación web**.
2. Registra exactamente esta URI de redirección autorizada: `http://localhost:8080/login/oauth2/code/google`.
3. Completa `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET` en `.env`.
4. Activa `spring.profiles.active=google` en `.env` y reinicia la aplicación. Si configuras las variables directamente en IntelliJ, usa `SPRING_PROFILES_ACTIVE=google`.

La ruta `/auth/callback.html` es la pantalla final interna de la aplicación; no es la URI que se registra como callback OAuth en Google.

## Ejecución

Verifique primero que la terminal use Java 21:

```powershell
java -version
```

Si IntelliJ ya tiene un JDK 21 configurado, puede ejecutar `UcaCfcConnectApplication`. Desde PowerShell:

```powershell
./mvnw.cmd spring-boot:run
```

La API queda disponible en `http://localhost:8080`. La documentación interactiva está en `http://localhost:8080/swagger-ui.html` y el contrato OpenAPI en `http://localhost:8080/v3/api-docs`.

La interfaz administrativa de cursos está disponible en `http://localhost:8080/admin/cursos.html`. Requiere iniciar sesión con una cuenta que tenga el rol `ADMIN`.

Al iniciar la aplicación se crean de forma idempotente las categorías `Tecnología`, `Administración` e `Idiomas`, y las modalidades `Presencial`, `Virtual` e `Híbrida`. El formulario valida cupos y duración como enteros positivos, costos positivos con hasta dos decimales, fechas coherentes y horarios con días y rango de horas.

## Módulo de cursos

| Método | Ruta | Función |
|---|---|---|
| `GET` | `/api/v1/cursos` | Lista, filtra, pagina y ordena cursos |
| `GET` | `/api/v1/cursos/catalogos/categorias` | Lista las categorías válidas |
| `GET` | `/api/v1/cursos/catalogos/modalidades` | Lista las modalidades válidas |
| `GET` | `/api/v1/cursos/{id}` | Consulta un curso |
| `POST` | `/api/v1/cursos` | Crea un curso activo |
| `PUT` | `/api/v1/cursos/{id}` | Actualiza todos los datos del curso |
| `PATCH` | `/api/v1/cursos/{id}/estado` | Activa o inactiva el curso |
| `DELETE` | `/api/v1/cursos/{id}` | Elimina un curso sin dependencias |

Ejemplo para crear un curso:

```json
{
  "titulo": "Excel avanzado para negocios",
  "descripcion": "Curso práctico para el análisis y visualización de datos empresariales",
  "duracionHoras": 32,
  "cupoMaximo": 25,
  "costo": 125.00,
  "fechaInicio": "2026-10-05",
  "fechaFin": "2026-11-05",
  "horario": "Lunes y miércoles, 18:00-20:00",
  "idCategoria": 1,
  "idModalidad": 1
}
```

Ejemplo de búsqueda personalizada:

```text
GET /api/v1/cursos?texto=excel&idCategoria=1&activo=true&pagina=0&tamano=10&ordenarPor=costo&direccion=desc
```

Los campos de ordenamiento permitidos son `idCurso`, `titulo`, `duracionHoras`, `cupoMaximo`, `costo`, `fechaInicio`, `fechaFin`, `horario`, `activo`, `categoria` y `modalidad`.

## Inscripción y pagos del cliente

Desde el catálogo, un cliente con sesión iniciada puede inscribirse en un curso activo si aún hay cupo. El sistema crea o reutiliza su participante y registra la inscripción como `PENDIENTE`. En **Mi aprendizaje** puede consultar sus inscripciones y pagos.

| Método | Ruta | Función |
|---|---|---|
| `GET` | `/api/v1/cliente/inscripciones` | Lista las inscripciones de la cuenta autenticada |
| `POST` | `/api/v1/cliente/inscripciones` | Inscribe al cliente en un curso activo y con cupo |
| `GET` | `/api/v1/cliente/pagos` | Lista los pagos de la cuenta autenticada |
| `POST` | `/api/v1/cliente/pagos` | Registra un pago manual pendiente para una inscripción propia |
| `POST` | `/api/v1/cliente/pagos/wompi/iniciar` | Crea el enlace seguro de pago con tarjeta |
| `GET` | `/api/v1/cliente/pagos/wompi/retorno` | Recibe el retorno de Wompi y devuelve al navegador a Mi aprendizaje en localhost |
| `POST` | `/api/v1/cliente/pagos/wompi/webhook` | Confirma el resultado del pago desde Wompi

Para inscribirse, envía `{"idCurso": 10}`. Para registrar un pago, envía `{"idInscripcion": 25, "metodo": "TRANSFERENCIA", "referencia": "REF-123"}`. Los métodos admitidos son `EFECTIVO`, `TRANSFERENCIA` y `TARJETA`. El importe siempre se toma del costo de la inscripción; el cliente no puede modificarlo.

Los pagos en efectivo o transferencia quedan `PENDIENTE` hasta que recepción los verifique. Los pagos con tarjeta se procesan en Wompi; el webhook confirma la transacción y actualiza el acceso al curso.

### Configurar Wompi para desarrollo local

Wompi no requiere instalar un programa adicional. Cada desarrollador debe usar las credenciales de su propio aplicativo Wompi: coloca su App ID y API Secret en el archivo local `.env`, que está excluido de Git. No escribas credenciales reales en `.env.example` ni en el repositorio.

La app y el inicio de sesión permanecen en `http://localhost:8080`. Cloudflare Quick Tunnel solo expone temporalmente el callback para que Wompi alcance el backend. En `WOMPI_REDIRECT_URL` configura `https://<host-del-tunel>/api/v1/cliente/pagos/wompi/retorno`; el host `trycloudflare.com` cambia al reiniciar un túnel gratuito, así que actualiza `.env` y vuelve a iniciar Spring. `WOMPI_FRONTEND_RETURN_URL` debe permanecer en `http://localhost:8080/cliente/aprendizaje.html` para conservar la sesión local.

## Módulo de cotizaciones

Las cotizaciones se crean para un cliente y contienen uno o más detalles de cursos o espacios. El servidor toma el precio vigente del catálogo, calcula cada subtotal y el monto estimado total. Sus estados son `PENDIENTE`, `EN_PROCESO`, `APROBADA` y `RECHAZADA`.

| Método | Ruta | Función |
|---|---|---|
| `GET` | `/api/v1/cotizaciones` | Lista, filtra y pagina cotizaciones |
| `GET` | `/api/v1/cotizaciones/{id}` | Consulta una cotización y sus detalles |
| `POST` | `/api/v1/cotizaciones` | Solicita una cotización |
| `PUT` | `/api/v1/cotizaciones/{id}` | Actualiza una cotización pendiente |
| `PATCH` | `/api/v1/cotizaciones/{id}/estado` | Cambia su estado |
| `DELETE` | `/api/v1/cotizaciones/{id}` | Elimina la cotización si no posee pagos |

## Módulo de alquiler de espacios

El catálogo de espacios administra nombre, tipo, capacidad, precio, disponibilidad y equipamiento. Los alquileres relacionan un cliente con un espacio, fecha y horario; se rechazan automáticamente las reservas que se crucen con otra reserva activa. Sus estados son `PENDIENTE`, `CONFIRMADO`, `CANCELADO` y `FINALIZADO`.

| Método | Ruta | Función |
|---|---|---|
| `GET/POST` | `/api/v1/espacios` | Lista o crea espacios |
| `GET/PUT/DELETE` | `/api/v1/espacios/{id}` | Consulta, actualiza o elimina un espacio |
| `GET/POST` | `/api/v1/alquileres` | Lista o solicita alquileres |
| `GET/PUT/DELETE` | `/api/v1/alquileres/{id}` | Consulta, actualiza o elimina un alquiler |
| `PATCH` | `/api/v1/alquileres/{id}/estado` | Cambia el estado del alquiler |

## Validaciones y errores

Los DTO de entrada validan campos obligatorios, tamaños, valores positivos y relaciones. La capa de servicio valida que la fecha final no sea anterior a la inicial. Todos los errores usan una respuesta uniforme con código HTTP, mensaje, ruta y errores por campo cuando corresponda.

- `400`: formato, parámetros o validaciones incorrectas.
- `404`: curso, categoría o modalidad inexistente.
- `409`: eliminación bloqueada por relaciones en la base de datos.
- `422`: regla de negocio incumplida.

## Pruebas

Las pruebas usan H2 en memoria, por lo que no modifican MySQL:

```powershell
./mvnw.cmd test
```

Se cubren el arranque del contexto, las validaciones del DTO y los principales casos de negocio con JUnit 5, Mockito y una integración JPA sobre H2: CRUD, búsquedas con filtros, paginación, ordenamiento, fechas inválidas, relaciones inexistentes, cambio de estado y eliminación con dependencias.

## Estructura de paquetes

- `controller`: endpoints REST, validación de entrada y respuestas HTTP.
- `service`: reglas de negocio, transacciones y mapeo de DTO.
- `repository`: persistencia y búsquedas con Spring Data JPA.
- `entity`: entidades y relaciones de la base de datos.
- `dto`: contratos de entrada, salida, paginación y errores.
- `exception`: excepciones personalizadas y manejo centralizado.
- `config`: seguridad temporal y metadatos OpenAPI.

## Equipo

- Kevin Samuel Portillo Díaz - Analista funcional y diseñador UI
- Ariel Ismael Rivas Chacón - Analista de requerimientos
- David Alessandro Sibrián Castillo - Arquitecto backend
- Christopher Enrique Villacorta Molina - Project Manager y DevOps
- Kevin Alexander Zepeda Velásquez - Administrador de base de datos
