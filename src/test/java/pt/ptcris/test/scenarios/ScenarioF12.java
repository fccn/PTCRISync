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
import pt.ptcris.utils.ORCIDHelper;
import pt.ptcris.utils.ORCIDFundingHelper;

/**
 * Scenario 12 of the PTCRISync specification v0.4.3, tests export and import.
 * 
 * Features: export breaks import export add modification notifications with
 * {same,more}
 * 
 * @see ScenarioFunding
 */
public class ScenarioF12 extends ScenarioFunding {

	/** {@inheritDoc} */
	@Override
	List<Funding> setupORCIDExternalFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		fundings.add(TestHelper.fundingNmbNmb(null, "0", "0", "3"));
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
		fundings.add(TestHelper.fundingNmbNmbNmbNmb(BigInteger.valueOf(2),
				"0", "0", "2", "3", "1"));
		return fundings;
	}

	/** {@inheritDoc} */
	@Override
	List<Funding> expectedORCIDCRISFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		fundings.add(TestHelper.fundingNmbNmbNmbNmb(BigInteger.valueOf(2),
				"0", "0", "2", "3", "1"));
		return fundings;
	}

	/** {@inheritDoc} */
	@Override
	List<Funding> expectedImportedFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		Funding w = TestHelper.fundingNmbNmbNmb(BigInteger.valueOf(1), null,
				"0", "3", "1");
		w.setExternalIds(new ORCIDFundingHelper(null).getSelfExternalIdsE(w));
		fundings.add(w);
		return fundings;
	}

	/** {@inheritDoc} */
	@Override
	ORCIDHelper crisClient() {
		return new ORCIDFundingHelper(
				TestClients.getCRISClient(Profile.TWOVALIDWORKS));
	}

	/** {@inheritDoc} */
	@Override
	ORCIDHelper externalClient() {
		return new ORCIDFundingHelper(
				TestClients.getExternalClient(Profile.TWOVALIDWORKS));
	}

}
