package com.incident.classifier.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Document-to-Incident Classification API")
                        .version("1.0.0")
                        .description("""
                                Backend system that processes documents (PDF or text),
                                breaks them into chunks, and classifies each chunk into
                                predefined incident topics using keyword scoring with fuzzy matching.
                                """)
                        .contact(new Contact()
                                .name("Incident Classifier")
                                .email("dev@incident-classifier.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
