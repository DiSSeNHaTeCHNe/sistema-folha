package br.com.techne.sistemafolha.folha.application;

import java.math.BigDecimal;

final class FolhaCustoEmpresaComposer {

    private FolhaCustoEmpresaComposer() {
    }

    static BigDecimal compor(BigDecimal custoFolha, BigDecimal encargosRateados, BigDecimal custoBeneficios) {
        BigDecimal folha = custoFolha != null ? custoFolha : BigDecimal.ZERO;
        BigDecimal encargos = encargosRateados != null ? encargosRateados : BigDecimal.ZERO;
        BigDecimal beneficios = custoBeneficios != null ? custoBeneficios : BigDecimal.ZERO;
        return FolhaMotorCalculo.arredondar(folha.add(encargos).add(beneficios));
    }
}
