package pt.ptcris.test.scenarios;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.ORCIDHelper;
import pt.ptcris.PTCRISync;

public class ScenarioInvalidCRIS2 extends Scenario {

	@Override
	List<Work> setupLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workHANDLE(BigInteger.valueOf(1), "Meta-data 1", "1"));
		return works;
	}

	@Override
	List<Work> exportLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIHANDLE(BigInteger.valueOf(2), null, "0", "1"));
		return works;
	}

	@Override
	Integer expectedExportCodes(BigInteger code) {
		if (code.equals(BigInteger.valueOf(2)))
			return PTCRISync.INVALID;
		else
			return PTCRISync.OK;
	}

	@Override
	ORCIDHelper clientSource() throws OrcidClientException {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWork(0));
	}

	@Override
	ORCIDHelper clientFixture() throws OrcidClientException {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWorkFixture(0));
	}

}
