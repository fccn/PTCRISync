/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.activities.WorkGroup;
import org.um.dsi.gavea.orcid.model.common.ClientId;
import org.um.dsi.gavea.orcid.model.common.ExternalId;
import org.um.dsi.gavea.orcid.model.common.ExternalIds;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;
import org.um.dsi.gavea.orcid.model.work.WorkType;

import pt.ptcris.ORCIDClient;
import pt.ptcris.PTCRISyncResult;
import pt.ptcris.handlers.ProgressHandler;

/**
 * An helper to simplify the use of the low-level ORCID
 * {@link pt.ptcris.ORCIDClient client}.
 * 
 * Provides support for asynchronous communication with ORCID although it is
 * only active for GET requests due to resource limitations.
 */
public class ORCIDWorkHelper extends ORCIDHelper<Work, WorkSummary, WorkGroup, WorkType> {

	public enum EIdType {
		OTHER_ID("other-id"), AGR("agr"), ARXIV("arxiv"), ASIN("asin"), BIBCODE(
				"bibcode"), CBA("cba"), CIT("cit"), CTX("ctx"), DOI("doi"), EID(
				"eid"), ETHOS("ethos"), HANDLE("handle"), HIR("hir"), ISBN(
				"isbn"), ISSN("issn"), JFM("jfm"), JSTOR("jstor"), LCCN("lccn"), MR(
				"mr"), OCLC("oclc"), OL("ol"), OSTI("osti"), PAT("pat"), PMC(
				"pmc"), PMID("pmid"), RFC("rfc"), SOURCE_WORK_ID(
				"source-work-id"), SSRN("ssrn"), URI("uri"), URN("urn"), WOSUID(
				"wosuid"), ZBL("zbl"), CIENCIAIUL("cienciaiul");

		public final String value;

		private EIdType(String value) {
			this.value = value;
		}
	}

	private static final Logger _log = LoggerFactory
			.getLogger(ORCIDWorkHelper.class);

	/**
	 * Initializes the helper with a given ORCID client.
	 *
	 * @param orcidClient
	 *            the ORCID client
	 */
	public ORCIDWorkHelper(ORCIDClient orcidClient) {
		super(orcidClient, 100, 50);
	}

	/**
	 * Retrieves the entire set of work summaries from the set ORCID profile
	 * that have at least an external identifier set. Merges each ORCID group
	 * into a single summary, following {@link #groupToWork}.
	 *
	 * @return the set of work summaries in the set ORCID profile
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	@Override
	public List<WorkSummary> getAllSummaries() throws OrcidClientException {
		_log.debug("[getSummaries]");

		final List<WorkSummary> workSummaryList = new LinkedList<WorkSummary>();
		final List<WorkGroup> workGroupList = client.getWorksSummary()
				.getGroup();
		for (WorkGroup group : workGroupList)
			workSummaryList.add(group(group));
		return workSummaryList;
	}

	/**
	 * Retrieves the entire set of work summaries in the ORCID profile whose
	 * source is the Member API id defined in the ORCID client.
	 *
	 * @return the set of work summaries in the ORCID profile for the defined
	 *         source
	 * @throws OrcidClientException
	 *             if the communication with ORCID fails
	 */
	@Override
	public List<WorkSummary> getSourcedSummaries() throws OrcidClientException {
		final String sourceClientID = client.getClientId();

		_log.debug("[getSourcedSummaries] " + sourceClientID);

		final List<WorkSummary> workSummaryList = new LinkedList<WorkSummary>();
		final List<WorkGroup> workGroupList = client.getWorksSummary()
				.getGroup();

		for (WorkGroup workGroup : workGroupList) {
			for (WorkSummary workSummary : workGroup.getWorkSummary()) {
				final ClientId workClient = workSummary.getSource()
						.getSourceClientId();
				// may be null is entry added by the user
				if (workClient != null
						&& workClient.getUriPath().equals(sourceClientID)) {
					workSummaryList.add(workSummary);
				}
			}
		}
		return workSummaryList;
	}

