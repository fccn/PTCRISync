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

import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.test.TestHelper;
import pt.ptcris.test.TestClients;
import pt.ptcris.test.TestClients.Profile;
import pt.ptcris.utils.ORCIDHelper;
import pt.ptcris.utils.ORCIDWorkHelper;

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
public class Scenario16 extends Scenario {

	/** {@inheritDoc} */
	@Override
	List<Work> setupORCIDExternalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workEIDHANDLE(null, "3", "0", "0"));
		return works;
	}

	/** {@inheritDoc} */
	@Override
	List<Work> setupLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOIEID(BigInteger.valueOf(1), "1", "0", "0"));
		return works;
	}

	/** {@inheritDoc} */
	@Override
	List<Work> expectedImportedWorks() {
		List<Work> works = new ArrayList<Work>();
		Work w = TestHelper.workHANDLE(BigInteger.valueOf(1), null, "0");
		w.setExternalIds(new ORCIDWorkHelper(null).getSelfExternalIdsE(w));
		works.add(w);
		Work w1 = TestHelper.workHANDLE(BigInteger.valueOf(1), null, "1");
		w1.setExternalIds(new ORCIDWorkHelper(null).getSelfExternalIdsE(w1));
		works.add(w1);
		return works;
	}

	/** {@inheritDoc} */
	@Override
	List<Work> expectedImportedInvalidWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.work(null, "0"));
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
		return new ORCIDWorkHelper(TestClients.getCRISClient(Profile.ONEVALIDWORKS));
	}

	/** {@inheritDoc} */
	@Override
	ORCIDHelper externalClient() {
		return new ORCIDWorkHelper(
				TestClients.getExternalClient(Profile.ONEVALIDWORKS));
	}

}
