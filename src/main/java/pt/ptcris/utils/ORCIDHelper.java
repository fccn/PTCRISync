/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.activities.FundingGroup;
import org.um.dsi.gavea.orcid.model.activities.WorkGroup;
import org.um.dsi.gavea.orcid.model.common.ClientId;
import org.um.dsi.gavea.orcid.model.common.ElementSummary;
import org.um.dsi.gavea.orcid.model.common.ExternalId;
import org.um.dsi.gavea.orcid.model.common.ExternalIds;
import org.um.dsi.gavea.orcid.model.common.RelationshipType;
import org.um.dsi.gavea.orcid.model.funding.Funding;
import org.um.dsi.gavea.orcid.model.funding.FundingSummary;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;
import org.um.dsi.gavea.orcid.model.work.WorkType;

import pt.ptcris.ORCIDClient;
import pt.ptcris.PTCRISyncResult;
import pt.ptcris.exceptions.InvalidWorkException;
import pt.ptcris.handlers.ProgressHandler;

/**
 * An helper to simplify the use of the low-level ORCID
 * {@link pt.ptcris.ORCIDClient client}.
 * 
 * Provides support for asynchronous communication with ORCID
 * although it is only active for GET requests due to resource
 * limitations.
 */
public class ORCIDHelper {

	public static final String INVALID_EXTERNALIDENTIFIERS = "ExternalIdentifiers";
	public static final String INVALID_TITLE = "Title";
	public static final String INVALID_PUBLICATIONDATE = "PublicationDate";
	public static final String INVALID_YEAR = "Year";
	public static final String INVALID_TYPE = "Type";
	public static final String INVALID_ORGANIZATION = "Organization";
	public static final String OVERLAPPING_EIDs = "OverlappingEIDs";
	
	public enum EIdType {
		OTHER_ID("other-id"), AGR("agr"), ARXIV("arxiv"), ASIN("asin"), 
				BIBCODE("bibcode"), CBA("cba"), CIT("cit"), CTX("ctx"), DOI("doi"), 
				EID("eid"), ETHOS("ethos"), HANDLE("handle"), HIR("hir"), ISBN("isbn"), 
				ISSN("issn"), JFM("jfm"), JSTOR("jstor"), LCCN("lccn"), MR("mr"), 
				OCLC("oclc"), OL("ol"), OSTI("osti"), PAT("pat"), PMC("pmc"), 
				PMID("pmid"), RFC("rfc"), SOURCE_WORK_ID("source-work-id"), 
				SSRN("ssrn"), URI("uri"), URN("urn"), WOSUID("wosuid"), ZBL("zbl"),
				CIENCIAIUL("cienciaiul"), GRANT_NUMBER("grant-number"); // TODO: should be divided between Works and Funding

		public final String value;

		private EIdType(String value) {
			this.value = value;
		}
	}

	private int bulk_size_add = 100;
	private int bulk_size_get = 50;
	
	private static final Logger _log = LoggerFactory.getLogger(ORCIDHelper.class);

	/**
	 * The client used to communicate with ORCID. Defines the ORCID user profile
	 * being managed and the Member API id being user to source works.
	 */
	public final ORCIDClient client;

	private ExecutorService executor;

	/**
	 * Initializes the helper with a given ORCID client.
	 *
	 * @param orcidClient
	 *            the ORCID client
	 */
	public ORCIDHelper(ORCIDClient orcidClient) {
		this.client = orcidClient;
		if (client.threads() > 1) executor = Executors.newFixedThreadPool(client.threads());
	}

	/**
	 * Retrieves the entire set of work summaries from the set ORCID profile
	 * that have at least an external identifier set. Merges each ORCID group
	 * into a single summary, following {@link #groupToWork}.
	 *
	 * @return the set of work summaries in the set ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public List<WorkSummary> getAllWorkSummaries() throws OrcidClientException {
		_log.debug("[getSummaries]");
		
		final List<WorkSummary> workSummaryList = new LinkedList<WorkSummary>();
		final List<WorkGroup> workGroupList = client.getWorksSummary().getGroup();
		for (WorkGroup group : workGroupList)
			workSummaryList.add(groupToWork(group));
		return workSummaryList;
	}
	
	/**
	 * Retrieves the entire set of funding summaries from the set ORCID profile
	 * that have at least an external identifier set. Merges each ORCID group
	 * into a single summary, following {@link #groupToWork}.
	 *
	 * @return the set of funding summaries in the set ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public List<FundingSummary> getAllFundingSummaries() throws OrcidClientException {
		_log.debug("[getSummaries]");
		
		final List<FundingSummary> fundSummaryList = new LinkedList<FundingSummary>();
		final List<FundingGroup> fundGroupList = client.getFundingsSummary().getGroup();
		for (FundingGroup group : fundGroupList)
			fundSummaryList.add(groupToFunding(group));
		return fundSummaryList;
	}

	/**
	 * Retrieves the entire set of work summaries in the ORCID profile whose
	 * source is the Member API id defined in the ORCID client.
	 *
	 * @return the set of work summaries in the ORCID profile for the defined
	 *         source
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public List<WorkSummary> getSourcedWorkSummaries() throws OrcidClientException {
		final String sourceClientID = client.getClientId();
		
		_log.debug("[getSourcedSummaries] " + sourceClientID);
		
		final List<WorkSummary> workSummaryList = new LinkedList<WorkSummary>();
		final List<WorkGroup> workGroupList = client.getWorksSummary().getGroup();
		
		for (WorkGroup workGroup : workGroupList) {
			for (WorkSummary workSummary : workGroup.getWorkSummary()) {
				final ClientId workClient = workSummary.getSource().getSourceClientId();
				// may be null is entry added by the user
				if (workClient != null && workClient.getUriPath().equals(sourceClientID)) {
					workSummaryList.add(workSummary);
				}
			}
		}
		return workSummaryList;
	}
	
	public List<FundingSummary> getSourcedFundingSummaries() throws OrcidClientException {
		final String sourceClientID = client.getClientId();
		
		_log.debug("[getSourcedSummaries] " + sourceClientID);
		
		final List<FundingSummary> workSummaryList = new LinkedList<FundingSummary>();
		final List<FundingGroup> workGroupList = client.getFundingsSummary().getGroup();
		
		for (FundingGroup workGroup : workGroupList) {
			for (FundingSummary workSummary : workGroup.getFundingSummary()) {
				final ClientId workClient = workSummary.getSource().getSourceClientId();
				// may be null is entry added by the user
				if (workClient != null && workClient.getUriPath().equals(sourceClientID)) {
					workSummaryList.add(workSummary);
				}
			}
		}
		return workSummaryList;
	}

	/**
	 * Gets a full work from an ORCID profile and adds it to a callback map. The
	 * resulting work contains every external identifier set in the input work
	 * summary, because the summary resulted from the merging of a group, but
	 * the retrieved full work is a single work. It also clears the put-code,
	 * since at this level they represent the local identifier. If possible, the
	 * process is asynchronous. If the process fails, the exception is embedded in 
	 * a failed {@link PTCRISyncResult}.
	 *
	 * @see ORCIDClient#getWork(BigInteger)
	 * 
	 * @param mergedWork
	 *            the work summary representing a merged group
	 * @param cb
	 *            the callback object
	 * @throws NullPointerException
	 *             if the merged work is null
	 */
	public void getFullWork(WorkSummary mergedWork, Map<BigInteger, PTCRISyncResult> cb)
			throws NullPointerException {
		if (mergedWork == null) throw new NullPointerException("Can't get null work.");
				
		if (client.threads() > 1 && cb != null) {
			final ORCIDGetWorker worker = new ORCIDGetWorker(mergedWork,client, cb, _log);
			executor.execute(worker);
		} else {
			PTCRISyncResult fullWork;
			
			_log.debug("[getFullWork] " + mergedWork.getPutCode());
			fullWork = client.getWork(mergedWork);
			
			cb.put(mergedWork.getPutCode(), fullWork);
		}
	}