	/**
	 * Gets a full work from an ORCID profile and adds it to a callback map. The
	 * resulting work contains every external identifier set in the input work
	 * summary, because the summary resulted from the merging of a group, but
	 * the retrieved full work is a single work. It also clears the put-code,
	 * since at this level they represent the local identifier. If possible, the
	 * process is asynchronous. If the process fails, the exception is embedded
	 * in a failed {@link PTCRISyncResult}.
	 *
	 * @see ORCIDClient#getWork(BigInteger)
	 * 
	 * @param mergedWork
	 *            the work summary representing a merged group
	 * @param cb
	 *            the callback object
	 * @throws NullPointerException
	 *             if the merged work is null
	 */
	@Override
	public void getFull(WorkSummary mergedWork,
			Map<BigInteger, PTCRISyncResult> cb) throws NullPointerException {
		if (mergedWork == null)
			throw new NullPointerException("Can't get null work.");

		if (client.threads() > 1 && cb != null) {
			final ORCIDGetWorker worker = new ORCIDGetWorker(mergedWork,
					client, cb, _log);
			executor.execute(worker);
		} else {
			PTCRISyncResult fullWork;

			_log.debug("[getFullWork] " + mergedWork.getPutCode());
			fullWork = client.getWork(mergedWork);

			cb.put(mergedWork.getPutCode(), fullWork);
		}
	}

	/**
	 * Gets a list of full works from an ORCID profile and adds them to a
	 * callback map. The resulting works contain every external identifier set
	 * in the input work summaries, because the latter resulted from the merging
	 * of a group, but the retrieved full works are a single work. It also
	 * clears the put-codes, since at this level they represent the local
	 * identifier. If possible, the process is asynchronous. If the list is not
	 * a singleton, a bulk request will be performed. If the process fails of
	 * for any work, the exceptions are embedded in failed
	 * {@link PTCRISyncResult}.
	 *
	 * @see ORCIDClient#getWorks(List)
	 * 
	 * @param mergedWork
	 *            the work summaries representing the merged groups
	 * @param cb
	 *            the callback object
	 * @throws NullPointerException
	 *             if the merged work is null
	 */
	@Override
	public void getFulls(List<WorkSummary> mergedWorks,
			Map<BigInteger, PTCRISyncResult> cb, ProgressHandler handler)
			throws OrcidClientException, NullPointerException {
		if (mergedWorks == null)
			throw new NullPointerException("Can't get null work.");
		_log.debug("[getFullWorks] " + mergedWorks.size());
		if (handler != null)
			handler.setCurrentStatus("ORCID_GET_ITERATION");

		if (client.threads() > 1 && cb != null) {
			for (int i = 0; i < mergedWorks.size();) {
				int progress = (int) ((double) i / mergedWorks.size() * 100);
				if (handler != null)
					handler.setProgress(progress);
				if (bulk_size_get > 1) {
					List<WorkSummary> putcodes = new ArrayList<WorkSummary>();
					for (int j = 0; j < bulk_size_get && i < mergedWorks.size(); j++) {
						putcodes.add(mergedWorks.get(i));
						i++;
					}
					final ORCIDBulkGetWorker worker = new ORCIDBulkGetWorker(
							putcodes, client, cb, _log);
					executor.execute(worker);
				} else {
					final ORCIDGetWorker worker = new ORCIDGetWorker(
							mergedWorks.get(i), client, cb, _log);
					executor.execute(worker);
					i++;
				}
			}
		} else {
			Map<BigInteger, PTCRISyncResult> fullWorks = new HashMap<BigInteger, PTCRISyncResult>();
			for (int i = 0; i < mergedWorks.size();) {
				int progress = (int) ((double) i / mergedWorks.size() * 100);
				if (handler != null)
					handler.setProgress(progress);
				if (bulk_size_get > 1) {
					List<WorkSummary> putcodes = new ArrayList<WorkSummary>();
					for (int j = 0; j < bulk_size_get && i < mergedWorks.size(); j++) {
						putcodes.add(mergedWorks.get(i));
						i++;
					}
					fullWorks.putAll(client.getWorks(putcodes));
				} else {
					fullWorks.put(mergedWorks.get(i).getPutCode(),
							client.getWork(mergedWorks.get(i)));
					i++;
				}
			}
			cb.putAll(fullWorks);
		}

	}

