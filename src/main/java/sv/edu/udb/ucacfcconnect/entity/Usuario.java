package sv.edu.udb.ucacfcconnect.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(name = "correo", columnNames = "correo"),
        @UniqueConstraint(name = "uq_oauth_provider_usuario", columnNames = {"oauth_provider", "oauth_provider_id"})
})
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Column(nullable = false, length = 120)
    private String correo;
    @Column(length = 255)
    private String password;
    @Column(nullable = false)
    private boolean activo = true;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rol", nullable = false, foreignKey = @ForeignKey(name = "fk_usuario_rol"))
    private Rol rol;
    @Column(name = "oauth_provider", length = 30)
    private String oauthProvider;
    @Column(name = "oauth_provider_id", length = 255)
    private String oauthProviderId;
    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado;
    @OneToOne(mappedBy = "usuario", fetch = FetchType.LAZY)
    private Cliente cliente;

    public Usuario() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public String getOauthProvider() { return oauthProvider; }
    public void setOauthProvider(String oauthProvider) { this.oauthProvider = oauthProvider; }
    public String getOauthProviderId() { return oauthProviderId; }
    public void setOauthProviderId(String oauthProviderId) { this.oauthProviderId = oauthProviderId; }
    public boolean isEmailVerificado() { return emailVerificado; }
    public void setEmailVerificado(boolean emailVerificado) { this.emailVerificado = emailVerificado; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
}
