package pt.ptcris;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.activities.WorkGroup;
import org.um.dsi.gavea.orcid.model.work.ExternalIdentifier;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkExternalIdentifiers;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

import pt.ptcris.handlers.ProgressHandler;
import pt.ptcris.utils.UpdateRecord;
import pt.ptcris.ORCIDHelper;

/**
 * <p>
 * An implementation of the PTCRISync synchronization service based on the
 * version 4.2 of the specification. This service allows CRIS services to keep
 * their repositories synchronized with ORCID. This requires the CRIS service to
 * have access to the ORCID Member API.
 * </p>
 * 
 * <p>
 * The service has two main functionalities: to keep a set of local productions
 * updated in an ORCID user profile through an
 * {@link #export(ORCIDClient, List, ProgressHandler) export} procedure, and
 * import productions not known locally from the ORCID profile, either through
 * the {@link #importWorks(ORCIDClient, List, ProgressHandler) import} of
 * complete new productions or through the
 * {@link #importUpdates(ORCIDClient, List, ProgressHandler) import} of new
 * information for already known productions.
 * </p>
 * 
 * <p>
 * The implementation of the service assumes that its user communicates the
 * local productions following the ORCID schema, in particular encoding them as
 * ORCID {@link Work works}. This uniforms the API and simplifies the
 * synchronization process.
 * </p>
 * 
 * <p>
 * The communication with ORCID is encapsulated in an ORCID {@link ORCIDClient
 * client} that contains information regarding the CRIS Member API and the ORCID
 * profile that is to be managed. Most of the communication is however handled
 * by an {@link ORCIDHelper helper}.
 * </p>
 * 
 * @see <a
 *      href="https://ptcris.pt/hub-ptcris/">https://ptcris.pt/hub-ptcris/</a>
 *
 */
public class PTCRISync {

	private static final int UPTODATE = -10;

	/**
	 * <p>
	 * Exports a list of local CRIS productions to an ORCID profile. This
	 * procedure essentially manages the works in the ORCID profile that are
	 * sourced by the CRIS, both set in the client.
	 * </p>
	 * 
	 * <p>
	 * The procedure detects every CRIS sourced work in the ORCID profile that
	 * matches any local work that is being exported; if there is no matching
	 * local work, the ORCID work is deleted from the profile. Otherwise it will
	 * be updated with the meta-data of one of the matching local works.
	 * Finally, for local works without any matching ORCID work new ORCID works
	 * are created. The matching is performed by detecting shared
	 * {@link ExternalIdentifier external identifiers} (see
	 * {@link ORCIDHelper#getExternalIdentifiersDiff(WorkSummary, Collection)}).
	 * </p>
	 * 
	 * <p>
	 * The update stage must be two-phased in order to avoid potential
	 * conflicts: the first phase removes external identifiers that are obsolete
	 * from the CRIS sourced works, so that there are no conflicts with the new
	 * ones inserted in the second phase.
	 * </p>
	 * 
	 * <p>
	 * The procedure expects the CRIS service to provide the local works in the
	 * ORCID schema, in particular encoding productions as {@link Work works}.
	 * Thus, the meta-data of the CRIS sourced works in the ORCID profile is
	 * exactly that of the provided local productions that are to be exported.
	 * The put-code of these local productions can however be used as local key
	 * identifiers, since these are disregarded during the update of the ORCID
	 * profile (new works are assigned fresh put-codes and updated works use the
	 * put-code of the existing ORCID work).
	 * </p>
	 * 
	 * <p>
	 * Note that there is a difference from the PTCRISync specification 4.1 to
	 * force ORCID works to be updated only once (by a single matching local
	 * work).
	 * </p>
	 * 
	 * TODO: ORCID conflict errors 409 must be handled if the user submits works
	 * to be exported with overlapping external identifiers.
	 * 
	 * @param orcidClient
	 *            The ORCID client defining the CRIS Member API and the profile
	 *            to be managed.
	 * @param localWorks
	 *            The list of local productions to be exported (those marked as
	 *            synced).
	 * @param progressHandler
	 *            The progress handler responsible for receiving progress
	 *            updates.
	 * @throws OrcidClientException
	 *             If the communication with ORCID fails.
	 * @throws InterruptedException
	 * @throws NullPointerException
	 */
	public static Map<BigInteger, Integer> export(ORCIDClient orcidClient, List<Work> localWorks,
			ProgressHandler progressHandler) throws InterruptedException, NullPointerException, OrcidClientException {
		return exportBase(orcidClient, localWorks, progressHandler, false);
	}

