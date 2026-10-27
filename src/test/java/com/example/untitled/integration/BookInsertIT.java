package com.example.untitled.integration;

import com.example.untitled.service.BookService;
import com.example.untitled.service.BulkOperationGuard;
import com.example.untitled.service.ImportResult;
import com.example.untitled.service.importer.BookImportService;
import com.example.untitled.service.importer.ImportFormat;
import com.example.untitled.service.importer.SampleFileService;
import com.example.untitled.service.importer.ParallelStreamImportStrategy;
import com.example.untitled.service.importer.TraditionalImportStrategy;
import com.example.untitled.service.importer.VirtualThreadsImportStrategy;
import com.example.untitled.support.XlsxTestFiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Inserts 100,000 rows with every import strategy, from both a CSV and an Excel file, into a real
 * MySQL 8.0 (Testcontainers), and verifies the data that actually landed in the table.
 * <p>
 * Requires Docker. Run with {@code mvn verify} (not part of {@code mvn test}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class BookInsertIT {

    private static final Logger log = LoggerFactory.getLogger(BookInsertIT.class);

    private static final int ROWS = 100_000;

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0")
            .withUrlParam("rewriteBatchedStatements", "true");

    @Autowired
    private BookImportService importService;

    @Autowired
    private BookService bookService;

    @Autowired
    private BulkOperationGuard guard;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SampleFileService sampleFiles;

    // Header + ROWS rows in both formats. Titles are unique and contain a comma; price = (i % 100) + 0.99.
    private static final byte[] CSV = buildCsv();
    private static final byte[] XLSX = buildXlsx();

    private static String title(int i) {
        return "Title " + i + ", Vol 1";
    }

    private static String author(int i) {
        return "Author " + (i % 500);
    }

    private static String price(int i) {
        return (i % 100) + ".99";
    }

    private static byte[] buildCsv() {
        StringBuilder sb = new StringBuilder(ROWS * 40);
        sb.append("title,author,price\n");
        for (int i = 0; i < ROWS; i++) {
            sb.append('"').append(title(i)).append("\",\"").append(author(i)).append("\",").append(price(i)).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] buildXlsx() {
        Stream<Object[]> header = Stream.<Object[]>of(new Object[]{"title", "author", "price"});
        Stream<Object[]> rows = IntStream.range(0, ROWS)
                .mapToObj(i -> new Object[]{title(i), author(i), Double.parseDouble(price(i))}); // numeric price cell
        return XlsxTestFiles.xlsx(Stream.concat(header, rows));
    }

    private static MockMultipartFile file(ImportFormat format) {
        return switch (format) {
            case CSV -> new MockMultipartFile("file", "books_100000.csv", "text/csv", CSV);
            case XLSX -> new MockMultipartFile("file", "books_100000.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", XLSX);
        };
    }

    private static BigDecimal expectedPriceSum() {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < ROWS; i++) {
            sum = sum.add(new BigDecimal(price(i)));
        }
        return sum;
    }

    static Stream<Arguments> strategiesAndFormats() {
        return Stream.of(TraditionalImportStrategy.STRATEGY_NAME, VirtualThreadsImportStrategy.STRATEGY_NAME, ParallelStreamImportStrategy.STRATEGY_NAME)
                .flatMap(strategy -> Stream.of(ImportFormat.values()).map(format -> Arguments.of(strategy, format)));
    }

    @BeforeEach
    void emptyTable() {
        // A background import from a previous test may still be finishing
        await().atMost(Duration.ofMinutes(1)).until(() -> !guard.isBusy());
        jdbcTemplate.execute("TRUNCATE TABLE books");
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("strategiesAndFormats")
    void importInserts100kRows(String strategy, ImportFormat format) throws Exception {
        ImportResult result = importService.importNow(file(format), format, strategy);

        log.info("IT {} / {}: imported={}, skipped={}, durationMs={}",
                strategy, format, result.imported(), result.skipped(), result.durationMs());
        assertThat(result.imported()).isEqualTo(ROWS);
        assertThat(result.skipped()).isEqualTo(1); // the header row
        assertRowsInDatabase();
    }

    @ParameterizedTest(name = "background / {0}")
    @EnumSource(ImportFormat.class)
    void backgroundImportEventuallyInserts100kRows(ImportFormat format) throws Exception {
        long start = System.currentTimeMillis();
        importService.importInBackground(file(format), format, VirtualThreadsImportStrategy.STRATEGY_NAME);
        log.info("IT background / {}: accepted after {} ms", format, System.currentTimeMillis() - start);

        await().atMost(Duration.ofMinutes(2)).pollInterval(Duration.ofMillis(100))
                .until(() -> !guard.isBusy());
        log.info("IT background / {}: finished after {} ms", format, System.currentTimeMillis() - start);

        assertRowsInDatabase();
    }

    /** The random sample files from /api/books/sample/{csv,excel} import cleanly into MySQL. */
    @ParameterizedTest(name = "sample file / {0}")
    @EnumSource(ImportFormat.class)
    void randomSampleFileImportsCompletely(ImportFormat format) throws Exception {
        long start = System.currentTimeMillis();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        sampleFiles.write(format, ROWS, out);
        log.info("IT sample file / {}: generated {} rows ({} KB) in {} ms",
                format, ROWS, out.size() / 1024, System.currentTimeMillis() - start);

        MockMultipartFile upload = new MockMultipartFile("file", sampleFiles.filename(format, ROWS), format.mediaType(),
                out.toByteArray());
        ImportResult result = importService.importNow(upload, format, VirtualThreadsImportStrategy.STRATEGY_NAME);

        assertThat(result.imported()).isEqualTo(ROWS);
        assertThat(result.skipped()).isEqualTo(1); // the header row
        assertThat(count("SELECT COUNT(*) FROM books")).isEqualTo(ROWS);
        assertThat(count("SELECT COUNT(DISTINCT title) FROM books")).isEqualTo(ROWS);
        assertThat(count("SELECT COUNT(*) FROM books WHERE price < 5.99 OR price > 99.99")).isZero();
    }

    @Test
    void generateRandomBooksInserts100kRows() {
        long start = System.currentTimeMillis();
        bookService.generateRandomBooks(ROWS);
        log.info("IT generate-random: {} rows in {} ms", ROWS, System.currentTimeMillis() - start);

        assertThat(count("SELECT COUNT(*) FROM books")).isEqualTo(ROWS);
        assertThat(count("SELECT COUNT(DISTINCT title) FROM books")).isEqualTo(ROWS);
        Map<String, Object> range = jdbcTemplate.queryForMap("SELECT MIN(price) AS minp, MAX(price) AS maxp FROM books");
        assertThat((BigDecimal) range.get("minp")).isGreaterThanOrEqualTo(new BigDecimal("5.99"));
        assertThat((BigDecimal) range.get("maxp")).isLessThanOrEqualTo(new BigDecimal("99.99"));
    }

    private void assertRowsInDatabase() {
        assertThat(count("SELECT COUNT(*) FROM books")).isEqualTo(ROWS);
        // No batch was written twice and none was lost
        assertThat(count("SELECT COUNT(DISTINCT title) FROM books")).isEqualTo(ROWS);
        assertThat(jdbcTemplate.queryForObject("SELECT SUM(price) FROM books", BigDecimal.class))
                .isEqualByComparingTo(expectedPriceSum());

        // Spot-check first and last rows, including the comma inside the title
        Map<String, Object> first = jdbcTemplate.queryForMap(
                "SELECT author, price FROM books WHERE title = ?", title(0));
        assertThat(first.get("author")).isEqualTo("Author 0");
        assertThat((BigDecimal) first.get("price")).isEqualByComparingTo("0.99");

        Map<String, Object> last = jdbcTemplate.queryForMap(
                "SELECT author, price FROM books WHERE title = ?", title(ROWS - 1));
        assertThat(last.get("author")).isEqualTo("Author 499");
        assertThat((BigDecimal) last.get("price")).isEqualByComparingTo("99.99");
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
