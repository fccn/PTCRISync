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

import pt.ptcris.PTCRISyncResult;
import pt.ptcris.test.TestHelper;
import pt.ptcris.test.TestClients;
import pt.ptcris.test.TestClients.Profile;
import pt.ptcris.utils.ORCIDHelper;
import pt.ptcris.utils.ORCIDWorkHelper;

/**
 * Scenario 19 of the PTCRISync specification v0.4.3, tests export.
 * 
 * Features: 
 * export updates with {same}
 *
 * @see Scenario
 */
public class Scenario19 extends Scenario {

	/** {@inheritDoc} */
	@Override
	List<Work> setupORCIDCRISWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOIEIDHANDLE(null, "1", "1", "0", "0"));
		return works;
	}

	/** {@inheritDoc} */
	@Override
	List<Work> exportLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOIEIDHANDLE(BigInteger.valueOf(0), "1", "1",
				"0", "0"));
		return works;
	}

	/** {@inheritDoc} */
	@Override
	List<Work> expectedORCIDCRISWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOIEIDHANDLE(null, "1", "1", "0", "0"));
		return works;
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
	List<Work> expectedImportedInvalidWorks() {
		List<Work> works = new ArrayList<Work>();
		Work work = TestHelper.workDOI(null, "1", "I2");
		work.setPublicationDate(null);
		works.add(work);
		return works;
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
	ORCIDWorkHelper crisClient() {
		return new ORCIDWorkHelper(
				TestClients.getCRISClient(Profile.ZEROVALIDWORKS));
	}

	/** {@inheritDoc} */
	@Override
	ORCIDWorkHelper externalClient() {
		return new ORCIDWorkHelper(
				TestClients.getExternalClient(Profile.ZEROVALIDWORKS));
	}

}
