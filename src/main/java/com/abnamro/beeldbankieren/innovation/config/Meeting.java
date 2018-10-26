package com.abnamro.beeldbankieren.innovation.config;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import lombok.ToString;

@ToString
public class Meeting {
	
	 private String meetingId;
	   
	 private Set<WebSocketSession> peers = new HashSet<WebSocketSession>();

	    public Meeting(String meetingId) {
	        this.meetingId = meetingId;
	    }

	    public String getMeetingId() {
	        return meetingId;
	    }

	    public void addPeer(WebSocketSession session) {
	        peers.add(session);
	    }

	    public Set<WebSocketSession> getPeers() {
	        return peers;
	    }

	    public void broadcast(TextMessage msg) throws IOException {
	        for (WebSocketSession session : peers) {
	            if (session.isOpen()) {
	                session.sendMessage(msg);
	            }
	        }
	    }

}
