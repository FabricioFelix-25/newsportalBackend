package com.newsportal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaMigrationRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE articles ALTER COLUMN image_url TYPE TEXT;");
            jdbcTemplate.execute("ALTER TABLE articles ALTER COLUMN seo_image TYPE TEXT;");
            jdbcTemplate.execute("ALTER TABLE articles ALTER COLUMN title TYPE TEXT;");
            jdbcTemplate.execute("ALTER TABLE articles ALTER COLUMN subtitle TYPE TEXT;");
            System.out.println("Migracao de banco executada com sucesso: image_url, seo_image, title e subtitle alterados para TEXT.");
        } catch (Exception e) {
            System.out.println("Nota sobre migracao de banco: " + e.getMessage());
        }
    }
}
