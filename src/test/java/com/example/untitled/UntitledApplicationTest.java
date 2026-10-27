package com.example.untitled;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class UntitledApplicationTest {

    @Test
    void mainStartsSpringApplication() {
        String[] args = {"--server.port=0"};
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            UntitledApplication.main(args);

            spring.verify(() -> SpringApplication.run(UntitledApplication.class, args));
        }
    }
}
