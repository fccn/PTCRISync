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

public class ScenarioPerformance extends Scenario {

	/** {@inheritDoc} */
	@Override
	List<Work> setupORCIDCRISWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOI(null, "0", "20"));
		works.add(TestHelper.workDOI(null, "1", "21"));
		works.add(TestHelper.workDOI(null, "2", "22"));
		works.add(TestHelper.workDOI(null, "3", "23"));
		works.add(TestHelper.workDOI(null, "4", "24"));
		works.add(TestHelper.workDOI(null, "5", "25"));
		works.add(TestHelper.workDOI(null, "6", "26"));
		works.add(TestHelper.workDOI(null, "7", "27"));
		works.add(TestHelper.workDOI(null, "8", "28"));
		works.add(TestHelper.workDOI(null, "9", "29"));
		works.add(TestHelper.workDOI(null, "0", "30"));
		works.add(TestHelper.workDOI(null, "1", "31"));
		works.add(TestHelper.workDOI(null, "2", "32"));
		works.add(TestHelper.workDOI(null, "3", "33"));
		works.add(TestHelper.workDOI(null, "4", "34"));
		works.add(TestHelper.workDOI(null, "5", "35"));
		works.add(TestHelper.workDOI(null, "6", "36"));
		works.add(TestHelper.workDOI(null, "7", "37"));
		works.add(TestHelper.workDOI(null, "8", "38"));
		works.add(TestHelper.workDOI(null, "9", "39"));
		works.add(TestHelper.workDOI(null, "9", "299"));
		return works;
	}

	/** {@inheritDoc} */
	@Override
	List<Work> setupORCIDExternalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOI(null, "0", "0"));
		works.add(TestHelper.workDOI(null, "1", "1"));
		works.add(TestHelper.workDOI(null, "2", "2"));
		works.add(TestHelper.workDOI(null, "3", "3"));
		works.add(TestHelper.workDOI(null, "4", "4"));
		works.add(TestHelper.workDOI(null, "5", "5"));
		works.add(TestHelper.workDOI(null, "6", "6"));
		works.add(TestHelper.workDOI(null, "7", "7"));
		works.add(TestHelper.workDOI(null, "8", "8"));
		works.add(TestHelper.workDOI(null, "9", "9"));
		works.add(TestHelper.workDOI(null, "0", "10"));
		works.add(TestHelper.workDOI(null, "1", "11"));
		works.add(TestHelper.workDOI(null, "2", "12"));
		works.add(TestHelper.workDOI(null, "3", "13"));
		works.add(TestHelper.workDOI(null, "4", "14"));
		works.add(TestHelper.workDOI(null, "5", "15"));
		works.add(TestHelper.workDOI(null, "6", "16"));
		works.add(TestHelper.workDOI(null, "7", "17"));
		works.add(TestHelper.workDOI(null, "8", "18"));
		works.add(TestHelper.workDOI(null, "9", "19"));
		works.add(TestHelper.workDOI(null, "0", "20"));
		works.add(TestHelper.workDOI(null, "1", "21"));
		works.add(TestHelper.workDOI(null, "2", "22"));
		works.add(TestHelper.workDOI(null, "3", "23"));
		works.add(TestHelper.workDOI(null, "4", "24"));
		works.add(TestHelper.workDOI(null, "5", "25"));
		works.add(TestHelper.workDOI(null, "6", "26"));
		works.add(TestHelper.workDOI(null, "7", "27"));
		works.add(TestHelper.workDOI(null, "8", "28"));
		works.add(TestHelper.workDOI(null, "9", "29"));
		return works;
	}

	/** {@inheritDoc} */
	@Override
	List<Work> expectedImportedWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOI(null, "0", "0"));
		works.add(TestHelper.workDOI(null, "1", "1"));
		works.add(TestHelper.workDOI(null, "2", "2"));
		works.add(TestHelper.workDOI(null, "3", "3"));
		works.add(TestHelper.workDOI(null, "4", "4"));
		works.add(TestHelper.workDOI(null, "5", "5"));
		works.add(TestHelper.workDOI(null, "6", "6"));
		works.add(TestHelper.workDOI(null, "7", "7"));
		works.add(TestHelper.workDOI(null, "8", "8"));
		works.add(TestHelper.workDOI(null, "9", "9"));
		works.add(TestHelper.workDOI(null, "0", "10"));
		works.add(TestHelper.workDOI(null, "1", "11"));
		works.add(TestHelper.workDOI(null, "2", "12"));
		works.add(TestHelper.workDOI(null, "3", "13"));
		works.add(TestHelper.workDOI(null, "4", "14"));
		works.add(TestHelper.workDOI(null, "5", "15"));
		works.add(TestHelper.workDOI(null, "6", "16"));
		works.add(TestHelper.workDOI(null, "7", "17"));
		works.add(TestHelper.workDOI(null, "8", "18"));
		works.add(TestHelper.workDOI(null, "9", "19"));
		works.add(TestHelper.workDOI(null, "0", "20"));
		works.add(TestHelper.workDOI(null, "1", "21"));
		works.add(TestHelper.workDOI(null, "2", "22"));
		works.add(TestHelper.workDOI(null, "3", "23"));
		works.add(TestHelper.workDOI(null, "4", "24"));
		works.add(TestHelper.workDOI(null, "5", "25"));
		works.add(TestHelper.workDOI(null, "6", "26"));
		works.add(TestHelper.workDOI(null, "7", "27"));
		works.add(TestHelper.workDOI(null, "8", "28"));
		works.add(TestHelper.workDOI(null, "9", "29"));
		return works;
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
	ORCIDHelper crisClient() {
		return new ORCIDHelper(
				TestClients.getCRISClient(Profile.ZEROVALIDWORKS));
	}

	/** {@inheritDoc} */
	@Override
	ORCIDHelper externalClient() {
		return new ORCIDHelper(
				TestClients.getExternalClient(Profile.ZEROVALIDWORKS));
	}

	/** {@inheritDoc} */
	List<Work> exportLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		for (int i = 100; i<=300; i++)
			works.add(TestHelper.workDOI(BigInteger.valueOf(i), i+"", i+""));
		return works;
	}
	
	/** {@inheritDoc} */
	List<Work> expectedORCIDCRISWorks() {
		return exportLocalWorks();
	}
	
	/** {@inheritDoc} */
	Set<Integer> expectedExportCodes(BigInteger putcode) {
		Set<Integer> res = new HashSet<Integer>();
		res.add(PTCRISyncResult.ADDOK);
		res.add(PTCRISyncResult.UPDATEOK);
		return res;
	}
	
	
	

}
