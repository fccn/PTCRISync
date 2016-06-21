package pt.ptcris.test;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.ORCIDClient;
import pt.ptcris.ORCIDException;
import pt.ptcris.PTCRISync;
import pt.ptcris.handlers.ProgressHandler;
import pt.ptcris.test.scenarios.ScenarioOrcidClient;

public class MainTester implements ProgressHandler {
	private static Logger logger = Logger.getLogger(MainTester.class.getName());

	public static void main(String[] args) throws ORCIDException, OrcidClientException {
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter());
		handler.setLevel(Level.ALL);
		logger.setLevel(Level.ALL);
		logger.addHandler(handler);

		List<Work> works = new LinkedList<Work>();
		MainTester progressHandler = new MainTester();

		ORCIDClient client = ScenarioOrcidClient.getClientWork(1);
		
		PTCRISync.export(client, works, progressHandler);
		List<Work> worksToImport = PTCRISync.importWorks(client, works, progressHandler);
		List<Work> worksToUpdate = PTCRISync.importUpdates(client, works, progressHandler);
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