	/**
	 * Gets a list of full works from an ORCID profile and adds them to a
	 * callback map. The resulting works contain every external identifier set
	 * in the input work summaries, because the latter resulted from the merging
	 * of a group, but the retrieved full works are a single work. It also
	 * clears the put-codes, since at this level they represent the local
	 * identifier. If possible, the process is asynchronous. If the list is not
	 * a singleton, a bulk request will be performed. If the process fails of
	 * for any work, the exceptions are embedded in failed
	 * {@link PTCRISyncResult}.
	 *
	 * @see ORCIDClient#getWorks(List)
	 * 
	 * @param mergedWork
	 *            the work summaries representing the merged groups
	 * @param cb
	 *            the callback object
	 * @throws NullPointerException
	 *             if the merged work is null
	 */
	public void getFullWorks(List<WorkSummary> mergedWorks, Map<BigInteger, PTCRISyncResult> cb, ProgressHandler handler)
			throws OrcidClientException, NullPointerException {
		if (mergedWorks == null) throw new NullPointerException("Can't get null work.");
		_log.debug("[getFullWorks] " + mergedWorks.size());
		if (handler != null) handler.setCurrentStatus("ORCID_GET_ITERATION");

		if (client.threads() > 1 && cb != null) {
			for (int i = 0; i < mergedWorks.size();) {
				int progress = (int) ((double) i / mergedWorks.size() * 100);
				if (handler != null) handler.setProgress(progress);
				if (bulk_size_get > 1) {
					List<WorkSummary> putcodes = new ArrayList<WorkSummary>();
					for (int j = 0; j < bulk_size_get && i < mergedWorks.size(); j++) {
						putcodes.add(mergedWorks.get(i));
						i++;
					}
					final ORCIDBulkGetWorker worker = new ORCIDBulkGetWorker(putcodes, client, cb, _log);
					executor.execute(worker);
				} else {
					final ORCIDGetWorker worker = new ORCIDGetWorker(mergedWorks.get(i), client, cb, _log);
					executor.execute(worker);
					i++;
				}
			}
		} else {
			Map<BigInteger, PTCRISyncResult> fullWorks = new HashMap<BigInteger, PTCRISyncResult>();
			for (int i = 0; i < mergedWorks.size();) {
				int progress = (int) ((double) i / mergedWorks.size() * 100);
				if (handler != null) handler.setProgress(progress);
				if (bulk_size_get > 1) {
					List<WorkSummary> putcodes = new ArrayList<WorkSummary>();
					for (int j = 0; j < bulk_size_get && i < mergedWorks.size(); j++) {
						putcodes.add(mergedWorks.get(i));
						i++;
					}
					fullWorks.putAll(client.getWorks(putcodes));
				} else {
					fullWorks.put(mergedWorks.get(i).getPutCode(), client.getWork(mergedWorks.get(i)));
					i++;
				}
			}
			cb.putAll(fullWorks);
		}

	}
	
	public void getFullFundings(List<FundingSummary> mergedWorks, Map<BigInteger, PTCRISyncResult> cb, ProgressHandler handler)
			throws OrcidClientException, NullPointerException {
		if (mergedWorks == null) throw new NullPointerException("Can't get null work.");
		_log.debug("[getFullWorks] " + mergedWorks.size());
		if (handler != null) handler.setCurrentStatus("ORCID_GET_ITERATION");

		if (client.threads() > 1 && cb != null) {
			for (int i = 0; i < mergedWorks.size();) {
				int progress = (int) ((double) i / mergedWorks.size() * 100);
				if (handler != null) handler.setProgress(progress);
			
				final ORCIDGetWorker2 worker = new ORCIDGetWorker2(mergedWorks.get(i), client, cb, _log);
				executor.execute(worker);
				i++;
			}
		} else {
			Map<BigInteger, PTCRISyncResult> fullWorks = new HashMap<BigInteger, PTCRISyncResult>();
			for (int i = 0; i < mergedWorks.size();) {
				int progress = (int) ((double) i / mergedWorks.size() * 100);
				if (handler != null) handler.setProgress(progress);
				fullWorks.put(mergedWorks.get(i).getPutCode(), client.getFunding(mergedWorks.get(i)));
				i++;
			}
			cb.putAll(fullWorks);
		}

	}

	/**
	 * Synchronously adds a work to an ORCID profile. The OK result includes the
	 * newly assigned put-code. If communication fails, error message is
	 * included in the result.
	 *
	 * @see ORCIDClient#addWork(Work)
	 * 
	 * @param work
	 *            the new work to be added
	 * @return the result of the ORCID call
	 * @throws NullPointerException
	 *             if the work is null
	 */
	private PTCRISyncResult addWork(Work work) throws NullPointerException {
		if (work == null) throw new NullPointerException("Can't add null work.");
		
		_log.debug("[addWork] " + getWorkTitle(work));
	
		// remove any put-code otherwise ORCID will throw an error
		final Work clone = ORCIDHelper.clone(work);
		clone.setPutCode(null);
	
		return client.addWork(clone);
	}
	
