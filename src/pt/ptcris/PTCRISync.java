package pt.ptcris;

import java.util.LinkedList;
import java.util.List;

import org.orcid.jaxb.model.record_rc2.Work;

import pt.ptcris.handlers.ProgressHandler;
import pt.ptcris.utils.UpdateRecord;

public class PTCRISync {

	/**
	 * Export a list of works to an ORCID profile.
	 * 
	 * @param orcidID
	 *            The ORCID id of the profile to be updated.
	 * @param accessToken
	 *            The security token that grants update access to the profile.
	 * @param works
	 *            The list of works to be exported (those marked as synced).
	 * @param progressHandler
	 *            The implementation of the ProgressHandler interface responsible for receiving progress updates
	 */
	public static void export(String orcidID, String accessToken, List<Work> works, String clientSourceName, ProgressHandler progressHandler) {
		int progress = 0;
		progressHandler.setProgress(progress);
		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_STARTED");

		List<Work> orcidWorks = getLocalCRISSourcedORCIDWorks(orcidID, accessToken, clientSourceName);

		List<UpdateRecord> recordsToUpdate = new LinkedList<UpdateRecord>();

		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_WORKS_ITERATION");
		for (int counter = 0; counter != orcidWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / orcidWorks.size()) * 100);
			progressHandler.setProgress(progress);

