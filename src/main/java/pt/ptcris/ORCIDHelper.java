package pt.ptcris;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.activities.ActivitiesSummary;
import org.um.dsi.gavea.orcid.model.activities.ActivitiesSummary.Works;
import org.um.dsi.gavea.orcid.model.activities.Identifier;
import org.um.dsi.gavea.orcid.model.activities.WorkGroup;
import org.um.dsi.gavea.orcid.model.common.ClientId;
import org.um.dsi.gavea.orcid.model.common.RelationshipType;
import org.um.dsi.gavea.orcid.model.work.ExternalIdentifier;
import org.um.dsi.gavea.orcid.model.work.ExternalIdentifierType;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkExternalIdentifiers;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

/**
 * An helper to simplify the use of the low-level ORCID
 * {@link pt.ptcris.ORCIDClient client}.
 *
 */
public class ORCIDHelper {

	/** The client used to communicate with ORCID. */
	public final ORCIDClient client;
	private static final Logger _log = LogManager.getLogger(ORCIDHelper.class);

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
	 * Retrieves the entire set of work summaries from the ORCID profile. Merges
	 * each ORCID group into a single summary, following {@link #groupToWork}.
	 * 
	 * @return The set of work summaries in the ORCID profile
	 * @throws OrcidClientException
	 *             If the communication with ORCID fails.
	 * @throws NullPointerException
	 */
	public List<WorkSummary> getAllWorkSummaries() throws OrcidClientException {
		ActivitiesSummary activitiesSummary = client.getActivitiesSummary();
		List<WorkGroup> workGroupList = activitiesSummary.getWorks().getGroup();
		List<WorkSummary> workSummaryList = new LinkedList<WorkSummary>();
		for (WorkGroup group : workGroupList) {
			workSummaryList.add(groupToWork(group));
		}
		return workSummaryList;
	}

	/**
	 * Retrieves the entire set of work summaries in the ORCID profile whose
	 * source is the Member API id defined in the ORCID client.
	 * 
	 * @return The set of work summaries in the ORCID profile for the set
	 *         source.
	 * @throws OrcidClientException
	 *             If the communication with ORCID fails.
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
	 * Delete all works from a specific source, i.e., from the Member API id
	 * defined in the ORCID client.
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
	 * Retrieves the entire set of putCodes from an Activities Summary,
	 * independently of the source.
	 * 
	 * @param activitiesSummary
	 *            the summaries from which to collect the put-codes.
	 * @return a list of put-codes in the summaries.
	 */
	public static List<BigInteger> getWorkSummaryPutCodes(ActivitiesSummary activitiesSummary)
			throws NullPointerException {
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

	/**
	 * Retrieves the put-code from a work.
	 * 
	 * @param work
	 *            the work.
	 * @return the work's put-code.
	 * @throws NullPointerException
	 */
	public static BigInteger getWorkPutCode(Work work) throws NullPointerException {
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
	 * Merges a work group into a single work. Simply selects the first of the
	 * group and assigns it any extra UIDs from the remainder works.
	 * 
	 * @param group
	 *            The work group to be merged.
	 * @return The resulting work summary.
	 */
	public static WorkSummary groupToWork(WorkGroup group) {
		WorkSummary aux = group.getWorkSummary().get(0);
		WorkSummary dummy = new WorkSummary();
		dummy.setCreatedDate(aux.getCreatedDate());
		dummy.setDisplayIndex(aux.getDisplayIndex());
		dummy.setLastModifiedDate(aux.getLastModifiedDate());
		dummy.setPath(aux.getPath());
		dummy.setPublicationDate(aux.getPublicationDate());
		dummy.setPutCode(aux.getPutCode());
		dummy.setSource(aux.getSource());
		dummy.setTitle(aux.getTitle());
		dummy.setType(aux.getType());
		dummy.setVisibility(aux.getVisibility());

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
	 * Checks whether a work is already up to date regarding another one, i.e.,
	 * whether a work has the same UIDs as another one.
	 * 
	 * @param existingWork
	 *            The potentially out of date work.
	 * @param updatedWork
	 *            The up to date work.
	 * @return true if all the UIDs between the two works are the same, false
	 *         otherwise.
	 */
	public static boolean isAlreadyUpToDate(Work existingWork, Work updatedWork) {
		Set<ExternalIdentifier> uids1 = new HashSet<ExternalIdentifier>(existingWork.getExternalIdentifiers()
				.getWorkExternalIdentifier());
		Set<ExternalIdentifier> uids2 = new HashSet<ExternalIdentifier>(updatedWork.getExternalIdentifiers()
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
	 * @see {@link ORCIDClient#deleteWork(BigInteger)}
	 */
	public void deleteWork(BigInteger putCode) throws OrcidClientException {
		_log.debug("[deleteWork] " + putCode);
		client.deleteWork(putCode);
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
		client.updateWork(putCode, work);
	}

	/**
	 * @see {@link ORCIDClient#addWork(Work)}
	 */
	public Work addWork(Work work) throws OrcidClientException {
		_log.debug("[addWork]" + getWorkTitle(work));

		// Remove any putCode if exists
		work.setPutCode(null);
		BigInteger putCode = client.addWork(work);
		work.setPutCode(putCode);
		_log.debug("[addWork] " + putCode);
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
	 * The low level client being user to communicate with the ORCID API.
	 * 
	 * @return the ORCID client.
	 */
	public ORCIDClient getClient() {
		return this.client;
	}

	public static WorkExternalIdentifiers difference(WorkExternalIdentifiers base,
			WorkExternalIdentifiers filter) {
		List<ExternalIdentifier> ids = new ArrayList<ExternalIdentifier>(
				base.getWorkExternalIdentifier());
		for (ExternalIdentifier eid : base.getWorkExternalIdentifier()) {
			for (ExternalIdentifier eid2 : filter.getWorkExternalIdentifier()) {
				if (eid.getExternalIdentifierId().equals(eid2.getExternalIdentifierId())
						&& eid.getExternalIdentifierType().equals(eid2.getExternalIdentifierType()) &&
						eid.getRelationship().equals(eid2.getRelationship())) {
					ids.remove(eid);
				}
			}
		}

		return new WorkExternalIdentifiers(ids);
	}
}
