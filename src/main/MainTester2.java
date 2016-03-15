package main;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.orcid.jaxb.model.common_rc2.Title;
import org.orcid.jaxb.model.record_rc2.ExternalID;
import org.orcid.jaxb.model.record_rc2.ExternalIDs;
import org.orcid.jaxb.model.record_rc2.Relationship;
import org.orcid.jaxb.model.record_rc2.Work;
import org.orcid.jaxb.model.record_rc2.WorkTitle;
import org.orcid.jaxb.model.record_rc2.WorkType;

import pt.ptcris.ORCIDException;
import pt.ptcris.PTCRISync;
import pt.ptcris.handlers.ProgressHandler;

public class MainTester2 implements ProgressHandler {
	private static Logger logger = Logger.getLogger(MainTester2.class.getName());

    private static final String orcidID = "0000-0002-5507-2082";
    private static final String accessTokenRead = "f93c2bb5-04b1-4b5f-b597-ca411d3b6561";
    private static final String accessTokenUpdate = "93de27e3-3e63-401e-856d-dc5461fbe2ad";

	private static final String serviceSourceName = "Local CRIS";

	public static void main(String[] args) throws ORCIDException {
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

		List<Work> worksToImport = PTCRISync.importWorks(orcidID, accessTokenRead, works, progressHandler);
		PTCRISync.export(orcidID, accessTokenRead, accessTokenUpdate, works, serviceSourceName, progressHandler);
		
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
		Title title2 = new Title();
		title2.setContent("Yet Another Work Updated Once"); 
		title.setTitle(title2);
		work.setWorkTitle(title);

		ExternalID e = new ExternalID();
		e.setRelationship(Relationship.SELF);
		e.setValue("3000");
		e.setType("doi");

		ExternalIDs uids = new ExternalIDs();
		
		uids.getExternalIdentifier().add(e);
		
		work.setWorkExternalIdentifiers(uids);
		
		work.setWorkType(WorkType.CONFERENCE_PAPER);

		return work;
	}
	
	private static Work work1() {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		Title title2 = new Title();
		title2.setContent("A Work Updated Once"); 
		title.setTitle(title2);
		work.setWorkTitle(title);

		ExternalID e = new ExternalID();
		e.setRelationship(Relationship.SELF);
		e.setValue("3000");
		e.setType("doi");

		ExternalIDs uids = new ExternalIDs();
		
		uids.getExternalIdentifier().add(e);
		
		work.setWorkExternalIdentifiers(uids);
		
		work.setWorkType(WorkType.CONFERENCE_PAPER);

		return work;
	}
	
	private static Work work2() {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		Title title2 = new Title();
		title2.setContent("Another Work Updated Twice"); 
		title.setTitle(title2);
		work.setWorkTitle(title);

		ExternalID e = new ExternalID();
		e.setRelationship(Relationship.SELF);
		e.setValue(String.valueOf(System.currentTimeMillis())); // will always create
		e.setType("doi");

		ExternalIDs uids = new ExternalIDs();
		
		uids.getExternalIdentifier().add(e);
		
		work.setWorkExternalIdentifiers(uids);
		
		work.setWorkType(WorkType.CONFERENCE_PAPER);
		
		return work;
	}
	
}
