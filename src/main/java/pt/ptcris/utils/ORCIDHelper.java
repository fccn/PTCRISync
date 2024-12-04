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
import org.um.dsi.gavea.orcid.model.common.FundingType;
import org.um.dsi.gavea.orcid.model.common.FuzzyDate;
import org.um.dsi.gavea.orcid.model.common.Relationship;
import org.um.dsi.gavea.orcid.model.common.WorkType;
import org.um.dsi.gavea.orcid.model.funding.Funding;
import org.um.dsi.gavea.orcid.model.funding.FundingSummary;
import org.um.dsi.gavea.orcid.model.person.externalidentifier.ExternalIdentifier;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

import pt.ptcris.ORCIDClient;
import pt.ptcris.PTCRISyncResult;
import pt.ptcris.exceptions.InvalidActivityException;
import pt.ptcris.handlers.ProgressHandler;
import pt.ptcris.utils.ORCIDWorkHelper.EIdType;

/**
 * An abstract helper to help manage ORCID activities and simplify the usage of
 * the low-level ORCID {@link pt.ptcris.ORCIDClient client}. Supports bulk
 * requests when available. The concrete ORCID activities to be managed by the
 * helper are expected to support {@link ExternalIds external identifiers}.
 * 
 * Provides support for asynchronous communication with ORCID although it is
 * only active for GET requests due to resource limitations.
 * 
 * @param <E>
 *            The class of ORCID activities being synchronized
 * @param <S>
 *            The class of ORCID activity summaries
 * @param <G>
 *            The class of ORCID activity groups
 * @param <T>
 *            The class of ORCID activity types
 */
public abstract class ORCIDHelper<E extends ElementSummary, S extends ElementSummary, G, T extends Enum<T>> {

	/**
	 * Creates a static (i.e., no server connection) ORCID helper to manage work
	 * activities.
	 * 
	 * @return the ORCID work helper
	 */
	public static ORCIDHelper<Work, WorkSummary, WorkGroup, WorkType> factoryStaticWorks() {
		return new ORCIDWorkHelper(null);
	}

	/**
	 * Creates a static (i.e., no server connection) ORCID helper to manage
	 * funding activities.
	 * 
	 * @return the ORCID funding helper
	 */
	public static ORCIDHelper<Funding, FundingSummary, FundingGroup, FundingType> factoryStaticFundings() {
		return new ORCIDFundingHelper(null);
	}

	public static final String INVALID_EXTERNALIDENTIFIERS = "ExternalIdentifiers";
	public static final String INVALID_TITLE = "Title";
	public static final String INVALID_PUBLICATIONDATE = "PublicationDate";
	public static final String INVALID_YEAR = "Year";
	public static final String INVALID_TYPE = "Type";
	public static final String INVALID_ORGANIZATION = "Organization"; 
	public static final String INVALID_ORGANIZATION_ID = "OrganizationId"; 
	public static final String OVERLAPPING_EIDs = "OverlappingEIDs";

	final int bulk_size_add;
	final int bulk_size_get;

	static final Logger _log = LoggerFactory.getLogger(ORCIDHelper.class);

	/**
	 * The client used to communicate with ORCID. Defines the ORCID user profile
	 * being managed and the Member API id being user to source activities.
	 */
	public final ORCIDClient client;

	ExecutorService executor;
	
	protected boolean useCache;
	
	public ORCIDHelper(ORCIDClient orcidClient, int bulk_size_add,
			int bulk_size_get) {
		this.client = orcidClient;
		this.useCache = false;
		this.bulk_size_add = bulk_size_add;
		this.bulk_size_get = bulk_size_get;
		if (client != null && client.threads() > 1)
			executor = Executors.newFixedThreadPool(client.threads());
	}

	/**
	 * Initializes the helper with a given ORCID client, which defines whether
	 * asynchronous calls will be performed, and sets whether bulk ORCID
	 * commands are available and with which size.
	 *
	 * @param orcidClient
	 *            the ORCID client
	 * @param bulk_size_add
	 *            number of activities per bulk add request
	 * @param bulk_size_get
	 *            number of activities per bulk get request
	 */
	public ORCIDHelper(ORCIDClient orcidClient, int bulk_size_add,
			int bulk_size_get, boolean useCache) {
		this.client = orcidClient;
		this.useCache = useCache;
		this.bulk_size_add = bulk_size_add;
		this.bulk_size_get = bulk_size_get;
		if (client != null && client.threads() > 1)
			executor = Executors.newFixedThreadPool(client.threads());
	}

	/*
	 * Generic client methods to be instantiated for concrete ORCID activity
	 * types.
	 */

