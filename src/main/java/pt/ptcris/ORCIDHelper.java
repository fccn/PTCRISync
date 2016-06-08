package pt.ptcris;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;
import org.um.dsi.gavea.orcid.model.activities.ActivitiesSummary;
import org.um.dsi.gavea.orcid.model.activities.ActivitiesSummary.Works;
import org.um.dsi.gavea.orcid.model.activities.WorkGroup;
import org.um.dsi.gavea.orcid.model.common.RelationshipType;
import org.um.dsi.gavea.orcid.model.work.WorkExternalIdentifiers;
import org.um.dsi.gavea.orcid.model.work.ExternalIdentifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;

public class ORCIDHelper {

	public final ORCIDClient client;
	private static final Logger _log = LogManager.getLogger(ORCIDHelper.class);

	public ORCIDHelper(ORCIDClient orcidClient) throws OrcidClientException {
		this.client = orcidClient;
	}

	/**
	 * Retrieves the entire set of work summaries in the ORCID profile. Merges
	 * each ORCID group into a single summary, following {@link #groupToWork}.
	 * 
	 * @return The set of work summaries in the ORCID profile
	 * @throws OrcidClientException
	 */
	public List<WorkSummary> getAllWorkSummaries() throws OrcidClientException, NullPointerException {
		ActivitiesSummary activitiesSummary = client.getActivitiesSummary();
		Stream<WorkGroup> workGroupList = activitiesSummary.getWorks().getGroup().stream();
		Stream<WorkSummary> workSummaryList = workGroupList.map(w -> groupToWork(w));
		return workSummaryList.collect(Collectors.toList());
	}

	/**
	 * Retrieves the entire set of works in the ORCID profile whose source is
	 * the local CRIS service.
	 * 
	 * @return The set of work summaries in the ORCID profile whose source is
	 *         useDefault.
	 * @throws OrcidClientException
	 */
	public List<WorkSummary> getSourcedWorkSummaries() throws OrcidClientException, NullPointerException {

		ActivitiesSummary activitiesSummary = client.getActivitiesSummary();
		String sourceClientID = client.getClientId();	
		Works works = activitiesSummary.getWorks();
		if (works == null) {
			return new LinkedList<WorkSummary>();
		} 
		Stream<WorkGroup> workGroupList = works.getGroup().stream();

		Stream<WorkSummary> workSummaryList = workGroupList.map(WorkGroup::getWorkSummary)
				.flatMap(List::stream)
				.filter(s -> s.getSource().getSourceOrcid().getUriPath().equals(sourceClientID));

		return workSummaryList.collect(Collectors.toList());
	}
		
	
	/**
	 * Delete all works from a specific Source
	 * @throws OrcidClientException
	 */
	public void deleteAllSourcedWorks () throws OrcidClientException {
		List<WorkSummary> workSummaryList = this.getSourcedWorkSummaries();
		
		for (WorkSummary workSummary : workSummaryList) {			
			client.deleteWork(workSummary.getPutCode());
		}

	}


	/**
	 * Retrieves the entire set of putCodes from an Activities Summary it's source independent
	 * 
	 * @return a list of putCodes
	 */
	public static List<BigInteger> getWorkSummaryPutCodes (ActivitiesSummary activitiesSummary) throws NullPointerException {		
		List<BigInteger> pubCodesList = new LinkedList<BigInteger>();		
		List<WorkSummary> workSummaryList;
		BigInteger putCode;

		for (WorkGroup workGroup : activitiesSummary.getWorks().getGroup()) {
			workSummaryList = workGroup.getWorkSummary();
			for (WorkSummary workSummary : workSummaryList) {			 
				putCode = workSummary.getPutCode();
				pubCodesList.add(putCode);			 
			}
			//putCode =  workGroup.getWorkSummary().get(0).getPutCode();
			//pubCodesList.add(putCode);

		}		

		return pubCodesList;
	}		
	

	/**
	 * @param work
	 * @return String with title
	 * @throws NullPointerException
	 */
	public static String getWorkTitle(Work work) throws NullPointerException {
		return work.getTitle().getTitle();
	}
	

	/**
	 * @param work
	 * @return BigInteger with putcode
	 * @throws NullPointerException
	 */
	public static BigInteger getWorkPutCode(Work work) throws NullPointerException {
		return work.getPutCode();
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
	private static boolean checkDuplicateUIDs(WorkExternalIdentifiers uids1, WorkExternalIdentifiers uids2) {
		if (uids2 != null && uids1 != null) {
			
			for (ExternalIdentifier uid2 : uids2.getWorkExternalIdentifier()) {
				for (ExternalIdentifier uid1 : uids1.getWorkExternalIdentifier()) {
					
					if (sameButNotBothPartOf(uid2.getRelationship(), uid1.getRelationship()) && 
							uid1.getExternalIdentifierId().equals(uid2.getExternalIdentifierId()) &&
							uid1.getExternalIdentifierType().equals(uid2.getExternalIdentifierType())) {
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
	private static boolean sameButNotBothPartOf(RelationshipType r1, RelationshipType r2) {
		if (r1 == null && r2 == null)
			return true;
		if (r1 != null && r1.equals(r2) && !r1.equals(RelationshipType.PART_OF))
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

	public void deleteWork(BigInteger putCode) throws OrcidClientException {
		_log.debug("[deleteWork] " + putCode);
		client.deleteWork(putCode);
	}

	public Work getFullWork(BigInteger putCode) throws OrcidClientException {
		_log.debug("[getFullWork] " + putCode);
		return client.getWork(putCode);
	}

	public void updateWork(BigInteger putCode, Work work) throws OrcidClientException {
		_log.debug("[updateWork] " + putCode);
		client.updateWork(putCode, work);
	}

	public Work addWork(Work work) throws OrcidClientException {
		_log.debug("[addWork]" + getWorkTitle(work));
		
		//Remove any putCode if exists
		work.setPutCode(null);		
		BigInteger putCode = new BigInteger(client.addWork(work));  
		work.setPutCode(putCode);
		_log.debug("[addWork] " + putCode);		
		return work;
	}

	public ActivitiesSummary getActivitiesSummary() throws OrcidClientException {
		_log.debug("[getActivitiesSummary]");
		return client.getActivitiesSummary();
	}

	public ORCIDClient getClient () {
		return this.client;
	}
	
}