	/**
	 * Synchronously adds a list of works to an ORCID profile. A list of results
	 * is returned, one for each input work. The OK result includes the newly
	 * assigned put-code. If communication fails, error message is included in
	 * the result. If the overall communication fails, the result is replicated
	 * for each input.
	 *
	 * @see ORCIDClient#addWorks(List)
	 * 
	 * @param works
	 *            the new works to be added
	 * @return the results of the ORCID call for each input work
	 * @throws NullPointerException
	 *             if the work is null
	 */
	private List<PTCRISyncResult> addWorks(Collection<Work> works) throws NullPointerException {
		if (works == null) throw new NullPointerException("Can't add null works.");
		
		_log.debug("[addWorks] " + works.size());
	
		List<Work> clones = new ArrayList<Work>();
		// remove any put-code otherwise ORCID will throw an error
		for (Work work : works) {
			final Work clone = ORCIDHelper.clone(work);
			clone.setPutCode(null);
			clones.add(clone);
		}
	
		return client.addWorks(clones);
	}
	
	/**
	 * Synchronously adds a list of works to an ORCID profile, either through
	 * atomic or bulk calls. A list of results is returned, one for each input
	 * work. The OK result includes the newly assigned put-code. If
	 * communication fails, error message is included in the result. If the
	 * overall communication fails, the result is replicated for each input.
	 *
	 * @param works
	 *            the new works to be added
	 * @return the results of the ORCID call for each input work
	 * @throws NullPointerException
	 *             if the work is null
	 */
	public List<PTCRISyncResult> addWorks(List<Work> localWorks, ProgressHandler handler) throws NullPointerException {
		List<PTCRISyncResult> res = new ArrayList<PTCRISyncResult>();
		if (handler != null) handler.setCurrentStatus("ORCID_ADDING_WORKS");

		for (int c = 0; c != localWorks.size();) {
			int progress = (int) ((double) c / localWorks.size() * 100);
			if (handler!=null) handler.setProgress(progress);
	
			if (bulk_size_add > 1) {
				List<Work> tmp = new ArrayList<Work>();
				for (int j = 0; j < bulk_size_add && c < localWorks.size(); j++) {
					tmp.add(localWorks.get(c));
					c++;
				}
				res.addAll(this.addWorks(tmp));
			} 
			else {
				Work localWork = localWorks.get(c);
				res.add(this.addWork(localWork));
				c++;
			}
		}
		return res;
	}
	
	
	private PTCRISyncResult addFunding(Funding work) throws NullPointerException {
		if (work == null) throw new NullPointerException("Can't add null work.");
		
		_log.debug("[addWork] " + getWorkTitle(work));
	
		// remove any put-code otherwise ORCID will throw an error
		final Funding clone = ORCIDHelper.clone(work);
		clone.setPutCode(null);
	
		return client.addFunding(clone);
	}
	
	public List<PTCRISyncResult> addFundings(List<Funding> localWorks, ProgressHandler handler) throws NullPointerException {
		List<PTCRISyncResult> res = new ArrayList<PTCRISyncResult>();
		if (handler != null) handler.setCurrentStatus("ORCID_ADDING_WORKS");

		for (int c = 0; c != localWorks.size();) {
			int progress = (int) ((double) c / localWorks.size() * 100);
			if (handler!=null) handler.setProgress(progress);
	
			Funding localWork = localWorks.get(c);
			res.add(this.addFunding(localWork));
			c++;
		}
		return res;
	}
	
	/**
	 * Synchronously updates a work in an ORCID profile.
	 * 
	 * @see ORCIDClient#updateWork(BigInteger, Work)
	 * 
	 * @param remotePutcode
	 *            the put-code of the remote ORCID work that will be updated
	 * @param updatedWork
	 *            the new state of the work that will be updated
	 * @return the result of the ORCID call
	 * @throws NullPointerException
	 *             if either parameter is null
	 */
	public PTCRISyncResult updateWork(BigInteger remotePutcode, Work updatedWork)
			throws NullPointerException {
		if (remotePutcode == null || updatedWork == null) 
			throw new NullPointerException("Can't update null work.");

		_log.debug("[updateWork] " + remotePutcode);

		final Work clone = ORCIDHelper.clone(updatedWork);
		// set the remote put-code
		clone.setPutCode(remotePutcode);

		return client.updateWork(remotePutcode, clone);
	}

	/**
	 * Deletes the entire set of work summaries in the ORCID profile whose
	 * source is the Member API id defined in the ORCID client.
	 *
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public void deleteAllSourcedWorks() throws OrcidClientException {
		_log.debug("[deleteSourced] " + client.getClientId());

		final List<WorkSummary> workSummaryList = getSourcedWorkSummaries();
	
		for (WorkSummary workSummary : workSummaryList) {
			deleteWork(workSummary.getPutCode());
		}
	}

	public void deleteAllSourcedFundings() throws OrcidClientException {
		_log.debug("[deleteSourced] " + client.getClientId());

		final List<FundingSummary> workSummaryList = getSourcedFundingSummaries();
	
		for (FundingSummary workSummary : workSummaryList) {
			deleteFunding(workSummary.getPutCode());
		}
	}

	/**
	 * Synchronously deletes a work in an ORCID profile.
	 * 
	 * @see ORCIDClient#deleteWork(BigInteger)
	 * 
	 * @param putcode the remote put-code of the work to be deleted
	 * @throws NullPointerException
	 *             if the put-code is null
	 */
	public PTCRISyncResult deleteWork(BigInteger putcode) 
			throws NullPointerException {
		if (putcode == null) 
			throw new NullPointerException("Can't delete null work.");

		_log.debug("[deleteWork] " + putcode);
	
		return client.deleteWork(putcode);
	}
	
	public PTCRISyncResult deleteFunding(BigInteger putcode) 
			throws NullPointerException {
		if (putcode == null) 
			throw new NullPointerException("Can't delete null work.");

		_log.debug("[deleteWork] " + putcode);
	
		return client.deleteFunding(putcode);
	}

	/**
	 * Waits for all active asynchronous workers communicating with ORCID to
	 * finish (if multi-threading is enabled, otherwise it is always true).
	 *
	 * @return whether the workers finished before the timeout
	 * @throws InterruptedException
	 *             if the process was interrupted
	 */
	public boolean waitWorkers() throws InterruptedException {
		if (client.threads() <= 1) return true;
		
		executor.shutdown();
		final boolean timeout = executor.awaitTermination(100, TimeUnit.SECONDS);
		executor = Executors.newFixedThreadPool(client.threads());
		return timeout;
	}
	
	
	/**
	 * Retrieves the local key of an activity, currently assumed to be stored in
	 * the put-code field.
	 *
	 * @param act
	 *            the activity from which to get the local key
	 * @return the local key
	 * @throws NullPointerException
	 *             if the activity is null
	 */
	public static BigInteger getActivityLocalKey(ElementSummary act) throws NullPointerException {
		if (act == null)
			throw new NullPointerException("Can't get local key.");

		return act.getPutCode();
	}
	
