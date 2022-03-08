package org.riekr.jloga.httpd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import fi.iki.elonen.NanoWSD;

abstract class LocalServer extends NanoWSD {

	private final ExecutorService _completionExecutor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r);
		t.setDaemon(true);
		return t;
	});

	private       BaseWebSocket                 _socket;
	private       List<String>                  _pendingMessages    = new ArrayList<>();
	private final Map<String, Consumer<String>> _pendingCompletions = new ConcurrentHashMap<>();
	private       int                           _seq                = 0;
	private final Runnable                      _onClose;

	public LocalServer(int port, Runnable onClose) {
		super("127.0.0.1", port);
		_onClose = onClose;
	}

	@Override
	protected final boolean useGzipWhenAccepted(Response r) {
		return false;
	}

	@Override
	protected synchronized final WebSocket openWebSocket(IHTTPSession handshake) {
		if (_socket != null) {
			try {
				_socket.close(WebSocketFrame.CloseCode.NormalClosure, "reconnect", false);
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
		return _socket = new BaseWebSocket(handshake) {
			@Override
			protected void onOpen() {
				System.out.println("JS WebSocket opened: " + handshake.getUri());
				if (_pendingMessages != null) {
					for (String message : _pendingMessages)
						LocalServer.this.send(message);
					_pendingMessages = null;
				}
			}

			@Override
			protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
				System.out.println("JS WebSocket closed: " + code + " \"" + reason + "\" remote=" + initiatedByRemote);
				if (_onClose != null)
					_onClose.run();
			}

			@Override
			protected void onMessage(WebSocketFrame webSocketFrame) {
				String msg = webSocketFrame.getTextPayload();
				// System.out.println("JS WebSocket received: " + msg);
				String id = msg.substring(0, 8);
				String result = msg.substring(8);
				_completionExecutor.execute(() -> _pendingCompletions.remove(id).accept(result));
			}
		};

	}

	@Override
	public final Response serve(IHTTPSession session) {
		return super.serve(session);
	}

	@Override
	protected Response serveHttp(IHTTPSession session) {
		String uri = session.getUri();
		if (uri.equals("/favicon.ico"))
			return newChunkedResponse(Response.Status.OK, "image/png", getClass().getResourceAsStream("/org/riekr/jloga/icon.png"));
		if ("websocket".equalsIgnoreCase(session.getHeaders().get("upgrade")))
			return super.serve(session);
		Response response = serveResponse(session);
		return response == null ? super.serveHttp(session) : response;
	}

	public final String getURL() {
		return "http://127.0.0.1:" + getListeningPort() + "/index.html";
	}

	public abstract Response serveResponse(IHTTPSession session);

	private void send(String message) {
		try {
			// System.out.println("JS WebSocket send: " + message.length() + " bytes");
			_socket.send(message);
		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
	}

	public synchronized void send(String message, Consumer<String> completion) {
		String seq = String.format("%08X", _seq++);
		_pendingCompletions.put(seq, completion);
		if (_pendingMessages == null)
			send(seq + message);
		else
			_pendingMessages.add(seq + message);
	}

	@Override
	public void stop() {
		super.stop();
		_completionExecutor.shutdownNow();
	}
}
