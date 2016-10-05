package pt.ptcris;

import java.math.BigInteger;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.activities.ActivitiesSummary;

/**
 * Interface that encapsulates the communication with the ORCID client for a
 * specified user profile.
 * 
 * Currently focuses on managing {@link org.um.dsi.gavea.orcid.model.work.Work
 * works}.
 */
public interface ORCIDClient {

	/**
	 * Returns the Member API client id that will commit the changes (i.e., the
	 * works' source).
	 * 
	 * @return The client id.
	 */
	public String getClientId();

	/**
	 * Retrieves a complete work from the ORCID profile (as opposed to only its
	 * summary).
	 * 
	 * @param putCode
	 *            The put-code of the work.
	 * @return The complete work.
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails.
	 */
	public Work getWork(BigInteger putCode) throws OrcidClientException;

	/**
	 * Adds a new work to the ORCID profile.
	 * 
	 * @param work
	 *            The work to be added to the ORCID profile
	 * @return the put-code in the ORCID profile of the newly created work.
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails.
	 */
	public BigInteger addWork(Work work) throws OrcidClientException;

	/**
	 * Deletes a work from the ORCID profile.
	 * 
	 * @param putCode
	 *            The put-code of the work to be deleted.
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails.
	 */
	public void deleteWork(BigInteger putCode) throws OrcidClientException;

	/**
	 * Updates a work in the ORCID profile.
	 * 
	 * @param putCode
	 *            The put-code of the work to be updated.
	 * @param work
	 *            the new state of the work.
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails.
	 */
	public void updateWork(BigInteger putCode, Work work) throws OrcidClientException;

	/**
	 * Retrieves every activity summary of the ORCID profile.
	 * 
	 * @return The activities summary of the ORCID profile.
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails.
	 */
	public ActivitiesSummary getActivitiesSummary() throws OrcidClientException;

}