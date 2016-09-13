package pt.ptcris.test.scenarios;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.ORCIDHelper;

/**
 * Features: export updates with {less,same,more}
 */
public class Scenario19 extends Scenario {

	@Override
	List<Work> setupORCIDWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIEIDHANDLE(null, "1", "1", "0", "0"));
		return works;
	}

	@Override
	List<Work> exportLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIEIDHANDLE(BigInteger.valueOf(0), "1", "1", "0", "0"));
		return works;
	}

	@Override
	List<Work> expectedSourcedORCIDWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIEIDHANDLE(null, "1", "1", "0", "0"));
		return works;
	}

	@Override
	Integer expectedExportCodes(BigInteger code) {
		if (code.equals(BigInteger.valueOf(0)))
			return ORCIDHelper.UPTODATE;
		else
			return ORCIDHelper.ADDOK;
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