	/**
	 * Retrieves the local key of an activity, currently assumed to be stored in
	 * the put-code field. If empty, returns the default value.
	 *
	 * @param act
	 *            the activity from which to get the local key
	 * @param defaultValue
	 *            a default value in case the put-code is empty
	 * @return the local key
	 * @throws NullPointerException
	 *             if the activity is null
	 */
	public static BigInteger getActivityLocalKey(ElementSummary act, BigInteger defaultValue) {		
		BigInteger putCode = getActivityLocalKey(act);
		if (putCode == null) {
			putCode = defaultValue;
		}		
		return putCode;
	}	

	/**
	 * Retrieves the local key of an activity, currently assumed to be stored in
	 * the put-code field.
	 *
	 * @param act
	 *            the activity to which to set the local key
	 * @param key
	 *            the local key
	 * @throws NullPointerException
	 *             if the activity is null
	 */
	public static void setWorkLocalKey(ElementSummary act, BigInteger key) throws NullPointerException {
		if (act == null)
			throw new NullPointerException("Can't set local key.");

		act.setPutCode(key);
	}

	/**
	 * Clears the local key of an activity, currently assumed to be stored in
	 * the put-code field.
	 *
	 * @param act
	 *            the activity to which to clear the local key
	 * @throws NullPointerException
	 *             if the activity is null
	 */
	public static void cleanWorkLocalKey(ElementSummary act) throws NullPointerException {
		if (act == null)
			throw new NullPointerException("Can't clear local key.");

		act.setPutCode(null);
	}

	/**
	 * Calculates the symmetric difference of self {@link ExternalId external
	 * identifiers} between a work summary and a set of works. Works that do not
	 * match (i.e., no identifier is common) are ignored.
	 *
	 * @param work
	 *            the work summary to be compared with other works
	 * @param works
	 *            the set of works against which the work summary is compared
	 * @return The symmetric difference of self external identifiers between
	 *         work and works
	 * @throws NullPointerException
	 *             if either of the parameters is null
	 */
	public static Map<Work, ExternalIdsDiff> getSelfExternalIdsDiff(WorkSummary work, Collection<Work> works) 
			throws NullPointerException {
		if (work == null || works == null)
			throw new NullPointerException("Can't get external ids.");
		
		final Map<Work, ExternalIdsDiff> matches = new HashMap<Work, ExternalIdsDiff>();
		for (Work match : works) {
			final ExternalIdsDiff diff = 
					new ExternalIdsDiff(getSelfExternalIds(match), getSelfExternalIds(work));
			if (!diff.same.isEmpty())
				matches.put(match, diff);
		}
		return matches;
	}
	
	public static Map<Funding, ExternalIdsDiff> getSelfExternalIdsDiff(FundingSummary work, Collection<Funding> works) 
			throws NullPointerException {
		if (work == null || works == null)
			throw new NullPointerException("Can't get external ids.");
		
		final Map<Funding, ExternalIdsDiff> matches = new HashMap<Funding, ExternalIdsDiff>();
		for (Funding match : works) {
			final ExternalIdsDiff diff = 
					new ExternalIdsDiff(getSelfExternalIds(match), getSelfExternalIds(work));
			if (!diff.same.isEmpty())
				matches.put(match, diff);
		}
		return matches;
	}

	/**
	 * Calculates the symmetric difference of self {@link ExternalId external
	 * identifiers} between a work and a set of works. Works that do not match
	 * (i.e., no identifier is common) are ignored.
	 *
	 * @param work
	 *            the work summary to be compared with other works
	 * @param works
	 *            the set of works against which the work summary is compared
	 * @return The symmetric difference of self external identifiers between
	 *         work and works
	 * @throws NullPointerException
	 *             if either of the parameters is null
	 */
	public static Map<Work, ExternalIdsDiff> getSelfExternalIdsDiff(Work work, Collection<Work> works) 
			throws NullPointerException {
		if (work == null || works == null)
			throw new NullPointerException("Can't get external ids.");
		
		final Map<Work, ExternalIdsDiff> matches = new HashMap<Work, ExternalIdsDiff>();
		for (Work match : works) {
			final ExternalIdsDiff diff = 
					new ExternalIdsDiff(getSelfExternalIds(match), getSelfExternalIds(work));
			if (!diff.same.isEmpty())
				matches.put(match, diff);
		}
		return matches;
	}
	
	public static Map<Funding, ExternalIdsDiff> getSelfExternalIdsDiff(Funding work, Collection<Funding> works) 
			throws NullPointerException {
		if (work == null || works == null)
			throw new NullPointerException("Can't get external ids.");
		
		final Map<Funding, ExternalIdsDiff> matches = new HashMap<Funding, ExternalIdsDiff>();
		for (Funding match : works) {
			final ExternalIdsDiff diff = 
					new ExternalIdsDiff(getSelfExternalIds(match), getSelfExternalIds(work));
			if (!diff.same.isEmpty())
				matches.put(match, diff);
		}
		return matches;
	}
	
	/**
	 * Checks whether a work is already up to date regarding another one, i.e.,
	 * whether a work has the same self {@link ExternalId external identifiers}
	 * as another one.
	 *
	 * This test is expected to be used by the import algorithms, where only new
	 * self external identifiers are to be considered.
	 *
	 * @param preWork
	 *            The potentially out of date work
	 * @param posWork
	 *            The up to date work
	 * @return true if all the self external identifiers between the two works
	 *         are the same, false otherwise
	 */
	public static boolean hasNewSelfIDs(Work preWork, WorkSummary posWork) {
		final ExternalIdsDiff diff = new ExternalIdsDiff(
				getSelfExternalIds(preWork),
				getSelfExternalIds(posWork));

		return diff.more.isEmpty();
	}

	public static boolean hasNewSelfIDs(Funding preWork, FundingSummary posWork) {
		final ExternalIdsDiff diff = new ExternalIdsDiff(
				getSelfExternalIds(preWork),
				getSelfExternalIds(posWork));

		return diff.more.isEmpty();
	}
	
