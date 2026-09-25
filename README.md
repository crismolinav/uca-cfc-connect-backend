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

La aplicación usa por defecto la base local `uca_cfc_connect`, el usuario `root` y una contraseña vacía. Se puede cambiar la configuración sin modificar el código mediante estas variables de entorno:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/uca_cfc_connect'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='tu_contrasena'
```

Las tablas `categorias` y `modalidades` deben contener registros antes de crear un curso, porque el API valida que ambas llaves foráneas existan. El archivo `database/datos_catalogo_cursos.sql` incluye datos iniciales idempotentes para ambos catálogos; puede ejecutarse desde MySQL Workbench. Los identificadores generados se consultan luego mediante los endpoints de catálogo.

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
