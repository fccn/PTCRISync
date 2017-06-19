/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
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
import org.um.dsi.gavea.orcid.model.common.ExternalId;
import org.um.dsi.gavea.orcid.model.common.ExternalIds;
import org.um.dsi.gavea.orcid.model.common.RelationshipType;
import org.um.dsi.gavea.orcid.model.work.Work;
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
		Map<BigInteger, PTCRISyncResult> exportResult = PTCRISync.exportWorks(crisClient, localWorks, progressHandler);
		// import new valid works found in the user profile
		List<Work> worksToImport = PTCRISync.importWorks(crisClient, localWorks, progressHandler);
		// import work updates found in the user profile
		List<Work> updatesToImport = PTCRISync.importWorkUpdates(crisClient, localWorks, progressHandler);

		// count the new works found in the user profile
		int worksToImportCount = PTCRISync.importWorkCounter(crisClient, localWorks, progressHandler);
		// import new invalid works found in the user profile
		Map<Work, Set<String>> worksToImportInvalid = PTCRISync.importInvalidWorks(crisClient, localWorks, progressHandler);

		progressHandler.setCurrentStatus(PTCRISExample.class.getName()+" end");

	}

	private static Work work0() {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		title.setTitle("Yet Another Work Updated Once");
		work.setTitle(title);

		ExternalId e = new ExternalId();
		e.setExternalIdRelationship(RelationshipType.SELF);
		e.setExternalIdValue("3000");
		e.setExternalIdType("DOI");

		ExternalIds uids = new ExternalIds();

		uids.getExternalId().add(e);

		work.setExternalIds(uids);

		work.setType(WorkType.CONFERENCE_PAPER);

		return work;
	}

	private static Work work1() {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		title.setTitle("A Work Updated Once");
		work.setTitle(title);

		ExternalId e = new ExternalId();
		e.setExternalIdRelationship(RelationshipType.SELF);
		e.setExternalIdValue("4000");
		e.setExternalIdType("EID");

		ExternalId e1 = new ExternalId();
		e1.setExternalIdRelationship(RelationshipType.SELF);
		e1.setExternalIdValue("00001");
		e1.setExternalIdType("DOI");

		ExternalIds uids = new ExternalIds();

		uids.getExternalId().add(e);
		uids.getExternalId().add(e1);

		work.setExternalIds(uids);

		work.setType(WorkType.CONFERENCE_PAPER);

		return work;
	}

	private static Work work2() {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		title.setTitle("Another Work Updated Twice");
		work.setTitle(title);

		ExternalId e = new ExternalId();
		e.setExternalIdRelationship(RelationshipType.SELF);
		// avoids conflicts
		e.setExternalIdValue(String.valueOf(System.currentTimeMillis())); 
		
		e.setExternalIdType("DOI");

		ExternalIds uids = new ExternalIds();

		uids.getExternalId().add(e);

		work.setExternalIds(uids);

		work.setType(WorkType.JOURNAL_ARTICLE);

		return work;
	}

}
