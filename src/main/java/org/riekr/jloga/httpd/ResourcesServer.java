package org.riekr.jloga.httpd;

import java.io.InputStream;
import java.util.function.Consumer;

import fi.iki.elonen.NanoHTTPD;

class ResourcesServer {

	private final String      _package;
	private final LocalServer _server;

	/** port 0 should mean "a free port" */
	public ResourcesServer(String aPackage, boolean closeOnDisconnect) {
		if (aPackage == null || (aPackage = aPackage.trim()).isEmpty())
			throw new IllegalArgumentException("No package");
		aPackage = aPackage.replace('.', '/');
		if (!aPackage.startsWith("/"))
			aPackage = '/' + aPackage;
		_package = aPackage;
		_server = new LocalServer(Integer.getInteger("jloga." + getClass().getSimpleName() + ".port", 0), closeOnDisconnect ? this::stop : null) {
			@Override
			public Response serveResponse(IHTTPSession session) {
				String uri = session.getUri().replace(' ', '+');
				InputStream is = getClass().getResourceAsStream(_package + uri);
				if (is != null)
					return newChunkedResponse(Response.Status.OK, NanoHTTPD.getMimeTypeForFile(uri), is);
				System.err.println("404 " + uri);
				return null;
			}
		};
	}

	public final String getURL() {
		return _server.getURL();
	}

	public final void start(boolean daemon) {
		try {
			_server.start(-1, daemon);
			Browser.open(_server.getURL());
		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
	}

	public final void stop() {
		System.out.println("Server stopping");
		_server.stop();
	}

	public final void sendJS(String js, Consumer<String> completion) {
		_server.send(js, completion == null ? (res) -> {} : completion);
	}

}
