package org.example.orbit.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Применяем ко всем эндпоинтам (api/satellites и т.д.)
                        .allowedOrigins("http://localhost:3000") // Явно разрешаем твой фронтенд
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Разрешаем все базовые HTTP-методы
                        .allowedHeaders("*") // Разрешаем любые заголовки
                        .allowCredentials(true); // Разрешаем передачу куки и токенов авторизации (если понадобятся)
            }
        };
    }
}