	/**
	 * Checks whether a work is already up to date regarding another one,
	 * considering the {@link ExternalId external identifiers} and
	 * additional meta-data.
	 *
	 * This test is expected to be used by the export algorithms, where the
	 * meta-data is expected to be up-to-date on the remote profile.
	 *
	 * @param preWork
	 *            the potentially out of date work
	 * @param posWork
	 *            the up to date work
	 * @return true if all the external identifiers and the meta-data between
	 *         the two works are the same, false otherwise
	 */
	public static boolean isUpToDate(Work preWork, WorkSummary posWork) {
		return isSelfEIDsUpToDate(preWork, posWork) && isMetaUpToDate(preWork, posWork);
	}
	
	public static boolean isUpToDate(Funding preWork, FundingSummary posWork) {
		return isSelfEIDsUpToDate(preWork, posWork) && isMetaUpToDate(preWork, posWork);
	}

	/**
	 * Checks whether a work is already up to date regarding another one,
	 * considering the {@link ExternalId external identifiers} and
	 * additional meta-data.
	 *
	 * @param preWork
	 *            the potentially out of date work
	 * @param posWork
	 *            the up to date work
	 * @return true if all the external identifiers and the meta-data between
	 *         the two works are the same, false otherwise
	 */
	public static boolean isUpToDate(Work preWork, Work posWork) {
		return isUpToDate(preWork, summarize(posWork));
	}
	
	public static boolean isUpToDate(Funding preWork, Funding posWork) {
		return isUpToDate(preWork, summarize(posWork));
	}

	/**
	 * Checks whether a work is already up to date regarding another one,
	 * considering the self {@link ExternalIdentifier external identifiers}.
	 *
	 * @param preWork
	 *            the potentially out of date work
	 * @param posWork
	 *            the up to date work
	 * @return true if all the self external identifiers are the same, false
	 *         otherwise.
	 * @throws NullPointerException
	 *             if either work is null
	 */
	private static boolean isSelfEIDsUpToDate(Work preWork, WorkSummary posWork) 
			throws NullPointerException {
		if (preWork == null || posWork == null)
			throw new NullPointerException("Can't test null works.");
		
		return identicalEIDs(getSelfExternalIds(preWork),
							 getSelfExternalIds(posWork));
	}
	
	private static boolean isSelfEIDsUpToDate(Funding preWork, FundingSummary posWork) 
			throws NullPointerException {
		if (preWork == null || posWork == null)
			throw new NullPointerException("Can't test null works.");
		
		return identicalEIDs(getSelfExternalIds(preWork),
							 getSelfExternalIds(posWork));
	}

	/**
	 * Checks whether a work is already up to date regarding another one,
	 * considering meta-data other than the self external identifiers.
	 *
	 * The considered fields are: title, publication date (year), work type and
	 * part-of external identifiers. All this meta-data is available in work
	 * summaries.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 *
	 * @param preWork
	 *            the potentially out of date work
	 * @param posWork
	 *            the up to date work
	 * @return true if the considered meta-data is the same, false otherwise.
	 * @throws NullPointerException
	 *             if either work is null
	 */
	private static boolean isMetaUpToDate(Work preWork, WorkSummary posWork) 
			throws NullPointerException {
		if (preWork == null || posWork == null)
			throw new NullPointerException("Can't test null works.");

		boolean res = true;
		res &= identicalEIDs(
				ORCIDHelper.getPartOfExternalIds(preWork), 
				ORCIDHelper.getPartOfExternalIds(posWork));
		res &= getWorkTitle(preWork).equals(getWorkTitle(posWork));
		res &= (getPubYear(preWork) == null && getPubYear(posWork) == null)
				|| (getPubYear(preWork) != null && getPubYear(posWork) != null 
						&& getPubYear(preWork).equals(getPubYear(posWork)));
		res &= (preWork.getType() == null && posWork.getType() == null)
				|| (preWork.getType() != null && posWork.getType() != null && preWork
						.getType().equals(posWork.getType()));
		return res;
	}
	
	private static boolean isMetaUpToDate(Funding preWork, FundingSummary posWork) 
			throws NullPointerException {
		if (preWork == null || posWork == null)
			throw new NullPointerException("Can't test null works.");

		boolean res = true;
		res &= identicalEIDs(
				ORCIDHelper.getPartOfExternalIds(preWork), 
				ORCIDHelper.getPartOfExternalIds(posWork));
		res &= getWorkTitle(preWork).equals(getWorkTitle(posWork));
		res &= (getPubYear(preWork) == null && getPubYear(posWork) == null)
				|| (getPubYear(preWork) != null && getPubYear(posWork) != null 
						&& getPubYear(preWork).equals(getPubYear(posWork)));
		res &= (preWork.getType() == null && posWork.getType() == null)
				|| (preWork.getType() != null && posWork.getType() != null && preWork
						.getType().equals(posWork.getType()));
		return res;
	}

	/**
	 * Tests whether a work summary has minimal quality to be synchronized,
	 * by inspecting its meta-data, returns the detected invalid fields.
	 * 
 	 * The considered fields are: self external identifiers, title, publication date
	 * (year), work type. All this meta-data is available in work summaries.
	 * 
	 * @see #testMinimalQuality(WorkSummary, Collection)
	 * 
	 * @param work
	 *            the work summary to test for quality
	 * @return the set of invalid fields
	 * @throws NullPointerException
	 *             if the work is null
	 */
	public static Set<String> testMinimalQuality(WorkSummary work) throws NullPointerException {
		return testMinimalQuality(work,new HashSet<Work>());
	}

	public static Set<String> testMinimalQuality(FundingSummary work) throws NullPointerException {
		return testMinimalQuality(work,new HashSet<Funding>());
	}

	/**
	 * Tests whether a work summary has minimal quality to be synchronized, by
	 * inspecting its meta-data and that of coexisting works, and returns the
	 * detected invalid fields.
	 * 
	 * The considered fields are: self external identifiers, title, publication date
	 * (year), work type. The test also checks whether the external identifiers
	 * overlap with those of the coexisting works. All this meta-data is
	 * available in work summaries. The publication date is not necessary for
	 * data sets and research techniques.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 * 
	 * @param work
	 *            the work to test for quality
	 * @param others
	 *            other coexisting works
	 * @return the set of invalid fields
	 * @throws NullPointerException
	 *             if the work is null
	 */
	public static Set<String> testMinimalQuality(WorkSummary work, Collection<Work> others) throws NullPointerException {
		if (work == null)
			throw new NullPointerException("Can't test null work.");
	
		final Set<String> res = new HashSet<String>();
		if (getSelfExternalIds(work).getExternalId().isEmpty())
			res.add(INVALID_EXTERNALIDENTIFIERS);
		else for (ExternalId eid : getSelfExternalIds(work).getExternalId())
				if (!validEIdType(eid.getExternalIdType())) res.add(INVALID_EXTERNALIDENTIFIERS);
		if (work.getTitle() == null)
			res.add(INVALID_TITLE);
		else if (work.getTitle().getTitle() == null)
			res.add(INVALID_TITLE);
		if (work.getType() == null)
			res.add(INVALID_TYPE);
		if (work.getType() == null || 
				(work.getType() != WorkType.DATA_SET && work.getType() != WorkType.RESEARCH_TECHNIQUE)) {
			if (work.getPublicationDate() == null)
				res.add(INVALID_PUBLICATIONDATE);
			else if (work.getPublicationDate().getYear() == null)
				res.add(INVALID_YEAR);
		}
		Map<Work, ExternalIdsDiff> worksDiffs = ORCIDHelper.getSelfExternalIdsDiff(work, others);
		for (Work match : worksDiffs.keySet())
			if (match.getPutCode() != work.getPutCode() && !worksDiffs.get(match).same.isEmpty())
				res.add(OVERLAPPING_EIDs);

		return res;
	}

