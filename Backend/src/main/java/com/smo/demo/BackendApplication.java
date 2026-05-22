package com.smo.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
		 System.out.println("==============================================");
	        System.out.println("  SmartOS Monitor is running on port 8080   ");
	        System.out.println("  Dashboard: http://localhost:8080/index.html ");
	        System.out.println("==============================================");
	}

}
