package br.com.techne.sistemafolha.relatorios.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(name = "relatorio_arquivo")
public class RelatorioArquivo {

    @Id
    @Column(name = "relatorio_id")
    private Long relatorioId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "relatorio_id")
    private Relatorio relatorio;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "pdf_bytes", nullable = false)
    private byte[] pdfBytes;

    @Column(name = "tamanho_bytes", nullable = false)
    private Long tamanhoBytes;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now(Clock.systemDefaultZone());
    }
}
