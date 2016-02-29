package pt.ptcris;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.orcid.jaxb.model.record_rc2.ExternalID;
import org.orcid.jaxb.model.record_rc2.Work;

public interface PTCRISync {
	
	public void export(String clientID, String clientSecret, List<Work> works);

	public List<Work> importWorks(String clientID, String clientSecret, Set<ExternalID> uids);

	public List<ExternalID> importUIDs(String clientID, String clientSecret, Map<Object,List<ExternalID>> productions);
    
}
