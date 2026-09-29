package com.aldo.event_pass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventPassApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventPassApplication.class, args);
	}

}