	/**
	 * Retrieve all ORCID activity groups through the ORCID client.
	 * 
	 * @return the remote ORCID activities
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	abstract List<G> getSummariesClient() throws OrcidClientException;

	/**
	 * Retrieves through the ORCID client a single full activity for which the
	 * summary is provided. If the communication with ORCID fails, the exception
	 * is embedded in a failed {@link PTCRISyncResult}.
	 * 
	 * @param summary
	 *            the ORCID activity summary for which to read the full ORCID
	 *            activity
	 * @return the remote full ORCID activity
	 */
	abstract PTCRISyncResult<E> readClient(S summary);

	/**
	 * Retrieves through the ORCID client every full activity for which
	 * summaries are provided. If the communication with ORCID fails, the
	 * exception is embedded in a failed {@link PTCRISyncResult}. Should
	 * generate bulk requests.
	 * 
	 * @param summaries
	 *            the ORCID activity summaries for which to read full ORCID
	 *            activities
	 * @return the remote full ORCID activities
	 */
	abstract Map<BigInteger, PTCRISyncResult<E>> readClient(List<S> summaries);

	/**
	 * Creates a worker to asynchronously read a single full activity for which
	 * the summary is provided. If the communication with ORCID fails, the
	 * exception is embedded in a failed {@link PTCRISyncResult}.
	 * 
	 * @param summary
	 *            the ORCID activity summary for which to read the full ORCID
	 *            activity
	 * @param cb
	 *            the callback on which to report results
	 * @param handler
	 *            a handler to report progress
	 * @return the get worker
	 */
	abstract ORCIDWorker<E> readWorker(S summary, Map<BigInteger, PTCRISyncResult<E>> cb, ProgressHandler handler);

	/**
	 * Creates a worker to asynchronously read full activities for which the
	 * summaries are provided. If the communication with ORCID fails, the
	 * exception is embedded in a failed {@link PTCRISyncResult}. Should
	 * generate bulk requests.
	 * 
	 * @param summaries
	 *            the ORCID activity summaries for which to read the full ORCID
	 *            activities
	 * @param cb
	 *            the callback on which to report results
	 * @param handler
	 *            a handler to report progress
	 * @return the get worker
	 */
	abstract ORCIDWorker<E> readWorker(List<S> summaries, Map<BigInteger, PTCRISyncResult<E>> cb, ProgressHandler handler);

	/**
	 * Adds through the ORCID client a new full activity. If the communication
	 * with ORCID fails, the exception is embedded in a failed
	 * {@link PTCRISyncResult}.
	 * 
	 * @param activity
	 *            the full ORCID activity to be added
	 * @return the result of the operation
	 */
	abstract PTCRISyncResult<E> addClient(E activity);

	/**
	 * Adds through the ORCID client a set of new full activities. If the
	 * communication with ORCID fails, the exception is embedded in a failed
	 * {@link PTCRISyncResult}.
	 * 
	 * @param activities
	 *            the full ORCID activities to be added
	 * @return the result of the operation
	 */
	abstract List<PTCRISyncResult<E>> addClient(List<E> activities);

	/**
	 * Updates through the ORCID client a remote activity. If the communication
	 * with ORCID fails, the exception is embedded in a failed
	 * {@link PTCRISyncResult}.
	 * 
	 * @param remotePutcode
	 *            the put-code of the remote ORCID activity
	 * @param activity
	 *            the new state of the ORCID activity
	 * @return the result of the operation
	 */
	abstract PTCRISyncResult<E> updateClient(BigInteger remotePutcode, E activity);

	/**
	 * Deletes through the ORCID client a remote activity. If the communication
	 * with ORCID fails, the exception is embedded in a failed
	 * {@link PTCRISyncResult}.
	 * 
	 * @param remotePutcode
	 *            the put-code of the remote ORCID activity
	 * @return the result of the operation
	 */
	abstract PTCRISyncResult<E> deleteClient(BigInteger remotePutcode);

	/*
	 * Helper client methods that build on the generic methods.
	 */

