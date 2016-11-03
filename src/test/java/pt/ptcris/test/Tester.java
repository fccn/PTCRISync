package pt.ptcris.test;

import java.util.logging.Level;
import java.util.logging.Logger;

import pt.ptcris.handlers.ProgressHandler;

/**
 * A simple implementation of the progress handler to log the execution of
 * PTCRISync procedures.
 */
public class Tester implements ProgressHandler {
	private static Logger logger = Logger.getLogger(Tester.class.getName());

	/** {@inheritDoc} */
	@Override
	public void setProgress(int progress) {
		logger.fine("Current progress: " + progress + "%");
	}

	/** {@inheritDoc} */
	@Override
	public void setCurrentStatus(String message) {
		logger.fine("Task: " + message);
	}

	/** {@inheritDoc} */
	@Override
	public void sendError(String message) {
		logger.log(Level.SEVERE, "ERROR: " + message);
	}

	/** {@inheritDoc} */
	@Override
	public void done() {
		logger.fine("Done.");
	}

}