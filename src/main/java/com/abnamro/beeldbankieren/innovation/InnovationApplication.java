package com.abnamro.beeldbankieren.innovation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication
@EnableWebSocket
@ComponentScan("com.abnamro.beeldbankieren.innovation")
public class InnovationApplication {

	public static void main(String[] args) {
		SpringApplication.run(InnovationApplication.class, args);
		System.out.println("Inside main");
	}
}
