package com.example.untitled.service.importer;

import com.example.untitled.entity.Book;
import com.example.untitled.service.impl.BookBatchWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Single-threaded baseline: reads, parses and inserts one {@value #BATCH_SIZE}-record batch at a time.
 */
@Component
public class TraditionalImportStrategy extends AbstractImportStrategy {

    public static final String NAME = "traditional";

    public TraditionalImportStrategy(BookBatchWriter batchWriter) {
        super(batchWriter);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected ImportCounts doImport(BookReader reader) throws IOException {
        long imported = 0;
        long skipped = 0;
        List<Book> batch = new ArrayList<>(BATCH_SIZE);

        BookRecord record;
        while ((record = reader.next()) != null) {
            Book book = record.toBook();
            if (book == null) {
                skipped++;
                continue;
            }
            batch.add(book);
            if (batch.size() == BATCH_SIZE) {
                batchWriter.insert(batch);
                imported += batch.size();
                batch = new ArrayList<>(BATCH_SIZE);
            }
        }
        batchWriter.insert(batch);
        imported += batch.size();
        return new ImportCounts(imported, skipped);
    }
}
