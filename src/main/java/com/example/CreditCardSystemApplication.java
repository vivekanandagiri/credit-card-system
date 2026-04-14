package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CreditCardSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(CreditCardSystemApplication.class, args);
	}

}
