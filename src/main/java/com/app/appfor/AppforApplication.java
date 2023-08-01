package com.app.appfor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJms
@SpringBootApplication
public class AppforApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppforApplication.class, args);
	}

}
