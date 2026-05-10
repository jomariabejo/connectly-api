package com.jomariabejo.connectly_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import com.jomariabejo.connectly_api.support.JwtTestInitializer;

@SpringBootTest
@ContextConfiguration(initializers = JwtTestInitializer.class)
class ConnectlyApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
