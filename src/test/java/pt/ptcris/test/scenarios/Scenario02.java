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
 * Scenario 2 of the PTCRISync specification v0.4.3, tests import.
 * 
 * Features: 
 * modification notifications with {same,more}
 * 
 * TODO: this scenario does not exactly represent the one from the specification
 * as this would require that the fixture work was set as the preferred, which
 * is impossible programmatically. This does not affect the scenario and one of
 * the user-sourced is selected instead.
 * 
 * @see Scenario
 */
public class Scenario02 extends Scenario {

	/** {@inheritDoc} */
	@Override
	List<Work> setupORCIDExternalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOIEID(null, "0", "0", "0"));
		works.add(TestHelper.workDOIHANDLE(null, "1", "1", "1"));
		return works;
	}

	/** {@inheritDoc} */
	@Override
	List<Work> setupLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOIEIDHANDLE(BigInteger.valueOf(2), "0", "0", "0", "1"));
		return works;
	}

	/** {@inheritDoc} */
	@Override
	List<Work> expectedImportedWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOI(BigInteger.valueOf(2), null, "1"));
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
		return new ORCIDHelper(TestClients.getExternalClient(Profile.ONEVALIDWORKS));
	}
}
