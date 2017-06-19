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
import org.um.dsi.gavea.orcid.model.funding.FundingType;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;
import org.um.dsi.gavea.orcid.model.work.WorkType;

import pt.ptcris.ORCIDClient;
import pt.ptcris.PTCRISyncResult;
import pt.ptcris.exceptions.InvalidWorkException;
import pt.ptcris.handlers.ProgressHandler;

/**
 * An abstract helper to simplify the use of the low-level ORCID
 * {@link pt.ptcris.ORCIDClient client}. Supports bulk requests when available.
 * The concrete ORCID elements to be managed by the helper are expected to
 * support {@link ExternalIds external identifiers}.
 * 
 * Provides support for asynchronous communication with ORCID although it is
 * only active for GET requests due to resource limitations.
 * 
 * @param <E>
 *            The class of ORCID elements being synchronized
 * @param <S>
 *            The class of ORCID element summaries
 * @param <G>
 *            The class of ORCID element groups
 * @param <T>
 *            The class of ORCID element types
 */
public abstract class ORCIDHelper<E extends ElementSummary, S extends ElementSummary, G, T extends Enum<T>> {

	/**
	 * Creates a static (i.e., no server connection) ORCID helper for work
	 * elements.
	 * 
	 * @return the ORCID work helper
	 */
	public static ORCIDHelper<Work, WorkSummary, WorkGroup, WorkType> factoryStaticWorks() {
		return new ORCIDWorkHelper(null);
	}

	/**
	 * Creates a static (i.e., no server connection) ORCID helper for funding
	 * elements.
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
	public static final String OVERLAPPING_EIDs = "OverlappingEIDs";

	protected final int bulk_size_add;
	protected final int bulk_size_get;

	private static final Logger _log = LoggerFactory
			.getLogger(ORCIDHelper.class);

	/**
	 * The client used to communicate with ORCID. Defines the ORCID user profile
	 * being managed and the Member API id being user to source elements.
	 */
	public final ORCIDClient client;

	protected ExecutorService executor;

	/**
	 * Initializes the helper with a given ORCID client, and sets whether bulk
	 * ORCID commands are available and with which size.
	 *
	 * @param orcidClient
	 *            the ORCID client
	 * @param bulk_size_add
	 *            number of elements per bulk add request
	 * @param bulk_size_get
	 *            number of elements per bulk get request
	 */
	public ORCIDHelper(ORCIDClient orcidClient, int bulk_size_add,
			int bulk_size_get) {
		this.client = orcidClient;
		this.bulk_size_add = bulk_size_add;
		this.bulk_size_get = bulk_size_get;
		if (client != null && client.threads() > 1)
			executor = Executors.newFixedThreadPool(client.threads());
	}

	/*
	 * Generic client methods to be instantiated for concrete ORCID element
	 * types.
	 */

	/**
	 * Generic method to retrieve all ORCID groups through the ORCID client.
	 * 
	 * @return the remote ORCID elements
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	protected abstract List<G> getSummariesClient() throws OrcidClientException;

	/**
	 * Generic method that retrieves through the ORCID client a single full
	 * element for which the summary is provided. If the communication with
	 * ORCID fails, the exception is embedded in a failed
	 * {@link PTCRISyncResult}.
	 * 
	 * @param summary
	 *            the summary for which to read the full ORCID element
	 * @return the remote full ORCID element
	 */
	protected abstract PTCRISyncResult getClient(S summary);

	/**
	 * Generic method that retrieves through the ORCID client every full element
	 * for which summaries are provided. If the communication with ORCID fails,
	 * the exception is embedded in a failed {@link PTCRISyncResult}.
	 * 
	 * @param summaries
	 *            the summaries for which to read full ORCID elements
	 * @return the remote full ORCID elements
	 */
	protected abstract Map<BigInteger, PTCRISyncResult> getClient(
			List<S> summaries);

