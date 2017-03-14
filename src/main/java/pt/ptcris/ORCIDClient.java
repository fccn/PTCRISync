/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris;

import java.math.BigInteger;
import java.util.List;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.activities.ActivitiesSummary;
import org.um.dsi.gavea.orcid.model.activities.Works;
import org.um.dsi.gavea.orcid.model.work.Work;

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
	 * @return the client id
	 */
	public String getClientId();

	/**
	 * Retrieves a complete work from the ORCID profile (as opposed to only its
	 * summary).
	 *
	 * @param putcode
	 *            the put-code of the work
	 * @return the complete work
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public Work getWork(BigInteger putcode) throws OrcidClientException;

	/**
	 * Retrieves a list of complete works from the ORCID profile (as opposed to
	 * only its summary).
	 *
	 * @param putcodes
	 *            the put-code of the works
	 * @return the complete works
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public List<Work> getWorks(List<BigInteger> putcodes) throws OrcidClientException;

	/**
	 * Adds a new work to the ORCID profile.
	 *
	 * @param work
	 *            the work to be added to the ORCID profile
	 * @return the put-code assigned by ORCID to the newly created work
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public PTCRISyncResult addWork(Work work);

	/**
	 * Adds a set of new works to the ORCID profile. This should generate a
	 * single API call internally.
	 *
	 * @param works
	 *            the works to be added to the ORCID profile
	 * @return the put-codes assigned by ORCID to each of the newly created works
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public List<PTCRISyncResult> addWorks(List<Work> works);

	/**
	 * Deletes a work from the ORCID profile.
	 *
	 * @param putcode
	 *            the put-code of the work to be deleted
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public PTCRISyncResult deleteWork(BigInteger putcode);

	/**
	 * Updates a work in the ORCID profile.
	 *
	 * @param putcode
	 *            the put-code of the work to be updated
	 * @param work
	 *            the new state of the work
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public PTCRISyncResult updateWork(BigInteger putcode, Work work);

	/**
	 * Retrieves every activity summary from the ORCID profile.
	 *
	 * @return the activities summary of the ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public ActivitiesSummary getActivitiesSummary() throws OrcidClientException;

	/**
	 * Retrieves every work summary from the ORCID profile.
	 *
	 * @return the works summary of the ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public Works getWorksSummary() throws OrcidClientException;
	
	/**
	 * The number of worker threads that will be used to communicate with the
	 * ORCID API.
	 * 
	 * @return the number of ORCID worker threads
	 */
	public int threads();

}