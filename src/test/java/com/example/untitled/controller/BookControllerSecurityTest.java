package com.example.untitled.controller;

import com.example.untitled.config.SecurityConfig;
import com.example.untitled.service.BookService;
import com.example.untitled.service.ImportResult;
import com.example.untitled.service.importer.BookImportService;
import com.example.untitled.service.importer.ImportFormat;
import com.example.untitled.service.importer.SampleFileService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({BookController.class, CsvImportController.class, ExcelImportController.class,
        RootController.class, AuthController.class})
@Import({SecurityConfig.class, SampleFileService.class})
class BookControllerSecurityTest {

    private static final SimpleGrantedAuthority USER = new SimpleGrantedAuthority("ROLE_USER");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private BookImportService importService;

    private static final MockMultipartFile CSV = new MockMultipartFile("file", "books.csv", "text/csv", new byte[]{1});
    private static final MockMultipartFile XLSX = new MockMultipartFile("file", "books.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});

    @Test
    void sampleExcelDownloadIsAValidWorkbook() throws Exception {
        MvcResult started = mvc.perform(get("/api/books/sample/excel").param("rows", "10").with(jwt().authorities(USER)))
                .andExpect(request().asyncStarted())
                .andReturn();
        MvcResult result = mvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", ImportFormat.XLSX.mediaType()))
                .andExpect(header().string("Content-Disposition", containsString("books_random_10.xlsx")))
                .andReturn();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            assertThat(workbook.getSheetAt(0).getPhysicalNumberOfRows()).isEqualTo(11); // header + 10
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("title");
        }
    }

    @Test
    void sampleCsvDownloadHasHeaderAndRows() throws Exception {
        MvcResult started = mvc.perform(get("/api/books/sample/csv").param("rows", "5").with(jwt().authorities(USER)))
                .andExpect(request().asyncStarted())
                .andReturn();
        MvcResult result = mvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("books_random_5.csv")))
                .andReturn();

        String[] lines = result.getResponse().getContentAsString().split("\n");
        assertThat(lines).hasSize(6);
        assertThat(lines[0]).isEqualTo("title,author,price");
        assertThat(lines[1]).matches("\"The \\w+ \\w+ 1\",\"\\w+ \\w+\",\\d+\\.\\d{2}");
    }

    @Test
    void sampleDownloadsRequireAuthenticationAndValidRows() throws Exception {
        mvc.perform(get("/api/books/sample/excel")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/books/sample/csv")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/books/sample/excel").param("rows", "0").with(jwt().authorities(USER)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/books/sample/csv").param("rows", "2000000").with(jwt().authorities(USER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void excelShortcutEndpointsUseExcelFormat() throws Exception {
        when(importService.importNow(any(), any(), any())).thenReturn(new ImportResult(1, 0, 1));

        mvc.perform(multipart("/api/books/import-excel-traditional").file(XLSX).with(jwt().authorities(USER)))
                .andExpect(status().isOk());
        mvc.perform(multipart("/api/books/import-excel-virtual-threads").file(XLSX).with(jwt().authorities(USER)))
                .andExpect(status().isOk());
        mvc.perform(multipart("/api/books/import-excel-parallel-stream").file(XLSX).with(jwt().authorities(USER)))
                .andExpect(status().isOk());
        mvc.perform(multipart("/api/books/import-excel-parallel-fire-and-forget").file(XLSX).with(jwt().authorities(USER)))
                .andExpect(status().isAccepted());

        verify(importService).importNow(any(), eq(ImportFormat.XLSX), eq("traditional"));
        verify(importService).importNow(any(), eq(ImportFormat.XLSX), eq("virtual-threads"));
        verify(importService).importNow(any(), eq(ImportFormat.XLSX), eq("parallel-stream"));
        verify(importService).importInBackground(any(), eq(ImportFormat.XLSX), eq("virtual-threads"));
    }

    @Test
    void importEndpointsRequireAuthentication() throws Exception {
        mvc.perform(multipart("/api/books/import/csv").file(CSV)).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/books/import/excel").file(XLSX)).andExpect(status().isUnauthorized());
    }

    @Test
    void csvEndpointPassesCsvFormatAndStrategy() throws Exception {
        when(importService.importNow(any(), eq(ImportFormat.CSV), eq("traditional"))).thenReturn(new ImportResult(3, 1, 10));

        mvc.perform(multipart("/api/books/import/csv").file(CSV).param("strategy", "traditional").with(jwt().authorities(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CSV import completed")))
                .andExpect(content().string(containsString("3 books imported")));
    }

    @Test
    void excelEndpointPassesExcelFormatAndDefaultStrategy() throws Exception {
        when(importService.importNow(any(), eq(ImportFormat.XLSX), eq("virtual-threads"))).thenReturn(new ImportResult(5, 0, 10));

        mvc.perform(multipart("/api/books/import/excel").file(XLSX).with(jwt().authorities(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("XLSX import completed")));
    }

    @Test
    void asyncExcelImportReturns202() throws Exception {
        mvc.perform(multipart("/api/books/import/excel").file(XLSX).param("async", "true").with(jwt().authorities(USER)))
                .andExpect(status().isAccepted());
        verify(importService).importInBackground(any(), eq(ImportFormat.XLSX), eq("virtual-threads"));
    }

    @Test
    void wrongFileTypeReturns400() throws Exception {
        when(importService.importNow(any(), eq(ImportFormat.XLSX), any()))
                .thenThrow(new IllegalArgumentException("Only .xlsx files are allowed on this endpoint"));

        mvc.perform(multipart("/api/books/import/excel").file(CSV).with(jwt().authorities(USER)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Only .xlsx files")));
    }

    @Test
    void oldCsvEndpointsUseCsvFormat() throws Exception {
        when(importService.importNow(any(), any(), any())).thenReturn(new ImportResult(1, 0, 1));

        mvc.perform(multipart("/api/books/import-csv-parallel-stream").file(CSV).with(jwt().authorities(USER)))
                .andExpect(status().isOk());
        verify(importService).importNow(any(), eq(ImportFormat.CSV), eq("parallel-stream"));

        mvc.perform(multipart("/api/books/import-csv-parallel-fire-and-forget").file(CSV).with(jwt().authorities(USER)))
                .andExpect(status().isAccepted());
        verify(importService).importInBackground(any(), eq(ImportFormat.CSV), eq("virtual-threads"));
    }

    @Test
    void publicEndpointsDoNotRequireAuthentication() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/health")).andExpect(status().isOk());
        mvc.perform(get("/info")).andExpect(status().isOk());
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/public/info")).andExpect(status().isOk());
    }

    @Test
    void booksRequireAuthentication() throws Exception {
        mvc.perform(get("/api/books")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        mvc.perform(get("/api/admin/dashboard").with(jwt().authorities(USER))).andExpect(status().isForbidden());
    }

    @Test
    void deleteReturns404ForUnknownBook() throws Exception {
        when(bookService.deleteById(anyLong())).thenReturn(false);
        mvc.perform(delete("/api/books/42").with(jwt().authorities(USER))).andExpect(status().isNotFound());
    }

    @Test
    void negativePageIsRejected() throws Exception {
        mvc.perform(get("/api/books").param("page", "-1").with(jwt().authorities(USER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generationCountIsCapped() throws Exception {
        mvc.perform(post("/api/books/generate-random").param("count", "1000000000").with(jwt().authorities(USER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidBookIsRejected() throws Exception {
        mvc.perform(post("/api/books").with(jwt().authorities(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"Someone\",\"price\":10}"))
                .andExpect(status().isBadRequest());
    }
}
