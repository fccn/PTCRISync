package pt.ptcris.test.scenarios;

import pt.ptcris.test.TestClients;
import pt.ptcris.test.TestClients.Profile;
import pt.ptcris.utils.ORCIDHelper;

/**
 * Features: empty profile
 */

public class Scenario20 extends Scenario {

	/** {@inheritDoc} */
	@Override
	ORCIDHelper crisClient() {
		return new ORCIDHelper(TestClients.getCRISClient(Profile.EMPTYWORKS));
	}

	/** {@inheritDoc} */
	@Override
	ORCIDHelper externalClient() {
		return new ORCIDHelper(
				TestClients.getExternalClient(Profile.EMPTYWORKS));
	}

}
