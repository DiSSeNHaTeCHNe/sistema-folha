package br.com.techne.sistemafolha.relatorios.application.pdf;

import br.com.techne.sistemafolha.relatorios.application.BrandingTheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioChartImageFactoryTest {

    private RelatorioChartImageFactory factory;
    private BrandingTheme theme;

    @BeforeEach
    void setUp() {
        factory = new RelatorioChartImageFactory();
        theme = new BrandingTheme("#7836FC", "#3661FC", "#273340", "#f8fafc", Optional.empty());
    }

    @Test
    void lineChart_comDoisPontos_retornaPngNonEmpty() {
        byte[] png = factory.lineChart(
            List.of("Jan/2024", "Jun/2024"),
            List.of(new BigDecimal("7000"), new BigDecimal("9000")),
            theme);

        assertTrue(png.length > 0);
    }

    @Test
    void lineChart_labelsMesAno_formatoMmmYyyy() {
        List<String> labels = List.of("Jan/2024", "Fev/2024", "Mar/2024");
        byte[] png = factory.lineChart(
            labels,
            List.of(new BigDecimal("1000"), new BigDecimal("1500"), new BigDecimal("1200")),
            theme);

        assertTrue(png.length > 0);
        labels.forEach(label -> assertTrue(label.matches("[A-Za-z]{3}/\\d{4}")));
    }

    @Test
    void lineChart_menosDeDoisPontos_retornaVazio() {
        byte[] png = factory.lineChart(
            List.of("Jan/2024"),
            List.of(new BigDecimal("1000")),
            theme);

        assertTrue(png.length == 0);
    }

    @Test
    void horizontalBarChart_respeitaMaxBars() {
        Map<String, BigDecimal> data = new LinkedHashMap<>();
        for (int i = 1; i <= 8; i++) {
            data.put("Item " + i, BigDecimal.valueOf(i * 100));
        }

        byte[] png = factory.horizontalBarChart(data, theme, 5);

        assertTrue(png.length > 0);
    }

    @Test
    void horizontalBarChart_vazio_retornaZeroBytes() {
        byte[] png = factory.horizontalBarChart(Map.of(), theme, 5);
        assertTrue(png.length == 0);
    }
}
