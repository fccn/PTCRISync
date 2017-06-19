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
import org.um.dsi.gavea.orcid.model.activities.FundingGroup;
import org.um.dsi.gavea.orcid.model.common.ExternalId;
import org.um.dsi.gavea.orcid.model.common.ExternalIds;
import org.um.dsi.gavea.orcid.model.funding.Funding;
import org.um.dsi.gavea.orcid.model.funding.FundingSummary;
import org.um.dsi.gavea.orcid.model.funding.FundingType;

import pt.ptcris.ORCIDClient;
import pt.ptcris.PTCRISyncResult;

/**
 * An helper to simplify the use of the low-level ORCID
 * {@link pt.ptcris.ORCIDClient client}.
 * 
 * Provides support for asynchronous communication with ORCID
 * although it is only active for GET requests due to resource
 * limitations.
 */
public class ORCIDFundingHelper extends ORCIDHelper<Funding, FundingSummary, FundingGroup, FundingType> {
	
	public enum EIdType {
		GRANT_NUMBER("grant-number"); 
		
		public final String value;

		private EIdType(String value) {
			this.value = value;
		}
	}
	
	private static final Logger _log = LoggerFactory.getLogger(ORCIDFundingHelper.class);

	/**
	 * Initializes the helper with a given ORCID client.
	 *
	 * @param orcidClient
	 *            the ORCID client
	 */
	public ORCIDFundingHelper(ORCIDClient orcidClient) {
		super(orcidClient,0,0);
	}
	
	@Override
	protected List<FundingGroup> getSummariesClient() throws OrcidClientException {
		return client.getFundingsSummary().getGroup();
	}

	@Override
	public List<FundingSummary> getGroupSummaries(FundingGroup group) {
		return group.getFundingSummary();
	}

	@Override
	protected PTCRISyncResult getClient(FundingSummary work) {
		return client.getFunding(work);
	}
	
	@Override
	protected ORCIDWorker readWorker(FundingSummary s, Map<BigInteger, PTCRISyncResult> cb, Logger log) {
		return new ORCIDGetWorker2(s, client, cb, _log);
	}

	@Override
	protected boolean isMetaUpToDate(Funding preWork, FundingSummary posWork) 
			throws NullPointerException {
		if (preWork == null || posWork == null)
			throw new NullPointerException("Can't test null works.");

		boolean res = true;
		res &= identicalExternalIDs(
				getPartOfExternalIdsE(preWork), 
				getPartOfExternalIdsS(posWork));
		res &= getTitleE(preWork).equals(getTitleS(posWork));
		res &= (getPubYearE(preWork) == null && getYearS(posWork) == null)
				|| (getPubYearE(preWork) != null && getYearS(posWork) != null 
						&& getPubYearE(preWork).equals(getYearS(posWork)));
		res &= (preWork.getType() == null && posWork.getType() == null)
				|| (preWork.getType() != null && posWork.getType() != null && preWork
						.getType().equals(posWork.getType()));
		return res;
	}


