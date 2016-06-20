package tests;

import static org.junit.Assert.*;

import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

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

import pt.ptcris.ORCIDException;
import pt.ptcris.ORCIDHelper;
import pt.ptcris.PTCRISync;

public class scenario1{
	private Profile haslab;
	private List<Work> localWorks;
	private static ORCIDHelper helper;
	private static final String ORCID_URI = "https://api.sandbox.orcid.org/v2.0_rc2/";
	
	@Before
	public void setUpClass() throws Exception {
		haslab = new Profile("0000-0003-3351-0229", "7b8b8632-62b8-4015-a34a-c03a297a2ddf", "HASLab, INESC TEC & University of Minho");
    	try {
			helper = new ORCIDHelper(ORCID_URI, haslab.orcidID, haslab.accessToken);
			helper.addWork(work0());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
    }
	
    // nmm: o test deve correr o import com o access token principal
    // nmm: depois deve testar the o resultado corresponde a um Work com um DOI, um EID e um Handle (a Creation no cenário)
	@Test
	public void test() throws ORCIDException {
    	Profile orcid = new Profile("0000-0003-3351-0229", "e49393b9-9494-4085-bf71-3c6bb03f3873", "PTCRIS");
    	orcid.progressHandler = orcid.handler();
    	
		localWorks = new LinkedList<Work>();
		
		// nmm: tinhamos falado de usar tokens diferentes para o "setUp" e para o "test"
		// nmm: caso contrário a coisa vai correr mal para quando houver IDs repetidos
		List<Work> worksToImport = PTCRISync.importWorks(orcid.orcidID, orcid.accessToken, localWorks, orcid.progressHandler);

		localWorks.addAll(worksToImport);
		
		orcid.progressHandler.setCurrentStatus(localWorks.toString());
		orcid.progressHandler.done();
		
		List<Work> expected = new LinkedList<Work>();
		expected.add(res1());
		
		//Verificar se os works s�o id�nticos (mesmos uids)
		assertEquals(expected.size(), worksToImport.size());
		assertEquals(expected.get(0).getExternalIdentifiers(), worksToImport.get(0).getExternalIdentifiers());
	}
	
	// nmm: o tearDown tem que apagar do perfil o Work com o access token secundário
	// nmm: o perfil "0000-0003-3351-0229" fica exatamente com um Work com um DOI e um Handle 
	@After
	public void tearDownClass() throws Exception {
		// Limpar o que foi inserido no perfil antes de executar o teste
		List<WorkSummary> wks = helper.getSourcedWorkSummaries(haslab.serviceSourceName);
		helper.deleteWork(wks.get(0).getPutCode());
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
	
	private static Work res1() {
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
		
		ExternalID e2 = new ExternalID();
		e2.setRelationship(Relationship.SELF);
		e2.setValue("1");
		e2.setType("handle");
		
		ExternalIDs uids = new ExternalIDs();
		
		uids.getExternalIdentifier().add(e);
		uids.getExternalIdentifier().add(e1);
		uids.getExternalIdentifier().add(e2);
		
		work.setWorkExternalIdentifiers(uids);
		
		work.setWorkType(WorkType.CONFERENCE_PAPER);

		return work;
	}
	
	

}
