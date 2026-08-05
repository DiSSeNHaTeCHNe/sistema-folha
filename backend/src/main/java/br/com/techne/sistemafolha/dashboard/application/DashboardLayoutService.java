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
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardLayoutService {

    static final int MAX_WIDGETS = LayoutValidator.MAX_WIDGETS;

    private static final Logger logger = LoggerFactory.getLogger(DashboardLayoutService.class);
    private static final String DOMAIN_PREFIX = DomainLogging.prefix("dashboard");

    private final DashboardLayoutRepository dashboardLayoutRepository;
    private final DashboardAccessGuard dashboardAccessGuard;
    private final DashboardWidgetCatalogService dashboardWidgetCatalogService;
    private final DashboardWidgetConfigValidator dashboardWidgetConfigValidator;
    private final LayoutValidator layoutValidator;

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
        DashboardAccessGuard.ResolvedDashboardAccess access = dashboardAccessGuard.resolve(login);
        Long usuarioId = access.usuarioId();
        Set<String> widgetIdsValidos = dashboardWidgetCatalogService.widgetIdsValidos(login);

        validarWidgets(dto.widgets(), widgetIdsValidos, access);

        List<WidgetInstancePayload> normalizados = layoutValidator.normalizarOrdem(toPayloads(dto.widgets()));
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

    private void validarWidgets(
            List<WidgetInstanceDTO> widgets,
            Set<String> widgetIdsValidos,
            DashboardAccessGuard.ResolvedDashboardAccess access) {
        if (widgets == null) {
            throw new IllegalArgumentException("widgets: obrigatório");
        }
        layoutValidator.validarQuantidade(widgets.size());
        Set<String> instanceIds = layoutValidator.novoConjuntoInstanceIds();
        for (WidgetInstanceDTO widget : widgets) {
            if (!widgetIdsValidos.contains(widget.widgetId())) {
                logger.warn("{}widgetId inválido rejeitado: {}", DOMAIN_PREFIX, widget.widgetId());
                throw new IllegalArgumentException("widgetId inválido: " + widget.widgetId());
            }
            layoutValidator.validarColSpan(widget.colSpan());
            layoutValidator.validarRowSpan(widget.rowSpan());
            layoutValidator.validarOrdem(widget.ordem());
            layoutValidator.validarInstanceIdUnico(instanceIds, widget.instanceId());
            dashboardWidgetConfigValidator.validar(widget.widgetId(), widget.config(), access);
        }
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
