package com.example.untitled.service.importer;

import com.example.untitled.service.ImportResult;

import java.io.IOException;

/**
 * Strategy pattern: one way of turning a file's records into database rows.
 * Implementations are Spring beans; {@link BookImportService} looks them up by {@link #name()}.
 */
public interface ImportStrategy {

    /** Name used in the API, e.g. {@code virtual-threads}. */
    String name();

    /** Imports every record of the job's file. Does not close the job. */
    ImportResult run(ImportJob job) throws IOException;
}
