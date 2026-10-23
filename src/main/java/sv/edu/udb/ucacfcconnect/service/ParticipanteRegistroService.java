package sv.edu.udb.ucacfcconnect.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.ucacfcconnect.entity.Cliente;
import sv.edu.udb.ucacfcconnect.entity.Participante;
import sv.edu.udb.ucacfcconnect.repository.ClienteRepository;
import sv.edu.udb.ucacfcconnect.repository.ParticipanteRepository;

/** Una sola identidad de participante por cliente, independientemente del origen del alta. */
@Service
public class ParticipanteRegistroService {
    private final ClienteRepository clientes;
    private final ParticipanteRepository participantes;

    public ParticipanteRegistroService(ClienteRepository clientes, ParticipanteRepository participantes) {
        this.clientes = clientes;
        this.participantes = participantes;
    }

    @Transactional
    public Participante asegurar(Cliente cliente) {
        clientes.bloquear(cliente.getId()).orElseThrow();
        Participante participante = participantes.findFirstByClienteIdOrderByIdAsc(cliente.getId()).orElse(null);
        boolean nuevo = participante == null;
        if (nuevo) {
            participante = new Participante();
            participante.setCliente(cliente);
        }
        if (cliente.getPrimerNombre() != null) {
            participante.setNombre((cliente.getPrimerNombre() + " " +
                    (cliente.getSegundoNombre() == null ? "" : cliente.getSegundoNombre())).trim());
            participante.setApellido(((cliente.getPrimerApellido() == null ? "" : cliente.getPrimerApellido()) + " " +
                    (cliente.getSegundoApellido() == null ? "" : cliente.getSegundoApellido())).trim());
        } else if (nuevo) {
            // Las fichas históricas mantienen el nombre original; no se inventan apellidos.
            participante.setNombre(cliente.getNombre());
            participante.setApellido("");
        }
        participante.setCorreo(cliente.getCorreo());
        participante.setTelefono(cliente.getTelefono());
        return participantes.saveAndFlush(participante);
    }
}
