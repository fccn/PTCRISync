package tests;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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

public class scenario1{
	private Profile orcid;
	private List<Work> localWorks;
	
    @Before
	public void setUpClass() throws Exception {
    	orcid = new Profile("0000-0003-3351-0229", "e49393b9-9494-4085-bf71-3c6bb03f3873", "PTCRIS");
    	orcid.progressHandler = orcid.handler();
    	
    	List<Work> orcidWorks = new LinkedList<Work>();
    	orcidWorks.add(work0());
    	
    	PTCRISync.export(orcid.orcidID, orcid.accessToken, orcidWorks, orcid.serviceSourceName, orcid.progressHandler);
    }
	
	@Test
	public void test() throws ORCIDException {
		localWorks = new LinkedList<Work>();
		List<Work> worksToImport = PTCRISync.importWorks(orcid.orcidID, orcid.accessToken, localWorks, orcid.progressHandler);

		localWorks.addAll(worksToImport);
		
		orcid.progressHandler.setCurrentStatus(localWorks.toString());
		orcid.progressHandler.done();
	}
	
	@After
	public void tearDownClass() throws Exception {
		// Limpar o perfil;
    }

	
	private static Work work0() {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		Title title2 = new Title();
		title2.setContent("Work0"); 
		title.setTitle(title2);
		work.setWorkTitle(title);

		ExternalID e = new ExternalID();
		e.setRelationship(Relationship.SELF);
		e.setValue("0");
		e.setType("eid");

		ExternalID e1 = new ExternalID();
		e1.setRelationship(Relationship.SELF);
		e1.setValue("0");
		e1.setType("doi");
		
		ExternalIDs uids = new ExternalIDs();
		
		uids.getExternalIdentifier().add(e);
		uids.getExternalIdentifier().add(e1);
		
		work.setWorkExternalIdentifiers(uids);
		
		work.setWorkType(WorkType.CONFERENCE_PAPER);

		return work;
	}

}
