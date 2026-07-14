package com.example.rankinggame.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @SuppressWarnings("RedundantMethodOverride")
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
    }
}
