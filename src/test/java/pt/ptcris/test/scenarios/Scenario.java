/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris.test.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import org.um.dsi.gavea.orcid.model.activities.WorkGroup;
import org.um.dsi.gavea.orcid.model.common.WorkType;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

import pt.ptcris.PTCRISync;
import pt.ptcris.PTCRISyncResult;
import pt.ptcris.handlers.ProgressHandler;
import pt.ptcris.test.TestHelper;
import pt.ptcris.utils.ORCIDHelper;
import pt.ptcris.utils.ORCIDWorkHelper;

/**
 * Represents a scenario as defined in the PTCRISync specification. Each
 * scenario consists of a pre-state for the CRIS and ORCID profiles, an update
 * on either profile and the result of running the import and/or export
 * procedures to restore consistency.
 * 
 * TODO: Scenarios with notifications in the pre-state (7 and 16) must be
 * handled with caution, since modifications are not explicit in the current
 * version of the system.
 * 
 * TODO: Scenarios that consider the promotion/demotion of preferred works (10
 * and 11) are not to be tested programmatically as they cannot be selected this
 * way.
 * 
 * @see <a href="https://ptcris.pt/hub-ptcris/">https://ptcris.pt/hub-ptcris/</a>
 */
public abstract class Scenario {
	private List<Work> localWorks, exportWorks;
	private static ORCIDHelper<Work,WorkSummary,WorkGroup,WorkType> externalClient, crisClient;

	@Before
	public void setUpClass() throws Exception {
		crisClient = crisClient();
		externalClient = externalClient();
		TestHelper.cleanUp(crisClient);
		TestHelper.cleanUp(externalClient);
		externalClient.add(setupORCIDExternalWorks(),null);
		crisClient.add(setupORCIDCRISWorks(),null);
		this.localWorks = setupLocalWorks();
		this.exportWorks = exportLocalWorks();
		this.localWorks.addAll(this.exportWorks);
	}

	@Test
	public void test() throws OrcidClientException, InterruptedException {
		ProgressHandler handler = TestHelper.handler();

		handler.setCurrentStatus(this.getClass().getName() + " start");
		long startTime = System.currentTimeMillis();
		Map<BigInteger, PTCRISyncResult<Work>> codes = PTCRISync.exportWorks(crisClient.client, exportWorks, handler);

		List<Work> worksToImport = PTCRISync.importWorks(crisClient.client, localWorks, handler);
		List<Work> worksToUpdate = PTCRISync.importWorkUpdates(crisClient.client, localWorks, handler);
		Map<Work, Set<String>> worksToInvalid = PTCRISync.importInvalidWorks(
				crisClient.client, localWorks, handler);
		int worksToImportCounter = PTCRISync.importWorkCounter(crisClient.client, localWorks, handler);
		long time = System.currentTimeMillis() - startTime;
		handler.setCurrentStatus(this.getClass().getName() + ": " + time + "ms");

		List<Work> allImports = new ArrayList<Work>(worksToImport);
		allImports.addAll(worksToUpdate);
		localWorks.addAll(allImports);

		List<Work> expectedLocal = expectedImportedWorks();
		List<Work> expectedInvalid = expectedImportedInvalidWorks();
		List<Work> expectedORCID = expectedORCIDCRISWorks();
		List<WorkSummary> sourcedORCID = crisClient.getSourcedSummaries();

		assertEquals(worksToImport.size(), worksToImportCounter);

		assertEquals(expectedLocal.size(), allImports.size());
		assertTrue(correctImports(expectedLocal, allImports));

		assertEquals(expectedInvalid.size(), worksToInvalid.size());
		assertTrue(correctInvalids(worksToInvalid));
		assertTrue(correctImports(expectedInvalid, worksToInvalid.keySet()));

		assertEquals(expectedORCID.size(), sourcedORCID.size());
		assertTrue(correctCodes(codes));
		assertTrue(correctExport(expectedORCID, sourcedORCID));
	}

	@After
	public void tearDownClass() throws Exception {
		TestHelper.cleanUp(crisClient);
		TestHelper.cleanUp(externalClient);
	}

	/**
	 * Defines the CRIS-sourced ORCID works for the fixture of the scenario,
	 * i.e., the works that initially exist in the user profile added by the
	 * CRIS.
	 * 
	 * @return the set of CRIS works for the fixture
	 */
	List<Work> setupORCIDCRISWorks() {
		return new ArrayList<Work>();
	}

	/**
	 * Defines the non CRIS-sourced ORCID works for the fixture of the scenario,
	 * i.e., the works that initially exist in the user profile added by other
	 * external sources.
	 * 
	 * @return the external works for the fixture
	 */
	List<Work> setupORCIDExternalWorks() {
		return new ArrayList<Work>();
	}

	/**
	 * Defines the local CRIS works that are not to be exported. The import
	 * tests will additionally consider those set to be
	 * {@link #exportLocalWorks() exported}.
	 * 
	 * @return the local works that is not to be exported
	 */
	List<Work> setupLocalWorks() {
		return new ArrayList<Work>();
	}

	/**
	 * Defines the local CRIS works that are to be exported. The import tests
	 * will additionally consider those not set to be {@link #setupLocalWorks()
	 * exported}.
	 * 
	 * @return the local works that is to be exported
	 */
	List<Work> exportLocalWorks() {
		return new ArrayList<Work>();
	}

	/**
	 * The CRIS-source works that are expected to be in ORCID after the
	 * {@link PTCRISync#export(pt.ptcris.ORCIDClient, List, ProgressHandler)
	 * export} synchronization procedure.
	 * 
	 * @return the expected CRIS-source works
	 */
	List<Work> expectedORCIDCRISWorks() {
		return new ArrayList<Work>();
	}

