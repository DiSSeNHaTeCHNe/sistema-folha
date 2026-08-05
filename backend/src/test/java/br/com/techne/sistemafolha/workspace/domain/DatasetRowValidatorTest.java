package br.com.techne.sistemafolha.workspace.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatasetRowValidatorTest {

    private DatasetRowValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DatasetRowValidator();
    }

    @Test
    void validate_numeroValido_semErros() {
        List<DatasetFieldSchema> schema = List.of(field("quantidade", DatasetFieldType.NUMERO));
        List<FieldValidationError> errors = validator.validate(schema, Map.of("quantidade", 42));

        assertTrue(errors.isEmpty());
    }

    @Test
    void validate_numeroTextoInvalido_rejeita() {
        List<DatasetFieldSchema> schema = List.of(field("quantidade", DatasetFieldType.NUMERO));
        List<FieldValidationError> errors = validator.validate(schema, Map.of("quantidade", "abc"));

        assertEquals(1, errors.size());
        assertEquals("quantidade", errors.get(0).field());
    }

    @Test
    void validate_textoValido_semErros() {
        List<DatasetFieldSchema> schema = List.of(field("descricao", DatasetFieldType.TEXTO));
        List<FieldValidationError> errors = validator.validate(schema, Map.of("descricao", "Planejamento"));

        assertTrue(errors.isEmpty());
    }

    @Test
    void validate_textoNumeroInvalido_rejeita() {
        List<DatasetFieldSchema> schema = List.of(field("descricao", DatasetFieldType.TEXTO));
        List<FieldValidationError> errors = validator.validate(schema, Map.of("descricao", 123));

        assertEquals(1, errors.size());
        assertEquals("descricao", errors.get(0).field());
    }

    @Test
    void validate_dataIsoValida_semErros() {
        List<DatasetFieldSchema> schema = List.of(field("competencia", DatasetFieldType.DATA));
        List<FieldValidationError> errors = validator.validate(schema, Map.of("competencia", "2026-01-15"));

        assertTrue(errors.isEmpty());
    }

    @Test
    void validate_dataLocalDateValida_semErros() {
        List<DatasetFieldSchema> schema = List.of(field("competencia", DatasetFieldType.DATA));
        List<FieldValidationError> errors = validator.validate(schema, Map.of("competencia", LocalDate.of(2026, 1, 15)));

        assertTrue(errors.isEmpty());
    }

    @Test
    void validate_dataInvalida_rejeita() {
        List<DatasetFieldSchema> schema = List.of(field("competencia", DatasetFieldType.DATA));
        List<FieldValidationError> errors = validator.validate(schema, Map.of("competencia", "15/01/2026"));

        assertEquals(1, errors.size());
        assertEquals("competencia", errors.get(0).field());
    }

    @Test
    void validate_moedaBigDecimalValida_semErros() {
        List<DatasetFieldSchema> schema = List.of(field("valor", DatasetFieldType.MOEDA));
        List<FieldValidationError> errors = validator.validate(schema, Map.of("valor", new BigDecimal("1234.56")));

        assertTrue(errors.isEmpty());
    }

    @Test
    void validate_moedaTextoInvalido_rejeita() {
        List<DatasetFieldSchema> schema = List.of(field("valor", DatasetFieldType.MOEDA));
        List<FieldValidationError> errors = validator.validate(schema, Map.of("valor", "R$ 1.234,56"));

        assertEquals(1, errors.size());
        assertEquals("valor", errors.get(0).field());
    }

    @Test
    void validate_referenciaLongValida_semErros() {
        List<DatasetFieldSchema> schema = List.of(field("centro_custo_id", DatasetFieldType.REFERENCIA));
        List<FieldValidationError> errors = validator.validate(schema, Map.of("centro_custo_id", 10L));

        assertTrue(errors.isEmpty());
    }

    @Test
    void validate_referenciaTextoInvalido_rejeita() {
        List<DatasetFieldSchema> schema = List.of(field("centro_custo_id", DatasetFieldType.REFERENCIA));
        List<FieldValidationError> errors = validator.validate(schema, Map.of("centro_custo_id", "TI"));

        assertEquals(1, errors.size());
        assertEquals("centro_custo_id", errors.get(0).field());
    }

    @Test
    void validate_campoObrigatorioAusente_rejeita() {
        List<DatasetFieldSchema> schema = List.of(
            new DatasetFieldSchema("nome", DatasetFieldType.TEXTO, null, true, null));
        List<FieldValidationError> errors = validator.validate(schema, Map.of());

        assertEquals(1, errors.size());
        assertEquals("nome", errors.get(0).field());
    }

    @Test
    void validate_multiplosTiposIncompativeis_retornaErrosPorCampo() {
        List<DatasetFieldSchema> schema = List.of(
            field("quantidade", DatasetFieldType.NUMERO),
            field("descricao", DatasetFieldType.TEXTO));
        List<FieldValidationError> errors = validator.validate(schema, Map.of(
            "quantidade", "x",
            "descricao", 99));

        assertEquals(2, errors.size());
    }

    private DatasetFieldSchema field(String nome, DatasetFieldType tipo) {
        return new DatasetFieldSchema(nome, tipo, null, false, null);
    }
}
