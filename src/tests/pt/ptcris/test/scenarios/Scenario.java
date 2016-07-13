package pt.ptcris.test.scenarios;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		for (Work work : setupORCIDFixtureWorks())
			helperFixture.addWork(work);
		for (Work work : setupORCIDWorks())
			helper.addWork(work);
		this.localWorks = setupLocalWorks();
		this.exportWorks = exportLocalWorks();
		this.localWorks.addAll(this.exportWorks);
	}

	@Test
	public void test() throws PTCRISyncException, OrcidClientException, InterruptedException {
		ProgressHandler handler = ScenariosHelper.handler();

		handler.setCurrentStatus(this.getClass().getName()+" start");
		long startTime = System.currentTimeMillis();
		Map<BigInteger, Integer> codes = PTCRISync.export(helper.client, exportWorks, handler);

		List<Work> worksToImport = PTCRISync.importWorks(helper.client, localWorks, handler);
		List<Work> worksToUpdate = PTCRISync.importUpdates(helper.client, localWorks, handler);
		List<Work> worksToInvalid = PTCRISync.importInvalid(helper.client, localWorks, handler);
		long time = System.currentTimeMillis() - startTime;
		handler.setCurrentStatus(this.getClass().getName()+": "+time+"ms");

		List<Work> allImports = new ArrayList<Work>(worksToImport);
		allImports.addAll(worksToUpdate);
		localWorks.addAll(allImports);

		List<Work> expectedLocal = expectedImportedLocalWorks();
		List<Work> expectedInvalid = expectedImportedInvalidWorks();
		List<Work> expectedORCID = expectedSourcedORCIDWorks();
		List<WorkSummary> sourcedORCID = helper.getSourcedWorkSummaries();

		assertEquals(expectedLocal.size(), allImports.size());
		assertTrue(correctImports(expectedLocal, allImports));

		assertEquals(expectedInvalid.size(), worksToInvalid.size());
		assertTrue(correctImports(expectedInvalid, worksToInvalid));
		
		assertEquals(expectedORCID.size(), sourcedORCID.size());
		assertTrue(correctCodes(codes));
		assertTrue(correctExport(expectedORCID, sourcedORCID));
	}

	@After
	public void tearDownClass() throws Exception {
		ScenariosHelper.cleanUp(helper);
		ScenariosHelper.cleanUp(helperFixture);
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
	
	List<Work> expectedImportedInvalidWorks() {
		return new ArrayList<Work>();
	}

	List<Work> exportLocalWorks() {
		return new ArrayList<Work>();
	}

	Integer expectedExportCodes(BigInteger putCode) {
		return ORCIDHelper.ADDOK;
	}

	abstract ORCIDHelper clientFixture() throws OrcidClientException;

	abstract ORCIDHelper clientSource() throws OrcidClientException;

	static boolean correctImports(Collection<Work> works1, Collection<Work> works2) {
		Set<Work> ws1 = new HashSet<Work>(works1);
		Set<Work> ws2 = new HashSet<Work>(works2);

		for (Work work1 : works1) {
			Iterator<Work> it = ws2.iterator();
			boolean found = false;
			while (it.hasNext() && !found) {
				Work work2 = it.next();
				if (ORCIDHelper.isUpToDate(work1, work2)
						&& ((work1.getPutCode() == null && work2.getPutCode() == null) || (work1.getPutCode()
								.equals(work2.getPutCode())))) {
					ws1.remove(work1);
					ws2.remove(work2);
					found = true;
				}
			}
		}
		return ws1.isEmpty() && ws2.isEmpty();
	}

	static boolean correctExport(Collection<Work> works1, Collection<WorkSummary> works2) {
		Set<Work> ws1 = new HashSet<Work>(works1);
		Set<WorkSummary> ws2 = new HashSet<WorkSummary>(works2);

		for (Work work1 : works1) {
			Iterator<WorkSummary> it = works2.iterator();
			boolean found = false;
			while (it.hasNext() && !found) {
				WorkSummary work2 = it.next();
				if (ORCIDHelper.isUpToDate(work1, work2)) {
					ws1.remove(work1);
					ws2.remove(work2);
					found = true;
				}
			}
		}
		return ws1.isEmpty() && ws2.isEmpty();
	}

	private boolean correctCodes(Map<BigInteger, Integer> codes) {
		for (BigInteger code : codes.keySet())
			if (!codes.get(code).equals(expectedExportCodes(code)))
				return false;
		return true;
	}

}
