package pt.ptcris;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.activities.ActivitiesSummary;
import org.um.dsi.gavea.orcid.model.activities.ActivitiesSummary.Works;
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

import pt.ptcris.workers.ORCIDAddWorker;
import pt.ptcris.workers.ORCIDDelWorker;
import pt.ptcris.workers.ORCIDGetWorker;
import pt.ptcris.workers.ORCIDUpdWorker;

/**
 * An helper to simplify the use of the low-level ORCID
 * {@link pt.ptcris.ORCIDClient client}.
 *
 */
public class ORCIDHelper {

	private boolean threaded = false;

	private static final Logger _log = LogManager.getLogger(ORCIDHelper.class);

	/**
	 * The client used to communicate with ORCID. Defines the ORCID user profile
	 * being managed and the Member API id being user to source works.
	 */
	public final ORCIDClient client;

	private ExecutorService executor = Executors.newFixedThreadPool(100);

	/**
	 * Initializes the helper with a given ORCID client.
	 * 
	 * @param orcidClient
	 *            The ORCID client.
	 * @throws OrcidClientException
	 *             If the communication with ORCID fails.
	 */
	public ORCIDHelper(ORCIDClient orcidClient) throws OrcidClientException {
		this.client = orcidClient;
	}

	/**
	 * Retrieves the entire set of work summaries from the ORCID profile that
	 * have at least an external identifier set. Merges each ORCID group into a
	 * single summary, following {@link #groupToWork}. Groups without external
	 * identifiers are ignored.
	 * 
	 * @return The set of work summaries in the ORCID profile.
	 * @throws OrcidClientException
	 *             If the communication with ORCID fails.
	 * @throws NullPointerException
	 */
	public List<WorkSummary> getAllWorkSummaries() throws OrcidClientException {
		ActivitiesSummary activitiesSummary = client.getActivitiesSummary();
		List<WorkGroup> workGroupList = activitiesSummary.getWorks().getGroup();
		List<WorkSummary> workSummaryList = new LinkedList<WorkSummary>();
		for (WorkGroup group : workGroupList) {
			if (!group.getIdentifiers().getIdentifier().isEmpty())
				workSummaryList.add(groupToWork(group));
		}
		return workSummaryList;
	}

	/**
	 * Retrieves the entire set of work summaries in the ORCID profile whose
	 * source is the Member API id defined in the ORCID client.
	 * 
	 * @return The set of work summaries in the ORCID profile for the defined
	 *         source.
	 * @throws OrcidClientException
	 *             If the communication with ORCID fails.
	 * @throws NullPointerException
	 */
	public List<WorkSummary> getSourcedWorkSummaries() throws OrcidClientException, NullPointerException {

		ActivitiesSummary activitiesSummary = client.getActivitiesSummary();
		String sourceClientID = client.getClientId();
		Works works = activitiesSummary.getWorks();
		List<WorkSummary> returnedWorkSummary = new LinkedList<WorkSummary>();

		if (works == null) {
			return returnedWorkSummary;
		}
		List<WorkGroup> workGroupList = works.getGroup();

		for (WorkGroup workGroup : workGroupList) {
			for (WorkSummary workSummary : workGroup.getWorkSummary()) {
				ClientId workClient = workSummary.getSource().getSourceClientId();
				// may be null is entry added by the user
				if (workClient != null && workClient.getUriPath().equals(sourceClientID)) {
					returnedWorkSummary.add(workSummary);
				}
			}
		}

		return returnedWorkSummary;
	}

	/**
	 * Deletes the entire set of work summaries in the ORCID profile whose
	 * source is the Member API id defined in the ORCID client.
	 * 
	 * @throws OrcidClientException
	 *             If the communication with ORCID fails.
	 */
	public void deleteAllSourcedWorks() throws OrcidClientException {
		List<WorkSummary> workSummaryList = this.getSourcedWorkSummaries();

		for (WorkSummary workSummary : workSummaryList) {
			client.deleteWork(workSummary.getPutCode());
		}
	}

