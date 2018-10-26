package com.abnamro.beeldbankieren.innovation.signalingsocket;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import com.abnamro.beeldbankieren.innovation.config.Meeting;
import com.abnamro.beeldbankieren.innovation.config.Payload;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import com.abnamro.beeldbankieren.innovation.config.Meeting;
import com.abnamro.beeldbankieren.innovation.config.Payload;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SignalingSocketHandler extends AbstractWebSocketHandler {
	
	  private static final String MSG_TYPE_JOIN = "join";
	  private static final String MSG_TYPE_CREATED = "created";
	  private static final String MSG_TYPE_REMOTE_PEER_JOINING = "remotePeerJoining";
	  private static final String MSG_TYPE_BROADCAST = "broadcast";
	  private static final String MSG_TYPE_ICE = "ice";

	  private static final String MSG_TYPE_OFFER = "offer";
	  private static final String MSG_TYPE_ANSWER = "answer";

	    private ObjectMapper objectMapper = new ObjectMapper();
	    Map<String, Meeting> meetings = new HashMap<String, Meeting>();
	
	@Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
        System.out.println("New client connected: " + webSocketSession.getRemoteAddress() + " hash " + webSocketSession.getRemoteAddress().hashCode());
    }
	
	@Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		System.out.println("inside handle text message");
		System.out.println("handleTextMessage:"+ message.getPayload());
		Payload payload = objectMapper.readValue(message.getPayload(), Payload.class);
		String meetingId = "12345";
		
		  if (MSG_TYPE_JOIN.equalsIgnoreCase(payload.getType())) {
			  System.out.println("payload type :"+payload.getType());
//	          System.out.println("{} joining channel: {}"+session+""+ meetingId);
	            if (session.isOpen()) {
	            	String id = payload.getMeeting() == null ? meetingId : payload.getMeeting();
	                Meeting meeting = meetings.get(id);
	                if (meeting == null) {
	                	System.out.println("if meeting is null");
	                	meeting = new Meeting(id);
	                	meetings.put(id, meeting);
	                	System.out.println("meetings info :"+meetings);
	                	Payload msg = new Payload(MSG_TYPE_CREATED, id, null, payload.getName());
	                	System.out.println("msg if:"+msg);
	                	session.sendMessage(new TextMessage( objectMapper.writeValueAsString(msg)));
	                	
	                } else {
	                    Payload msg = new Payload(MSG_TYPE_REMOTE_PEER_JOINING, id, null, payload.getName());
	                    System.out.println("msg else:"+msg);
	                    session.sendMessage(new TextMessage( objectMapper.writeValueAsString(msg)));

	                    // Send to others in meeting that peer has joined
						for (WebSocketSession s : meeting.getPeers()) {
							s.sendMessage(new TextMessage( objectMapper.writeValueAsString(msg)));
						}
	                }
	               
	                meeting.addPeer(session);

//	                Payload msg = new Payload(MSG_TYPE_BROADCAST, id, "broadcast: client " + payload.getName()+" " +session + " joined channel " + id, payload.getName());
//	                System.out.println("msg after else:"+msg);
//	                meeting.broadcast(new TextMessage( objectMapper.writeValueAsString(msg)));
	            }
		  } else if(MSG_TYPE_ICE.equalsIgnoreCase(payload.getType())) {
			  String id = payload.getMeeting() == null ? meetingId : payload.getMeeting();
			  Meeting meeting = meetings.get(id);
			  System.out.println("size of peers :"+meeting.getPeers().size());
			  for (WebSocketSession s : meeting.getPeers()) {
				  if (!session.equals(s)) {
					 Payload msg= new Payload(MSG_TYPE_ICE, id, payload.getData(), payload.getName());
					 s.sendMessage(new TextMessage( objectMapper.writeValueAsString(msg)));
				  }
			  }
		  } else if(MSG_TYPE_OFFER.equalsIgnoreCase(payload.getType()) || MSG_TYPE_ANSWER.equalsIgnoreCase(payload.getType())) {
			  // Relay the offer/answer to the others in the meeting
			  String id = payload.getMeeting() == null ? meetingId : payload.getMeeting();
			  Meeting meeting = meetings.get(id);
			  for (WebSocketSession s : meeting.getPeers()) {
				  if (!session.equals(s)) {
					  s.sendMessage(new TextMessage( objectMapper.writeValueAsString(payload)));
				  }
			  }
		  }
	}
	
	  @Override
	    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus closeStatus) throws Exception {
	        System.out.println("client connection closed: "+ webSocketSession.getRemoteAddress());
	    }
	
}
