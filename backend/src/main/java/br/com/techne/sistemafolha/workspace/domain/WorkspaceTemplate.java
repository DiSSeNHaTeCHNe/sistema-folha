package br.com.techne.sistemafolha.workspace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "workspace_template")
public class WorkspaceTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "publicador_usuario_id", nullable = false)
    private Long publicadorUsuarioId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TemplateTipo tipo;

    @Column(name = "organograma_no_id")
    private Long organogramaNoId;

    @Column(nullable = false)
    private Boolean ativo = true;
}