	/**
	 * Generic method that creates a worker to asynchronously read a single full
	 * element for which the summary is provided. If the communication with
	 * ORCID fails, the exception is embedded in a failed
	 * {@link PTCRISyncResult}.
	 * 
	 * @param summary
	 *            the summary for which to read the full ORCID element
	 * @param cb
	 *            the callback on which to report results
	 * @param log
	 *            the logger
	 * @return the get worker
	 */
	protected abstract ORCIDWorker readWorker(S summary,
			Map<BigInteger, PTCRISyncResult> cb, Logger log);

	/**
	 * Generic method that creates a worker to asynchronously read a full
	 * elements for which the summaries are provided. If the communication with
	 * ORCID fails, the exception is embedded in a failed
	 * {@link PTCRISyncResult}.
	 * 
	 * @param summaries
	 *            the summary for which to read the full ORCID element
	 * @param cb
	 *            the callback on which to report results
	 * @param log
	 *            the logger
	 * @return the get worker
	 */
	protected abstract ORCIDWorker readWorker(List<S> summaries,
			Map<BigInteger, PTCRISyncResult> cb, Logger log);

	/**
	 * Generic method that adds through the ORCID client a new full element. If
	 * the communication with ORCID fails, the exception is embedded in a failed
	 * {@link PTCRISyncResult}.
	 * 
	 * @param element
	 *            the full ORCID element to be added
	 * @return the result of the operation
	 */
	protected abstract PTCRISyncResult addClient(E element);

	/**
	 * Generic method that adds through the ORCID client a set of new full
	 * elements. If the communication with ORCID fails, the exception is
	 * embedded in a failed {@link PTCRISyncResult}.
	 * 
	 * @param elements
	 *            the full ORCID elements to be added
	 * @return the result of the operation
	 */
	protected abstract List<PTCRISyncResult> addClient(List<E> elements);

	/**
	 * Generic method that updates through the ORCID client a remote element. If
	 * the communication with ORCID fails, the exception is embedded in a failed
	 * {@link PTCRISyncResult}.
	 * 
	 * @param remotePutcode
	 *            the put-code of the remote ORCID element
	 * @param element
	 *            the new state of the ORCID element
	 * @return the result of the operation
	 */
	protected abstract PTCRISyncResult updateClient(BigInteger remotePutcode,
			E element);

	/**
	 * Generic method that deletes through the ORCID client a remote element. If
	 * the communication with ORCID fails, the exception is embedded in a failed
	 * {@link PTCRISyncResult}.
	 * 
	 * @param remotePutcode
	 *            the put-code of the remote ORCID element
	 * @return the result of the operation
	 */
	protected abstract PTCRISyncResult deleteClient(BigInteger remotePutcode);

	/*
	 * Helper client methods that build on the generic methods.
	 */

