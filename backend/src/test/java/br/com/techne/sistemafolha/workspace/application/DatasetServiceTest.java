package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.api.CreateDatasetRequest;
import br.com.techne.sistemafolha.workspace.api.DatasetDTO;
import br.com.techne.sistemafolha.workspace.api.DatasetFieldSchemaDTO;
import br.com.techne.sistemafolha.workspace.api.DatasetSummaryDTO;
import br.com.techne.sistemafolha.workspace.api.UpdateDatasetSchemaRequest;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldSchema;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldType;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDataset;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetConflictException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetRow;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceLimits;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceQuotaExceededException;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceDatasetRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetServiceTest {

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 10L;
    private static final Long DATASET_ID = 1L;

    @Mock
    private WorkspaceAccessGuard workspaceAccessGuard;

    @Mock
    private WorkspaceDatasetRepository datasetRepository;

    @Mock
    private WorkspaceDatasetRowRepository rowRepository;

    private DatasetService datasetService;

    @BeforeEach
    void setUp() {
        datasetService = new DatasetService(
            workspaceAccessGuard,
            datasetRepository,
            rowRepository,
            new DatasetQuotaPolicy());
    }

    @Test
    void criar_persisteDatasetComSchema() {
        stubAcesso();
        when(datasetRepository.countByUsuarioId(USUARIO_ID)).thenReturn(0L);
        when(datasetRepository.save(any())).thenAnswer(inv -> {
            WorkspaceDataset ds = inv.getArgument(0);
            ds.setId(DATASET_ID);
            return ds;
        });
        when(rowRepository.countByDatasetId(DATASET_ID)).thenReturn(0L);

        CreateDatasetRequest request = new CreateDatasetRequest("Orçamento", List.of(
            new DatasetFieldSchemaDTO("valor", DatasetFieldType.MOEDA, null, true)));

        DatasetDTO result = datasetService.criar(LOGIN, request);

        assertEquals(DATASET_ID, result.id());
        assertEquals("Orçamento", result.nome());
        assertEquals(1, result.campos().size());
        assertEquals(1, result.schemaVersion());
    }

    @Test
    void criar_quotaDatasetExcedida_lanca400() {
        stubAcesso();
        when(datasetRepository.countByUsuarioId(USUARIO_ID))
            .thenReturn((long) WorkspaceLimits.MAX_DATASETS_PER_USER);

        CreateDatasetRequest request = new CreateDatasetRequest("Novo", List.of(
            new DatasetFieldSchemaDTO("campo", DatasetFieldType.TEXTO, null, false)));

        WorkspaceQuotaExceededException ex = assertThrows(WorkspaceQuotaExceededException.class,
            () -> datasetService.criar(LOGIN, request));
        assertTrue(ex.getMessage().contains(
            String.format("Limite de %d datasets", WorkspaceLimits.MAX_DATASETS_PER_USER)));
    }

    @Test
    void criar_quotaCamposExcedida_lanca400() {
        stubAcesso();
        when(datasetRepository.countByUsuarioId(USUARIO_ID)).thenReturn(0L);
        List<DatasetFieldSchemaDTO> campos = new ArrayList<>();
        for (int i = 0; i <= WorkspaceLimits.MAX_FIELDS_PER_DATASET; i++) {
            campos.add(new DatasetFieldSchemaDTO("campo" + i, DatasetFieldType.TEXTO, null, false));
        }

        assertThrows(WorkspaceQuotaExceededException.class, () ->
            datasetService.criar(LOGIN, new CreateDatasetRequest("Grande", campos)));
    }

    @Test
    void criar_semEscopo_lanca403() {
        doThrow(new WorkspaceAcessoNegadoException()).when(workspaceAccessGuard).assertEscopo(LOGIN);

        assertThrows(WorkspaceAcessoNegadoException.class, () ->
            datasetService.criar(LOGIN, new CreateDatasetRequest("X", List.of(
                new DatasetFieldSchemaDTO("a", DatasetFieldType.TEXTO, null, false)))));
    }

    @Test
    void listar_retornaResumosDoUsuario() {
        stubAcesso();
        WorkspaceDataset dataset = datasetComSchema("Planilha");
        when(datasetRepository.findByUsuarioIdOrderByNomeAsc(USUARIO_ID)).thenReturn(List.of(dataset));
        when(rowRepository.countByDatasetId(DATASET_ID)).thenReturn(3L);

        List<DatasetSummaryDTO> result = datasetService.listar(LOGIN);

        assertEquals(1, result.size());
        assertEquals("Planilha", result.get(0).nome());
        assertEquals(3L, result.get(0).totalLinhas());
    }

    @Test
    void obter_datasetDeOutroUsuario_lanca404() {
        stubAcesso();
        when(datasetRepository.findByUsuarioIdAndId(USUARIO_ID, DATASET_ID)).thenReturn(Optional.empty());

        assertThrows(WorkspaceDatasetNotFoundException.class, () -> datasetService.obter(LOGIN, DATASET_ID));
    }

    @Test
    void atualizarSchema_versaoStale_lanca409() {
        stubAcesso();
        when(datasetRepository.findByUsuarioIdAndId(USUARIO_ID, DATASET_ID))
            .thenReturn(Optional.of(datasetComSchema("Atual")));

        UpdateDatasetSchemaRequest request = new UpdateDatasetSchemaRequest(
            List.of(new DatasetFieldSchemaDTO("valor", DatasetFieldType.MOEDA, null, true)),
            99,
            false);

        assertThrows(WorkspaceDatasetConflictException.class,
            () -> datasetService.atualizarSchema(LOGIN, DATASET_ID, request));
    }

    @Test
    void atualizarSchema_removeCampoComDadosSemConfirmacao_lanca409() {
        stubAcesso();
        WorkspaceDataset dataset = datasetComSchema("Com dados");
        dataset.setSchema(List.of(
            new DatasetFieldSchema("valor", DatasetFieldType.MOEDA, null, true),
            new DatasetFieldSchema("descricao", DatasetFieldType.TEXTO, null, false)));
        when(datasetRepository.findByUsuarioIdAndId(USUARIO_ID, DATASET_ID)).thenReturn(Optional.of(dataset));
        when(rowRepository.findByDatasetIdOrderByOrdemAscIdAsc(DATASET_ID))
            .thenReturn(List.of(rowComValor()));

        UpdateDatasetSchemaRequest request = new UpdateDatasetSchemaRequest(
            List.of(new DatasetFieldSchemaDTO("descricao", DatasetFieldType.TEXTO, null, false)),
            1,
            false);

        assertThrows(WorkspaceDatasetConflictException.class,
            () -> datasetService.atualizarSchema(LOGIN, DATASET_ID, request));
    }

    @Test
    void atualizarSchema_removeCampoComDadosComConfirmacao_incrementaVersao() {
        stubAcesso();
        WorkspaceDataset dataset = datasetComSchema("Com dados");
        dataset.setSchema(List.of(
            new DatasetFieldSchema("valor", DatasetFieldType.MOEDA, null, true),
            new DatasetFieldSchema("descricao", DatasetFieldType.TEXTO, null, false)));
        when(datasetRepository.findByUsuarioIdAndId(USUARIO_ID, DATASET_ID)).thenReturn(Optional.of(dataset));
        when(rowRepository.findByDatasetIdOrderByOrdemAscIdAsc(DATASET_ID)).thenReturn(List.of(rowComValor()));
        when(datasetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rowRepository.countByDatasetId(DATASET_ID)).thenReturn(1L);

        DatasetDTO result = datasetService.atualizarSchema(LOGIN, DATASET_ID, new UpdateDatasetSchemaRequest(
            List.of(new DatasetFieldSchemaDTO("descricao", DatasetFieldType.TEXTO, null, false)),
            1,
            true));

        assertEquals(2, result.schemaVersion());
        assertEquals(1, result.campos().size());
        assertEquals("descricao", result.campos().get(0).nome());
    }

    @Test
    void atualizarSchema_adicionaCampo_incrementaVersao() {
        stubAcesso();
        when(datasetRepository.findByUsuarioIdAndId(USUARIO_ID, DATASET_ID))
            .thenReturn(Optional.of(datasetComSchema("Expandir")));
        when(datasetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rowRepository.countByDatasetId(DATASET_ID)).thenReturn(0L);

        DatasetDTO result = datasetService.atualizarSchema(LOGIN, DATASET_ID, new UpdateDatasetSchemaRequest(
            List.of(
                new DatasetFieldSchemaDTO("valor", DatasetFieldType.MOEDA, null, true),
                new DatasetFieldSchemaDTO("obs", DatasetFieldType.TEXTO, null, false)),
            1,
            false));

        assertEquals(2, result.schemaVersion());
        assertEquals(2, result.campos().size());
    }

    @Test
    void excluir_removeDataset() {
        stubAcesso();
        WorkspaceDataset dataset = datasetComSchema("Excluir");
        when(datasetRepository.findByUsuarioIdAndId(USUARIO_ID, DATASET_ID)).thenReturn(Optional.of(dataset));

        datasetService.excluir(LOGIN, DATASET_ID);

        verify(datasetRepository).delete(dataset);
    }

    @Test
    void criar_nomeCampoDuplicado_lanca400() {
        stubAcesso();
        when(datasetRepository.countByUsuarioId(USUARIO_ID)).thenReturn(0L);

        assertThrows(IllegalArgumentException.class, () -> datasetService.criar(LOGIN, new CreateDatasetRequest(
            "Dup",
            List.of(
                new DatasetFieldSchemaDTO("a", DatasetFieldType.TEXTO, null, false),
                new DatasetFieldSchemaDTO("a", DatasetFieldType.NUMERO, null, false)))));
        verify(datasetRepository, never()).save(any());
    }

    private void stubAcesso() {
        when(workspaceAccessGuard.resolve(LOGIN))
            .thenReturn(new WorkspaceAccessGuard.ResolvedWorkspaceAccess(false, USUARIO_ID, null, null));
    }

    private WorkspaceDataset datasetComSchema(String nome) {
        WorkspaceDataset dataset = new WorkspaceDataset();
        dataset.setId(DATASET_ID);
        dataset.setUsuarioId(USUARIO_ID);
        dataset.setNome(nome);
        dataset.setSchemaVersion(1);
        dataset.setSchema(new ArrayList<>(List.of(
            new DatasetFieldSchema("valor", DatasetFieldType.MOEDA, null, true))));
        return dataset;
    }

    private WorkspaceDatasetRow rowComValor() {
        WorkspaceDatasetRow row = new WorkspaceDatasetRow();
        row.setValores(Map.of("valor", "100.00", "descricao", "Item A"));
        return row;
    }
}
