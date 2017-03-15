/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
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
import org.um.dsi.gavea.orcid.model.common.ExternalId;
import org.um.dsi.gavea.orcid.model.common.ExternalIds;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

import pt.ptcris.exceptions.InvalidWorkException;
import pt.ptcris.handlers.ProgressHandler;
import pt.ptcris.utils.ExternalIdsDiff;
import pt.ptcris.utils.ORCIDHelper;
import pt.ptcris.utils.UpdateRecord;

/**
 * <p>
 * An implementation of the PTCRISync synchronization service based on the
 * version 5.0 of the specification. This service allows CRIS services to
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
 * the local productions following the established ORCID schema, according to
 * the Member API 2.0rc2. This uniforms the API and simplifies the
 * synchronization process. The current version focuses on synchronizing
 * research productions, which must be encoded as ORCID {@link Work works}.
 * </p>
 *
 * <p>
 * The communication with ORCID is encapsulated in an ORCID {@link ORCIDClient
 * client} that contains information regarding the CRIS Member API and the ORCID
 * profile that is to be managed. An {@link ORCIDHelper helper} provides utility
 * methods for the synchronization of works.
 * </p>
 * 
 * <p>
 * The procedures do not currently consider the contributors (authors) of works
 * when assessing the quality criteria nor when assessing whether works are
 * up-to-date, as this information is not available in ORCID summaries and would
 * require additional calls to the ORCID API.
 * </p>
 *
 * @see <a href="https://ptcris.pt/hub-ptcris/">https://ptcris.pt/hub-ptcris/</a>
 */
public final class PTCRISync {

	/**
	 * <p>
	 * A version of the export procedure that tests whether the meta-data is
	 * up-to-date prior to updating a work in ORCID.
	 * </p>
	 *
	 * <p>
	 * A work is assumed to be up-to-date if the external identifiers, title,
	 * type and publication year are the same (see
	 * {@link ORCIDHelper#isUpToDate(Work, WorkSummary)}).
	 * </p>
	 *
	 * @see #exportBase(ORCIDClient, List, ProgressHandler, boolean)
	 * 
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param localWorks
	 *            the list of local productions to be exported that should be
	 *            kept synchronized in the ORCID user profile
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the result of the synchronization of each of the provided local
	 *          works.
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws NullPointerException
	 *             if any of the arguments is null	 */
	public static Map<BigInteger, PTCRISyncResult> export(ORCIDClient client, List<Work> localWorks, ProgressHandler handler)
			throws OrcidClientException, NullPointerException {
		return exportBase(client, localWorks, handler, false);
	}

	/**
	 * <p>
	 * A version of the export procedure that forces the update of the CRIS
	 * sourced works at ORCID, even if they are already up-to-date.
	 * </p>
	 *
	 * <p>
	 * The caller of this method should guarantee that the input local productions
	 * have been effectively updated, otherwise there will be unnecessary calls
	 * to the ORCID API.
	 * </p>
	 *
	 * @see #exportBase(ORCIDClient, List, ProgressHandler, boolean)
	 * 
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param localWorks
	 *            the list of local productions to be exported that should be
	 *            kept synchronized in the ORCID user profile
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the result of the synchronization of each of the provided local
	 *          works.
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws NullPointerException
	 *             if any of the arguments is null
	 */
	public static Map<BigInteger, PTCRISyncResult> exportForce(ORCIDClient client, List<Work> localWorks, ProgressHandler handler)
			throws OrcidClientException, NullPointerException {
		return exportBase(client, localWorks, handler, true);
	}

