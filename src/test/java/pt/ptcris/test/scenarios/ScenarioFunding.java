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
import org.um.dsi.gavea.orcid.model.funding.Funding;
import org.um.dsi.gavea.orcid.model.funding.FundingSummary;

import pt.ptcris.PTCRISync;
import pt.ptcris.PTCRISyncResult;
import pt.ptcris.handlers.ProgressHandler;
import pt.ptcris.test.TestHelper;
import pt.ptcris.utils.ORCIDHelper;

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
 * TODO: Scenarios that consider the promotion/demotion of preferred fundings (10
 * and 11) are not to be tested programmatically as they cannot be selected this
 * way.
 * 
 * @see <a href="https://ptcris.pt/hub-ptcris/">https://ptcris.pt/hub-ptcris/</a>
 */
public abstract class ScenarioFunding {
	private List<Funding> localFundings, exportFundings;
	private static ORCIDHelper externalClient, crisClient;

	@Before
	public void setUpClass() throws Exception {
		crisClient = crisClient();
		externalClient = externalClient();
		TestHelper.cleanUp(crisClient);
		TestHelper.cleanUp(externalClient);
		externalClient.addFundings(setupORCIDExternalFundings(),null);
		crisClient.addFundings(setupORCIDCRISFundings(),null);
		this.localFundings = setupLocalFundings();
		this.exportFundings = exportLocalFundings();
		this.localFundings.addAll(this.exportFundings);
	}

	@Test
	public void test() throws OrcidClientException, InterruptedException {
		ProgressHandler handler = TestHelper.handler();

		handler.setCurrentStatus(this.getClass().getName() + " start");
		long startTime = System.currentTimeMillis();
//		Map<BigInteger, PTCRISyncResult> codes = PTCRISync.export(crisClient.client, exportFundings, handler);

		List<Funding> fundingsToImport = PTCRISync.importFundings(crisClient.client, localFundings, handler);
		List<Funding> fundingsToUpdate = PTCRISync.importFundingUpdates(crisClient.client, localFundings, handler);
		Map<Funding, Set<String>> fundingsToInvalid = PTCRISync.importFundingInvalid(
				crisClient.client, localFundings, handler);
		int fundingsToImportCounter = PTCRISync.importFundingCounter(crisClient.client, localFundings, handler);
		long time = System.currentTimeMillis() - startTime;
		handler.setCurrentStatus(this.getClass().getName() + ": " + time + "ms");

		List<Funding> allImports = new ArrayList<Funding>(fundingsToImport);
		allImports.addAll(fundingsToUpdate);
		localFundings.addAll(allImports);

		List<Funding> expectedLocal = expectedImportedFundings();
		List<Funding> expectedInvalid = expectedImportedInvalidFundings();
		List<Funding> expectedORCID = expectedORCIDCRISFundings();
		List<FundingSummary> sourcedORCID = crisClient.getSourcedFundingSummaries();

		assertEquals(fundingsToImport.size(), fundingsToImportCounter);

		assertEquals(expectedLocal.size(), allImports.size());
		assertTrue(correctImports(expectedLocal, allImports));

		assertEquals(expectedInvalid.size(), fundingsToInvalid.size());
		assertTrue(correctInvalids(fundingsToInvalid));
		assertTrue(correctImports(expectedInvalid, fundingsToInvalid.keySet()));

		assertEquals(expectedORCID.size(), sourcedORCID.size());
//		assertTrue(correctCodes(codes));
		assertTrue(correctExport(expectedORCID, sourcedORCID));
	}

	@After
	public void tearDownClass() throws Exception {
		TestHelper.cleanUp(crisClient);
		TestHelper.cleanUp(externalClient);
	}

	/**
	 * Defines the CRIS-sourced ORCID fundings for the fixture of the scenario,
	 * i.e., the fundings that initially exist in the user profile added by the
	 * CRIS.
	 * 
	 * @return the set of CRIS fundings for the fixture
	 */
	List<Funding> setupORCIDCRISFundings() {
		return new ArrayList<Funding>();
	}

	/**
	 * Defines the non CRIS-sourced ORCID fundings for the fixture of the scenario,
	 * i.e., the fundings that initially exist in the user profile added by other
	 * external sources.
	 * 
	 * @return the external fundings for the fixture
	 */
	List<Funding> setupORCIDExternalFundings() {
		return new ArrayList<Funding>();
	}

	/**
	 * Defines the local CRIS fundings that are not to be exported. The import
	 * tests will additionally consider those set to be
	 * {@link #exportLocalFundings() exported}.
	 * 
	 * @return the local fundings that is not to be exported
	 */
	List<Funding> setupLocalFundings() {
		return new ArrayList<Funding>();
	}

