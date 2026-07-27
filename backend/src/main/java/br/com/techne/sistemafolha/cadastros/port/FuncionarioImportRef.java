package br.com.techne.sistemafolha.cadastros.port;

public record FuncionarioImportRef(
    Long id,
    String idExterno,
    String nome,
    String cpf,
    Long cargoId,
    Long centroCustoId,
    Long linhaNegocioId
) {}
