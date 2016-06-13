package pt.ptcris;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;

import java.math.BigInteger;

import org.um.dsi.gavea.orcid.client.OrcidAccessToken;
import org.um.dsi.gavea.orcid.client.OrcidOAuthClient;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.activities.ActivitiesSummary;


public class ORCIDClientImpl implements ORCIDClient {

	private final OrcidAccessToken orcidToken;
	private final OrcidOAuthClient orcidClient;
	private final String clientId;

	public ORCIDClientImpl(String loginUri, String apiUri, String clientId, String clientSecret, String redirectUri, OrcidAccessToken orcidToken) {
		this.orcidToken = orcidToken;
		this.clientId = clientId;
		
		// Instantiate the Orcid Client
		this.orcidClient = new OrcidOAuthClient(loginUri, apiUri, clientId, clientSecret, redirectUri);
	}

	/**
	 * @see pt.ptcris.ORCIDClient#getFullWork(java.lang.Long)
	 */
	public Work getWork(BigInteger putCode) throws OrcidClientException {
	    return this.orcidClient.readWork(this.orcidToken, putCode.toString());
	}

	/**
	 * @see pt.ptcris.ORCIDClient#addWork(org.orcid.jaxb.model.record_rc2.Work)
	 */
	public String addWork(Work work) throws OrcidClientException {
		return this.orcidClient.addWork(this.orcidToken, work);          
	}

	/**
	 * @see pt.ptcris.ORCIDClient#deleteWork(java.lang.Long)
	 */
	public void deleteWork(BigInteger putCode) throws OrcidClientException {
		this.orcidClient.deleteWork(this.orcidToken, putCode.toString());		

		// NOTE: according to the ORCID API, to delete a work, one must provide
		// the entire list of works in the ORCID profile minus the work(s) that
		// should be deleted. This means that this operation must be done in
		// three steps: first, retrieve the entire set of works; second, remove
		// the
		// work to be deleted from the list of works; and three, send the
		// updated list to the ORCID API.
	}

	/**
	 * @see pt.ptcris.ORCIDClient#updateWork(java.lang.Long,
	 *      org.orcid.jaxb.model.record_rc2.Work)
	 */
	public void updateWork(BigInteger putCode, Work work) throws OrcidClientException {		
		this.orcidClient.updateWork(this.orcidToken, putCode.toString(), work);

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

	public String getClientId() {
		return this.clientId;
	}
		

}
