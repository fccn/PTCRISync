package tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.orcid.jaxb.model.common_rc2.Source;
import org.orcid.jaxb.model.common_rc2.Title;
import org.orcid.jaxb.model.record_rc2.ExternalID;
import org.orcid.jaxb.model.record_rc2.ExternalIDs;
import org.orcid.jaxb.model.record_rc2.Relationship;
import org.orcid.jaxb.model.record_rc2.Work;
import org.orcid.jaxb.model.record_rc2.WorkTitle;
import org.orcid.jaxb.model.record_rc2.WorkType;

import pt.ptcris.ORCIDClient;
import pt.ptcris.ORCIDException;
import pt.ptcris.PTCRISync;
import pt.ptcris.handlers.ProgressHandler;

import java.util.logging.Logger;

public class scenario14{
	private Profile orcid;
	private List<Work> localWorks;
	
	@Before
	public void setUpClass() throws Exception {
    	orcid = new Profile("0000-0002-4622-8073", "4a5a72ad-9215-4f5d-b698-06380da2f1d6", "PTCRIS");
    	orcid.progressHandler = orcid.handler();
    	
    	Work aux = prod0();
		
		ExternalID e2 = new ExternalID();
		e2.setRelationship(Relationship.SELF);
		e2.setValue("5");
		e2.setType("doi");
		
		ExternalIDs uids = aux.getExternalIdentifiers();
		uids.getExternalIdentifier().add(e2);
		aux.setWorkExternalIdentifiers(uids);
		
		localWorks = new LinkedList<Work>();
    	localWorks.add(aux);
    	
    	PTCRISync.export(orcid.orcidID, orcid.accessToken, localWorks, orcid.serviceSourceName, orcid.progressHandler);
    }

	@Test
	public void test() throws ORCIDException {
		List<Work> exportWorks = new LinkedList<Work>();
		exportWorks.add(prod0());
		exportWorks.add(prod1());
		
		PTCRISync.export(orcid.orcidID, orcid.accessToken, exportWorks, orcid.serviceSourceName, orcid.progressHandler);
	    
		orcid.progressHandler.setCurrentStatus(exportWorks.toString());
		orcid.progressHandler.done();
	}
	
	@After
	public void tearDownClass() throws Exception {
	    // Limpar o perfil;
	}

	
	private static Work prod0() {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		Title title2 = new Title();
		title2.setContent("Production 0"); 
		title.setTitle(title2);
		work.setWorkTitle(title);
		
		ExternalID e2 = new ExternalID();
		e2.setRelationship(Relationship.SELF);
		e2.setValue("5");
		e2.setType("handle");
		
		ExternalIDs uids = new ExternalIDs();
		uids.getExternalIdentifier().add(e2);
		
		work.setWorkExternalIdentifiers(uids);
		
		work.setWorkType(WorkType.CONFERENCE_PAPER);

		return work;
	}
	
	private static Work prod1() {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		Title title2 = new Title();
		title2.setContent("Production 1"); 
		title.setTitle(title2);
		work.setWorkTitle(title);
		
		ExternalID e3 = new ExternalID();
		e3.setRelationship(Relationship.SELF);
		e3.setValue("5");
		e3.setType("doi");
		
		ExternalIDs uids = new ExternalIDs();

		uids.getExternalIdentifier().add(e3);
		
		work.setWorkExternalIdentifiers(uids);
		
		work.setWorkType(WorkType.CONFERENCE_PAPER);

		return work;
	}

	
}
