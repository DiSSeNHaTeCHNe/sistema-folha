package br.com.techne.sistemafolha.relatorios.application.pdf;

import br.com.techne.sistemafolha.dashboard.api.CentroCustoStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.EvolucaoMensalDTO;
import br.com.techne.sistemafolha.dashboard.api.LinhaNegocioStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.RubricaStatsDTO;
import br.com.techne.sistemafolha.relatorios.application.RelatorioFolhaModel;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FolhaExecutivoPdfRenderer {

    private static final int TOP_N = 15;

    private final RelatorioLayoutHelper layoutHelper;
    private final RelatorioChartImageFactory chartFactory;

    public byte[] render(RelatorioFolhaModel model) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 48);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setCompressionLevel(0);
            writer.setPageEvent(layoutHelper.createFooterEvent());
            document.open();

            renderCapa(document, model);
            document.newPage();

            if (model.semDados()) {
                document.add(new Paragraph(
                    "Sem dados para a competência selecionada",
                    layoutHelper.bodyFont(model.branding())));
                document.close();
                return out.toByteArray();
            }

            renderSecoes(document, model);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao renderizar PDF executivo de folha", e);
        }
    }

    private void renderCapa(Document document, RelatorioFolhaModel model) throws Exception {
        var theme = model.branding();
        Color primary = layoutHelper.parseColor(theme.primaryColor());

        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        com.lowagie.text.pdf.PdfPCell headerCell = new com.lowagie.text.pdf.PdfPCell();
        headerCell.setBackgroundColor(primary);
        headerCell.setFixedHeight(72);
        headerCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        header.addCell(headerCell);
        document.add(header);

        if (theme.logoBytes().isPresent()) {
            Image logo = Image.getInstance(theme.logoBytes().get());
            logo.scaleToFit(180, 50);
            logo.setAlignment(Element.ALIGN_CENTER);
            document.add(logo);
        } else {
            Font wordmark = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, primary);
            Paragraph techne = new Paragraph("TECHNE", wordmark);
            techne.setAlignment(Element.ALIGN_CENTER);
            document.add(techne);
        }

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Color.WHITE);
        Paragraph title = new Paragraph("Relatório Executivo de Folha", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(12);
        document.add(title);

        Font compFont = FontFactory.getFont(FontFactory.HELVETICA, 18, layoutHelper.parseColor(theme.textColor()));
        Paragraph competencia = new Paragraph(model.competenciaLabel(), compFont);
        competencia.setAlignment(Element.ALIGN_CENTER);
        competencia.setSpacingAfter(20);
        document.add(competencia);

        var stats = model.stats();
        PdfPTable kpis = new PdfPTable(4);
        kpis.setWidthPercentage(100);
        kpis.setSpacingBefore(12);
        addKpiCell(kpis, "Funcionários", String.valueOf(stats.totalFuncionarios()), theme);
        addKpiCell(kpis, "Custo Folha", layoutHelper.formatCurrency(stats.custoMensalFolha()), theme);
        addKpiCell(kpis, "Benefícios Ativos", String.valueOf(stats.totalBeneficiosAtivos()), theme);
        addKpiCell(kpis, "Proventos", layoutHelper.formatCurrency(stats.totalProventos()), theme);
        document.add(kpis);

        Font metaFont = layoutHelper.bodyFont(theme);
        document.add(new Paragraph("Gerado por: " + model.geradoPor(), metaFont));
        document.add(new Paragraph("Gerado pelo Sistema de Folha — Techne", metaFont));
    }

    private void addKpiCell(PdfPTable table, String label, String value,
            br.com.techne.sistemafolha.relatorios.application.BrandingTheme theme) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell();
        cell.setBorderWidth(1);
        cell.setBorderColor(layoutHelper.parseColor(theme.primaryColor()));
        cell.setBackgroundColor(layoutHelper.parseColor(theme.mutedBackground()));
        cell.setPadding(8);
        cell.addElement(new Phrase(label, layoutHelper.kpiLabelFont(theme)));
        cell.addElement(new Phrase(value, layoutHelper.kpiValueFont(theme)));
        table.addCell(cell);
    }

    private void renderSecoes(Document document, RelatorioFolhaModel model) throws Exception {
        var theme = model.branding();
        var stats = model.stats();

        document.add(new Paragraph("Centros de Custo", layoutHelper.titleFont(theme)));
        document.add(layoutHelper.createZebraTable(
            List.of("Centro de Custo", "Funcionários", "Valor Total"),
            topComOutros(stats.porCentroCusto()).stream()
                .map(c -> List.of(
                    c.descricao(),
                    String.valueOf(c.quantidadeFuncionarios()),
                    layoutHelper.formatCurrency(c.valorTotal())))
                .toList(),
            theme));

        document.add(new Paragraph("Linhas de Negócio", layoutHelper.titleFont(theme)));
        document.add(layoutHelper.createZebraTable(
            List.of("Linha de Negócio", "Funcionários", "Valor Total"),
            topComOutrosLn(stats.porLinhaNegocio()).stream()
                .map(l -> List.of(
                    l.descricao(),
                    String.valueOf(l.quantidadeFuncionarios()),
                    layoutHelper.formatCurrency(l.valorTotal())))
                .toList(),
            theme));

        document.add(new Paragraph("Top 5 Proventos", layoutHelper.titleFont(theme)));
        document.add(layoutHelper.createZebraTable(
            List.of("Código", "Descrição", "Valor", "Qtd"),
            stats.topProventos().stream()
                .map(r -> rubricaRow(r))
                .toList(),
            theme));

        document.add(new Paragraph("Top 5 Descontos", layoutHelper.titleFont(theme)));
        document.add(layoutHelper.createZebraTable(
            List.of("Código", "Descrição", "Valor", "Qtd"),
            stats.topDescontos().stream()
                .map(r -> rubricaRow(r))
                .toList(),
            theme));

        document.add(new Paragraph("Evolução — últimos 6 meses", layoutHelper.titleFont(theme)));
        List<EvolucaoMensalDTO> evolucao = model.evolucao6Meses() != null ? model.evolucao6Meses() : List.of();
        if (!evolucao.isEmpty()) {
            List<String> labels = evolucao.stream().map(EvolucaoMensalDTO::mesAno).toList();
            List<BigDecimal> values = evolucao.stream().map(EvolucaoMensalDTO::valorTotal).toList();
            byte[] chart = chartFactory.lineChart(labels, values, theme);
            if (chart.length > 0) {
                Image img = Image.getInstance(chart);
                img.scaleToFit(480, 200);
                document.add(img);
            }
        }
    }

    private List<String> rubricaRow(RubricaStatsDTO r) {
        return List.of(
            r.codigo() != null ? r.codigo() : "",
            r.descricao() != null ? r.descricao() : "",
            layoutHelper.formatCurrency(r.valorTotal()),
            String.valueOf(r.quantidadeOcorrencias())
        );
    }

    private List<CentroCustoStatsDTO> topComOutros(List<CentroCustoStatsDTO> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<CentroCustoStatsDTO> sorted = items.stream()
            .sorted(Comparator.comparing(CentroCustoStatsDTO::valorTotal).reversed())
            .toList();
        if (sorted.size() <= TOP_N) {
            return sorted;
        }
        List<CentroCustoStatsDTO> top = new ArrayList<>(sorted.subList(0, TOP_N));
        BigDecimal outrosValor = sorted.subList(TOP_N, sorted.size()).stream()
            .map(CentroCustoStatsDTO::valorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long outrosFunc = sorted.subList(TOP_N, sorted.size()).stream()
            .mapToLong(CentroCustoStatsDTO::quantidadeFuncionarios)
            .sum();
        top.add(new CentroCustoStatsDTO(null, "Outros", outrosFunc, outrosValor));
        return top;
    }

    private List<LinhaNegocioStatsDTO> topComOutrosLn(List<LinhaNegocioStatsDTO> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<LinhaNegocioStatsDTO> sorted = items.stream()
            .sorted(Comparator.comparing(LinhaNegocioStatsDTO::valorTotal).reversed())
            .toList();
        if (sorted.size() <= TOP_N) {
            return sorted;
        }
        List<LinhaNegocioStatsDTO> top = new ArrayList<>(sorted.subList(0, TOP_N));
        BigDecimal outrosValor = sorted.subList(TOP_N, sorted.size()).stream()
            .map(LinhaNegocioStatsDTO::valorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long outrosFunc = sorted.subList(TOP_N, sorted.size()).stream()
            .mapToLong(LinhaNegocioStatsDTO::quantidadeFuncionarios)
            .sum();
        top.add(new LinhaNegocioStatsDTO(null, "Outros", outrosFunc, outrosValor));
        return top;
    }
}
