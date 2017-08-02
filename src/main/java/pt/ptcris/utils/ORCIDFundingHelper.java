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

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.activities.FundingGroup;
import org.um.dsi.gavea.orcid.model.common.ExternalId;
import org.um.dsi.gavea.orcid.model.common.ExternalIds;
import org.um.dsi.gavea.orcid.model.funding.Funding;
import org.um.dsi.gavea.orcid.model.funding.FundingSummary;
import org.um.dsi.gavea.orcid.model.funding.FundingType;

import pt.ptcris.ORCIDClient;
import pt.ptcris.PTCRISyncResult;
import pt.ptcris.handlers.ProgressHandler;

/**
 * An helper to simplify the use of the low-level ORCID
 * {@link pt.ptcris.ORCIDClient client}.
 * 
 * Provides support for asynchronous communication with ORCID
 * although it is only active for GET requests due to resource
 * limitations.
 */
public final class ORCIDFundingHelper extends ORCIDHelper<Funding, FundingSummary, FundingGroup, FundingType> {
	
	/**
	 * Initializes the helper with a given ORCID client.
	 *
	 * @param orcidClient
	 *            the ORCID client
	 */
	public ORCIDFundingHelper(ORCIDClient orcidClient) {
		super(orcidClient,0,0);
	}
	
	/*
	 * Client methods instantiated for ORCID funding activities.
	 */
	
	/** {@inheritDoc} */
	@Override
	protected List<FundingGroup> getSummariesClient() throws OrcidClientException {
		assert client != null;
		_log.debug("[getFundingSummaries]"+client.getUserId());
		return client.getFundingsSummary().getGroup();
	}

	/** {@inheritDoc} */
	@Override
	protected PTCRISyncResult<Funding> readClient(FundingSummary summary) {
		assert client != null;
		assert summary != null;
		assert summary.getPutCode() != null;
		_log.debug("[getFullFunding] "+summary.getPutCode());
		return client.getFunding(summary);
	}

	/** {@inheritDoc} */
	@Override
	protected Map<BigInteger, PTCRISyncResult<Funding>> readClient(
			List<FundingSummary> fundings) {
		throw new UnsupportedOperationException("No support for bulk reading fundings.");
	}

	/** {@inheritDoc} */
	@Override
	protected ORCIDWorker<Funding> readWorker(FundingSummary summary, Map<BigInteger, PTCRISyncResult<Funding>> cb, ProgressHandler handler) {
		assert client != null;
		assert summary != null;
		return new ORCIDGetFundingWorker(summary, client, cb, _log, handler);
	}

	/** {@inheritDoc} */
	@Override
	protected ORCIDWorker<Funding> readWorker(List<FundingSummary> summaries,
			Map<BigInteger, PTCRISyncResult<Funding>> cb, ProgressHandler handler) {
		throw new UnsupportedOperationException("No support for bulk reading fundings.");
	}

	/** {@inheritDoc} */
	@Override
	protected PTCRISyncResult<Funding> addClient(Funding funding) {
		assert client != null;
		assert funding != null;
		_log.debug("[addFunding] "+funding.getTitle());
		return client.addFunding(funding);
	}

	/** {@inheritDoc} */
	@Override
	protected List<PTCRISyncResult<Funding>> addClient(List<Funding> fundings) {
		throw new UnsupportedOperationException("No support for bulk reading fundings.");
	}

	/** {@inheritDoc} */
	@Override
	protected PTCRISyncResult<Funding> updateClient(BigInteger remotePutcode, Funding funding) {
		assert client != null;
		assert remotePutcode != null;
		assert funding != null;
		_log.debug("[updateFunding] "+remotePutcode);
		return client.updateFunding(remotePutcode, funding);
	}

	/** {@inheritDoc} */
	@Override
	protected PTCRISyncResult<Funding> deleteClient(BigInteger remotePutcode) {
		assert client != null;
		assert remotePutcode != null;
		_log.debug("[deleteFunding] "+remotePutcode);
		return client.deleteFunding(remotePutcode);
	}

	/*
	 * Static methods instantiated for ORCID funding activities.
	 */
	
