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

	int k = 75;
	int _i = k, _j = _i + 2*k, _k = _j + k, _l = _k + k;
	
	/** {@inheritDoc} */
	@Override
	List<Work> setupORCIDCRISWorks() {
		List<Work> works = new ArrayList<Work>();
		for (int i = _i; i<_k; i++)
			works.add(TestHelper.workDOI(BigInteger.valueOf(i), "PTCRIS "+i, i+""));
		
		return works;
	}

	/** {@inheritDoc} */
	@Override
	List<Work> setupORCIDExternalWorks() {
		List<Work> works = new ArrayList<Work>();
		for (int i = _i; i<_j; i++)
			works.add(TestHelper.workDOI(null, "External source "+i, i+""));
		for (int i = _j; i<_l; i++)
			works.add(TestHelper.workDOIHANDLE(null, "External source "+i, i+"", i+""));
		return works;
	}
	
	/** {@inheritDoc} */
	List<Work> exportLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		for (int i = 0; i < _j; i++)
			works.add(TestHelper.workDOI(BigInteger.valueOf(i), "PTCRIS "+i, i+""));
		for (int i = _j; i < _k; i++)
			works.add(TestHelper.workDOI(BigInteger.valueOf(i), "PTCRIS' "+i, i+""));
		
		return works;
	}
	
	
	/** {@inheritDoc} */
	@Override
	List<Work> expectedImportedWorks() {
		List<Work> works = new ArrayList<Work>();
	
		for (int i = _j; i<_k; i++)
			works.add(TestHelper.workHANDLE(BigInteger.valueOf(i), null, i+""));
		for (int i = _k; i<_l; i++)
			works.add(TestHelper.workDOIHANDLE(null, "External source "+i, i+"", i+""));

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
	List<Work> expectedORCIDCRISWorks() {
		return exportLocalWorks();
	}
	
	/** {@inheritDoc} */
	Set<Integer> expectedExportCodes(BigInteger putcode) {
		Set<Integer> res = new HashSet<Integer>();
		res.add(PTCRISyncResult.ADDOK);
		res.add(PTCRISyncResult.UPTODATE); 
		res.add(PTCRISyncResult.UPDATEOK); 
		return res;
	}
	
	
	

}
