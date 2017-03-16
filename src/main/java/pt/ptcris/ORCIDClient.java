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
import java.util.Map;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.activities.ActivitiesSummary;
import org.um.dsi.gavea.orcid.model.activities.Works;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

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
	 * summary). Exceptions are embedded in the {@link PTCRISyncResult}.
	 *
	 * @param summary
	 *            the summary of the work to be retrieved
	 * @return the complete work
	 */
	public PTCRISyncResult getWork(WorkSummary summary);

	/**
	 * Retrieves a list of complete work from the ORCID profile (as opposed to
	 * only their summaries). Exceptions are embedded in the
	 * {@link PTCRISyncResult}. This should generate a single API call
	 * internally.
	 *
	 * @param summaries
	 *            the summaries of the works to be retrieved
	 * @return the complete works
	 */
	public Map<BigInteger, PTCRISyncResult> getWorks(List<WorkSummary> summaries);

	/**
	 * Adds a new work to the ORCID profile. Exceptions are embedded in the
	 * {@link PTCRISyncResult}.
	 *
	 * @param work
	 *            the work to be added to the ORCID profile
	 * @return the put-code assigned by ORCID to the newly created work
	 */
	public PTCRISyncResult addWork(Work work);

	/**
	 * Adds a list of new works to the ORCID profile. Exceptions are embedded in
	 * the {@link PTCRISyncResult}. This should generate a single API call
	 * internally.
	 *
	 * @param works
	 *            the works to be added to the ORCID profile
	 * @return the put-codes assigned by ORCID to each of the newly created
	 *         works
	 */
	public List<PTCRISyncResult> addWorks(List<Work> works);

	/**
	 * Deletes a work from the ORCID profile. Exceptions are embedded in
	 * the {@link PTCRISyncResult}.
	 *
	 * @param putcode
	 *            the put-code of the work to be deleted
	 */
	public PTCRISyncResult deleteWork(BigInteger putcode);

	/**
	 * Updates a work in the ORCID profile. Exceptions are embedded in
	 * the {@link PTCRISyncResult}.
	 *
	 * @param putcode
	 *            the put-code of the work to be updated
	 * @param work
	 *            the new state of the work
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