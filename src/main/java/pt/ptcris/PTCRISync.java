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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.common.ElementSummary;
import org.um.dsi.gavea.orcid.model.common.ExternalId;
import org.um.dsi.gavea.orcid.model.common.ExternalIds;
import org.um.dsi.gavea.orcid.model.funding.Funding;
import org.um.dsi.gavea.orcid.model.funding.FundingType;
import org.um.dsi.gavea.orcid.model.person.externalidentifier.ExternalIdentifier;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkType;

import pt.ptcris.exceptions.InvalidActivityException;
import pt.ptcris.handlers.ProgressHandler;
import pt.ptcris.utils.ExternalIdsDiff;
import pt.ptcris.utils.ORCIDFundingHelper;
import pt.ptcris.utils.ORCIDHelper;
import pt.ptcris.utils.ORCIDWorkHelper;
import pt.ptcris.utils.UpdateRecord;

/**
 * <p>
 * An implementation of the PTCRISync synchronization service based on the
 * version 0.4.3 of the specification, enhanced with minimal quality criteria.
 * This service allows CRIS services to maintain their repositories synchronized
 * with ORCID. This requires the CRIS service to have access to the ORCID Member
 * API.
 * </p>
 *
 * <p>
 * The service has two main functionalities: to keep a set of local activities
 * updated in an ORCID user profile through an
 * {@link #exportBase(ORCIDHelper, List, Collection, boolean, ProgressHandler)
 * export} procedure, and import activities not known locally from the ORCID
 * profile, either through the
 * {@link #importBase(ORCIDHelper, List, Collection, ProgressHandler) import}
 * for completely new activities or through the
 * {@link #importUpdatesBase(ORCIDHelper, List, Collection, ProgressHandler)
 * import} for new information for already known entries. Activities must meet
 * certain quality criteria to be imported (the set of invalid activities can be
 * retrieved as well through
 * {@link #importInvalidBase(ORCIDHelper, List, Collection, ProgressHandler)}.
 * </p>
 *
 * <p>
 * The implementation of the service assumes that the local CRIS communicates
 * the local activities following the established ORCID schema, according to the
 * ORCID Member API 2.0. This uniforms the API and simplifies the
 * synchronization process. The current version focuses on synchronizing
 * research productions and funding entries, which must be encoded as ORCID
 * {@link Work works} and {@link Funding funding} activities.
 * </p>
 *
 * <p>
 * The communication with ORCID is encapsulated in an ORCID {@link ORCIDClient
 * client} that contains information regarding the CRIS Member API and the ORCID
 * profile that is to be managed. An {@link ORCIDHelper helper} provides utility
 * methods for the synchronization of activities.
 * </p>
 * 
 * <p>
 * The procedures do not currently consider the contributors (authors) of activities
 * when assessing the quality criteria nor when assessing whether they are
 * up-to-date, as this information is not available in ORCID summaries and would
 * require additional calls to the ORCID API.
 * </p>
 *
 * See <a
 * href="https://ptcris.pt/hub-ptcris/">https://ptcris.pt/hub-ptcris/</a>.
 */
public final class PTCRISync {
	