	/**
	 * Defines the local CRIS fundings that are to be exported. The import tests
	 * will additionally consider those not set to be {@link #setupLocalFundings()
	 * exported}.
	 * 
	 * @return the local fundings that is to be exported
	 */
	List<Funding> exportLocalFundings() {
		return new ArrayList<Funding>();
	}

	/**
	 * The CRIS-source fundings that are expected to be in ORCID after the
	 * {@link PTCRISync#export(pt.ptcris.ORCIDClient, List, ProgressHandler)
	 * export} synchronization procedure.
	 * 
	 * @return the expected CRIS-source fundings
	 */
	List<Funding> expectedORCIDCRISFundings() {
		return new ArrayList<Funding>();
	}

	/**
	 * The expected outcome for each of the fundings that were to be exported for
	 * the
	 * {@link PTCRISync#export(pt.ptcris.ORCIDClient, List, ProgressHandler)
	 * export} synchronization procedure.
	 * 
	 * @param putcode
	 *            the put-code of a funding provided by {#link
	 *            {@link #exportLocalFundings()}
	 * @return the expected CRIS-source fundings
	 */
	Set<Integer> expectedExportCodes(BigInteger putcode) {
		Set<Integer> res = new HashSet<Integer>();
		res.add(PTCRISyncResult.ADDOK);
		return res;
	}

	/**
	 * The new fundings that are expected to be detected in ORCID by the
	 * {@link PTCRISync#importFundings(pt.ptcris.ORCIDClient, List, ProgressHandler)
	 * import} synchronization procedure.
	 * 
	 * @return the expected CRIS-source fundings
	 */
	List<Funding> expectedImportedFundings() {
		return new ArrayList<Funding>();
	}

	/**
	 * The new invalid fundings that are expected to be detected in ORCID by the
	 * {@link PTCRISync#importInvalid(pt.ptcris.ORCIDClient, List, ProgressHandler)
	 * import invalid} synchronization procedure.
	 * 
	 * @return the expected CRIS-source fundings
	 */
	List<Funding> expectedImportedInvalidFundings() {
		return new ArrayList<Funding>();
	}

	/**
	 * The expected invalidity reasons for the new invalid fundings detected in
	 * ORCID by the
	 * {@link PTCRISync#importInvalid(pt.ptcris.ORCIDClient, List, ProgressHandler)
	 * import invalid} synchronization procedure.
	 * 
	 * @param putcode
	 *            the put-code of a funding returned by {#link
	 *            {@link #expectedImportedInvalidFundings()}
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
	 * Tests whether the effectively exported fundings are the ones expected.
	 * 
	 * @param exported
	 *            the effectively exported fundings
	 * @param expected
	 *            the expected fundings
	 * @return whether the exported fundings were the expected
	 */
	private static boolean correctExport(Collection<Funding> exported, Collection<FundingSummary> expected) {
		Set<Funding> ws1 = new HashSet<Funding>(exported);
		Set<FundingSummary> ws2 = new HashSet<FundingSummary>(expected);

		for (Funding funding1 : exported) {
			Iterator<FundingSummary> it = expected.iterator();
			boolean found = false;
			while (it.hasNext() && !found) {
				FundingSummary funding2 = it.next();
				if (ORCIDHelper.isUpToDate(funding1, funding2)) {
					ws1.remove(funding1);
					ws2.remove(funding2);
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
			if (!expectedExportCodes(id).contains(results.get(id).code)) {
				TestHelper.handler().sendError("Was "+results.get(id).code);
				return false;
			}
		return true;
	}

	/**
	 * Tests whether the effectively imported fundings are the ones expected.
	 * 
	 * @param expected
	 *            the effectively imported fundings
	 * @param imported
	 *            the expected fundings
	 * @return whether the imported fundings were the expected
	 */
	private static boolean correctImports(Collection<Funding> expected, Collection<Funding> imported) {
		Set<Funding> ws1 = new HashSet<Funding>(expected);
		Set<Funding> ws2 = new HashSet<Funding>(imported);

		for (Funding funding1 : expected) {
			BigInteger localKey1 = ORCIDHelper.getActivityLocalKey(funding1);
			Iterator<Funding> it = ws2.iterator();
			boolean found = false;
			while (it.hasNext() && !found) {
				Funding funding2 = it.next();
				BigInteger localKey2 = ORCIDHelper.getActivityLocalKey(funding2);
				if (ORCIDHelper.isUpToDate(funding1, funding2)
						&& ((localKey1 == null && localKey2 == null) || (localKey1.equals(localKey2)))) {
					ws1.remove(funding1);
					ws2.remove(funding2);
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
	private boolean correctInvalids(Map<Funding, Set<String>> codes) {
		for (Funding funding : codes.keySet())
			if (!codes.get(funding).equals(expectedInvalidCodes(funding.getPutCode())))
				return false;
		return true;
	}

}
