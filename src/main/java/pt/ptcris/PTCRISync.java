package pt.ptcris;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.activities.WorkGroup;
import org.um.dsi.gavea.orcid.model.work.ExternalIdentifier;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

import pt.ptcris.handlers.ProgressHandler;
import pt.ptcris.utils.UpdateRecord;
import pt.ptcris.ORCIDHelper;

public class PTCRISync {

	private static ORCIDHelper helper;

	/**
	 * Export a list of works to an ORCID profile.
	 * 
	 * @param orcidClient
	 *            The ORCID client to access to the profile.
	 * @param local_work
	 *            The list of works to be exported (those marked as synced).
	 * @param progressHandler
	 *            The implementation of the ProgressHandler interface
	 *            responsible for receiving progress updates.
	 * @throws ORCIDClientException
	 */
	public static void export(ORCIDClient orcidClient, List<Work> localWorks, ProgressHandler progressHandler)
			throws OrcidClientException {

		int progress = 0;
		progressHandler.setProgress(progress);
		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_STARTED");

		helper = new ORCIDHelper(orcidClient);

		List<WorkSummary> orcidWorks = helper.getSourcedWorkSummaries();
		List<UpdateRecord> recordsToUpdate = new LinkedList<UpdateRecord>();

		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_WORKS_ITERATION");
		for (int counter = 0; counter != orcidWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / orcidWorks.size()) * 100);
			progressHandler.setProgress(progress);

			List<Work> matchingWorks = ORCIDHelper.getWorksWithSharedUIDs(orcidWorks.get(counter), localWorks);
			if (matchingWorks.isEmpty()) {
				helper.deleteWork(orcidWorks.get(counter).getPutCode());
			} else {
				for (Work localWork : matchingWorks) {
					Work orcidWork = helper.getFullWork(orcidWorks.get(counter).getPutCode());
					recordsToUpdate.add(new UpdateRecord(localWork, orcidWork));
					localWorks.remove(localWork);
				}
			}
		}

		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_UPDATING_WORKS");
		for (int counter = 0; counter != recordsToUpdate.size(); counter++) {
			progress = (int) ((double) ((double) counter / recordsToUpdate.size()) * 100);
			progressHandler.setProgress(progress);

			helper.updateWork(ORCIDHelper.getWorkPutCode(recordsToUpdate.get(counter).getRemoteWork()), recordsToUpdate
					.get(counter).getLocalWork());
		}

		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_ADDING_WORKS");
		for (int counter = 0; counter != localWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / localWorks.size()) * 100);
			progressHandler.setProgress(progress);

			helper.addWork(localWorks.get(counter));
		}

		progressHandler.done();

	}

	/**
	 * Discover new works in an ORCID profile. Creates creation notifications
	 * for each work group at ORCID (merged into as a single work by the
	 * {@link ORCIDHelper helper}) without matching local works (i.e., those
	 * without shared {@link ExternalIdentifier external identifiers}).
	 * 
	 * Currently, these creation notifications simply take the shape of ORCID
	 * works themselves (representing a matching group). The group merging
	 * selects the meta-data of the preferred work and the external identifiers
	 * of the whole group (see {@link ORCIDHelper#groupToWork(WorkGroup)}). The
	 * selection of the meta-data from a group could be changed without
	 * affecting the correction of the procedure.
	 * 
	 * Since the put-code attribute is used as a local key of each work, is
	 * should be null for these creation notifications.Since only the put-codes
	 * are being updated, the remainder meta-data of the local works can be
	 * currently left null.
	 * 
	 * @param orcidClient
	 *            The ORCID client to access to the profile.
	 * @param localWorks
	 *            The full list of works in the local profile.
	 * @param progressHandler
	 *            The implementation of the ProgressHandler interface
	 *            responsible for receiving progress updates
	 * @return The list of new works found in the profile.
	 * @throws OrcidClientException
	 *             If the communication with ORCID fails.
	 */
	public static List<Work> importWorks(ORCIDClient orcidClient, List<Work> localWorks, ProgressHandler progressHandler)
			throws OrcidClientException {
		int progress = 0;
		progressHandler.setProgress(progress);
		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_STARTED");

		List<Work> worksToImport = new LinkedList<Work>();

		helper = new ORCIDHelper(orcidClient);

		List<WorkSummary> orcidWorks = helper.getAllWorkSummaries();

		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_ITERATION");
		for (int counter = 0; counter != orcidWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / orcidWorks.size()) * 100);
			progressHandler.setProgress(progress);

			List<Work> matchingWorks = ORCIDHelper.getWorksWithSharedUIDs(orcidWorks.get(counter), localWorks);
			if (matchingWorks.isEmpty()) {
				Work orcidWork = helper.getFullWork(orcidWorks.get(counter).getPutCode());
				orcidWork.setExternalIdentifiers(orcidWorks.get(counter).getExternalIdentifiers());
				orcidWork.setPutCode(null);
				worksToImport.add(orcidWork);
			}
		}

		progressHandler.done();

		return worksToImport;
	}

	/**
	 * Discover updates to existing works in an ORCID profile. For each work
	 * group at ORCID (merged into as a single work by the {@link ORCIDHelper
	 * helper}), finds matching local works (i.e., those with shared
	 * {@link ExternalIdentifier external identifiers}) and creates update
	 * notifications if not already up to date.
	 * 
	 * Currently, these update notifications simply take the shape of ORCID
	 * works themselves (representing a matching group). The group merging
	 * selects the meta-data of the preferred work and the external identifiers
	 * of the whole group (see {@link ORCIDHelper#groupToWork(WorkGroup)}). The
	 * selection of the meta-data from a group could be changed without
	 * affecting the correction of the procedure.
	 * 
	 * Only the changed data is returned. Concretely, only the new
	 * external identifiers are contained in the updated works.
	 * 
	 * The put-code attribute should be used as a local key of each work. This
	 * means that the returned works representing the updates should have the
	 * put-code of the local work that is to be updated. Since only the
	 * put-codes are being updated, the remainder meta-data of the local works
	 * can be currently left null.
	 *
	 * The local works are tested to be up-to-date by simply checking whether
	 * they contain every external identifiers in the ORCID group (see
	 * {@link ORCIDHelper#updateWork(BigInteger, Work)}).
	 * 
	 * @param orcidClient
	 *            The ORCID client to access to the profile.
	 * @param localWorks
	 *            The list of works for which we wish to discover updates (local
	 *            works marked as synced).
	 * @param progressHandler
	 *            The implementation of the ProgressHandler interface
	 *            responsible for receiving progress updates
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
		helper = new ORCIDHelper(orcidClient);
		List<WorkSummary> orcidWorks = helper.getAllWorkSummaries();

		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_UPDATES_ITERATION");
		for (int counter = 0; counter != orcidWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / orcidWorks.size()) * 100);
			progressHandler.setProgress(progress);

			List<Work> matchingWorks = ORCIDHelper.getWorksWithSharedUIDs(orcidWorks.get(counter), localWorks);
			if (!matchingWorks.isEmpty()) {
				for (Work localWork : matchingWorks) {
					Work orcidWork = helper.getFullWork(orcidWorks.get(counter).getPutCode());
					orcidWork.setExternalIdentifiers(ORCIDHelper.difference(orcidWorks.get(counter).getExternalIdentifiers(),localWork.getExternalIdentifiers()));
					orcidWork.setPutCode(localWork.getPutCode());
					if (!ORCIDHelper.isAlreadyUpToDate(localWork, orcidWork)) {
						worksToUpdate.add(orcidWork);
					}
				}
			}
		}

		progressHandler.done();

		return worksToUpdate;
	}

}
