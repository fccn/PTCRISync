package pt.ptcris.test.scenarios;

import pt.ptcris.test.TestClients;
import pt.ptcris.test.TestClients.Profile;
import pt.ptcris.utils.ORCIDHelper;

/**
 * Scenario 20 of the PTCRISync specification v0.4.3, tests export.
 * 
 * Features: empty profile
 * 
 * @see Scenario
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