	/**
	 * Retrieves the entire set of activity summaries from the set ORCID profile
	 * that have at least an external identifier set. Merges each ORCID group
	 * into a single summary, following {@link #group(Object)}.
	 *
	 * @return the set of ORCID activity summaries in the defined ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	private final List<S> getAllSummaries() throws OrcidClientException {
		final List<S> fundSummaryList = new LinkedList<S>();
		final List<G> fundGroupList = getSummariesClient();
		for (G group : fundGroupList)
			fundSummaryList.add(group(group));
		return fundSummaryList;
	}

	/**
	 * Retrieves the entire set of activity summaries of given types from the
	 * set ORCID profile that have at least an external identifier set. Merges
	 * each ORCID group into a single summary, following {@link #group(Object)}.
	 * 
	 * @param types
	 *            the ORCID types of the activities to be retrieved (may be null)
	 * @return the set of ORCID activity summaries in the defined ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public final List<S> getAllTypedSummaries(Collection<T> types) 
			throws OrcidClientException {
		List<S> res = new ArrayList<S>();
		if (types != null)
			for (S r : getAllSummaries())
				if (types.contains(getTypeS(r)))
					res.add(r);
		return res;
	}

	/**
	 * Retrieves the entire set (i.e., not merged) of activity summaries in the
	 * ORCID profile whose source is the Member API id defined in the ORCID
	 * client.
	 *
	 * @return the set of ORCID activity summaries in the ORCID profile for the
	 *         defined source
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public final List<S> getSourcedSummaries() throws OrcidClientException {
		final String sourceClientID = client.getClientId();

		final List<S> summaryList = new LinkedList<S>();
		final List<G> groupList = getSummariesClient();

		for (G group : groupList) {
			for (S summary : getGroupSummaries(group)) {
				final ClientId client = summary.getSource().getSourceClientId();
				// may be null is entry added by the user
				if (client != null
						&& client.getUriPath().equals(sourceClientID)) {
					summaryList.add(summary);
				}
			}
		}
		return summaryList;
	}

	/**
	 * Reads a full ORCID activity from an ORCID profile and adds it to a
	 * callback map. The resulting activity contains every external identifier
	 * set in the input activity summary, because the summary resulted from the
	 * merging of a group, but the retrieved full activity is a single activity.
	 * It also clears the put-code, since at this level they represent the local
	 * identifier. If possible the number of threads is higher than 1, process
	 * is asynchronous. If the list is not a singleton, a bulk request will be
	 * performed if supported for the concrete ORCID activity type. If the
	 * communication with ORCID fails for any activity, the exceptions are
	 * embedded in failed {@link PTCRISyncResult}.
	 *
	 * @see #readClient(List)
	 * 
	 * @param summaries
	 *            the ORCID activity summaries representing the merged groups
	 * @param cb
	 *            the callback object
	 * @param handler
	 *            the handler to report progress
	 * @throws InterruptedException
	 *             if the asynchronous GET process is interrupted
	 */
	public final void getFulls(List<S> summaries,
			Map<BigInteger, PTCRISyncResult<E>> cb, ProgressHandler handler)
			throws InterruptedException {
		if (cb == null)
			throw new IllegalArgumentException("Null callback map.");
		if (summaries == null || summaries.isEmpty())
			return;

		if (client.threads() > 1 && cb != null) {
			for (int i = 0; i < summaries.size();) {
				if (bulk_size_get > 1) {
					List<S> putcodes = new ArrayList<S>();
					for (int j = 0; j < bulk_size_get && i < summaries.size(); j++) {
						putcodes.add(summaries.get(i));
						i++;
					}
					final ORCIDWorker<E> worker = readWorker(putcodes, cb, handler);
					executor.execute(worker);
				} else {
					final ORCIDWorker<E> worker = readWorker(summaries.get(i), cb, handler);
					executor.execute(worker);
					i++;
				}
			}
		} else {
			Map<BigInteger, PTCRISyncResult<E>> fulls = new HashMap<BigInteger, PTCRISyncResult<E>>();
			for (int i = 0; i < summaries.size();) {
				if (bulk_size_get > 1) {
					List<S> putcodes = new ArrayList<S>();
					for (int j = 0; j < bulk_size_get && i < summaries.size(); j++) {
						putcodes.add(summaries.get(i));
						i++;
					}
					fulls.putAll(readClient(putcodes));
					if (handler!=null) handler.step(putcodes.size());
				} else {
					fulls.put(summaries.get(i).getPutCode(),
							readClient(summaries.get(i)));
					if (handler!=null) handler.step();
					i++;
				}
			}
			cb.putAll(fulls);
		}
		waitWorkers();
	}

	/**
	 * Synchronously adds an activity to an ORCID profile. The OK result
	 * includes the newly assigned put-code. If the communication with ORCID
	 * fails, the exception is embedded in a failed {@link PTCRISyncResult}.
	 *
	 * @see #addClient(ElementSummary)
	 * 
	 * @param activity
	 *            the ORCID activity to be added
	 * @return the result of the ORCID call
	 */
	private final PTCRISyncResult<E> add(E activity) {
		assert activity != null;

		// remove any put-code otherwise ORCID will throw an error
		final E clone = cloneE(activity);
		clone.setPutCode(null);

		return addClient(clone);
	}

	/**
	 * Synchronously adds a list of activities to an ORCID profile. A list of
	 * results is returned, one for each input activity. The OK result includes
	 * the newly assigned put-code. If the communication with ORCID fails, the
	 * exception is embedded in a failed {@link PTCRISyncResult}. If the overall
	 * communication fails, the result is replicated for each input.
	 *
	 * @see #addClient(List)
	 * 
	 * @param activities
	 *            the new ORCID activities to be added
	 * @return the results of the ORCID call for each input activity
	 */
	private final List<PTCRISyncResult<E>> add(Collection<E> activities) {
		assert activities != null;
		
		List<E> clones = new ArrayList<E>();
		// remove any put-code otherwise ORCID will throw an error
		for (E activity : activities) {
			final E clone = cloneE(activity);
			clone.setPutCode(null);
			clones.add(clone);
		}

		return addClient(clones);
	}

