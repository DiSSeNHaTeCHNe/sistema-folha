package br.com.techne.sistemafolha.relatorios.domain;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(name = "relatorio")
public class Relatorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RelatorioTipo tipo;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false)
    private Integer ano;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RelatorioStatus status = RelatorioStatus.PENDENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "total_funcionarios")
    private Integer totalFuncionarios;

    @Column(name = "total_folha", precision = 19, scale = 2)
    private BigDecimal totalFolha;

    @Column(name = "total_beneficios", precision = 19, scale = 2)
    private BigDecimal totalBeneficios;

    @Column(name = "total_valor", precision = 19, scale = 2)
    private BigDecimal totalValor;

    @Column(length = 500)
    private String erro;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_processamento")
    private LocalDateTime dataProcessamento;

    @Column(nullable = false)
    private Boolean ativo = true;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now(Clock.systemDefaultZone());
        if (status == null) {
            status = RelatorioStatus.PENDENTE;
        }
        if (ativo == null) {
            ativo = true;
        }
    }
}
