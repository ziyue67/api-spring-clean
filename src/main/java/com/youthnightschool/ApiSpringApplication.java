package com.youthnightschool;

import com.youthnightschool.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ApiSpringApplication {

  public static void main(String[] args) {
    SpringApplication.run(ApiSpringApplication.class, args);
  }
}
