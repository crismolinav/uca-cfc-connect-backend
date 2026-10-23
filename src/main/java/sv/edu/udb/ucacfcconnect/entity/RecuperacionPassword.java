package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "recuperaciones_password", indexes = {
        @Index(name = "idx_recuperacion_usuario_creada", columnList = "id_usuario, creado_en")
})
public class RecuperacionPassword {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recuperacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "usado_en")
    private Instant usadoEn;

    @Column(name = "password_version", nullable = false)
    private long passwordVersion;

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
    public Instant getExpiraEn() { return expiraEn; }
    public void setExpiraEn(Instant expiraEn) { this.expiraEn = expiraEn; }
    public Instant getUsadoEn() { return usadoEn; }
    public void setUsadoEn(Instant usadoEn) { this.usadoEn = usadoEn; }
    public long getPasswordVersion() { return passwordVersion; }
    public void setPasswordVersion(long passwordVersion) { this.passwordVersion = passwordVersion; }
}