	/**
	 * Retrieves the entire set of element summaries from the set ORCID profile
	 * that have at least an external identifier set. Merges each ORCID group
	 * into a single summary, following {@link #group(Object)}.
	 *
	 * @return the set of ORCID element summaries in the defined ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	private final List<S> getAllSummaries() throws OrcidClientException {
		_log.debug("[getSummaries]");

		final List<S> fundSummaryList = new LinkedList<S>();
		final List<G> fundGroupList = getSummariesClient();
		for (G group : fundGroupList)
			fundSummaryList.add(group(group));
		return fundSummaryList;
	}

	/**
	 * Retrieves the entire set of element summaries of given types from the set
	 * ORCID profile that have at least an external identifier set. Merges each
	 * ORCID group into a single summary, following {@link #group(Object)}.
	 * 
	 * @param types
	 *            the ORCID types of the elements to be retrieved
	 * @return the set of ORCID element summaries in the defined ORCID profile
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
	 * Retrieves the entire set (i.e., not merged) of element summaries in the
	 * ORCID profile whose source is the Member API id defined in the ORCID
	 * client.
	 *
	 * @return the set of ORCID element summaries in the ORCID profile for the
	 *         defined source
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public final List<S> getSourcedSummaries() throws OrcidClientException {
		final String sourceClientID = client.getClientId();

		_log.debug("[getSourcedSummaries] " + sourceClientID);

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
	 * Reads a full ORCID element from an ORCID profile and adds it to a
	 * callback map. The resulting element contains every external identifier
	 * set in the input element summary, because the summary resulted from the
	 * merging of a group, but the retrieved full element is a single element.
	 * It also clears the put-code, since at this level they represent the local
	 * identifier. If possible the number of threads is higher than 1, process
	 * is asynchronous. If the list is not a singleton, a bulk request will be
	 * performed if supported for the concrete ORCID element type. If the
	 * communication with ORCID fails for any element, the exceptions are
	 * embedded in failed {@link PTCRISyncResult}.
	 *
	 * @see #getClient(List)
	 * 
	 * @param summaries
	 *            the ORCID element summaries representing the merged groups
	 * @param cb
	 *            the callback object
	 * @param handler
	 *            the handler to report progress
	 * @throws NullPointerException
	 *             if the merged element is null
	 * @throws InterruptedException
	 *             if the asynchronous GET process is interrupted
	 */
	public final void getFulls(List<S> summaries,
			Map<BigInteger, PTCRISyncResult> cb, ProgressHandler handler)
			throws OrcidClientException, NullPointerException,
			InterruptedException {
		if (summaries == null)
			throw new NullPointerException("Can't get null element.");
		_log.debug("[getBulk] " + summaries.size());
		if (handler != null)
			handler.setCurrentStatus("ORCID_GET_ITERATION");

		if (client.threads() > 1 && cb != null) {
			for (int i = 0; i < summaries.size();) {
				int progress = (int) ((double) i / summaries.size() * 100);
				if (handler != null)
					handler.setProgress(progress);
				if (bulk_size_get > 1) {
					List<S> putcodes = new ArrayList<S>();
					for (int j = 0; j < bulk_size_get && i < summaries.size(); j++) {
						putcodes.add(summaries.get(i));
						i++;
					}
					final ORCIDWorker worker = readWorker(putcodes, cb, _log);
					executor.execute(worker);
				} else {
					final ORCIDWorker worker = readWorker(summaries.get(i), cb,
							_log);
					executor.execute(worker);
					i++;
				}
			}
		} else {
			Map<BigInteger, PTCRISyncResult> fulls = new HashMap<BigInteger, PTCRISyncResult>();
			for (int i = 0; i < summaries.size();) {
				int progress = (int) ((double) i / summaries.size() * 100);
				if (handler != null)
					handler.setProgress(progress);
				if (bulk_size_get > 1) {
					List<S> putcodes = new ArrayList<S>();
					for (int j = 0; j < bulk_size_get && i < summaries.size(); j++) {
						putcodes.add(summaries.get(i));
						i++;
					}
					fulls.putAll(getClient(putcodes));
				} else {
					fulls.put(summaries.get(i).getPutCode(),
							getClient(summaries.get(i)));
					i++;
				}
			}
			cb.putAll(fulls);
		}
		waitWorkers();
	}

	/**
	 * Synchronously adds an element to an ORCID profile. The OK result includes
	 * the newly assigned put-code. If the communication with ORCID fails, the
	 * exception is embedded in a failed {@link PTCRISyncResult}.
	 *
	 * @see #addClient(ElementSummary)
	 * 
	 * @param element
	 *            the ORCID element to be added
	 * @return the result of the ORCID call
	 * @throws NullPointerException
	 *             if the element is null
	 */
	private final PTCRISyncResult add(E element) throws NullPointerException {
		if (element == null)
			throw new NullPointerException("Can't add null element.");

		_log.debug("[add] " + getTitleE(element));

		// remove any put-code otherwise ORCID will throw an error
		final E clone = cloneE(element);
		clone.setPutCode(null);

		return addClient(clone);
	}

	/**
	 * Synchronously adds a list of elements to an ORCID profile. A list of
	 * results is returned, one for each input element. The OK result includes
	 * the newly assigned put-code. If the communication with ORCID fails, the
	 * exception is embedded in a failed {@link PTCRISyncResult}. If the overall
	 * communication fails, the result is replicated for each input.
	 *
	 * @see #addClient(List)
	 * 
	 * @param elements
	 *            the new ORCID elements to be added
	 * @return the results of the ORCID call for each input element
	 * @throws NullPointerException
	 *             if the element is null
	 */
	private final List<PTCRISyncResult> add(Collection<E> elements)
			throws NullPointerException {
		if (elements == null)
			throw new NullPointerException("Can't add null elements.");

		_log.debug("[addBulk] " + elements.size());

		List<E> clones = new ArrayList<E>();
		// remove any put-code otherwise ORCID will throw an error
		for (E element : elements) {
			final E clone = cloneE(element);
			clone.setPutCode(null);
			clones.add(clone);
		}

		return addClient(clones);
	}

