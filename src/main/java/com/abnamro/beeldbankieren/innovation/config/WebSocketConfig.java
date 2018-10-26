package com.abnamro.beeldbankieren.innovation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.abnamro.beeldbankieren.innovation.signalingsocket.SignalingSocketHandler;

@Configuration
public class WebSocketConfig implements WebSocketConfigurer{

	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(signalingSocketHandler(), "/signal").setAllowedOrigins("*");	
		System.out.println("Inside registerWebSocketHandlers in websocketconfig");
	}
	
	private WebSocketHandler signalingSocketHandler() {
		System.out.println("Inside signaling socket handler");
      return new SignalingSocketHandler();
  }

}
