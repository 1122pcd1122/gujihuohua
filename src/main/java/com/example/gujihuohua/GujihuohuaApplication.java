package com.example.gujihuohua;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.gujihuohua.mapper")
public class GujihuohuaApplication {
    public static void main(String[] args) {
        SpringApplication.run(GujihuohuaApplication.class, args);
    }

}
