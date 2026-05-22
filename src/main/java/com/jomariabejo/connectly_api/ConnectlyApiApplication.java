package com.jomariabejo.connectly_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class ConnectlyApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConnectlyApiApplication.class, args);
	}
}
