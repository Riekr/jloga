package org.riekr.jloga.httpd;

@SuppressWarnings({"CommentedOutCode", "GrazieInspection"})
public class FinosPerspectiveServerTest {

	@Deprecated
	public static void main(String[] args) {
		try {
			// System.setProperty("jloga.FinosPerspectiveServer.port", "0");
			FinosPerspectiveServer server = new FinosPerspectiveServer();
			server.start(false);
			System.err.println("Server running at: " + server.getURL());

			server.load("Test", "A,B",
					"1,2",
					"3,4"
			);

			// server.load("Test", "A\tB",
			// 		"1\t2",
			// 		"3\t4"
			// );
			//
			// server.load("Test", new String[][] {
			// 		{"A", "B"},
			// 		{"1", "2"},
			// 		{"3", "4"},
			// });

			// still no webassembly suport
			// openJFX(url);

			// https://www.jsdelivr.com/package/npm/@finos/perspective?path=dist%2Fpkg%2Fesm
			// https://github.com/finos/perspective/search?l=JavaScript&q=wasm
			// https://github.com/cretz/asmble

		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	// private static void openJFX(String url) {
	// 	JFrame frame = new JFrame("Swing and JavaFX");
	// 	final JFXPanel fxPanel = new JFXPanel();
	// 	frame.add(fxPanel);
	// 	frame.setSize(300, 200);
	// 	frame.setVisible(true);
	// 	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	//
	// 	Platform.runLater(() -> {
	// 		// This method is invoked on the JavaFX thread
	// 		Scene scene = new Scene(new Group());
	// 		VBox root = new VBox();
	// 		final WebView browser = new WebView();
	// 		System.out.println(browser.getEngine().getUserAgent());
	// 		final WebEngine webEngine = browser.getEngine();
	// 		WebConsoleListener.setDefaultListener((webView, message, lineNumber, sourceId)
	// 				-> System.out.println(message + "[at " + lineNumber + "] " + sourceId)
	// 		);
	// 		webEngine.load(url);
	// 		root.getChildren().add(browser);
	// 		scene.setRoot(root);
	// 		fxPanel.setScene(scene);
	// 	});
	// }

}
