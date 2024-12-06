package com.xcu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.xcu.mapper")
@EnableAsync
@EnableAspectJAutoProxy(exposeProxy = true)
public class EasyPanApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyPanApplication.class, args);
    }

}