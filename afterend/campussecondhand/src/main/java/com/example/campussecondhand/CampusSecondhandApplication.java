package com.example.campussecondhand;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.campussecondhand.repository")
public class CampusSecondhandApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusSecondhandApplication.class, args);
    }
}