	@Override
	public Set<String> testMinimalQuality(FundingSummary work, Collection<Funding> others) throws NullPointerException {
		if (work == null)
			throw new NullPointerException("Can't test null work.");
		
		final Set<String> res = new HashSet<String>();
		if (getSelfExternalIdsS(work).getExternalId().isEmpty())
			res.add(INVALID_EXTERNALIDENTIFIERS);
		else for (ExternalId eid : getSelfExternalIdsS(work).getExternalId())
				if (!validExternalIdType(eid.getExternalIdType())) res.add(INVALID_EXTERNALIDENTIFIERS);
		if (work.getTitle() == null)
			res.add(INVALID_TITLE);
		else if (work.getTitle().getTitle() == null)
			res.add(INVALID_TITLE);
		if (work.getType() == null)
			res.add(INVALID_TYPE);
		if (work.getOrganization() == null)
			res.add(INVALID_ORGANIZATION);
		if (work.getStartDate() == null)
			res.add(INVALID_PUBLICATIONDATE);
		else if (work.getStartDate().getYear() == null)
			res.add(INVALID_YEAR);

		Map<Funding, ExternalIdsDiff> worksDiffs = getSelfExternalIdsDiffS(work, others);
		for (Funding match : worksDiffs.keySet())
			if (match.getPutCode() != work.getPutCode() && !worksDiffs.get(match).same.isEmpty())
				res.add(OVERLAPPING_EIDs);
		
		return res;
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
	protected boolean validExternalIdType(String eid) {
		try {
			EIdType.valueOf(eid.replace('-', '_').toUpperCase());
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Merges a work group into a single funding summary. Simply selects the
	 * meta-data (including part-of external identifiers) from the first funding
	 * entrey of the group (i.e., the preferred one) and assigns it any extra
	 * (self) external identifiers from the remainder funding entries. These
	 * remainder identifiers are the ones grouped by ORCID.
	 *
	 * @param group
	 *            the funding group to be merged
	 * @return the resulting funding summary
	 * @throws NullPointerException
	 *             if the group is null
	 * @throws IllegalArgumentException
	 *             if the group is empty
	 */
	@Override
	protected FundingSummary group(FundingGroup group) 
			throws NullPointerException, IllegalArgumentException {
		if (group == null || group.getFundingSummary() == null)
			throw new NullPointerException("Can't merge null group");
		if (group.getFundingSummary().isEmpty())
			throw new IllegalArgumentException("Can't merge empty group.");
		
		final FundingSummary preferred = group.getFundingSummary().get(0);
		final FundingSummary dummy = cloneS(preferred);

		final List<ExternalId> eids = getPartOfExternalIdsS(dummy).getExternalId();
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

	@Override
	protected String getTitleS(FundingSummary work) {
		if (work == null || work.getTitle() == null)
			return "";
		return work.getTitle().getTitle();
	}
	
	/**
	 * Returns the non-null external identifiers of a funding entry (null
	 * becomes empty list).
	 * 
	 * @param fund
	 *            the funding entry from which to retrieve the external
	 *            identifiers
	 * @return the non-null external identifiers
	 */
	@Override
	public ExternalIds getNonNullExternalIdsE (Funding work) {
		if (work.getExternalIds() != null && work.getExternalIds().getExternalId() != null) {
			return work.getExternalIds();
		} else {
			return new ExternalIds(new ArrayList<ExternalId>());
		}
	}
	
	/**
	 * Returns the non-null external identifiers of a funding summary (null becomes
	 * empty list).
	 * 
	 * @param funding
	 *            the funding summary from which to retrieve the external
	 *            identifiers
	 * @return the non-null external identifiers
	 */
	@Override
	public ExternalIds getNonNullExternalIdsS (FundingSummary funding) {
		if (funding.getExternalIds() != null && funding.getExternalIds().getExternalId() != null) {
			return funding.getExternalIds();
		} else {
			return new ExternalIds(new ArrayList<ExternalId>());
		}
	}
	
	/**
	 * Clones a funding summary.
	 * 
	 * @param fund
	 *            the summary to be cloned
	 * @return the clone
	 */
	@Override
	public FundingSummary cloneS(FundingSummary fund) {
		if (fund == null) return null;
		
		final FundingSummary dummy = new FundingSummary();
		copy(fund, dummy);
		dummy.setStartDate(fund.getStartDate());
		dummy.setEndDate(fund.getEndDate());
		dummy.setOrganization(fund.getOrganization());
		dummy.setTitle(fund.getTitle());
		dummy.setType(fund.getType());
		dummy.setExternalIds(getNonNullExternalIdsS(fund));
		return dummy;
	}
	
	@Override
	public Funding cloneE(Funding fund) {
		if (fund == null) return null;
		
		final Funding dummy = new Funding();
		copy(fund, dummy);
		dummy.setStartDate(fund.getStartDate());
		dummy.setEndDate(fund.getEndDate());
		dummy.setOrganization(fund.getOrganization());
		dummy.setTitle(fund.getTitle());
		dummy.setType(fund.getType());
		dummy.setAmount(fund.getAmount());
		dummy.setContributors(fund.getContributors());
		dummy.setShortDescription(fund.getShortDescription());
		dummy.setOrganizationDefinedType(fund.getOrganizationDefinedType());
		dummy.setUrl(fund.getUrl());
		dummy.setExternalIds(getNonNullExternalIdsE(fund));
		return dummy;
	}

	@Override
	protected FundingSummary summarize(Funding work) {
		if (work == null) return null;
		
		final FundingSummary dummy = new FundingSummary();
		copy(work, dummy);
		dummy.setOrganization(work.getOrganization());
		dummy.setStartDate(work.getStartDate());
		dummy.setEndDate(work.getEndDate());
		dummy.setTitle(work.getTitle());
		dummy.setType(work.getType());
		dummy.setExternalIds(getNonNullExternalIdsE(work));
		return dummy;
	}

	@Override
	protected String getYearS(FundingSummary work) {
		if (work == null 
				|| work.getStartDate() == null
				|| work.getStartDate().getYear() == null)
			return null;
		return work.getStartDate().getYear().getValue();
	}

	@Override
	public Funding createUpdate(Funding original, ExternalIdsDiff diff) {
		Funding workUpdate = cloneE(original);
		ExternalIds weids = new ExternalIds();
		List<ExternalId> neids = new ArrayList<ExternalId>(diff.more);
		weids.setExternalId(neids);
		ORCIDHelper.setWorkLocalKey(workUpdate, ORCIDHelper.getActivityLocalKey(original));
		workUpdate.setExternalIds(weids);
		workUpdate.setTitle(null);
		workUpdate.setType(null);
		workUpdate.setStartDate(null);
		workUpdate.setEndDate(null);
		workUpdate.setOrganization(null);
		return workUpdate;
	}

	@Override
	public void setExternalIdsE(Funding work, ExternalIds weids) {
		work.setExternalIds(weids);
	}

	@Override
	protected FundingType getTypeS(FundingSummary work) {
		return work.getType();
	}

	@Override
	protected PTCRISyncResult addClient(Funding work) {
		return client.addFunding(work);
	}

	@Override
	protected List<PTCRISyncResult> addClient(List<Funding> clones) {
		throw new UnsupportedOperationException("No support for bulk reading fundings.");
	}
	
	@Override
	protected PTCRISyncResult updateClient(BigInteger remotePutcode, Funding clone) {
		return client.updateFunding(remotePutcode, clone);
	}

	@Override
	protected PTCRISyncResult deleteClient(BigInteger putcode) {
		return client.deleteFunding(putcode);
	}

	@Override
	protected Map<BigInteger, PTCRISyncResult> getClient(
			List<FundingSummary> putcodes) {
		throw new UnsupportedOperationException("No support for bulk reading fundings.");
	}

	@Override
	protected ORCIDWorker readWorker(List<FundingSummary> putcodes,
			Map<BigInteger, PTCRISyncResult> cb, Logger log) {
		throw new UnsupportedOperationException("No support for bulk reading fundings.");
	}

	@Override
	public void setExternalIdsS(FundingSummary summary, ExternalIds eids) {
		summary.setExternalIds(eids);
	}
	
	
}