	public static Map<BigInteger, Integer> exportForce(ORCIDClient orcidClient, List<Work> localWorks,
			ProgressHandler progressHandler) throws InterruptedException, NullPointerException, OrcidClientException {
		return exportBase(orcidClient, localWorks, progressHandler, true);
	}
	
	private static Map<BigInteger, Integer> exportBase(ORCIDClient orcidClient, List<Work> localWorks,
			ProgressHandler progressHandler, boolean force) throws InterruptedException, NullPointerException,
			OrcidClientException {

		Map<BigInteger, Integer> result = new HashMap<BigInteger, Integer>();

		int progress = 0;
		progressHandler.setProgress(progress);
		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_STARTED");

		ORCIDHelper helper = new ORCIDHelper(orcidClient);
		List<WorkSummary> orcidWorks = helper.getSourcedWorkSummaries();

		List<UpdateRecord> recordsToUpdate = new LinkedList<UpdateRecord>();

		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_WORKS_ITERATION");
		for (int counter = 0; counter != orcidWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / orcidWorks.size()) * 100);
			progressHandler.setProgress(progress);

			Map<Work, ExternalIdentifiersUpdate> matchingWorks = ORCIDHelper.getExternalIdentifiersDiff(
					orcidWorks.get(counter), localWorks);
			if (matchingWorks.isEmpty()) {
				try {
					helper.deleteWork(orcidWorks.get(counter).getPutCode());
				} catch (OrcidClientException e) {
					// TODO: what to do?
				}
			} else {
				Work localWork = matchingWorks.keySet().iterator().next();
				if (!ORCIDHelper.isUpToDate(localWork, orcidWorks.get(counter)) || force)
					recordsToUpdate.add(new UpdateRecord(localWork, orcidWorks.get(counter), matchingWorks
							.get(localWork)));
				else
					result.put(ORCIDHelper.getWorkLocalKey(localWork), UPTODATE);
				localWorks.remove(localWork);
			}
		}

		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_UPDATING_WORKS");
		for (int counter = 0; counter != recordsToUpdate.size(); counter++) {
			progress = (int) ((double) ((double) counter / recordsToUpdate.size()) * 100);
			progressHandler.setProgress(progress);

			if (!recordsToUpdate.get(counter).getMatches().more.isEmpty()) {
				Work localWork = recordsToUpdate.get(counter).getLocalWork();
				WorkExternalIdentifiers weids = new WorkExternalIdentifiers();
				List<ExternalIdentifier> ids = new ArrayList<ExternalIdentifier>(recordsToUpdate.get(counter)
						.getMatches().same);
				weids.setWorkExternalIdentifier(ids);
				localWork.setExternalIdentifiers(weids);
				try {
					helper.updateWork(recordsToUpdate.get(counter).getRemoteWork().getPutCode(), localWork);
					result.put(ORCIDHelper.getWorkLocalKey(localWork), 200);
				} catch (OrcidClientException e) {
					result.put(ORCIDHelper.getWorkLocalKey(localWork), e.getCode());
					// TODO: what else to do?
				}
			}
		}

		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_UPDATING_WORKS");
		for (int counter = 0; counter != recordsToUpdate.size(); counter++) {
			progress = (int) ((double) ((double) counter / recordsToUpdate.size()) * 100);
			progressHandler.setProgress(progress);

			if (!recordsToUpdate.get(counter).getMatches().less.isEmpty()
					|| recordsToUpdate.get(counter).getMatches().more.isEmpty()) { // it
																					// is
																					// only
																					// in
																					// the
																					// list
																					// of
																					// not
																					// up-to-date
				Work localWork = recordsToUpdate.get(counter).getLocalWork();
				WorkExternalIdentifiers weids = new WorkExternalIdentifiers();
				List<ExternalIdentifier> ids = new ArrayList<ExternalIdentifier>(recordsToUpdate.get(counter)
						.getMatches().same);
				ids.addAll(recordsToUpdate.get(counter).getMatches().less);
				weids.setWorkExternalIdentifier(ids);
				localWork.setExternalIdentifiers(weids);
				try {
					helper.updateWork(recordsToUpdate.get(counter).getRemoteWork().getPutCode(), localWork);
					result.put(localWork.getPutCode(), 200);
				} catch (OrcidClientException e) {
					result.put(localWork.getPutCode(), e.getCode());
					// TODO: what else to do?
				}
			}
		}

		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_ADDING_WORKS");
		for (int counter = 0; counter != localWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / localWorks.size()) * 100);
			progressHandler.setProgress(progress);

			Work localWork = localWorks.get(counter);

			try {
				BigInteger remotePutcode = helper.addWork(localWork);
				result.put(localWork.getPutCode(), 200);
			} catch (OrcidClientException e) {
				result.put(localWork.getPutCode(), e.getCode());
				// TODO: what else to do?
			}
		}

		progressHandler.done();

		return result;
	}

	/**
	 * <p>
	 * Discovers new works in an ORCID profile given a set of known local CRIS
	 * productions. Creates creation notifications for each work group at ORCID
	 * (merged into as a single work by the {@link ORCIDHelper helper}) without
	 * matching local productions (i.e., those without shared
	 * {@link ExternalIdentifier external identifiers}).
	 * </p>
	 * 
	 * <p>
	 * Currently, these creation notifications simply take the shape of ORCID
	 * works themselves (representing a matching work group). The group merging
	 * selects the meta-data of the preferred work and the external identifiers
	 * of the whole group (see {@link ORCIDHelper#groupToWork(WorkGroup)}). The
	 * selection of the meta-data from a group could be changed without
	 * affecting the correction of the procedure.
	 * </p>
	 * 
	 * <p>
	 * Since the put-code attribute is used as a local key of each work, it
	 * should be null for these creation notifications (and not the put-code of
	 * the ORCID works that gave origin to it). Since only the external
	 * identifiers of the local productions are used to search for matches, the
	 * remainder meta-data can be currently left null.
	 * </p>
	 * 
	 * @param orcidClient
	 *            The ORCID client defining the CRIS Member API and the profile
	 *            to be managed.
	 * @param localWorks
	 *            The full list of productions in the local profile.
	 * @param progressHandler
	 *            The progress handler responsible for receiving progress
	 *            updates.
	 * @return The list of new works found in the profile.
	 * @throws OrcidClientException
	 *             If the communication with ORCID fails.
	 * @throws InterruptedException
	 */
	public static List<Work> importWorks(ORCIDClient orcidClient, List<Work> localWorks, ProgressHandler progressHandler)
			throws OrcidClientException {
		int progress = 0;
		progressHandler.setProgress(progress);
		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_STARTED");

		List<Work> worksToImport = new LinkedList<Work>();

		ORCIDHelper helper = new ORCIDHelper(orcidClient);

		List<WorkSummary> orcidWorks = helper.getAllWorkSummaries();

		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_ITERATION");
		Set<Work> fullWorks = new HashSet<Work>();
		Map<BigInteger, Integer> putcodes = new HashMap<BigInteger, Integer>();
		for (int counter = 0; counter != orcidWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / orcidWorks.size()) * 100);
			progressHandler.setProgress(progress);

			Map<Work, ExternalIdentifiersUpdate> matchingWorks = ORCIDHelper.getExternalIdentifiersDiff(
					orcidWorks.get(counter), localWorks);
			if (matchingWorks.isEmpty()) {
				helper.getFullWork(orcidWorks.get(counter).getPutCode(), fullWorks);
				putcodes.put(orcidWorks.get(counter).getPutCode(), counter);
			}
		}

		try {
			helper.waitWorkers();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (Work fullWork : fullWorks) {
			int counter = putcodes.get(fullWork.getPutCode());
			fullWork.setExternalIdentifiers(orcidWorks.get(counter).getExternalIdentifiers());
			ORCIDHelper.cleanWorkLocalKey(fullWork);
			worksToImport.add(fullWork);
		}

		progressHandler.done();

		return worksToImport;
	}

	/**
	 * <p>
	 * Discovers updates to existing local CRIS productions in an ORCID profile.
	 * For each work group at ORCID (merged into as a single work by the
	 * {@link ORCIDHelper helper}), finds matching local productions (i.e.,
	 * those with shared {@link ExternalIdentifier external identifiers}) and
	 * creates update notifications if not already up to date.
	 * </p>
	 * 
	 * <p>
	 * Currently, these update notifications simply take the shape of ORCID
	 * works themselves (representing a matching work group). Concretely, the
	 * meta-data of the local productions is preserved, the only modification
	 * being the introduction of newly found external identifiers (i.e., those
	 * that were already present in the local productions that is being updated
	 * are removed from the returned updates).
	 * </p>
	 * 
	 * <p>
	 * The local productions are tested to be up-to-date by simply checking
	 * whether they contain every external identifiers in the ORCID group (see
	 * {@link ORCIDHelper#updateWork(BigInteger, Work)}). Thus the remainder
	 * meta-data of the local productions can be currently left null.
	 * </p>
	 * 
	 * <p>
	 * The put-code attribute is used as a local key of each CRIS productions.
	 * This means that the returned works representing the updates should have
	 * the put-code of the local work that is to be updated (and not the
	 * put-code of the ORCID works that gave origin to it).
	 * </p>
	 *
	 * @param orcidClient
	 *            The ORCID client defining the CRIS Member API and the profile
	 *            to be managed.
	 * @param localWorks
	 *            The list of local productions for which we wish to discover
	 *            updates (those marked as synced).
	 * @param progressHandler
	 *            The progress handler responsible for receiving progress
	 *            updates.
	 * @return The list of updated works found in the profile.
	 * @throws OrcidClientException
	 *             If the communication with ORCID fails.
	 * @throws InterruptedException
	 */
	public static List<Work> importUpdates(ORCIDClient orcidClient, List<Work> localWorks,
			ProgressHandler progressHandler) throws OrcidClientException {
		int progress = 0;
		progressHandler.setProgress(progress);
		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_UPDATES_STARTED");

		List<Work> worksToUpdate = new LinkedList<Work>();
		ORCIDHelper helper = new ORCIDHelper(orcidClient);
		List<WorkSummary> orcidWorks = helper.getAllWorkSummaries();

		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_UPDATES_ITERATION");
		for (int counter = 0; counter != orcidWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / orcidWorks.size()) * 100);
			progressHandler.setProgress(progress);

			Map<Work, ExternalIdentifiersUpdate> matchingLocalWorks = ORCIDHelper.getExternalIdentifiersDiff(
					orcidWorks.get(counter), localWorks);
			if (!matchingLocalWorks.isEmpty()) {
				for (Work mathingLocalWork : matchingLocalWorks.keySet()) {
					if (!ORCIDHelper.hasNewIDs(mathingLocalWork, orcidWorks.get(counter))) {
						Work workUpdate = ORCIDHelper.clone(mathingLocalWork);
						WorkExternalIdentifiers weids = new WorkExternalIdentifiers();
						weids.setWorkExternalIdentifier(new ArrayList<ExternalIdentifier>(matchingLocalWorks
								.get(mathingLocalWork).more));
						ORCIDHelper.setWorkLocalKey(workUpdate, ORCIDHelper.getWorkLocalKey(mathingLocalWork));
						workUpdate.setExternalIdentifiers(weids);
						worksToUpdate.add(workUpdate);
					}
				}
			}
		}

		progressHandler.done();
		return worksToUpdate;
	}

}