	/**
	 * The expected outcome for each of the works that were to be exported for
	 * the
	 * {@link PTCRISync#export(pt.ptcris.ORCIDClient, List, ProgressHandler)
	 * export} synchronization procedure.
	 * 
	 * @param putcode
	 *            the put-code of a work provided by {#link
	 *            {@link #exportLocalWorks()}
	 * @return the expected CRIS-source works
	 */
	Set<Integer> expectedExportCodes(BigInteger putcode) {
		Set<Integer> res = new HashSet<Integer>();
		res.add(PTCRISyncResult.ADDOK);
		return res;
	}

	/**
	 * The new works that are expected to be detected in ORCID by the
	 * {@link PTCRISync#importNews(pt.ptcris.ORCIDClient, List, ProgressHandler)
	 * import} synchronization procedure.
	 * 
	 * @return the expected CRIS-source works
	 */
	List<Work> expectedImportedWorks() {
		return new ArrayList<Work>();
	}

	/**
	 * The new invalid works that are expected to be detected in ORCID by the
	 * {@link PTCRISync#importInvalid(pt.ptcris.ORCIDClient, List, ProgressHandler)
	 * import invalid} synchronization procedure.
	 * 
	 * @return the expected CRIS-source works
	 */
	List<Work> expectedImportedInvalidWorks() {
		return new ArrayList<Work>();
	}

	/**
	 * The expected invalidity reasons for the new invalid works detected in
	 * ORCID by the
	 * {@link PTCRISync#importInvalid(pt.ptcris.ORCIDClient, List, ProgressHandler)
	 * import invalid} synchronization procedure.
	 * 
	 * @param putcode
	 *            the put-code of a work returned by {#link
	 *            {@link #expectedImportedInvalidWorks()}
	 * @return the expected reasons for invalidity
	 */
	Set<String> expectedInvalidCodes(BigInteger putcode) {
		return new HashSet<String>();
	}

	/**
	 * Sets the client to use as the CRIS source.
	 * 
	 * @return the CRIS source client
	 */
	abstract ORCIDHelper<Work,WorkSummary,WorkGroup,WorkType> crisClient();

	/**
	 * Sets the client to use as an external ORCID source.
	 * 
	 * @return the external source client
	 */
	abstract ORCIDHelper<Work,WorkSummary,WorkGroup,WorkType> externalClient();

	/**
	 * Tests whether the effectively exported works are the ones expected.
	 * 
	 * @param exported
	 *            the effectively exported works
	 * @param expected
	 *            the expected works
	 * @return whether the exported works were the expected
	 */
	private static boolean correctExport(Collection<Work> exported, Collection<WorkSummary> expected) {
		Set<Work> ws1 = new HashSet<Work>(exported);
		Set<WorkSummary> ws2 = new HashSet<WorkSummary>(expected);

		for (Work work1 : exported) {
			Iterator<WorkSummary> it = expected.iterator();
			boolean found = false;
			while (it.hasNext() && !found) {
				WorkSummary work2 = it.next();
				if (new ORCIDWorkHelper(null).isUpToDateS(work1, work2)) {
					ws1.remove(work1);
					ws2.remove(work2);
					found = true;
				}
			}
		}
		return ws1.isEmpty() && ws2.isEmpty();
	}

	/**
	 * Tests whether the effectively export outcomes are the ones
	 * expected according to {@link #expectedExportCodes(BigInteger)}.
	 * 
	 * @param reasons
	 *            the effectively export outcome
	 * @return whether the export outcome was the expected
	 */
	private boolean correctCodes(Map<BigInteger, PTCRISyncResult<Work>> results) {
		for (BigInteger id : results.keySet())
			if (!expectedExportCodes(id).contains(results.get(id).code)) {
				TestHelper.handler().sendError("Was "+results.get(id).code);
				return false;
			}
		return true;
	}

	/**
	 * Tests whether the effectively imported works are the ones expected.
	 * 
	 * @param expected
	 *            the effectively imported works
	 * @param imported
	 *            the expected works
	 * @return whether the imported works were the expected
	 */
	private static boolean correctImports(Collection<Work> expected, Collection<Work> imported) {
		Set<Work> ws1 = new HashSet<Work>(expected);
		Set<Work> ws2 = new HashSet<Work>(imported);

		for (Work work1 : expected) {
			BigInteger localKey1 = ORCIDHelper.getActivityLocalKey(work1);
			Iterator<Work> it = ws2.iterator();
			boolean found = false;
			while (it.hasNext() && !found) {
				Work work2 = it.next();
				BigInteger localKey2 = ORCIDHelper.getActivityLocalKey(work2);
				if (new ORCIDWorkHelper(null).isUpToDateE(work1, work2)
						&& ((localKey1 == null && localKey2 == null) || (localKey1.equals(localKey2)))) {
					ws1.remove(work1);
					ws2.remove(work2);
					found = true;
				}
			}
		}
		return ws1.isEmpty() && ws2.isEmpty();
	}

	/**
	 * Tests whether the effectively imported invalidity reasons are the ones
	 * expected according to {@link #expectedInvalidCodes(BigInteger)}.
	 * 
	 * @param reasons
	 *            the effectively imported reasons for invalidity
	 * @return whether the invalidity reasons were the expected
	 */
	private boolean correctInvalids(Map<Work, Set<String>> codes) {
		for (Work work : codes.keySet())
			if (!codes.get(work).equals(expectedInvalidCodes(work.getPutCode())))
				return false;
		return true;
	}

}
