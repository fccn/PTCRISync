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

import pt.ptcris.test.TestHelper;
import pt.ptcris.test.TestClients;
import pt.ptcris.test.TestClients.Profile;
import pt.ptcris.utils.ORCIDFundingHelper;
import pt.ptcris.utils.ORCIDHelper;

/**
 * Scenario 3 of the PTCRISync specification v0.4.3, tests import.
 * 
 * Features:
 * 
 * TODO: this scenario does not exactly represent the one from the specification
 * as this would require that the fixture funding was set as the preferred, which
 * is impossible programmatically. This does not affect the scenario and one of
 * the user-sourced is selected instead.
 * 
 * @see Scenario
 */
public class ScenarioF03 extends ScenarioFunding {

	/** {@inheritDoc} */
	@Override
	List<Funding> setupORCIDExternalFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		fundings.add(TestHelper.fundingNmbNmb(null, "0", "0", "2"));
		fundings.add(TestHelper.fundingNmbNmb(null, "1", "1", "3"));
		return fundings;
	}

	/** {@inheritDoc} */
	@Override
	List<Funding> setupLocalFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		fundings.add(TestHelper.fundingNmbNmbNmbNmb(BigInteger.valueOf(2), "0", "0", "1", "2", "3"));
		return fundings;
	}

	/** {@inheritDoc} */
	@Override
	List<Funding> expectedImportedInvalidFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		fundings.add(TestHelper.funding(null, "0"));
		return fundings;
	}
	
	/** {@inheritDoc} */
	@Override
	Set<String> expectedInvalidCodes(BigInteger putCode) {
		Set<String> res = new HashSet<String>();
		res.add(ORCIDHelper.INVALID_EXTERNALIDENTIFIERS);
		return res;
	}

	/** {@inheritDoc} */
	@Override
	ORCIDFundingHelper crisClient() {
		return new ORCIDFundingHelper(TestClients.getCRISClient(Profile.ONEVALIDWORKS));
	}

	/** {@inheritDoc} */
	@Override
	ORCIDFundingHelper externalClient() {
		return new ORCIDFundingHelper(TestClients.getExternalClient(Profile.ONEVALIDWORKS));
	}

}
