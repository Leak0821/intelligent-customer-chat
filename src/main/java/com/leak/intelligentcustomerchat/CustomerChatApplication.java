package com.leak.intelligentcustomerchat;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.leak.intelligentcustomerchat.infrastructure.persistence")
public class CustomerChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerChatApplication.class, args);
    }
}
