package sv.edu.udb.ucacfcconnect.service;

import sv.edu.udb.ucacfcconnect.dto.auth.UsuarioResponse;

public record AuthSession(
        String token,
        long expiraEnSegundos,
        UsuarioResponse usuario
) {}
