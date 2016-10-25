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

import pt.ptcris.PTCRISync;
import pt.ptcris.PTCRISyncResult;
import pt.ptcris.handlers.ProgressHandler;
import pt.ptcris.test.TestClients;
import pt.ptcris.test.TestHelper;
import pt.ptcris.utils.ORCIDHelper;

/**
 * A PTCRISync scenario, as defined by the PTCRISync specification report. Each
 * scenario consists of a pre-state for the local CRIS and the ORCID user
 * profile, and the expected post-state after executing the synchronization
 * procedures.
 *
 * The fixture of the scenarios may involve works sourced by the local CRIS, by
 * other external services or by the user. The external sources may be simulated
 * with an additional Member API client id defined in {@link TestClients}; the
 * user sourced works, however, cannot be managed remotely, so
 * {@link TestClients} also provides a set of different user profiles, each with
 * a different fixture.
 *
 * Since modification notifications are not explicit in the current version of
 * the system, scenarios with notifications in the pre-state (7 and 16) must be
 * handled with caution.
 */

public abstract class Scenario {
	private List<Work> localWorks, exportWorks;
	private static ORCIDHelper externalClient, crisClient;

	/**
	 * Sets up the scenario by adding to the ORCID user profile the works
	 * defined as the fixture.
	 * 
	 * @throws OrcidClientException
	 *             if communication with the ORCID service failed
	 */
	@Before
	public void setUpClass() throws OrcidClientException {
		crisClient = crisClient();
		externalClient = externalClient();
		TestHelper.cleanUp(crisClient);
		TestHelper.cleanUp(externalClient);
		for (Work work : setupORCIDExternalWorks())
			externalClient.addWork(work);
		for (Work work : setupORCIDCRISWorks())
			crisClient.addWork(work);
		this.localWorks = setupLocalWorks();
		this.exportWorks = exportLocalWorks();
		this.localWorks.addAll(this.exportWorks);
	}

	/**
	 * Runs the PTCRISync synchronization procedures for the scenario and
	 * asserts their correctness.
	 * 
	 * @throws OrcidClientException
	 *             if communication with the ORCID service failed
	 * @throws InterruptedException
	 *             if the asynchronous ORCID workers were interrupted
	 */
	@Test
	public void test() throws OrcidClientException, InterruptedException {
		ProgressHandler handler = TestHelper.handler();
		handler.setCurrentStatus(this.getClass().getName() + " start");
		long startTime = System.currentTimeMillis();
		
		// run every PTCRISync procedure
		Map<BigInteger, PTCRISyncResult> codes = PTCRISync.export(crisClient.client, exportWorks, handler);

		List<Work> worksToImport = PTCRISync.importWorks(crisClient.client, localWorks, handler);
		List<Work> worksToUpdate = PTCRISync.importUpdates(crisClient.client, localWorks, handler);
		Map<Work, Set<String>> worksToInvalid = PTCRISync.importInvalid(crisClient.client, localWorks, handler);
		int worksToImportCounter = PTCRISync.importCounter(crisClient.client, localWorks, handler);
		long time = System.currentTimeMillis() - startTime;
		handler.setCurrentStatus(this.getClass().getName() + ": " + time + "ms");

		List<Work> allImports = new ArrayList<Work>(worksToImport);
		allImports.addAll(worksToUpdate);

		// retrieve the expected results
		List<Work> expectedImported = expectedImportedWorks();
		List<Work> expectedInvalid = expectedImportedInvalidWorks();
		List<Work> expectedORCID = expectedORCIDCRISWorks();
		List<WorkSummary> sourcedORCID = crisClient.getSourcedWorkSummaries();

		// assert the correction of the procedures
		assertEquals(worksToImport.size(), worksToImportCounter);

		assertEquals(expectedImported.size(), allImports.size());
		assertTrue(correctImports(expectedImported, allImports));

		assertEquals(expectedInvalid.size(), worksToInvalid.size());
		assertTrue(correctInvalids(worksToInvalid));
		assertTrue(correctImports(expectedInvalid, worksToInvalid.keySet()));

		assertEquals(expectedORCID.size(), sourcedORCID.size());
		assertTrue(correctCodes(codes));
		assertTrue(correctExport(expectedORCID, sourcedORCID));
	}

	/**
	 * Tears down the scenario by removing every work from the ORCID user
	 * profile.
	 * 
	 * @throws OrcidClientException
	 *             if communication with the ORCID service failed
	 */
	@After
	public void tearDownClass() throws OrcidClientException {
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
	int expectedExportCodes(BigInteger putcode) {
		return PTCRISyncResult.ADDOK;
	}

	/**
	 * The new works that are expected to be detected in ORCID by the
	 * {@link PTCRISync#importWorks(pt.ptcris.ORCIDClient, List, ProgressHandler)
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
	abstract ORCIDHelper crisClient();

	/**
	 * Sets the client to use as an external ORCID source.
	 * 
	 * @return the external source client
	 */
	abstract ORCIDHelper externalClient();

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
				if (ORCIDHelper.isUpToDate(work1, work2)) {
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
	private boolean correctCodes(Map<BigInteger, PTCRISyncResult> results) {
		for (BigInteger id : results.keySet())
			if (!(results.get(id).code == expectedExportCodes(id)))
				return false;
		return true;
	}

	/**
	 * Tests whether the effectively imported works are the ones expected.
	 * 
	 * @param imported
	 *            the effectively imported works
	 * @param expected
	 *            the expected works
	 * @return whether the imported works were the expected
	 */
	private static boolean correctImports(Collection<Work> imported, Collection<Work> expected) {
		Set<Work> ws1 = new HashSet<Work>(imported);
		Set<Work> ws2 = new HashSet<Work>(expected);

		for (Work work1 : imported) {
			BigInteger localKey1 = ORCIDHelper.getActivityLocalKey(work1);
			Iterator<Work> it = ws2.iterator();
			boolean found = false;
			while (it.hasNext() && !found) {
				Work work2 = it.next();
				BigInteger localKey2 = ORCIDHelper.getActivityLocalKey(work2);
				if (ORCIDHelper.isUpToDate(work1, work2)
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
