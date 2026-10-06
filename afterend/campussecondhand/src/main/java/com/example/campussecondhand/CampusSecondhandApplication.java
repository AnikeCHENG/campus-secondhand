package com.example.campussecondhand;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.example.campussecondhand.repository")
// 启用定时任务：缺失时 @Scheduled 方法会被静默忽略（不报错也不执行）
@EnableScheduling
public class CampusSecondhandApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusSecondhandApplication.class, args);
    }
}




