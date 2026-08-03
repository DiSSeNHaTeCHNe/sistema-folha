package br.com.techne.sistemafolha.organograma.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrganogramaDomainExceptionsTest {

    @Test
    void noOrganogramaNotFoundException_porId() {
        NoOrganogramaNotFoundException ex = new NoOrganogramaNotFoundException(42L);
        assertEquals("Nó do organograma não encontrado com ID: 42", ex.getMessage());
    }

    @Test
    void noOrganogramaNotFoundException_porMensagem() {
        NoOrganogramaNotFoundException ex = new NoOrganogramaNotFoundException("custom");
        assertEquals("custom", ex.getMessage());
    }

    @Test
    void organogramaAtivoConflictException_padrao() {
        OrganogramaAtivoConflictException ex = new OrganogramaAtivoConflictException();
        assertNotNull(ex.getMessage());
    }

    @Test
    void organogramaAtivoConflictException_customizada() {
        OrganogramaAtivoConflictException ex = new OrganogramaAtivoConflictException("conflito");
        assertEquals("conflito", ex.getMessage());
    }
}