	/**
	 * Synchronously adds a work to an ORCID profile. The OK result includes the
	 * newly assigned put-code. If communication fails, error message is
	 * included in the result.
	 *
	 * @see ORCIDClient#addWork(Work)
	 * 
	 * @param work
	 *            the new work to be added
	 * @return the result of the ORCID call
	 * @throws NullPointerException
	 *             if the work is null
	 */
	@Override
	protected PTCRISyncResult add(Work work) throws NullPointerException {
		if (work == null)
			throw new NullPointerException("Can't add null work.");

		_log.debug("[addWork] " + getWorkTitleE(work));

		// remove any put-code otherwise ORCID will throw an error
		final Work clone = cloneE(work);
		clone.setPutCode(null);

		return client.addWork(clone);
	}

	/**
	 * Synchronously adds a list of works to an ORCID profile. A list of results
	 * is returned, one for each input work. The OK result includes the newly
	 * assigned put-code. If communication fails, error message is included in
	 * the result. If the overall communication fails, the result is replicated
	 * for each input.
	 *
	 * @see ORCIDClient#addWorks(List)
	 * 
	 * @param works
	 *            the new works to be added
	 * @return the results of the ORCID call for each input work
	 * @throws NullPointerException
	 *             if the work is null
	 */
	@Override
	protected List<PTCRISyncResult> add(Collection<Work> works)
			throws NullPointerException {
		if (works == null)
			throw new NullPointerException("Can't add null works.");

		_log.debug("[addWorks] " + works.size());

		List<Work> clones = new ArrayList<Work>();
		// remove any put-code otherwise ORCID will throw an error
		for (Work work : works) {
			final Work clone = cloneE(work);
			clone.setPutCode(null);
			clones.add(clone);
		}

		return client.addWorks(clones);
	}

	/**
	 * Synchronously updates a work in an ORCID profile.
	 * 
	 * @see ORCIDClient#updateWork(BigInteger, Work)
	 * 
	 * @param remotePutcode
	 *            the put-code of the remote ORCID work that will be updated
	 * @param updatedWork
	 *            the new state of the work that will be updated
	 * @return the result of the ORCID call
	 * @throws NullPointerException
	 *             if either parameter is null
	 */
	@Override
	public PTCRISyncResult update(BigInteger remotePutcode, Work updatedWork)
			throws NullPointerException {
		if (remotePutcode == null || updatedWork == null)
			throw new NullPointerException("Can't update null work.");

		_log.debug("[updateWork] " + remotePutcode);

		final Work clone = cloneE(updatedWork);
		// set the remote put-code
		clone.setPutCode(remotePutcode);

		return client.updateWork(remotePutcode, clone);
	}

	/**
	 * Synchronously deletes a work in an ORCID profile.
	 * 
	 * @see ORCIDClient#deleteWork(BigInteger)
	 * 
	 * @param putcode
	 *            the remote put-code of the work to be deleted
	 * @throws NullPointerException
	 *             if the put-code is null
	 */
	@Override
	public PTCRISyncResult delete(BigInteger putcode)
			throws NullPointerException {
		if (putcode == null)
			throw new NullPointerException("Can't delete null work.");

		_log.debug("[deleteWork] " + putcode);

		return client.deleteWork(putcode);
	}

	/**
	 * Checks whether a work is already up to date regarding another one,
	 * considering meta-data other than the self external identifiers.
	 *
	 * The considered fields are: title, publication date (year), work type and
	 * part-of external identifiers. All this meta-data is available in work
	 * summaries.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 *
	 * @param preWork
	 *            the potentially out of date work
	 * @param posWork
	 *            the up to date work
	 * @return true if the considered meta-data is the same, false otherwise.
	 * @throws NullPointerException
	 *             if either work is null
	 */
	@Override
	protected boolean isMetaUpToDate(Work preWork, WorkSummary posWork)
			throws NullPointerException {
		if (preWork == null || posWork == null)
			throw new NullPointerException("Can't test null works.");

		boolean res = true;
		res &= identicalEIDs(getPartOfExternalIdsE(preWork),
				getPartOfExternalIdsS(posWork));
		res &= getWorkTitleE(preWork).equals(getWorkTitleS(posWork));
		res &= (getPubYearE(preWork) == null && getPubYearS(posWork) == null)
				|| (getPubYearE(preWork) != null
						&& getPubYearS(posWork) != null && getPubYearE(preWork)
						.equals(getPubYearS(posWork)));
		res &= (preWork.getType() == null && posWork.getType() == null)
				|| (preWork.getType() != null && posWork.getType() != null && preWork
						.getType().equals(posWork.getType()));
		return res;
	}

