package com.example.untitled.integration;

import com.example.untitled.service.BookService;
import com.example.untitled.service.ImportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Inserts 100,000 rows with every bulk insert method into a real MySQL 8.0 (Testcontainers)
 * and verifies the data that actually landed in the table.
 * <p>
 * Requires Docker. Run with {@code mvn verify} (not part of {@code mvn test}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookInsertIT {

    private static final Logger log = LoggerFactory.getLogger(BookInsertIT.class);

    private static final int ROWS = 100_000;

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0")
            .withUrlParam("rewriteBatchedStatements", "true");

    @Autowired
    private BookService bookService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Header + {@value #ROWS} rows. Titles are unique and contain a quoted comma; price = (i % 100) + 0.99. */
    private static final byte[] CSV = buildCsv();

    private static byte[] buildCsv() {
        StringBuilder sb = new StringBuilder(ROWS * 40);
        sb.append("title,author,price\n");
        for (int i = 0; i < ROWS; i++) {
            sb.append("\"Title ").append(i).append(", Vol 1\",\"Author ").append(i % 500).append("\",")
                    .append(i % 100).append(".99\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static BigDecimal expectedPriceSum() {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < ROWS; i++) {
            sum = sum.add(new BigDecimal((i % 100) + ".99"));
        }
        return sum;
    }

    private static MockMultipartFile csvFile() {
        return new MockMultipartFile("file", "books_100000.csv", "text/csv", CSV);
    }

    @BeforeEach
    void emptyTable() {
        jdbcTemplate.execute("TRUNCATE TABLE books");
    }

    @Test
    @Order(1)
    void traditionalImportInserts100kRows() throws Exception {
        ImportResult result = bookService.importCsvTraditional(csvFile());

        assertImportResult("traditional", result);
        assertCsvRowsInDatabase();
    }

    @Test
    @Order(2)
    void virtualThreadsImportInserts100kRows() throws Exception {
        ImportResult result = bookService.importCsvVirtualThreads(csvFile());

        assertImportResult("virtual-threads", result);
        assertCsvRowsInDatabase();
    }

    @Test
    @Order(3)
    void parallelStreamImportInserts100kRows() throws Exception {
        ImportResult result = bookService.importCsvParallelStream(csvFile());

        assertImportResult("parallel-stream", result);
        assertCsvRowsInDatabase();
    }

    @Test
    @Order(4)
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

    /**
     * Runs last: the background import holds the service's bulk-operation permit for a moment
     * after the last row is committed, which would make a following test get "busy".
     */
    @Test
    @Order(5)
    void fireAndForgetImportEventuallyInserts100kRows() throws Exception {
        long start = System.currentTimeMillis();
        bookService.importCsvParallelFireAndForget(csvFile());
        log.info("IT parallel-fire-and-forget: accepted after {} ms", System.currentTimeMillis() - start);

        await().atMost(Duration.ofMinutes(2)).pollInterval(Duration.ofMillis(200))
                .until(() -> count("SELECT COUNT(*) FROM books") == ROWS);
        log.info("IT parallel-fire-and-forget: {} rows visible after {} ms", ROWS, System.currentTimeMillis() - start);

        assertCsvRowsInDatabase();
    }

    private void assertImportResult(String strategy, ImportResult result) {
        log.info("IT {}: imported={}, skipped={}, durationMs={}", strategy, result.imported(), result.skipped(), result.durationMs());
        assertThat(result.imported()).isEqualTo(ROWS);
        assertThat(result.skipped()).isEqualTo(1); // the header row
    }

    private void assertCsvRowsInDatabase() {
        assertThat(count("SELECT COUNT(*) FROM books")).isEqualTo(ROWS);
        // No batch was written twice and none was lost
        assertThat(count("SELECT COUNT(DISTINCT title) FROM books")).isEqualTo(ROWS);
        assertThat(jdbcTemplate.queryForObject("SELECT SUM(price) FROM books", BigDecimal.class))
                .isEqualByComparingTo(expectedPriceSum());

        // Spot-check first and last rows, including the comma inside the quoted title
        Map<String, Object> first = jdbcTemplate.queryForMap(
                "SELECT author, price FROM books WHERE title = ?", "Title 0, Vol 1");
        assertThat(first.get("author")).isEqualTo("Author 0");
        assertThat((BigDecimal) first.get("price")).isEqualByComparingTo("0.99");

        Map<String, Object> last = jdbcTemplate.queryForMap(
                "SELECT author, price FROM books WHERE title = ?", "Title 99999, Vol 1");
        assertThat(last.get("author")).isEqualTo("Author 499");
        assertThat((BigDecimal) last.get("price")).isEqualByComparingTo("99.99");
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
