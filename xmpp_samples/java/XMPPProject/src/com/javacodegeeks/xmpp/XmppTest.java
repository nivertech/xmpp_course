package com.javacodegeeks.xmpp;

public class XmppTest {
	
	public static void main(String[] args) throws Exception {
		
		String username = "echobot";
		String password = "mypass";
		
		XmppManager xmppManager = new XmppManager("xmpp.nivertech.com", 5222);
		
		xmppManager.init();
		xmppManager.performLogin(username, password);
		xmppManager.setStatus(true, "Hello everyone");
		xmppManager.addListener();
		
		boolean isRunning = true;
		
		while (isRunning) {
			Thread.sleep(50);
		}
		
		xmppManager.destroy();
		
	}

}