	/**
	 * Synchronously adds a list of activities to an ORCID profile, either
	 * through atomic or bulk calls if available. A list of results is returned,
	 * one for each input activity. The OK result includes the newly assigned
	 * put-code. If the communication with ORCID fails, the exception is
	 * embedded in a failed {@link PTCRISyncResult}. If the overall
	 * communication fails, the result is replicated for each input.
	 *
	 * @param activities
	 *            the new ORCID activities to be added
	 * @param handler
	 *            the handler to report progress
	 * @return the results of the ORCID call for each input activity
	 */
	public final List<PTCRISyncResult<E>> add(List<E> activities, ProgressHandler handler) {
		List<PTCRISyncResult<E>> res = new ArrayList<PTCRISyncResult<E>>();
		if (activities == null || activities.isEmpty())
			return new ArrayList<PTCRISyncResult<E>>();
		
		for (int c = 0; c != activities.size();) {
			if (bulk_size_add > 1 && activities.size() > 1) {
				List<E> tmp = new ArrayList<E>();
				for (int j = 0; j < bulk_size_add && c < activities.size(); j++) {
					tmp.add(activities.get(c));
					c++;
				}
				res.addAll(this.add(tmp));
				if (handler!=null) handler.step(tmp.size());
			} else {
				E local = activities.get(c);
				res.add(this.add(local));
				if (handler!=null) handler.step();
				c++;
			}
		}
		return res;
	}

	/**
	 * Synchronously updates an activity to an ORCID profile. If the
	 * communication with ORCID fails, the exception is embedded in a failed
	 * {@link PTCRISyncResult}.
	 * 
	 * @see #updateClient(BigInteger, ElementSummary)
	 * 
	 * @param remotePutcode
	 *            the put-code of the remote ORCID activity that will be updated
	 * @param updated
	 *            the new state of the activity that will be updated
	 * @return the result of the ORCID call
	 */
	public final PTCRISyncResult<E> update(BigInteger remotePutcode, E updated) {
		if (remotePutcode == null || updated == null)
			throw new IllegalArgumentException("Can't update null activity.");

		final E clone = cloneE(updated);
		// set the remote put-code
		clone.setPutCode(remotePutcode);

		return updateClient(remotePutcode, clone);
	}

	/**
	 * Synchronously deletes an activity from an ORCID profile. If the
	 * communication with ORCID fails, the exception is embedded in a failed
	 * {@link PTCRISyncResult}.
	 * 
	 * @see #deleteClient(BigInteger)
	 * 
	 * @param putcode
	 *            the remote put-code of the ORCID activity to be deleted
	 * @return the outcome of the delete request
	 */
	public final PTCRISyncResult<E> delete(BigInteger putcode) {
		if (putcode == null)
			throw new IllegalArgumentException("Can't delete null activity.");

		return deleteClient(putcode);
	}

	/**
	 * Deletes the entire set of activity summaries in the ORCID profile whose
	 * source is the Member API id defined in the ORCID client.
	 *
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public final void deleteAllSourced() throws OrcidClientException {
		final List<S> summaryList = getSourcedSummaries();

		for (S summary : summaryList) {
			delete(summary.getPutCode());
		}
	}

	/**
	 * Waits for all active asynchronous workers communicating with ORCID to
	 * finish (if multi-threading is enabled, otherwise it is always true).
	 *
	 * @return whether the workers finished before the timeout
	 * @throws InterruptedException
	 *             if the process was interrupted
	 */
	private final boolean waitWorkers() throws InterruptedException {
		if (client.threads() <= 1)
			return true;

		executor.shutdown();
		final boolean timeout = executor
				.awaitTermination(100, TimeUnit.SECONDS);
		executor = Executors.newFixedThreadPool(client.threads());
		return timeout;
	}

	/*
	 * Generic static methods to be instantiated for concrete ORCID activity
	 * types.
	 */

	/**
	 * Returns the non-null external identifiers of an activity (null becomes
	 * empty list). Cannot rely on
	 * {@link #getNonNullExternalIdsS(ElementSummary)} because
	 * {@link #summarize(ElementSummary)} itself calls this method.
	 * 
	 * @param activity
	 *            the ORCID activity from which to retrieve the external
	 *            identifiers
	 * @return the non-null external identifiers
	 */
	public abstract ExternalIds getNonNullExternalIdsE(E activity);

	/**
	 * Returns the non-null external identifiers of an activity summary (null
	 * becomes empty list).
	 * 
	 * @param summary
	 *            the ORCID activity summary from which to retrieve the external
	 *            identifiers
	 * @return the non-null external identifiers
	 */
	public abstract ExternalIds getNonNullExternalIdsS(S summary);

