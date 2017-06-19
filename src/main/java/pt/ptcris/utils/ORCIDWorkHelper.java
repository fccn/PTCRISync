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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.activities.WorkGroup;
import org.um.dsi.gavea.orcid.model.common.ExternalId;
import org.um.dsi.gavea.orcid.model.common.ExternalIds;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;
import org.um.dsi.gavea.orcid.model.work.WorkType;

import pt.ptcris.ORCIDClient;
import pt.ptcris.PTCRISyncResult;

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

	@Override
	protected List<WorkGroup> getSummariesClient() throws OrcidClientException {
		return client.getWorksSummary().getGroup();
	}

	@Override
	protected PTCRISyncResult getClient(WorkSummary work) {
		return client.getWork(work);
	}
	
	@Override
	protected ORCIDWorker readWorker(WorkSummary s, Map<BigInteger, PTCRISyncResult> cb, Logger log) {
		return new ORCIDGetWorker(s, client, cb, _log);
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
		res &= identicalExternalIDs(getPartOfExternalIdsE(preWork),
				getPartOfExternalIdsS(posWork));
		res &= getTitleE(preWork).equals(getTitleS(posWork));
		res &= (getPubYearE(preWork) == null && getYearS(posWork) == null)
				|| (getPubYearE(preWork) != null
						&& getYearS(posWork) != null && getPubYearE(preWork)
						.equals(getYearS(posWork)));
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
				if (!validExternalIdType(eid.getExternalIdType()))
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
	protected String getTitleS(WorkSummary work) {
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
	protected String getYearS(WorkSummary work) {
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
	protected boolean validExternalIdType(String eid) {
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
	public void setExternalIdsE(Work work, ExternalIds weids) {
		work.setExternalIds(weids);
	}

	@Override
	protected WorkType getTypeS(WorkSummary work) {
		return work.getType();
	}

	@Override
	protected List<WorkSummary> getGroupSummaries(WorkGroup group) {
		return group.getWorkSummary();
	}

	@Override
	protected PTCRISyncResult addClient(Work work) {
		return client.addWork(work);
	}

	@Override
	protected List<PTCRISyncResult> addClient(List<Work> clones) {
		return client.addWorks(clones);
	}

	@Override
	protected PTCRISyncResult updateClient(BigInteger remotePutcode, Work clone) {
		return client.updateWork(remotePutcode, clone);
	}

	@Override
	protected PTCRISyncResult deleteClient(BigInteger putcode) {
		return client.deleteWork(putcode);
	}

	@Override
	protected Map<BigInteger, PTCRISyncResult> getClient(List<WorkSummary> putcodes) {
		return client.getWorks(putcodes);
	}

	@Override
	protected ORCIDWorker readWorker(List<WorkSummary> putcodes,
			Map<BigInteger, PTCRISyncResult> cb, Logger log) {
		return new ORCIDBulkGetWorker(putcodes, client, cb, log);
	}

	@Override
	public void setExternalIdsS(WorkSummary summary, ExternalIds eids) {
		summary.setExternalIds(eids);
	}
	
}
