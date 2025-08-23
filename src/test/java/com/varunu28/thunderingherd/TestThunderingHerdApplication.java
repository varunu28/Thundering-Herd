package com.varunu28.thunderingherd;

import org.springframework.boot.SpringApplication;

public class TestThunderingHerdApplication {

    public static void main(String[] args) {
        SpringApplication.from(ThunderingHerdApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
