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
import pt.ptcris.exceptions.InvalidWorkException;

/**
 * <p>
 * An implementation of the PTCRISync synchronization service based on the
 * version 4.2 of the specification. This service allows CRIS services to
 * maintain their repositories synchronized with ORCID. This requires the CRIS
 * service to have access to the ORCID Member API.
 * </p>
 * 
 * <p>
 * The service has two main functionalities: to keep a set of local productions
 * updated in an ORCID user profile through an
 * {@link #export(ORCIDClient, List, ProgressHandler) export} procedure, and
 * import productions not known locally from the ORCID profile, either through
 * the {@link #importWorks(ORCIDClient, List, ProgressHandler) import} for
 * completely new productions or through the
 * {@link #importUpdates(ORCIDClient, List, ProgressHandler) import} for new
 * information for already known productions. Works must meet certain quality
 * criteria to be imported (the set of invalid works can be retrieved as well
 * through {@link #importInvalid(ORCIDClient, List, ProgressHandler)}.
 * </p>
 * 
 * <p>
 * The implementation of the service assumes that the local CRIS communicates
 * the local productions following the ORCID schema, in particular encoding them
 * as ORCID {@link Work works}. This uniforms the API and simplifies the
 * synchronization process.
 * </p>
 * 
 * <p>
 * The communication with ORCID is encapsulated in an ORCID {@link ORCIDClient
 * client} that contains information regarding the CRIS Member API and the ORCID
 * profile that is to be managed. Nonetheless, communication should ideally be
 * performed through the {@link ORCIDHelper helper}.
 * </p>
 * 
 * @see <a
 *      href="https://ptcris.pt/hub-ptcris/">https://ptcris.pt/hub-ptcris/</a>
 *
 */
public class PTCRISync {

	/**
	 * <p>
	 * A version of the export procedure (see
	 * {@link #exportBase(ORCIDClient, List, ProgressHandler, boolean)}) that
	 * tests whether the meta-data is up-to-date prior to updating a work in
	 * ORCID.
	 * </p>
	 * 
	 * <p>
	 * A work is assumed to be up-to-date if the external identifiers, title,
	 * type and publication year are the same (see
	 * {@link ORCIDHelper#isUpToDate(Work, WorkSummary)}).
	 * </p>
	 * 
	 * TODO: the algorithm does not currently consider contributors because this
	 * information is not contained in the work summaries, which would require
	 * additional calls to the ORCID API.
	 * 
	 * @see #exportBase(ORCIDClient, List, ProgressHandler, boolean)
	 */
	public static Map<BigInteger, PTCRISyncResult> export(ORCIDClient orcidClient, List<Work> localWorks,
			ProgressHandler progressHandler) throws OrcidClientException {
		return exportBase(orcidClient, localWorks, progressHandler, false);
	}

	/**
	 * <p>
	 * A version of the export procedure (see
	 * {@link #exportBase(ORCIDClient, List, ProgressHandler, boolean)}) that
	 * forces the update of the CRIS sourced works at ORCID, even if they are
	 * already up-to-date.
	 * </p>
	 * 
	 * <p>
	 * The caller of this method should guarantee that the input local works
	 * have been effectively updated, otherwise there will be unnecessary calls
	 * to the ORCID API.
	 * </p>
	 * 
	 * @see #exportBase(ORCIDClient, List, ProgressHandler, boolean)
	 */
	public static Map<BigInteger, PTCRISyncResult> exportForce(ORCIDClient orcidClient, List<Work> localWorks,
			ProgressHandler progressHandler) throws OrcidClientException {
		return exportBase(orcidClient, localWorks, progressHandler, true);
	}

