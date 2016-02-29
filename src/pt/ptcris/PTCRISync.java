package pt.ptcris;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.orcid.jaxb.model.record_rc2.ExternalID;
import org.orcid.jaxb.model.record_rc2.Work;

public class PTCRISync {
	
	/**
	 * Export a list of works to an ORCID profile, trying to update
	 * existing works of the smae source when possible.
	 * 
	 * @param clientId The ORCID id of the profile to be updated.
	 * @param clientSecret The security token that grants update access to the profile.
	 * @param works The list of works to be updated.
	 */
	public static void export(String clientID, String clientSecret, List<Work> works) {
		throw new UnsupportedOperationException("Yet!");
	}

	/**
	 * Discover new works in an ORCID profile.
	 * 
	 * @param clientId The ORCID id of the profile to be searched.
	 * @param clientSecret The security token that grants update access to the profile.
	 * @param uids The set of external identifiers currently known in the local profile.
	 * @return The list of new works found in the ORCID profile.
	 */
	public static List<Work> importWorks(String clientID, String clientSecret, Set<ExternalID> uids) {
		throw new UnsupportedOperationException("Yet!");
	}

	/**
	 * Discover new external identifiers for existing works.
	 * 
	 * @param clientId The ORCID id of the profile to be serched.
	 * @param clientSecret The security token that grants update access to the profile.
	 * @param uids A map with the external identifiers known in the local profile, grouped by work key.
	 *             It will be updated to countain only the new uids for each key.
	 */
	public static void importUIDs(String clientID, String clientSecret, Map<Object,Set<ExternalID>> uids) {
		throw new UnsupportedOperationException("Yet!");
	}
    
}