	/**
	 * Tests whether a work summary has minimal quality to be synchronized, by
	 * inspecting its meta-data and that of coexisting works, and returns the
	 * detected invalid fields.
	 * 
	 * The considered fields are: self external identifiers, title, publication
	 * date (year), work type. The test also checks whether the external
	 * identifiers overlap with those of the coexisting works. All this
	 * meta-data is available in work summaries. The publication date is not
	 * necessary for data sets and research techniques.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 * 
	 * @param work
	 *            the work to test for quality
	 * @param others
	 *            other coexisting works
	 * @return the set of invalid fields
	 * @throws NullPointerException
	 *             if the work is null
	 */
	@Override
	public Set<String> testMinimalQuality(WorkSummary work,
			Collection<Work> others) throws NullPointerException {
		if (work == null)
			throw new NullPointerException("Can't test null work.");

		final Set<String> res = new HashSet<String>();
		if (getSelfExternalIdsS(work).getExternalId().isEmpty())
			res.add(INVALID_EXTERNALIDENTIFIERS);
		else
			for (ExternalId eid : getSelfExternalIdsS(work).getExternalId())
				if (!validEIdType(eid.getExternalIdType()))
					res.add(INVALID_EXTERNALIDENTIFIERS);
		if (work.getTitle() == null)
			res.add(INVALID_TITLE);
		else if (work.getTitle().getTitle() == null)
			res.add(INVALID_TITLE);
		if (work.getType() == null)
			res.add(INVALID_TYPE);
		if (work.getType() == null
				|| (work.getType() != WorkType.DATA_SET && work.getType() != WorkType.RESEARCH_TECHNIQUE)) {
			if (work.getPublicationDate() == null)
				res.add(INVALID_PUBLICATIONDATE);
			else if (work.getPublicationDate().getYear() == null)
				res.add(INVALID_YEAR);
		}
		Map<Work, ExternalIdsDiff> worksDiffs = getSelfExternalIdsDiffS(work,
				others);
		for (Work match : worksDiffs.keySet())
			if (match.getPutCode() != work.getPutCode()
					&& !worksDiffs.get(match).same.isEmpty())
				res.add(OVERLAPPING_EIDs);

		return res;
	}

	/**
	 * Merges a work group into a single work summary. Simply selects the
	 * meta-data (including part-of external identifiers) from the first work of
	 * the group (i.e., the preferred one) and assigns it any extra (self)
	 * external identifiers from the remainder works. These remainder
	 * identifiers are the ones grouped by ORCID.
	 *
	 * @param group
	 *            the work group to be merged
	 * @return the resulting work summary
	 * @throws NullPointerException
	 *             if the group is null
	 * @throws IllegalArgumentException
	 *             if the group is empty
	 */
	@Override
	protected WorkSummary group(WorkGroup group) throws NullPointerException,
			IllegalArgumentException {
		if (group == null || group.getWorkSummary() == null)
			throw new NullPointerException("Can't merge null group");
		if (group.getWorkSummary().isEmpty())
			throw new IllegalArgumentException("Can't merge empty group.");

		final WorkSummary preferred = group.getWorkSummary().get(0);
		final WorkSummary dummy = cloneS(preferred);

		final List<ExternalId> eids = getPartOfExternalIdsS(dummy)
				.getExternalId();
		for (ExternalId id : group.getExternalIds().getExternalId()) {
			final ExternalId eid = new ExternalId();
			eid.setExternalIdRelationship(id.getExternalIdRelationship());
			eid.setExternalIdType(id.getExternalIdType().toLowerCase());
			eid.setExternalIdValue(id.getExternalIdValue());
			eids.add(eid);
		}
		dummy.setExternalIds(new ExternalIds(eids));

		return dummy;
	}

	/**
	 * Retrieves the title from a work summary.
	 *
	 * @param work
	 *            the work summary
	 * @return the work's title if defined, empty string otherwise
	 */
	@Override
	protected String getWorkTitleS(WorkSummary work) {
		if (work == null || work.getTitle() == null)
			return "";
		return work.getTitle().getTitle();
	}

	/**
	 * Retrieves the publication year from a work summary.
	 *
	 * @param work
	 *            the work summary
	 * @return the publication year if defined, null otherwise
	 */
	@Override
	protected String getPubYearS(WorkSummary work) {
		if (work == null || work.getPublicationDate() == null
				|| work.getPublicationDate().getYear() == null)
			return null;
		return work.getPublicationDate().getYear().getValue();
	}

