package br.com.techne.sistemafolha.dto;

public class RubricaDTO {
    private Long id;
    private String codigo;
    private String descricao;
    private String tipoRubricaDescricao;
    private Double porcentagem;
    private Boolean ativo;

    public RubricaDTO(Long id, String codigo, String descricao, String tipoRubricaDescricao, Double porcentagem, Boolean ativo) {
        this.id = id;
        this.codigo = codigo;
        this.descricao = descricao;
        this.tipoRubricaDescricao = tipoRubricaDescricao;
        this.porcentagem = porcentagem;
        this.ativo = ativo;
    }

    // getters e setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getTipoRubricaDescricao() { return tipoRubricaDescricao; }
    public void setTipoRubricaDescricao(String tipoRubricaDescricao) { this.tipoRubricaDescricao = tipoRubricaDescricao; }
    public Double getPorcentagem() { return porcentagem; }
    public void setPorcentagem(Double porcentagem) { this.porcentagem = porcentagem; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
} 