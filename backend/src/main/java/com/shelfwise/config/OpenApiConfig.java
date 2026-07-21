package com.shelfwise.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shelfWiseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShelfWise ERP API")
                        .description("""
                                AI-Powered Smart Shelf ERP System
                                
                                **Architecture:** Java Spring Boot + Python ML Microservice (polyglot)
                                **Models:** DQN (discount pricing), XGBoost (demand forecasting),
                                           Isolation Forest (anomaly/fraud), NMF (B2B recommendations)
                                
                                **Base URL:** http://localhost:8080
                                **ML Service:** http://localhost:8000
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ShelfWise")
                                .url("https://github.com/yourusername/ShelfWise-ERP")));
    }
}
