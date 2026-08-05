package br.com.techne.sistemafolha.workspace.infrastructure;

import br.com.techne.sistemafolha.workspace.api.TemplateCatalogItemDTO;
import br.com.techne.sistemafolha.workspace.application.SystemFieldCatalog;
import br.com.techne.sistemafolha.workspace.application.TemplatePublishService;
import br.com.techne.sistemafolha.workspace.domain.TemplateTipo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceConsultaAdapterTest {

    private static final String LOGIN = "gestor";

    @Mock private TemplatePublishService templatePublishService;

    private WorkspaceConsultaAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new WorkspaceConsultaAdapter(templatePublishService, new SystemFieldCatalog());
    }

    @Test
    void listarTemplatesVisiveis_delegaAoPublishService() {
        when(templatePublishService.listarCatalogo(LOGIN)).thenReturn(List.of(
            new TemplateCatalogItemDTO(1L, "Tpl", TemplateTipo.DATASET, 1, 1, false, 2L, null, null)
        ));

        List<TemplateCatalogItemDTO> result = adapter.listarTemplatesVisiveis(LOGIN);

        assertEquals(1, result.size());
        verify(templatePublishService).listarCatalogo(LOGIN);
    }

    @Test
    void listarCamposSistema_retornaCamposDasFontesConhecidas() {
        when(templatePublishService.listarCatalogo(LOGIN)).thenReturn(List.of());

        var result = adapter.listarCamposSistema(LOGIN);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(c -> "FOLHA".equals(c.fonte())));
        assertTrue(result.stream().anyMatch(c -> "total_liquido".equals(c.nome())));
        verify(templatePublishService).listarCatalogo(LOGIN);
    }

    @Test
    void listarCamposSistema_incluiOrcamento() {
        when(templatePublishService.listarCatalogo(LOGIN)).thenReturn(List.of());

        var result = adapter.listarCamposSistema(LOGIN);

        assertTrue(result.stream().anyMatch(c -> "ORCAMENTO".equals(c.fonte()) && "orcado".equals(c.nome())));
    }
}
