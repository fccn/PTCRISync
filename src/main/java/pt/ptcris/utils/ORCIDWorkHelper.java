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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.activities.WorkGroup;
import org.um.dsi.gavea.orcid.model.common.ExternalId;
import org.um.dsi.gavea.orcid.model.common.ExternalIds;
import org.um.dsi.gavea.orcid.model.common.WorkType;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

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
public final class ORCIDWorkHelper extends ORCIDHelper<Work, WorkSummary, WorkGroup, WorkType> {
	
	public enum EIdType {
		OTHER_ID("other-id"), AGR("agr"), ARXIV("arxiv"), ARK("ark"), ASIN("asin"),
				BIBCODE("bibcode"), CBA("cba"), CIT("cit"), CTX("ctx"), DNB("dnb"), DOI("doi"), 
				EID("eid"), ETHOS("ethos"), HANDLE("handle"), HIR("hir"), ISBN("isbn"), 
				ISSN("issn"), JFM("jfm"), JSTOR("jstor"), LCCN("lccn"), MR("mr"), 
				OCLC("oclc"), OL("ol"), OSTI("osti"), PAT("pat"), PMC("pmc"), 
				PMID("pmid"), RFC("rfc"), SOURCE_WORK_ID("source-work-id"), 
				SSRN("ssrn"), URI("uri"), URN("urn"), WOSUID("wosuid"), ZBL("zbl"),
				CIENCIAIUL("cienciaiul"), LENSID("lensid"), PDB("pdb"), KUID("kuid"),
				ASIN_TLD("asin-tld"), AUTHENTICUSID("authenticusid"), RRID("rrid"),
				HAL("hal"), ISMN("ismn"), EMDB("emdb"), PPR("ppr");

		public final String value;

		private EIdType(String value) {
			this.value = value;
		}
	}
	
	private List<WorkGroup> worksGroupCache;
	
	public ORCIDWorkHelper(ORCIDClient orcidClient) {
		super(orcidClient, 100, 50, false);
		this.worksGroupCache = new ArrayList<>();
	}
	
	/**
	 * Initializes the helper with a given ORCID client.
	 *
	 * @param orcidClient
	 *            the ORCID client
	 */
	public ORCIDWorkHelper(ORCIDClient orcidClient, boolean useCache) {
		super(orcidClient, 100, 50, useCache);
		this.worksGroupCache = new ArrayList<>();
	}

	/*
	 * Client methods instantiated for ORCID work activities.
	 */
	
	/** {@inheritDoc} */
	@Override
	protected List<WorkGroup> getSummariesClient() throws OrcidClientException {
		assert client != null;
		if (useCache && !worksGroupCache.isEmpty()) {
			_log.debug("cache used on [getWorkSummaries] "+client.getUserId());
			return worksGroupCache;
		}
		_log.debug("[getWorkSummaries] "+client.getUserId());
		
		worksGroupCache = client.getWorksSummary().getGroup();
		return worksGroupCache;
	}

	/** {@inheritDoc} */
	@Override
	protected PTCRISyncResult<Work> readClient(WorkSummary work) {
		assert client != null;
		assert work != null;
		assert work.getPutCode() != null;
		_log.debug("[getFullWork] "+work.getPutCode());
		return client.getWork(work);
	}
	
	/** {@inheritDoc} */
	@Override
	protected Map<BigInteger, PTCRISyncResult<Work>> readClient(List<WorkSummary> summaries) {
		assert client != null;
		if (summaries == null || summaries.isEmpty())
			return new HashMap<BigInteger, PTCRISyncResult<Work>>();
		_log.debug("[getFullBulkWork] "+summaries.size());
		return client.getWorks(summaries);
	}

	/** {@inheritDoc} */
	@Override
	protected ORCIDWorker<Work> readWorker(WorkSummary summary, Map<BigInteger, PTCRISyncResult<Work>> cb, ProgressHandler handler) {
		assert client != null;
		assert cb != null;
		assert summary != null;
		return new ORCIDGetWorkWorker(summary, client, cb, _log, handler);
	}

