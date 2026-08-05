package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.api.DatasetFieldSchemaDTO;
import br.com.techne.sistemafolha.workspace.api.TemplateCatalogItemDTO;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldSchema;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldType;
import br.com.techne.sistemafolha.workspace.domain.ProposalPayload;
import br.com.techne.sistemafolha.workspace.domain.ReferenciaEntidade;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceKind;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceRef;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class ProposalContentBuilder {

    public Optional<TemplateCatalogItemDTO> buscarTemplateSimilar(
            String descricaoNatural, List<TemplateCatalogItemDTO> catalogo) {
        if (descricaoNatural == null || descricaoNatural.isBlank() || catalogo.isEmpty()) {
            return Optional.empty();
        }
        String normalized = descricaoNatural.toLowerCase(Locale.ROOT);
        for (TemplateCatalogItemDTO item : catalogo) {
            String nome = item.nome().toLowerCase(Locale.ROOT);
            if (normalized.contains(nome) || nomeContainsSignificantWord(nome, normalized)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    public ProposalPayload montarDeDescricao(String tipo, String descricaoNatural) {
        ProposalPayload payload = new ProposalPayload();
        String descricao = descricaoNatural != null ? descricaoNatural.trim() : "";
        if ("WIDGET".equalsIgnoreCase(tipo)) {
            payload.setKind("WIDGET");
            payload.setNome(extrairNome(descricao, "Widget sugerido"));
            payload.setTipoWidget("KPI");
            payload.setFontes(List.of(new WidgetSourceRef(WidgetSourceKind.SISTEMA, "FOLHA")));
            payload.setFormula("SOMA(total_liquido)");
            payload.setDescricao(descricao);
        } else {
            payload.setKind("DATASET");
            payload.setNome(extrairNome(descricao, "Dataset sugerido"));
            payload.setCampos(inferirCampos(descricao));
            payload.setDescricao(descricao);
        }
        payload.setDedupHash(calcularHash(payload));
        return payload;
    }

    public ProposalPayload montarSugestao(
            List<String> nomesDatasets,
            List<TemplateCatalogItemDTO> catalogo) {
        ProposalPayload payload = new ProposalPayload();
        boolean temOrcamento = nomesDatasets.stream()
            .anyMatch(n -> n.toLowerCase(Locale.ROOT).contains("orcamento")
                || n.toLowerCase(Locale.ROOT).contains("orçamento"));

        if (temOrcamento) {
            payload.setKind("WIDGET");
            payload.setNome("Variação mês a mês");
            payload.setTipoWidget("KPI");
            payload.setFontes(List.of(new WidgetSourceRef(WidgetSourceKind.SISTEMA, "ORCAMENTO")));
            payload.setFormula("SE(MÉDIA(realizado)=0, 0, (SOMA(orcado)-SOMA(realizado))/SOMA(realizado)*100)");
            payload.setDescricao("Widget KPI sugerido com base no dataset de orçamento existente");
        } else if (!catalogo.isEmpty()) {
            TemplateCatalogItemDTO template = catalogo.get(0);
            payload.setKind("TEMPLATE_INSTALL");
            payload.setTemplateId(template.id());
            payload.setNome(template.nome());
            payload.setDescricao("Instalar template \"" + template.nome() + "\" do catálogo");
        } else {
            payload.setKind("WIDGET");
            payload.setNome("Resumo folha");
            payload.setTipoWidget("KPI");
            payload.setFontes(List.of(new WidgetSourceRef(WidgetSourceKind.SISTEMA, "FOLHA")));
            payload.setFormula("SOMA(total_liquido)");
            payload.setDescricao("Widget KPI sugerido com base no seu workspace");
        }
        payload.setDedupHash(calcularHash(payload));
        return payload;
    }

    public ProposalPayload montarInstalacaoTemplate(TemplateCatalogItemDTO template, String descricao) {
        ProposalPayload payload = new ProposalPayload();
        payload.setKind("TEMPLATE_INSTALL");
        payload.setTemplateId(template.id());
        payload.setNome(template.nome());
        payload.setDescricao(descricao != null ? descricao : "Instalar template \"" + template.nome() + "\"");
        payload.setDedupHash(calcularHash(payload));
        return payload;
    }

    public List<DatasetFieldSchemaDTO> toFieldDtos(List<DatasetFieldSchema> campos) {
        return campos.stream()
            .map(c -> new DatasetFieldSchemaDTO(
                c.nome(), c.tipo(), c.referenciaEntidade(), c.obrigatorio()))
            .toList();
    }

    public String calcularHash(ProposalPayload payload) {
        String raw = payload.getKind() + "|"
            + nullSafe(payload.getNome()) + "|"
            + nullSafe(payload.getTemplateId()) + "|"
            + nullSafe(payload.getTipoWidget()) + "|"
            + nullSafe(payload.getFormula());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private boolean nomeContainsSignificantWord(String nomeTemplate, String descricao) {
        for (String word : nomeTemplate.split("\\s+")) {
            if (word.length() >= 4 && descricao.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String extrairNome(String descricao, String fallback) {
        if (descricao.isBlank()) {
            return fallback;
        }
        String trimmed = descricao.length() > 120 ? descricao.substring(0, 120) : descricao;
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1);
    }

    private List<DatasetFieldSchema> inferirCampos(String descricao) {
        List<DatasetFieldSchema> campos = new ArrayList<>();
        String lower = descricao.toLowerCase(Locale.ROOT);
        if (lower.contains("competencia") || lower.contains("competência") || lower.contains("data")) {
            campos.add(campo("competencia", DatasetFieldType.DATA, null));
        }
        if (lower.contains("cargo")) {
            campos.add(campo("cargo", DatasetFieldType.REFERENCIA, ReferenciaEntidade.CARGO));
        }
        if (lower.contains("centro") || lower.contains("cc")) {
            campos.add(campo("centro_custo_id", DatasetFieldType.REFERENCIA, ReferenciaEntidade.CENTRO_CUSTO));
        }
        if (lower.contains("quantidade") || lower.contains("qtd")) {
            campos.add(campo("quantidade", DatasetFieldType.NUMERO, null));
        }
        if (lower.contains("valor") || lower.contains("orçado") || lower.contains("orcado")) {
            campos.add(campo("valor", DatasetFieldType.MOEDA, null));
        }
        if (campos.isEmpty()) {
            campos.add(campo("descricao", DatasetFieldType.TEXTO, null));
        }
        return campos;
    }

    private DatasetFieldSchema campo(String nome, DatasetFieldType tipo, ReferenciaEntidade ref) {
        return new DatasetFieldSchema(nome, tipo, ref, true);
    }

    private String nullSafe(Object value) {
        return value != null ? value.toString() : "";
    }
}
