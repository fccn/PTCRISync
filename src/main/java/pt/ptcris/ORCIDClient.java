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
import org.um.dsi.gavea.orcid.model.activities.Fundings;
import org.um.dsi.gavea.orcid.model.activities.Works;
import org.um.dsi.gavea.orcid.model.funding.Funding;
import org.um.dsi.gavea.orcid.model.funding.FundingSummary;
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
	 * @return the work summaries of the ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public Works getWorksSummary() throws OrcidClientException;

	/**
	 * Retrieves every funding summary from the ORCID profile.
	 *
	 * @return the funding summaries of the ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public Fundings getFundingsSummary() throws OrcidClientException;

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
	 * Retrieves a complete funding entry from the ORCID profile (as opposed to
	 * only its summary). Exceptions are embedded in the {@link PTCRISyncResult}.
	 *
	 * @param summary
	 *            the summary of the funding entry to be retrieved
	 * @return the complete funding entry
	 */
	public PTCRISyncResult getFunding(FundingSummary summary);

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
	 * Adds a new funding entry to the ORCID profile. Exceptions are embedded in
	 * the {@link PTCRISyncResult}.
	 *
	 * @param funding
	 *            the funding entry to be added to the ORCID profile
	 * @return the put-code assigned by ORCID to the newly created funding entry
	 */
	public PTCRISyncResult addFunding(Funding funding);

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
	 * Deletes a funding entry from the ORCID profile. Exceptions are embedded
	 * in the {@link PTCRISyncResult}.
	 *
	 * @param putcode
	 *            the put-code of the funding entry to be deleted
	 */
	public PTCRISyncResult deleteFunding(BigInteger putcode);

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
	 * Updates a funding entry in the ORCID profile. Exceptions are embedded in
	 * the {@link PTCRISyncResult}.
	 *
	 * @param putcode
	 *            the put-code of the funding entry to be updated
	 * @param funding
	 *            the new state of the funding entry
	 */
	public PTCRISyncResult updateFunding(BigInteger putcode, Funding funding);

	/**
	 * The number of worker threads that will be used to communicate with the
	 * ORCID API.
	 * 
	 * @return the number of ORCID worker threads
	 */
	public int threads();

}