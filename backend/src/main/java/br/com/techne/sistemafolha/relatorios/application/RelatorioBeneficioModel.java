package br.com.techne.sistemafolha.relatorios.application;

import br.com.techne.sistemafolha.beneficios.port.BeneficioCcTipoSnapshot;
import br.com.techne.sistemafolha.beneficios.port.BeneficioFuncionarioValorSnapshot;
import br.com.techne.sistemafolha.beneficios.port.BeneficioTipoResumoSnapshot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record RelatorioBeneficioModel(
    BrandingTheme branding,
    String competenciaLabel,
    String geradoPor,
    LocalDateTime geradoEm,
    BigDecimal totalBeneficios,
    long qtdLancamentos,
    BigDecimal totalCustoFolha,
    BigDecimal custoConsolidado,
    List<BeneficioTipoResumoSnapshot> porTipo,
    Map<Long, List<BeneficioFuncionarioValorSnapshot>> top10PorTipo,
    List<BeneficioCcTipoSnapshot> matrizCcTipo,
    boolean semBeneficios,
    boolean semFolha
) {}
