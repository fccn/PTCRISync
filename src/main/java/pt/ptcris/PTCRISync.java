package pt.ptcris;

import java.util.LinkedList;
import java.util.List;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
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

			helper.updateWork(ORCIDHelper.getWorkPutCode(recordsToUpdate.get(counter).getRemoteWork()),
					recordsToUpdate.get(counter).getLocalWork());
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
	 * Discover new works in an ORCID profile.
	 * 
	 * @param orcidClient
	 *            The ORCID client to access to the profile.
	 * @param localWorks
	 *            The full list of works in the local profile. In fact, for each
	 *            work only the external identifiers are needed, so the
	 *            remaining attributes may be left null.
	 * @param progressHandler
	 *            The implementation of the ProgressHandler interface
	 *            responsible for receiving progress updates
	 * @return The list of new works found in the ORCID profile.
	 * @throws ORCIDClientException
	 */
	public static List<Work> importWorks(ORCIDClient orcidClient, List<Work> localWorks,
			ProgressHandler progressHandler) throws OrcidClientException {
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
				orcidWork.setWorkExternalIdentifiers(orcidWorks.get(counter).getExternalIdentifiers());
				worksToImport.add(orcidWork);
			}
		}

		progressHandler.done();

		return worksToImport;
	}

	/**
	 * Discover updates to existing works in an ORCID profile.
	 * 
	 * @param orcidClient
	 *            The ORCID client to access to the profile.
	 * @param localWorks
	 *            The list of works for which we wish to discover updates (those
	 *            marked as synced). For the moment, only new external
	 *            identifiers will be found, so, for each work only the external
	 *            identifiers are needed, so the remaining attributes may be
	 *            left null. Also the putcode attribute should be used to store
	 *            the local key of each work.
	 * @param progressHandler
	 *            The implementation of the ProgressHandler interface
	 *            responsible for receiving progress updates
	 * @return The list of updated works. Only the works that have changes are
	 *         returned. Also, for each of them, only the attributes that
	 *         changed are set. For the moment, only new external identifiers
	 *         will be returned.
	 * @throws ORCIDClientException
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
					orcidWork.setWorkExternalIdentifiers(orcidWorks.get(counter).getExternalIdentifiers());
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
