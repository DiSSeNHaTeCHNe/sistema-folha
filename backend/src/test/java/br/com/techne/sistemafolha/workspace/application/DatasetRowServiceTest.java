package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.api.DatasetRowRequest;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldSchema;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldType;
import br.com.techne.sistemafolha.workspace.domain.DatasetRowValidationException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDataset;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetRow;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetRowNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceLimits;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceQuotaExceededException;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceDatasetRowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetRowServiceTest {

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 10L;
    private static final Long DATASET_ID = 1L;

    @Mock
    private WorkspaceAccessGuard workspaceAccessGuard;

    @Mock
    private DatasetService datasetService;

    @Mock
    private WorkspaceDatasetRowRepository rowRepository;

    private DatasetRowService rowService;

    @BeforeEach
    void setUp() {
        rowService = new DatasetRowService(
            workspaceAccessGuard,
            datasetService,
            rowRepository,
            new DatasetQuotaPolicy()
        );
    }

    @Test
    void adicionarLinha_valoresValidos_persiste() {
        stubAcesso();
        stubDataset();
        when(rowRepository.countByDatasetId(DATASET_ID)).thenReturn(0L);
        when(rowRepository.save(any())).thenAnswer(inv -> {
            WorkspaceDatasetRow row = inv.getArgument(0);
            row.setId(100L);
            return row;
        });

        var result = rowService.adicionarLinha(LOGIN, DATASET_ID,
            new DatasetRowRequest(Map.of("quantidade", 10)));

        assertEquals(100L, result.id());
        assertEquals(DATASET_ID, result.datasetId());
        assertEquals(10, result.valores().get("quantidade"));
    }

    @Test
    void adicionarLinha_tipoInvalido_lanca400() {
        stubAcesso();
        stubDataset();

        assertThrows(DatasetRowValidationException.class, () ->
            rowService.adicionarLinha(LOGIN, DATASET_ID,
                new DatasetRowRequest(Map.of("quantidade", "abc"))));
    }

    @Test
    void adicionarLinha_quotaExcedida_lancaQuota() {
        stubAcesso();
        stubDataset();
        when(rowRepository.countByDatasetId(DATASET_ID))
            .thenReturn((long) WorkspaceLimits.MAX_ROWS_PER_DATASET);

        assertThrows(WorkspaceQuotaExceededException.class, () ->
            rowService.adicionarLinha(LOGIN, DATASET_ID,
                new DatasetRowRequest(Map.of("quantidade", 1))));
    }

    @Test
    void adicionarLinha_datasetDeOutroUsuario_lanca404() {
        stubAcesso();
        when(datasetService.findOwnedDataset(USUARIO_ID, DATASET_ID))
            .thenThrow(new WorkspaceDatasetNotFoundException(DATASET_ID));

        assertThrows(WorkspaceDatasetNotFoundException.class, () ->
            rowService.adicionarLinha(LOGIN, DATASET_ID,
                new DatasetRowRequest(Map.of("quantidade", 1))));
    }

    @Test
    void adicionarLinha_semEscopo_lanca403() {
        org.mockito.Mockito.doThrow(new WorkspaceAcessoNegadoException())
            .when(workspaceAccessGuard).assertEscopo(LOGIN);

        assertThrows(WorkspaceAcessoNegadoException.class, () ->
            rowService.adicionarLinha(LOGIN, DATASET_ID,
                new DatasetRowRequest(Map.of("quantidade", 1))));
    }

    @Test
    void atualizarLinha_valoresValidos_atualiza() {
        stubAcesso();
        stubDataset();
        WorkspaceDatasetRow row = sampleRow();
        when(rowRepository.findByDatasetIdAndId(DATASET_ID, 100L)).thenReturn(Optional.of(row));
        when(rowRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = rowService.atualizarLinha(LOGIN, DATASET_ID, 100L,
            new DatasetRowRequest(Map.of("quantidade", 20)));

        assertEquals(20, result.valores().get("quantidade"));
    }

    @Test
    void atualizarLinha_linhaInexistente_lanca404() {
        stubAcesso();
        stubDataset();
        when(rowRepository.findByDatasetIdAndId(DATASET_ID, 999L)).thenReturn(Optional.empty());

        assertThrows(WorkspaceDatasetRowNotFoundException.class, () ->
            rowService.atualizarLinha(LOGIN, DATASET_ID, 999L,
                new DatasetRowRequest(Map.of("quantidade", 1))));
    }

    @Test
    void removerLinha_existente_remove() {
        stubAcesso();
        stubDataset();
        WorkspaceDatasetRow row = sampleRow();
        when(rowRepository.findByDatasetIdAndId(DATASET_ID, 100L)).thenReturn(Optional.of(row));

        rowService.removerLinha(LOGIN, DATASET_ID, 100L);

        verify(rowRepository).delete(row);
    }

    @Test
    void listarLinhas_retornaOrdenadas() {
        stubAcesso();
        stubDataset();
        when(rowRepository.findByDatasetIdOrderByOrdemAscIdAsc(DATASET_ID))
            .thenReturn(List.of(sampleRow()));

        var result = rowService.listarLinhas(LOGIN, DATASET_ID);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).id());
    }

    @Test
    void obterLinha_existente_retornaDto() {
        stubAcesso();
        stubDataset();
        when(rowRepository.findByDatasetIdAndId(DATASET_ID, 100L)).thenReturn(Optional.of(sampleRow()));

        var result = rowService.obterLinha(LOGIN, DATASET_ID, 100L);

        assertEquals(100L, result.id());
    }

    @Test
    void obterLinha_inexistente_lanca404() {
        stubAcesso();
        stubDataset();
        when(rowRepository.findByDatasetIdAndId(DATASET_ID, 100L)).thenReturn(Optional.empty());

        assertThrows(WorkspaceDatasetRowNotFoundException.class, () ->
            rowService.obterLinha(LOGIN, DATASET_ID, 100L));
        verify(rowRepository, never()).delete(any());
    }

    private void stubAcesso() {
        when(workspaceAccessGuard.resolve(LOGIN)).thenReturn(
            new WorkspaceAccessGuard.ResolvedWorkspaceAccess(false, USUARIO_ID, null, null));
    }

    private void stubDataset() {
        when(datasetService.findOwnedDataset(USUARIO_ID, DATASET_ID)).thenReturn(sampleDataset());
    }

    private WorkspaceDataset sampleDataset() {
        WorkspaceDataset ds = new WorkspaceDataset();
        ds.setId(DATASET_ID);
        ds.setUsuarioId(USUARIO_ID);
        ds.setSchema(new ArrayList<>(List.of(
            new DatasetFieldSchema("quantidade", DatasetFieldType.NUMERO, null, true)
        )));
        return ds;
    }

    private WorkspaceDatasetRow sampleRow() {
        WorkspaceDatasetRow row = new WorkspaceDatasetRow();
        row.setId(100L);
        row.setDatasetId(DATASET_ID);
        row.setValores(Map.of("quantidade", 10));
        row.setOrdem(0);
        return row;
    }
}
