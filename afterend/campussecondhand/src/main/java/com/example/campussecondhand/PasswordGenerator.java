package com.example.campussecondhand;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "123456";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("rawPassword=" + rawPassword);
        System.out.println("encodedPassword=" + encodedPassword);
        System.out.println("matches=" + encoder.matches(rawPassword, encodedPassword));
    }
}
