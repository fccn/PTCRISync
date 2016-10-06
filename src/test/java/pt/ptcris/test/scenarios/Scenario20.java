package pt.ptcris.test.scenarios;

import pt.ptcris.test.scenarios.ScenarioOrcidClient.Profile;
import pt.ptcris.utils.ORCIDHelper;

/**
 * Features:
 * empty profile
 */

public class Scenario20 extends Scenario {

	@Override
	ORCIDHelper clientSource() {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWork(Profile.EMPTYWORKS));
	}

	@Override
	ORCIDHelper clientFixture() {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWorkFixture(Profile.EMPTYWORKS));
	}

}
