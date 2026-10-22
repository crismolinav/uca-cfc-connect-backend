package sv.edu.udb.ucacfcconnect.dto;

import java.util.List;

public record DocentePerfilDTO(
        Long idDocente,
        String nombre,
        String especialidad,
        String descripcion,
        String fotoUrl,
        Integer cantidadCursos,
        Long estudiantesInscritos,
        Double calificacionPromedio,
        Long cantidadResenas,
        List<ResenaResumenDTO> resenas
) {}