	/**
	 * Synchronously adds a list of elements to an ORCID profile, either through
	 * atomic or bulk calls if available. A list of results is returned, one for
	 * each input element. The OK result includes the newly assigned put-code.
	 * If the communication with ORCID fails, the exception is embedded in a
	 * failed {@link PTCRISyncResult}. If the overall communication fails, the
	 * result is replicated for each input.
	 *
	 * @param elements
	 *            the new ORCID elements to be added
	 * @return the results of the ORCID call for each input element
	 * @throws NullPointerException
	 *             if an element is null
	 */
	public final List<PTCRISyncResult> add(List<E> elements,
			ProgressHandler handler) throws NullPointerException {
		List<PTCRISyncResult> res = new ArrayList<PTCRISyncResult>();
		if (handler != null)
			handler.setCurrentStatus("ORCID_ADDING_WORKS");

		for (int c = 0; c != elements.size();) {
			int progress = (int) ((double) c / elements.size() * 100);
			if (handler != null)
				handler.setProgress(progress);

			if (bulk_size_add > 1) {
				List<E> tmp = new ArrayList<E>();
				for (int j = 0; j < bulk_size_add && c < elements.size(); j++) {
					tmp.add(elements.get(c));
					c++;
				}
				res.addAll(this.add(tmp));
			} else {
				E local = elements.get(c);
				res.add(this.add(local));
				c++;
			}
		}
		return res;
	}

	/**
	 * Synchronously updates an element to an ORCID profile. If the
	 * communication with ORCID fails, the exception is embedded in a failed
	 * {@link PTCRISyncResult}.
	 * 
	 * @see #updateClient(BigInteger, ElementSummary)
	 * 
	 * @param remotePutcode
	 *            the put-code of the remote ORCID element that will be updated
	 * @param updated
	 *            the new state of the element that will be updated
	 * @return the result of the ORCID call
	 * @throws NullPointerException
	 *             if either parameter is null
	 */
	public final PTCRISyncResult update(BigInteger remotePutcode, E updated)
			throws NullPointerException {
		if (remotePutcode == null || updated == null)
			throw new NullPointerException("Can't update null element.");

		_log.debug("[update] " + remotePutcode);

		final E clone = cloneE(updated);
		// set the remote put-code
		clone.setPutCode(remotePutcode);

		return updateClient(remotePutcode, clone);
	}

	/**
	 * Synchronously deletes an element from an ORCID profile. If the
	 * communication with ORCID fails, the exception is embedded in a failed
	 * {@link PTCRISyncResult}.
	 * 
	 * @see #deleteClient(BigInteger)
	 * 
	 * @param putcode
	 *            the remote put-code of the ORCID element to be deleted
	 * @throws NullPointerException
	 *             if the put-code is null
	 */
	public final PTCRISyncResult delete(BigInteger putcode)
			throws NullPointerException {
		if (putcode == null)
			throw new NullPointerException("Can't delete null element.");

		_log.debug("[delete] " + putcode);

		return deleteClient(putcode);
	}

	/**
	 * Deletes the entire set of element summaries in the ORCID profile whose
	 * source is the Member API id defined in the ORCID client.
	 *
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public final void deleteAllSourced() throws OrcidClientException {
		_log.debug("[deleteSourced] " + client.getClientId());

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
	 * Generic static methods to be instantiated for concrete ORCID element
	 * types.
	 */

	/**
	 * Returns the non-null external identifiers of an element (null becomes
	 * empty list). Cannot rely on
	 * {@link #getNonNullExternalIdsS(ElementSummary)} because
	 * {@link #summarize(ElementSummary)} itself calls this method.
	 * 
	 * @param element
	 *            the ORCID element from which to retrieve the external
	 *            identifiers
	 * @return the non-null external identifiers
	 */
	protected abstract ExternalIds getNonNullExternalIdsE(E element);

