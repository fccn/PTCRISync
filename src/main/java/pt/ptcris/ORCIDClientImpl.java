package pt.ptcris;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.um.dsi.gavea.orcid.client.OrcidAccessToken;
import org.um.dsi.gavea.orcid.client.OrcidOAuthClient;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.activities.ActivitiesSummary;
import org.um.dsi.gavea.orcid.model.common.ScopePathType;

/**
 * An implementation of the ORCID client interface built over the
 * {@link org.um.dsi.gavea.orcid.client.OrcidOAuthClient Degois client}.
 * 
 */
public class ORCIDClientImpl implements ORCIDClient {

	private final OrcidAccessToken orcidToken;
	private final OrcidOAuthClient orcidClient;
	private final String clientId;

	/**
	 * Instantiates an ORCID client to communicate with the ORCID API.
	 * 
	 * @param loginUri
	 *            The login URI of the ORCID service.
	 * @param apiUri
	 *            The URI of the ORCID API.
	 * @param clientId
	 *            The id of the ORCID Member API client.
	 * @param clientSecret
	 *            The secret of the ORCID Member API client.
	 * @param redirectUri
	 *            The redirect URI for requesting the access token.
	 * @param orcidToken
	 *            The access token to the user ORCID profile.
	 */
	public ORCIDClientImpl(String loginUri, String apiUri, String clientId, String clientSecret, String redirectUri,
			OrcidAccessToken orcidToken) {
		this.orcidToken = orcidToken;
		this.clientId = clientId;
		
		// Instantiate the ORCID Client
		this.orcidClient = new OrcidOAuthClient(loginUri, apiUri, clientId, clientSecret, redirectUri);
		List<ScopePathType> scopes = new ArrayList<ScopePathType>();
		scopes.add(ScopePathType.AUTHENTICATE);
		scopes.add(ScopePathType.ORCID_PROFILE_READ_LIMITED);
		scopes.add(ScopePathType.ACTIVITIES_UPDATE);
		scopes.add(ScopePathType.ORCID_BIO_UPDATE);


	}

	/**
	 * @see pt.ptcris.ORCIDClient#getWork(BigInteger)
	 */
	public Work getWork(BigInteger putCode) throws OrcidClientException {
		return this.orcidClient.readWork(this.orcidToken, putCode.toString());
	}

	/**
	 * @see pt.ptcris.ORCIDClient#addWork(Work)
	 */
	public BigInteger addWork(Work work) throws OrcidClientException {
		//System.out.println(work.toString());
		return new BigInteger(this.orcidClient.addWork(this.orcidToken, work));
	}

	/**
	 * @see pt.ptcris.ORCIDClient#deleteWork(BigInteger)
	 */
	public void deleteWork(BigInteger putCode) throws OrcidClientException {
		this.orcidClient.deleteWork(this.orcidToken, putCode.toString());

		// TODO: is this note still relevant?
		// NOTE: according to the ORCID API, to delete a work, one must provide
		// the entire list of works in the ORCID profile minus the work(s) that
		// should be deleted. This means that this operation must be done in
		// three steps: first, retrieve the entire set of works; second, remove
		// the work to be deleted from the list of works; and three, send the
		// updated list to the ORCID API.
	}

	/**
	 * @see pt.ptcris.ORCIDClient#updateWork(BigInteger, Work)
	 */
	public void updateWork(BigInteger putCode, Work work) throws OrcidClientException {
		this.orcidClient.updateWork(this.orcidToken, putCode.toString(), work);

		// TODO: is this note still relevant?
		// NOTE: according to the ORCID API, to update a work, one must provide
		// the entire list of works in the ORCID profile including the work(s)
		// that should be updated. This means that this operation must be done
		// in three steps: first, retrieve the entire set of works; second,
		// replace the work to be updated with the new record in the list of
		// works; and three, send the updated list to the ORCID API.
	}

	/**
	 * @see pt.ptcris.ORCIDClient#getActivitiesSummary()
	 */
	public ActivitiesSummary getActivitiesSummary() throws OrcidClientException {
		return orcidClient.readActivitiesSummary(orcidToken);
	}

	/**
	 * @see pt.ptcris.ORCIDClient#getClientId()
	 */
	public String getClientId() {
		return this.clientId;
	}

}
