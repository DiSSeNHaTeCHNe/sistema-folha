package br.com.techne.sistemafolha.relatorios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioArquivoMappingTest {

    @Test
    void pdfBytes_mapeadoComoByteaSemLob() throws Exception {
        Field pdfBytes = RelatorioArquivo.class.getDeclaredField("pdfBytes");

        assertFalse(pdfBytes.isAnnotationPresent(Lob.class),
            "pdfBytes não deve usar @Lob (PostgreSQL BYTEA via VARBINARY)");

        JdbcTypeCode jdbcTypeCode = pdfBytes.getAnnotation(JdbcTypeCode.class);
        assertNotNull(jdbcTypeCode, "pdfBytes deve ter @JdbcTypeCode");
        assertEquals(SqlTypes.VARBINARY, jdbcTypeCode.value());

        Column column = pdfBytes.getAnnotation(Column.class);
        assertNotNull(column, "pdfBytes deve ter @Column");
        assertTrue(column.columnDefinition().toLowerCase().contains("bytea"),
            "columnDefinition deve declarar bytea");
    }
}
