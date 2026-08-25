package com.dssap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
@MapperScan("com.dssap.mybatis.mapper")
public class DsSapApplication {

    public static void main(String[] args) {
        SpringApplication.run(DsSapApplication.class, args);
    }
}
