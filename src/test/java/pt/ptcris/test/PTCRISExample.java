package pt.ptcris.test;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import pt.ptcris.PTCRISyncResult;
import pt.ptcris.test.TestClients.Profile;

/**
 * A class that exemplifies the use of the various PTCRISync synchronization
 * procedures.
 */
public class PTCRISExample {
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws OrcidClientException, InterruptedException {
		// log the results to the console
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter());
		handler.setLevel(Level.ALL);
		Logger.getLogger(Tester.class.getName()).setLevel(Level.ALL);
		Logger.getLogger(Tester.class.getName()).addHandler(handler);
		Tester progressHandler = new Tester();
		
		progressHandler.setCurrentStatus(PTCRISExample.class.getName()+" start");

		// use one of the user profiles from the sandbox
		Profile profile = Profile.ONEVALIDWORKS;

		// fixture works, representing those from other sources
		List<Work> fixtureWorks = new LinkedList<Work>();
		
		// get a external source client for the user profile
		ORCIDClient externalClient = TestClients.getExternalClient(profile);
		
		// add some works with an external source as a fixture
		for (Work work : fixtureWorks)
			externalClient.addWork(work);

		// get a local CRIS client for the user profile
		ORCIDClient crisClient = TestClients.getCRISClient(profile);
		
		// the complete list of local works
		List<Work> localWorks = new LinkedList<Work>();
		localWorks.add(work0());
		localWorks.add(work1());
		localWorks.add(work2());
		
		// the list of local works that are to be exported
		List<Work> exportWorks = new LinkedList<Work>();
		exportWorks.add(work1());
		exportWorks.add(work2());

		// export the local works that are to by synchronized
		Map<BigInteger, PTCRISyncResult> exportResult = PTCRISync.export(crisClient, localWorks, progressHandler);
		// import new valid works found in the user profile
		List<Work> worksToImport = PTCRISync.importWorks(crisClient, localWorks, progressHandler);
		// import work updates found in the user profile
		List<Work> updatesToImport = PTCRISync.importUpdates(crisClient, localWorks, progressHandler);

		// count the new works found in the user profile
		int worksToImportCount = PTCRISync.importCounter(crisClient, localWorks, progressHandler);
		// import new invalid works found in the user profile
		Map<Work, Set<String>> worksToImportInvalid = PTCRISync.importInvalid(crisClient, localWorks, progressHandler);

		progressHandler.setCurrentStatus(PTCRISExample.class.getName()+" end");

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
		// avoids conflicts
		e.setExternalIdentifierId(String.valueOf(System.currentTimeMillis())); 
		
		e.setExternalIdentifierType(ExternalIdentifierType.DOI);

		WorkExternalIdentifiers uids = new WorkExternalIdentifiers();

		uids.getWorkExternalIdentifier().add(e);

		work.setExternalIdentifiers(uids);

		work.setType(WorkType.JOURNAL_ARTICLE);

		return work;
	}

}
