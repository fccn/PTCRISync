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

import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.test.TestHelper;
import pt.ptcris.test.TestClients;
import pt.ptcris.test.TestClients.Profile;
import pt.ptcris.utils.ORCIDWorkHelper;

/**
 * Scenario 8 of the PTCRISync specification v0.4.3, tests export.
 * 
 * Features: 
 * export add
 * 
 * TODO: this scenario does not exactly represent the one from the specification
 * as this would require that the fixture work was set as the preferred, which
 * is impossible programmatically. This does not affect the scenario and one of
 * the user-sourced is selected instead.
 * 
 * @see Scenario
 */
public class Scenario08 extends Scenario {

	/** {@inheritDoc} */
	@Override
	List<Work> setupORCIDExternalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOIEID(BigInteger.valueOf(2), "0", "0", "0"));
		return works;
	}

	/** {@inheritDoc} */
	@Override
	List<Work> setupLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOI(BigInteger.valueOf(1), "1", "1"));
		return works;
	}

	/** {@inheritDoc} */
	@Override
	List<Work> exportLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOIEIDHANDLE(BigInteger.valueOf(2),"0", "0", "0", "1"));
		return works;
	}

	/** {@inheritDoc} */
	@Override
	List<Work> expectedORCIDCRISWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOIEIDHANDLE(BigInteger.valueOf(2),"0", "0", "0", "1"));
		return works;
	}

	/** {@inheritDoc} */
	@Override
	ORCIDWorkHelper crisClient() {
		return new ORCIDWorkHelper(TestClients.getCRISClient(Profile.TWOVALIDWORKS));
	}

	/** {@inheritDoc} */
	@Override
	ORCIDWorkHelper externalClient() {
		return new ORCIDWorkHelper(TestClients.getExternalClient(Profile.TWOVALIDWORKS));
	}

}
