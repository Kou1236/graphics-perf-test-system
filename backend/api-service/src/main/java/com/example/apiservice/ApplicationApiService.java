package com.example.apiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ApplicationApiService {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationApiService.class, args);
    }
}