	/**
	 * Returns the non-null external identifiers of an element summary (null
	 * becomes empty list).
	 * 
	 * @param summary
	 *            the ORCID element summary from which to retrieve the external
	 *            identifiers
	 * @return the non-null external identifiers
	 */
	protected abstract ExternalIds getNonNullExternalIdsS(S summary);

	/**
	 * Assigns a set of external identifiers to an element.
	 * 
	 * @param element
	 *            the ORCID element to which to assign the external identifiers
	 * @param eids
	 *            the external identifiers to be assigned
	 */
	public abstract void setExternalIdsE(E element, ExternalIds eids);

	/**
	 * Assigns a set of external identifiers to an element summary.
	 * 
	 * @param summary
	 *            the ORCID element summary to which to assign the external
	 *            identifiers
	 * @param eids
	 *            the external identifiers to be assigned
	 */
	public abstract void setExternalIdsS(S summary, ExternalIds eids);

	/**
	 * Retrieves the type of an element summary.
	 *
	 * @param summary
	 *            the ORCID element summary
	 * @return the summary's type
	 */
	protected abstract T getTypeS(S sumary);

	/**
	 * Tests whether a given external identifier type name is valid.
	 * 
	 * @param eid
	 *            a potential external identifier type name
	 * @return whether the string is a valid external identifier type
	 */
	protected abstract boolean validExternalIdType(String eid);

	/**
	 * Retrieves the title of an element summary.
	 *
	 * @param summary
	 *            the ORCID element summary
	 * @return the summary's title if defined, empty string otherwise
	 */
	protected abstract String getTitleS(S summary);

	/**
	 * Retrieves the publication year of an element summary.
	 *
	 * @param summary
	 *            the ORCID element summary
	 * @return the summary's publication year, may be null
	 */
	protected abstract String getYearS(S summary);

	/**
	 * Retrieve the element summaries that compose an element group.
	 * 
	 * @param group
	 *            the ORCID group from which to retrieve the ORCID element
	 *            summaries
	 * @return the OCRID element summaries contained in the group
	 */
	protected abstract List<S> getGroupSummaries(G group);

	/**
	 * Merges an element group into a single element summary. Simply selects the
	 * meta-data (including part-of external identifiers) from the first element
	 * of the group (i.e., the preferred one) and assigns it any extra (self)
	 * external identifiers from the remainder elements. These remainder
	 * identifiers are the ones grouped by ORCID.
	 *
	 * @param group
	 *            the element group to be merged
	 * @return the resulting element summary
	 * @throws NullPointerException
	 *             if the group is null
	 * @throws IllegalArgumentException
	 *             if the group is empty
	 */
	protected abstract S group(G group) throws NullPointerException,
			IllegalArgumentException;

	/**
	 * Checks whether an element is already up to date regarding another one,
	 * considering meta-data other than the self external identifiers. Uses only
	 * meta-data is available in element summaries.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 *
	 * @param preElement
	 *            the potentially out of date ORCID element
	 * @param posElement
	 *            the up to date ORCID element
	 * @return true if the considered meta-data is the same, false otherwise.
	 * @throws NullPointerException
	 *             if either element is null
	 */
	protected abstract boolean isMetaUpToDate(E preElement, S posElement)
			throws NullPointerException;

	/**
	 * Tests whether an element summary has minimal quality to be synchronized,
	 * by inspecting its meta-data and that of coexisting elements, and returns
	 * the detected invalid fields. Only uses meta-data available in element
	 * summaries. Coexisting element may be used to test for overlaps.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 * 
	 * @param summary
	 *            the ORCID element summary to test for quality
	 * @param others
	 *            other coexisting elements
	 * @return the set of invalid meta-data, empty if valid
	 * @throws NullPointerException
	 *             if the element is null
	 */
	protected abstract Set<String> testMinimalQuality(S summary,
			Collection<E> others) throws NullPointerException;

