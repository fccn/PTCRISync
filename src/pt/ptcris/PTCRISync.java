package pt.ptcris;

import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.orcid.jaxb.model.record.summary_rc2.WorkSummary;
import org.orcid.jaxb.model.record_rc2.Work;

import pt.ptcris.handlers.ProgressHandler;
import pt.ptcris.utils.UpdateRecord;

public class PTCRISync {

	private static ORCIDHelper helper;

	private static final String ORCID_URI = "https://api.sandbox.orcid.org/v2.0_rc2/";

	/**
	 * Export a list of works to an ORCID profile.
	 * 
	 * @param orcidID
	 *            The ORCID id of the profile to be updated.
	 * @param accessToken
	 *            The security token that grants update access to the profile.
	 * @param local_work
	 *            The list of works to be exported (those marked as synced).
	 * @param clientSourceName
	 *            The identifier of the client profile.
	 * @param progressHandler
	 *            The implementation of the ProgressHandler interface responsible for receiving progress updates.
	 * @throws ORCIDException 
	 */
	public static void export(String orcidID, String accessToken, List<Work> localWorks, String clientSourceName, ProgressHandler progressHandler) throws ORCIDException {
		int progress = 0;
		progressHandler.setProgress(progress);
		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_STARTED");

		try {
			helper = new ORCIDHelper(ORCID_URI,orcidID,accessToken);
	
			List<WorkSummary> orcidWorks = helper.getSourcedWorkSummaries(clientSourceName);
	
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
	
				helper.updateWork(recordsToUpdate.get(counter).getRemoteWork().getPutCode(),recordsToUpdate.get(counter).getLocalWork());
			}
	
			progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_ADDING_WORKS");
			for (int counter = 0; counter != localWorks.size(); counter++) {
				progress = (int) ((double) ((double) counter / localWorks.size()) * 100);
				progressHandler.setProgress(progress);
	
				helper.addWork(localWorks.get(counter));
			}
	
			progressHandler.done();

		} catch (URISyntaxException e) {
			e.printStackTrace();
		}


	}

	/**
	 * Discover new works in an ORCID profile.
	 * 
	 * @param orcidID
	 *            The ORCID id of the profile to be searched.
	 * @param accessToken
	 *            The security token that grants update access to the profile.
	 * @param localWorks
	 *            The full list of works in the local profile. In fact, for each work only the external identifiers are needed, so the remaining
	 *            attributes may be left null.
	 * @param progressHandler
	 *            The implementation of the ProgressHandler interface responsible for receiving progress updates
	 * @return The list of new works found in the ORCID profile.
	 * @throws ORCIDException 
	 */
	public static List<Work> importWorks(String orcidID, String accessToken, List<Work> localWorks, ProgressHandler progressHandler) throws ORCIDException {
		int progress = 0;
		progressHandler.setProgress(progress);
		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_STARTED");

		List<Work> worksToImport = new LinkedList<Work>();

		try {
			helper = new ORCIDHelper(ORCID_URI,orcidID,accessToken);

			List<WorkSummary> orcidWorks = helper.getAllWorkSummaries();

			progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_ITERATION");
			for (int counter = 0; counter != orcidWorks.size(); counter++) {
				progress = (int) ((double) ((double) counter / orcidWorks.size()) * 100);
				progressHandler.setProgress(progress);

				List<Work> matchingWorks = ORCIDHelper.getWorksWithSharedUIDs(orcidWorks.get(counter), localWorks);
				if (matchingWorks.isEmpty()) {
					Work orcidWork = helper.getFullWork(orcidWorks.get(counter).getPutCode());
					worksToImport.add(orcidWork);
				}
			}

			progressHandler.done();

		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		return worksToImport;
	}

	/**
	 * Discover updates to existing works in an ORCID profile.
	 * 
	 * @param orcidID
	 *            The ORCID id of the profile to be searched.
	 * @param accessToken
	 *            The security token that grants update access to the profile.
	 * @param localWorks
	 *            The list of works for which we wish to discover updates (those marked as synced). For the moment, only new external identifiers will
	 *            be found, so, for each work only the external identifiers are needed, so the remaining attributes may be left null. Also the putcode
	 *            attribute should be used to store the local key of each work.
	 * @param progressHandler
	 *            The implementation of the ProgressHandler interface responsible for receiving progress updates
	 * @return The list of updated works. Only the works that have changes are returned. Also, for each of them, only the attributes that changed are
	 *         set. For the moment, only new external identifiers will be returned.
	 * @throws ORCIDException 
	 */
	public static List<Work> importUpdates(String orcidID, String accessToken, List<Work> localWorks, ProgressHandler progressHandler) throws ORCIDException {
		int progress = 0;
		progressHandler.setProgress(progress);
		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_UPDATES_STARTED");

		List<Work> worksToUpdate = new LinkedList<Work>();

		List<WorkSummary> orcidWorks = helper.getAllWorkSummaries();

		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_UPDATES_ITERATION");
		for (int counter = 0; counter != orcidWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / orcidWorks.size()) * 100);
			progressHandler.setProgress(progress);

			List<Work> matchingWorks = ORCIDHelper.getWorksWithSharedUIDs(orcidWorks.get(counter), localWorks);
			if (!matchingWorks.isEmpty()) {
				for (Work localWork : matchingWorks) {
					Work orcidWork = helper.getFullWork(orcidWorks.get(counter).getPutCode());
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