	/**
	 * @see {@link ORCIDClient#deleteWork(BigInteger)}
	 */
	public void deleteWork(BigInteger putCode) throws OrcidClientException {
		_log.debug("[deleteWork] " + putCode);

		if (threaded) {
			ORCIDDelWorker worker = new ORCIDDelWorker(client, putCode, _log);
			executor.execute(worker);
		} else
			client.deleteWork(putCode);

	}

	/**
	 * @see {@link ORCIDClient#getWork(BigInteger)}
	 */
	public void getFullWork(BigInteger putCode, Set<Work> works) throws OrcidClientException {
		_log.debug("[getFullWork] " + putCode);
		if (threaded) {
			ORCIDGetWorker worker = new ORCIDGetWorker(client, works, putCode, _log);
			executor.execute(worker);
		} else
			works.add(client.getWork(putCode));
	}

	/**
	 * @see {@link ORCIDClient#getWork(BigInteger)}
	 */
	public Work getFullWork(BigInteger putCode) throws OrcidClientException {
		_log.debug("[getFullWork] " + putCode);
		return client.getWork(putCode);
	}

	/**
	 * @see {@link ORCIDClient#updateWork(BigInteger, Work)}
	 */
	public void updateWork(BigInteger putCode, Work work) throws OrcidClientException {
		_log.debug("[updateWork] " + putCode);
		work.setPutCode(putCode);

		if (threaded) {
			ORCIDUpdWorker worker = new ORCIDUpdWorker(client, work, _log);
			executor.execute(worker);
		} else
			client.updateWork(work.getPutCode(), work);

	}

	/**
	 * @see {@link ORCIDClient#addWork(Work)}
	 */
	public Work addWork(Work work) throws OrcidClientException {
		_log.debug("[addWork]" + getWorkTitle(work));

		// Remove any putCode if exists
		work.setPutCode(null);

		if (threaded) {
			ORCIDAddWorker worker = new ORCIDAddWorker(client, work, _log);
			executor.execute(worker);
		} else {
			BigInteger putCode = client.addWork(work);
			work.setPutCode(putCode);
			_log.debug("[addWork] " + work.getPutCode());
		}

		return work;
	}

	/**
	 * @see {@link ORCIDClient#getActivitiesSummary()}
	 */
	public ActivitiesSummary getActivitiesSummary() throws OrcidClientException {
		_log.debug("[getActivitiesSummary]");
		return client.getActivitiesSummary();
	}

	/**
	 * Waits for all active works communicating with ORCID to finish (if
	 * multi-threading is enabled).
	 * 
	 * @return Whether the workers finished before the timeout.
	 * @throws InterruptedException
	 */
	public boolean waitWorkers() throws InterruptedException {
		if (!threaded)
			return true;
		executor.shutdown();
		boolean timeout = executor.awaitTermination(100, TimeUnit.SECONDS);
		executor = Executors.newFixedThreadPool(100);
		return timeout;
	}

	/**
	 * Retrieves the entire set of putCodes from an Activities Summary,
	 * independently of the source.
	 * 
	 * @param activitiesSummary
	 *            the summaries from which to collect the put-codes.
	 * @return a list of put-codes in the summaries.
	 */
	public static List<BigInteger> getSummaryPutCodes(ActivitiesSummary activitiesSummary) throws NullPointerException {
		List<BigInteger> pubCodesList = new LinkedList<BigInteger>();
		List<WorkSummary> workSummaryList;
		BigInteger putCode;

		for (WorkGroup workGroup : activitiesSummary.getWorks().getGroup()) {
			workSummaryList = workGroup.getWorkSummary();
			for (WorkSummary workSummary : workSummaryList) {
				putCode = workSummary.getPutCode();
				pubCodesList.add(putCode);
			}
			// putCode = workGroup.getWorkSummary().get(0).getPutCode();
			// pubCodesList.add(putCode);
		}

		return pubCodesList;
	}

	/**
	 * Retrieves the title from a work.
	 * 
	 * @param work
	 *            the work.
	 * @return the work's title.
	 * @throws NullPointerException
	 */
	public static String getWorkTitle(Work work) throws NullPointerException {
		return work.getTitle().getTitle();
	}

