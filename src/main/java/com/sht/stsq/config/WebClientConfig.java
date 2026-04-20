package com.sht.stsq.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient aiWebClient() {
        // 配置 Python AI 边车的基础访问地址
        return WebClient.builder()
                .baseUrl("http://127.0.0.1:8000")
                .build();
    }
}