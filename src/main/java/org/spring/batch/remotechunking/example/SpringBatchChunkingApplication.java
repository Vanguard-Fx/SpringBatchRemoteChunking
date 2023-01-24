package org.spring.batch.remotechunking.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringBatchChunkingApplication {

  public static void main(String[] args) {
    SpringApplication.run(SpringBatchChunkingApplication.class, args);
  }
}
