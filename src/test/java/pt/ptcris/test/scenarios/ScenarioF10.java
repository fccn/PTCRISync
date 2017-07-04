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
import java.util.List;

import org.um.dsi.gavea.orcid.model.funding.Funding;

import pt.ptcris.test.TestHelper;
import pt.ptcris.test.TestClients;
import pt.ptcris.test.TestClients.Profile;
import pt.ptcris.utils.ORCIDFundingHelper;

/**
 * Scenario 10 of the PTCRISync specification v0.4.3, tests export.
 * 
 * Features: 
 * export rem
 * preferred consequences
 * 
 * TODO: this scenario does not exactly represent the one from the specification
 * as this would require that the fixture funding was set as the preferred, which
 * is impossible programmatically. This affects the scenario since its goal
 * is to depict the promotion of a funding to preferred.
 * 
 * @see Scenario
 */
public class ScenarioF10 extends ScenarioFunding {

	/** {@inheritDoc} */
	@Override
	List<Funding> setupORCIDCRISFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		fundings.add(TestHelper.fundingNmbNmbNmb (null,"0","0","2", "1"));
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
		fundings.add(TestHelper.fundingNmbNmbNmb(BigInteger.valueOf(2),"0","0","3", "1"));
		return fundings;
	}
	
	/** {@inheritDoc} */
	@Override
	ORCIDFundingHelper crisClient() {
		return new ORCIDFundingHelper(TestClients.getCRISClient(Profile.TWOVALIDWORKS));
	}

	/** {@inheritDoc} */
	@Override
	ORCIDFundingHelper externalClient() {
		return new ORCIDFundingHelper(TestClients.getExternalClient(Profile.TWOVALIDWORKS));
	}

}
