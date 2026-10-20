# Swagger Integration - Quick Reference

## Summary of Changes

### 1. pom.xml - Added Dependencies
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-annotations</artifactId>
    <version>2.2.15</version>
</dependency>
```

### 2. BookController.java - Added Annotations
```java
@RestController
@RequestMapping("/api/books")
@Tag(name = "Books", description = "API for managing books")
public class BookController {

    @GetMapping
    @Operation(summary = "Get all books", description = "Retrieve a list of all books from the database")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of books")
    public List<Book> all() { ... }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID", description = "Retrieve a specific book by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book found"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<Book> byId(@PathVariable @Parameter(description = "Book ID") Long id) { ... }

    @PostMapping
    @Operation(summary = "Create a new book", description = "Add a new book to the database")
    @ApiResponse(responseCode = "201", description = "Book created successfully")
    public ResponseEntity<Book> create(@RequestBody Book book) { ... }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a book", description = "Remove a book from the database by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Book deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<Void> delete(@PathVariable @Parameter(description = "Book ID") Long id) { ... }

    @PostMapping("/generate-random")
    @Operation(summary = "Generate random books", description = "Generate a specified number of random books in the database")
    @ApiResponse(responseCode = "200", description = "Books generated successfully")
    public ResponseEntity<String> generateRandomBooks(@RequestParam(defaultValue = "100000") @Parameter(description = "Number of books to generate", example = "100000") int count) { ... }

    @PostMapping("/import-csv-traditional")
    @Operation(summary = "Import books from CSV (Traditional)", description = "Import books from a CSV file using traditional threading")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV imported successfully"),
            @ApiResponse(responseCode = "400", description = "Import failed")
    })
    public ResponseEntity<String> importCsvTraditional(@RequestParam @Parameter(description = "Path to CSV file") String filePath) { ... }

    @PostMapping("/import-csv-virtual-threads")
    @Operation(summary = "Import books from CSV (Virtual Threads)", description = "Import books from a CSV file using virtual threads for better performance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV imported successfully"),
            @ApiResponse(responseCode = "400", description = "Import failed")
    })
    public ResponseEntity<String> importCsvVirtualThreads(@RequestParam @Parameter(description = "Path to CSV file") String filePath) { ... }
}
```

### 3. Book.java - Added Schema Annotations
```java
@Entity
@Table(name = "books")
@Schema(description = "Book entity representing a book in the library")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier for the book", example = "1")
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Title of the book", example = "The Great Gatsby")
    private String title;

    @Column(nullable = false)
    @Schema(description = "Author of the book", example = "F. Scott Fitzgerald")
    private String author;

    @Column(nullable = false, precision = 10, scale = 2)
    @Schema(description = "Price of the book", example = "19.99")
    private BigDecimal price;
    
    // ... rest of the class
}
```

### 4. application.yml - Added Swagger Configuration
```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    operations-sorter: method
    tags-sorter: alpha
    syntaxHighlight:
      activate: true
      theme: monokai
  api-docs:
    path: /v3/api-docs
    enabled: true
```

### 5. New File: OpenApiConfig.java
Created at: `src/main/java/com/example/untitled/config/OpenApiConfig.java`

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Books API")
                        .version("1.0.0")
                        .description("REST API for managing a library of books with support for CRUD operations and bulk CSV imports")
                        .contact(new Contact()
                                .name("Books API Support")
                                .url("https://example.com")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
```

## Build & Deploy

```bash
# Build
mvn clean package -DskipTests

# Run locally
java -jar target/untitled-0.0.1-SNAPSHOT.jar

# Docker
docker-compose up -d
```

## Access Swagger UI

After starting the application:
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI Spec (JSON)**: http://localhost:8080/v3/api-docs
- **OpenAPI Spec (YAML)**: http://localhost:8080/v3/api-docs.yaml

## Testing the API

All endpoints are fully documented and can be tested directly from the Swagger UI:
1. Open http://localhost:8080/swagger-ui/index.html in your browser
2. Expand any endpoint
3. Click "Try it out"
4. Fill in parameters
5. Click "Execute"
6. View the response

## Debug Mode

The application is configured to run in debug mode:
- **Debug Port**: 5005 (available for remote debugging)
- **Application Port**: 8080

To connect a debugger (IntelliJ, VS Code, etc.):
1. Configure remote debug connection to localhost:5005
2. All breakpoints will be hit
3. Full debugging capabilities available

