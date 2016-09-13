package pt.ptcris.test.scenarios;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.ORCIDHelper;

/**
 * Features:
 * creation notification
 * 
 * TODO: this scenario does not exactly represent the one
 * from the specification as this would require that the 
 * fixture work was set as the preferred, which is impossible
 * programmatically.
 */

public class Scenario1 extends Scenario {

	@Override
	List<Work> setupORCIDFixtureWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIEID(null, "0", "0", "0"));
		return works;
	}

	@Override
	List<Work> expectedImportedLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIEIDHANDLE(null, "1", "0", "0", "1"));
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
