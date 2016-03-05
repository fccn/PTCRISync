package main;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.orcid.jaxb.model.record_rc2.Work;

import pt.ptcris.PTCRISync;
import pt.ptcris.handlers.ProgressHandler;

public class MainTester implements ProgressHandler {
	private static Logger logger = Logger.getLogger(MainTester.class.getName());

	private static final String orcidID = "0000-0000-0000-0000";
	private static final String accessToken = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
	private static final String serviceSourceName = "Local CRIS";

	public static void main(String[] args) {
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter());
		handler.setLevel(Level.ALL);
		logger.setLevel(Level.ALL);
		logger.addHandler(handler);

		List<Work> works = new LinkedList<Work>();
		MainTester progressHandler = new MainTester();

		PTCRISync.export(orcidID, accessToken, works, serviceSourceName, progressHandler);
		List<Work> worksToImport = PTCRISync.importWorks(orcidID, accessToken, works, progressHandler);
		List<Work> worksToUpdate = PTCRISync.importUpdates(orcidID, accessToken, works, progressHandler);
	}

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
