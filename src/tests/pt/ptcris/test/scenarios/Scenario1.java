package pt.ptcris.test.scenarios;

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

public class Scenario1 extends Scenario {

	@Override
	List<Work> setupORCIDFixtureWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIEID(null, "Meta-data 0", "0", "0"));
		return works;
	}

	@Override
	List<Work> expectedImportedLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIEIDHANDLE(null,"Meta-data 0","0","0", "1"));
		return works;
	}

	@Override
	ORCIDHelper clientSource() throws OrcidClientException {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWork(1));
	}

	@Override
	ORCIDHelper clientFixture() throws OrcidClientException {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWorkFixture(1));
	}

}
