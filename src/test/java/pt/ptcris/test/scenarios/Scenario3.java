package pt.ptcris.test.scenarios;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.utils.ORCIDHelper;

/**
 * Features:
 * 
 */

public class Scenario3 extends Scenario {

	@Override
	List<Work> setupORCIDFixtureWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIEID(null, "0", "0", "0"));
		works.add(ScenariosHelper.workDOIHANDLE(null, "1", "1", "1"));
		return works;
	}

	@Override
	List<Work> setupLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIDOIEIDHANDLE(BigInteger.valueOf(2), "0", "0", "1", "0", "1"));
		return works;
	}

	@Override
	List<Work> expectedImportedInvalidWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.work(null, "0"));
		return works;
	}
	
	@Override
	Set<String> expectedInvalidCodes(BigInteger putCode) {
		Set<String> res = new HashSet<String>();
		res.add(ORCIDHelper.INVALID_WORKEXTERNALIDENTIFIERS);
		return res;
	}

	@Override
	ORCIDHelper clientSource() {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWork(ScenarioOrcidClient.ONEVALIDWORKS));
	}

	@Override
	ORCIDHelper clientFixture() {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWorkFixture(ScenarioOrcidClient.ONEVALIDWORKS));
	}

}