	/**
	 * <p>
	 * Exports a list of local CRIS productions to an ORCID profile. This
	 * procedure essentially manages the works in the ORCID profile that are
	 * sourced by the CRIS, both previously specified in the client.
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
	 * Unless {@code forced}, the ORCID works are only updated if the meta-data
	 * is not up-to-date. Currently, the title, publication year and type are
	 * considered (see {@link ORCIDHelper#isUpToDate(Work, WorkSummary)}).
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
	 * The provided local works must match a quality criteria to be kept
	 * synchronized in ORCID. Currently, this forces the existence of external
	 * identifiers, the title, publication year and publication type (see
	 * {@link ORCIDHelper#hasMinimalQuality(Work)}).
	 * </p>
	 * 
	 * <p>
	 * The procedure reports the status for each of the input local works,
	 * identifying them by the provided local put-code. The codes are the same
	 * as reported by the ORCID API, with two exceptions: when a local work does
	 * not match the quality criteria ( {@link ORCIDHelper#INVALID}) and when an
	 * ORCID work is not updated due to already being up-to-date (
	 * {@link ORCIDHelper#UPTODATE}).
	 * </p>
	 * 
	 * <p>
	 * This procedure performs a GET call to the API to obtain the summaries and
	 * PUT or POST calls for each of the local input works. Additionally, DELETE
	 * calls can also be performed.
	 * </p>
	 * 
	 * TODO: The procedure does not currently consider the contributors
	 * (authors) of a work when assessing the quality criteria nor when
	 * assessing whether an ORCID work is up-to-date, as this would require the
	 * retrieval of the full work, and an additional call to the ORCID API.
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
	 * @param forced
	 *            Whether the update of ORCID works should be forced, even if
	 *            up-to-date.
	 * @returns The status of the export of each of the provided local works.
	 * 
	 * @throws OrcidClientException
	 *             If the communication with ORCID fails.
	 */
	private static Map<BigInteger,PTCRISyncResult> exportBase(ORCIDClient orcidClient, List<Work> localWorks,
			ProgressHandler progressHandler, boolean forced) throws OrcidClientException {

		int progress = 0;
		progressHandler.setProgress(progress);
		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_STARTED");

		Map<BigInteger, PTCRISyncResult> result = new HashMap<BigInteger, PTCRISyncResult>();

		ORCIDHelper helper = new ORCIDHelper(orcidClient);
		List<WorkSummary> orcidWorks = helper.getSourcedWorkSummaries();

		List<UpdateRecord> recordsToUpdate = new LinkedList<UpdateRecord>();

		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_WORKS_QUALITY");
		Set<Work> no_quality = new HashSet<Work>();
		for (int counter = 0; counter != localWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / localWorks.size()) * 100);
			progressHandler.setProgress(progress);
			Work localWork = localWorks.get(counter);

