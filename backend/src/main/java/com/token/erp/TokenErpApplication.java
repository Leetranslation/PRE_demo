package com.token.erp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.token.erp.mapper")
public class TokenErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(TokenErpApplication.class, args);
    }
}