	/**
	 * <p>
	 * Exports a list of local CRIS productions to an ORCID profile. This
	 * procedure essentially manages the works in the ORCID profile that are
	 * sourced by the CRIS, both previously specified in the {@code client}.
	 * </p>
	 *
	 * <p>
	 * The procedure detects every CRIS sourced work in the ORCID profile that
	 * matches any local production that is being exported; if there is no matching
	 * local production, the ORCID work is deleted from the profile. Otherwise it will
	 * be updated with the meta-data of one of the matching local productions.
	 * Finally, for local productions without any matching ORCID work new ORCID works
	 * are created. The matching is performed by detecting shared
	 * {@link ExternalIdentifier external identifiers} (see
	 * {@link ORCIDHelper#getExternalIdsDiff(WorkSummary, Collection)}).
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
	 * The procedure expects the CRIS service to provide the local production in the
	 * ORCID schema, in particular encoding productions as {@link Work works}.
	 * Thus, the meta-data of the CRIS sourced works in the ORCID profile is
	 * exactly that of the provided local productions that are to be exported.
	 * The put-code of these local productions is however expected to be used as
	 * local key identifiers, since these are disregarded during the update of
	 * the ORCID profile (new works are assigned fresh put-codes and updated
	 * works use the put-code of the existing ORCID work).
	 * </p>
	 *
	 * <p>
	 * The provided local productions must pass a quality criteria to be kept
	 * synchronized in ORCID. Currently, this forces the existence of external
	 * identifiers, the title, publication year and publication type (see
	 * {@link ORCIDHelper#hasMinimalQuality(Work)}).
	 * </p>
	 *
	 * <p>
	 * The procedure reports the status for each of the input local productions,
	 * identifying them by the provided local put-code, including the ORCID
	 * error if the process failed. See {@link PTCRISyncResult} for the codes.
	 * If the local put-code is empty, returns a default value, which currently
	 * is the put-code remotely assigned by ORCID.
	 * </p>
	 *
	 * <p>
	 * This procedure performs a single GET call to the API to obtain the
	 * summaries and PUT or POST calls for each of the local input works.
	 * Additionally, DELETE calls can also be performed. The procedure only
	 * fails if the initial GET fails, otherwise individual failures are
	 * reported in the output. No asynchronous workers are used.
	 * </p>
	 *
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param localWorks
	 *            the list of local productions to be exported that should be
	 *            kept synchronized in the ORCID user profile
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @param forced
	 *            whether the update of ORCID works should be forced, without
	 *            testing if up-to-date
	 * @returns the result of the synchronization of each of the provided local
	 *          works.
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws NullPointerException
	 *             if any of the arguments is null
	 */
	private static Map<BigInteger, PTCRISyncResult> exportBase(ORCIDClient client, List<Work> localWorks, ProgressHandler handler, boolean forced)
			throws OrcidClientException, NullPointerException {
		if (client == null || localWorks == null || handler == null)
			throw new NullPointerException("Export failed.");
		
		
		int progress = 0;
		handler.setProgress(progress);
		handler.setCurrentStatus("ORCID_SYNC_EXPORT_STARTED");

		ORCIDHelper helper = new ORCIDHelper(client);
		List<WorkSummary> orcidWorks = helper.getSourcedWorkSummaries();

		Map<BigInteger, PTCRISyncResult> result = new HashMap<BigInteger, PTCRISyncResult>();

		// start by filtering local works that do not pass the quality criteria
		handler.setCurrentStatus("ORCID_SYNC_EXPORT_WORKS_QUALITY");
		Set<Work> invalidWorks = new HashSet<Work>();
		for (int c = 0; c != localWorks.size(); c++) {
			progress = (int) ((double) c / localWorks.size() * 100);
			handler.setProgress(progress);
			Work localWork = localWorks.get(c);

			try {
				ORCIDHelper.tryMinimalQuality(localWork,localWorks);
			} catch (InvalidWorkException invalid) {
				invalidWorks.add(localWork);
				result.put(ORCIDHelper.getActivityLocalKey(localWork, BigInteger.valueOf(c)),
						PTCRISyncResult.invalid(invalid));
			}
		}
		localWorks.removeAll(invalidWorks);

		// detect which remote works should be deleted or updated
		handler.setCurrentStatus("ORCID_SYNC_EXPORT_WORKS_ITERATION");
		List<UpdateRecord> toUpdate = new LinkedList<UpdateRecord>();
		for (int c = 0; c != orcidWorks.size(); c++) {
			progress = (int) ((double) c / orcidWorks.size() * 100);
			handler.setProgress(progress);
			WorkSummary orcidWork = orcidWorks.get(c);

			Map<Work, ExternalIdsDiff> worksDiffs = ORCIDHelper.getExternalIdsDiff(orcidWork, localWorks);
			// there is no local work matching a CRIS sourced remote work
			if (worksDiffs.isEmpty()) {
				// TODO: the delete may fail (the result is returned); how to communicate this to the caller?
				helper.deleteWork(orcidWork.getPutCode());
			}
			// there is at least one local work matching a CRIS sourced remote work
			else {
				Work localWork = worksDiffs.keySet().iterator().next();
				// if the remote work is not up-to-date or forced updates
				if (forced || !ORCIDHelper.isUpToDate(localWork, orcidWork))
					toUpdate.add(new UpdateRecord(localWork, orcidWork, worksDiffs.get(localWork)));
				else
					result.put(ORCIDHelper.getActivityLocalKey(localWork, BigInteger.valueOf(c)),
							PTCRISyncResult.UPTODATE_RESULT);
				localWorks.remove(localWork);
			}
		}

		// first update phase, remove spurious identifiers
		handler.setCurrentStatus("ORCID_SYNC_EXPORT_UPDATING_WORKS_PHASE_1");
		for (int c = 0; c != toUpdate.size(); c++) {
			progress = (int) ((double) c / toUpdate.size() * 100);
			handler.setProgress(progress);

			UpdateRecord update = toUpdate.get(c);
			// the remote work has spurious external identifiers
			if (!update.eidsDiff.more.isEmpty()) {
				Work localWork = update.preWork;
				ExternalIds weids = new ExternalIds();
				List<ExternalId> ids = new ArrayList<ExternalId>(update.eidsDiff.same);
				weids.setExternalId(ids);
				localWork.setExternalIds(weids);

				PTCRISyncResult res = helper.updateWork(update.posWork.getPutCode(), localWork);
				result.put(ORCIDHelper.getActivityLocalKey(localWork, BigInteger.valueOf(c)),res);
			}
		}

		// second update phase, add missing identifiers
		handler.setCurrentStatus("ORCID_SYNC_EXPORT_UPDATING_WORKS_PHASE_2");
		for (int c = 0; c != toUpdate.size(); c++) {
			progress = (int) ((double) c / toUpdate.size() * 100);
			handler.setProgress(progress);

			// the remote work is missing external identifiers or not updated in the 1st phase
			UpdateRecord update = toUpdate.get(c);
			if (!update.eidsDiff.less.isEmpty() || update.eidsDiff.more.isEmpty()) {
				Work localWork = update.preWork;
				ExternalIds weids = new ExternalIds();
				List<ExternalId> ids = new ArrayList<ExternalId>(update.eidsDiff.same);
				ids.addAll(update.eidsDiff.less);
				weids.setExternalId(ids);
				localWork.setExternalIds(weids);

				PTCRISyncResult res = helper.updateWork(update.posWork.getPutCode(), localWork);
				result.put(ORCIDHelper.getActivityLocalKey(localWork, BigInteger.valueOf(c)),res);
			}
		}
		
		// add the local works that had no match
		// the progress handler must be moved to the helper due to bulk additions
		handler.setCurrentStatus("ORCID_SYNC_EXPORT_ADDING_WORKS");
		List<PTCRISyncResult> res = helper.addWorks(localWorks,handler);

		int pad = result.size();
		for (int i = 0; i < res.size(); i++)
			result.put(ORCIDHelper.getActivityLocalKey(localWorks.get(i), BigInteger.valueOf(pad+i)),res.get(i));
		
		handler.done();
		return result;
	}