	public static String getWorkTitle(WorkSummary work) throws NullPointerException {
		return work.getTitle().getTitle();
	}

	/**
	 * Retrieves the put-code from a work.
	 * 
	 * @param work
	 *            the work.
	 * @return the work's put-code.
	 * @throws NullPointerException
	 */
	public static BigInteger getWorkPutCode(ActivitySummary work) throws NullPointerException {
		return work.getPutCode();
	}

	/**
	 * Selects from a set of works those that share some UIDs (external
	 * identifiers) with a given work summary.
	 * 
	 * @param work
	 *            The work summary to compare with the set of works.
	 * @param works
	 *            The set of works to search for productions with shared UIDs.
	 * @return The set of works with matching UIDs.
	 */
	public static List<Work> getWorksWithSharedUIDs(WorkSummary work, Collection<Work> works) {
		List<Work> matches = new LinkedList<Work>();
		for (Work match : works) {
			if (checkDuplicateUIDs(match.getExternalIdentifiers(), work.getExternalIdentifiers()))
				matches.add(match);
		}
		return matches;
	}

	/**
	 * Tests whether two sets of UIDs (external identifiers) have duplicates.
	 * The algorithm is the same as the one implemented by the ORCID service.
	 * Only considered duplicate if UIDs have the same relationship and are not
	 * "part of".
	 * 
	 * @param uids1
	 *            a set of UIDs.
	 * @param uids2
	 *            another set of UIDs.
	 * @return whether there are duplicate UIDs.
	 */
	private static boolean checkDuplicateUIDs(WorkExternalIdentifiers uids1, WorkExternalIdentifiers uids2) {
		if (uids2 != null && uids1 != null) {
			for (ExternalIdentifier uid2 : uids2.getWorkExternalIdentifier()) {
				for (ExternalIdentifier uid1 : uids1.getWorkExternalIdentifier()) {
					if (sameButNotBothPartOf(uid2.getRelationship(), uid1.getRelationship())
							&& uid1.getExternalIdentifierId().equals(uid2.getExternalIdentifierId())
							&& uid1.getExternalIdentifierType().equals(uid2.getExternalIdentifierType())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Tests whether two UID relationship types are the same but not part of.
	 * 
	 * @param r1
	 *            a UID relationship type.
	 * @param r2
	 *            another UID relationship type.
	 * @return whether UIDs are the same but not part of.
	 */
	private static boolean sameButNotBothPartOf(RelationshipType r1, RelationshipType r2) {
		if (r1 == null && r2 == null)
			return true;
		if (r1 != null && r1.equals(r2) && !r1.equals(RelationshipType.PART_OF))
			return true;
		return false;
	}

	/**
	 * Checks whether a work is already up to date regarding another one, i.e.,
	 * whether a work has the same UIDs as another one.
	 * 
	 * @param existingWork
	 *            The potentially out of date work.
	 * @param workSummary
	 *            The up to date work.
	 * @return true if all the UIDs between the two works are the same, false
	 *         otherwise.
	 */
	public static boolean isAlreadyUpToDate(Work existingWork, WorkSummary workSummary) {
		Set<ExternalIdentifier> uids1 = new HashSet<ExternalIdentifier>(existingWork.getExternalIdentifiers()
				.getWorkExternalIdentifier());
		Set<ExternalIdentifier> uids2 = new HashSet<ExternalIdentifier>(workSummary.getExternalIdentifiers()
				.getWorkExternalIdentifier());
		for (ExternalIdentifier x : uids2) {
			boolean found = false;
			Iterator<ExternalIdentifier> it = uids1.iterator();
			while (it.hasNext() && !found) {
				ExternalIdentifier y = it.next();
				if (x.getExternalIdentifierId().equals(y.getExternalIdentifierId())
						&& x.getExternalIdentifierType().equals(y.getExternalIdentifierType())
						&& x.getRelationship().equals(y.getRelationship()))
					found = true;
			}
			if (!found)
				return false;
		}

		return true;
	}

	// TODO: needed because JAXB does not define equals
	public static boolean equalsUIDs(Set<ExternalIdentifier> uids1, Set<ExternalIdentifier> uids2) {
		if (uids1.size() != uids2.size())
			return false;
		for (ExternalIdentifier x : uids1) {
			boolean found = false;
			Iterator<ExternalIdentifier> it = uids2.iterator();
			while (it.hasNext() && !found) {
				ExternalIdentifier y = it.next();
				if (x.getExternalIdentifierId().equals(y.getExternalIdentifierId())
						&& x.getExternalIdentifierType().equals(y.getExternalIdentifierType())
						&& x.getRelationship().equals(y.getRelationship()))
					found = true;
			}
			if (!found)
				return false;
		}
		return true;
	}

	/**
	 * Merges a work group into a single work summary. Simply selects the
	 * meta-data from the first work of the group (i.e., the preferred one) and
	 * assigns it any extra external identifiers from the remainder works.
	 * 
	 * @param group
	 *            The work group to be merged.
	 * @return The resulting work summary.
	 */
	private static WorkSummary groupToWork(WorkGroup group) {
		WorkSummary aux = group.getWorkSummary().get(0);
		WorkSummary dummy = clone(aux);

		List<ExternalIdentifier> eids = new ArrayList<ExternalIdentifier>();
		for (Identifier id : group.getIdentifiers().getIdentifier()) {
			ExternalIdentifier eid = new ExternalIdentifier();
			eid.setRelationship(RelationshipType.SELF);
			eid.setExternalIdentifierType(ExternalIdentifierType.fromValue(id.getExternalIdentifierType()));
			eid.setExternalIdentifierId(id.getExternalIdentifierId());
			eids.add(eid);
		}
		dummy.setExternalIdentifiers(new WorkExternalIdentifiers(eids));

		return dummy;
	}

	/**
	 * Calculates the difference between two sets of external identifiers.
	 * 
	 * @param base
	 * @param filter
	 * @return
	 */
	public static WorkExternalIdentifiers difference(WorkExternalIdentifiers base, WorkExternalIdentifiers filter) {
		List<ExternalIdentifier> ids = new ArrayList<ExternalIdentifier>(base.getWorkExternalIdentifier());
		for (ExternalIdentifier eid : base.getWorkExternalIdentifier()) {
			for (ExternalIdentifier eid2 : filter.getWorkExternalIdentifier()) {
				if (eid.getExternalIdentifierId().equals(eid2.getExternalIdentifierId())
						&& eid.getExternalIdentifierType().equals(eid2.getExternalIdentifierType())
						&& eid.getRelationship().equals(eid2.getRelationship())) {
					ids.remove(eid);
				}
			}
		}

		return new WorkExternalIdentifiers(ids);
	}

	public static void copy(ActivitySummary from, ActivitySummary to) {
		to.setCreatedDate(from.getCreatedDate());
		to.setDisplayIndex(from.getDisplayIndex());
		to.setLastModifiedDate(from.getLastModifiedDate());
		to.setPath(from.getPath());
		to.setPutCode(from.getPutCode());
		to.setSource(from.getSource());
		to.setVisibility(from.getVisibility());
	}

	public static WorkSummary clone(WorkSummary aux) {
		WorkSummary dummy = new WorkSummary();
		copy(aux, dummy);
		dummy.setPublicationDate(aux.getPublicationDate());
		dummy.setTitle(aux.getTitle());
		dummy.setType(aux.getType());
		dummy.setExternalIdentifiers(aux.getExternalIdentifiers());
		return dummy;
	}

	public static Work clone(Work aux) {
		Work dummy = new Work();
		copy(aux, dummy);
		dummy.setPublicationDate(aux.getPublicationDate());
		dummy.setTitle(aux.getTitle());
		dummy.setType(aux.getType());
		dummy.setExternalIdentifiers(aux.getExternalIdentifiers());
		dummy.setContributors(aux.getContributors());
		return dummy;
	}

}
