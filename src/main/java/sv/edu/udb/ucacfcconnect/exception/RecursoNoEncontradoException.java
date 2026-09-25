package sv.edu.udb.ucacfcconnect.exception;

public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, Long id) {
        super(recurso + " con identificador " + id + " no existe");
    }
}
