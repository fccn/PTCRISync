package pt.ptcris.test.scenarios;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.PTCRISyncResult;
import pt.ptcris.utils.ORCIDHelper;

public class ScenarioInvalidCRIS1 extends Scenario {

	@Override
	List<Work> exportLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIHANDLE(BigInteger.valueOf(2), "3", "0", "1"));
		works.add(ScenariosHelper.workDOIHANDLE(BigInteger.valueOf(3), "3", "1", "1"));
		return works;
	}

	@Override
	List<Work> expectedSourcedORCIDWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIHANDLE(BigInteger.valueOf(2), "3", "0", "1"));
		return works;
	}

	@Override
	List<Work> expectedImportedLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOI(BigInteger.valueOf(3), null, "0"));
		return works;
	}

	@Override
	int expectedExportCodes(BigInteger code) {
		if (code.equals(BigInteger.valueOf(3)))
			return PTCRISyncResult.CLIENTERROR;
		else
			return PTCRISyncResult.ADDOK;
	}

	@Override
	List<Work> expectedImportedInvalidWorks() {
		List<Work> works = new ArrayList<Work>();
		Work work = ScenariosHelper.workDOI(null, "1", "I2");
		work.setPublicationDate(null);
		works.add(work);
		return works;
	}

	@Override
	Set<String> expectedInvalidCodes(BigInteger putCode) {
		Set<String> res = new HashSet<String>();
		res.add(ORCIDHelper.INVALID_PUBLICATIONDATE);
		return res;
	}

	@Override
	ORCIDHelper clientSource() {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWork(ScenarioOrcidClient.ZEROVALIDWORKS));
	}

	@Override
	ORCIDHelper clientFixture() {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWorkFixture(ScenarioOrcidClient.ZEROVALIDWORKS));
	}

}
