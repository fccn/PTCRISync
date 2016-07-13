package pt.ptcris.test.scenarios;

import java.util.ArrayList;
import java.util.List;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.ORCIDHelper;

public class ScenarioPerformance extends Scenario {

	@Override
	List<Work> setupORCIDWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOI(null, "0", "20"));
		works.add(ScenariosHelper.workDOI(null, "1", "21"));
		works.add(ScenariosHelper.workDOI(null, "2", "22"));
		works.add(ScenariosHelper.workDOI(null, "3", "23"));
		works.add(ScenariosHelper.workDOI(null, "4", "24"));
		works.add(ScenariosHelper.workDOI(null, "5", "25"));
		works.add(ScenariosHelper.workDOI(null, "6", "26"));
		works.add(ScenariosHelper.workDOI(null, "7", "27"));
		works.add(ScenariosHelper.workDOI(null, "8", "28"));
		works.add(ScenariosHelper.workDOI(null, "9", "29"));
		works.add(ScenariosHelper.workDOI(null, "0", "30"));
		works.add(ScenariosHelper.workDOI(null, "1", "31"));
		works.add(ScenariosHelper.workDOI(null, "2", "32"));
		works.add(ScenariosHelper.workDOI(null, "3", "33"));
		works.add(ScenariosHelper.workDOI(null, "4", "34"));
		works.add(ScenariosHelper.workDOI(null, "5", "35"));
		works.add(ScenariosHelper.workDOI(null, "6", "36"));
		works.add(ScenariosHelper.workDOI(null, "7", "37"));
		works.add(ScenariosHelper.workDOI(null, "8", "38"));
		works.add(ScenariosHelper.workDOI(null, "9", "39"));
		return works;
	}

	@Override
	List<Work> setupORCIDFixtureWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOI(null, "0", "0"));
		works.add(ScenariosHelper.workDOI(null, "1", "1"));
		works.add(ScenariosHelper.workDOI(null, "2", "2"));
		works.add(ScenariosHelper.workDOI(null, "3", "3"));
		works.add(ScenariosHelper.workDOI(null, "4", "4"));
		works.add(ScenariosHelper.workDOI(null, "5", "5"));
		works.add(ScenariosHelper.workDOI(null, "6", "6"));
		works.add(ScenariosHelper.workDOI(null, "7", "7"));
		works.add(ScenariosHelper.workDOI(null, "8", "8"));
		works.add(ScenariosHelper.workDOI(null, "9", "9"));
		works.add(ScenariosHelper.workDOI(null, "0", "10"));
		works.add(ScenariosHelper.workDOI(null, "1", "11"));
		works.add(ScenariosHelper.workDOI(null, "2", "12"));
		works.add(ScenariosHelper.workDOI(null, "3", "13"));
		works.add(ScenariosHelper.workDOI(null, "4", "14"));
		works.add(ScenariosHelper.workDOI(null, "5", "15"));
		works.add(ScenariosHelper.workDOI(null, "6", "16"));
		works.add(ScenariosHelper.workDOI(null, "7", "17"));
		works.add(ScenariosHelper.workDOI(null, "8", "18"));
		works.add(ScenariosHelper.workDOI(null, "9", "19"));
		works.add(ScenariosHelper.workDOI(null, "0", "20"));
		works.add(ScenariosHelper.workDOI(null, "1", "21"));
		works.add(ScenariosHelper.workDOI(null, "2", "22"));
		works.add(ScenariosHelper.workDOI(null, "3", "23"));
		works.add(ScenariosHelper.workDOI(null, "4", "24"));
		works.add(ScenariosHelper.workDOI(null, "5", "25"));
		works.add(ScenariosHelper.workDOI(null, "6", "26"));
		works.add(ScenariosHelper.workDOI(null, "7", "27"));
		works.add(ScenariosHelper.workDOI(null, "8", "28"));
		works.add(ScenariosHelper.workDOI(null, "9", "29"));
		return works;
	}

	@Override
	List<Work> expectedImportedLocalWorks() {
		List<Work> works = new ArrayList<Work>();
		works.add(ScenariosHelper.workDOI(null, "0", "0"));
		works.add(ScenariosHelper.workDOI(null, "1", "1"));
		works.add(ScenariosHelper.workDOI(null, "2", "2"));
		works.add(ScenariosHelper.workDOI(null, "3", "3"));
		works.add(ScenariosHelper.workDOI(null, "4", "4"));
		works.add(ScenariosHelper.workDOI(null, "5", "5"));
		works.add(ScenariosHelper.workDOI(null, "6", "6"));
		works.add(ScenariosHelper.workDOI(null, "7", "7"));
		works.add(ScenariosHelper.workDOI(null, "8", "8"));
		works.add(ScenariosHelper.workDOI(null, "9", "9"));
		works.add(ScenariosHelper.workDOI(null, "0", "10"));
		works.add(ScenariosHelper.workDOI(null, "1", "11"));
		works.add(ScenariosHelper.workDOI(null, "2", "12"));
		works.add(ScenariosHelper.workDOI(null, "3", "13"));
		works.add(ScenariosHelper.workDOI(null, "4", "14"));
		works.add(ScenariosHelper.workDOI(null, "5", "15"));
		works.add(ScenariosHelper.workDOI(null, "6", "16"));
		works.add(ScenariosHelper.workDOI(null, "7", "17"));
		works.add(ScenariosHelper.workDOI(null, "8", "18"));
		works.add(ScenariosHelper.workDOI(null, "9", "19"));
		works.add(ScenariosHelper.workDOI(null, "0", "20"));
		works.add(ScenariosHelper.workDOI(null, "1", "21"));
		works.add(ScenariosHelper.workDOI(null, "2", "22"));
		works.add(ScenariosHelper.workDOI(null, "3", "23"));
		works.add(ScenariosHelper.workDOI(null, "4", "24"));
		works.add(ScenariosHelper.workDOI(null, "5", "25"));
		works.add(ScenariosHelper.workDOI(null, "6", "26"));
		works.add(ScenariosHelper.workDOI(null, "7", "27"));
		works.add(ScenariosHelper.workDOI(null, "8", "28"));
		works.add(ScenariosHelper.workDOI(null, "9", "29"));
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
	ORCIDHelper clientSource() throws OrcidClientException {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWork(0));
	}

	@Override
	ORCIDHelper clientFixture() throws OrcidClientException {
		return new ORCIDHelper(ScenarioOrcidClient.getClientWorkFixture(0));
	}

}
