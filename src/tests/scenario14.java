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
import org.orcid.jaxb.model.common_rc2.Source;
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

public class scenario14{
	private Profile orcid;
	private List<Work> localWorks;
	private static ORCIDHelper orcidHelper;
	private static final String ORCID_URI = "https://api.sandbox.orcid.org/v2.0_rc2/";
	
	@Before
	public void setUpClass() throws Exception {
    	Work aux = prod0();
		
		ExternalID e2 = new ExternalID();
		e2.setRelationship(Relationship.SELF);
		e2.setValue("5");
		e2.setType("doi");
		
		ExternalIDs uids = aux.getExternalIdentifiers();
		uids.getExternalIdentifier().add(e2);
		aux.setWorkExternalIdentifiers(uids);
		
		orcid = new Profile("0000-0002-1062-9967", "8e144767-d061-463a-b48d-4a47ec38219b", "PTCRIS");
    	try {
    		orcidHelper = new ORCIDHelper(ORCID_URI, orcid.orcidID, orcid.accessToken);
    		orcidHelper.addWork(aux);
    	} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		localWorks = new LinkedList<Work>();
    	localWorks.add(prod0());
    	localWorks.add(prod1());
    }

	@Test
	public void test() throws ORCIDException {
		orcid.progressHandler = orcid.handler();
    	
		List<Work> exportWorks = new LinkedList<Work>();
		exportWorks.add(prod0());
		exportWorks.add(prod1());
		
		PTCRISync.export(orcid.orcidID, orcid.accessToken, exportWorks, orcid.serviceSourceName, orcid.progressHandler);
		 
		try {
			orcidHelper = new ORCIDHelper(ORCID_URI, orcid.orcidID, orcid.accessToken);
			List<WorkSummary> wks = orcidHelper.getSourcedWorkSummaries(orcid.serviceSourceName);
			assertEquals(localWorks.size(), wks.size());
			for(int i=0; i<wks.size(); i++)
				assertEquals(wks.get(i).getExternalIdentifiers(), localWorks.get(i).getExternalIdentifiers());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}		
		
		orcid.progressHandler.setCurrentStatus(exportWorks.toString());
		orcid.progressHandler.done();
	}
	
	@After
	public void tearDownClass() throws Exception {
		// Limpar o que foi inserido no perfil antes de executar o teste
		List<WorkSummary> wks = orcidHelper.getSourcedWorkSummaries("PTCRIS");
		for(WorkSummary aux : wks)
			orcidHelper.deleteWork(aux.getPutCode());
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
