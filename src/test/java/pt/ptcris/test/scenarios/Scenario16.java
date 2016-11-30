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
		works.add(TestHelper.workHANDLE(BigInteger.valueOf(1), null, "0"));
		works.add(TestHelper.workHANDLE(BigInteger.valueOf(1), null, "1"));
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
		return new ORCIDHelper(TestClients.getCRISClient(Profile.ONEVALIDWORKS));
	}

	/** {@inheritDoc} */
	@Override
	ORCIDHelper externalClient() {
		return new ORCIDHelper(
				TestClients.getExternalClient(Profile.ONEVALIDWORKS));
	}

}
