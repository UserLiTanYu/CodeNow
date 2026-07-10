package com.codenow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.codenow.mapper")
public class CodenowBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodenowBackendApplication.class, args);
    }

}
