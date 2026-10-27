package com.example.untitled.service.importer;

import com.example.untitled.service.BulkOperationGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ImportJobTest {

    @TempDir
    Path tempDir;

    @Test
    void releasesPermitEvenWhenTheFileCannotBeDeleted() throws Exception {
        Path notEmptyDir = Files.createDirectory(tempDir.resolve("upload.csv"));
        Files.createFile(notEmptyDir.resolve("child"));
        BulkOperationGuard guard = new BulkOperationGuard();

        new ImportJob("books.csv", ImportFormat.CSV, notEmptyDir, 0, new BookReaderFactory(), guard.acquire()).close();

        assertThat(notEmptyDir).exists();   // deleteIfExists failed; the error was only logged
        assertThat(guard.isBusy()).isFalse();
    }
}
