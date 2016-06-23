package pt.ptcris.test.scenarios;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

import pt.ptcris.PTCRISyncException;
import pt.ptcris.ORCIDHelper;
import pt.ptcris.PTCRISync;
import pt.ptcris.handlers.ProgressHandler;

/* TODO: Scenarios with notifications in the pre-state (7 and 16) must 
 * be handled with caution, since modifications are not explicit in the 
 * current version of the system. */

public abstract class Scenario {
	private List<Work> localWorks, exportWorks;
	private static ORCIDHelper helperFixture, helper;

	@Before
	public void setUpClass() throws Exception {
		helper = clientSource();
		helperFixture = clientFixture();
		ScenariosHelper.cleanUp(helper);
		ScenariosHelper.cleanUp(helperFixture);
		helper.waitWorkers();
		helperFixture.waitWorkers();
		for (Work work : setupORCIDFixtureWorks())
			helperFixture.addWork(work);
		for (Work work : setupORCIDWorks())
			helper.addWork(work);
		this.localWorks = setupLocalWorks();
		this.exportWorks = exportLocalWorks();
		this.localWorks.addAll(this.exportWorks);
		helper.waitWorkers();
		helperFixture.waitWorkers();
	}

	@Test
	public void test() throws PTCRISyncException, OrcidClientException, InterruptedException {
		ProgressHandler handler = ScenariosHelper.handler();

		long startTime = System.currentTimeMillis();
		PTCRISync.export(helper.client, exportWorks, handler);

		List<Work> worksToImport = PTCRISync.importWorks(helper.client, localWorks, handler);
		List<Work> worksToUpdate = PTCRISync.importUpdates(helper.client, localWorks, handler);
		System.out.println(System.currentTimeMillis() - startTime);

		List<Work> allImports = new ArrayList<Work>(worksToImport);
		allImports.addAll(worksToUpdate);
		localWorks.addAll(allImports);

		handler.setCurrentStatus(localWorks.toString());

		List<Work> expectedLocal = expectedImportedLocalWorks();
		List<Work> expectedORCID = expectedSourcedORCIDWorks();
		List<WorkSummary> sourcedORCID = helper.getSourcedWorkSummaries();

		assertEquals(expectedLocal.size(), allImports.size());
		assertTrue(ScenariosHelper.correctImports(expectedLocal, allImports));

		assertEquals(expectedORCID.size(), sourcedORCID.size());
		assertTrue(ScenariosHelper.correctExport(expectedORCID, sourcedORCID));
	}

	@After
	public void tearDownClass() throws Exception {
		ScenariosHelper.cleanUp(helper);
		ScenariosHelper.cleanUp(helperFixture);
		helper.waitWorkers();
		helperFixture.waitWorkers();
	}

	List<Work> setupORCIDWorks() {
		return new ArrayList<Work>();
	}

	List<Work> setupORCIDFixtureWorks() {
		return new ArrayList<Work>();
	}

	List<Work> setupLocalWorks() {
		return new ArrayList<Work>();
	}

	List<Work> expectedSourcedORCIDWorks() {
		return new ArrayList<Work>();
	}

	List<Work> expectedImportedLocalWorks() {
		return new ArrayList<Work>();
	}

	List<Work> exportLocalWorks() {
		return new ArrayList<Work>();
	}

	abstract ORCIDHelper clientFixture() throws OrcidClientException;

	abstract ORCIDHelper clientSource() throws OrcidClientException;

}
