package br.com.techne.sistemafolha.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "nos_organograma")
@EqualsAndHashCode(callSuper = false)
public class NoOrganograma {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 1, max = 100, message = "O nome deve ter entre 1 e 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nome;

    @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
    @Column(columnDefinition = "TEXT")
    private String descricao;

    @NotNull(message = "O nível é obrigatório")
    @Column(nullable = false)
    private Integer nivel = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private NoOrganograma parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<NoOrganograma> children = new ArrayList<>();

    @NotNull(message = "A posição é obrigatória")
    @Column(nullable = false)
    private Integer posicao = 0;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "organograma_ativo", nullable = false)
    private Boolean organogramaAtivo = false;

    @OneToMany(mappedBy = "noOrganograma", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FuncionarioOrganograma> funcionarios = new ArrayList<>();

    @OneToMany(mappedBy = "noOrganograma", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CentroCustoOrganograma> centrosCusto = new ArrayList<>();

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    @Column(name = "criado_por", length = 100)
    private String criadoPor;

    @Column(name = "atualizado_por", length = 100)
    private String atualizadoPor;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
        
        // Se o parent existe, definir o nível baseado no parent
        if (parent != null) {
            nivel = parent.getNivel() + 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    // Método auxiliar para verificar se é um nó raiz
    public boolean isRaiz() {
        return parent == null;
    }

    // Método auxiliar para verificar se tem filhos
    public boolean temFilhos() {
        return children != null && !children.isEmpty();
    }

    // Método auxiliar para obter funcionários ativos
    public List<FuncionarioOrganograma> getFuncionariosAtivos() {
        return funcionarios.stream()
                .filter(fo -> fo.getFuncionario().getAtivo())
                .toList();
    }

    // Método auxiliar para obter centros de custo ativos
    public List<CentroCustoOrganograma> getCentrosCustoAtivos() {
        return centrosCusto.stream()
                .filter(cco -> cco.getCentroCusto().getAtivo())
                .toList();
    }
} 