/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris.test;

import org.um.dsi.gavea.orcid.client.OrcidAccessToken;

import pt.ptcris.ORCIDClient;
import pt.ptcris.ORCIDClientImpl;

/**
 * Helper class defining a set of sandbox ORCID profiles, with varied
 * user-inserted productions, that can be used for testing. It also contains
 * access tokens for two different Member API clients, that can represent the
 * local CRIS service and an external source.
 */
public final class TestClients {

	/**
	 * The user profiles registered in the sandbox.
	 */
	public enum Profile {
		ZEROVALIDWORKS(0), ONEVALIDWORKS(1), TWOVALIDWORKS(2), EMPTYWORKS(3);

		private final int value;

		private Profile(int value) {
			this.value = value;
		}
	}

	private static final String orcid_login_uri = "https://sandbox.orcid.org";
	private static final String orcid_api_uri = "https://api.sandbox.orcid.org/";
	private static final String orcid_redirect_uri = "https://developers.google.com/oauthplayground";

	private static final String cris_client_id = "APP-X7DMY3AKDXK34RVS"; //PTCRIS
	private static final String cris_client_secret = "d622a047-deef-4368-a1e8-223101911563";
	private static final String external_client_id = "APP-JFDCD0I82SXO91F9"; //HASLab, INESC TEC & University of Minho
	private static final String external_client_secret = "a205bf62-e4b1-4d22-8a4b-2395e493358a";

	private static final String[] profiles = { "0000-0003-4777-9763",
			"0000-0002-1811-9160", "0000-0002-3505-9366", "0000-0002-6134-4419" };
			//ZECA					//MANEL					//Toze

	private static final String[] cris_profile_secrets = {
			"ea00bc47-1541-4824-a439-bf4feee40248",
			"118f715f-058b-42ff-96a4-8ae015ca53f3",
			"ebe045ed-497c-477f-b41b-5e0dee768857",
			"e7e76ead-26e0-4d51-b51d-90a5c4085950" };
	private static final String[] external_profile_secrets = {
			"f0c619fe-07f6-4713-bc06-02a5aa66c640",
			"70022d8d-4bec-400e-9181-4dca0233ce2a",
			"54d08408-146f-4150-a360-ee65a2fd8f90",
			"34ac7d34-8b95-48e2-b16c-68af144b7a00" };

	/**
	 * Retrieves an ORCID client for a given user profile using the local CRIS
	 * Member API client id.
	 * 
	 * @param profile
	 *            the ORCID user profile
	 * @return the client for the local CRIS source to manage the user profile
	 */
	public static ORCIDClient getCRISClient(Profile profile) {
		ORCIDClientImpl orcidClient = new ORCIDClientImpl(
				orcid_login_uri, orcid_api_uri, cris_client_id, cris_client_secret, orcid_redirect_uri,
				TestClients.getCRISAccessToken(profile.value));
		return orcidClient;
	}

	/**
	 * Retrieves an ORCID client for a given user profile using the a Member API
	 * client id that is not the local CRIS (used to simulate an external
	 * source).
	 * 
	 * @param profile
	 *            the ORCID user profile
	 * @return the client for the external source to manage the user profile
	 */
	public static ORCIDClient getExternalClient(Profile profile) {
		ORCIDClientImpl orcidClient = new ORCIDClientImpl(
				orcid_login_uri, orcid_api_uri, external_client_id, external_client_secret, orcid_redirect_uri,
				TestClients.getExternalAccessToken(profile.value));
		return orcidClient;
	}

	/**
	 * Retrieves the access token to a given user profile for the external
	 * source Member API client id.
	 * 
	 * @param i
	 *            the user profile
	 * @return the access token for the external source to manage the user
	 *         profile
	 */
	private static OrcidAccessToken getCRISAccessToken(int i) {
		OrcidAccessToken orcidToken = new OrcidAccessToken();

		orcidToken.setAccess_token(cris_profile_secrets[i]);
		orcidToken.setOrcid(profiles[i]);

		return orcidToken;
	}

	/**
	 * Retrieves the access token to a given user profile for the local CRIS
	 * Member API client id.
	 * 
	 * @param i
	 *            the user profile
	 * @return the access token for local CRIS to manage the user profile
	 */
	private static OrcidAccessToken getExternalAccessToken(int i) {
		OrcidAccessToken orcidToken = new OrcidAccessToken();
	
		orcidToken.setAccess_token(external_profile_secrets[i]);
		orcidToken.setOrcid(profiles[i]);
	
		return orcidToken;
	}

}
