package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.domain.DatasetRowAuditAction;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetRowAudit;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceDatasetRowAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetAuditServiceTest {

    @Mock
    private WorkspaceDatasetRowAuditRepository auditRepository;

    @Mock
    private br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceDatasetRowRepository rowRepository;

    @Mock
    private DatasetService datasetService;

    @Mock
    private WorkspaceAccessGuard workspaceAccessGuard;

    private DatasetAuditService service;

    @BeforeEach
    void setUp() {
        service = new DatasetAuditService(auditRepository, rowRepository, datasetService, workspaceAccessGuard);
    }

    @Test
    void registrarCriacao_persisteCreate() {
        service.registrarCriacao(1L, 10L, Map.of("valor", 100));

        ArgumentCaptor<WorkspaceDatasetRowAudit> captor =
            ArgumentCaptor.forClass(WorkspaceDatasetRowAudit.class);
        verify(auditRepository).save(captor.capture());
        assertEquals(DatasetRowAuditAction.CREATE, captor.getValue().getAcao());
        assertEquals(10L, captor.getValue().getAutorUsuarioId());
        assertEquals(100, captor.getValue().getValoresNovos().get("valor"));
        assertNull(captor.getValue().getValoresAnteriores());
    }

    @Test
    void registrarAtualizacao_persisteUpdateComAntesEDepois() {
        service.registrarAtualizacao(2L, 11L, Map.of("valor", 50), Map.of("valor", 75));

        ArgumentCaptor<WorkspaceDatasetRowAudit> captor =
            ArgumentCaptor.forClass(WorkspaceDatasetRowAudit.class);
        verify(auditRepository).save(captor.capture());
        assertEquals(DatasetRowAuditAction.UPDATE, captor.getValue().getAcao());
        assertEquals(50, captor.getValue().getValoresAnteriores().get("valor"));
        assertEquals(75, captor.getValue().getValoresNovos().get("valor"));
    }

    @Test
    void registrarExclusao_persisteDelete() {
        service.registrarExclusao(3L, 12L, Map.of("valor", 99));

        ArgumentCaptor<WorkspaceDatasetRowAudit> captor =
            ArgumentCaptor.forClass(WorkspaceDatasetRowAudit.class);
        verify(auditRepository).save(captor.capture());
        assertEquals(DatasetRowAuditAction.DELETE, captor.getValue().getAcao());
        assertNull(captor.getValue().getValoresNovos());
    }

    @Test
    void listarHistorico_ordemCronologica() {
        WorkspaceDatasetRowAudit a1 = entry(1L, DatasetRowAuditAction.CREATE, LocalDateTime.of(2026, 1, 1, 10, 0));
        WorkspaceDatasetRowAudit a2 = entry(2L, DatasetRowAuditAction.UPDATE, LocalDateTime.of(2026, 1, 1, 11, 0));
        when(auditRepository.findByRowIdOrderByDataEventoAscIdAsc(5L)).thenReturn(List.of(a1, a2));

        var historico = service.listarHistorico(5L);

        assertEquals(2, historico.size());
        assertEquals(DatasetRowAuditAction.CREATE, historico.get(0).acao());
        assertEquals(DatasetRowAuditAction.UPDATE, historico.get(1).acao());
    }

    @Test
    void registrarCriacao_duasVezes_duasEntradas() {
        service.registrarCriacao(1L, 10L, Map.of("a", 1));
        service.registrarAtualizacao(1L, 10L, Map.of("a", 1), Map.of("a", 2));

        verify(auditRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void listarHistorico_vazio_retornaListaVazia() {
        when(auditRepository.findByRowIdOrderByDataEventoAscIdAsc(99L)).thenReturn(List.of());
        assertEquals(0, service.listarHistorico(99L).size());
    }

    @Test
    void registrarAtualizacao_preservaAutor() {
        service.registrarAtualizacao(4L, 99L, Map.of(), Map.of("x", "y"));

        ArgumentCaptor<WorkspaceDatasetRowAudit> captor =
            ArgumentCaptor.forClass(WorkspaceDatasetRowAudit.class);
        verify(auditRepository).save(captor.capture());
        assertEquals(99L, captor.getValue().getAutorUsuarioId());
    }

    @Test
    void listarHistorico_incluiValoresAnterioresENovos() {
        WorkspaceDatasetRowAudit audit = entry(10L, DatasetRowAuditAction.UPDATE, LocalDateTime.now());
        audit.setValoresAnteriores(Map.of("k", "old"));
        audit.setValoresNovos(Map.of("k", "new"));
        when(auditRepository.findByRowIdOrderByDataEventoAscIdAsc(7L)).thenReturn(List.of(audit));

        var historico = service.listarHistorico(7L);

        assertEquals("old", historico.get(0).valoresAnteriores().get("k"));
        assertEquals("new", historico.get(0).valoresNovos().get("k"));
    }

    private WorkspaceDatasetRowAudit entry(Long id, DatasetRowAuditAction acao, LocalDateTime when) {
        WorkspaceDatasetRowAudit e = new WorkspaceDatasetRowAudit();
        e.setId(id);
        e.setRowId(5L);
        e.setAutorUsuarioId(10L);
        e.setAcao(acao);
        e.setDataEvento(when);
        return e;
    }
}
