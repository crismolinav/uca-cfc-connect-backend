package sv.edu.udb.ucacfcconnect.dto.auth;

public record AuthResponse(
        long expiraEnSegundos,
        UsuarioResponse usuario
) {}
