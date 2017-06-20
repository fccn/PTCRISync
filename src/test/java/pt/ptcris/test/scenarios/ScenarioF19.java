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
 * Scenario 19 of the PTCRISync specification v0.4.3, tests export.
 * 
 * Features: 
 * export updates with {same}
 *
 * @see Scenario
 */
public class ScenarioF19 extends ScenarioFunding {

	/** {@inheritDoc} */
	@Override
	List<Funding> setupORCIDCRISFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		fundings.add(TestHelper.fundingNmbNmbNmb(null, "1", "1", "0", "2"));
		return fundings;
	}

	/** {@inheritDoc} */
	@Override
	List<Funding> exportLocalFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		fundings.add(TestHelper.fundingNmbNmbNmb(BigInteger.valueOf(0), "1", "1",
				"0", "2"));
		return fundings;
	}

	/** {@inheritDoc} */
	@Override
	List<Funding> expectedORCIDCRISFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		fundings.add(TestHelper.fundingNmbNmbNmb(null, "1", "1", "0", "2"));
		return fundings;
	}

	/** {@inheritDoc} */
	Set<Integer> expectedExportCodes(BigInteger putcode) {
		Set<Integer> res = new HashSet<Integer>();
		if (putcode.equals(BigInteger.valueOf(0)))
			res.add(PTCRISyncResult.UPTODATE);
		else
			res.add(PTCRISyncResult.ADDOK);
		return res;
	}

	/** {@inheritDoc} */
	@Override
	List<Funding> expectedImportedInvalidFundings() {
		List<Funding> fundings = new ArrayList<Funding>();
		Funding funding = TestHelper.fundingNmb(null, "1", "I2");
		funding.setStartDate(null);
		fundings.add(funding);
		return fundings;
	}

	/** {@inheritDoc} */
	@Override
	Set<String> expectedInvalidCodes(BigInteger putCode) {
		Set<String> res = new HashSet<String>();
		res.add(ORCIDHelper.INVALID_PUBLICATIONDATE);
		return res;
	}

	/** {@inheritDoc} */
	@Override
	ORCIDFundingHelper crisClient() {
		return new ORCIDFundingHelper(
				TestClients.getCRISClient(Profile.ZEROVALIDWORKS));
	}

	/** {@inheritDoc} */
	@Override
	ORCIDFundingHelper externalClient() {
		return new ORCIDFundingHelper(
				TestClients.getExternalClient(Profile.ZEROVALIDWORKS));
	}

}
