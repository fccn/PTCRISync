package pt.ptcris.test.scenarios;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.test.TestHelper;
import pt.ptcris.test.TestClients;
import pt.ptcris.test.TestClients.Profile;
import pt.ptcris.utils.ORCIDHelper;

public class ScenarioInvalidORCID extends Scenario {

	@Override
	List<Work> setupORCIDWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOI(null, "Meta-data 3", "0"));
		works.add(TestHelper.workHANDLE(null, "Meta-data 1", "1"));
		return works;
	}

	@Override
	List<Work> setupLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workHANDLE(BigInteger.valueOf(1), "Meta-data 1", "1"));
		return works;
	}

	@Override
	List<Work> exportLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOIHANDLE(BigInteger.valueOf(2), "Meta-data 3", "0", "1"));
		return works;
	}

	@Override
	List<Work> expectedSourcedORCIDWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOIHANDLE(BigInteger.valueOf(2), "Meta-data 3", "0", "1"));
		return works;
	}

	@Override
	List<Work> expectedImportedLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOI(BigInteger.valueOf(1), null, "0"));
		return works;
	}

	@Override
	ORCIDHelper clientSource() {
		return new ORCIDHelper(TestClients.getPTCRISClient(Profile.ZEROVALIDWORKS));
	}

	@Override
	ORCIDHelper clientFixture() {
		return new ORCIDHelper(TestClients.getExternalClient(Profile.ZEROVALIDWORKS));
	}

}
