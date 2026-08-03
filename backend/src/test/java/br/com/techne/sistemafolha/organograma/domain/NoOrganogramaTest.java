package br.com.techne.sistemafolha.organograma.domain;

import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NoOrganogramaTest {

    @Test
    void onCreate_comParentDefineNivel() {
        NoOrganograma parent = new NoOrganograma();
        parent.setNivel(2);

        NoOrganograma no = new NoOrganograma();
        no.setParent(parent);
        no.onCreate();

        assertEquals(3, no.getNivel());
        assertNotNull(no.getDataCriacao());
        assertNotNull(no.getDataAtualizacao());
    }

    @Test
    void onCreate_semParentNivelZero() {
        NoOrganograma no = new NoOrganograma();
        no.onCreate();
        assertEquals(0, no.getNivel());
    }

    @Test
    void onCreate_parentSemNivelDefineZero() {
        NoOrganograma parent = new NoOrganograma();
        parent.setNivel(null);
        NoOrganograma no = new NoOrganograma();
        no.setParent(parent);
        no.onCreate();
        assertEquals(0, no.getNivel());
    }

    @Test
    void onUpdate_atualizaData() {
        NoOrganograma no = new NoOrganograma();
        no.onCreate();
        var antes = no.getDataAtualizacao();
        no.onUpdate();
        assertTrue(!no.getDataAtualizacao().isBefore(antes));
    }

    @Test
    void isRaiz_e_temFilhos() {
        NoOrganograma raiz = new NoOrganograma();
        assertTrue(raiz.isRaiz());
        assertFalse(raiz.temFilhos());

        NoOrganograma filho = new NoOrganograma();
        raiz.setChildren(new ArrayList<>(List.of(filho)));
        assertTrue(raiz.temFilhos());

        filho.setParent(raiz);
        assertFalse(filho.isRaiz());
    }

    @Test
    void getFuncionariosAtivos_filtraInativos() {
        Funcionario ativo = mock(Funcionario.class);
        when(ativo.getAtivo()).thenReturn(true);
        Funcionario inativo = mock(Funcionario.class);
        when(inativo.getAtivo()).thenReturn(false);

        FuncionarioOrganograma foAtivo = new FuncionarioOrganograma();
        foAtivo.setFuncionario(ativo);
        FuncionarioOrganograma foInativo = new FuncionarioOrganograma();
        foInativo.setFuncionario(inativo);

        NoOrganograma no = new NoOrganograma();
        no.setFuncionarios(List.of(foAtivo, foInativo));

        assertEquals(1, no.getFuncionariosAtivos().size());
    }

    @Test
    void getCentrosCustoAtivos_filtraInativos() {
        CentroCusto ativo = mock(CentroCusto.class);
        when(ativo.getAtivo()).thenReturn(true);
        CentroCusto inativo = mock(CentroCusto.class);
        when(inativo.getAtivo()).thenReturn(false);

        CentroCustoOrganograma ccoAtivo = new CentroCustoOrganograma();
        ccoAtivo.setCentroCusto(ativo);
        CentroCustoOrganograma ccoInativo = new CentroCustoOrganograma();
        ccoInativo.setCentroCusto(inativo);

        NoOrganograma no = new NoOrganograma();
        no.setCentrosCusto(List.of(ccoAtivo, ccoInativo));

        assertEquals(1, no.getCentrosCustoAtivos().size());
    }
}
