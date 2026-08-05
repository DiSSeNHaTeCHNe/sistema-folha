package br.com.techne.sistemafolha.workspace.port;

import br.com.techne.sistemafolha.workspace.api.SystemFieldDescriptorDTO;
import br.com.techne.sistemafolha.workspace.api.TemplateCatalogItemDTO;

import java.util.List;

public interface WorkspaceConsultaPort {

    List<TemplateCatalogItemDTO> listarTemplatesVisiveis(String login);

    List<SystemFieldDescriptorDTO> listarCamposSistema(String login);
}
