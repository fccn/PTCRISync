package pt.ptcris;

import java.math.BigInteger;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.activities.ActivitiesSummary;

public interface ORCIDClient {
	

	/**
	 * Retrieves a full work from the ORCID profile.
	 * 
	 * @param putCode The put-code of the work.
	 * @return The full work.
	 * @throws ORCIDException 
	 */
	public Work getWork(BigInteger putCode) throws OrcidClientException;

	/**
	 * Add a work to the ORCID profile.
	 * 
	 * @param work
	 *            The work to be added to the ORCID profile
	 * @return the put-code in the ORCID profile of the newly created work.            
	 * @throws ORCIDException 
	 */
	public String addWork(Work work) throws OrcidClientException;

	/**
	 * Delete a work from the ORCID profile.
	 * 
	 * @param putCode
	 *            The put-code of the work to be deleted.
	 * @throws ORCIDException 
	 */
	public void deleteWork(BigInteger putCode) throws OrcidClientException;

	/**
	 * Update a work in the ORCID profile.
	 * 
	 * @param putCode
	 *           The put-code of the work to be updated.
	 * @param work
	 * 			 the new state of the work.
	 * @return the updated work as represented in the ORCID profile.       
	 * @throws ORCIDException 
	 */
	public void updateWork(BigInteger putCode, Work work) throws OrcidClientException;

	/**
	 * Retrieves every activity summary of the ORCID profile.
	 * 
	 * @return The activities summary of the ORCID profile.
	 * @throws ORCIDException 
	 */
	public ActivitiesSummary getActivitiesSummary() throws OrcidClientException;
	
	/**
	 * Returns the ORCID's client id
	 * 
	 * @return String with cliend id.
	 */
	public String getClientId();	

}