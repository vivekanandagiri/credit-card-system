package com.example.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.entity.User;
import com.example.enums.UserRole;
import com.example.repository.UserRepository;

@Configuration
public class DataInitializer {
	@Bean
	CommandLineRunner createDefaultAdmin(UserRepository userRepository,PasswordEncoder passwordEncoder) {
		return args->{
			String adminEmail="admin@gmail.com";
			
			//check if Admin exists
			if(userRepository.findByEmail(adminEmail).isEmpty()) {
				User admin = new User();
				
				admin.setEmail(adminEmail);
				admin.setMobileNumber("9999999999");
				admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
				admin.setRole(UserRole.ADMIN);
				admin.setActive(true);
				admin.setLocked(false);
				admin.setFailedAttempts(0);
				admin.setLastLoginAt(LocalDateTime.now());

				
				userRepository.save(admin);
				
				System.out.println("Default ADMIN created !!!!");
			}
			else {
				System.out.println("Default Admin is already Exist");
				
			}
			
		};
	}

}
