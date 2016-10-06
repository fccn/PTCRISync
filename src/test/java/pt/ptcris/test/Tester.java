package pt.ptcris.test;

import java.util.logging.Level;
import java.util.logging.Logger;

import pt.ptcris.handlers.ProgressHandler;

public class Tester implements ProgressHandler {
	private static Logger logger = Logger.getLogger(Tester.class.getName());

	@Override
	public void setProgress(int progress) {
		logger.fine("Current progress: " + progress + "%");
	}

	@Override
	public void setCurrentStatus(String message) {
		logger.fine("Task: " + message);
	}

	@Override
	public void sendError(String message) {
		logger.log(Level.SEVERE, "ERROR: " + message);
	}

	@Override
	public void done() {
		logger.fine("Done.");
	}
	
	
}