	/**
	 * <p>
	 * Discovers new valid works in an ORCID profile given a set of known local
	 * CRIS productions. Creates creation notifications for each work group at
	 * ORCID (merged into as a single work by the {@link ORCIDHelper helper})
	 * without matching local productions (i.e., those without shared
	 * {@link ExternalId external identifiers}). To import updates for
	 * works with shared external identifiers
	 * {@link #importUpdates(ORCIDClient, List, ProgressHandler)} should be used
	 * instead.
	 * </p>
	 *
	 * <p>
	 * Currently, these creation notifications simply take the shape of ORCID
	 * works themselves (representing a merged work group). The group merging
	 * selects the meta-data of the preferred work and the external identifiers
	 * of the whole group (see {@link ORCIDHelper#groupToWork(WorkGroup)}). The
	 * selection of the meta-data from a group could be changed without
	 * affecting the correction of the procedure.
	 * </p>
	 *
	 * <p>
	 * Since the put-code attribute is used as a local key of each work, it is
	 * null for these creation notifications (and not the put-code of the remote
	 * ORCID works that gave origin to it). Since only the external identifiers
	 * of the local productions are used to search for matches, the remainder
	 * meta-data of the input local productions could be left null.
	 * </p>
	 *
	 * <p>
	 * ORCID works without minimal quality are ignored by this procedure.
	 * Currently, the quality criteria forces the existence of external
	 * identifiers, the title, publication year and publication type (see
	 * {@link ORCIDHelper#testMinimalQuality(Work)}). Works that do not match the
	 * criteria can be imported with
	 * {@link #importInvalid(ORCIDClient, List, ProgressHandler)}. Note that
	 * currently group merging simply collects the meta-data (other than the
	 * external identifiers) from the preferred work, which is used in the
	 * quality assessment.
	 * </p>
	 * 
	 * <p>
	 * This procedure performs a GET call to the API to obtain the summaries and
	 * an additional GET call for each work identified as valid. The procedure
	 * only fails if the initial GET fails. Asynchronous workers are used for
	 * getting the full works.
	 * </p>
	 *
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param localWorks
	 *            the full list of local productions
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the list of new valid works found in the ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws InterruptedException
	 *             if the asynchronous GET process is interrupted
	 * @throws NullPointerException
	 *             if any of the arguments is null
	 */
	public static List<Work> importWorks(ORCIDClient client, List<Work> localWorks, ProgressHandler handler)
			throws OrcidClientException, InterruptedException, NullPointerException {
		if (client == null || localWorks == null || handler == null)
			throw new NullPointerException("Import works failed.");

		int progress = 0;
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_STARTED");

		ORCIDHelper helper = new ORCIDHelper(client);
		List<WorkSummary> orcidWorks = helper.getAllWorkSummaries();

		Map<BigInteger, PTCRISyncResult> worksToImport = new HashMap<BigInteger, PTCRISyncResult>();

		// filter novel works only
		List<WorkSummary> temp = new ArrayList<WorkSummary>();
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_ITERATION");
		for (int c = 0; c != orcidWorks.size(); c++) {
			progress = (int) ((double) c / orcidWorks.size() * 100);
			handler.setProgress(progress);

			WorkSummary mergedOrcidWork = orcidWorks.get(c);
			Map<Work, ExternalIdsDiff> matchingWorks = ORCIDHelper.getExternalIdsDiff(mergedOrcidWork, localWorks);
			if (matchingWorks.isEmpty() && ORCIDHelper.testMinimalQuality(mergedOrcidWork).isEmpty()) {
				temp.add(mergedOrcidWork);
			}
		}

		helper.getFullWorks(temp, worksToImport);

		helper.waitWorkers();

		List<Work> results = new ArrayList<Work>();
		for (PTCRISyncResult r : worksToImport.values())
			if (r.act != null)
				results.add(r.act);
			else {
				// TODO: r instanceof OrcidClientException
				// meaning that the GET of a particular work failed
			}

		handler.done();
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_FINISHED");
		return new LinkedList<Work>(results);
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
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param localWorks
	 *            the full list of local productions
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the number of new valid works found in the ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws NullPointerException
	 *             if any of the arguments is null
	 */
	public static Integer importCounter(ORCIDClient client, List<Work> localWorks, ProgressHandler handler)
			throws OrcidClientException, NullPointerException {
		if (client == null || localWorks == null || handler == null)
			throw new NullPointerException("Import counter failed.");

		int progress = 0;
		handler.setProgress(progress);
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_STARTED");

		ORCIDHelper helper = new ORCIDHelper(client);
		List<WorkSummary> orcidWorks = helper.getAllWorkSummaries();

		int counter = 0;

		// filter novel works only
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_WORKS_ITERATION");
		for (int c = 0; c != orcidWorks.size(); c++) {
			progress = (int) ((double) c / orcidWorks.size() * 100);
			handler.setProgress(progress);

			WorkSummary mergedOrcidWork = orcidWorks.get(c);
			Map<Work, ExternalIdsDiff> matchingWorks = ORCIDHelper.getExternalIdsDiff(mergedOrcidWork, localWorks);
			if (matchingWorks.isEmpty() && ORCIDHelper.testMinimalQuality(mergedOrcidWork).isEmpty()) {
				counter++;
			}
		}

		handler.done();
		return counter;
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
	 * an additional GET call for each work identified as invalid. The procedure
	 * only fails if the initial GET fails. Asynchronous workers are used for
	 * getting the full works.
	 * </p>
	 *
	 * @see #importWorks(ORCIDClient, List, ProgressHandler)
	 *
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param localWorks
	 *            the full list of local productions
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the list of new invalid works found in the ORCID profile, as well
	 *         as the reasons for invalidity
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws InterruptedException
	 *             if the asynchronous GET process is interrupted
	 * @throws NullPointerException
	 *             if any of the arguments is null
	 */
	public static Map<Work, Set<String>> importInvalid(ORCIDClient client, List<Work> localWorks, ProgressHandler handler)
			throws OrcidClientException, InterruptedException, NullPointerException {
		if (client == null || localWorks == null || handler == null)
			throw new NullPointerException("Import invalid failed.");
	
		int progress = 0;
		handler.setProgress(progress);
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_INVALID_STARTED");
	
		ORCIDHelper helper = new ORCIDHelper(client);
		List<WorkSummary> orcidWorks = helper.getAllWorkSummaries();
	
		Map<BigInteger, Set<String>> invalidsToImport = new HashMap<BigInteger, Set<String>>();
		Map<BigInteger, PTCRISyncResult> worksToImport = new HashMap<BigInteger, PTCRISyncResult>();
	
		// filter invalid works only
		List<WorkSummary> temp = new ArrayList<WorkSummary>();
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_INVALID_ITERATION");
		for (int c = 0; c != orcidWorks.size(); c++) {
			progress = (int) ((double) c / orcidWorks.size() * 100);
			handler.setProgress(progress);
	
			WorkSummary mergedOrcidWork = orcidWorks.get(c);
			Map<Work, ExternalIdsDiff> matchingWorks = ORCIDHelper.getExternalIdsDiff(mergedOrcidWork, localWorks);
			Set<String> invalids = ORCIDHelper.testMinimalQuality(mergedOrcidWork);
			invalidsToImport.put(mergedOrcidWork.getPutCode(), invalids);
			if (matchingWorks.isEmpty() && !invalids.isEmpty()) {
				temp.add(mergedOrcidWork);
			}
		}
	
		helper.getFullWorks(temp, worksToImport);

		helper.waitWorkers();
	
		Map<Work, Set<String>> results = new HashMap<Work, Set<String>>();
		for (BigInteger i : worksToImport.keySet())
			if (worksToImport.get(i).act != null)
				results.put(worksToImport.get(i).act, invalidsToImport.get(i));
			else {
				// TODO: r instanceof OrcidClientException
				// meaning that the GET of a particular work failed
			}
	
		handler.done();
		return results;
	}

	/**
	 * <p>
	 * Discovers updates to existing local CRIS productions in an ORCID profile.
	 * For each work group at ORCID (merged into as a single work by the
	 * {@link ORCIDHelper helper}), finds matching local productions (i.e.,
	 * those with shared {@link ExternalId external identifiers}) and
	 * creates update notifications if not already up to date. To import works
	 * without shared external identifiers,
	 * {@link #importWorks(ORCIDClient, List, ProgressHandler)} should be used
	 * instead.
	 * </p>
	 *
	 * <p>
	 * Currently, these update notifications simply take the shape of ORCID
	 * works themselves (representing a matching work group). These works
	 * contain only the meta-data that needs to be updated locally. Currently,
	 * only the introduction of newly found external identifiers is considered
	 * (i.e., those that were already present in the local productions that is
	 * being updated are removed from the returned updates). Thus, the remainder
	 * fields are returned null. Since only external identifiers are considered
	 * the quality criteria is not enforced on the remote ORCID works.
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
	 * This means that the returned works representing the updates have the
	 * put-code of the local production that is to be updated (and not the put-code of
	 * the ORCID works that gave origin to it).
	 * </p>
	 *
	 * <p>
	 * This procedure simply performs a GET call to the API to obtain the
	 * summaries, since the remainder meta-data is irrelevant.
	 * </p>
	 *
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param localWorks
	 *            the list of local productions for which we wish to discover
	 *            updates, i.e., keep synchronized with ORCID
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the list of updates found in the ORCID profile, pointing to the
	 *         respective local production
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws NullPointerException
	 *             if any of the arguments is null
	 */
	public static List<Work> importUpdates(ORCIDClient client, List<Work> localWorks, ProgressHandler handler)
			throws OrcidClientException {
		if (client == null || localWorks == null || handler == null)
			throw new NullPointerException("Import updates failed.");

		int progress = 0;
		handler.setProgress(progress);
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_UPDATES_STARTED");

		ORCIDHelper helper = new ORCIDHelper(client);
		List<WorkSummary> orcidWorks = helper.getAllWorkSummaries();

		List<Work> worksToUpdate = new LinkedList<Work>();

		// filter already known works only
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_UPDATES_ITERATION");
		for (int c = 0; c != orcidWorks.size(); c++) {
			progress = (int) ((double) c / orcidWorks.size() * 100);
			handler.setProgress(progress);

			WorkSummary orcidWork = orcidWorks.get(c);
			Map<Work, ExternalIdsDiff> matchingLocalWorks = ORCIDHelper.getExternalIdsDiff(orcidWork, localWorks);
			if (!matchingLocalWorks.isEmpty()) {
				for (Work mathingLocalWork : matchingLocalWorks.keySet()) {
					if (!ORCIDHelper.hasNewIDs(mathingLocalWork, orcidWork)) {
						Work workUpdate = ORCIDHelper.clone(mathingLocalWork);
						ExternalIds weids = new ExternalIds();
						List<ExternalId> neids = new ArrayList<ExternalId>(matchingLocalWorks.get(mathingLocalWork).more);
						weids.setExternalId(neids);
						ORCIDHelper.setWorkLocalKey(workUpdate, ORCIDHelper.getActivityLocalKey(mathingLocalWork));
						workUpdate.setExternalIds(weids);
						workUpdate.setTitle(null);
						workUpdate.setType(null);
						workUpdate.setPublicationDate(null);
						worksToUpdate.add(workUpdate);
					}
				}
			}
		}

		handler.done();
		return worksToUpdate;
	}

}