	/**
	 * Assigns a set of external identifiers to an activity.
	 * 
	 * @param activity
	 *            the ORCID activity to which to assign the external identifiers
	 * @param eids
	 *            the external identifiers to be assigned
	 */
	public abstract void setExternalIdsE(E activity, ExternalIds eids);

	/**
	 * Assigns a set of external identifiers to an activity summary.
	 * 
	 * @param summary
	 *            the ORCID activity summary to which to assign the external
	 *            identifiers
	 * @param eids
	 *            the external identifiers to be assigned
	 */
	public abstract void setExternalIdsS(S summary, ExternalIds eids);

	/**
	 * Retrieves the type of an activity summary.
	 *
	 * @param summary
	 *            the ORCID activity summary
	 * @return the summary's type
	 */
	abstract T getTypeS(S summary);

	/**
	 * Tests whether a given external identifier type name is valid.
	 * 
	 * @param eid
	 *            a potential external identifier type name
	 * @return whether the string is a valid external identifier type
	 */
	abstract boolean validExternalIdType(String eid);

	/**
	 * Retrieves the title of an activity summary.
	 *
	 * @param summary
	 *            the ORCID activity summary
	 * @return the summary's title if defined, empty string otherwise
	 */
	abstract String getTitleS(S summary);

	/**
	 * Retrieves the publication year of an activity summary.
	 *
	 * @param summary
	 *            the ORCID activity summary
	 * @return the summary's publication year, may be null
	 */
	abstract String getYearS(S summary);

	/**
	 * Retrieve the activity summaries that compose an activity group.
	 * 
	 * @param group
	 *            the ORCID group from which to retrieve the ORCID activity
	 *            summaries
	 * @return the OCRID activity summaries contained in the group
	 */
	abstract List<S> getGroupSummaries(G group);

	/**
	 * Merges an activity group into a single activity summary. Simply selects
	 * the meta-data (including part-of external identifiers) from the first
	 * activity of the group (i.e., the preferred one) and assigns it any extra
	 * (self) external identifiers from the remainder activities. These
	 * remainder identifiers are the ones grouped by ORCID.
	 *
	 * @param group
	 *            the activity group to be merged
	 * @return the resulting activity summary
	 * @throws IllegalArgumentException
	 *             if the group is empty
	 */
	abstract S group(G group) throws IllegalArgumentException;

	/**
	 * Checks whether an activity is already up to date regarding another one,
	 * considering meta-data other than the self external identifiers. Uses only
	 * meta-data is available in activity summaries.
	 *
	 * @param preElement
	 *            the potentially out of date ORCID activity
	 * @param posElement
	 *            the up to date ORCID activity
	 * @return true if the considered meta-data is the same, false otherwise.
	 */
	abstract boolean isMetaUpToDate(E preElement, S posElement);

	/**
	 * Tests whether an activity summary has minimal quality to be synchronized,
	 * by inspecting its meta-data and that of coexisting activities, and
	 * returns the detected invalid fields. Only uses meta-data available in
	 * activity summaries. Coexisting activity may be used to test for overlaps.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 * 
	 * @param summary
	 *            the ORCID activity summary to test for quality
	 * @param others
	 *            other coexisting activities
	 * @return the set of invalid meta-data, empty if valid
	 */
	public abstract Set<String> testMinimalQuality(S summary, Collection<E> others);
	
	/**
	 * Creates an update to an activity given the difference on meta-data.
	 * Essentially creates an activity with the same put-code as the original
	 * activity and with the new meta-data that must be assigned to it.
	 * Currently, only new external identifiers are considered.
	 * 
	 * @param original
	 *            the original ORCID activity
	 * @param diff
	 *            the difference on external identifiers
	 * @return the update to be applied to the ORCID activity
	 */
	public abstract E createUpdate(E original, ExternalIdsDiff diff);

	/**
	 * Clones an activity summary.
	 * 
	 * @param summary
	 *            the ORCID activity summary to be cloned
	 * @return the cloned ORCID activity summary
	 */
	abstract S cloneS(S summary);

	/**
	 * Clones an activity.
	 * 
	 * @param activity
	 *            the ORCID activity to be cloned
	 * @return the cloned ORCID activity
	 */
	abstract E cloneE(E activity);

	/**
	 * Summarizes an activity into an activity summary. Most methods on
	 * activities rely on this to re-use methods on activity summaries.
	 * 
	 * @param activity
	 *            the ORCID activity to be summarized
	 * @return the corresponding ORCID activity summary
	 */
	public abstract S summarize(E activity);

	/*
	 * Helper static methods that build on the generic methods.
	 */

	/**
	 * Retrieves the local key of an activity, currently assumed to be stored in
	 * the put-code field.
	 *
	 * @param activity
	 *            the ORCID activity from which to get the local key
	 * @return the local key
	 */
	public static BigInteger getActivityLocalKey(ElementSummary activity) {
		if (activity == null)
			throw new IllegalArgumentException("Null element.");

		return activity.getPutCode();
	}

