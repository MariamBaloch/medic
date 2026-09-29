package com.ga.medic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MedicApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicApplication.class, args);
    }

}
