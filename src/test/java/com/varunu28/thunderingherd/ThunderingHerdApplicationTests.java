package com.varunu28.thunderingherd;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ThunderingHerdApplicationTests {

    @Test
    void contextLoads() {
    }
}