	/**
	 * Retrieves the local key of an activity, currently assumed to be stored in
	 * the put-code field. If empty, returns a default value.
	 *
	 * @param activity
	 *            the ORCID activity from which to get the local key
	 * @param defaultValue
	 *            a default value in case the put-code is empty
	 * @return the local key
	 */
	public static BigInteger getActivityLocalKey(ElementSummary activity, BigInteger defaultValue) {
		if (activity == null)
			throw new IllegalArgumentException("Null element.");
		
		BigInteger putCode = getActivityLocalKey(activity);
		if (putCode == null)
			putCode = defaultValue;

		return putCode;
	}

	/**
	 * Assign a local key to an activity, currently assumed to be stored in the
	 * put-code field.
	 *
	 * @param activity
	 *            the activity to which to set the local key
	 * @param key
	 *            the local key
	 */
	static void setWorkLocalKey(ElementSummary activity, BigInteger key) {
		if (activity == null)
			throw new IllegalArgumentException("Null element.");

		activity.setPutCode(key);
	}

	/**
	 * Clears (sets to null) the local key of an activity, currently assumed to
	 * be stored in the put-code field.
	 *
	 * @param activity
	 *            the activity to which to clear the local key
	 */
	public static void cleanWorkLocalKey(ElementSummary activity) {
		if (activity == null)
			throw new IllegalArgumentException("Null element.");

		activity.setPutCode(null);
	}

	/**
	 * Copies all meta-data from an activity summary into another.
	 * 
	 * @param from
	 *            the source summary
	 * @param to
	 *            the target summary
	 */
	static void copy(ElementSummary from, ElementSummary to) {
		assert from != null;
		assert to != null;

		to.setCreatedDate(from.getCreatedDate());
		to.setDisplayIndex(from.getDisplayIndex());
		to.setLastModifiedDate(from.getLastModifiedDate());
		to.setPath(from.getPath());
		to.setPutCode(from.getPutCode());
		to.setSource(from.getSource());
		to.setVisibility(from.getVisibility());
	}

	/**
	 * Clones an external identifier.
	 * 
	 * @param id
	 *            the identifier to be clones
	 * @return the clone
	 */
	static ExternalId clone(ExternalId id) {
		assert id != null;
		
		final ExternalId eid = new ExternalId();
		eid.setExternalIdRelationship(id.getExternalIdRelationship());
		eid.setExternalIdType(id.getExternalIdType().toLowerCase());
		eid.setExternalIdValue(id.getExternalIdValue().replaceAll("\\p{C}", "").trim());
		eid.setExternalIdUrl(id.getExternalIdUrl());
		return eid;
	}
	
	/**
	 * Retrieves the type of an activity. Build on
	 * {@link #getTypeS(ElementSummary)}.
	 *
	 * @param activity
	 *            the ORCID activity
	 * @return the activity's type
	 */
	public final T getTypeE(E activity) {
		return getTypeS(summarize(activity));
	}

	/**
	 * Retrieves the title of an activity. Builds on
	 * {@link #getTitleS(ElementSummary)}.
	 *
	 * @param activity
	 *            the ORCID activity
	 * @return the activity's title if defined, empty string otherwise
	 */
	final String getTitleE(E activity) {
		return getTitleS(summarize(activity));
	}

	/**
	 * Retrieves the publication year of an activity. Builds on
	 * {@link #getYearS(ElementSummary)}.
	 *
	 * @param activity
	 *            the ORCID activity
	 * @return the activity's publication year, may be null
	 */
	final String getPubYearE(E activity) {
		return getYearS(summarize(activity));
	}

	/**
	 * Returns the non-null part-of external identifiers of an activity summary
	 * (null becomes empty list).
	 * 
	 * @param summary
	 *            the ORCID activity summary from which to retrieve the external
	 *            identifiers
	 * @return the non-null part-of external identifiers
	 */
	final ExternalIds getPartOfExternalIdsS(S summary) {
		if (summary == null)
			throw new IllegalArgumentException("Null element.");
		
		List<ExternalId> res = new ArrayList<ExternalId>();
		for (ExternalId eid : getNonNullExternalIdsS(summary).getExternalId())
			if (eid.getExternalIdRelationship() == Relationship.PART_OF)
				res.add(eid);
		return new ExternalIds(res);
	}
	
