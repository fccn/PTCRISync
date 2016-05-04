package tests;

import java.util.LinkedList;
import java.util.List;

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

import pt.ptcris.ORCIDException;
import pt.ptcris.PTCRISync;

public class scenario1{
	private Profile orcid;
	private List<Work> localWorks;
	
	// nmm: o perfil "0000-0003-3351-0229" deve ter exatamente um Work com um DOI e um Handle (o Work do User no cenário)
	// nmm: o setUp deve adicionar exatamente um Work, com o access token secundário, com um DOI e um EID, sendo que o DOI é o mesmo do User (o Work do Scopus no cenário)
    @Before
	public void setUpClass() throws Exception {
    	orcid = new Profile("0000-0003-3351-0229", "e49393b9-9494-4085-bf71-3c6bb03f3873", "PTCRIS");
    	orcid.progressHandler = orcid.handler();
    	
    	List<Work> orcidWorks = new LinkedList<Work>();
    	orcidWorks.add(work0());
    	
    	// nmm: aqui não podem usar o "export" para enviar os Works porque esse é um dos métodos do PTCRIS a ser testado
    	// nmm: usem antes o ORCIDClientImpl.addWork que comunica diretamente com o ORCID
    	PTCRISync.export(orcid.orcidID, orcid.accessToken, orcidWorks, orcid.serviceSourceName, orcid.progressHandler);
    }
	
    // nmm: o test deve correr o import com o access token principal
    // nmm: depois deve testar the o resultado corresponde a um Work com um DOI, um EID e um Handle (a Creation no cenário)
	@Test
	public void test() throws ORCIDException {
		localWorks = new LinkedList<Work>();
		
		// nmm: tinhamos falado de usar tokens diferentes para o "setUp" e para o "test"
		// nmm: caso contrário a coisa vai correr mal para quando houver IDs repetidos
		List<Work> worksToImport = PTCRISync.importWorks(orcid.orcidID, orcid.accessToken, localWorks, orcid.progressHandler);

		localWorks.addAll(worksToImport);
		
		orcid.progressHandler.setCurrentStatus(localWorks.toString());
		orcid.progressHandler.done();
		
		// nmm: aqui agora tem que ter um "assert" que testa se o "worksToImport" é idêntico ao definido pelo cenário
	}
	
	// nmm: o tearDown tem que apagar do perfil o Work com o access token secundário
	// nmm: o perfil "0000-0003-3351-0229" fica exatamente com um Work com um DOI e um Handle 
	@After
	public void tearDownClass() throws Exception {
		// Limpar o perfil;
		
		// nmm: têm mesmo que limpar o perfil senão os outros testes vão correr mal, por ter lá Works a mais
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
