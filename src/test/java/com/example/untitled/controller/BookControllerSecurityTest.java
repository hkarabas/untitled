package com.example.untitled.controller;

import com.example.untitled.config.SecurityConfig;
import com.example.untitled.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({BookController.class, RootController.class, AuthController.class})
@Import(SecurityConfig.class)
class BookControllerSecurityTest {

    private static final SimpleGrantedAuthority USER = new SimpleGrantedAuthority("ROLE_USER");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BookService bookService;

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
