package com.example.untitled.config;

import org.apache.catalina.connector.Connector;
import org.junit.jupiter.api.Test;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void tomcatAcceptsPostsUpTo200Mb() {
        TomcatServletWebServerFactory factory = new OpenApiConfig().servletContainer();
        Connector connector = new Connector();

        factory.getConnectorCustomizers().forEach(customizer -> customizer.customize(connector));

        assertThat(connector.getMaxPostSize()).isEqualTo(200 * 1024 * 1024);
    }
}
