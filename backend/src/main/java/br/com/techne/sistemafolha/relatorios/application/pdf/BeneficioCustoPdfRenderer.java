package br.com.techne.sistemafolha.relatorios.application.pdf;

import br.com.techne.sistemafolha.beneficios.port.BeneficioCcTipoSnapshot;
import br.com.techne.sistemafolha.beneficios.port.BeneficioFuncionarioValorSnapshot;
import br.com.techne.sistemafolha.beneficios.port.BeneficioTipoResumoSnapshot;
import br.com.techne.sistemafolha.relatorios.application.RelatorioBeneficioModel;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BeneficioCustoPdfRenderer {

    private final RelatorioLayoutHelper layoutHelper;

    public byte[] render(RelatorioBeneficioModel model) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 48);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setCompressionLevel(0);
            writer.setPageEvent(layoutHelper.createFooterEvent());
            document.open();

            renderCapa(document, model);
            document.newPage();
            renderCorpo(document, model);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao renderizar PDF de custo benefício", e);
        }
    }

    private void renderCapa(Document document, RelatorioBeneficioModel model) throws Exception {
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

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, layoutHelper.parseColor(theme.textColor()));
        Paragraph title = new Paragraph("Relatório de Custo — Benefícios e Folha", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(16);
        document.add(title);

        Paragraph competencia = new Paragraph(model.competenciaLabel(), layoutHelper.bodyFont(theme));
        competencia.setAlignment(Element.ALIGN_CENTER);
        competencia.setSpacingAfter(16);
        document.add(competencia);

        PdfPTable kpis = new PdfPTable(4);
        kpis.setWidthPercentage(100);
        addKpi(kpis, "Benefícios", layoutHelper.formatCurrency(model.totalBeneficios()), theme);
        addKpi(kpis, "Lançamentos", String.valueOf(model.qtdLancamentos()), theme);
        addKpi(kpis, "Custo Folha", layoutHelper.formatCurrency(model.totalCustoFolha()), theme);
        addKpi(kpis, "Consolidado", layoutHelper.formatCurrency(model.custoConsolidado()), theme);
        document.add(kpis);

        document.add(new Paragraph("Gerado por: " + model.geradoPor(), layoutHelper.bodyFont(theme)));
        document.add(new Paragraph("Gerado pelo Sistema de Folha — Techne", layoutHelper.bodyFont(theme)));
    }

    private void addKpi(PdfPTable table, String label, String value,
            br.com.techne.sistemafolha.relatorios.application.BrandingTheme theme) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell();
        cell.setPadding(8);
        cell.setBorderColor(layoutHelper.parseColor(theme.primaryColor()));
        cell.setBackgroundColor(layoutHelper.parseColor(theme.mutedBackground()));
        cell.addElement(new Phrase(label, layoutHelper.kpiLabelFont(theme)));
        cell.addElement(new Phrase(value, layoutHelper.kpiValueFont(theme)));
        table.addCell(cell);
    }

    private void renderCorpo(Document document, RelatorioBeneficioModel model) {
        var theme = model.branding();

        if (model.semBeneficios()) {
            document.add(new Paragraph("Nenhum benefício lançado", layoutHelper.bodyFont(theme)));
        }
        if (model.semFolha()) {
            document.add(new Paragraph("Sem dados de folha para a competência", layoutHelper.bodyFont(theme)));
        }

        document.add(new Paragraph("Resumo por Tipo", layoutHelper.titleFont(theme)));
        List<BeneficioTipoResumoSnapshot> tipos = model.porTipo() != null ? model.porTipo() : List.of();
        document.add(layoutHelper.createZebraTable(
            List.of("Código", "Descrição", "Total", "Qtd"),
            tipos.stream()
                .map(t -> List.of(
                    t.codigo(),
                    t.descricao(),
                    layoutHelper.formatCurrency(t.total()),
                    String.valueOf(t.qtdLancamentos())))
                .toList(),
            theme));

        Map<Long, List<BeneficioFuncionarioValorSnapshot>> top10 = model.top10PorTipo();
        if (top10 != null && !top10.isEmpty()) {
            document.add(new Paragraph("Drill-down — Top 10 por Tipo", layoutHelper.titleFont(theme)));
            for (BeneficioTipoResumoSnapshot tipo : tipos) {
                List<BeneficioFuncionarioValorSnapshot> funcionarios = top10.get(tipo.tipoBeneficioId());
                if (funcionarios == null || funcionarios.isEmpty()) {
                    continue;
                }
                document.add(new Paragraph(
                    tipo.codigo() + " — " + tipo.descricao(),
                    layoutHelper.bodyFont(theme)));
                document.add(layoutHelper.createZebraTable(
                    List.of("Funcionário", "Centro de Custo", "Valor"),
                    funcionarios.stream()
                        .map(f -> List.of(
                            f.funcionarioNome(),
                            formatCentroCusto(f),
                            layoutHelper.formatCurrency(f.valor())))
                        .toList(),
                    theme));
            }
        }

        document.add(new Paragraph("Matriz Centro de Custo × Tipo", layoutHelper.titleFont(theme)));
        List<BeneficioCcTipoSnapshot> matriz = model.matrizCcTipo() != null ? model.matrizCcTipo() : List.of();
        document.add(layoutHelper.createZebraTable(
            List.of("Centro de Custo", "Tipo", "Total"),
            matriz.stream()
                .map(m -> List.of(
                    m.centroCustoDescricao(),
                    m.tipoCodigo() + " — " + m.tipoDescricao(),
                    layoutHelper.formatCurrency(m.total())))
                .toList(),
            theme));
    }

    private String formatCentroCusto(BeneficioFuncionarioValorSnapshot funcionario) {
        if (funcionario.funcionarioId() == null) {
            return "";
        }
        String codigo = funcionario.centroCustoCodigo();
        String descricao = funcionario.centroCustoDescricao();
        if (codigo != null && descricao != null) {
            return codigo + " — " + descricao;
        }
        if (descricao != null) {
            return descricao;
        }
        if (codigo != null) {
            return codigo;
        }
        return "";
    }
}