	/**
	 * Creates an update to an element given the difference on meta-data.
	 * Essentially creates an element with the same put-code as the original
	 * element and with the new meta-data that must be assigned to it.
	 * Currently, only new external identifiers are considered.
	 * 
	 * @param original
	 *            the original ORCID element
	 * @param diff
	 *            the difference on external identifiers
	 * @return the update to be applied to the ORCID element
	 */
	public abstract E createUpdate(E original, ExternalIdsDiff diff);

	/**
	 * Clones an element summary.
	 * 
	 * @param summary
	 *            the ORCID element summary to be cloned
	 * @return the cloned ORCID element summary
	 */
	protected abstract S cloneS(S summary);

	/**
	 * Clones an element.
	 * 
	 * @param element
	 *            the ORCID element to be cloned
	 * @return the cloned ORCID element
	 */
	protected abstract E cloneE(E element);

	/**
	 * Summarizes an element into an element summary. Most methods on elements
	 * rely on this to re-use methods on element summaries.
	 * 
	 * @param element
	 *            the ORCID element to be summarized
	 * @return the corresponding ORCID element summary
	 */
	protected abstract S summarize(E element);

	/*
	 * Helper static methods that build on the generic methods.
	 */

	/**
	 * Retrieves the local key of an element, currently assumed to be stored in
	 * the put-code field.
	 *
	 * @param element
	 *            the ORCID element from which to get the local key
	 * @return the local key
	 * @throws NullPointerException
	 *             if the element is null
	 */
	public static BigInteger getActivityLocalKey(ElementSummary element)
			throws NullPointerException {
		if (element == null)
			throw new NullPointerException("Can't get local key from null.");

		return element.getPutCode();
	}

	/**
	 * Retrieves the local key of an element, currently assumed to be stored in
	 * the put-code field. If empty, returns a default value.
	 *
	 * @param element
	 *            the ORCID element from which to get the local key
	 * @param defaultValue
	 *            a default value in case the put-code is empty
	 * @return the local key
	 * @throws NullPointerException
	 *             if the element is null
	 */
	public static BigInteger getActivityLocalKey(ElementSummary element,
			BigInteger defaultValue) throws NullPointerException {

		BigInteger putCode = getActivityLocalKey(element);
		if (putCode == null)
			putCode = defaultValue;

		return putCode;
	}

	/**
	 * Assign a local key to an element, currently assumed to be stored in the
	 * put-code field.
	 *
	 * @param element
	 *            the element to which to set the local key
	 * @param key
	 *            the local key
	 * @throws NullPointerException
	 *             if the element is null
	 */
	protected static void setWorkLocalKey(ElementSummary element, BigInteger key)
			throws NullPointerException {
		if (element == null)
			throw new NullPointerException("Can't set local key to null.");

		element.setPutCode(key);
	}

	/**
	 * Clears (sets to null) the local key of an element, currently assumed to
	 * be stored in the put-code field.
	 *
	 * @param element
	 *            the element to which to clear the local key
	 * @throws NullPointerException
	 *             if the element is null
	 */
	public static void cleanWorkLocalKey(ElementSummary element)
			throws NullPointerException {
		if (element == null)
			throw new NullPointerException("Can't clear local key from null.");

		element.setPutCode(null);
	}

