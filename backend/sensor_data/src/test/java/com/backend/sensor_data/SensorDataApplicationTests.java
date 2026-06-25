package com.backend.sensor_data;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Disabled because environment variables (SPRING_DATASOURCE_URL) are not available during Maven build")
class SensorDataApplicationTests {

	@Test
	void contextLoads() {
	}

}