	/**
	 * Returns the non-null funded-by external identifiers of an activity summary
	 * (null becomes empty list).
	 * 
	 * @param summary
	 *            the ORCID activity summary from which to retrieve the external
	 *            identifiers
	 * @return the non-null part-of external identifiers
	 */
	final ExternalIds getFundedByExternalIdsS(S summary) {
		if (summary == null)
			throw new IllegalArgumentException("Null element.");
		
		List<ExternalId> res = new ArrayList<ExternalId>();
		for (ExternalId eid : getNonNullExternalIdsS(summary).getExternalId())
			if (eid.getExternalIdRelationship() == Relationship.FUNDED_BY && eid.getExternalIdType().equalsIgnoreCase(EIdType.DOI.value))
				res.add(eid);
		return new ExternalIds(res);
	}

	/**
	 * Returns the non-null part-of external identifiers of an activity (null
	 * becomes empty list). Builds on
	 * {@link #getPartOfExternalIdsS(ElementSummary)}.
	 * 
	 * @param activity
	 *            the ORCID activity from which to retrieve the external
	 *            identifiers
	 * @return the non-null part-of external identifiers
	 */
	public final ExternalIds getPartOfExternalIdsE(E activity) {
		return getPartOfExternalIdsS(summarize(activity));
	}

	/**
	 * Returns the non-null self external identifiers of an activity summary
	 * (null becomes empty list).
	 * 
	 * @param summary
	 *            the ORCID activity summary from which to retrieve the external
	 *            identifiers
	 * @return the non-null self external identifiers
	 */
	final ExternalIds getSelfExternalIdsS(S summary) {
		if (summary == null)
			throw new IllegalArgumentException("Null element.");
		
		List<ExternalId> res = new ArrayList<ExternalId>();
		for (ExternalId eid : getNonNullExternalIdsS(summary).getExternalId())
			if (eid.getExternalIdRelationship() == Relationship.SELF)
				res.add(eid);
		return new ExternalIds(res);
	}

	/**
	 * Returns the non-null self external identifiers of an activity (null
	 * becomes empty list). Builds on
	 * {@link #getSelfExternalIdsS(ElementSummary)}.
	 * 
	 * @param activity
	 *            the ORCID activity from which to retrieve the external
	 *            identifiers
	 * @return the non-null self external identifiers
	 */
	public final ExternalIds getSelfExternalIdsE(E activity) {
		return getSelfExternalIdsS(summarize(activity));
	}

	/**
	 * Calculates the symmetric difference of self {@link ExternalId external
	 * identifiers} between an activity summary and a set of activities.
	 * Elements that do not match (i.e., no identifier is common) are ignored.
	 *
	 * @param summary
	 *            the activity summary to be compared with other activities
	 * @param activities
	 *            the set of activities against which the activity summary is
	 *            compared
	 * @return The symmetric difference of self external identifiers between the
	 *         summary and other activities
	 */
	public final Map<E, ExternalIdsDiff> getSelfExternalIdsDiffS(S summary, Collection<E> activities) {
		if (summary == null)
			throw new IllegalArgumentException("Null element.");
		if (activities == null)
			activities = new HashSet<E>();

		final Map<E, ExternalIdsDiff> matches = new HashMap<E, ExternalIdsDiff>();
		for (E match : activities) {
			final ExternalIdsDiff diff = new ExternalIdsDiff(
					getSelfExternalIdsE(match), getSelfExternalIdsS(summary));
			if (!diff.same.isEmpty())
				matches.put(match, diff);
		}
		return matches;
	}
	
	
	public final ExternalIds getFundedByExternalIdsE(E activity) {
		return getFundedByExternalIdsS(summarize(activity));
	}
	

	/**
	 * Tests whether two sets of (non-exclusively self or part-of) external
	 * identifiers are identical.
	 * 
	 * TODO: the URLs assigned to the external ids are not being considered in
	 * this comparison.
	 * 
	 * @param eids1
	 *            the first set of external identifiers
	 * @param eids2
	 *            the second set of external identifiers
	 * @return whether the external identifiers are identical
	 */
	static boolean identicalExternalIDs(ExternalIds eids1,
			ExternalIds eids2) {
		assert eids1 != null;
		assert eids2 != null;
		
		final ExternalIdsDiff diff = new ExternalIdsDiff(eids1, eids2);
		return diff.more.isEmpty() && diff.less.isEmpty();
	}

	/**
	 * Checks whether an activity is already up to date regarding another one
	 * regarding self {@link ExternalId external identifiers}.
	 *
	 * This test is expected to be used by the import algorithms, where only new
	 * self external identifiers are to be considered.
	 *
	 * @param preElement
	 *            the potentially out of date ORCID activity
	 * @param posElement
	 *            the up to date ORCID activity
	 * @return true if all the self external identifiers between the two
	 *         activities are the same, false otherwise
	 */
	public final boolean hasNewSelfIDs(E preElement, S posElement) {
		if (preElement == null || posElement == null)
			throw new IllegalArgumentException("Null element.");
		
		final ExternalIdsDiff diff = new ExternalIdsDiff(
				getSelfExternalIdsE(preElement),
				getSelfExternalIdsS(posElement));

		return diff.more.isEmpty();
	}
	
	
	public final ExternalIdsDiff getFundedByExternalIdsDiff(E preElement, S posElement) {
		if (preElement == null || posElement == null)
			throw new IllegalArgumentException("Null element.");
		
		final ExternalIdsDiff diff = new ExternalIdsDiff(
				getFundedByExternalIdsE(preElement),
				getFundedByExternalIdsS(posElement));

		return diff;
	}

