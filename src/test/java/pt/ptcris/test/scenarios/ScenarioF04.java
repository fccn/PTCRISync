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
import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.test.TestHelper;
import pt.ptcris.test.TestClients;
import pt.ptcris.test.TestClients.Profile;
import pt.ptcris.utils.ORCIDFundingHelper;
import pt.ptcris.utils.ORCIDHelper;
import pt.ptcris.utils.ORCIDWorkHelper;

/**
 * Scenario 4 of the PTCRISync specification v0.4.3, tests import.
 * 
 * Features:
 * 
 * TODO: this scenario does not exactly represent the one from the specification
 * as this would require that the fixture work was set as the preferred, which
 * is impossible programmatically. This does not affect the scenario and one of
 * the user-sourced is selected instead.
 * 
 * @see ScenarioFunding
 */
public class ScenarioF04 extends ScenarioFunding {

	/** {@inheritDoc} */
	@Override
	List<Funding> setupORCIDExternalFundings() {
		List<Funding> works = new ArrayList<Funding>();
		works.add(TestHelper.fundingNmbNmb(null, "0", "0", "1"));
		return works;
	}

	/** {@inheritDoc} */
	@Override
	List<Funding> setupLocalFundings() {
		List<Funding> works = new ArrayList<Funding>();
		works.add(TestHelper.fundingNmbNmbNmbNmb(BigInteger.valueOf(2), "0", "0", "2", "1", "3"));
		return works;
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