	/** {@inheritDoc} */
	@Override
	public ExternalIds getNonNullExternalIdsE (Funding funding) {
		if (funding.getExternalIds() == null || funding.getExternalIds().getExternalId() == null) {
			return new ExternalIds(new ArrayList<ExternalId>());
		} else {
			return funding.getExternalIds();
		}
	}

	/** {@inheritDoc} */
	@Override
	public ExternalIds getNonNullExternalIdsS (FundingSummary funding) {
		if (funding.getExternalIds() == null || funding.getExternalIds().getExternalId() == null) {
			return new ExternalIds(new ArrayList<ExternalId>());
		} else {
			return funding.getExternalIds();
		}
	}

	/** {@inheritDoc} */
	@Override
	public void setExternalIdsE(Funding funding, ExternalIds eids) {
		assert funding != null;
		if (eids == null) eids = new ExternalIds(new ArrayList<ExternalId>());
		funding.setExternalIds(eids);
	}

	/** {@inheritDoc} */
	@Override
	public void setExternalIdsS(FundingSummary summary, ExternalIds eids) {
		assert summary != null;
		if (eids == null) eids = new ExternalIds(new ArrayList<ExternalId>());
		summary.setExternalIds(eids);
	}

	/** {@inheritDoc} */
	@Override
	protected FundingType getTypeS(FundingSummary funding) {
		assert funding != null;
		return funding.getType();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Funding types are always grant_number.
	 */
	protected boolean validExternalIdType(String eid) {
		return eid.equals("grant_number");
	}

	/** {@inheritDoc} */
	@Override
	protected String getTitleS(FundingSummary summary) {
		assert summary != null;
		if (summary.getTitle() == null)
			return "";
		return summary.getTitle().getTitle();
	}

	/** {@inheritDoc} */
	@Override
	protected String getYearS(FundingSummary summary) {
		assert summary != null;
		if (summary.getStartDate() == null
				|| summary.getStartDate().getYear() == null)
			return null;
		return summary.getStartDate().getYear().getValue();
	}

	/** {@inheritDoc} */
	@Override
	protected List<FundingSummary> getGroupSummaries(FundingGroup group) {
		assert group != null;
		return group.getFundingSummary();
	}
	
	/** {@inheritDoc} */
	@Override
	protected FundingSummary group(FundingGroup group) throws IllegalArgumentException {
		assert group != null;
		if (group.getFundingSummary() == null || group.getFundingSummary().isEmpty())
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

	/**
	 * {@inheritDoc}
	 *
	 * The considered fields are: title, start date (year), funding type and
	 * part-of external identifiers. All this meta-data is available in funding
	 * summaries.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 */
	@Override
	protected boolean isMetaUpToDate(Funding preFunding, FundingSummary posFunding) {
		assert preFunding != null;
		assert posFunding != null;

		boolean res = true;
		res &= identicalExternalIDs(
				getPartOfExternalIdsE(preFunding), 
				getPartOfExternalIdsS(posFunding));
		res &= getTitleE(preFunding).equals(getTitleS(posFunding));
		res &= (getPubYearE(preFunding) == null && getYearS(posFunding) == null)
				|| (getPubYearE(preFunding) != null && getYearS(posFunding) != null 
						&& getPubYearE(preFunding).equals(getYearS(posFunding)));
		res &= (preFunding.getType() == null && posFunding.getType() == null)
				|| (preFunding.getType() != null && posFunding.getType() != null && preFunding
						.getType().equals(posFunding.getType()));
		return res;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * The considered fields are: self external identifiers, title, start date
	 * (year), funding type and funding organization. The test also checks
	 * whether the external identifiers overlap with those of the coexisting
	 * funding entries. All this meta-data is available in funding summaries.
	 * 
	 * TODO: contributors are not being considered as they are not contained in
	 * the summaries.
	 */
	@Override
	protected Set<String> testMinimalQuality(FundingSummary funding, Collection<Funding> others) {
		assert funding != null;
		if (others == null) others = new ArrayList<Funding>();
		
		final Set<String> res = new HashSet<String>();
		if (getSelfExternalIdsS(funding).getExternalId().isEmpty())
			res.add(INVALID_EXTERNALIDENTIFIERS);
		else for (ExternalId eid : getSelfExternalIdsS(funding).getExternalId())
				if (!validExternalIdType(eid.getExternalIdType())) res.add(INVALID_EXTERNALIDENTIFIERS);
		if (funding.getTitle() == null)
			res.add(INVALID_TITLE);
		else if (funding.getTitle().getTitle() == null)
			res.add(INVALID_TITLE);
		if (funding.getType() == null)
			res.add(INVALID_TYPE);
		if (funding.getOrganization() == null 
				|| funding.getOrganization().getAddress() == null
				|| funding.getOrganization().getAddress().getCity() == null 
				|| funding.getOrganization().getAddress().getCountry() == null)
			res.add(INVALID_ORGANIZATION);
		if (funding.getStartDate() == null)
			res.add(INVALID_PUBLICATIONDATE);
		else {
			if (!testQualityFuzzyDate(funding.getStartDate()))
				res.add(INVALID_PUBLICATIONDATE);
			if (funding.getStartDate().getYear() == null)
				res.add(INVALID_YEAR);
		}		
		
		if (funding.getEndDate() != null && !!testQualityFuzzyDate(funding.getEndDate()))
			res.add(INVALID_PUBLICATIONDATE);
			
		Map<Funding, ExternalIdsDiff> fundingsDiffs = getSelfExternalIdsDiffS(funding, others);
		for (Funding match : fundingsDiffs.keySet())
			if (match.getPutCode() != funding.getPutCode() && !fundingsDiffs.get(match).same.isEmpty())
				res.add(OVERLAPPING_EIDs);
		
		return res;
	}

	/** {@inheritDoc} */
	@Override
	public Funding createUpdate(Funding original, ExternalIdsDiff diff) {
		assert original != null;
		assert diff != null;
		
		Funding fundingUpdate = cloneE(original);
		ExternalIds weids = new ExternalIds();
		List<ExternalId> neids = new ArrayList<ExternalId>(diff.more);
		weids.setExternalId(neids);
		ORCIDHelper.setWorkLocalKey(fundingUpdate, ORCIDHelper.getActivityLocalKey(original));
		fundingUpdate.setExternalIds(weids);
		fundingUpdate.setTitle(null);
		fundingUpdate.setType(null);
		fundingUpdate.setStartDate(null);
		fundingUpdate.setEndDate(null);
		fundingUpdate.setOrganization(null);
		return fundingUpdate;
	}

	/** {@inheritDoc} */
	@Override
	public FundingSummary cloneS(FundingSummary summary) {
		assert summary != null;
		
		final FundingSummary dummy = new FundingSummary();
		copy(summary, dummy);
		dummy.setStartDate(summary.getStartDate());
		dummy.setEndDate(summary.getEndDate());
		dummy.setOrganization(summary.getOrganization());
		dummy.setTitle(summary.getTitle());
		dummy.setType(summary.getType());
		dummy.setExternalIds(getNonNullExternalIdsS(summary));
		return dummy;
	}
	
	/** {@inheritDoc} */
	@Override
	public Funding cloneE(Funding funding) {
		assert funding != null;
		
		final Funding dummy = new Funding();
		copy(funding, dummy);
		dummy.setStartDate(funding.getStartDate());
		dummy.setEndDate(funding.getEndDate());
		dummy.setOrganization(funding.getOrganization());
		dummy.setTitle(funding.getTitle());
		dummy.setType(funding.getType());
		dummy.setAmount(funding.getAmount());
		dummy.setContributors(funding.getContributors());
		dummy.setShortDescription(funding.getShortDescription());
		dummy.setOrganizationDefinedType(funding.getOrganizationDefinedType());
		dummy.setUrl(funding.getUrl());
		dummy.setExternalIds(getNonNullExternalIdsE(funding));
		return dummy;
	}

	/** {@inheritDoc} */
	@Override
	protected FundingSummary summarize(Funding funding) {
		assert funding != null;
		
		final FundingSummary dummy = new FundingSummary();
		copy(funding, dummy);
		dummy.setOrganization(funding.getOrganization());
		dummy.setStartDate(funding.getStartDate());
		dummy.setEndDate(funding.getEndDate());
		dummy.setTitle(funding.getTitle());
		dummy.setType(funding.getType());
		dummy.setExternalIds(getNonNullExternalIdsE(funding));
		return dummy;
	}
}