	/**
	 * Checks whether an activity is already up to date regarding another one,
	 * considering the self {@link ExternalIdentifier external identifiers}.
	 * This comparison disregards the URLs assigned to the identifiers.
	 *
	 * @param preElement
	 *            the potentially out of date ORCID activity
	 * @param posElement
	 *            the up to date ORCID activity
	 * @return true if all the self external identifiers are the same, false
	 *         otherwise.
	 */
	private final boolean isSelfExternalIDsUpToDate(E preElement, S posElement) {
		assert preElement != null;
		assert posElement != null;

		return identicalExternalIDs(getSelfExternalIdsE(preElement),
				getSelfExternalIdsS(posElement));
	}

	/**
	 * Checks whether an activity is already up to date regarding another one,
	 * considering the self {@link ExternalId external identifiers} and
	 * additional meta-data. Only meta-data existent in the activity summaries
	 * is conside
	 *
	 * This test is expected to be used by the export algorithms, where the
	 * meta-data is expected to be up-to-date on the remote profile.
	 *
	 * @param preElement
	 *            the potentially out of date ORCID activity
	 * @param posElement
	 *            the up to date ORCID activity
	 * @return true if all the self external identifiers and the meta-data
	 *         between the two activities are the same, false otherwise
	 */
	public final boolean isUpToDateS(E preElement, S posElement) {
		if (preElement == null || posElement == null)
			throw new IllegalArgumentException("Null element.");
		
		return isSelfExternalIDsUpToDate(preElement, posElement)
				&& isMetaUpToDate(preElement, posElement);
	}

	/**
	 * Checks whether an activity is already up to date regarding another one,
	 * considering the self {@link ExternalId external identifiers} and
	 * additional meta-data. Only meta-data existent in the activity summaries
	 * is considered. Builds on
	 * {@link #isUpToDateS(ElementSummary, ElementSummary)}.
	 *
	 * This test is expected to be used by the export algorithms, where the
	 * meta-data is expected to be up-to-date on the remote profile.
	 *
	 * @param preElement
	 *            the potentially out of date ORCID activity
	 * @param posElement
	 *            the up to date ORCID activity
	 * @return true if all the self external identifiers and the meta-data
	 *         between the two activities are the same, false otherwise
	 */
	public final boolean isUpToDateE(E preElement, E posElement) {
		if (preElement == null || posElement == null)
			throw new IllegalArgumentException("Null element.");
		
		return isUpToDateS(preElement, summarize(posElement));
	}

	/**
	 * Tests whether an activity has minimal quality to be synchronized, by
	 * inspecting its meta-data. Throws an exception if the test fails. Only
	 * meta-data available in activity summaries is considered.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 * 
	 * @param activity
	 *            the activity to test for quality
	 * @param others
	 *            other coexisting activities
	 * @throws InvalidActivityException
	 *             if the quality test fails, containing the reasons for failing
	 */
	public final void tryMinimalQualityE(E activity, Collection<E> others)
			throws InvalidActivityException {
		if (activity == null)
			throw new IllegalArgumentException("Null activity.");
		if (others == null)
			others = new HashSet<E>();

		Set<String> invs = testMinimalQuality(summarize(activity), others);
		if (!invs.isEmpty()) {
			throw new InvalidActivityException(invs);
		}
	}

	/**
	 * Tests whether an activity summary has minimal quality to be synchronized,
	 * by inspecting its meta-data and that of coexisting activities, and
	 * returns the detected invalid fields. Only uses meta-data available in
	 * activity summaries. Builds on
	 * {@link #testMinimalQuality(ElementSummary, Collection)}.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 * 
	 * @param summary
	 *            the ORCID activity summary to test for quality
	 * @return the set of invalid meta-data, empty if valid
	 */
	public final Set<String> testMinimalQuality(S summary) {
		return testMinimalQuality(summary, new HashSet<E>());
	}

	/**
	 * Tests whether a date is well constructed.
	 * 
	 * @param date
	 *            the date to be tested
	 * @return whether the date is well formed
	 */
	static boolean testQualityFuzzyDate(FuzzyDate date) {
		if (date.getYear() != null && String.valueOf(date.getYear().getValue()).length() != 4)
			return false;
		if (date.getMonth() != null && date.getMonth().getValue() < 1 && date.getMonth().getValue() > 12 )
			return false;
		if (date.getDay() != null && date.getDay().getValue() < 1 && date.getDay().getValue() > 31)
			return false;
		
		return true;
	}
	
}