	/**
	 * <p>
	 * Exports a list of local CRIS productions to an ORCID profile and keeps
	 * them up-to-date. This procedure manages the work activities in the ORCID
	 * user profile that are sourced by the CRIS, both previously specified in
	 * the {@code client}.
	 * </p>
	 *
	 * <p>
	 * The procedure detects every CRIS sourced work in the ORCID profile that
	 * matches any local entry that is being exported; if there is no matching
	 * local entry, the ORCID activity is deleted from the profile. Otherwise it
	 * will be updated with the meta-data of one of the matching local entries.
	 * Finally, for local entries without any matching ORCID activity, new ones
	 * are created. The matching is performed by detecting shared
	 * {@link ExternalIdentifier external identifiers} (see
	 * {@link ORCIDWorkHelper#getSelfExternalIdsDiffS(ElementSummary, Collection)}
	 * ).
	 * </p>
	 *
	 * <p>
	 * Activities are only updated if the meta-data is not up-to-date.
	 * Currently, the title, the publication year and the publication are
	 * considered (see {@link ORCIDWorkHelper#isUpToDateE(ElementSummary, ElementSummary)}).
	 * </p>
	 *
	 * <p>
	 * The procedure expects the CRIS service to provide the local works in the
	 * ORCID schema, in particular encoding them as {@link Work works}. Thus,
	 * the meta-data of the CRIS sourced entries in the ORCID profile is exactly
	 * that of the provided local entries that are to be exported. The put-code
	 * of these local entries is assumed to be used as local key identifiers,
	 * and are disregarded during the update of the ORCID profile (new entries
	 * are assigned fresh put-codes and updated entries use the put-code of the
	 * existing ORCID entry).
	 * </p>
	 *
	 * <p>
	 * The provided local activities must pass a quality criteria to be kept
	 * synchronized in ORCID. Currently, this forces the existence of external
	 * identifiers, the title, the publication year and the publication type
	 * (see {@link ORCIDWorkHelper#testMinimalQuality(ElementSummary)}.
	 * </p>
	 *
	 * <p>
	 * The procedure reports the status for each of the input local activities,
	 * identifying them by the provided local put-code, including the ORCID
	 * error if the process failed. See {@link PTCRISyncResult} for the codes.
	 * If the local put-code is empty, returns a default value, which currently
	 * is the put-code remotely assigned by ORCID.
	 * </p>
	 *
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param locals
	 *            the list of local productions to be exported that should be
	 *            kept synchronized in the ORCID user profile
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the result of the synchronization of each of the provided local
	 *          work
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	public static Map<BigInteger, PTCRISyncResult<Work>> exportWorks(ORCIDClient client, List<Work> locals, ProgressHandler handler)
			throws OrcidClientException, IllegalArgumentException {
		return exportBase(new ORCIDWorkHelper(client), locals, Arrays.asList(WorkType.values()), false, handler);
	}

	/**
	 * @deprecated Replaced by {@link #exportWorks(ORCIDClient, List, ProgressHandler)}
	 * 
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param locals
	 *            the list of local productions to be exported that should be
	 *            kept synchronized in the ORCID user profile
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the result of the synchronization of each of the provided local
	 *          work
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	@Deprecated
	public static Map<BigInteger, PTCRISyncResult<Work>> export(ORCIDClient client, List<Work> locals, ProgressHandler handler)
			throws OrcidClientException, IllegalArgumentException {
		return exportWorks(client, locals, handler);
	}
	
	/**
	 * <p>
	 * Exports a list of local CRIS productions to an ORCID profile and keeps
	 * them up-to-date. This procedure manages the work activities in the ORCID
	 * user profile that are sourced by the CRIS, both previously specified in
	 * the {@code client}.
	 * </p>
	 *
	 * <p>
	 * The procedure detects every CRIS sourced work in the ORCID profile that
	 * matches any local entry that is being exported; if there is no matching
	 * local entry, the ORCID activity is deleted from the profile. Otherwise it
	 * will be updated with the meta-data of one of the matching local entries.
	 * Finally, for local entries without any matching ORCID activity, new ones
	 * are created. The matching is performed by detecting shared
	 * {@link ExternalIdentifier external identifiers} (see
	 * {@link ORCIDWorkHelper#getSelfExternalIdsDiffS(ElementSummary, Collection)}
	 * ).
	 * </p>
	 *
	 * <p>
	 * Activities are always updated, even if already up-to-date (no test is
	 * performed). The caller of this method should guarantee that the input
	 * local productions have been effectively updated, otherwise there will be
	 * unnecessary calls to the ORCID API.
	 * </p>
	 *
	 * <p>
	 * The procedure expects the CRIS service to provide the local works in the
	 * ORCID schema, in particular encoding them as {@link Work works}. Thus,
	 * the meta-data of the CRIS sourced entries in the ORCID profile is exactly
	 * that of the provided local entries that are to be exported. The put-code
	 * of these local entries is assumed to be used as local key identifiers,
	 * and are disregarded during the update of the ORCID profile (new entries
	 * are assigned fresh put-codes and updated entries use the put-code of the
	 * existing ORCID entry).
	 * </p>
	 *
	 * <p>
	 * The provided local activities must pass a quality criteria to be kept
	 * synchronized in ORCID. Currently, this forces the existence of external
	 * identifiers, the title, the publication year and the publication type
	 * (see {@link ORCIDWorkHelper#testMinimalQuality(ElementSummary)}.
	 * </p>
	 *
	 * <p>
	 * The procedure reports the status for each of the input local activities,
	 * identifying them by the provided local put-code, including the ORCID
	 * error if the process failed. See {@link PTCRISyncResult} for the codes.
	 * If the local put-code is empty, returns a default value, which currently
	 * is the put-code remotely assigned by ORCID.
	 * </p>
	 *
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param locals
	 *            the list of local productions to be exported that should be
	 *            kept synchronized in the ORCID user profile
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the result of the synchronization of each of the provided local
	 *          work
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	public static Map<BigInteger, PTCRISyncResult<Work>> exportWorksForced(ORCIDClient client, List<Work> locals, ProgressHandler handler)
			throws OrcidClientException, IllegalArgumentException {
		return exportBase(new ORCIDWorkHelper(client), locals, Arrays.asList(WorkType.values()), true, handler);
	}
	
	/**
	 * @deprecated Replaced by {@link #exportWorksForced(ORCIDClient, List, ProgressHandler)}
	 * 
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param locals
	 *            the list of local productions to be exported that should be
	 *            kept synchronized in the ORCID user profile
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the result of the synchronization of each of the provided local
	 *          work
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	@Deprecated
	public static Map<BigInteger, PTCRISyncResult<Work>> exportForce(ORCIDClient client, List<Work> locals, ProgressHandler handler)
			throws OrcidClientException, IllegalArgumentException {
		return exportWorksForced(client, locals, handler);
	}

	/**
	 * <p>
	 * Exports a list of local CRIS funding entries to an ORCID profile and
	 * keeps them up-to-date. This procedure manages the funding activities in
	 * the ORCID user profile that are sourced by the CRIS, both previously
	 * specified in the {@code client}.
	 * </p>
	 *
	 * <p>
	 * The procedure detects every CRIS sourced funding activity in the ORCID
	 * profile that matches any local entry that is being exported; if there is
	 * no matching local entry, the ORCID activity is deleted from the profile.
	 * Otherwise it will be updated with the meta-data of one of the matching
	 * local entries. Finally, for local entries without any matching ORCID
	 * activity, new ones are created. The matching is performed by detecting
	 * shared {@link ExternalIdentifier external identifiers} (see
	 * {@link ORCIDFundingHelper#getSelfExternalIdsDiffS(ElementSummary, Collection)}
	 * ).
	 * </p>
	 *
	 * <p>
	 * Activities are only updated if the meta-data is not up-to-date.
	 * Currently, the title, the start year, the funding type and the funding
	 * organization are considered (see
	 * {@link ORCIDFundingHelper#isUpToDateE(ElementSummary, ElementSummary)}).
	 * </p>
	 *
	 * <p>
	 * The procedure expects the CRIS service to provide the local funding
	 * entries in the ORCID schema, in particular encoding them as
	 * {@link Funding funding activities}. Thus, the meta-data of the CRIS
	 * sourced entries in the ORCID profile is exactly that of the provided
	 * local entries that are to be exported. The put-code of these local
	 * entries is assumed to be used as local key identifiers, and are
	 * disregarded during the update of the ORCID profile (new entries are
	 * assigned fresh put-codes and updated entries use the put-code of the
	 * existing ORCID entry).
	 * </p>
	 *
	 * <p>
	 * The provided local activities must pass a quality criteria to be kept
	 * synchronized in ORCID. Currently, this forces the existence of external
	 * identifiers, the title, the start year, the funding type and the funding
	 * organization (see
	 * {@link ORCIDFundingHelper#testMinimalQuality(ElementSummary)}.
	 * </p>
	 * *
	 * <p>
	 * A set of funding types can be provided to allow the independent
	 * synchronization of different types of entries. Local and remote
	 * activities outside the provided types are simply ignored (they are not
	 * considered invalid).
	 * </p>
	 *
	 * <p>
	 * The procedure reports the status for each of the input local activities,
	 * identifying them by the provided local put-code, including the ORCID
	 * error if the process failed. See {@link PTCRISyncResult} for the codes.
	 * If the local put-code is empty, returns a default value, which currently
	 * is the put-code remotely assigned by ORCID.
	 * </p>
	 *
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param locals
	 *            the list of local productions to be exported that should be
	 *            kept synchronized in the ORCID user profile
	 * @param types
	 *            the types of ORCID funding entries that should be considered
	 *            (others are simply ignored).
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the result of the synchronization of each of the provided local
	 *         funding entry
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	public static Map<BigInteger, PTCRISyncResult<Funding>> exportFundings(ORCIDClient client, List<Funding> locals, Collection<FundingType> types, ProgressHandler handler)
			throws OrcidClientException, IllegalArgumentException {
		return exportBase(new ORCIDFundingHelper(client), locals, types, false, handler);
	}

	/**
	 * <p>
	 * Exports a list of local CRIS activities to an ORCID profile and keeps
	 * them up-to-date. This procedure manages the activities in the ORCID user
	 * profile that are sourced by the CRIS, both previously specified in the
	 * {@code client}.
	 * </p>
	 *
	 * <p>
	 * The procedure detects every CRIS sourced activity in the ORCID profile
	 * that matches any local entry that is being exported; if there is no
	 * matching local entry, the ORCID activity is deleted from the profile.
	 * Otherwise it will be updated with the meta-data of one of the matching
	 * local entries. Finally, for local entries without any matching ORCID
	 * activity, new ones are created. The matching is performed by detecting
	 * shared {@link ExternalIdentifier external identifiers} (see
	 * {@link ORCIDHelper#getSelfExternalIdsDiffS(ElementSummary, Collection)}
	 * ).
	 * </p>
	 *
	 * <p>
	 * The procedure expects the CRIS service to provide the local activities in
	 * the ORCID schema, in particular encoding them as {@link ElementSummary
	 * activities}. Thus, the meta-data of the CRIS sourced entries in the ORCID
	 * profile is exactly that of the provided local entries that are to be
	 * exported. The put-code of these local entries is assumed to be used as
	 * local key identifiers, and are disregarded during the update of the ORCID
	 * profile (new entries are assigned fresh put-codes and updated entries use
	 * the put-code of the existing ORCID entry).
	 * </p>
	 *
	 * <p>
	 * The provided local activities must pass a quality criteria to be kept
	 * synchronized in ORCID (see
	 * {@link ORCIDHelper#testMinimalQuality(ElementSummary)}.
	 * </p>
	 *
	 * <p>
	 * A set of funding types can be provided to allow the independent
	 * synchronization of different types of entries. Local and remote
	 * activities outside the provided types are simply ignored (they are not
	 * considered invalid).
	 * </p>
	 *
	 * <p>
	 * The procedure reports the status for each of the input local activities,
	 * identifying them by the provided local put-code, including the ORCID
	 * error if the process failed. See {@link PTCRISyncResult} for the codes.
	 * If the local put-code is empty, returns a default value, which currently
	 * is the put-code remotely assigned by ORCID.
	 * </p>
	 * *
	 * <p>
	 * Unless {@code forced}, the ORCID activities are only updated if the
	 * meta-data is not up-to-date (see
	 * {@link ORCIDHelper#isUpToDateE(ElementSummary, ElementSummary)}).
	 * </p>
	 *
	 * <p>
	 * The update stage must be two-phased in order to avoid potential
	 * conflicts: the first phase removes external identifiers that are obsolete
	 * from the CRIS sourced activities, so that there are no conflicts with the
	 * new ones inserted in the second phase.
	 * </p>
	 *
	 * <p>
	 * This procedure performs a single GET call to the API to obtain the
	 * summaries and PUT or POST calls for each of the local input activities.
	 * Bulk POST requests are performed when supported. Additionally, DELETE
	 * calls can also be performed. The procedure only fails if the initial GET
	 * fails, otherwise individual failures are reported in the output. No
	 * asynchronous workers are used.
	 * </p>
	 * 
	 * @param helper
	 *            helper that encapsulates the ORCID client defining the CRIS
	 *            Member API and user the profile to be managed
	 * @param locals
	 *            the list of local activities to be exported that should be
	 *            kept synchronized in the ORCID user profile
	 * @param types
	 *            the types of ORCID activities that should be considered
	 *            (others are simply ignored).
	 * @param forced
	 *            whether the update of ORCID activities should be forced,
	 *            without testing if up-to-date
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the result of the synchronization of each of the provided local
	 *         activity
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	private static <E extends ElementSummary, S extends ElementSummary, G, T extends Enum<T>> Map<BigInteger, PTCRISyncResult<E>> exportBase(
			ORCIDHelper<E, S, G, T> helper, List<E> locals,
			Collection<T> types, boolean forced, ProgressHandler handler)
			throws OrcidClientException, IllegalArgumentException {
		
		if (helper == null || locals == null || handler == null)
			throw new IllegalArgumentException("Null arguments.");
		
		if (types == null)
			types = new HashSet<T>();
		
		handler.setCurrentStatus("ORCID_SYNC_EXPORT_STARTED");

		List<S> orcids = helper.getSourcedSummaries();

		Map<BigInteger, PTCRISyncResult<E>> result = new HashMap<BigInteger, PTCRISyncResult<E>>();

		// start by filtering local works that do not pass the quality criteria
		handler.setCurrentStatus("ORCID_SYNC_EXPORT_QUALITY",locals.size());
		Set<E> invalids = new HashSet<E>();
		for (int c = 0; c != locals.size(); c++) {
			E local = locals.get(c);

			if (!types.contains(helper.getTypeE(local))) {
				invalids.add(local);
			} else {
				try {
					helper.tryMinimalQualityE(local, locals);
				} catch (InvalidActivityException invalid) {
					invalids.add(local);
					result.put(ORCIDHelper.getActivityLocalKey(local,
							BigInteger.valueOf(c)), PTCRISyncResult
							.<E>invalid(invalid));
				}
			}
			handler.step();
		}
		locals.removeAll(invalids);

		// detect which remote works should be deleted or updated
		handler.setCurrentStatus("ORCID_SYNC_EXPORT_ITERATION",orcids.size());
		List<UpdateRecord<E,S>> toUpdate = new LinkedList<UpdateRecord<E,S>>();
		for (int c = 0; c != orcids.size(); c++) {
			S orcid = orcids.get(c);

			Map<E, ExternalIdsDiff> worksDiffs = helper.getSelfExternalIdsDiffS(orcid, locals);
			// there is no local work matching a CRIS sourced remote work
			if (worksDiffs.isEmpty()) {
				// TODO: the delete may fail (the result is returned); how to communicate this to the caller?
				helper.delete(orcid.getPutCode());
			}
			// there is at least one local work matching a CRIS sourced remote work
			else {
				E local = worksDiffs.keySet().iterator().next();
				// if the remote work is not up-to-date or forced updates
				if (forced || !helper.isUpToDateS(local, orcid))
					toUpdate.add(new UpdateRecord<E,S>(local, orcid, worksDiffs.get(local)));
				else
					result.put(ORCIDHelper.getActivityLocalKey(local, BigInteger.valueOf(c)),
							PTCRISyncResult.<E>uptodate());
				locals.remove(local);
			}
			handler.step();
		}

		// first update phase, remove spurious identifiers
		handler.setCurrentStatus("ORCID_SYNC_EXPORT_UPDATING_PHASE_1",toUpdate.size());
		for (int c = 0; c != toUpdate.size(); c++) {

			UpdateRecord<E,S> update = toUpdate.get(c);
			// the remote work has spurious external identifiers
			if (!update.eidsDiff.more.isEmpty()) {
				E local = update.preElement;
				ExternalIds weids = new ExternalIds();
				List<ExternalId> ids = new ArrayList<ExternalId>(update.eidsDiff.same);
				ids.addAll(helper.getPartOfExternalIdsE(local).getExternalId());
				weids.setExternalId(ids);
				helper.setExternalIdsE(local,weids);

				PTCRISyncResult<E> res = helper.update(update.posElement.getPutCode(), local);
				result.put(ORCIDHelper.getActivityLocalKey(local, BigInteger.valueOf(c)),res);
			}
			handler.step();
		}

		// second update phase, add missing identifiers
		handler.setCurrentStatus("ORCID_SYNC_EXPORT_UPDATING_PHASE_2",toUpdate.size());
		for (int c = 0; c != toUpdate.size(); c++) {

			// the remote work is missing external identifiers or not updated in the 1st phase
			UpdateRecord<E,S> update = toUpdate.get(c);
			if (!update.eidsDiff.less.isEmpty() || update.eidsDiff.more.isEmpty()) {
				E local = update.preElement;
				ExternalIds weids = new ExternalIds();
				List<ExternalId> ids = new ArrayList<ExternalId>(update.eidsDiff.same);
				ids.addAll(update.eidsDiff.less);
				ids.addAll(helper.getPartOfExternalIdsE(local).getExternalId());
				weids.setExternalId(ids);
				helper.setExternalIdsE(local,weids);

				PTCRISyncResult<E> res = helper.update(update.posElement.getPutCode(), local);
				result.put(ORCIDHelper.getActivityLocalKey(local, BigInteger.valueOf(c)),res);
			}
			handler.step();
		}
		
		// add the local works that had no match
		// the progress handler must be moved to the helper due to bulk additions
		handler.setCurrentStatus("ORCID_SYNC_EXPORT_ADDING",locals.size());
		List<PTCRISyncResult<E>> res = helper.add(locals,handler);

		int pad = result.size();
		for (int i = 0; i < res.size(); i++)
			result.put(ORCIDHelper.getActivityLocalKey(locals.get(i), BigInteger.valueOf(pad+i)),res.get(i));
		
		handler.done();
		return result;
	}

	/**
	 * <p>
	 * Discovers new valid works in an ORCID profile given a set of known local
	 * CRIS productions. Creates creation notifications for each work group at
	 * ORCID (merged into as a single work by the {@link ORCIDWorkHelper helper}
	 * ) without matching local productions (i.e., those without shared
	 * {@link ExternalId external identifiers}). To import updates for works
	 * with shared external identifiers
	 * {@link #importWorkUpdates(ORCIDClient, List, ProgressHandler)} should be
	 * used instead.
	 * </p>
	 *
	 * <p>
	 * Currently, these creation notifications simply take the shape of ORCID
	 * works themselves (representing a merged work group). The group merging
	 * selects the meta-data of the preferred activity and the external
	 * identifiers of the whole group (see
	 * {@link ORCIDWorkHelper#group(Object)}). The selection of the
	 * meta-data from a group could be changed without affecting the correction
	 * of the procedure.
	 * </p>
	 *
	 * <p>
	 * Since the put-code attribute is used as a local key of each activity, it
	 * is null for these creation notifications (and not the put-code of the
	 * remote ORCID activity that gave origin to it). Since only the external
	 * identifiers of the local productions are used to search for matches, the
	 * remainder meta-data of the input local activities could be left null.
	 * </p>
	 *
	 * <p>
	 * ORCID activities without minimal quality are ignored by this procedure.
	 * Currently, the quality criteria forces the existence of external
	 * identifiers, the title, publication year and publication type (see
	 * {@link ORCIDWorkHelper#testMinimalQuality(ElementSummary)}). Works that do
	 * not match the criteria can be imported with
	 * {@link #importInvalid(ORCIDClient, List, ProgressHandler)}. Note that
	 * currently group merging simply collects the meta-data (other than the
	 * external identifiers) from the preferred activity, which is used in the
	 * quality assessment.
	 * </p>
	 *
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param locals
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
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	public static List<Work> importWorks(ORCIDClient client, List<Work> locals, ProgressHandler handler)
			throws OrcidClientException, InterruptedException, IllegalArgumentException {
		return importBase(new ORCIDWorkHelper(client), locals, Arrays.asList(WorkType.values()), handler);
	}

	/**
	 * <p>
	 * Discovers new valid funding activities in an ORCID profile given a set of
	 * known local CRIS funding entries. Creates creation notifications for each
	 * funding group at ORCID (merged into as a single funding entry by the
	 * {@link ORCIDFundingHelper helper} ) without matching local entries (i.e.,
	 * those without shared {@link ExternalId external identifiers}). To import
	 * updates for funding activities with shared external identifiers
	 * {@link #importFundingUpdates(ORCIDClient, List, Collection, ProgressHandler)}
	 * should be used instead.
	 * </p>
	 *
	 * <p>
	 * Currently, these creation notifications simply take the shape of ORCID
	 * funding activities themselves (representing a merged activity group). The
	 * group merging selects the meta-data of the preferred activity and the
	 * external identifiers of the whole group (see
	 * {@link ORCIDFundingHelper#group(Object)}). The selection of the meta-data
	 * from a group could be changed without affecting the correction of the
	 * procedure.
	 * </p>
	 *
	 * <p>
	 * Since the put-code attribute is used as a local key of each activity, it
	 * is null for these creation notifications (and not the put-code of the
	 * remote ORCID activity that gave origin to it). Since only the external
	 * identifiers of the local funding entries are used to search for matches,
	 * the remainder meta-data of the input local activities could be left null.
	 * </p>
	 *
	 * <p>
	 * ORCID activities without minimal quality are ignored by this procedure.
	 * Currently, the quality criteria forces the existence of external
	 * identifiers, the title, publication year and publication type (see
	 * {@link ORCIDFundingHelper#testMinimalQuality(ElementSummary)}). Funding
	 * entries that do not match the criteria can be imported with
	 * {@link #importInvalid(ORCIDClient, List, ProgressHandler)}. Note that
	 * currently group merging simply collects the meta-data (other than the
	 * external identifiers) from the preferred activity, which is used in the
	 * quality assessment.
	 * </p>
	 *
	 * <p>
	 * A set of funding types can be provided to allow the independent
	 * synchronization of different types of entries. Local and remote
	 * activities outside the provided types are simply ignored (they are not
	 * considered invalid).
	 * </p>
	 * *
	 * 
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param locals
	 *            the full list of local funding entries
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @param types
	 *            the types of ORCID funding activities that should be
	 *            considered (others are simply ignored)
	 * @return the list of new valid funding activities found in the ORCID
	 *         profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws InterruptedException
	 *             if the asynchronous GET process is interrupted
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	public static List<Funding> importFundings(ORCIDClient client, List<Funding> locals, Collection<FundingType> types, ProgressHandler handler)
			throws OrcidClientException, InterruptedException, IllegalArgumentException {
		return importBase(new ORCIDFundingHelper(client), locals, types, handler);
	}

	/**
	 * <p>
	 * Discovers new valid activities in an ORCID profile given a set of known
	 * local CRIS entries. Creates creation notifications for each activity
	 * group at ORCID (merged into as a single entry by the {@link ORCIDHelper
	 * helper} ) without matching local entries (i.e., those without shared
	 * {@link ExternalId external identifiers}). To import updates for
	 * activities with shared external identifiers
	 * {@link #importBase(ORCIDHelper, List, Collection, ProgressHandler)}
	 * should be used instead.
	 * </p>
	 *
	 * <p>
	 * Currently, these creation notifications simply take the shape of ORCID
	 * activities themselves (representing a merged activity group). The group
	 * merging selects the meta-data of the preferred activity and the external
	 * identifiers of the whole group (see
	 * {@link ORCIDHelper#group(ElementSummary)}). The selection of the
	 * meta-data from a group could be changed without affecting the correction
	 * of the procedure.
	 * </p>
	 *
	 * <p>
	 * Since the put-code attribute is used as a local key of each activity, it
	 * is null for these creation notifications (and not the put-code of the
	 * remote ORCID activity that gave origin to it). Since only the external
	 * identifiers of the local activities are used to search for matches, the
	 * remainder meta-data of the input local activities could be left null.
	 * </p>
	 *
	 * <p>
	 * ORCID activities without minimal quality are ignored by this procedure.
	 * Activities that do not match the criteria can be imported with
	 * {@link #importInvalid(ORCIDClient, List, ProgressHandler)}. Note that
	 * currently group merging simply collects the meta-data (other than the
	 * external identifiers) from the preferred activity, which is used in the
	 * quality assessment.
	 * </p>
	 * 
	 * <p>
	 * A set of activity types can be provided to allow the independent
	 * synchronization of different types of entries. Local and remote
	 * activities outside the provided types are simply ignored (they are not
	 * considered invalid).
	 * </p>
	 * 
	 * <p>
	 * This procedure performs a GET call to the API to obtain the summaries and
	 * an additional GET call for each activity identified as valid. Bulk GET
	 * requests are performed when supported. The procedure only fails if the
	 * initial GET fails. Asynchronous workers are used for getting the full
	 * activities.
	 * </p>
	 *
	 * @param helper
	 *            helper that encapsulates the ORCID client defining the CRIS
	 *            Member API and user the profile to be managed
	 * @param locals
	 *            the full list of local activities
	 * @param types
	 *            the types of ORCID activities that should be considered
	 *            (others are simply ignored)
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the list of new valid activities found in the ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws InterruptedException
	 *             if the asynchronous GET process is interrupted
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	private static <E extends ElementSummary, S extends ElementSummary, G, T extends Enum<T>> List<E> importBase(
			ORCIDHelper<E, S, G, T> helper, List<E> locals,
			Collection<T> types, ProgressHandler handler)
			throws OrcidClientException, InterruptedException, IllegalArgumentException {

		if (helper == null || locals == null || handler == null)
			throw new IllegalArgumentException("Null arguments.");
		
		if (types == null)
			types = new HashSet<T>();
		
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_STARTED");

		List<S> orcids = helper.getAllTypedSummaries(types);

		Map<BigInteger, PTCRISyncResult<E>> toImport = new HashMap<BigInteger, PTCRISyncResult<E>>();

		// filter novel works only
		List<S> temp = new ArrayList<S>();
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_ITERATION",orcids.size());
		for (int c = 0; c != orcids.size(); c++) {
			S mergedOrcidWork = orcids.get(c);
			Map<E, ExternalIdsDiff> matchingWorks = helper.getSelfExternalIdsDiffS(mergedOrcidWork, locals);
			if (matchingWorks.isEmpty() && helper.testMinimalQuality(mergedOrcidWork).isEmpty()) {
				temp.add(mergedOrcidWork);
			}
			handler.step();
		}

		handler.setCurrentStatus("ORCID_SYNC_IMPORT_GETTING",temp.size());
		helper.getFulls(temp, toImport, handler);

		List<E> results = new ArrayList<E>();
		for (PTCRISyncResult<E> r : toImport.values())
			if (r.act != null)
				results.add(r.act);
			else {
				// TODO: r instanceof OrcidClientException
				// meaning that the GET of a particular work failed
			}

		handler.done();
		return new LinkedList<E>(results);
	}

	/**
	 * <p>
	 * Counts new valid works in an ORCID profile given a set of
	 * known local CRIS productions, following the criteria of
	 * {@link #importWorks(ORCIDClient, List, ProgressHandler)}
	 * but is more efficient, generating less API calls.
	 * </p>
	 *
	 * @see #importWorks(ORCIDClient, List, ProgressHandler)
	 *
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param locals
	 *            the full list of local productions
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the number of new valid works found in the ORCID
	 *         profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	public static Integer importWorkCounter(ORCIDClient client, List<Work> locals, ProgressHandler handler)
			throws OrcidClientException, IllegalArgumentException {
		return importCounterBase(new ORCIDWorkHelper(client), locals, Arrays.asList(WorkType.values()), handler);
	}

	/**
	 * @deprecated Replaced by
	 *             {@link #importWorkCounter(ORCIDClient, List, ProgressHandler)}
	 * 
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param locals
	 *            the full list of local productions
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the number of new valid works found in the ORCID
	 *         profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	@Deprecated
	public static Integer importCounter(ORCIDClient client, List<Work> locals, ProgressHandler handler)
			throws OrcidClientException, IllegalArgumentException {
		return importWorkCounter(client, locals, handler);
	}

	/**
	 * <p>
	 * Counts new valid funding activities in an ORCID profile given a set of
	 * known local CRIS funding entries, following the criteria of
	 * {@link #importFundings(ORCIDClient, List, Collection, ProgressHandler)}
	 * but is more efficient, generating less API calls.
	 * </p>
	 *
	 * @see #importFundings(ORCIDClient, List, Collection, ProgressHandler)
	 *
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param locals
	 *            the full list of local funding entries
	 * @param types
	 *            the types of ORCID funding entries that should be considered
	 *            (others are simply ignored)
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the number of new valid funding activities found in the ORCID
	 *         profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	public static Integer importFundingCounter(ORCIDClient client, List<Funding> locals, Collection<FundingType> types, ProgressHandler handler)
			throws OrcidClientException, IllegalArgumentException {
		return importCounterBase(new ORCIDFundingHelper(client), locals, types, handler);
	}

	/**
	 * <p>
	 * Counts new valid activities in an ORCID profile given a set of known
	 * local CRIS entries, following the criteria of
	 * {@link #importBase(ORCIDHelper, List, Collection, ProgressHandler)}.
	 * </p>
	 *
	 * <p>
	 * This procedure simply performs a GET call to the API to obtain the
	 * summaries, since the remainder meta-data is irrelevant, rendering it more
	 * efficient than
	 * {@link #importBase(ORCIDHelper, List, Collection, ProgressHandler)}.
	 * </p>
	 *
	 * @see #importBase(ORCIDHelper, List, Collection, ProgressHandler)
	 *
	 * @param helper
	 *            helper that encapsulates the ORCID client defining the CRIS
	 *            Member API and user the profile to be managed
	 * @param locals
	 *            the full list of local entries
	 * @param types
	 *            the types of ORCID activities that should be considered
	 *            (others are simply ignored)
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the number of new valid activities found in the ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	private static <E extends ElementSummary, S extends ElementSummary, G, T extends Enum<T>> Integer importCounterBase(
			ORCIDHelper<E, S, G, T> helper, List<E> locals,
			Collection<T> types, ProgressHandler handler)
			throws OrcidClientException, IllegalArgumentException {

		if (helper == null || locals == null || handler == null)
			throw new IllegalArgumentException("Null arguments.");
		
		if (types == null)
			types = new HashSet<T>();
		
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_COUNTER_STARTED");

		List<S> orcids = helper.getAllTypedSummaries(types);

		int counter = 0;

		// filter novel works only
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_COUNTER_ITERATION",orcids.size());
		for (int c = 0; c != orcids.size(); c++) {
			S mergedOrcidWork = orcids.get(c);
			Map<E, ExternalIdsDiff> matchingWorks = helper.getSelfExternalIdsDiffS(mergedOrcidWork, locals);
			if (matchingWorks.isEmpty() && helper.testMinimalQuality(mergedOrcidWork).isEmpty()) {
				counter++;
			}
			handler.step();
		}

		handler.done();
		return counter;
	}

	/**
	 * <p>
	 * Discovers new invalid works (that do not pass the quality criteria, see
	 * {@link ORCIDWorkHelper#testMinimalQuality(ElementSummary)}) in an ORCID
	 * profile given a set of known local CRIS entries, as well as the causes
	 * for invalidity (defined at {@link ORCIDWorkHelper}). Other than the
	 * criteria, the behavior is similar to that of
	 * {@link #importWorks(ORCIDClient, List, ProgressHandler)}.
	 * Note that currently group merging simply collects the meta-data (other
	 * than the external identifiers) from the preferred activity, which is used
	 * in the quality assessment.
	 * </p>
	 *
	 * @see #importWorks(ORCIDClient, List, ProgressHandler)
	 *
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param locals
	 *            the full list of local works
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the list of new invalid works found in the ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws InterruptedException
	 *             if the asynchronous GET process is interrupted
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	public static Map<Work, Set<String>> importInvalidWorks(ORCIDClient client,
			List<Work> locals, ProgressHandler handler)
			throws OrcidClientException, InterruptedException, IllegalArgumentException {
		return importInvalidBase(new ORCIDWorkHelper(client), locals,
				Arrays.asList(WorkType.values()), handler);
	}

	/**
	 * @deprecated Replaced by
	 *             {@link #importInvalidWorks(ORCIDClient, List, ProgressHandler)}
	 * 
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param locals
	 *            the full list of local works
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the list of new invalid works found in the ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws InterruptedException
	 *             if the asynchronous GET process is interrupted
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	@Deprecated
	public static Map<Work, Set<String>> importInvalid(ORCIDClient client,
			List<Work> locals, ProgressHandler handler)
			throws OrcidClientException, InterruptedException, IllegalArgumentException {
		return importInvalidWorks(client, locals, handler);
	}

	/**
	 * <p>
	 * Discovers new invalid funding activities (that do not pass the quality
	 * criteria, see
	 * {@link ORCIDFundingHelper#testMinimalQuality(ElementSummary)}) in an
	 * ORCID profile given a set of known local CRIS entries, as well as the
	 * causes for invalidity (defined at {@link ORCIDFundingHelper}). Other than
	 * the criteria, the behavior is similar to that of
	 * {@link #importFundings(ORCIDClient, List, Collection, ProgressHandler)}.
	 * Note that currently group merging simply collects the meta-data (other
	 * than the external identifiers) from the preferred activity, which is used
	 * in the quality assessment.
	 * </p>
	 *
	 * @see #importFundings(ORCIDClient, List, Collection, ProgressHandler)
	 *
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param locals
	 *            the full list of local funding activities
	 * @param types
	 *            the types of ORCID funding activities that should be
	 *            considered (others are simply ignored)
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the list of new invalid funding activities found in the ORCID
	 *         profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws InterruptedException
	 *             if the asynchronous GET process is interrupted
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	public static Map<Funding, Set<String>> importInvalidFundings(
			ORCIDClient client, List<Funding> locals,
			Collection<FundingType> types, ProgressHandler handler)
			throws OrcidClientException, InterruptedException, IllegalArgumentException {
		return importInvalidBase(new ORCIDFundingHelper(client), locals, types,
				handler);
	}

	/**
	 * <p>
	 * Discovers new invalid activities (that do not pass the quality criteria,
	 * see {@link ORCIDHelper#testMinimalQuality(ElementSummary)}) in an ORCID
	 * profile given a set of known local CRIS entries, as well as the causes
	 * for invalidity (defined at {@link ORCIDHelper}). Other than the criteria,
	 * the behavior is similar to that of
	 * {@link #importBase(ORCIDHelper, List, Collection, ProgressHandler)}. Note
	 * that currently group merging simply collects the meta-data (other than
	 * the external identifiers) from the preferred activity, which is used in
	 * the quality assessment.
	 * </p>
	 *
	 * <p>
	 * This procedure performs a GET call to the API to obtain the summaries and
	 * an additional GET call for each activity identified as invalid. Bulk GET
	 * requests are performed when supported. The procedure only fails if the
	 * initial GET fails. Asynchronous workers are used for getting the full
	 * activities.
	 * </p>
	 * 
	 * @see #importBase(ORCIDHelper, List, Collection, ProgressHandler)
	 *
	 * @param helper
	 *            helper that encapsulates the ORCID client defining the CRIS
	 *            Member API and user the profile to be managed
	 * @param locals
	 *            the full list of local activities
	 * @param types
	 *            the types of ORCID activities that should be considered
	 *            (others are simply ignored)
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the list of new invalid activities found in the ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws InterruptedException
	 *             if the asynchronous GET process is interrupted
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	private static <E extends ElementSummary, S extends ElementSummary, G, T extends Enum<T>> Map<E, Set<String>> importInvalidBase(
			ORCIDHelper<E, S, G, T> helper, List<E> locals,
			Collection<T> types, ProgressHandler handler)
			throws OrcidClientException, InterruptedException, IllegalArgumentException {
	
		if (helper == null || locals == null || handler == null)
			throw new IllegalArgumentException("Null arguments.");
		
		if (types == null)
			types = new HashSet<T>();
		
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_INVALID_STARTED");
	
		List<S> orcids = helper.getAllTypedSummaries(types);
	
		Map<BigInteger, Set<String>> invalidsToImport = new HashMap<BigInteger, Set<String>>();
		Map<BigInteger, PTCRISyncResult<E>> toImport = new HashMap<BigInteger, PTCRISyncResult<E>>();
	
		// filter invalid works only
		List<S> temp = new ArrayList<S>();
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_INVALID_ITERATION",orcids.size());
		for (int c = 0; c != orcids.size(); c++) {
			S mergedOrcidWork = orcids.get(c);
			Map<E, ExternalIdsDiff> matchingWorks = helper.getSelfExternalIdsDiffS(mergedOrcidWork, locals);
			Set<String> invalids = helper.testMinimalQuality(mergedOrcidWork);
			invalidsToImport.put(mergedOrcidWork.getPutCode(), invalids);
			if (matchingWorks.isEmpty() && !invalids.isEmpty()) {
				temp.add(mergedOrcidWork);
			}
			handler.step();
		}
	
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_INVALID_GETTING",temp.size());
		helper.getFulls(temp, toImport, handler);
	
		Map<E, Set<String>> results = new HashMap<E, Set<String>>();
		for (BigInteger i : toImport.keySet())
			if (toImport.get(i).act != null)
				results.put(toImport.get(i).act, invalidsToImport.get(i));
			else {
				// TODO: r instanceof OrcidClientException
				// meaning that the GET of a particular work failed
			}
	
		handler.done();
		return results;
	}
	
	/**
	 * <p>
	 * Discovers updates to existing local CRIS productions in an ORCID
	 * profile. For each work group at ORCID (merged into as a
	 * single activity by the {@link ORCIDWorkHelper helper}), finds matching
	 * local entries (i.e., those with shared {@link ExternalId external
	 * identifiers}) and creates update notifications if not already up to date.
	 * To import works without shared external identifiers,
	 * {@link #importWorks(ORCIDClient, List, ProgressHandler)}
	 * should be used instead.
	 * </p>
	 *
	 * <p>
	 * These update notifications simply take the shape of ORCID activities
	 * themselves (representing a matching activity group). These works contain
	 * only the meta-data that needs to be updated locally. Currently, only the
	 * introduction of newly found external identifiers is considered (i.e.,
	 * those that were already present in the local entry that is being updated
	 * are removed from the returned update). Thus, the remainder fields are
	 * returned null. Since only external identifiers are considered the quality
	 * criteria is not enforced on the remote ORCID activities.
	 * </p>
	 *
	 * <p>
	 * The local entries are tested to be up-to-date by simply checking whether
	 * they contain every external identifiers in the ORCID group (see
	 * {@link ORCIDHelper#hasNewSelfIDs(ElementSummary, ElementSummary)}. Thus
	 * the remainder meta-data of the local entries can be currently left null.
	 * </p>
	 * 
	 * <p>
	 * The put-code attribute is used as a local key of each CRIS production.
	 * This means that the returned activities representing the updates have the
	 * put-code of the local entry that is to be updated (and not the put-code
	 * of the ORCID activity that gave origin to it).
	 * </p>
	 *
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param locals
	 *            the full list of local productions
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the list of work updates found in the ORCID profile, pointing
	 *         to the respective local activity
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	public static List<Work> importWorkUpdates(ORCIDClient client, List<Work> locals, ProgressHandler handler)
			throws OrcidClientException, IllegalArgumentException {
		return importUpdatesBase(new ORCIDWorkHelper(client), locals, Arrays.asList(WorkType.values()), handler);
	}

	/**
	 * @deprecated Replaced by {@link #importWorkUpdates(ORCIDClient, List, ProgressHandler)}
	 * 
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param locals
	 *            the full list of local productions
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the list of work updates found in the ORCID profile, pointing
	 *         to the respective local activity
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	@Deprecated
	public static List<Work> importUpdates(ORCIDClient client, List<Work> locals, ProgressHandler handler)
			throws OrcidClientException, IllegalArgumentException {
		return importWorkUpdates(client, locals, handler);
	}

	/**
	 * <p>
	 * Discovers updates to existing local CRIS funding activities in an ORCID
	 * profile. For each funding activity group at ORCID (merged into as a
	 * single activity by the {@link ORCIDFundingHelper helper}), finds matching
	 * local entries (i.e., those with shared {@link ExternalId external
	 * identifiers}) and creates update notifications if not already up to date.
	 * To import funding activities without shared external identifiers,
	 * {@link #importFundings(ORCIDClient, List, Collection, ProgressHandler)}
	 * should be used instead.
	 * </p>
	 *
	 * <p>
	 * These update notifications simply take the shape of ORCID activities
	 * themselves (representing a matching activity group). These works contain
	 * only the meta-data that needs to be updated locally. Currently, only the
	 * introduction of newly found external identifiers is considered (i.e.,
	 * those that were already present in the local entry that is being updated
	 * are removed from the returned update). Thus, the remainder fields are
	 * returned null. Since only external identifiers are considered the quality
	 * criteria is not enforced on the remote ORCID activities.
	 * </p>
	 *
	 * <p>
	 * The local entries are tested to be up-to-date by simply checking whether
	 * they contain every external identifiers in the ORCID group (see
	 * {@link ORCIDHelper#hasNewSelfIDs(ElementSummary, ElementSummary)}. Thus
	 * the remainder meta-data of the local entries can be currently left null.
	 * </p>
	 *
	 * <p>
	 * A set of activity types can be provided to allow the independent
	 * synchronization of different types of entries. Local and remote
	 * activities outside the provided types are simply ignored (they are not
	 * considered invalid).
	 * </p>
	 * 
	 * <p>
	 * The put-code attribute is used as a local key of each CRIS funding entry.
	 * This means that the returned activities representing the updates have the
	 * put-code of the local entry that is to be updated (and not the put-code
	 * of the ORCID activity that gave origin to it).
	 * </p>
	 *
	 * @param client
	 *            the ORCID client defining the CRIS Member API and user the
	 *            profile to be managed
	 * @param locals
	 *            the full list of local funding activities
	 * @param types
	 *            the types of ORCID funding activities that should be
	 *            considered (others are simply ignored)
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the list of funding updates found in the ORCID profile, pointing
	 *         to the respective local activity
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	public static List<Funding> importFundingUpdates(ORCIDClient client, List<Funding> locals, Collection<FundingType> types, ProgressHandler handler)
			throws OrcidClientException, IllegalArgumentException {
		return importUpdatesBase(new ORCIDFundingHelper(client), locals, types, handler);
	}

	/**
	 * <p>
	 * Discovers updates to existing local CRIS activities in an ORCID profile.
	 * For each activity group at ORCID (merged into as a single activity by the
	 * {@link ORCIDHelper helper}), finds matching local entries (i.e., those
	 * with shared {@link ExternalId external identifiers}) and creates update
	 * notifications if not already up to date. To import activities without
	 * shared external identifiers,
	 * {@link #importBase(ORCIDHelper, List, Collection, ProgressHandler)}
	 * should be used instead.
	 * </p>
	 *
	 * <p>
	 * These update notifications simply take the shape of ORCID activities
	 * themselves (representing a matching activity group). These works contain
	 * only the meta-data that needs to be updated locally, and the remainder
	 * fields are returned null.
	 * </p>
	 *
	 * <p>
	 * The local entries are tested to be up-to-date by simply checking whether
	 * they contain every external identifiers in the ORCID group (see
	 * {@link ORCIDHelper#hasNewSelfIDs(ElementSummary, ElementSummary)}. Thus
	 * the remainder meta-data of the local entries can be currently left null.
	 * </p>
	 *
	 * <p>
	 * A set of activity types can be provided to allow the independent
	 * synchronization of different types of entries. Local and remote
	 * activities outside the provided types are simply ignored (they are not
	 * considered invalid).
	 * </p>
	 * 
	 * <p>
	 * The put-code attribute is used as a local key of each CRIS entry. This
	 * means that the returned activities representing the updates have the
	 * put-code of the local entry that is to be updated (and not the put-code
	 * of the ORCID activity that gave origin to it).
	 * </p>
	 *
	 * <p>
	 * This procedure simply performs a GET call to the API to obtain the
	 * summaries, since the remainder meta-data is irrelevant.
	 * </p>
	 * 
	 * @param helper
	 *            helper that encapsulates the ORCID client defining the CRIS
	 *            Member API and user the profile to be managed
	 * @param locals
	 *            the full list of local activities
	 * @param types
	 *            the types of ORCID activities that should be considered
	 *            (others are simply ignored)
	 * @param handler
	 *            the progress handler responsible for receiving progress
	 *            updates
	 * @return the list of updates found in the ORCID profile, pointing to the
	 *         respective local activity
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails when getting the
	 *             activities summary
	 * @throws IllegalArgumentException
	 *             if null arguments
	 */
	private static <E extends ElementSummary, S extends ElementSummary, G, T extends Enum<T>> List<E> importUpdatesBase(
			ORCIDHelper<E, S, G, T> helper, List<E> locals,
			Collection<T> types, ProgressHandler handler)
			throws OrcidClientException, IllegalArgumentException {

		if (helper == null || locals == null || handler == null)
			throw new IllegalArgumentException("Null arguments.");
		
		if (types == null)
			types = new HashSet<T>();
		
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_UPDATES_STARTED");

		List<S> orcids = helper.getAllTypedSummaries(types);

		List<E> toUpdate = new LinkedList<E>();

		// filter already known works only
		handler.setCurrentStatus("ORCID_SYNC_IMPORT_UPDATES_ITERATION",orcids.size());
		for (int c = 0; c != orcids.size(); c++) {

			S orcid = orcids.get(c);
			Map<E, ExternalIdsDiff> matchingLocals = helper.getSelfExternalIdsDiffS(orcid, locals);
			if (!matchingLocals.isEmpty()) {
				for (E mathingLocal : matchingLocals.keySet()) {
					if (!helper.hasNewSelfIDs(mathingLocal, orcid)) {
						toUpdate.add(helper.createUpdate(mathingLocal, matchingLocals.get(mathingLocal)));
					}
				}
			}
			handler.step();
		}

		handler.done();
		return toUpdate;
	}
}
