package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.dashboard.api.DashboardLayoutDTO;
import br.com.techne.sistemafolha.dashboard.api.WidgetInstanceDTO;
import br.com.techne.sistemafolha.dashboard.domain.DashboardLayout;
import br.com.techne.sistemafolha.dashboard.domain.WidgetCatalog;
import br.com.techne.sistemafolha.dashboard.domain.WidgetInstancePayload;
import br.com.techne.sistemafolha.dashboard.infrastructure.DashboardLayoutRepository;
import br.com.techne.sistemafolha.shared.logging.DomainLogging;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DashboardLayoutService {

    static final int MAX_WIDGETS = 30;

    private static final Logger logger = LoggerFactory.getLogger(DashboardLayoutService.class);
    private static final String DOMAIN_PREFIX = DomainLogging.prefix("dashboard");

    private final DashboardLayoutRepository dashboardLayoutRepository;
    private final DashboardAccessGuard dashboardAccessGuard;
    private final DashboardWidgetCatalogService dashboardWidgetCatalogService;

    @Transactional
    public DashboardLayoutDTO obterOuCriarPadrao(String login) {
        dashboardAccessGuard.assertEscopo(login);
        Long usuarioId = dashboardAccessGuard.resolve(login).usuarioId();

        return dashboardLayoutRepository.findByUsuarioId(usuarioId)
            .map(this::toDtoFiltrado)
            .orElseGet(() -> {
                DashboardLayout layout = criarLayoutPadrao(usuarioId);
                dashboardLayoutRepository.save(layout);
                logger.info("{}Layout padrão criado para usuarioId={}", DOMAIN_PREFIX, usuarioId);
                return toDtoFiltrado(layout);
            });
    }

    @Transactional
    public DashboardLayoutDTO salvar(String login, DashboardLayoutDTO dto) {
        dashboardAccessGuard.assertEscopo(login);
        Long usuarioId = dashboardAccessGuard.resolve(login).usuarioId();
        Set<String> widgetIdsValidos = dashboardWidgetCatalogService.widgetIdsValidos(login);

        validarWidgets(dto.widgets(), widgetIdsValidos);

        List<WidgetInstancePayload> normalizados = normalizarOrdem(toPayloads(dto.widgets()));
        DashboardLayout layout = dashboardLayoutRepository.findByUsuarioId(usuarioId)
            .orElseGet(() -> {
                DashboardLayout novo = new DashboardLayout();
                novo.setUsuarioId(usuarioId);
                return novo;
            });

        if (dto.nome() != null && !dto.nome().isBlank()) {
            layout.setNome(dto.nome());
        }
        layout.setWidgets(new ArrayList<>(normalizados));
        DashboardLayout salvo = dashboardLayoutRepository.save(layout);
        logger.info("{}Layout gravado para usuarioId={}, widgets={}", DOMAIN_PREFIX, usuarioId, normalizados.size());
        return toDtoFiltrado(salvo);
    }

    @Transactional
    public void restaurarPadrao(String login) {
        dashboardAccessGuard.assertEscopo(login);
        Long usuarioId = dashboardAccessGuard.resolve(login).usuarioId();
        dashboardLayoutRepository.deleteByUsuarioId(usuarioId);
        logger.info("{}Layout resetado para usuarioId={}", DOMAIN_PREFIX, usuarioId);
    }

    List<WidgetInstancePayload> criarWidgetsPadrao() {
        List<WidgetInstancePayload> widgets = new ArrayList<>();
        int ordem = 0;
        for (WidgetCatalog entry : WidgetCatalog.values()) {
            if (!entry.noLayoutPadrao()) {
                continue;
            }
            widgets.add(new WidgetInstancePayload(
                entry.widgetId(),
                gerarInstanceId(),
                ordem++,
                entry.colSpanPadrao(),
                entry.rowSpanPadrao(),
                null
            ));
        }
        return widgets;
    }

    private DashboardLayout criarLayoutPadrao(Long usuarioId) {
        DashboardLayout layout = new DashboardLayout();
        layout.setUsuarioId(usuarioId);
        layout.setWidgets(new ArrayList<>(criarWidgetsPadrao()));
        return layout;
    }

    private void validarWidgets(List<WidgetInstanceDTO> widgets, Set<String> widgetIdsValidos) {
        if (widgets == null) {
            throw new IllegalArgumentException("widgets: obrigatório");
        }
        if (widgets.size() > MAX_WIDGETS) {
            throw new IllegalArgumentException("Limite de 30 widgets atingido");
        }
        Set<String> instanceIds = new HashSet<>();
        for (WidgetInstanceDTO widget : widgets) {
            if (!widgetIdsValidos.contains(widget.widgetId())) {
                logger.warn("{}widgetId inválido rejeitado: {}", DOMAIN_PREFIX, widget.widgetId());
                throw new IllegalArgumentException("widgetId inválido: " + widget.widgetId());
            }
            if (widget.colSpan() != null && widget.colSpan() > 12) {
                throw new IllegalArgumentException("colSpan deve estar entre 1 e 12");
            }
            if (widget.rowSpan() != null && (widget.rowSpan() < 1 || widget.rowSpan() > 3)) {
                throw new IllegalArgumentException("rowSpan deve estar entre 1 e 3");
            }
            if (widget.ordem() != null && widget.ordem() < 0) {
                throw new IllegalArgumentException("ordem não pode ser negativa");
            }
            if (!instanceIds.add(widget.instanceId())) {
                throw new IllegalArgumentException("instanceId duplicado: " + widget.instanceId());
            }
        }
    }

    private List<WidgetInstancePayload> normalizarOrdem(List<WidgetInstancePayload> widgets) {
        List<WidgetInstancePayload> sorted = widgets.stream()
            .sorted(Comparator.comparingInt(w -> w.ordem() != null ? w.ordem() : 0))
            .toList();
        return IntStream.range(0, sorted.size())
            .mapToObj(i -> new WidgetInstancePayload(
                sorted.get(i).widgetId(),
                sorted.get(i).instanceId(),
                i,
                sorted.get(i).colSpan(),
                sorted.get(i).rowSpan(),
                sorted.get(i).config()
            ))
            .toList();
    }

    private List<WidgetInstancePayload> toPayloads(List<WidgetInstanceDTO> widgets) {
        return widgets.stream()
            .map(w -> new WidgetInstancePayload(
                w.widgetId(), w.instanceId(), w.ordem(), w.colSpan(), w.rowSpan(), w.config()))
            .toList();
    }

    private DashboardLayoutDTO toDtoFiltrado(DashboardLayout layout) {
        Set<String> idsConhecidos = WidgetCatalog.allWidgetIds();
        List<WidgetInstanceDTO> widgets = layout.getWidgets().stream()
            .filter(w -> idsConhecidos.contains(w.widgetId()))
            .sorted(Comparator.comparingInt(w -> w.ordem() != null ? w.ordem() : 0))
            .map(w -> new WidgetInstanceDTO(
                w.widgetId(), w.instanceId(), w.ordem(), w.colSpan(), w.rowSpan(), w.config()))
            .toList();
        return new DashboardLayoutDTO(layout.getId(), layout.getNome(), widgets);
    }

    private String gerarInstanceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
