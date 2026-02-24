package com.example.viewer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.viewer.mapper")
public class GshmdApplication {

    public static void main(String[] args) {
        SpringApplication.run(GshmdApplication.class, args);
    }

}
