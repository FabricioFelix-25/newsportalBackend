
package com.newsportal.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "Ff122015"; // A senha que você quer criptografar
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("Senha criptografada para 'Ff122015': " + encodedPassword);
    }
}