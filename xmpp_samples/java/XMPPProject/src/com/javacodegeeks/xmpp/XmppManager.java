package com.javacodegeeks.xmpp;


import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;

public class XmppManager {
	
	private static final int packetReplyTimeout = 500; // millis
	
	private String server;
	private int port;
	
	private ConnectionConfiguration config;
	private XMPPConnection connection;
	
	private ChatManager chatManager;
	
	private Map<String, Chat> chatRegistry = new HashMap<String, Chat>();
	
	public XmppManager(String server, int port) {
		this.server = server;
		this.port = port;
	}
	
	public void init() throws XMPPException {
		System.setProperty("smack.debugEnabled", "true");
		XMPPConnection.DEBUG_ENABLED = true;
		
		System.out.println(String.format("Initializing connection to server %1$s port %2$d", server, port));

		SmackConfiguration.setPacketReplyTimeout(packetReplyTimeout);
		
		config = new ConnectionConfiguration(server, port);
		config.setSASLAuthenticationEnabled(true);
		config.setSecurityMode(SecurityMode.disabled);
		config.setCompressionEnabled(false);
		connection = new XMPPConnection(config);
		connection.connect();
		
		System.out.println("Connected: " + connection.isConnected());
		
		chatManager = connection.getChatManager();		
	}
	
	public void performLogin(String username, String password) throws XMPPException {
		if (connection!=null && connection.isConnected()) {
			SASLAuthentication.supportSASLMechanism("PLAIN", 0);
			System.out.println("Username is " + username);
			System.out.println("Password is " + password);
			try { Thread.sleep(5000); } catch (InterruptedException e ) {}
			connection.login(username, password);
		}
	}
	
	public void setStatus(boolean available, String status) {
		
		Presence.Type type = available? Type.available: Type.unavailable;
		Presence presence = new Presence(type);
		
		presence.setStatus(status);
		connection.sendPacket(presence);
		
	}
	
	public void addListener() {
		connection.addPacketListener(new PacketListener() {
			public void processPacket(Packet packet) {
				System.out.println("Got message from " + packet.getFrom());
				MessageListener dummy = new MessageListener() {
					public void processMessage(Chat ignored, Message ignored2) {}
				};
				Chat c;
				if (chatRegistry.containsKey(packet.getFrom())) {
					c = chatRegistry.get(packet.getFrom());
				} else {
					c = chatManager.createChat(packet.getFrom(), dummy);
					chatRegistry.put(packet.getFrom(), c);
				}
				try { c.sendMessage("ECHO: " + ((Message)packet).getBody()); }
				catch (XMPPException e) {}
			}
		}, new PacketTypeFilter(Message.class));
	}
	
	public void destroy() {
		if (connection!=null && connection.isConnected()) {
			connection.disconnect();
		}
	}
	
	
}