			try {
				ORCIDHelper.testMinimalQuality(localWork);
			} catch (InvalidWorkException invalidWork) {
				no_quality.add(localWork);
				PTCRISyncResult resultObj = new PTCRISyncResult(ORCIDHelper.INVALID,invalidWork);
				result.put(ORCIDHelper.getWorkLocalKey(localWork), resultObj);
			}
		}
		localWorks.removeAll(no_quality);

		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_WORKS_ITERATION");
		for (int counter = 0; counter != orcidWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / orcidWorks.size()) * 100);
			progressHandler.setProgress(progress);

			Map<Work, ExternalIdentifiersUpdate> matchingWorks = ORCIDHelper.getExternalIdentifiersDiff(
					orcidWorks.get(counter), localWorks);
			// there is no local work matching a CRIS sourced remote work
			if (matchingWorks.isEmpty()) {
				try {
					helper.deleteWork(orcidWorks.get(counter).getPutCode());
				} catch (OrcidClientException e) {
					// TODO: what to do?
					PTCRISyncResult resultObj = new PTCRISyncResult(ORCIDHelper.CLIENTERROR,e);
					result.put(ORCIDHelper.getWorkLocalKey(orcidWorks.get(counter)), resultObj);
				}
			}
			// there is at least one local work matching a CRIS sourced remote
			// work
			else {
				Work localWork = matchingWorks.keySet().iterator().next();
				// if the remote work is not up-to-date or forced updates
				if (forced || !ORCIDHelper.isUpToDate(localWork, orcidWorks.get(counter))) {
					recordsToUpdate.add(new UpdateRecord(localWork, orcidWorks.get(counter), matchingWorks
							.get(localWork)));
				} else {
					PTCRISyncResult resultObj = new PTCRISyncResult(ORCIDHelper.UPTODATE);
					result.put(ORCIDHelper.getWorkLocalKey(localWork), resultObj);
				}
				localWorks.remove(localWork);
			}
		}

		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_UPDATING_WORKS_PHASE_1");
		for (int counter = 0; counter != recordsToUpdate.size(); counter++) {
			progress = (int) ((double) ((double) counter / recordsToUpdate.size()) * 100);
			progressHandler.setProgress(progress);

			// the remote work has spurious external identifiers
			if (!recordsToUpdate.get(counter).getMatches().more.isEmpty()) {
				Work localWork = recordsToUpdate.get(counter).getLocalWork();
				WorkExternalIdentifiers weids = new WorkExternalIdentifiers();
				List<ExternalIdentifier> ids = new ArrayList<ExternalIdentifier>(recordsToUpdate.get(counter)
						.getMatches().same);
				weids.setWorkExternalIdentifier(ids);
				localWork.setExternalIdentifiers(weids);
				try {
					helper.updateWork(recordsToUpdate.get(counter).getRemoteWork().getPutCode(), localWork);
					PTCRISyncResult resultObj = new PTCRISyncResult(ORCIDHelper.UPDATEOK);
					result.put(ORCIDHelper.getWorkLocalKey(localWork), resultObj);
				} catch (OrcidClientException e) {
					PTCRISyncResult resultObj = new PTCRISyncResult(ORCIDHelper.CLIENTERROR,e);
					result.put(ORCIDHelper.getWorkLocalKey(localWork), resultObj);
					// TODO: what else to do?
				}
			}
		}

		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_UPDATING_WORKS_PHASE_2");
		for (int counter = 0; counter != recordsToUpdate.size(); counter++) {
			progress = (int) ((double) ((double) counter / recordsToUpdate.size()) * 100);
			progressHandler.setProgress(progress);

			// the remote work is missing external identifiers or not updated in
			// the 1st phase
			if (!recordsToUpdate.get(counter).getMatches().less.isEmpty()
					|| recordsToUpdate.get(counter).getMatches().more.isEmpty()) {
				Work localWork = recordsToUpdate.get(counter).getLocalWork();
				WorkExternalIdentifiers weids = new WorkExternalIdentifiers();
				List<ExternalIdentifier> ids = new ArrayList<ExternalIdentifier>(recordsToUpdate.get(counter)
						.getMatches().same);
				ids.addAll(recordsToUpdate.get(counter).getMatches().less);
				weids.setWorkExternalIdentifier(ids);
				localWork.setExternalIdentifiers(weids);
				try {
					helper.updateWork(recordsToUpdate.get(counter).getRemoteWork().getPutCode(), localWork);
					PTCRISyncResult resultObj = new PTCRISyncResult(ORCIDHelper.UPDATEOK);
					result.put(ORCIDHelper.getWorkLocalKey(localWork), resultObj);
				} catch (OrcidClientException e) {
					PTCRISyncResult resultObj = new PTCRISyncResult(ORCIDHelper.CLIENTERROR,e);
					result.put(ORCIDHelper.getWorkLocalKey(localWork), resultObj);
					// TODO: what else to do?
				}
			}
		}

		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_ADDING_WORKS");
		for (int counter = 0; counter != localWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / localWorks.size()) * 100);
			progressHandler.setProgress(progress);

			Work localWork = localWorks.get(counter);

			// local works that were not updated remaining
			try {
				BigInteger remotePutcode = helper.addWork(localWork);
				PTCRISyncResult resultObj = new PTCRISyncResult(ORCIDHelper.ADDOK);
				result.put(ORCIDHelper.getWorkLocalKey(localWork), resultObj);
			} catch (OrcidClientException e) {
				PTCRISyncResult resultObj = new PTCRISyncResult(ORCIDHelper.CLIENTERROR,e);
				result.put(ORCIDHelper.getWorkLocalKey(localWork), resultObj);
				// TODO: what else to do?
			}
		}

		progressHandler.done();
		return result;
	}

	/**
	 * <p>
	 * Discovers new valid works in an ORCID profile given a set of known local
	 * CRIS productions. Creates creation notifications for each work group at
	 * ORCID (merged into as a single work by the {@link ORCIDHelper helper})
	 * without matching local productions (i.e., those without shared
	 * {@link ExternalIdentifier external identifiers}). To import updates for
	 * works with shared external identifiers
	 * {@link #importUpdates(ORCIDClient, List, ProgressHandler)} should be used
	 * instead.
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
	 * the remote ORCID works that gave origin to it). Since only the external
	 * identifiers of the local productions are used to search for matches, the
	 * remainder meta-data of the input local works could be left null.
	 * </p>
	 *
	 * <p>
	 * ORCID works without minimal quality are ignored by this procedure.
	 * Currently, the quality criteria forces the existence of external
	 * identifiers, the title, publication year and publication type (see
	 * {@link ORCIDHelper#hasMinimalQuality(Work)}). Works that do not match the
	 * criteria can be imported with
	 * {@link #importInvalid(ORCIDClient, List, ProgressHandler)}. Note that
	 * currently group merging simply collects the meta-data (other than the
	 * external identifiers) from the preferred work, which is used in the
	 * quality assessment.
	 * </p>
	 * 
	 * <p>
	 * This procedure performs a GET call to the API to obtain the summaries and
	 * an additional GET call for each work identified as valid.
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
			throws OrcidClientException, InterruptedException {
		int progress = 0;
		progressHandler.setProgress(progress);
		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_STARTED");

		Map<BigInteger, Work> worksToImport = new HashMap<BigInteger, Work>();

		ORCIDHelper helper = new ORCIDHelper(orcidClient);

		List<WorkSummary> mergedOrcidWorks = helper.getAllWorkSummaries();

		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_ITERATION");
		for (int counter = 0; counter != mergedOrcidWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / mergedOrcidWorks.size()) * 100);
			progressHandler.setProgress(progress);

			WorkSummary mergedOrcidWork = mergedOrcidWorks.get(counter);
			Map<Work, ExternalIdentifiersUpdate> matchingWorks = ORCIDHelper.getExternalIdentifiersDiff(
					mergedOrcidWork, localWorks);
			if (matchingWorks.isEmpty() && ORCIDHelper.testMinimalQuality(mergedOrcidWork).isEmpty()) {
				helper.getFullWork(mergedOrcidWork, worksToImport);
			}
		}

		helper.waitWorkers();

		progressHandler.done();

		return new LinkedList<Work>(worksToImport.values());
	}

	/**
	 * <p>
	 * Counts new valid works in an ORCID profile given a set of known local
	 * CRIS productions, following the criteria of
	 * {@link #importWorks(ORCIDClient, List, ProgressHandler)}.
	 * </p>
	 * 
	 * <p>
	 * This procedure simply performs a GET call to the API to obtain the
	 * summaries, since the remainder meta-data is irrelevant, rendering it more
	 * efficient than {@link #importWorks(ORCIDClient, List, ProgressHandler)}.
	 * </p>
	 * 
	 * @see #importWorks(ORCIDClient, List, ProgressHandler)
	 * 
	 * @param orcidClient
	 *            The ORCID client defining the CRIS Member API and the profile
	 *            to be managed.
	 * @param localWorks
	 *            The full list of productions in the local profile.
	 * @param progressHandler
	 *            The progress handler responsible for receiving progress
	 *            updates.
	 * @return The number of new works found in the profile.
	 * @throws OrcidClientException
	 *             If the communication with ORCID fails.
	 */
	public static Integer importCounter(ORCIDClient orcidClient, List<Work> localWorks, ProgressHandler progressHandler)
			throws OrcidClientException {
		int progress = 0;
		int c = 0;
		progressHandler.setProgress(progress);
		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_STARTED");

		ORCIDHelper helper = new ORCIDHelper(orcidClient);

		List<WorkSummary> mergedOrcidWorks = helper.getAllWorkSummaries();

		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_ITERATION");
		for (int counter = 0; counter != mergedOrcidWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / mergedOrcidWorks.size()) * 100);
			progressHandler.setProgress(progress);

			WorkSummary mergedOrcidWork = mergedOrcidWorks.get(counter);
			Map<Work, ExternalIdentifiersUpdate> matchingWorks = ORCIDHelper.getExternalIdentifiersDiff(
					mergedOrcidWork, localWorks);
			if (matchingWorks.isEmpty() && ORCIDHelper.testMinimalQuality(mergedOrcidWork).isEmpty()) {
				c++;
			}
		}

		progressHandler.done();

		return c;
	}

	/**
	 * <p>
	 * Discovers updates to existing local CRIS productions in an ORCID profile.
	 * For each work group at ORCID (merged into as a single work by the
	 * {@link ORCIDHelper helper}), finds matching local productions (i.e.,
	 * those with shared {@link ExternalIdentifier external identifiers}) and
	 * creates update notifications if not already up to date. To import works
	 * without shared external identifiers,
	 * {@link #importWorks(ORCIDClient, List, ProgressHandler)} should be used
	 * instead.
	 * </p>
	 * 
	 * <p>
	 * Currently, these update notifications simply take the shape of ORCID
	 * works themselves (representing a matching work group). These works
	 * contain only the data that needs to be updated locally. Currently, only
	 * the introduction of newly found external identifiers is considered (i.e.,
	 * those that were already present in the local productions that is being
	 * updated are removed from the returned updates). Thus, the remainder
	 * fields are returned null.
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
	 * <p>
	 * This procedure simply performs a GET call to the API to obtain the
	 * summaries, since the remainder meta-data is irrelevant.
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
						workUpdate.setTitle(null);
						workUpdate.setType(null);
						workUpdate.setPublicationDate(null);
						worksToUpdate.add(workUpdate);
					}
				}
			}
		}

		progressHandler.done();
		return worksToUpdate;
	}

	/**
	 * <p>
	 * Discovers new invalid works (that do not pass the quality criteria) in an
	 * ORCID profile given a set of known local CRIS productions, as well as the
	 * causes for invalidity (defined at {@link ORCIDHelper}). Other than the
	 * criteria, the behavior is similar to that of
	 * {@link #importWorks(ORCIDClient, List, ProgressHandler)}. Note that
	 * currently group merging simply collects the meta-data (other than the
	 * external identifiers) from the preferred work, which is used in the
	 * quality assessment.
	 * </p>
	 * 
	 * <p>
	 * This procedure performs a GET call to the API to obtain the summaries and
	 * an additional GET call for each work identified as invalid.
	 * </p>
	 * 
	 * @see #importWorks(ORCIDClient, List, ProgressHandler)
	 * 
	 * @param orcidClient
	 *            The ORCID client defining the CRIS Member API and the profile
	 *            to be managed.
	 * @param localWorks
	 *            The full list of productions in the local profile.
	 * @param progressHandler
	 *            The progress handler responsible for receiving progress
	 *            updates.
	 * @return The list of invalid works found in the profile, as well as the
	 *         invalid fields.
	 * @throws OrcidClientException
	 *             If the communication with ORCID fails.
	 * @throws InterruptedException
	 */
	public static Map<Work, Set<String>> importInvalid(ORCIDClient orcidClient, List<Work> localWorks,
			ProgressHandler progressHandler) throws OrcidClientException, InterruptedException {
		int progress = 0;
		progressHandler.setProgress(progress);
		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_INVALID_STARTED");

		Map<BigInteger, Set<String>> invalidsToImport = new HashMap<BigInteger, Set<String>>();
		Map<BigInteger, Work> worksToImport = new HashMap<BigInteger, Work>();

		ORCIDHelper helper = new ORCIDHelper(orcidClient);

		List<WorkSummary> mergedOrcidWorks = helper.getAllWorkSummaries();

		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_INVALID_ITERATION");
		for (int counter = 0; counter != mergedOrcidWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / mergedOrcidWorks.size()) * 100);
			progressHandler.setProgress(progress);

			WorkSummary mergedOrcidWork = mergedOrcidWorks.get(counter);
			Map<Work, ExternalIdentifiersUpdate> matchingWorks = ORCIDHelper.getExternalIdentifiersDiff(
					mergedOrcidWork, localWorks);
			Set<String> invalids = ORCIDHelper.testMinimalQuality(mergedOrcidWork);
			invalidsToImport.put(mergedOrcidWork.getPutCode(), invalids);
			if (matchingWorks.isEmpty() && !invalids.isEmpty()) {
				helper.getFullWork(mergedOrcidWork, worksToImport);
			}
		}

		helper.waitWorkers();

		progressHandler.done();

		Map<Work, Set<String>> res = new HashMap<Work, Set<String>>();

		for (BigInteger i : worksToImport.keySet())
			res.put(worksToImport.get(i), invalidsToImport.get(i));

		return res;
	}

}
