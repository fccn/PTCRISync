/*
r * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
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
 * Scenario 1 of the PTCRISync specification v0.4.3, tests import.
 * 
 * Features: 
 * creation notification
 * 
 * TODO: this scenario does not exactly represent the one from the specification
 * as this would require that the fixture work was set as the preferred, which
 * is impossible programmatically. This does not affect the scenario and one of
 * the user-sourced is selected instead.
 * 
 * @see Scenario
 */
public class ScenarioF01 extends ScenarioFunding {

	/** {@inheritDoc} */
	@Override
	List<Funding> setupORCIDExternalFundings() {
		List<Funding> works = new ArrayList<Funding>();
		works.add(TestHelper.fundingNmbNmb(null, "0", "0", "2"));
		return works;
	}

	/** {@inheritDoc} */
	@Override
	List<Funding> expectedImportedFundings() {
		List<Funding> works = new ArrayList<Funding>();
		works.add(TestHelper.fundingNmbNmbNmb(null, "1", "0", "2", "1"));
		return works;
	}
	
	/** {@inheritDoc} */
	@Override
	List<Funding> expectedImportedInvalidFundings() {
		List<Funding> works = new ArrayList<Funding>();
		works.add(TestHelper.funding(null, "0"));
		return works;
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
	ORCIDHelper crisClient() {
		return new ORCIDFundingHelper(TestClients.getCRISClient(Profile.ONEVALIDWORKS));
	}

	/** {@inheritDoc} */
	@Override
	ORCIDHelper externalClient() {
		return new ORCIDFundingHelper(TestClients.getExternalClient(Profile.ONEVALIDWORKS));
	}

}
