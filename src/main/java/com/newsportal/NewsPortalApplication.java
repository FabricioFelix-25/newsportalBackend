package com.newsportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class NewsPortalApplication {
    public static void main(String[] args) {
        SpringApplication.run(NewsPortalApplication.class, args);
    }
}