package com.example.untitled.service.importer;

import java.io.Closeable;
import java.io.IOException;

/**
 * Iterator over the records of an uploaded file. Import strategies only see this interface,
 * so every strategy works with every format. Create instances with {@link BookReaderFactory}.
 */
public interface BookReader extends Closeable {

    /**
     * @return the next non-blank record, or {@code null} when the input is exhausted
     */
    BookRecord next() throws IOException;

    /**
     * @return the line (CSV) or row (Excel) number of the last record returned, for log messages
     */
    long position();
}
