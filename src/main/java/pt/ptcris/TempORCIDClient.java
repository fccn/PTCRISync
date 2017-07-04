package pt.ptcris;

import javax.ws.rs.core.Response;

import org.um.dsi.gavea.orcid.client.OrcidAccessToken;
import org.um.dsi.gavea.orcid.client.OrcidOAuthClient;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.activities.Fundings;

/**
 * @deprecated temporary class to be deleted once readFundingsSummary is added to the
 * ORCID client.
 */
public class TempORCIDClient extends OrcidOAuthClient {

	private static final long serialVersionUID = 1L;

	public TempORCIDClient(String loginUri, String apiUri, String clientId,
			String clientSecret, String redirectUri, boolean debugMode) {
		super(loginUri, apiUri, clientId, clientSecret, redirectUri, debugMode);
	}

	public Fundings readFundingsSummary(final OrcidAccessToken token)
			throws OrcidClientException {
		Response response = null;
		try {
			response = get("/fundings", token, null);
			Fundings x = response.readEntity(Fundings.class);
			return x;
		} finally {
			if (response != null)
				response.close();
		}
	}
}
