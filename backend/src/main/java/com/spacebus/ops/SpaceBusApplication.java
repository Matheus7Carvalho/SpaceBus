package com.spacebus.ops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpaceBusApplication {

    /** Fuso da operação piloto. LocalDateTime em toda a aplicação segue este fuso, inclusive em container (que vem em UTC). */
    public static final String FUSO_PADRAO = "America/Araguaina";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(System.getenv().getOrDefault("APP_FUSO_HORARIO", FUSO_PADRAO)));
        SpringApplication.run(SpaceBusApplication.class, args);
    }
}
