package pt.ptcris.test.scenarios;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.ORCIDHelper;

/**
 * Features:
 * export updates with {less,same,more}
 */
public class Scenario18 extends Scenario {

	@Override
	List<Work> setupORCIDWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIHANDLE(null, "3", "0", "1"));
		works.add(ScenariosHelper.workDOIHANDLE(null, "3", "1", "0"));
		return works;
	}

	@Override
	List<Work> exportLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIHANDLE(BigInteger.valueOf(1), "3", "1", "1"));
		works.add(ScenariosHelper.workDOIHANDLE(BigInteger.valueOf(2), "3", "0", "0"));
		return works;
	}

	@Override
	List<Work> expectedSourcedORCIDWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOIHANDLE(null, "3", "1", "1"));
		works.add(ScenariosHelper.workDOIHANDLE(null, "3", "0", "0"));
		return works;
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
	ORCIDHelper clientSource() {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWork(ScenarioOrcidClient.ZEROVALIDWORKS));
	}

	@Override
	ORCIDHelper clientFixture() {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWorkFixture(ScenarioOrcidClient.ZEROVALIDWORKS));
	}

}