			List<Work> matchingWorks = getWorksWithSharedUIDs(orcidWorks.get(counter), works);
			if (matchingWorks.isEmpty()) {
				deleteWork(orcidID, accessToken, orcidWorks.get(counter));
			} else {
				for (Work work : matchingWorks) {
					recordsToUpdate.add(new UpdateRecord(work, orcidWorks.get(counter)));
					works.remove(work);
				}
			}
		}

		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_UPDATING_WORKS");
		for (int counter = 0; counter != recordsToUpdate.size(); counter++) {
			progress = (int) ((double) ((double) counter / recordsToUpdate.size()) * 100);
			progressHandler.setProgress(progress);

			updateWork(orcidID, accessToken, recordsToUpdate.get(counter));
		}

		progressHandler.setCurrentStatus("ORCID_SYNC_EXPORT_ADDING_WORKS");
		for (int counter = 0; counter != works.size(); counter++) {
			progress = (int) ((double) ((double) counter / works.size()) * 100);
			progressHandler.setProgress(progress);

			addWork(orcidID, accessToken, works.get(counter));
		}

		progressHandler.done();
	}

	/**
	 * Discover new works in an ORCID profile.
	 * 
	 * @param orcidID
	 *            The ORCID id of the profile to be searched.
	 * @param accessToken
	 *            The security token that grants update access to the profile.
	 * @param works
	 *            The full list of works in the local profile. In fact, for each work only the external identifiers are needed, so the remaining
	 *            attributes may be left null.
	 * @param progressHandler
	 *            The implementation of the ProgressHandler interface responsible for receiving progress updates
	 * @return The list of new works found in the ORCID profile.
	 */
	public static List<Work> importWorks(String orcidID, String accessToken, List<Work> works, ProgressHandler progressHandler) {
		int progress = 0;
		progressHandler.setProgress(progress);
		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_STARTED");

		List<Work> worksToImport = new LinkedList<Work>();

		List<Work> orcidWorks = getAllORCIDWorks(orcidID, accessToken);

		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_ITERATION");
		for (int counter = 0; counter != orcidWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / orcidWorks.size()) * 100);
			progressHandler.setProgress(progress);

			List<Work> matchingWorks = getWorksWithSharedUIDs(orcidWorks.get(counter), works);
			if (matchingWorks.isEmpty()) {
				// Aqui é melhor ir ao ORCID sacar o registo completo do work
				// Temos que adicionar um métod para isso
				// Se deixarmos assim só vai adicionar informação básica sobre o work
				worksToImport.add(orcidWorks.get(counter));
			}
		}

		progressHandler.done();

		return worksToImport;
	}

	/**
	 * Discover updates to existing works in an ORCID profile.
	 * 
	 * @param orcidID
	 *            The ORCID id of the profile to be searched.
	 * @param accessToken
	 *            The security token that grants update access to the profile.
	 * @param works
	 *            The list of works for which we wish to discover updates (those marked as synced). For the moment, only new external identifiers will
	 *            be found, so, for each work only the external identifiers are needed, so the remaining attributes may be left null. Also the putcode
	 *            attribute should be used to store the local key of each work.
	 * @param progressHandler
	 *            The implementation of the ProgressHandler interface responsible for receiving progress updates
	 * @return The list of updated works. Only the works that have changes are returned. Also, for each of them, only the attributes that changed are
	 *         set. For the moment, only new external identifiers will be returned.
	 */
	public static List<Work> importUpdates(String orcidID, String accessToken, List<Work> works, ProgressHandler progressHandler) {
		int progress = 0;
		progressHandler.setProgress(progress);
		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_UPDATES_STARTED");

		List<Work> worksToUpdate = new LinkedList<Work>();

		List<Work> orcidWorks = getAllORCIDWorks(orcidID, accessToken);

		progressHandler.setCurrentStatus("ORCID_SYNC_IMPORT_UPDATES_ITERATION");
		for (int counter = 0; counter != orcidWorks.size(); counter++) {
			progress = (int) ((double) ((double) counter / orcidWorks.size()) * 100);
			progressHandler.setProgress(progress);

			List<Work> matchingWorks = getWorksWithSharedUIDs(orcidWorks.get(counter), works);
			if (!matchingWorks.isEmpty()) {
				for (Work work : matchingWorks) {
					if (!isAlreadyUpToDate(work, orcidWorks.get(counter))) {
						worksToUpdate.add(orcidWorks.get(counter));
					}
				}
			}
		}

		progressHandler.done();

		return worksToUpdate;
	}

	/**
	 * Retrieves the entire set of works in the ORCID profile whose source is the local CRIS service
	 * 
	 * @param orcidID
	 *            The ORCID id of the profile to be searched
	 * @param accessToken
	 *            The security token that grants update access to the profile
	 * @param serviceSourceName
	 *            The source name of the local CRIS service
	 * @return The set of works in the ORCID profile whose source is the local CRIS service
	 */
	private static List<Work> getLocalCRISSourcedORCIDWorks(String orcidID, String accessToken, String serviceSourceName) {
		// TODO Contact the ORCID API and retrieve the works which source name is the one given by serviceSourceName
		return new LinkedList<Work>();
	}

	/**
	 * Retrieves the set of productions (from works) that share some UIDs with work
	 * 
	 * @param work
	 *            The work to compare with the list of works
	 * @param works
	 *            The set of works to search for productions with shared UIDs
	 * @return The set of works with matching UIDs
	 */
	private static List<Work> getWorksWithSharedUIDs(Work work, List<Work> works) {
		// TODO Iterate through works and compare UIDs. If any UIDs match, return it
		// If the work has no UIDs it should return an empry list
		return new LinkedList<Work>();
	}

	/**
	 * Delete a work from the ORCID profile
	 * 
	 * @param orcidID
	 *            The ORCID id of the profile to be searched
	 * @param accessToken
	 *            The security token that grants update access to the profile
	 * @param work
	 *            The work to be deleted
	 */
	private static void deleteWork(String orcidID, String accessToken, Work work) {
		// TODO Contact the ORCID API and delete the work from the ORCID profile
		// NOTE: according to the ORCID API, to delete a work, one must provide the entire list of works in the ORCID profile minus the work(s) that
		// should be deleted. This means that this operation must be done in three steps: first, retrieve the entire set of works; second, remove the
		// work to be deleted from the list of works; and three, send the updated list to the ORCID API.
	}

	/**
	 * Update a work in the ORCID profile
	 * 
	 * @param orcidID
	 *            The ORCID id of the profile to be searched
	 * @param accessToken
	 *            The security token that grants update access to the profile
	 * @param updateRecord
	 *            The updateRecord that contains both the local and remote Works (the remote work is updated based on the data in the local work)
	 */
	private static void updateWork(String orcidID, String accessToken, UpdateRecord updateRecord) {
		// TODO Contact the ORCID API and update the work on the ORCID profile
		// NOTE: according to the ORCID API, to update a work, one must provide the entire list of works in the ORCID profile including the work(s)
		// that should be updated. This means that this operation must be done in three steps: first, retrieve the entire set of works; second,
		// replace the work to be updated with the new record in the list of works; and three, send the updated list to the ORCID API.
	}

	/**
	 * Add a work to the ORCID profile
	 * 
	 * @param orcidID
	 *            The ORCID id of the profile to be searched
	 * @param accessToken
	 *            The security token that grants update access to the profile
	 * @param work
	 *            The work to be added to the ORCID profile
	 */
	private static void addWork(String orcidID, String accessToken, Work work) {
		// TODO Contact the ORCID API and add the work in the ORCID profile
	}

	/**
	 * Retrieves the entire set of works in the ORCID profile
	 * 
	 * @param orcidID
	 *            The ORCID id of the profile to be searched
	 * @param accessToken
	 *            The security token that grants update access to the profile
	 * @return The set of works in the ORCID profile
	 */
	private static List<Work> getAllORCIDWorks(String orcidID, String accessToken) {
		// TODO Contact the ORCID API and retrieve all works in the ORCID profile
		return new LinkedList<Work>();
	}

	/**
	 * Checks if localWork is already up to date on the information from remoteWork, i.e., localWork already has the same UIDs as remoteWork
	 * 
	 * @param localWork
	 *            The local work to check if it is up to date
	 * @param remoteWork
	 *            The remote work to use when checking if the local work is up to date
	 * @return true if all the UIDs between the two works are the same, false otherwise
	 */
	private static boolean isAlreadyUpToDate(Work localWork, Work remoteWork) {
		// TODO Compare the two records to check if they are equal (when it comes to matching UIDs)
		return false;
	}
}