	/**
	 * Copies all meta-data from an element summary into another.
	 * 
	 * @param from
	 *            the source summary
	 * @param to
	 *            the target summary
	 * @throws NullPointerException
	 *             if either argument is null
	 */
	protected static void copy(ElementSummary from, ElementSummary to)
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
	 * Retrieves the type of an element. Build on
	 * {@link #getTypeS(ElementSummary)}.
	 *
	 * @param element
	 *            the ORCID element
	 * @return the element's type
	 */
	public final T getTypeE(E element) {
		return getTypeS(summarize(element));
	}

	/**
	 * Retrieves the title of an element. Builds on
	 * {@link #getTitleS(ElementSummary)}.
	 *
	 * @param element
	 *            the ORCID element
	 * @return the element's title if defined, empty string otherwise
	 */
	protected final String getTitleE(E element) {
		return getTitleS(summarize(element));
	}

	/**
	 * Retrieves the publication year of an element. Builds on
	 * {@link #getYearS(ElementSummary)}.
	 *
	 * @param element
	 *            the ORCID element
	 * @return the element's publication year, may be null
	 */
	protected final String getPubYearE(E element) {
		return getYearS(summarize(element));
	}

	/**
	 * Returns the non-null part-of external identifiers of an element summary
	 * (null becomes empty list).
	 * 
	 * @param summary
	 *            the ORCID element summary from which to retrieve the external
	 *            identifiers
	 * @return the non-null part-of external identifiers
	 */
	protected final ExternalIds getPartOfExternalIdsS(S summary) {
		List<ExternalId> res = new ArrayList<ExternalId>();
		for (ExternalId eid : getNonNullExternalIdsS(summary).getExternalId())
			if (eid.getExternalIdRelationship() == RelationshipType.PART_OF)
				res.add(eid);
		return new ExternalIds(res);
	}

	/**
	 * Returns the non-null part-of external identifiers of an element (null
	 * becomes empty list). Builds on
	 * {@link #getPartOfExternalIdsS(ElementSummary)}.
	 * 
	 * @param element
	 *            the ORCID element from which to retrieve the external
	 *            identifiers
	 * @return the non-null part-of external identifiers
	 */
	public final ExternalIds getPartOfExternalIdsE(E element) {
		return getPartOfExternalIdsS(summarize(element));
	}

	/**
	 * Returns the non-null self external identifiers of an element summary
	 * (null becomes empty list).
	 * 
	 * @param summary
	 *            the ORCID element summary from which to retrieve the external
	 *            identifiers
	 * @return the non-null self external identifiers
	 */
	protected final ExternalIds getSelfExternalIdsS(S summary) {
		List<ExternalId> res = new ArrayList<ExternalId>();
		for (ExternalId eid : getNonNullExternalIdsS(summary).getExternalId())
			if (eid.getExternalIdRelationship() == RelationshipType.SELF)
				res.add(eid);
		return new ExternalIds(res);
	}

	/**
	 * Returns the non-null self external identifiers of an element (null
	 * becomes empty list). Builds on
	 * {@link #getSelfExternalIdsS(ElementSummary)}.
	 * 
	 * @param element
	 *            the ORCID element from which to retrieve the external
	 *            identifiers
	 * @return the non-null self external identifiers
	 */
	public final ExternalIds getSelfExternalIdsE(E element) {
		return getSelfExternalIdsS(summarize(element));
	}

	/**
	 * Calculates the symmetric difference of self {@link ExternalId external
	 * identifiers} between an element summary and a set of elements. Elements
	 * that do not match (i.e., no identifier is common) are ignored.
	 *
	 * @param summary
	 *            the element summary to be compared with other elements
	 * @param elements
	 *            the set of elements against which the element summary is
	 *            compared
	 * @return The symmetric difference of self external identifiers between the
	 *         summary and other elements
	 * @throws NullPointerException
	 *             if either of the parameters is null
	 */
	public final Map<E, ExternalIdsDiff> getSelfExternalIdsDiffS(S summary,
			Collection<E> elements) throws NullPointerException {
		if (summary == null || elements == null)
			throw new NullPointerException("Can't get external ids.");

		final Map<E, ExternalIdsDiff> matches = new HashMap<E, ExternalIdsDiff>();
		for (E match : elements) {
			final ExternalIdsDiff diff = new ExternalIdsDiff(
					getSelfExternalIdsE(match), getSelfExternalIdsS(summary));
			if (!diff.same.isEmpty())
				matches.put(match, diff);
		}
		return matches;
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
	protected static boolean identicalExternalIDs(ExternalIds eids1,
			ExternalIds eids2) {
		final ExternalIdsDiff diff = new ExternalIdsDiff(eids1, eids2);
		return diff.more.isEmpty() && diff.less.isEmpty();
	}

	/**
	 * Checks whether an element is already up to date regarding another one
	 * regarding self {@link ExternalId external identifiers}.
	 *
	 * This test is expected to be used by the import algorithms, where only new
	 * self external identifiers are to be considered.
	 *
	 * @param preElement
	 *            the potentially out of date ORCID element
	 * @param posElement
	 *            the up to date ORCID element
	 * @return true if all the self external identifiers between the two
	 *         elements are the same, false otherwise
	 */
	public final boolean hasNewSelfIDs(E preElement, S posElement) {
		final ExternalIdsDiff diff = new ExternalIdsDiff(
				getSelfExternalIdsE(preElement),
				getSelfExternalIdsS(posElement));

		return diff.more.isEmpty();
	}

	/**
	 * Checks whether an element is already up to date regarding another one,
	 * considering the self {@link ExternalIdentifier external identifiers}.
	 *
	 * @param preElement
	 *            the potentially out of date ORCID element
	 * @param posElement
	 *            the up to date ORCID element
	 * @return true if all the self external identifiers are the same, false
	 *         otherwise.
	 * @throws NullPointerException
	 *             if either element is null
	 */
	private final boolean isSelfExternalIDsUpToDate(E preElement, S posElement)
			throws NullPointerException {
		if (preElement == null || posElement == null)
			throw new NullPointerException("Can't test null works.");

		return identicalExternalIDs(getSelfExternalIdsE(preElement),
				getSelfExternalIdsS(posElement));
	}

	/**
	 * Checks whether an element is already up to date regarding another one,
	 * considering the self {@link ExternalId external identifiers} and
	 * additional meta-data. Only meta-data existent in the element summaries is
	 * conside
	 *
	 * This test is expected to be used by the export algorithms, where the
	 * meta-data is expected to be up-to-date on the remote profile.
	 *
	 * @param preElement
	 *            the potentially out of date ORCID element
	 * @param posElement
	 *            the up to date ORCID element
	 * @return true if all the self external identifiers and the meta-data
	 *         between the two elements are the same, false otherwise
	 */
	public final boolean isUpToDateS(E preElement, S posElement) {
		return isSelfExternalIDsUpToDate(preElement, posElement)
				&& isMetaUpToDate(preElement, posElement);
	}

	/**
	 * Checks whether an element is already up to date regarding another one,
	 * considering the self {@link ExternalId external identifiers} and
	 * additional meta-data. Only meta-data existent in the element summaries is
	 * considered. Builds on {@link #isUpToDateS(ElementSummary, ElementSummary)}.
	 *
	 * This test is expected to be used by the export algorithms, where the
	 * meta-data is expected to be up-to-date on the remote profile.
	 *
	 * @param preElement
	 *            the potentially out of date ORCID element
	 * @param posElement
	 *            the up to date ORCID element
	 * @return true if all the self external identifiers and the meta-data
	 *         between the two elements are the same, false otherwise
	 */
	public final boolean isUpToDateE(E preElement, E posElement) {
		return isUpToDateS(preElement, summarize(posElement));
	}

	/**
	 * Tests whether an element has minimal quality to be synchronized, by
	 * inspecting its meta-data. Throws an exception if the test fails. Only
	 * meta-data available in element summaries is considered.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 * 
	 * @param element
	 *            the element to test for quality
	 * @param others
	 *            other coexisting elements
	 * @throws InvalidWorkException
	 *             if the quality test fails, containing the reasons for failing
	 * @throws NullPointerException
	 *             if the element is null
	 */
	public final void tryMinimalQualityE(E element, Collection<E> others)
			throws InvalidWorkException {
		Set<String> invs = testMinimalQuality(summarize(element), others);
		if (!invs.isEmpty()) {
			throw new InvalidWorkException(invs);
		}
	}

	/**
	 * Tests whether an element summary has minimal quality to be synchronized,
	 * by inspecting its meta-data and that of coexisting elements, and returns
	 * the detected invalid fields. Only uses meta-data available in element
	 * summaries. Builds on
	 * {@link #testMinimalQuality(ElementSummary, Collection)}.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 * 
	 * @param summary
	 *            the ORCID element summary to test for quality
	 * @param others
	 *            other coexisting elements
	 * @return the set of invalid meta-data, empty if valid
	 * @throws NullPointerException
	 *             if the element is null
	 */
	public final Set<String> testMinimalQuality(S work)
			throws NullPointerException {
		return testMinimalQuality(work, new HashSet<E>());
	}

}