	public static Set<String> testMinimalQuality(FundingSummary work, Collection<Funding> others) throws NullPointerException {
		if (work == null)
			throw new NullPointerException("Can't test null work.");
		
		final Set<String> res = new HashSet<String>();
		if (getSelfExternalIds(work).getExternalId().isEmpty())
			res.add(INVALID_EXTERNALIDENTIFIERS);
		else for (ExternalId eid : getSelfExternalIds(work).getExternalId())
				if (!validEIdType(eid.getExternalIdType())) res.add(INVALID_EXTERNALIDENTIFIERS);
		if (work.getTitle() == null)
			res.add(INVALID_TITLE);
		else if (work.getTitle().getTitle() == null)
			res.add(INVALID_TITLE);
		if (work.getType() == null)
			res.add(INVALID_TYPE);
		if (work.getOrganization() == null)
			res.add(INVALID_ORGANIZATION);
	
		Map<Funding, ExternalIdsDiff> worksDiffs = ORCIDHelper.getSelfExternalIdsDiff(work, others);
		for (Funding match : worksDiffs.keySet())
			if (match.getPutCode() != work.getPutCode() && !worksDiffs.get(match).same.isEmpty())
				res.add(OVERLAPPING_EIDs);
		
		return res;
	}

	/**
	 * Tests whether a work has minimal quality to be synchronized, by
	 * inspecting its meta-data. Throws an exception if the test fails.
	 * 
	 * The considered fields are: self external identifiers, title, publication date
	 * (year), work type. The overlapping of external identifiers with other
	 * works is also tested. All this meta-data is available in work summaries.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 * 
	 * @param work
	 *            the work to test for quality
	 * @param others
	 *            other coexisting works
	 * @throws InvalidWorkException
	 *             if the quality test fails, containing the reasons for failing
	 * @throws NullPointerException
	 *             if the work is null
	 */
	public static void tryMinimalQuality(Work work, Collection<Work> others) throws InvalidWorkException {
		Set<String> invs = testMinimalQuality(summarize(work),others);
		if (!invs.isEmpty()) {
			throw new InvalidWorkException(invs);
		}
	}

	/**
	 * Tests whether a work summary has minimal quality to be synchronized, by
	 * inspecting its meta-data. Throws an exception if the test fails.
	 * 
	 * The considered fields are: self external identifiers, title, publication date
	 * (year), work type. The overlapping of external identifiers with other
	 * works is also tested. All this meta-data is available in work summaries.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 * 
	 * @param work
	 *            the summary work to test for quality
	 * @param others
	 *            other coexisting works
	 * @throws InvalidWorkException
	 *             if the quality test fails, containing the reasons for failing
	 * @throws NullPointerException
	 *             if the work is null
	 */
	public static void tryMinimalQuality(WorkSummary work, Collection<Work> others) throws InvalidWorkException {
		Set<String> invs = testMinimalQuality(work, others);
		if (!invs.isEmpty()) {
			throw new InvalidWorkException(invs);
		}
	}
	
