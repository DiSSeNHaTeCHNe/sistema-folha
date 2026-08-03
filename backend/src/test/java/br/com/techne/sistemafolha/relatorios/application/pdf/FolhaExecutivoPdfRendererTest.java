package br.com.techne.sistemafolha.relatorios.application.pdf;

import br.com.techne.sistemafolha.dashboard.api.CentroCustoStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.DashboardStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.EvolucaoMensalDTO;
import br.com.techne.sistemafolha.dashboard.api.LinhaNegocioStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.RubricaStatsDTO;
import br.com.techne.sistemafolha.relatorios.application.BrandingTheme;
import br.com.techne.sistemafolha.relatorios.application.RelatorioFolhaModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FolhaExecutivoPdfRendererTest {

    private FolhaExecutivoPdfRenderer renderer;
    private BrandingTheme theme;

    @BeforeEach
    void setUp() {
        renderer = new FolhaExecutivoPdfRenderer(
            new RelatorioLayoutHelper(),
            new RelatorioChartImageFactory());
        theme = new BrandingTheme("#7836FC", "#3661FC", "#273340", "#f8fafc", Optional.empty());
    }

    @Test
    void render_produzPdfComMagicBytesEStringsChave() {
        RelatorioFolhaModel model = modelCompleto(false);
        byte[] pdf = renderer.render(model);

        assertTrue(new String(pdf, 0, 4, StandardCharsets.US_ASCII).startsWith("%PDF"));
        String text = extractText(pdf);
        assertTrue(text.contains("Relat") && text.contains("rio Executivo de Folha"));
        assertTrue(text.contains("06/2024"));
        assertTrue(text.contains("Gerado pelo Sistema de Folha"));
        assertTrue(text.contains("Techne"));
    }

    @Test
    void render_semDados_contemNotaSemDados() {
        RelatorioFolhaModel model = new RelatorioFolhaModel(
            theme, "06/2024", "gestor", LocalDateTime.of(2024, 6, 15, 10, 0),
            statsVazios(), List.of(), true);

        byte[] pdf = renderer.render(model);
        String text = extractText(pdf);

        assertTrue(text.contains("Sem dados para a compet"));
        assertTrue(text.contains("Relat") && text.contains("rio Executivo"));
    }

    @Test
    void render_refleteKpisFormatados() {
        RelatorioFolhaModel model = modelCompleto(false);
        byte[] pdf = renderer.render(model);
        String text = extractText(pdf);

        assertTrue(text.contains("Total Funcion"));
        assertTrue(text.contains("Custo Empresa"));
        assertTrue(text.contains("Total Proventos"));
        assertTrue(text.contains("Total Descontos"));
        assertTrue(text.contains("150"));
        assertTrue(text.contains("8.000") || text.contains("8000"));
        assertTrue(text.contains("1.000") || text.contains("1000"));
        assertTrue(text.contains("9.000") || text.contains("9000"));
        assertTrue(text.contains("Gerado em: 15/06/2024 10:00"));
    }

    private RelatorioFolhaModel modelCompleto(boolean semDados) {
        return new RelatorioFolhaModel(
            theme,
            "06/2024",
            "gestor@teste.com",
            LocalDateTime.of(2024, 6, 15, 10, 0),
            statsComDados(),
            List.of(
                new EvolucaoMensalDTO("Jan/2024", new BigDecimal("7000"), 140),
                new EvolucaoMensalDTO("Jun/2024", new BigDecimal("9000"), 150)),
            semDados);
    }

    private DashboardStatsDTO statsComDados() {
        return new DashboardStatsDTO(
            150L,
            new BigDecimal("9000.00"),
            25L,
            List.of(new LinhaNegocioStatsDTO(1L, "Educacional", 100L, new BigDecimal("6000"))),
            List.of(new CentroCustoStatsDTO(1L, "CC Admin", 80L, new BigDecimal("5000"))),
            List.of(),
            new BigDecimal("8000.00"),
            new BigDecimal("1000.00"),
            List.of(new RubricaStatsDTO(1L, "001", "Salário", new BigDecimal("7000"), 150L)),
            List.of(new RubricaStatsDTO(2L, "002", "INSS", new BigDecimal("800"), 150L)),
            List.of());
    }

    private DashboardStatsDTO statsVazios() {
        return new DashboardStatsDTO(
            0L, BigDecimal.ZERO, 0L,
            List.of(), List.of(), List.of(),
            BigDecimal.ZERO, BigDecimal.ZERO,
            List.of(), List.of(), List.of());
    }

    private String extractText(byte[] pdf) {
        return new String(pdf, StandardCharsets.ISO_8859_1);
    }
}
