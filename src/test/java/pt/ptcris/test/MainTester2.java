package pt.ptcris.test;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.common.RelationshipType;
import org.um.dsi.gavea.orcid.model.work.ExternalIdentifier;
import org.um.dsi.gavea.orcid.model.work.ExternalIdentifierType;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkExternalIdentifiers;
import org.um.dsi.gavea.orcid.model.work.WorkTitle;
import org.um.dsi.gavea.orcid.model.work.WorkType;

import pt.ptcris.ORCIDClient;
import pt.ptcris.PTCRISync;
import pt.ptcris.handlers.ProgressHandler;
import pt.ptcris.test.scenarios.ScenarioOrcidClient;

public class MainTester2 implements ProgressHandler {
	private static Logger logger = Logger.getLogger(MainTester2.class.getName());

	public static void main(String[] args) throws OrcidClientException, InterruptedException {
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter());
		handler.setLevel(Level.ALL);
		logger.setLevel(Level.ALL);
		logger.addHandler(handler);
		
		List<Work> works = new LinkedList<Work>();

		works.add(work0());
		works.add(work1());
		works.add(work2());
		MainTester2 progressHandler = new MainTester2();

		ORCIDClient client = ScenarioOrcidClient.getClientWork(1);

		List<Work> worksToImport = PTCRISync.importWorks(client, works, progressHandler);
		PTCRISync.export(client, works, progressHandler);
		
		progressHandler.setCurrentStatus(worksToImport.toString());
		progressHandler.done();
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
	
	private static Work work0() {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		title.setTitle("Yet Another Work Updated Once"); 
		work.setTitle(title);

		ExternalIdentifier e = new ExternalIdentifier();
		e.setRelationship(RelationshipType.SELF);
		e.setExternalIdentifierId("3000");
		e.setExternalIdentifierType(ExternalIdentifierType.DOI);

		WorkExternalIdentifiers uids = new WorkExternalIdentifiers();
		
		uids.getWorkExternalIdentifier().add(e);
		
		work.setExternalIdentifiers(uids);
		
		work.setType(WorkType.CONFERENCE_PAPER);

		return work;
	}
	
	private static Work work1() {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		title.setTitle("A Work Updated Once"); 
		work.setTitle(title);

		ExternalIdentifier e = new ExternalIdentifier();
		e.setRelationship(RelationshipType.SELF);
		e.setExternalIdentifierId("4000");
		e.setExternalIdentifierType(ExternalIdentifierType.EID);

		ExternalIdentifier e1 = new ExternalIdentifier();
		e1.setRelationship(RelationshipType.SELF);
		e1.setExternalIdentifierId("00001");
		e1.setExternalIdentifierType(ExternalIdentifierType.DOI);
		
		WorkExternalIdentifiers uids = new WorkExternalIdentifiers();
		
		uids.getWorkExternalIdentifier().add(e);
		uids.getWorkExternalIdentifier().add(e1);
		
		work.setExternalIdentifiers(uids);
		
		work.setType(WorkType.CONFERENCE_PAPER);

		return work;
	}
	
	private static Work work2() {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		title.setTitle("Another Work Updated Twice"); 
		work.setTitle(title);

		ExternalIdentifier e = new ExternalIdentifier();
		e.setRelationship(RelationshipType.SELF);
		e.setExternalIdentifierId(String.valueOf(System.currentTimeMillis())); // will always create
		e.setExternalIdentifierType(ExternalIdentifierType.DOI);

		WorkExternalIdentifiers uids = new WorkExternalIdentifiers();
		
		uids.getWorkExternalIdentifier().add(e);
		
		work.setExternalIdentifiers(uids);
		
		work.setType(WorkType.JOURNAL_ARTICLE);
		
		return work;
	}
	
}
