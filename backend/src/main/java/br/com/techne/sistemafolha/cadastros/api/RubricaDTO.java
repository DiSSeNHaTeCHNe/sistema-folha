package br.com.techne.sistemafolha.cadastros.api;

public class RubricaDTO {
    private Long id;
    private String codigo;
    private String descricao;
    private String tipoRubricaDescricao;
    private String tipo;
    private Double porcentagem;
    private Short operadorBruto;
    private Short operadorLiquido;
    private Short operadorCusto;
    private Boolean ativo;

    @SuppressWarnings("java:S107") // OpenAPI/DTO field parity; builder refactor deferred
    public RubricaDTO(Long id, String codigo, String descricao, String tipoRubricaDescricao, String tipo,
                      Double porcentagem, Short operadorBruto, Short operadorLiquido, Short operadorCusto,
                      Boolean ativo) {
        this.id = id;
        this.codigo = codigo;
        this.descricao = descricao;
        this.tipoRubricaDescricao = tipoRubricaDescricao;
        this.tipo = tipo;
        this.porcentagem = porcentagem;
        this.operadorBruto = operadorBruto;
        this.operadorLiquido = operadorLiquido;
        this.operadorCusto = operadorCusto;
        this.ativo = ativo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getTipoRubricaDescricao() { return tipoRubricaDescricao; }
    public void setTipoRubricaDescricao(String tipoRubricaDescricao) { this.tipoRubricaDescricao = tipoRubricaDescricao; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Double getPorcentagem() { return porcentagem; }
    public void setPorcentagem(Double porcentagem) { this.porcentagem = porcentagem; }
    public Short getOperadorBruto() { return operadorBruto; }
    public void setOperadorBruto(Short operadorBruto) { this.operadorBruto = operadorBruto; }
    public Short getOperadorLiquido() { return operadorLiquido; }
    public void setOperadorLiquido(Short operadorLiquido) { this.operadorLiquido = operadorLiquido; }
    public Short getOperadorCusto() { return operadorCusto; }
    public void setOperadorCusto(Short operadorCusto) { this.operadorCusto = operadorCusto; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}
