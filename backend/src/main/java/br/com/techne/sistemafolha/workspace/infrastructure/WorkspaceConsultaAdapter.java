package br.com.techne.sistemafolha.workspace.infrastructure;

import br.com.techne.sistemafolha.workspace.api.SystemFieldDescriptorDTO;
import br.com.techne.sistemafolha.workspace.api.TemplateCatalogItemDTO;
import br.com.techne.sistemafolha.workspace.application.SystemFieldCatalog;
import br.com.techne.sistemafolha.workspace.application.TemplatePublishService;
import br.com.techne.sistemafolha.workspace.domain.formula.AvailableField;
import br.com.techne.sistemafolha.workspace.port.WorkspaceConsultaPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WorkspaceConsultaAdapter implements WorkspaceConsultaPort {

    private static final Set<String> FONTES_SISTEMA = Set.of("FOLHA", "BENEFICIO", "ORCAMENTO");

    private final TemplatePublishService templatePublishService;
    private final SystemFieldCatalog systemFieldCatalog;

    @Override
    public List<TemplateCatalogItemDTO> listarTemplatesVisiveis(String login) {
        return templatePublishService.listarCatalogo(login);
    }

    @Override
    public List<SystemFieldDescriptorDTO> listarCamposSistema(String login) {
        templatePublishService.listarCatalogo(login);
        List<SystemFieldDescriptorDTO> campos = new ArrayList<>();
        for (String fonte : FONTES_SISTEMA) {
            List<AvailableField> fields = systemFieldCatalog.fieldsForSource(fonte);
            for (AvailableField field : fields) {
                campos.add(new SystemFieldDescriptorDTO(fonte, field.name(), "NUMERO"));
            }
        }
        return campos;
    }
}
