package com.datn.pc_store;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:pc-store-test;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false",
		"app.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class PcStoreApplicationTests {

	@Test
	void contextLoads() {
	}

}
