package pt.ptcris.test.scenarios;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.PTCRISyncResult;
import pt.ptcris.test.TestHelper;
import pt.ptcris.test.TestClients;
import pt.ptcris.test.TestClients.Profile;
import pt.ptcris.utils.ORCIDHelper;

public class ScenarioInvalidLocal1 extends Scenario {

	/** {@inheritDoc} */
	@Override
	List<Work> exportLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(TestHelper.workDOIHANDLE(BigInteger.valueOf(2), "3", "0", "1"));
		works.add(TestHelper.workDOIHANDLE(BigInteger.valueOf(3), "3", "1", "1"));
		return works;
	}

	/** {@inheritDoc} */
	@Override
	int expectedExportCodes(BigInteger code) {
		return PTCRISyncResult.INVALID;
	}

	/** {@inheritDoc} */
	@Override
	List<Work> expectedImportedInvalidWorks() {
		List<Work> works = new ArrayList<Work>();
		Work work = TestHelper.workDOI(null, "1", "I2");
		work.setPublicationDate(null);
		works.add(work);
		return works;
	}

	/** {@inheritDoc} */
	@Override
	Set<String> expectedInvalidCodes(BigInteger putCode) {
		Set<String> res = new HashSet<String>();
		res.add(ORCIDHelper.INVALID_PUBLICATIONDATE);
		return res;
	}

	/** {@inheritDoc} */
	@Override
	ORCIDHelper crisClient() {
		return new ORCIDHelper(
				TestClients.getCRISClient(Profile.ZEROVALIDWORKS));
	}

	/** {@inheritDoc} */
	@Override
	ORCIDHelper externalClient() {
		return new ORCIDHelper(
				TestClients.getExternalClient(Profile.ZEROVALIDWORKS));
	}

}
