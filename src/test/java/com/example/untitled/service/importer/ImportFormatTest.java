package com.example.untitled.service.importer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportFormatTest {

    @Test
    void acceptsMatchingExtensionInAnyCase() {
        assertThatCode(() -> ImportFormat.CSV.requireFilename("BOOKS.CSV")).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingFilename() {
        assertThatThrownBy(() -> ImportFormat.XLSX.requireFilename(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only .xlsx files are allowed on this endpoint");
    }
}