	/** {@inheritDoc} */
	@Override
	protected ORCIDWorker<Work> readWorker(List<WorkSummary> summaries,
			Map<BigInteger, PTCRISyncResult<Work>> cb, ProgressHandler handler) {
		assert client != null;
		assert cb != null;
		if (summaries == null)
			summaries = new ArrayList<WorkSummary>();
		return new ORCIDGetBulkWorkWorker(summaries, client, cb, _log, handler);
	}

	/** {@inheritDoc} */
	@Override
	protected PTCRISyncResult<Work> addClient(Work work) {
		assert client != null;
		_log.debug("[addWork] "+work.getTitle());
		return client.addWork(work);
	}

	/** {@inheritDoc} */
	@Override
	protected List<PTCRISyncResult<Work>> addClient(List<Work> works) {
		assert client != null;
		_log.debug("[addBulkWork] "+works.size());
		if (works == null || works.isEmpty())
			return new ArrayList<PTCRISyncResult<Work>>();
		return client.addWorks(works);
	}

	/** {@inheritDoc} */
	@Override
	protected PTCRISyncResult<Work> updateClient(BigInteger remotePutcode, Work work) {
		assert client != null;
		assert remotePutcode != null;
		assert work != null;
		_log.debug("[updateWork] "+remotePutcode);
		return client.updateWork(remotePutcode, work);
	}

	/** {@inheritDoc} */
	@Override
	protected PTCRISyncResult<Work> deleteClient(BigInteger remotePutcode) {
		assert client != null;
		assert remotePutcode != null;
		_log.debug("[deleteWork] "+remotePutcode);
		return client.deleteWork(remotePutcode);
	}

	/*
	 * Static methods instantiated for ORCID work activities.
	 */
	
	/** {@inheritDoc} */
	@Override
	public ExternalIds getNonNullExternalIdsE(Work work) {
		if (work.getExternalIds() == null || work.getExternalIds().getExternalId() == null) {
			return new ExternalIds(new ArrayList<ExternalId>());
		} else {
			return work.getExternalIds();
		}
	}

