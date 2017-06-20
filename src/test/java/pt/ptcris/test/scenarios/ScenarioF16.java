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
import pt.ptcris.utils.ORCIDHelper;
import pt.ptcris.utils.ORCIDFundingHelper;

/**
 * Scenario 16 of the PTCRISync specification v0.4.3, tests import.
 * 
 * Features: 
 * modification notifications at pre-state 
 * multiple modification notifications 
 * modification notifications with {same,more,less}
 * 
 * @see Scenario
 */
public class ScenarioF16 extends ScenarioFunding {

	/** {@inheritDoc} */
	@Override
	List<Funding> setupORCIDExternalFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		fundings.add(TestHelper.fundingNmbNmb(null, "3", "3", "2"));
		return fundings;
	}

	/** {@inheritDoc} */
	@Override
	List<Funding> setupLocalFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		fundings.add(TestHelper.fundingNmbNmb(BigInteger.valueOf(1), "1", "0", "3"));
		return fundings;
	}

	/** {@inheritDoc} */
	@Override
	List<Funding> expectedImportedFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		Funding w = TestHelper.fundingNmb(BigInteger.valueOf(1), null, "2");
		w.setExternalIds(new ORCIDFundingHelper(null).getSelfExternalIdsE(w));
		fundings.add(w);
		Funding w1 = TestHelper.fundingNmb(BigInteger.valueOf(1), null, "1");
		w1.setExternalIds(new ORCIDFundingHelper(null).getSelfExternalIdsE(w1));
		fundings.add(w1);
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
		return new ORCIDFundingHelper(
				TestClients.getExternalClient(Profile.ONEVALIDWORKS));
	}

}
