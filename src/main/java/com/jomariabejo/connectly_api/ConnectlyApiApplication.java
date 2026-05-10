package com.jomariabejo.connectly_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@EnableScheduling
@EnableCaching
public class ConnectlyApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConnectlyApiApplication.class, args);
	}


	@GetMapping("/hello")
	public String getHello() {
		return "Hello, Connectly API is running!";
	}
}
