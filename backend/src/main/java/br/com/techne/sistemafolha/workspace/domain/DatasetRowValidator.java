package br.com.techne.sistemafolha.workspace.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatasetRowValidator {

    public List<FieldValidationError> validate(List<DatasetFieldSchema> schema, Map<String, Object> valores) {
        List<FieldValidationError> errors = new ArrayList<>();
        if (schema == null || schema.isEmpty()) {
            return errors;
        }
        Map<String, Object> safeValores = valores != null ? valores : Map.of();

        for (DatasetFieldSchema field : schema) {
            Object value = safeValores.get(field.nome());
            if (isMissing(value)) {
                if (Boolean.TRUE.equals(field.obrigatorio())) {
                    errors.add(new FieldValidationError(field.nome(),
                        "Campo obrigatório não informado"));
                }
                continue;
            }
            if (!matchesType(field.tipo(), value)) {
                errors.add(new FieldValidationError(field.nome(),
                    incompatibleMessage(field.tipo())));
            }
        }
        return errors;
    }

    private boolean isMissing(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String s) {
            return s.isBlank();
        }
        return false;
    }

    private boolean matchesType(DatasetFieldType tipo, Object value) {
        return switch (tipo) {
            case NUMERO -> isNumero(value);
            case TEXTO -> value instanceof String;
            case DATA -> isData(value);
            case MOEDA -> isMoeda(value);
            case REFERENCIA -> isReferencia(value);
        };
    }

    private boolean isNumero(Object value) {
        if (value instanceof Number) {
            return true;
        }
        if (value instanceof String s) {
            try {
                new BigDecimal(s.trim());
                return true;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return false;
    }

    private boolean isData(Object value) {
        if (value instanceof LocalDate) {
            return true;
        }
        if (value instanceof String s) {
            try {
                LocalDate.parse(s.trim());
                return true;
            } catch (DateTimeParseException ex) {
                return false;
            }
        }
        return false;
    }

    private boolean isMoeda(Object value) {
        return isNumero(value);
    }

    private boolean isReferencia(Object value) {
        if (value instanceof Long || value instanceof Integer) {
            return true;
        }
        if (value instanceof Number number) {
            return number.longValue() == number.doubleValue();
        }
        if (value instanceof String s) {
            try {
                Long.parseLong(s.trim());
                return true;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return false;
    }

    private String incompatibleMessage(DatasetFieldType tipo) {
        return switch (tipo) {
            case NUMERO -> "Valor incompatível com campo numérico";
            case TEXTO -> "Valor incompatível com campo texto";
            case DATA -> "Valor incompatível com campo data (use AAAA-MM-DD)";
            case MOEDA -> "Valor incompatível com campo moeda";
            case REFERENCIA -> "Valor incompatível com campo referência (identificador numérico)";
        };
    }
}
