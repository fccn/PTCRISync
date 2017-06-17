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
 * An helper to simplify the use of the low-level ORCID
 * {@link pt.ptcris.ORCIDClient client}.
 * 
 * Provides support for asynchronous communication with ORCID
 * although it is only active for GET requests due to resource
 * limitations.
 */
public abstract class ORCIDHelper<E extends ElementSummary, S extends ElementSummary, G, T extends Enum<T>> {

	public static ORCIDHelper<Work, WorkSummary, WorkGroup, WorkType> factoryWorks (ORCIDClient orcidClient) {
		return new ORCIDWorkHelper(null);
	}

	public static ORCIDHelper<Funding, FundingSummary, FundingGroup, FundingType> factoryFundings (ORCIDClient orcidClient) {
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
	
	private static final Logger _log = LoggerFactory.getLogger(ORCIDHelper.class);

	/**
	 * The client used to communicate with ORCID. Defines the ORCID user profile
	 * being managed and the Member API id being user to source works.
	 */
	public final ORCIDClient client;

	protected ExecutorService executor;

	/**
	 * Initializes the helper with a given ORCID client.
	 *
	 * @param orcidClient
	 *            the ORCID client
	 */
	public ORCIDHelper(ORCIDClient orcidClient, int i, int j) {
		this.client = orcidClient;
		this.bulk_size_add = i;
		this.bulk_size_get = j;
		if (client != null && client.threads() > 1) executor = Executors.newFixedThreadPool(client.threads());
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
	public abstract List<S> getAllSummaries() throws OrcidClientException;

	/**
	 * Retrieves the entire set of work summaries in the ORCID profile whose
	 * source is the Member API id defined in the ORCID client.
	 *
	 * @return the set of work summaries in the ORCID profile for the defined
	 *         source
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public abstract List<S> getSourcedSummaries() throws OrcidClientException;

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
	public abstract void getFull(S mergedWork, Map<BigInteger, PTCRISyncResult> cb)
			throws NullPointerException;

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
	public abstract void getFulls(List<S> mergedWorks, Map<BigInteger, PTCRISyncResult> cb, ProgressHandler handler)
			throws OrcidClientException, NullPointerException;

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
	protected abstract PTCRISyncResult add(E work) throws NullPointerException;
	
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
	protected abstract List<PTCRISyncResult> add(Collection<E> works) throws NullPointerException;
	
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
	public final List<PTCRISyncResult> add(List<E> localWorks, ProgressHandler handler) throws NullPointerException {
		List<PTCRISyncResult> res = new ArrayList<PTCRISyncResult>();
		if (handler != null) handler.setCurrentStatus("ORCID_ADDING_WORKS");

		for (int c = 0; c != localWorks.size();) {
			int progress = (int) ((double) c / localWorks.size() * 100);
			if (handler!=null) handler.setProgress(progress);
	
			if (bulk_size_add > 1) {
				List<E> tmp = new ArrayList<E>();
				for (int j = 0; j < bulk_size_add && c < localWorks.size(); j++) {
					tmp.add(localWorks.get(c));
					c++;
				}
				res.addAll(this.add(tmp));
			} 
			else {
				E localWork = localWorks.get(c);
				res.add(this.add(localWork));
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
	public abstract PTCRISyncResult update(BigInteger remotePutcode, E updatedWork)
			throws NullPointerException;

	/**
	 * Deletes the entire set of work summaries in the ORCID profile whose
	 * source is the Member API id defined in the ORCID client.
	 *
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public final void deleteAllSourced() throws OrcidClientException {
		_log.debug("[deleteSourced] " + client.getClientId());

		final List<S> workSummaryList = getSourcedSummaries();
	
		for (S workSummary : workSummaryList) {
			delete(workSummary.getPutCode());
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
	public abstract PTCRISyncResult delete(BigInteger putcode) 
			throws NullPointerException;
	
	/**
	 * Waits for all active asynchronous workers communicating with ORCID to
	 * finish (if multi-threading is enabled, otherwise it is always true).
	 *
	 * @return whether the workers finished before the timeout
	 * @throws InterruptedException
	 *             if the process was interrupted
	 */
	public final boolean waitWorkers() throws InterruptedException {
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
	
	public abstract void setExternalIds(E work, ExternalIds weids);

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
	public final Map<E, ExternalIdsDiff> getSelfExternalIdsDiffS(S work, Collection<E> works) 
			throws NullPointerException {
		if (work == null || works == null)
			throw new NullPointerException("Can't get external ids.");
		
		final Map<E, ExternalIdsDiff> matches = new HashMap<E, ExternalIdsDiff>();
		for (E match : works) {
			final ExternalIdsDiff diff = 
					new ExternalIdsDiff(getSelfExternalIdsE(match), getSelfExternalIdsS(work));
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
	public final Map<E, ExternalIdsDiff> getSelfExternalIdsDiffE(E work, Collection<E> works) 
			throws NullPointerException {
		if (work == null || works == null)
			throw new NullPointerException("Can't get external ids.");
		
		final Map<E, ExternalIdsDiff> matches = new HashMap<E, ExternalIdsDiff>();
		for (E match : works) {
			final ExternalIdsDiff diff = 
					new ExternalIdsDiff(getSelfExternalIdsE(match), getSelfExternalIdsE(work));
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
	public final boolean hasNewSelfIDs(E preWork, S posWork) {
		final ExternalIdsDiff diff = new ExternalIdsDiff(
				getSelfExternalIdsE(preWork),
				getSelfExternalIdsS(posWork));

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
	public final boolean isUpToDateS(E preWork, S posWork) {
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
	public final boolean isUpToDateE(E preWork, E posWork) {
		return isUpToDateS(preWork, summarize(posWork));
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
	private final boolean isSelfEIDsUpToDate(E preWork, S posWork) 
			throws NullPointerException {
		if (preWork == null || posWork == null)
			throw new NullPointerException("Can't test null works.");
		
		return identicalEIDs(getSelfExternalIdsE(preWork),
							 getSelfExternalIdsS(posWork));
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
	protected abstract boolean isMetaUpToDate(E preWork, S posWork) 
			throws NullPointerException;
	
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
	public final Set<String> testMinimalQuality(S work) throws NullPointerException {
		return testMinimalQuality(work,new HashSet<E>());
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
	public abstract Set<String> testMinimalQuality(S work, Collection<E> others) throws NullPointerException;

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
	public final void tryMinimalQualityE(E work, Collection<E> others) throws InvalidWorkException {
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
	public final void tryMinimalQualityS(S work, Collection<E> others) throws InvalidWorkException {
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
	protected abstract boolean validEIdType(String eid);
	
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
	protected abstract S group(G group);

	protected final T getTypeE(E work) {
		return getTypeS(summarize(work));
	}
	
	protected abstract T getTypeS(S work);

	
	/**
	 * Retrieves the title from a work.
	 *
	 * @param work
	 *            the work
	 * @return the work's title if defined, empty string otherwise
	 */
	protected final String getWorkTitleE(E work) {
		return getWorkTitleS(summarize(work));
	}

	/**
	 * Retrieves the title from a work summary.
	 *
	 * @param work
	 *            the work summary
	 * @return the work's title if defined, empty string otherwise
	 */
	protected abstract String getWorkTitleS(S work);
	
	/**
	 * Retrieves the publication year from a work.
	 *
	 * @param work
	 *            the work
	 * @return the publication year if defined, null otherwise
	 */
	protected final String getPubYearE(E work) {
		return getPubYearS(summarize(work));
	}
	
	/**
	 * Retrieves the publication year from a work summary.
	 *
	 * @param work
	 *            the work summary
	 * @return the publication year if defined, null otherwise
	 */
	protected abstract String getPubYearS(S work);

	/**
	 * Returns the non-null external identifiers of a work (null becomes empty
	 * list).
	 * 
	 * @param work
	 *            the work from which to retrieve the external identifiers
	 * @return the non-null external identifiers
	 */
	public abstract ExternalIds getNonNullExternalIdsE (E work);

	/**
	 * Returns the non-null external identifiers of a work summary (null becomes
	 * empty list).
	 * 
	 * @param work
	 *            the work summary from which to retrieve the external
	 *            identifiers
	 * @return the non-null external identifiers
	 */
	public abstract ExternalIds getNonNullExternalIdsS (S work);
	
	/**
	 * Returns the non-null part-of external identifiers of a work (null becomes
	 * empty list).
	 * 
	 * @param work
	 *            the work from which to retrieve the external identifiers
	 * @return the non-null part-of external identifiers
	 */
	public final ExternalIds getPartOfExternalIdsE (E work) {
		return getPartOfExternalIdsS(summarize(work));
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
	public final ExternalIds getPartOfExternalIdsS (S work) {
		List<ExternalId> res = new ArrayList<ExternalId>();
		for (ExternalId eid : getNonNullExternalIdsS(work).getExternalId())
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
	public final ExternalIds getSelfExternalIdsE (E work) {
		return getSelfExternalIdsS(summarize(work));
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
	public final ExternalIds getSelfExternalIdsS (S work) {
		List<ExternalId> res = new ArrayList<ExternalId>();
		for (ExternalId eid : getNonNullExternalIdsS(work).getExternalId())
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
	protected static boolean identicalEIDs(ExternalIds eids1, ExternalIds eids2) {
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
	 * Clones a work summary.
	 * 
	 * @param work
	 *            the summary to be cloned
	 * @return the clone
	 */
	public abstract S cloneS(S work);

	/**
	 * Clones a work.
	 * 
	 * @param work
	 *            the work to be cloned
	 * @return the clone
	 */
	public abstract E cloneE(E work);
	
	protected abstract S summarize(E work);

	public abstract E createUpdate(E original, ExternalIdsDiff diff);
	
}
