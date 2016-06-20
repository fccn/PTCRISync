package tests;

import static org.junit.Assert.*;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.orcid.jaxb.model.common_rc2.Title;
import org.orcid.jaxb.model.record.summary_rc2.WorkSummary;
import org.orcid.jaxb.model.record_rc2.ExternalID;
import org.orcid.jaxb.model.record_rc2.ExternalIDs;
import org.orcid.jaxb.model.record_rc2.Relationship;
import org.orcid.jaxb.model.record_rc2.Work;
import org.orcid.jaxb.model.record_rc2.WorkTitle;
import org.orcid.jaxb.model.record_rc2.WorkType;

import pt.ptcris.ORCIDClient;
import pt.ptcris.ORCIDException;
import pt.ptcris.ORCIDHelper;
import pt.ptcris.PTCRISync;
import pt.ptcris.handlers.ProgressHandler;

import java.util.logging.Logger;

public class scenario4{
	private Profile haslab;
	private List<Work> localWorks;
	private static ORCIDHelper helper;
	private static final String ORCID_URI = "https://api.sandbox.orcid.org/v2.0_rc2/";

	@Before
	public void setUpClass() throws Exception {
		haslab = new Profile("0000-0002-4622-8073", "22f5abc8-ac82-4f89-a13b-428900d764ce", "HASLab, INESC TEC & University of Minho");
    	try {
			helper = new ORCIDHelper(ORCID_URI, haslab.orcidID, haslab.accessToken);
			helper.addWork(work0());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
    	
    	localWorks = new LinkedList<Work>();
    	localWorks.add(prod0());
    }

	@Test
	public void test() throws ORCIDException {
		Profile orcid = new Profile("0000-0002-4622-8073", "4a5a72ad-9215-4f5d-b698-06380da2f1d6", "PTCRIS");
    	orcid.progressHandler = orcid.handler();
    	
		List<Work> worksToImport = PTCRISync.importWorks(orcid.orcidID, orcid.accessToken, localWorks, orcid.progressHandler);
		
		orcid.progressHandler.setCurrentStatus(worksToImport.toString());
		orcid.progressHandler.done();
		
		List<Work> expected = new LinkedList<Work>();
		
		//Neste caso nenhum work foi importado
		assertEquals(expected.size(), worksToImport.size(), 0);
	}
	
	@After
	public void tearDownClass() throws Exception {
		// Limpar o que foi inserido no perfil antes de executar o teste
		List<WorkSummary> wks = helper.getSourcedWorkSummaries(haslab.serviceSourceName);
		for(WorkSummary aux : wks)
			helper.deleteWork(aux.getPutCode());
	}

	
	private static Work prod0() {
		Work work = new Work();
		WorkTitle title = new WorkTitle();
		Title title2 = new Title();
		title2.setContent("Production 0"); 
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
		
		ExternalID e2 = new ExternalID();
		e2.setRelationship(Relationship.SELF);
		e2.setValue("1");
		e2.setType("handle");
		
		ExternalID e3 = new ExternalID();
		e3.setRelationship(Relationship.SELF);
		e3.setValue("1");
		e3.setType("doi");
		
		ExternalIDs uids = new ExternalIDs();
		
		uids.getExternalIdentifier().add(e);
		uids.getExternalIdentifier().add(e1);
		uids.getExternalIdentifier().add(e2);
		uids.getExternalIdentifier().add(e3);
		
		work.setWorkExternalIdentifiers(uids);
		
		work.setWorkType(WorkType.CONFERENCE_PAPER);

		return work;
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
