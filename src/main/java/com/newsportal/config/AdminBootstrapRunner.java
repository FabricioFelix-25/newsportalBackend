package com.newsportal.config;

import com.newsportal.model.User;
import com.newsportal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class AdminBootstrapRunner implements CommandLineRunner {
    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.enabled:true}")
    private boolean adminBootstrapEnabled;

    @Value("${app.bootstrap.admin.name:}")
    private String defaultAdminName;

    @Value("${app.bootstrap.admin.email:}")
    private String defaultAdminEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String defaultAdminPassword;

    @Override
    public void run(String... args) {
        if (!adminBootstrapEnabled) {
            return;
        }
        if (isBlank(defaultAdminName) || isBlank(defaultAdminEmail) || isBlank(defaultAdminPassword)) {
            System.out.println("Admin bootstrap skipped: set APP_BOOTSTRAP_ADMIN_NAME, APP_BOOTSTRAP_ADMIN_EMAIL and APP_BOOTSTRAP_ADMIN_PASSWORD.");
            return;
        }
        if (!STRONG_PASSWORD_PATTERN.matcher(defaultAdminPassword).matches()) {
            System.out.println("Admin bootstrap skipped: password must have at least 8 chars, one uppercase, one lowercase and one number.");
            return;
        }
        if (userRepository.existsByEmail(defaultAdminEmail)) {
            return;
        }

        User admin = new User();
        admin.setName(defaultAdminName);
        admin.setEmail(defaultAdminEmail);
        admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
        admin.setRole(User.Role.ADMIN);
        admin.setIsActive(true);
        admin.setAvatarUrl("");

        userRepository.save(admin);
        System.out.println("Admin bootstrap user created: " + defaultAdminEmail);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
