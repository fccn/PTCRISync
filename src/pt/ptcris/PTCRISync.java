package pt.ptcris;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.orcid.jaxb.model.record_rc2.ExternalID;
import org.orcid.jaxb.model.record_rc2.Work;

public class PTCRISync {
	
	/**
	 * Export a list of works to an ORCID profile.
	 * 
	 * @param clientId The ORCID id of the profile to be updated.
	 * @param clientSecret The security token that grants update access to the profile.
	 * @param works The list of works to be exported (those marked as synced).
	 */
	public static void export(String clientID, String clientSecret, List<Work> works) {
		throw new UnsupportedOperationException("Yet!");
	}

	/**
	 * Discover new works in an ORCID profile.
	 * 
	 * @param clientId The ORCID id of the profile to be searched.
	 * @param clientSecret The security token that grants update access to the profile.
	 * @param works The full list of works in the local profile. In fact, for each work 
	 *              only the external identifiers are needed, so the remaining attributes may
	 *              be left null.
	 * @return The list of new works found in the ORCID profile.
	 */
	public static List<Work> importWorks(String clientID, String clientSecret, List<Work> works) {
		throw new UnsupportedOperationException("Yet!");
	}

	/**
	 * Discover updates to existing works.
	 * 
	 * @param clientId The ORCID id of the profile to be serched.
	 * @param clientSecret The security token that grants update access to the profile.
	 * @param works The list of works for which we wish to discover updates (those marked as synced).
	 *              For the moment, only new external identifiers will be found, so, for each work 
	 *              only the external identifiers are needed, so the remaining attributes may
	 *              be left null. Also the putcode attribute should be used to store the local key of
	 *              each work.
	 * @return The list of updated works. Only the works that have changes are returned. Also, for each
	 *         of them, only the attributes that changed are set. For the moment, only new external
	 *         identifiers will be returned.
	 */
	public static List<Work> importUpdates(String clientID, String clientSecret, List<Work> works) {
		throw new UnsupportedOperationException("Yet!");
	}
    
}
