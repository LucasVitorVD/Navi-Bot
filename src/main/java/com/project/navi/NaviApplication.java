package com.project.navi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NaviApplication {

	public static void main(String[] args) {
		SpringApplication.run(NaviApplication.class, args);
	}

}
