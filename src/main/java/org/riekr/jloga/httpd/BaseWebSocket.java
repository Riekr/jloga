package org.riekr.jloga.httpd;

import java.io.IOException;
import java.net.SocketException;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

class BaseWebSocket extends NanoWSD.WebSocket {
	public BaseWebSocket(NanoHTTPD.IHTTPSession handshakeRequest) {
		super(handshakeRequest);
	}

	@Override
	protected void onOpen() {
		System.out.println("onOpen");
	}

	@Override
	protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
		System.out.println("onClose");
	}

	@Override
	protected void onMessage(NanoWSD.WebSocketFrame webSocketFrame) {
		System.out.println("onMessage " + webSocketFrame.getTextPayload());
	}

	@Override
	protected void onPong(NanoWSD.WebSocketFrame pong) {
		System.out.println("onPong");
	}

	@Override
	protected void onException(IOException exception) {
		if (exception instanceof SocketException)
			System.out.println("onException: " + exception.getLocalizedMessage());
		else {
			System.err.println("onException");
			exception.printStackTrace(System.err);
		}
	}
}
