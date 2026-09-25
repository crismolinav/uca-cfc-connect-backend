package sv.edu.udb.ucacfcconnect.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import sv.edu.udb.ucacfcconnect.dto.ErrorResponseDTO;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> manejarValidacion(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return construir(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos", request, errores);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> manejarRestricciones(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errores.put(violation.getPropertyPath().toString(), violation.getMessage())
        );
        return construir(HttpStatus.BAD_REQUEST, "Parámetros inválidos", request, errores);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponseDTO> manejarFormatoInvalido(
            Exception ex,
            HttpServletRequest request
    ) {
        return construir(
                HttpStatus.BAD_REQUEST,
                "El cuerpo o los parámetros de la solicitud tienen un formato inválido",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorResponseDTO> manejarSolicitudInvalida(
            SolicitudInvalidaException ex,
            HttpServletRequest request
    ) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> manejarNoEncontrado(
            RecursoNoEncontradoException ex,
            HttpServletRequest request
    ) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponseDTO> manejarConflicto(
            ConflictException ex,
            HttpServletRequest request
    ) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponseDTO> manejarReglaNegocio(
            ReglaNegocioException ex,
            HttpServletRequest request
    ) {
        return construir(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> manejarIntegridad(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        return construir(
                HttpStatus.CONFLICT,
                "La operación viola una restricción de integridad de la base de datos",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> manejarErrorInesperado(
            Exception ex,
            HttpServletRequest request
    ) {
        LOGGER.error("Error inesperado procesando {}", request.getRequestURI(), ex);
        return construir(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno al procesar la solicitud",
                request,
                Map.of()
        );
    }

    private ResponseEntity<ErrorResponseDTO> construir(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors
    ) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(error);
    }
}
