package pt.ptcris;

import org.orcid.jaxb.model.record.summary_rc2.ActivitiesSummary;
import org.orcid.jaxb.model.record_rc2.Work;

public interface ORCIDClient {

	/**
	 * Retrieves a full work from the ORCID profile.
	 * 
	 * @param putCode The put-code of the work.
	 * @return The full work.
	 * @throws ORCIDException 
	 */
	public Work getWork(Long putCode) throws ORCIDException;

	/**
	 * Add a work to the ORCID profile.
	 * 
	 * @param work
	 *            The work to be added to the ORCID profile
	 * @return the put-code in the ORCID profile of the newly created work.            
	 * @throws ORCIDException 
	 */
	public Long addWork(Work work) throws ORCIDException;

	/**
	 * Delete a work from the ORCID profile.
	 * 
	 * @param putCode
	 *            The put-code of the work to be deleted.
	 * @throws ORCIDException 
	 */
	public void deleteWork(Long putCode) throws ORCIDException;

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
	public Work updateWork(Long putCode, Work work) throws ORCIDException;

	/**
	 * Retrieves every activity summary of the ORCID profile.
	 * 
	 * @return The activities summary of the ORCID profile.
	 * @throws ORCIDException 
	 */
	public ActivitiesSummary getActivitiesSummary() throws ORCIDException;

}