package pt.ptcris.test.scenarios;

import pt.ptcris.ORCIDHelper;

/**
 * Features:
 * empty profile
 */

public class Scenario20 extends Scenario {

	@Override
	ORCIDHelper clientSource() {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWork(ScenarioOrcidClient.EMPTYWORKS));
	}

	@Override
	ORCIDHelper clientFixture() {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWorkFixture(ScenarioOrcidClient.EMPTYWORKS));
	}

}
