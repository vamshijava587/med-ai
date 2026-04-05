package com.medai.vector;

import com.medai.vector.config.VectorServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(VectorServiceProperties.class)
public class VectorDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VectorDataServiceApplication.class, args);
    }
}
