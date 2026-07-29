package br.com.techne.sistemafolha.shared.access;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CentroCustoEfetivoTest {

    @Test
    void idOf_quandoLinhaPresente_retornaLinha() {
        assertEquals(100L, CentroCustoEfetivo.idOf(100L, 200L));
    }

    @Test
    void idOf_quandoLinhaNull_retornaFuncionario() {
        assertEquals(200L, CentroCustoEfetivo.idOf(null, 200L));
    }

    @Test
    void idOf_quandoAmbosNull_retornaNull() {
        assertNull(CentroCustoEfetivo.idOf(null, null));
    }

    @Test
    void pertenceAoEscopo_idDentroDoSet_retornaTrue() {
        assertTrue(CentroCustoEfetivo.pertenceAoEscopo(10L, Set.of(10L, 20L)));
    }

    @Test
    void pertenceAoEscopo_idForaDoSet_retornaFalse() {
        assertFalse(CentroCustoEfetivo.pertenceAoEscopo(30L, Set.of(10L, 20L)));
    }

    @Test
    void pertenceAoEscopo_idNull_retornaFalse() {
        assertFalse(CentroCustoEfetivo.pertenceAoEscopo(null, Set.of(10L)));
    }

    @Test
    void pertenceAoEscopo_setNull_retornaFalse() {
        assertFalse(CentroCustoEfetivo.pertenceAoEscopo(10L, null));
    }

    @Test
    void pertenceAoEscopo_setVazio_retornaFalse() {
        assertFalse(CentroCustoEfetivo.pertenceAoEscopo(10L, Collections.emptySet()));
    }
}
