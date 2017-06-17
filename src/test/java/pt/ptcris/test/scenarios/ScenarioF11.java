/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris.test.scenarios;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.um.dsi.gavea.orcid.model.funding.Funding;

import pt.ptcris.PTCRISyncResult;
import pt.ptcris.test.TestHelper;
import pt.ptcris.test.TestClients;
import pt.ptcris.test.TestClients.Profile;
import pt.ptcris.utils.ORCIDHelper;
import pt.ptcris.utils.ORCIDFundingHelper;

/**
 * Scenario 11 of the PTCRISync specification v0.4.3, tests export.
 * 
 * Features:
 * export update with {same}
 * 
 * TODO: this scenario does not exactly represent the one from the specification
 * as this would require that the fixture funding was set as the preferred, which
 * is impossible programmatically. This affects the scenario since its goal
 * is to depict the promotion of a funding to preferred.
 * 
 * @see Scenario
 */
public class ScenarioF11 extends ScenarioFunding {

	/** {@inheritDoc} */
	@Override
	List<Funding> setupORCIDCRISFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		fundings.add(TestHelper.fundingNmbNmbNmb(BigInteger.valueOf(2), "0", "0", "3", "1"));
		return fundings;
	}

	/** {@inheritDoc} */
	@Override
	List<Funding> setupORCIDExternalFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		fundings.add(TestHelper.fundingNmbNmb(BigInteger.valueOf(2), "0", "0", "3"));
		return fundings;
	}

	/** {@inheritDoc} */
	@Override
	List<Funding> setupLocalFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		fundings.add(TestHelper.fundingNmb(BigInteger.valueOf(1), "1", "2"));
		return fundings;
	}

	/** {@inheritDoc} */
	@Override
	List<Funding> exportLocalFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		fundings.add(TestHelper.fundingNmbNmbNmb(BigInteger.valueOf(2), "3", "0", "3", "1"));
		return fundings;
	}

	/** {@inheritDoc} */
	@Override
	List<Funding> expectedORCIDCRISFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		fundings.add(TestHelper.fundingNmbNmbNmb(BigInteger.valueOf(2), "3", "0", "3", "1"));
		return fundings;
	}

	/** {@inheritDoc} */
	Set<Integer> expectedExportCodes(BigInteger putcode) {
		Set<Integer> res = new HashSet<Integer>();
		res.add(PTCRISyncResult.UPDATEOK);
		return res;
	}
	
	/** {@inheritDoc} */
	@Override
	ORCIDHelper crisClient() {
		return new ORCIDFundingHelper(TestClients.getCRISClient(Profile.TWOVALIDWORKS));
	}

	/** {@inheritDoc} */
	@Override
	ORCIDHelper externalClient() {
		return new ORCIDFundingHelper(TestClients.getExternalClient(Profile.TWOVALIDWORKS));
	}

}
