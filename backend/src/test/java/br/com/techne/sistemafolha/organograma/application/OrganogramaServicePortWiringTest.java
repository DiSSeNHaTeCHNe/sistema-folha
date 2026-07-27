package br.com.techne.sistemafolha.organograma.application;

import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.cadastros.port.CadastrosLookupPort;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioConsultaPort;
import br.com.techne.sistemafolha.organograma.api.FuncionarioOrganogramaDTO;
import br.com.techne.sistemafolha.organograma.domain.FuncionarioOrganograma;
import br.com.techne.sistemafolha.organograma.domain.NoOrganograma;
import br.com.techne.sistemafolha.organograma.infrastructure.CentroCustoOrganogramaRepository;
import br.com.techne.sistemafolha.organograma.infrastructure.FuncionarioOrganogramaRepository;
import br.com.techne.sistemafolha.organograma.infrastructure.NoOrganogramaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganogramaServicePortWiringTest {

    private static final Long NO_ID = 5L;
    private static final Long FUNCIONARIO_ID = 42L;

    @Mock
    private NoOrganogramaRepository noOrganogramaRepository;

    @Mock
    private FuncionarioOrganogramaRepository funcionarioOrganogramaRepository;

    @Mock
    private CentroCustoOrganogramaRepository centroCustoOrganogramaRepository;

    @Mock
    private FuncionarioConsultaPort funcionarioConsultaPort;

    @Mock
    private CadastrosLookupPort cadastrosLookupPort;

    @InjectMocks
    private OrganogramaService organogramaService;

    @Test
    void associarFuncionario_resolve_funcionario_via_port() {
        NoOrganograma no = noAtivo(NO_ID);
        Funcionario funcionario = funcionarioAtivo(FUNCIONARIO_ID);

        when(noOrganogramaRepository.findByIdAndAtivoTrue(NO_ID)).thenReturn(Optional.of(no));
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(FUNCIONARIO_ID)).thenReturn(Optional.of(funcionario));
        when(funcionarioOrganogramaRepository.existsByFuncionarioAndNoOrganograma(funcionario, no))
            .thenReturn(false);
        when(funcionarioOrganogramaRepository.findByFuncionario(funcionario))
            .thenReturn(Collections.emptyList());
        when(funcionarioOrganogramaRepository.save(any(FuncionarioOrganograma.class))).thenAnswer(inv -> {
            FuncionarioOrganograma associacao = inv.getArgument(0);
            associacao.setId(99L);
            return associacao;
        });

        FuncionarioOrganogramaDTO result = organogramaService.associarFuncionario(NO_ID, FUNCIONARIO_ID);

        assertEquals(FUNCIONARIO_ID, result.funcionarioId());
        assertEquals(NO_ID, result.noOrganogramaId());
        verify(funcionarioConsultaPort).findByIdAndAtivoTrue(FUNCIONARIO_ID);
    }

    @Test
    void associarFuncionario_funcionario_nao_encontrado_lanca_excecao() {
        NoOrganograma no = noAtivo(NO_ID);

        when(noOrganogramaRepository.findByIdAndAtivoTrue(NO_ID)).thenReturn(Optional.of(no));
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(FUNCIONARIO_ID)).thenReturn(Optional.empty());

        assertThrows(FuncionarioNotFoundException.class,
            () -> organogramaService.associarFuncionario(NO_ID, FUNCIONARIO_ID));
        verify(funcionarioOrganogramaRepository, never()).save(any());
    }

    private NoOrganograma noAtivo(Long id) {
        NoOrganograma no = new NoOrganograma();
        no.setId(id);
        no.setNome("Diretoria");
        no.setAtivo(true);
        return no;
    }

    private Funcionario funcionarioAtivo(Long id) {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setNome("João Silva");
        funcionario.setAtivo(true);
        return funcionario;
    }
}
