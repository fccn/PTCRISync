package pt.ptcris.test.scenarios;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.ORCIDHelper;

/**
 * Features:
 * creation notification
 * 
 */

public class Scenario5 extends Scenario {

	@Override
	List<Work> setupORCIDFixtureWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIEID(null, "Meta-data 0", "0", "0"));
		return works;
	}

	@Override
	List<Work> setupLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIEIDHANDLE(BigInteger.valueOf(2),"Meta-data 0","0","0", "1"));
		return works;
	}
	
	@Override
	List<Work> expectedImportedLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOI(null, "Meta-data 1", "1"));
		return works;
	}
	
	@Override
	ORCIDHelper clientSource() throws OrcidClientException {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWork(2));
	}

	@Override
	ORCIDHelper clientFixture() throws OrcidClientException {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWorkFixture(2));
	}
}
