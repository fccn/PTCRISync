package pt.ptcris;

import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.orcid.jaxb.model.record.summary_rc2.ActivitiesSummary;
import org.orcid.jaxb.model.record.summary_rc2.WorkGroup;
import org.orcid.jaxb.model.record.summary_rc2.WorkSummary;
import org.orcid.jaxb.model.record_rc2.ExternalID;
import org.orcid.jaxb.model.record_rc2.ExternalIDs;
import org.orcid.jaxb.model.record_rc2.Relationship;
import org.orcid.jaxb.model.record_rc2.Work;

public class ORCIDHelper {

	public final ORCIDClient client;

	public ORCIDHelper(String baseUri, String profile, String accessToken) throws URISyntaxException {
		client = new ORCIDClientImpl(baseUri, profile, accessToken);
	}

	/**
	 * Retrieves the entire set of work summaries in the ORCID profile. Merges
	 * each ORCID group into a single summary, following {@link #groupToWork}.
	 * 
	 * @return The set of work summaries in the ORCID profile
	 * @throws ORCIDException
	 */
	public List<WorkSummary> getAllWorkSummaries() throws ORCIDException {
		ActivitiesSummary summs = client.getActivitiesSummary();
		Stream<WorkGroup> groups = summs.getWorks().getWorkGroup().stream();
		Stream<WorkSummary> works = groups.map(w -> groupToWork(w));
		return works.collect(Collectors.toList());
	}

	/**
	 * Retrieves the entire set of works in the ORCID profile whose source is
	 * the local CRIS service.
	 * 
	 * @param sourceName
	 *            The source name of the local CRIS service.
	 * @return The set of work summaries in the ORCID profile whose source is
	 *         useDefault.
	 * @throws ORCIDException
	 */
	public List<WorkSummary> getSourcedWorkSummaries(String sourceName) throws ORCIDException {
		ActivitiesSummary summs = client.getActivitiesSummary();
		Stream<WorkGroup> groups = summs.getWorks().getWorkGroup().stream();
		Stream<WorkSummary> work_summs = groups.map(WorkGroup::getWorkSummary)
											   .flatMap(List::stream)
											   .filter(s -> s.getSource().getSourceName().getContent().equals(sourceName));
		return work_summs.collect(Collectors.toList());
	}

	/**
	 * Retrieves the set of productions (from works) that share some UIDs with a
	 * work summary.
	 * 
	 * @param summary
	 *            The work summary to compare with the list of works.
	 * @param works
	 *            The set of works to search for productions with shared UIDs.
	 * @return The set of works with matching UIDs.
	 */
	public static List<Work> getWorksWithSharedUIDs(WorkSummary summary, List<Work> works) {
		List<Work> matches = new LinkedList<Work>();
		for (Work match : works) {
			if (checkDuplicateUIDs(match.getExternalIdentifiers(), summary.getExternalIdentifiers()))
				matches.add(match);
		}
		return matches;
	}

	/**
	 * Tests whether two sets of external IDs have duplicates. The algorithm is
	 * the same as the one implemented by ORCID. Only considered duplicate if
	 * UIDs have the same relationship and are not "part of".
	 * 
	 * @param uids1
	 * @param uids2
	 * @return
	 */
	private static boolean checkDuplicateUIDs(ExternalIDs uids1, ExternalIDs uids2) {
		if (uids2 != null && uids1 != null) {
			for (ExternalID uid2 : uids2.getExternalIdentifier()) {
				for (ExternalID uid1 : uids1.getExternalIdentifier()) {
					if (sameButNotBothPartOf(uid2.getRelationship(), uid1.getRelationship()) && uid1.equals(uid2)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Tests whether two UIDs relationships are the same but not part of.
	 * 
	 * @param r1
	 * @param r2
	 * @return
	 */
	private static boolean sameButNotBothPartOf(Relationship r1, Relationship r2) {
		if (r1 == null && r2 == null)
			return true;
		if (r1 != null && r1.equals(r2) && !r1.equals(Relationship.PART_OF))
			return true;
		return false;
	}

	/**
	 * Merges a group into a work. Simply selects the first of the group and
	 * assigns it any extra UIDs.
	 * 
	 * @param group
	 *            The group to be merged.
	 * @return The resulting work summary.
	 */
	public static WorkSummary groupToWork(WorkGroup group) {
		WorkSummary aux = group.getWorkSummary().get(0);
		WorkSummary dummy = new WorkSummary();
		dummy.setCreatedDate(aux.getCreatedDate());
		dummy.setDisplayIndex(aux.getDisplayIndex());
		dummy.setExternalIdentifiers(aux.getExternalIdentifiers());
		dummy.setLastModifiedDate(aux.getLastModifiedDate());
		dummy.setPath(aux.getPath());
		dummy.setPublicationDate(aux.getPublicationDate());
		dummy.setPutCode(aux.getPutCode());
		dummy.setSource(aux.getSource());
		dummy.setTitle(aux.getTitle());
		dummy.setType(aux.getType());
		dummy.setVisibility(aux.getVisibility());
		// TODO: add the other UIDs of the group
		return dummy;
	}

	/**
	 * Checks if localWork is already up to date on the information from
	 * remoteWork, i.e., localWork already has the same UIDs as remoteWork
	 * 
	 * @param localWork
	 *            The local work to check if it is up to date
	 * @param remoteWork
	 *            The remote work to use when checking if the local work is up
	 *            to date
	 * @return true if all the UIDs between the two works are the same, false
	 *         otherwise
	 */
	public static boolean isAlreadyUpToDate(Work localWork, Work remoteWork) {
		// TODO Compare the two records to check if they are equal (when it
		// comes to matching UIDs)
		return false;
	}

	public void deleteWork(Long putCode) throws ORCIDException {
		client.deleteWork(putCode);
	}

	public Work getFullWork(Long putCode) throws ORCIDException {
		return client.getWork(putCode);
	}

	public void updateWork(Long putCode, Work work) throws ORCIDException {
		client.updateWork(putCode, work);
	}

	public void addWork(Work work) throws ORCIDException {
		client.addWork(work);
	}

}