	/**
	 * Test whether a give external identifiers type is valid. Elements of the
	 * enum {@link EIdType} take the shape of upper-case valid EId types, with
	 * slashes replaced by underscores.
	 * 
	 * @param eid
	 *            a potential EId type
	 * @return whether the string is a valid EId type
	 */
	private static boolean validEIdType(String eid) {
		try {
			EIdType.valueOf(eid.replace('-', '_').toUpperCase());
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Merges a work group into a single work summary. Simply selects the
	 * meta-data (including part-of external identifiers) from the first work of
	 * the group (i.e., the preferred one) and assigns it any extra (self)
	 * external identifiers from the remainder works. These remainder identifiers
	 * are the ones grouped by ORCID.
	 *
	 * @param group
	 *            the work group to be merged
	 * @return the resulting work summary
	 * @throws NullPointerException
	 *             if the group is null
	 * @throws IllegalArgumentException
	 *             if the group is empty
	 */
	private static WorkSummary groupToWork(WorkGroup group) 
			throws NullPointerException, IllegalArgumentException {
		if (group == null || group.getWorkSummary() == null)
			throw new NullPointerException("Can't merge null group");
		if (group.getWorkSummary().isEmpty())
			throw new IllegalArgumentException("Can't merge empty group.");
		
		final WorkSummary preferred = group.getWorkSummary().get(0);
		final WorkSummary dummy = clone(preferred);

		final List<ExternalId> eids = getPartOfExternalIds(dummy).getExternalId();
		for (ExternalId id : group.getExternalIds().getExternalId()) {
			final ExternalId eid = new ExternalId();
			eid.setExternalIdRelationship(id.getExternalIdRelationship());
			eid.setExternalIdType(id.getExternalIdType().toLowerCase());
			eid.setExternalIdValue(id.getExternalIdValue());
			eids.add(eid);
		}
		dummy.setExternalIds(new ExternalIds(eids));

		return dummy;
	}
	
	/**
	 * Merges a work group into a single funding summary. Simply selects the
	 * meta-data (including part-of external identifiers) from the first funding
	 * entrey of the group (i.e., the preferred one) and assigns it any extra
	 * (self) external identifiers from the remainder funding entries. These
	 * remainder identifiers are the ones grouped by ORCID.
	 *
	 * @param group
	 *            the funding group to be merged
	 * @return the resulting funding summary
	 * @throws NullPointerException
	 *             if the group is null
	 * @throws IllegalArgumentException
	 *             if the group is empty
	 */
	private static FundingSummary groupToFunding(FundingGroup group) 
			throws NullPointerException, IllegalArgumentException {
		if (group == null || group.getFundingSummary() == null)
			throw new NullPointerException("Can't merge null group");
		if (group.getFundingSummary().isEmpty())
			throw new IllegalArgumentException("Can't merge empty group.");
		
		final FundingSummary preferred = group.getFundingSummary().get(0);
		final FundingSummary dummy = clone(preferred);

		final List<ExternalId> eids = getPartOfExternalIds(dummy).getExternalId();
		for (ExternalId id : group.getExternalIds().getExternalId()) {
			final ExternalId eid = new ExternalId();
			eid.setExternalIdRelationship(id.getExternalIdRelationship());
			eid.setExternalIdType(id.getExternalIdType().toLowerCase());
			eid.setExternalIdValue(id.getExternalIdValue());
			eids.add(eid);
		}
		dummy.setExternalIds(new ExternalIds(eids));

		return dummy;
	}

	/**
	 * Retrieves the title from a work.
	 *
	 * @param work
	 *            the work
	 * @return the work's title if defined, empty string otherwise
	 */
	protected static String getWorkTitle(Work work) {
		return getWorkTitle(summarize(work));
	}
	
	protected static String getWorkTitle(Funding work) {
		return getWorkTitle(summarize(work));
	}

	/**
	 * Retrieves the title from a work summary.
	 *
	 * @param work
	 *            the work summary
	 * @return the work's title if defined, empty string otherwise
	 */
	protected static String getWorkTitle(WorkSummary work) {
		if (work == null || work.getTitle() == null)
			return "";
		return work.getTitle().getTitle();
	}
	
	protected static String getWorkTitle(FundingSummary work) {
		if (work == null || work.getTitle() == null)
			return "";
		return work.getTitle().getTitle();
	}
	
	/**
	 * Retrieves the publication year from a work.
	 *
	 * @param work
	 *            the work
	 * @return the publication year if defined, null otherwise
	 */
	private static String getPubYear(Work work) {
		return getPubYear(summarize(work));
	}
	
	private static String getPubYear(Funding work) {
		return getPubYear(summarize(work));
	}
	
	/**
	 * Retrieves the publication year from a work summary.
	 *
	 * @param work
	 *            the work summary
	 * @return the publication year if defined, null otherwise
	 */
	private static String getPubYear(WorkSummary work) {
		if (work == null 
				|| work.getPublicationDate() == null
				|| work.getPublicationDate().getYear() == null)
			return null;
		return work.getPublicationDate().getYear().getValue();
	}
	
	private static String getPubYear(FundingSummary work) {
		if (work == null 
				|| work.getStartDate() == null
				|| work.getStartDate().getYear() == null)
			return null;
		return work.getStartDate().getYear().getValue();
	}

	/**
	 * Returns the non-null external identifiers of a work (null becomes empty
	 * list).
	 * 
	 * @param work
	 *            the work from which to retrieve the external identifiers
	 * @return the non-null external identifiers
	 */
	public static ExternalIds getNonNullExternalIds (Work work) {
		if (work.getExternalIds() != null && work.getExternalIds().getExternalId() != null) {
			return work.getExternalIds();
		} else {
			return new ExternalIds(new ArrayList<ExternalId>());
		}
	}
	
	/**
	 * Returns the non-null external identifiers of a funding entry (null
	 * becomes empty list).
	 * 
	 * @param fund
	 *            the funding entry from which to retrieve the external
	 *            identifiers
	 * @return the non-null external identifiers
	 */
	public static ExternalIds getNonNullExternalIds (Funding work) {
		if (work.getExternalIds() != null && work.getExternalIds().getExternalId() != null) {
			return work.getExternalIds();
		} else {
			return new ExternalIds(new ArrayList<ExternalId>());
		}
	}
	
	/**
	 * Returns the non-null external identifiers of a work summary (null becomes
	 * empty list).
	 * 
	 * @param work
	 *            the work summary from which to retrieve the external
	 *            identifiers
	 * @return the non-null external identifiers
	 */
	public static ExternalIds getNonNullExternalIds (WorkSummary work) {
		if (work.getExternalIds() != null && work.getExternalIds().getExternalId() != null) {
			return work.getExternalIds();
		} else {
			return new ExternalIds(new ArrayList<ExternalId>());
		}
	}
	
	/**
	 * Returns the non-null external identifiers of a funding summary (null becomes
	 * empty list).
	 * 
	 * @param funding
	 *            the funding summary from which to retrieve the external
	 *            identifiers
	 * @return the non-null external identifiers
	 */
	public static ExternalIds getNonNullExternalIds (FundingSummary funding) {
		if (funding.getExternalIds() != null && funding.getExternalIds().getExternalId() != null) {
			return funding.getExternalIds();
		} else {
			return new ExternalIds(new ArrayList<ExternalId>());
		}
	}
	
	/**
	 * Returns the non-null part-of external identifiers of a work (null becomes
	 * empty list).
	 * 
	 * @param work
	 *            the work from which to retrieve the external identifiers
	 * @return the non-null part-of external identifiers
	 */
	public static ExternalIds getPartOfExternalIds (Work work) {
		return getPartOfExternalIds(summarize(work));
	}

	/**
	 * Returns the non-null part-of external identifiers of a funding entry
	 * (null becomes empty list).
	 * 
	 * @param fund
	 *            the funding entry from which to retrieve the external
	 *            identifiers
	 * @return the non-null part-of external identifiers
	 */
	public static ExternalIds getPartOfExternalIds (Funding fund) {
		return getPartOfExternalIds(summarize(fund));
	}
	
	/**
	 * Returns the non-null part-of external identifiers of a work summary (null
	 * becomes empty list).
	 * 
	 * @param work
	 *            the work summary from which to retrieve the external
	 *            identifiers
	 * @return the non-null part-of external identifiers
	 */
	public static ExternalIds getPartOfExternalIds (WorkSummary work) {
		List<ExternalId> res = new ArrayList<ExternalId>();
		for (ExternalId eid : getNonNullExternalIds(work).getExternalId())
			if (eid.getExternalIdRelationship() == RelationshipType.PART_OF)
				res.add(eid);
		return new ExternalIds(res);
	}
	
	/**
	 * Returns the non-null part-of external identifiers of a funding summary
	 * (null becomes empty list).
	 * 
	 * @param fund
	 *            the funding summary from which to retrieve the external
	 *            identifiers
	 * @return the non-null part-of external identifiers
	 */
	public static ExternalIds getPartOfExternalIds (FundingSummary fund) {
		List<ExternalId> res = new ArrayList<ExternalId>();
		for (ExternalId eid : getNonNullExternalIds(fund).getExternalId())
			if (eid.getExternalIdRelationship() == RelationshipType.PART_OF)
				res.add(eid);
		return new ExternalIds(res);
	}
	
	/**
	 * Returns the non-null self external identifiers of a work (null becomes
	 * empty list).
	 * 
	 * @param work
	 *            the work from which to retrieve the external identifiers
	 * @return the non-null self external identifiers
	 */
	public static ExternalIds getSelfExternalIds (Work work) {
		return getSelfExternalIds(summarize(work));
	}
	
	public static ExternalIds getSelfExternalIds (Funding work) {
		return getSelfExternalIds(summarize(work));
	}

	/**
	 * Returns the non-null self external identifiers of a work summary (null
	 * becomes empty list).
	 * 
	 * @param work
	 *            the work summary from which to retrieve the external
	 *            identifiers
	 * @return the non-null self external identifiers
	 */
	public static ExternalIds getSelfExternalIds (WorkSummary work) {
		List<ExternalId> res = new ArrayList<ExternalId>();
		for (ExternalId eid : getNonNullExternalIds(work).getExternalId())
			if (eid.getExternalIdRelationship() == RelationshipType.SELF)
				res.add(eid);
		return new ExternalIds(res);
	}
	
	public static ExternalIds getSelfExternalIds (FundingSummary work) {
		List<ExternalId> res = new ArrayList<ExternalId>();
		for (ExternalId eid : getNonNullExternalIds(work).getExternalId())
			if (eid.getExternalIdRelationship() == RelationshipType.SELF)
				res.add(eid);
		return new ExternalIds(res);
	}


	/**
	 * Tests whether two sets of (non-exclusively self or part-of) external
	 * identifiers are identical.
	 * 
	 * @param eids1
	 *            the first set of external identifiers
	 * @param eids2
	 *            the second set of external identifiers
	 * @return whether the external identifiers are identical
	 */
	private static boolean identicalEIDs(ExternalIds eids1, ExternalIds eids2) {
		final ExternalIdsDiff diff = new ExternalIdsDiff(eids1, eids2);
		return diff.more.isEmpty() && diff.less.isEmpty();
	}

	/**
	 * Copies all meta-data from an activity summary into another.
	 * 
	 * @param from
	 *            the source summary
	 * @param to
	 *            the target summary
	 * @throws NullPointerException if either argument is null
	 */
	private static void copy(ElementSummary from, ElementSummary to) 
			throws NullPointerException {
		if (from == null || to == null) 
			throw new NullPointerException("Can't copy null works.");
		
		to.setCreatedDate(from.getCreatedDate());
		to.setDisplayIndex(from.getDisplayIndex());
		to.setLastModifiedDate(from.getLastModifiedDate());
		to.setPath(from.getPath());
		to.setPutCode(from.getPutCode());
		to.setSource(from.getSource());
		to.setVisibility(from.getVisibility());
	}

	/**
	 * Clones a work summary.
	 * 
	 * @param work
	 *            the summary to be cloned
	 * @return the clone
	 */
	public static WorkSummary clone(WorkSummary work) {
		if (work == null) return null;
		
		final WorkSummary dummy = new WorkSummary();
		copy(work, dummy);
		dummy.setPublicationDate(work.getPublicationDate());
		dummy.setTitle(work.getTitle());
		dummy.setType(work.getType());
		dummy.setExternalIds(getNonNullExternalIds(work));
		return dummy;
	}

	/**
	 * Clones a funding summary.
	 * 
	 * @param fund
	 *            the summary to be cloned
	 * @return the clone
	 */
	public static FundingSummary clone(FundingSummary fund) {
		if (fund == null) return null;
		
		final FundingSummary dummy = new FundingSummary();
		copy(fund, dummy);
		dummy.setStartDate(fund.getStartDate());
		dummy.setEndDate(fund.getEndDate());
		dummy.setOrganization(fund.getOrganization());
		dummy.setTitle(fund.getTitle());
		dummy.setType(fund.getType());
		dummy.setExternalIds(getNonNullExternalIds(fund));
		return dummy;
	}
	
	public static Funding clone(Funding fund) {
		if (fund == null) return null;
		
		final Funding dummy = new Funding();
		copy(fund, dummy);
		dummy.setStartDate(fund.getStartDate());
		dummy.setEndDate(fund.getEndDate());
		dummy.setOrganization(fund.getOrganization());
		dummy.setTitle(fund.getTitle());
		dummy.setType(fund.getType());
		dummy.setAmount(fund.getAmount());
		dummy.setContributors(fund.getContributors());
		dummy.setShortDescription(fund.getShortDescription());
		dummy.setOrganizationDefinedType(fund.getOrganizationDefinedType());
		dummy.setUrl(fund.getUrl());
		dummy.setExternalIds(getNonNullExternalIds(fund));
		return dummy;
	}

	/**
	 * Clones a work.
	 * 
	 * @param work
	 *            the work to be cloned
	 * @return the clone
	 */
	public static Work clone(Work work) {
		if (work == null) return null;
		
		final Work dummy = new Work();
		copy(work, dummy);
		dummy.setPublicationDate(work.getPublicationDate());
		dummy.setTitle(work.getTitle());
		dummy.setType(work.getType());
		dummy.setExternalIds(getNonNullExternalIds(work));
		dummy.setContributors(work.getContributors());

		dummy.setCitation(work.getCitation());
		dummy.setContributors(work.getContributors());
		dummy.setCountry(work.getCountry());
		dummy.setJournalTitle(work.getJournalTitle());
		dummy.setLanguageCode(work.getLanguageCode());
		dummy.setShortDescription(work.getShortDescription());
		dummy.setUrl(work.getUrl());
		return dummy;
	}

	static WorkSummary summarize(Work work) {
		if (work == null) return null;
		
		final WorkSummary dummy = new WorkSummary();
		copy(work, dummy);
		dummy.setPublicationDate(work.getPublicationDate());
		dummy.setTitle(work.getTitle());
		dummy.setType(work.getType());
		dummy.setExternalIds(getNonNullExternalIds(work));
		return dummy;
	}
	
	static FundingSummary summarize(Funding work) {
		if (work == null) return null;
		
		final FundingSummary dummy = new FundingSummary();
		copy(work, dummy);
		dummy.setOrganization(work.getOrganization());
		dummy.setStartDate(work.getStartDate());
		dummy.setEndDate(work.getEndDate());
		dummy.setTitle(work.getTitle());
		dummy.setType(work.getType());
		dummy.setExternalIds(getNonNullExternalIds(work));
		return dummy;
	}
	
	
}
