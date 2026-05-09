package com.jomariabejo.connectly_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class ConnectlyApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConnectlyApiApplication.class, args);
	}


	@GetMapping("/hello")
	public String getHello() {
		return "Hello, Connectly API is running!";
	}
}
