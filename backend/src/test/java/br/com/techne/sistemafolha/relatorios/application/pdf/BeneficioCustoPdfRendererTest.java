package br.com.techne.sistemafolha.relatorios.application.pdf;

import br.com.techne.sistemafolha.beneficios.port.BeneficioCcTipoSnapshot;
import br.com.techne.sistemafolha.beneficios.port.BeneficioFuncionarioValorSnapshot;
import br.com.techne.sistemafolha.beneficios.port.BeneficioTipoResumoSnapshot;
import br.com.techne.sistemafolha.relatorios.application.BrandingTheme;
import br.com.techne.sistemafolha.relatorios.application.RelatorioBeneficioModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BeneficioCustoPdfRendererTest {

    private BeneficioCustoPdfRenderer renderer;
    private BrandingTheme theme;

    @BeforeEach
    void setUp() {
        renderer = new BeneficioCustoPdfRenderer(new RelatorioLayoutHelper());
        theme = new BrandingTheme("#7836FC", "#3661FC", "#273340", "#f8fafc", Optional.empty());
    }

    @Test
    void render_contemTituloETabelaTipos() {
        byte[] pdf = renderer.render(modelCompleto(false, false));
        String text = extractText(pdf);

        assertTrue(new String(pdf, 0, 4, StandardCharsets.US_ASCII).startsWith("%PDF"));
        assertTrue(text.contains("Relat") && text.contains("rio de Custo"));
        assertTrue(text.contains("Benef") && text.contains("cios e Folha"));
        assertTrue(text.contains("4000"));
        assertTrue(text.contains("Vale Refei"));
        assertTrue(text.contains("R$"));
    }

    @Test
    void render_semBeneficios_contemNota() {
        byte[] pdf = renderer.render(modelCompleto(true, false));
        String text = extractText(pdf);

        assertTrue(text.contains("Nenhum benef") && text.contains("cio lan"));
    }

    @Test
    void render_semFolha_contemNotaFolha() {
        byte[] pdf = renderer.render(modelCompleto(false, true));
        String text = extractText(pdf);

        assertTrue(text.contains("Sem dados de folha"));
    }

    @Test
    void render_valoresFormatadosPtBr() {
        byte[] pdf = renderer.render(modelCompleto(false, false));
        String text = extractText(pdf);

        assertTrue(text.contains("R$") && (text.contains("1.500") || text.contains("1500")));
    }

    private RelatorioBeneficioModel modelCompleto(boolean semBeneficios, boolean semFolha) {
        BeneficioTipoResumoSnapshot vr = new BeneficioTipoResumoSnapshot(
            1L, "4000", "Vale Refeição", new BigDecimal("1500.00"), 10L);
        BeneficioFuncionarioValorSnapshot func = new BeneficioFuncionarioValorSnapshot(
            100L, "Maria Silva", new BigDecimal("500.00"));
        BeneficioCcTipoSnapshot matriz = new BeneficioCcTipoSnapshot(
            10L, "CC Admin", 1L, "4000", "Vale Refeição", new BigDecimal("1500.00"));

        return new RelatorioBeneficioModel(
            theme,
            "06/2024",
            "gestor@teste.com",
            LocalDateTime.of(2024, 6, 15, 10, 0),
            semBeneficios ? BigDecimal.ZERO : new BigDecimal("1500.00"),
            semBeneficios ? 0L : 10L,
            semFolha ? BigDecimal.ZERO : new BigDecimal("9000.00"),
            semBeneficios && semFolha ? BigDecimal.ZERO : new BigDecimal("10500.00"),
            semBeneficios ? List.of() : List.of(vr),
            semBeneficios ? Map.of() : Map.of(1L, List.of(func)),
            semBeneficios ? List.of() : List.of(matriz),
            semBeneficios,
            semFolha);
    }

    private String extractText(byte[] pdf) {
        return new String(pdf, StandardCharsets.ISO_8859_1);
    }
}
