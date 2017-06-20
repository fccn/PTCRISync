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

import pt.ptcris.test.TestClients;
import pt.ptcris.test.TestHelper;
import pt.ptcris.test.TestClients.Profile;
import pt.ptcris.utils.ORCIDFundingHelper;

/**
 * Scenario 20 of the PTCRISync specification v0.4.3, tests export.
 * 
 * Features: empty profile
 * 
 * @see Scenario
 */
public class ScenarioFIgnoredTypes extends ScenarioFunding {


	/** {@inheritDoc} */
	@Override
	List<Funding> exportLocalFundings() {
		List<Funding> res = new ArrayList<Funding>();
		res.add(TestHelper.fundingIgn(BigInteger.valueOf(2), "0", "0"));
		res.add(TestHelper.fundingIgn(BigInteger.valueOf(3), "1", "1"));
		return res;
	}
	
	/** {@inheritDoc} */
	@Override
	ORCIDFundingHelper crisClient() {
		return new ORCIDFundingHelper(TestClients.getCRISClient(Profile.EMPTYWORKS));
	}

	/** {@inheritDoc} */
	@Override
	ORCIDFundingHelper externalClient() {
		return new ORCIDFundingHelper(
				TestClients.getExternalClient(Profile.EMPTYWORKS));
	}

}
