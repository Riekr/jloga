package org.riekr.jloga.utils;

import static java.awt.EventQueue.invokeAndWait;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.Main;
import org.riekr.jloga.search.SearchException;
import org.riekr.jloga.ui.MainPanel;

public class PopupUtils {

	public static <E extends Throwable> E popupError(@NotNull E err) {
		return popupError(null, err);
	}

	public static void popupError(String message) {
		popupError(message, "Error");
	}

	public static <E extends Throwable> E popupError(String message, E err) {
		return popupError(message, err == null ? "Error" : err.getMessage(), err);
	}

	public static void popupError(String message, String title) {
		popupError(message, title, null);
	}

	public static <E extends Throwable> E popupError(String message, String title, E err) {
		MainPanel main = Main.getMain();
		if (err != null) {
			if (err instanceof SearchException) {
				if (((SearchException)err).userHasAlreadyBeenNotified.get())
					return err;
			}
			if (message == null || message.isEmpty())
				message = err.getLocalizedMessage();
			else
				message += "\n(" + err.getLocalizedMessage() + ')';
		} else
			System.err.println(message);
		if (main != null) {
			if (err != null)
				err.printStackTrace(System.err);
			if (EventQueue.isDispatchThread())
				JOptionPane.showMessageDialog(main, message, title, JOptionPane.ERROR_MESSAGE);
			else {
				final String popupMessage = message;
				try {
					invokeAndWait(() -> JOptionPane.showMessageDialog(main, popupMessage, title, JOptionPane.ERROR_MESSAGE));
				} catch (InterruptedException | InvocationTargetException e) {
					e.printStackTrace(System.err);
				}
			}
		} else
			System.err.println("ERROR: " + message.replace("\n", ""));
		return err;
	}

	public static void popupWarning(String message, String title) {
		MainPanel main = Main.getMain();
		if (main != null) {
			if (EventQueue.isDispatchThread())
				JOptionPane.showMessageDialog(main, message, title, JOptionPane.WARNING_MESSAGE);
			else {
				try {
					invokeAndWait(() -> JOptionPane.showMessageDialog(main, message, title, JOptionPane.WARNING_MESSAGE));
				} catch (InterruptedException | InvocationTargetException e) {
					e.printStackTrace(System.err);
				}
			}
		} else
			System.err.println("WARNING: " + message);
	}


	private PopupUtils() {}
}
