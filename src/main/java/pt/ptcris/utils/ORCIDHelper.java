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
import java.util.Iterator;
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
import org.um.dsi.gavea.orcid.model.activities.WorkGroup;
import org.um.dsi.gavea.orcid.model.common.ClientId;
import org.um.dsi.gavea.orcid.model.common.ElementSummary;
import org.um.dsi.gavea.orcid.model.common.ExternalId;
import org.um.dsi.gavea.orcid.model.common.ExternalIds;
import org.um.dsi.gavea.orcid.model.common.RelationshipType;
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
	public static final String OVERLAPPING_EIDs = "OverlappingEIDs";
	
	public enum EIdType {
		OTHER_ID("other-id"), AGR("agr"), ARXIV("arxiv"), ASIN("asin"), 
				BIBCODE("bibcode"), CBA("cba"), CIT("cit"), CTX("ctx"), DOI("doi"), 
				EID("eid"), ETHOS("ethos"), HANDLE("handle"), HIR("hir"), ISBN("isbn"), 
				ISSN("issn"), JFM("jfm"), JSTOR("jstor"), LCCN("lccn"), MR("mr"), 
				OCLC("oclc"), OL("ol"), OSTI("osti"), PAT("pat"), PMC("pmc"), 
				PMID("pmid"), RFC("rfc"), SOURCE_WORK_ID("source-work-id"), 
				SSRN("ssrn"), URI("uri"), URN("urn"), WOSUID("wosuid"), ZBL("zbl"),
				CIENCIAIUL("cienciaiul");

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

	/**
	 * Asynchronously gets a full work from an ORCID profile. The resulting work
	 * contains every external identifier set in the input work summary, because
	 * the summary resulted from the merging of a group, but the retrieve full
	 * work is a single work. It also clears the put-code, since at this level
	 * they represent the local identifier.
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
	private void getFullWork(WorkSummary mergedWork, Map<BigInteger, Object> cb)
			throws NullPointerException {
		if (mergedWork == null) throw new NullPointerException("Can't get null work.");
				
		if (client.threads() > 1) {
			final ORCIDGetWorker worker = new ORCIDGetWorker(mergedWork,client, cb, _log);
			executor.execute(worker);
		} else {
			Work fullWork;
			try {
				_log.debug("[getFullWork] " + mergedWork.getPutCode());
				fullWork = client.getWork(mergedWork.getPutCode());
				finalizeGet(fullWork, mergedWork);
				
				cb.put(mergedWork.getPutCode(), fullWork);
			} catch (OrcidClientException e) {
				cb.put(mergedWork.getPutCode(), e);
			}
		}
	}

	/**
	 * Synchronously gets a full work from an ORCID profile. The resulting work
	 * contains every external identifier set in the input work summary, because
	 * the summary resulted from the merging of a group, but the retrieve full
	 * work is a single work. It also clears the put-code, since at this level
	 * they represent the local identifier.
	 *
	 * @see ORCIDClient#getWork(BigInteger)
	 * 
	 * @param mergedWork
	 *            the work summary representing a merged group
	 * @throws OrcidClientException
	 *             if communication with the ORCID API fails
	 * @throws NullPointerException
	 *             if the merged work is null
	 * @return the full work retrieved from ORCID
	 */
	private Work getFullWork(WorkSummary mergedWork) 
			throws OrcidClientException, NullPointerException {
		if (mergedWork == null) throw new NullPointerException("Can't get null work.");
		
		_log.debug("[getFullWork] " + mergedWork.getPutCode());
		
		final Work fullWork = client.getWork(mergedWork.getPutCode());
		finalizeGet(fullWork, mergedWork);

		return fullWork;
	}
	
	public void getFullWorks(List<WorkSummary> mergedWorks, Map<BigInteger, Object> cb)
			throws OrcidClientException, NullPointerException {
		if (mergedWorks == null) throw new NullPointerException("Can't get null work.");
		
		if (client.threads() > 1) {
			if (bulk_size_get > 1) {
				for (int i = 0; i < mergedWorks.size();) {
					List<WorkSummary> putcodes = new ArrayList<WorkSummary>();
					for (int j = 0; j < bulk_size_get && i<mergedWorks.size(); j ++) {
		  				putcodes.add(mergedWorks.get(i));
		  				i++;
					}
					final ORCIDBulkGetWorker worker = new ORCIDBulkGetWorker(putcodes,client, cb, _log);
					executor.execute(worker);
				}
			} else {
				for (int i = 0; i < mergedWorks.size(); i++) {
					final ORCIDGetWorker worker = new ORCIDGetWorker(mergedWorks.get(i),client, cb, _log);
					executor.execute(worker);
				}
			}
		} else {
			_log.debug("[getFullWorks] " + mergedWorks.size());
			Map<BigInteger,Object> fullWorks = new HashMap<BigInteger, Object>();
			if (bulk_size_get > 1) {
				for (int i = 0; i < mergedWorks.size();) {
					List<BigInteger> putcodes = new ArrayList<BigInteger>();
					for (int j = 0; j < bulk_size_get && i<mergedWorks.size(); j ++) {
		  				putcodes.add(mergedWorks.get(i).getPutCode());
		  				i++;
					}
					fullWorks.putAll(client.getWorks(putcodes));
				}	
			} else {
				for (int i = 0; i < mergedWorks.size(); i++) {
					fullWorks.put(mergedWorks.get(i).getPutCode(),client.getWork(mergedWorks.get(i).getPutCode()));
				}	
			}
			for (WorkSummary s : mergedWorks)
				if (fullWorks.get(s.getPutCode()) instanceof Work)
					finalizeGet((Work) fullWorks.get(s.getPutCode()), s);
			cb.putAll(fullWorks);
		}
		
		
		

	}

	/**
	 * Finalizes a get by updating the meta-data.
	 * 
	 * @see #getFullWork(WorkSummary)
	 * 
	 * @param fullWork
	 *            the newly retrieved work
	 * @param sumWork
	 *            the original summary
	 */
	static void finalizeGet(Work fullWork, WorkSummary sumWork) {
		fullWork.setExternalIds(getNonNullExternalIds(sumWork));
		cleanWorkLocalKey(fullWork);
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
	private static void cleanWorkLocalKey(ElementSummary act) throws NullPointerException {
		if (act == null)
			throw new NullPointerException("Can't clear local key.");

		act.setPutCode(null);
	}

	/**
	 * Calculates the symmetric difference of {@link ExternalId external
	 * identifiers} between a work summary and a set of works. Works that do not
	 * match (i.e., no identifier is common) are ignored.
	 *
	 * @param work
	 *            the work summary to be compared with other works
	 * @param works
	 *            the set of works against which the work summary is compared
	 * @return The symmetric difference of external identifiers between work and works
	 * @throws NullPointerException
	 *             if either of the parameters is null
	 */
	public static Map<Work, ExternalIdsDiff> getExternalIdsDiff(WorkSummary work, Collection<Work> works) 
			throws NullPointerException {
		if (work == null || works == null)
			throw new NullPointerException("Can't get external ids.");
		
		final Map<Work, ExternalIdsDiff> matches = new HashMap<Work, ExternalIdsDiff>();
		for (Work match : works) {
			final ExternalIdsDiff diff = 
					new ExternalIdsDiff(getNonNullExternalIds(match), getNonNullExternalIds(work));
			if (!diff.same.isEmpty())
				matches.put(match, diff);
		}
		return matches;
	}

	/**
	 * Calculates the symmetric difference of {@link ExternalId external
	 * identifiers} between a work and a set of works. Works that do not match
	 * (i.e., no identifier is common) are ignored.
	 *
	 * @param work
	 *            the work summary to be compared with other works
	 * @param works
	 *            the set of works against which the work summary is compared
	 * @return The symmetric difference of external identifiers between work and works
	 * @throws NullPointerException
	 *             if either of the parameters is null
	 */
	public static Map<Work, ExternalIdsDiff> getExternalIdsDiff(Work work, Collection<Work> works) 
			throws NullPointerException {
		if (work == null || works == null)
			throw new NullPointerException("Can't get external ids.");
		
		final Map<Work, ExternalIdsDiff> matches = new HashMap<Work, ExternalIdsDiff>();
		for (Work match : works) {
			final ExternalIdsDiff diff = 
					new ExternalIdsDiff(getNonNullExternalIds(match), getNonNullExternalIds(work));
			if (!diff.same.isEmpty())
				matches.put(match, diff);
		}
		return matches;
	}
	
	/**
	 * Checks whether a work is already up to date regarding another one, i.e.,
	 * whether a work has the same {@link ExternalId external
	 * identifiers} as another one.
	 *
	 * This test is expected to be used by the import algorithms, where only new
	 * external identifiers	 are to be considered.
	 *
	 * @param preWork
	 *            The potentially out of date work.
	 * @param posWork
	 *            The up to date work.
	 * @return true if all the UIDs between the two works are the same, false
	 *         otherwise.
	 */
	public static boolean hasNewIDs(Work preWork, WorkSummary posWork) {
		final ExternalIdsDiff diff = new ExternalIdsDiff(
				getNonNullExternalIds(preWork),
				getNonNullExternalIds(posWork));

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
	 *         the two works are the same, false otherwise.
	 */
	public static boolean isUpToDate(Work preWork, WorkSummary posWork) {
		return isIDsUpToDate(preWork, posWork) && isMetaUpToDate(preWork, posWork);
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
	 *         the two works are the same, false otherwise.
	 */
	public static boolean isUpToDate(Work preWork, Work posWork) {
		return isIDsUpToDate(preWork, posWork) && isMetaUpToDate(preWork, posWork);
	}

	/**
	 * Checks whether a work is already up to date regarding another one,
	 * considering the {@link ExternalIdentifier external identifiers}.
	 *
	 * @param preWork
	 *            the potentially out of date work
	 * @param posWork
	 *            the up to date work
	 * @return true if all the external identifiers are the same, false otherwise.
	 * @throws NullPointerException if either work is null
	 */
	private static boolean isIDsUpToDate(Work preWork, WorkSummary posWork) 
			throws NullPointerException {
		if (preWork == null || posWork == null)
			throw new NullPointerException("Can't test null works.");
		
		final ExternalIdsDiff diff = new ExternalIdsDiff(
				getNonNullExternalIds(preWork),
				getNonNullExternalIds(posWork));
		return diff.more.isEmpty() && diff.less.isEmpty();
	}

	/**
	 * Checks whether a work is already up to date regarding another one,
	 * considering the {@link ExternalIdentifier external identifiers}.
	 *
	 * @param preWork
	 *            the potentially out of date work
	 * @param posWork
	 *            the up to date work
	 * @return true if all the external identifiers are the same, false otherwise.
	 * @throws NullPointerException if either work is null
	 */
	private static boolean isIDsUpToDate(Work preWork, Work posWork) 
			throws NullPointerException {
		if (preWork == null || posWork == null)
			throw new NullPointerException("Can't test null works.");

		final ExternalIdsDiff diff = new ExternalIdsDiff(
				getNonNullExternalIds(preWork),
				getNonNullExternalIds(posWork));
		return diff.more.isEmpty() && diff.less.isEmpty();
	}

	/**
	 * Checks whether a work is already up to date regarding another one,
	 * considering meta-data other than the external identifiers.
	 *
	 * The considered fields are: title, publication date (year), work type. All
	 * these meta-data is available in work summaries.
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
	 * Checks whether a work is already up to date regarding another one,
	 * considering meta-data other than the external identifiers.
	 *
	 * The considered fields are: title, publication date (year), work type. All
	 * these meta-data is available in work summaries.
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
	private static boolean isMetaUpToDate(Work preWork, Work posWork)
			throws NullPointerException {
		if (preWork == null || posWork == null)
			throw new NullPointerException("Can't test null works.");

		boolean res = true;
		res &= testPartOfIDs(
				ORCIDHelper.getNonNullExternalIds(preWork).getExternalId(), 
				ORCIDHelper.getNonNullExternalIds(posWork).getExternalId());
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
	 * Checks whether the part-of identifiers of a work are up-to-date.
	 * 
	 * @param eids1
	 *            the current external identifiers
	 * @param eids2
	 *            the external identifiers to be checked if up-to-date
	 * @return true if up-to-date
	 */
	private static boolean testPartOfIDs(List<ExternalId> eids1, List<ExternalId> eids2) {
		for (final ExternalId eid2 : eids2) {
			if (eid2.getExternalIdRelationship() == RelationshipType.SELF) {}
			else {
				boolean found = false;
				Iterator<ExternalId> it = eids1.iterator();
				while (!found && it.hasNext()) {
					ExternalId eid1 = it.next();
					if (eid2.getExternalIdRelationship() == eid1.getExternalIdRelationship()
						&& eid1.getExternalIdValue().equals(eid2.getExternalIdValue())
						&& eid1.getExternalIdType().equals(eid2.getExternalIdType())) 
					found = true;
				}
				if (!found) return false;
			}
		}
		return true;
	}

	/**
	 * Tests whether a work has minimal quality to be synchronized, by
	 * inspecting its meta-data, returns the detected invalid fields.
	 * 
 	 * The considered fields are: external identifiers, title, publication date
	 * (year), work type. All this meta-data is available in work summaries.
	 * 
	 * @see #testMinimalQuality(Work, Collection)
	 * 
	 * @param work
	 *            the work to test for quality
	 * @return the set of invalid fields
	 * @throws NullPointerException
	 *             if the work is null
	 */
	public static Set<String> testMinimalQuality(Work work) throws NullPointerException {
		return testMinimalQuality(work,new HashSet<Work>());
	}
	
	/**
	 * Tests whether a work has minimal quality to be synchronized, by
	 * inspecting its meta-data and that of coexisting works, and returns the
	 * detected invalid fields.
	 * 
	 * The considered fields are: external identifiers, title, publication date
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
	public static Set<String> testMinimalQuality(Work work, Collection<Work> others) throws NullPointerException {
		if (work == null)
			throw new NullPointerException("Can't test null work.");
		
		final Set<String> res = new HashSet<String>();
		if (work.getExternalIds() == null)
			res.add(INVALID_EXTERNALIDENTIFIERS);
		else if (work.getExternalIds().getExternalId() == null
				|| work.getExternalIds().getExternalId().isEmpty())
			res.add(INVALID_EXTERNALIDENTIFIERS);
		else
			for (ExternalId eid : work.getExternalIds().getExternalId())
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
		Map<Work, ExternalIdsDiff> worksDiffs = ORCIDHelper.getExternalIdsDiff(work, others);
		for (Work match : worksDiffs.keySet())
			if (match.getPutCode() != work.getPutCode() && !worksDiffs.get(match).same.isEmpty())
				res.add(OVERLAPPING_EIDs);
		
		return res;
	}

	/**
	 * Tests whether a work summary has minimal quality to be synchronized,
	 * by inspecting its meta-data, returns the detected invalid fields.
	 * 
 	 * The considered fields are: external identifiers, title, publication date
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

	/**
	 * Tests whether a work summary has minimal quality to be synchronized, by
	 * inspecting its meta-data and that of coexisting works, and returns the
	 * detected invalid fields.
	 * 
	 * The considered fields are: external identifiers, title, publication date
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
		if (work.getExternalIds() == null)
			res.add(INVALID_EXTERNALIDENTIFIERS);
		else if (work.getExternalIds().getExternalId() == null
				|| work.getExternalIds().getExternalId().isEmpty())
			res.add(INVALID_EXTERNALIDENTIFIERS);
		else
			for (ExternalId eid : work.getExternalIds().getExternalId())
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
		Map<Work, ExternalIdsDiff> worksDiffs = ORCIDHelper.getExternalIdsDiff(work, others);
		for (Work match : worksDiffs.keySet())
			if (match.getPutCode() != work.getPutCode() && !worksDiffs.get(match).same.isEmpty())
				res.add(OVERLAPPING_EIDs);

		return res;
	}

	/**
	 * Tests whether a work has minimal quality to be synchronized, by
	 * inspecting its meta-data. Throws an exception if the test fails.
	 * 
	 * The considered fields are: external identifiers, title, publication date
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
		Set<String> invs = testMinimalQuality(work,others);
		if (!invs.isEmpty()) {
			throw new InvalidWorkException(invs);
		}
	}

	/**
	 * Tests whether a work summary has minimal quality to be synchronized, by
	 * inspecting its meta-data. Throws an exception if the test fails.
	 * 
	 * The considered fields are: external identifiers, title, publication date
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
	 * meta-data from the first work of the group (i.e., the preferred one) and
	 * assigns it any extra external identifiers from the remainder works.
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

		final List<ExternalId> eids = getNonNullExternalIds(dummy).getExternalId();
		for (ExternalId id : group.getExternalIds().getExternalId()) {
			final ExternalId eid = new ExternalId();
			eid.setExternalIdRelationship(RelationshipType.SELF);
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
		if (work == null || work.getTitle() == null)
			return "";
		return work.getTitle().getTitle();
	}

	/**
	 * Retrieves the title from a work summary.
	 *
	 * @param work
	 *            the work summary
	 * @return the work's title if defined, empty string otherwise
	 */
	private static String getWorkTitle(WorkSummary work) {
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
		if (work == null 
				|| work.getPublicationDate() == null
				|| work.getPublicationDate().getYear() == null)
			return null;
		return work.getPublicationDate().getYear().getValue();
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


}