	/**
	 * Returns the non-null external identifiers of a work (null becomes empty
	 * list).
	 * 
	 * @param work
	 *            the work from which to retrieve the external identifiers
	 * @return the non-null external identifiers
	 */
	@Override
	public ExternalIds getNonNullExternalIdsE(Work work) {
		if (work.getExternalIds() != null
				&& work.getExternalIds().getExternalId() != null) {
			return work.getExternalIds();
		} else {
			return new ExternalIds(new ArrayList<ExternalId>());
		}
	}

	/**
	 * Returns the non-null external identifiers of a work summary (null becomes
	 * empty list).
	 * 
	 * @param work
	 *            the work summary from which to retrieve the external
	 *            identifiers
	 * @return the non-null external identifiers
	 */
	@Override
	public ExternalIds getNonNullExternalIdsS(WorkSummary work) {
		if (work.getExternalIds() != null
				&& work.getExternalIds().getExternalId() != null) {
			return work.getExternalIds();
		} else {
			return new ExternalIds(new ArrayList<ExternalId>());
		}
	}

	/**
	 * Clones a work summary.
	 * 
	 * @param work
	 *            the summary to be cloned
	 * @return the clone
	 */
	@Override
	public WorkSummary cloneS(WorkSummary work) {
		if (work == null)
			return null;

		final WorkSummary dummy = new WorkSummary();
		copy(work, dummy);
		dummy.setPublicationDate(work.getPublicationDate());
		dummy.setTitle(work.getTitle());
		dummy.setType(work.getType());
		dummy.setExternalIds(getNonNullExternalIdsS(work));
		return dummy;
	}

	/**
	 * Clones a work.
	 * 
	 * @param work
	 *            the work to be cloned
	 * @return the clone
	 */
	@Override
	public Work cloneE(Work work) {
		if (work == null)
			return null;

		final Work dummy = new Work();
		copy(work, dummy);
		dummy.setPublicationDate(work.getPublicationDate());
		dummy.setTitle(work.getTitle());
		dummy.setType(work.getType());
		dummy.setExternalIds(getNonNullExternalIdsE(work));
		dummy.setContributors(work.getContributors());

		dummy.setCitation(work.getCitation());
		dummy.setContributors(work.getContributors());
		dummy.setCountry(work.getCountry());
		dummy.setJournalTitle(work.getJournalTitle());
		dummy.setLanguageCode(work.getLanguageCode());
		dummy.setShortDescription(work.getShortDescription());
		dummy.setUrl(work.getUrl());
		return dummy;
	}

	@Override
	protected WorkSummary summarize(Work work) {
		if (work == null)
			return null;

		final WorkSummary dummy = new WorkSummary();
		copy(work, dummy);
		dummy.setPublicationDate(work.getPublicationDate());
		dummy.setTitle(work.getTitle());
		dummy.setType(work.getType());
		dummy.setExternalIds(getNonNullExternalIdsE(work));
		return dummy;
	}

	/**
	 * Test whether a give external identifiers type is valid. Elements of the
	 * enum {@link EIdType} take the shape of upper-case valid EId types, with
	 * slashes replaced by underscores.
	 * 
	 * @param eid
	 *            a potential EId type
	 * @return whether the string is a valid EId type
	 */
	@Override
	protected boolean validEIdType(String eid) {
		try {
			EIdType.valueOf(eid.replace('-', '_').toUpperCase());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public Work createUpdate(Work original, ExternalIdsDiff diff) {
		Work workUpdate = cloneE(original);
		ExternalIds weids = new ExternalIds();
		List<ExternalId> neids = new ArrayList<ExternalId>(diff.more);
		weids.setExternalId(neids);
		ORCIDHelper.setWorkLocalKey(workUpdate,
				ORCIDHelper.getActivityLocalKey(original));
		workUpdate.setExternalIds(weids);
		workUpdate.setTitle(null);
		workUpdate.setType(null);
		workUpdate.setPublicationDate(null);
		return workUpdate;
	}

	@Override
	public void setExternalIds(Work work, ExternalIds weids) {
		work.setExternalIds(weids);
	}

	@Override
	protected WorkType getTypeS(WorkSummary work) {
		return work.getType();
	}

}
