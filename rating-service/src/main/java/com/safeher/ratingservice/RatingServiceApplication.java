package com.safeher.ratingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableMongoAuditing
@EnableAsync
public class RatingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RatingServiceApplication.class, args);
    }
}
