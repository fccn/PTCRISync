package pt.ptcris.test.scenarios;

import org.um.dsi.gavea.orcid.client.OrcidAccessToken;

import pt.ptcris.ORCIDClient;
import pt.ptcris.ORCIDClientImpl;

public class ScenarioOrcidClient {

	private static final String orcid_login_uri = "https://sandbox.orcid.org";
	private static final String orcid_api_uri = "https://api.sandbox.orcid.org/";
	private static final String orcid_redirect_uri = "https://developers.google.com/oauthplayground";

	private static final String orcid_client_id_fixture = "APP-JFDCD0I82SXO91F9";
	private static final String orcid_client_secret_fixture = "a205bf62-e4b1-4d22-8a4b-2395e493358a";

	private static final String orcid_client_id = "APP-X7DMY3AKDXK34RVS";
	private static final String orcid_client_secret = "d622a047-deef-4368-a1e8-223101911563";

	private static final String[] orcid_profile_work = { "0000-0002-4464-361X", "0000-0003-3351-0229",
			"0000-0002-5507-2082" };
	private static final String[] orcid_profile_work_secret = { "dd90ad6f-3ec2-4a0a-8762-725f95389b22",
			"e49393b9-9494-4085-bf71-3c6bb03f3873", "c8962118-bd00-4bd2-8784-8b7bf0c3b84b" };
	private static final String[] orcid_profile_work_secret_fixture = { "ba052ca1-b65b-41d4-969a-bc97a0f67386",
			"7b8b8632-62b8-4015-a34a-c03a297a2ddf", "59e45d2f-d7e0-47fa-b6a1-31e0066781f3" };

	public static ORCIDClient getClientWork(int i) {
		ORCIDClientImpl orcidClient = new ORCIDClientImpl(orcid_login_uri, orcid_api_uri, orcid_client_id,
				orcid_client_secret, orcid_redirect_uri, ScenarioOrcidClient.getAccessTokenWork(i));
		return orcidClient;
	}

	public static ORCIDClient getClientWorkFixture(int i) {
		ORCIDClientImpl orcidClient = new ORCIDClientImpl(orcid_login_uri, orcid_api_uri, orcid_client_id_fixture,
				orcid_client_secret_fixture, orcid_redirect_uri, ScenarioOrcidClient.getAccessTokenWorkFixture(i));
		return orcidClient;
	}

	private static OrcidAccessToken getAccessTokenWorkFixture(int i) {
		OrcidAccessToken orcidToken = new OrcidAccessToken();

		orcidToken.setAccess_token(orcid_profile_work_secret_fixture[i]);
		orcidToken.setOrcid(orcid_profile_work[i]);

		return orcidToken;
	}

	private static OrcidAccessToken getAccessTokenWork(int i) {
		OrcidAccessToken orcidToken = new OrcidAccessToken();

		orcidToken.setAccess_token(orcid_profile_work_secret[i]);
		orcidToken.setOrcid(orcid_profile_work[i]);

		return orcidToken;
	}

}
