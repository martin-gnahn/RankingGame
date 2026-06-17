package com.example.rankinggame.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI rankingGameOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RankingGame API")
                        .description("REST API for rooms, players, health checks, and products.")
                        .version("v1"));
    }
}
