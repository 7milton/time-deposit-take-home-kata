package org.ikigaidigital.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("demo")
public class DemoSwaggerCorsConfiguration implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/time-deposits/**")
            .allowedOrigins("http://localhost:8081")
            .allowedMethods("GET", "POST");
    }
}
