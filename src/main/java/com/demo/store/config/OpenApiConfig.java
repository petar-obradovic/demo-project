package com.demo.store.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * OpenAPI metadata and a convenience redirect from the app root to Swagger UI,
 * so opening the container's base URL lands on the interactive docs.
 */
@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    @Bean
    public OpenAPI storeOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Demo Store API")
                .description("E-commerce store backend: products, customers, carts, and orders.")
                .version("v1"));
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui.html");
    }
}
