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

	private static final String cris_client_id = "APP-X7DMY3AKDXK34RVS";
	private static final String cris_client_secret = "d622a047-deef-4368-a1e8-223101911563";
	private static final String external_client_id = "APP-JFDCD0I82SXO91F9";
	private static final String external_client_secret = "a205bf62-e4b1-4d22-8a4b-2395e493358a";

	private static final String[] profiles = { "0000-0002-4464-361X",
			"0000-0002-9007-3574", "0000-0002-5507-2082", "0000-0002-9055-9726" };

	private static final String[] cris_profile_secrets = {
			"dd90ad6f-3ec2-4a0a-8762-725f95389b22",
			"944a4b86-38ef-4f2b-8c9c-305df02559db",
			"c8962118-bd00-4bd2-8784-8b7bf0c3b84b",
			"7421f8d5-3173-4344-994e-e669d991c1d9" };
	private static final String[] external_profile_secrets = {
			"ba052ca1-b65b-41d4-969a-bc97a0f67386",
			"7935697c-5c34-448a-9fad-484ad16867eb",
			"59e45d2f-d7e0-47fa-b6a1-31e0066781f3",
			"ac790728-36d6-455d-9469-deb6fbaf0589" };

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
