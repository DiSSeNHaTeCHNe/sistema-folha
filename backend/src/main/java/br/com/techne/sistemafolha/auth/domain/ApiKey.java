package br.com.techne.sistemafolha.auth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "api_keys")
public class ApiKey {

    public static final String ESCOPO_READ = "READ";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 32)
    private String prefixo;

    @Column(name = "hash_chave", nullable = false)
    private String hashChave;

    @Column(nullable = false, length = 16)
    private String escopo = ESCOPO_READ;

    @Column(name = "data_expiracao", nullable = false)
    private LocalDateTime dataExpiracao;

    @Column(nullable = false)
    private Boolean revogado = false;

    @Column(name = "ultimo_uso_em")
    private LocalDateTime ultimoUsoEm;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now(Clock.systemDefaultZone());
        if (escopo == null) {
            escopo = ESCOPO_READ;
        }
    }

    public boolean isExpirada() {
        return LocalDateTime.now(Clock.systemDefaultZone()).isAfter(dataExpiracao);
    }

    public boolean isRevogado() {
        return revogado != null && revogado;
    }

    public boolean isValida() {
        return !isExpirada() && !isRevogado();
    }
}
