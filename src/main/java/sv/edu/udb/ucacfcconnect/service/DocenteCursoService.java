package sv.edu.udb.ucacfcconnect.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.dto.CursoResponseDTO;
import sv.edu.udb.ucacfcconnect.repository.CursoDocenteRepository;
import sv.edu.udb.ucacfcconnect.repository.UsuarioRepository;

import java.util.List;

@Service
public class DocenteCursoService {
    private final CursoDocenteRepository asignaciones;
    private final UsuarioRepository usuarios;
    private final CursoService cursos;

    public DocenteCursoService(CursoDocenteRepository asignaciones, UsuarioRepository usuarios, CursoService cursos) {
        this.asignaciones = asignaciones;
        this.usuarios = usuarios;
        this.cursos = cursos;
    }

    @Transactional(readOnly = true)
    public List<CursoResponseDTO> listar(Long usuarioId) {
        var usuario = usuarios.findById(usuarioId).orElseThrow();
        if (usuario.getDocente() == null) return List.of();
        return asignaciones.findByDocente_Usuario_Id(usuarioId).stream()
                .map(asignacion -> cursos.obtenerPorId(asignacion.getCurso().getIdCurso())).toList();
    }
}
