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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.activities.ActivitiesSummary;
import org.um.dsi.gavea.orcid.model.activities.Identifier;
import org.um.dsi.gavea.orcid.model.activities.WorkGroup;
import org.um.dsi.gavea.orcid.model.common.ActivitySummary;
import org.um.dsi.gavea.orcid.model.common.ClientId;
import org.um.dsi.gavea.orcid.model.common.RelationshipType;
import org.um.dsi.gavea.orcid.model.work.ExternalIdentifier;
import org.um.dsi.gavea.orcid.model.work.ExternalIdentifierType;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkExternalIdentifiers;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;
import org.um.dsi.gavea.orcid.model.work.WorkType;

import pt.ptcris.ORCIDClient;
import pt.ptcris.exceptions.InvalidWorkException;

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
	public static final String INVALID_WORKEXTERNALIDENTIFIERS = "WorkExternalIdentifiers";
	public static final String INVALID_TITLE = "Title";
	public static final String INVALID_PUBLICATIONDATE = "PublicationDate";
	public static final String INVALID_YEAR = "Year";
	public static final String INVALID_TYPE = "Type";

	/**
	 * Whether to multi-thread the "get" of full works.
	 */
	private final boolean threaded = true;

	private static final Logger _log = LogManager.getLogger(ORCIDHelper.class);

	/**
	 * The client used to communicate with ORCID. Defines the ORCID user profile
	 * being managed and the Member API id being user to source works.
	 */
	public final ORCIDClient client;

	private ExecutorService executor = Executors.newFixedThreadPool(10);

	/**
	 * Initializes the helper with a given ORCID client.
	 *
	 * @param orcidClient
	 *            the ORCID client
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public ORCIDHelper(ORCIDClient orcidClient) {
		this.client = orcidClient;
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
		final ActivitiesSummary activitiesSummary = client.getActivitiesSummary();
		final List<WorkSummary> workSummaryList = new LinkedList<WorkSummary>();
		final List<WorkGroup> workGroupList = getWorkGroups(activitiesSummary);
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
		_log.debug("[getSourcedSummaries]");
		final ActivitiesSummary activitiesSummary = client.getActivitiesSummary();
		final String sourceClientID = client.getClientId();
		final List<WorkSummary> workSummaryList = new LinkedList<WorkSummary>();
		final List<WorkGroup> workGroupList = getWorkGroups(activitiesSummary);
		
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
	 * Retrieves a list of groups from an activities summary, empty if the
	 * objects are null.
	 * 
	 * @param activitiesSummary
	 *            the activities summary
	 * @return the list of work groups
	 */
	private static List<WorkGroup> getWorkGroups(ActivitiesSummary activitiesSummary) {
		if (activitiesSummary != null 
				&& activitiesSummary.getWorks() != null
				&& activitiesSummary.getWorks().getGroup() != null)
			return activitiesSummary.getWorks().getGroup();
		else
			return new LinkedList<WorkGroup>();
	}

	/**
	 * Asynchronously gets a full work from an ORCID profile. The resulting work
	 * contains every external identifier set in the input work summary, because
	 * the summary resulted from the merging of a group, but the retrieve full
	 * work is a single work. It also clears the put-code, since at this level
	 * they represent the local identifier.
	 *
	 * @see {@link ORCIDClient#getWork(BigInteger)}
	 * 
	 * @param mergedWork
	 *            the work summary representing a merged group
	 * @param cb
	 *            the callback object
	 * @throws NullPointerException
	 *             if the merged work is null
	 */
	public void getFullWork(WorkSummary mergedWork, Map<BigInteger, Object> cb)
			throws NullPointerException {
		if (mergedWork == null) throw new NullPointerException("Can't get null work.");
		
		_log.debug("[getFullWork] " + mergedWork.getPutCode());
		if (threaded) {
			final ORCIDGetWorker worker = new ORCIDGetWorker(mergedWork,client, cb, _log);
			executor.execute(worker);
		} else {
			Work fullWork;
			try {
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
	 * @see {@link ORCIDClient#getWork(BigInteger)}
	 * 
	 * @param mergedWork
	 *            the work summary representing a merged group
	 * @throws OrcidClientException
	 *             if communication with the ORCID API fails
	 * @throws NullPointerException
	 *             if the merged work is null
	 */
	public Work getFullWork(WorkSummary mergedWork) 
			throws OrcidClientException, NullPointerException {
		if (mergedWork == null) throw new NullPointerException("Can't get null work.");
		
		_log.debug("[getFullWork] " + mergedWork.getPutCode());
		final Work fullWork = client.getWork(mergedWork.getPutCode());
		finalizeGet(fullWork, mergedWork);

		return fullWork;
	}

	/**
	 * Finalizes a get by updating the meta-data.
	 * 
	 * @see {@link #getFullWork(WorkSummary)}
	 * @param fullWork
	 *            the newly retrieved work
	 * @param sumWork
	 *            the original summary
	 */
	static void finalizeGet(Work fullWork, WorkSummary sumWork) {
		fullWork.setExternalIdentifiers(sumWork.getExternalIdentifiers());
		cleanWorkLocalKey(fullWork);
	}

	/**
	 * Synchronously adds a work to an ORCID profile.
	 *
	 * @see {@link ORCIDClient#addWork(Work)}
	 * 
	 * @param work
	 *            the new work to be added
	 * @return the put-code assigned to the new work by ORCID
	 * @throws OrcidClientException
	 *             if communication with the ORCID API fails
	 * @throws NullPointerException
	 *             if the work is null
	 */
	public BigInteger addWork(Work work) throws OrcidClientException, NullPointerException {
		if (work == null) throw new NullPointerException("Can't add null work.");
		
		_log.debug("[addWork]" + getWorkTitle(work));
	
		// remove any put-code otherwise ORCID will throw an error
		final Work clone = ORCIDHelper.clone(work);
		clone.setPutCode(null);
	
		final BigInteger putcode = client.addWork(clone);
	
		return putcode;
	}
	
	/**
	 * Synchronously updates a work in an ORCID profile.
	 * 
	 * @see {@link ORCIDClient#updateWork(BigInteger, Work)}
	 * 
	 * @param remotePutcode
	 *            the put-code of the remote ORCID work that will be updated
	 * @param updatedWork
	 *            the new state of the work that will be updated
	 * @throws OrcidClientException
	 *             if communication with the ORCID API fails
	 * @throws NullPointerException
	 *             if either parameter is null
	 */
	public void updateWork(BigInteger remotePutcode, Work updatedWork)
			throws OrcidClientException, NullPointerException {
		if (remotePutcode == null || updatedWork == null) 
			throw new NullPointerException("Can't update null work.");

		_log.debug("[updateWork] " + remotePutcode);

		final Work clone = ORCIDHelper.clone(updatedWork);
		// set the remote put-code
		clone.setPutCode(remotePutcode);

		client.updateWork(remotePutcode, clone);
	}

	/**
	 * Deletes the entire set of work summaries in the ORCID profile whose
	 * source is the Member API id defined in the ORCID client.
	 *
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	public void deleteAllSourcedWorks() throws OrcidClientException {
		_log.debug("[deleteSourced]");

		final List<WorkSummary> workSummaryList = getSourcedWorkSummaries();
	
		for (WorkSummary workSummary : workSummaryList) {
			deleteWork(workSummary.getPutCode());
		}
	}

	/**
	 * Synchronously deletes a work in an ORCID profile.
	 * 
	 * @see {@link ORCIDClient#deleteWork(BigInteger)}
	 * 
	 * @param putcode the remote put-code of the work to be deleted
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 * @throws NullPointerException
	 *             if the put-code is null
	 */
	public void deleteWork(BigInteger putcode) 
			throws OrcidClientException, NullPointerException {
		if (putcode == null) 
			throw new NullPointerException("Can't delete null work.");

		_log.debug("[deleteWork] " + putcode);
	
		client.deleteWork(putcode);
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
		if (!threaded) return true;
		
		executor.shutdown();
		final boolean timeout = executor.awaitTermination(100, TimeUnit.SECONDS);
		executor = Executors.newFixedThreadPool(100);
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
	public static BigInteger getActivityLocalKey(ActivitySummary act) throws NullPointerException {
		if (act == null)
			throw new NullPointerException("Can't get local key.");

		return act.getPutCode();
	}

	/**
	 * Retrieves the local key of an activity, currently assumed to be stored in
	 * the put-code field.
	 *
	 * @param act
	 *            the activity to which to set the local key
	 * @throws NullPointerException
	 *             if the activity is null
	 */
	public static void setWorkLocalKey(ActivitySummary act, BigInteger key) throws NullPointerException {
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
	private static void cleanWorkLocalKey(ActivitySummary act) throws NullPointerException {
		if (act == null)
			throw new NullPointerException("Can't clear local key.");

		act.setPutCode(null);
	}

	/**
	 * Calculates the symmetric difference of {@link ExternalIdentifier external
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
	public static Map<Work, ExternalIdsDiff> getExternalIdsDiff(WorkSummary work, Collection<Work> works) 
			throws NullPointerException {
		if (work == null || works == null)
			throw new NullPointerException("Can't get external ids.");
		
		final Map<Work, ExternalIdsDiff> matches = new HashMap<Work, ExternalIdsDiff>();
		for (Work match : works) {
			final ExternalIdsDiff diff = 
					new ExternalIdsDiff(match.getExternalIdentifiers(), work.getExternalIdentifiers());
			if (!diff.same.isEmpty())
				matches.put(match, diff);
		}
		return matches;
	}

	/**
	 * Checks whether a work is already up to date regarding another one, i.e.,
	 * whether a work has the same {@link ExternalIdentifier external
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
				preWork.getExternalIdentifiers(),
				posWork.getExternalIdentifiers());

		return diff.more.isEmpty();
	}

	/**
	 * Checks whether a work is already up to date regarding another one,
	 * considering the {@link ExternalIdentifier external identifiers} and
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
	 * considering the {@link ExternalIdentifier external identifiers} and
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
				preWork.getExternalIdentifiers(),
				posWork.getExternalIdentifiers());
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
				preWork.getExternalIdentifiers(),
				posWork.getExternalIdentifiers());
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
	 * Tests whether a work has minimal quality to be synchronized, by
	 * inspecting its meta-data, returns the detected invalid fields.
	 * 
	 * The considered fields are: external identifiers, title, publication date
	 * (year), work type. All these meta-data is available in work summaries.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 * 
	 * @param work
	 *            the work to test for quality
	 * @return the set of invalid fields
	 * @throws NullPointerException
	 *             if the work is null
	 */
	public static Set<String> testMinimalQuality(Work work) throws NullPointerException {
		if (work == null)
			throw new NullPointerException("Can't test null work.");
		
		final Set<String> res = new HashSet<String>();
		if (work.getExternalIdentifiers() == null)
			res.add(INVALID_EXTERNALIDENTIFIERS);
		else if (work.getExternalIdentifiers().getWorkExternalIdentifier() == null
				|| work.getExternalIdentifiers().getWorkExternalIdentifier().isEmpty())
			res.add(INVALID_WORKEXTERNALIDENTIFIERS);
		if (work.getTitle() == null)
			res.add(INVALID_TITLE);
		else if (work.getTitle().getTitle() == null)
			res.add(INVALID_TITLE);
		if (work.getType() == null)
			res.add(INVALID_TYPE);
		if (work.getType() == null
				|| (work.getType() != WorkType.DATA_SET && work.getType() != WorkType.RESEARCH_TECHNIQUE)) {
			if (work.getPublicationDate() == null)
				res.add(INVALID_PUBLICATIONDATE);
			else if (work.getPublicationDate().getYear() == null)
				res.add(INVALID_YEAR);
		}
		return res;
	}
	
	/**
	 * Tests whether a work has minimal quality to be synchronized, by
	 * inspecting its meta-data, returns the detected invalid fields.
	 * 
	 * The considered fields are: external identifiers, title, publication date
	 * (year), work type. All these meta-data is available in work summaries.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 * 
	 * @param work
	 *            the work to test for quality
	 * @return the set of invalid fields
	 * @throws NullPointerException
	 *             if the work is null
	 */
	public static Set<String> testMinimalQuality(WorkSummary work) throws NullPointerException {
		if (work == null)
			throw new NullPointerException("Can't test null work.");
	
		final Set<String> res = new HashSet<String>();
		if (work.getExternalIdentifiers() == null)
			res.add(INVALID_EXTERNALIDENTIFIERS);
		else if (work.getExternalIdentifiers().getWorkExternalIdentifier() == null
				|| work.getExternalIdentifiers().getWorkExternalIdentifier().isEmpty())
			res.add(INVALID_WORKEXTERNALIDENTIFIERS);
		if (work.getTitle() == null)
			res.add(INVALID_TITLE);
		else if (work.getTitle().getTitle() == null)
			res.add(INVALID_TITLE);
		if (work.getType() == null)
			res.add(INVALID_TYPE);
		if (work.getType() == null
				|| (work.getType() != WorkType.DATA_SET && work.getType() != WorkType.RESEARCH_TECHNIQUE)) {
			if (work.getPublicationDate() == null)
				res.add(INVALID_PUBLICATIONDATE);
			else if (work.getPublicationDate().getYear() == null)
				res.add(INVALID_YEAR);
		}
		return res;
	}

	/**
	 * Tests whether a work has minimal quality to be synchronized, by
	 * inspecting its meta-data. Throws an exception if the test fails.
	 * 
	 * The considered fields are: external identifiers, title, publication date
	 * (year), work type. All these meta-data is available in work summaries.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 * 
	 * @param work
	 *            the work to test for quality
	 * @throws InvalidWorkException
	 *             if the quality test fails, containing the reasons for failing
	 * @throws NullPointerException
	 *             if the work is null
	 */
	public static void tryMinimalQuality(Work work) throws InvalidWorkException {
		Set<String> invs = testMinimalQuality(work);
		if (!invs.isEmpty()) {
			throw new InvalidWorkException(invs);
		}
	}

	/**
	 * Tests whether a work has minimal quality to be synchronized, by
	 * inspecting its meta-data. Throws an exception if the test fails.
	 * 
	 * The considered fields are: external identifiers, title, publication date
	 * (year), work type. All these meta-data is available in work summaries.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 * 
	 * @param work
	 *            the work to test for quality
	 * @throws InvalidWorkException
	 *             if the quality test fails, containing the reasons for failing
	 * @throws NullPointerException
	 *             if the work is null
	 */
	public static void tryMinimalQuality(WorkSummary work) throws InvalidWorkException {
		Set<String> invs = testMinimalQuality(work);
		if (!invs.isEmpty()) {
			throw new InvalidWorkException(invs);
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

		final List<ExternalIdentifier> eids = new ArrayList<ExternalIdentifier>();
		for (Identifier id : group.getIdentifiers().getIdentifier()) {
			final ExternalIdentifier eid = new ExternalIdentifier();
			eid.setRelationship(RelationshipType.SELF);
			eid.setExternalIdentifierType(ExternalIdentifierType.fromValue(id
					.getExternalIdentifierType().toLowerCase()));
			eid.setExternalIdentifierId(id.getExternalIdentifierId());
			eids.add(eid);
		}
		dummy.setExternalIdentifiers(new WorkExternalIdentifiers(eids));

		return dummy;
	}

	/**
	 * Retrieves the title from a work.
	 *
	 * @param work
	 *            the work
	 * @return the work's title if defined, empty string otherwise
	 */
	private static String getWorkTitle(Work work) {
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
	 * Copies all meta-data from an activity summary into another.
	 * 
	 * @param from
	 *            the source summary
	 * @param to
	 *            the target summary
	 * @throws NullPointerException if either argument is null
	 */
	private static void copy(ActivitySummary from, ActivitySummary to) 
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
		dummy.setExternalIdentifiers(work.getExternalIdentifiers());
		return dummy;
	}

	/**
	 * Clones a work summary.
	 * 
	 * @param work
	 *            the summary to be cloned
	 * @return the clone
	 */
	public static Work clone(Work work) {
		if (work == null) return null;
		
		final Work dummy = new Work();
		copy(work, dummy);
		dummy.setPublicationDate(work.getPublicationDate());
		dummy.setTitle(work.getTitle());
		dummy.setType(work.getType());
		dummy.setExternalIdentifiers(work.getExternalIdentifiers());
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