	/** {@inheritDoc} */
	@Override
	public ExternalIds getNonNullExternalIdsS(WorkSummary summary) {
		if (summary.getExternalIds() == null || summary.getExternalIds().getExternalId() == null) {
			return new ExternalIds(new ArrayList<ExternalId>());
		} else {
			return summary.getExternalIds();
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public void setExternalIdsE(Work work, ExternalIds eids) {
		assert work != null;
		if (eids == null) eids = new ExternalIds(new ArrayList<ExternalId>());
		work.setExternalIds(eids);
	}

	/** {@inheritDoc} */
	@Override
	public void setExternalIdsS(WorkSummary summary, ExternalIds eids) {
		assert summary != null;
		if (eids == null) eids = new ExternalIds(new ArrayList<ExternalId>());
		summary.setExternalIds(eids);
	}

	/** {@inheritDoc} */
	@Override
	protected WorkType getTypeS(WorkSummary summary) {
		assert summary != null;
		return summary.getType();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Elements of the enum {@link EIdType} take the shape of upper-case valid
	 * EId types, with slashes replaced by underscores.
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

	/** {@inheritDoc} */
	@Override
		protected String getTitleS(WorkSummary summary) {
		assert summary != null;
		if (summary.getTitle() == null)
			return "";
		return summary.getTitle().getTitle();
	}

	/** {@inheritDoc} */
	@Override
	protected String getYearS(WorkSummary summary) {
		assert summary != null;
		if (summary.getPublicationDate() == null
				|| summary.getPublicationDate().getYear() == null)
			return null;
		return String.valueOf(summary.getPublicationDate().getYear().getValue());
	}

	/** {@inheritDoc} */
	@Override
	protected List<WorkSummary> getGroupSummaries(WorkGroup group) {
		assert group != null;
		return group.getWorkSummary();
	}

	/** {@inheritDoc} */
	@Override
	protected WorkSummary group(WorkGroup group) throws IllegalArgumentException {
		assert group != null;
		if (group.getWorkSummary() == null || group.getWorkSummary().isEmpty())
			throw new IllegalArgumentException("Can't merge empty group.");
	
		final WorkSummary preferred = group.getWorkSummary().get(0);
		final WorkSummary dummy = cloneS(preferred);
	
		final List<ExternalId> eids = getPartOfExternalIdsS(dummy)
				.getExternalId();
		
		addFundedByEidsFromAllWorkSummaries(group, eids);
		
		for (ExternalId id : group.getExternalIds().getExternalId())
			eids.add(clone(id));
		dummy.setExternalIds(new ExternalIds(eids));
	
		return dummy;
	}

	private void addFundedByEidsFromAllWorkSummaries(WorkGroup group, final List<ExternalId> eids) {
		Set<String> set = new HashSet<>(eids.size());
		for (int i = 0 ; i < group.getWorkSummary().size(); i++){
			WorkSummary cloned = cloneS(group.getWorkSummary().get(i));
			List<ExternalId> fundedByEids = getFundedByExternalIdsS(cloned).getExternalId();
			List<ExternalId> fundedByEidsWithoutDuplicates = fundedByEids.stream()
					.filter(eid -> set.add(eid.getExternalIdValue()))
					.collect(Collectors.toList());
			eids.addAll(fundedByEidsWithoutDuplicates);
		}
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * The considered fields are: title, publication date (year), work type and
	 * part-of external identifiers (excluding URLs). All this meta-data is
	 * available in work summaries.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 */
	@Override
	protected boolean isMetaUpToDate(Work preWork, WorkSummary posWork) {
		assert preWork != null;
		assert posWork != null;

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
	 * {@inheritDoc}
	 * 
	 * The considered fields are: self external identifiers, title, publication
	 * date (year) and work type. The test also checks whether the external
	 * identifiers overlap with those of the coexisting works. All this
	 * meta-data is available in work summaries. The publication date is not
	 * necessary for data sets and research techniques.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 */
	@Override
	public Set<String> testMinimalQuality(WorkSummary work, Collection<Work> others) {
		assert work != null;
		if (others == null) others = new ArrayList<Work>();

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
			else {
				if (!testQualityFuzzyDate(work.getPublicationDate()))
					res.add(INVALID_PUBLICATIONDATE);
				if (work.getPublicationDate().getYear() == null)
					res.add(INVALID_YEAR);
			}
			// TODO: months and days must have two characters; but these are optional; should it be tested here?
		}
		Map<Work, ExternalIdsDiff> worksDiffs = getSelfExternalIdsDiffS(work,
				others);
		for (Work match : worksDiffs.keySet())
			if (match.getPutCode() != work.getPutCode()
					&& !worksDiffs.get(match).same.isEmpty())
				res.add(OVERLAPPING_EIDs);

		return res;
	}

	/** {@inheritDoc} */
	@Override
	public Work createUpdate(Work original, ExternalIdsDiff diff) {
		assert original != null;
		assert diff != null;
		
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

	/** {@inheritDoc} */
	@Override
	public WorkSummary cloneS(WorkSummary summary) {
		assert summary != null;

		final WorkSummary dummy = new WorkSummary();
		copy(summary, dummy);
		dummy.setPublicationDate(summary.getPublicationDate());
		dummy.setTitle(summary.getTitle());
		dummy.setType(summary.getType());
		dummy.setExternalIds(getNonNullExternalIdsS(summary));
		return dummy;
	}

	/** {@inheritDoc} */
	@Override
	public Work cloneE(Work work) {
		assert work != null;

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

	/** {@inheritDoc} */
	@Override
	public WorkSummary summarize(Work work) {
		assert work != null;

		final WorkSummary dummy = new WorkSummary();
		copy(work, dummy);
		dummy.setPublicationDate(work.getPublicationDate());
		dummy.setTitle(work.getTitle());
		dummy.setType(work.getType());
		dummy.setExternalIds(getNonNullExternalIdsE(work));
		return dummy;
	}
}
